/*
 * The MIT License
 *
 * Copyright 2015 peter.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a replaceWith
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, replaceWith, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package system;

import api.NullShared;
import api.RemoteEventListener;
import api.ReturnValue;
import api.Shared;
import api.Space;
import api.TaskCompose;
import applications.euclideantsp.SharedTour;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SpaceImpl implements the space for coordinating sending/receiving Task and Result objects.
 * @author Peter Cappello
 */
public final class SpaceImpl extends UnicastRemoteObject implements Space
{
    static final public int PROXIES_PER_PROCESSOR = 2;
    static final public int FINAL_RETURN_VALUE = -1;
    static final private AtomicInteger computerIds = new AtomicInteger();
    
    final private AtomicInteger taskIds = new AtomicInteger();
    final private BlockingQueue<Task>     readyTaskQ = new LinkedBlockingQueue<>();
    final private BlockingQueue<ReturnValue> resultQ = new LinkedBlockingQueue<>();
    final private BlockingQueue<SharedTour>   eventQ = new LinkedBlockingQueue<>();
    final private Map<Computer, ComputerProxy> computerProxies = Collections.synchronizedMap( new HashMap<>() );
    final private Map<UUID, TaskCompose>        waitingTaskMap = Collections.synchronizedMap( new HashMap<>() );
    final private AtomicInteger numTasks = new AtomicInteger();
    final private ComputerImpl computerInternal;
    final private Boolean sharedLock = true;
          private UUID rootTaskReturnValue;
          private Shared shared = new NullShared();
          private long tInf;
    
    public SpaceImpl() throws RemoteException 
    {
        computerInternal = new ComputerImpl( this );
        Logger.getLogger( getClass().getName() )
              .log( Level.INFO, "Space started." );
    }
    
    public Computer computer() { return computerInternal; }
    
    /**
     * Compute a Task and return its Return.
     * Precondition: rootTask is the only computation the Space is servicing.
     * 
     * @param rootTask task that encapsulates the overall computation.
     * @return the Task's Return object.
     */
    @Override public ReturnValue compute( Task rootTask )
    {
        assert readyTaskQ.isEmpty() && waitingTaskMap.isEmpty();
        initTimeMeasures();
        execute( rootTask );
        return take();
    }
    
    /**
     *
     * @param rootTask task that encapsulates the overall computation.
     * Precondition: rootTask is the only computation the Space is servicing.
     * @param shared
     * @param remoteEventConsumer
     * @return
     */
    @Override public ReturnValue compute( Task rootTask, Shared shared, RemoteEventListener remoteEventConsumer )
    {
        assert readyTaskQ.isEmpty() && waitingTaskMap.isEmpty();
        ListenerProxy listenerProxy = new ListenerProxy( remoteEventConsumer );
        listenerProxy.start();
        this.shared = shared;
        eventQ.add( (SharedTour) shared );
        initTimeMeasures();
        execute( rootTask );
        ReturnValue result = take();
        reportTimeMeasures( result );
        return result;
    }
    
    /**
     * Put a task into the Task queue.
     * @param task
     */
    private void execute( Task rootTask ) 
    { 
        rootTaskReturnValue = UUID.randomUUID();
        rootTask.composeId( rootTaskReturnValue );
        readyTaskQ.add( rootTask );
    }
    
    @Override synchronized public void putAll( final List<? extends Task> taskList ) { readyTaskQ.addAll( taskList ); }

    /**
     * Take a Return from the Return queue.
     * @return a Return object.
     */
    @Override public ReturnValue take() 
    {
        try { return resultQ.take(); } 
        catch ( InterruptedException ignore ) 
        {
            Logger.getLogger( getClass().getName() )
                  .log(Level.INFO, null, ignore );
        }
        assert false; // should never reach this point
        return null;
    }
    
    public Shared shared() { return shared; }

    /**
     * Register Computer with Space.  
     * Will override existing key-value pair, if any.
     * @param computer
     * @param numProcessors
     * @throws RemoteException
     */
    @Override public void register( Computer computer, int numProcessors ) throws RemoteException
    {
        final ComputerProxy computerProxy = new ComputerProxy( computer, PROXIES_PER_PROCESSOR* numProcessors );
        computerProxies.put( computer, computerProxy );
        computerProxy.startWorkerProxies();
        Logger.getLogger( getClass().getName() )
              .log( Level.INFO, "Registered computer {0}.", computerProxy.computerId );    
    }
    
    /**
     *
     * @param args unused.
     * @throws Exception
     */
    public static void main( final String[] args ) throws Exception
    {
        System.setSecurityManager( new SecurityManager() );
        LocateRegistry.createRegistry( Space.PORT )
                      .rebind(Space.SERVICE_NAME, new SpaceImpl() );
    }

    synchronized public void processResult( final Task parentTask, final Return result )
    { 
        result.process( parentTask, this );
        shared = newerShared( result.shared() );
//        t1 += result.taskRunTime();
        numTasks.getAndIncrement();
    }
    
    private Shared newerShared( final Shared that )
    {
        synchronized ( sharedLock )
        {
            if ( this.shared.isOlderThan( that ) )
            {
                eventQ.add( (SharedTour) that );
                return that;
            }
            else
            {
                return this.shared;
            }
        }
    }
    
    public int makeTaskId() { return taskIds.incrementAndGet(); }
    
    public TaskCompose getCompose( final UUID composeId ) { return waitingTaskMap.get( composeId ); }
            
    public void putCompose( final TaskCompose compose )
    {
        assert waitingTaskMap.get( compose.id() ) == null : compose.id(); 
        waitingTaskMap.put( compose.id(), compose );
    }
    
    public void putReadyTask( final Task task ) 
    { 
        assert waitingTaskMap.get( task.composeId() ) != null 
            || task.composeId() == rootTaskReturnValue : task.composeId();
        try 
        { 
            processResult( task, computer().execute( task, shared() ) );
        }
        catch ( RemoteException ignore ) { /* Not a Remote invocation. */ }  
    }
    
    public void putReadyTasks( final List<? extends Task> tasks ) { readyTaskQ.addAll( tasks ); }
    
    public void removeWaitingTask( final UUID composeId ) { waitingTaskMap.remove( composeId ); }
    
    public void putResult( final ReturnValue result ) { resultQ.add( result ); }
    
    public void tInf( final long tInf ) { this.tInf = tInf; }
    
    private void initTimeMeasures()
    {
        numTasks.getAndSet( 0 );
        tInf = 0;
    }
    
    public UUID rootTaskReturnValue() { return rootTaskReturnValue; }
    
    private void reportTimeMeasures( final Return result )
    {
        Logger.getLogger( getClass().getCanonicalName() )
              .log( Level.INFO, 
                    "\n\tTotal tasks: {0} \n\tT_1: {1}ms.\n\tT_inf: {2}ms.\n\tT_1 / T_inf: {3}", 
                    new Object[]{ numTasks, result.t1() / 1000000, result.tInf() / 1000000, result.t1() / result.tInf() } );
    }
    
    private class ComputerProxy
    {
        final private Computer computer;
        final private int computerId = computerIds.getAndIncrement();
        final private Map<Integer, WorkerProxy> workerMap = new HashMap<>();

        ComputerProxy( final Computer computer, final int numWorkerProxies )
        { 
            this.computer = computer;
            for ( int id = 0; id < numWorkerProxies; id++ )
            {
                WorkerProxy workerProxy = new WorkerProxy( id );
                workerMap.put( id, workerProxy );
            }
        }
        
        private void startWorkerProxies()
        {
            for ( WorkerProxy workerProxy : workerMap.values() )
            {
                workerProxy.start();
            }
        }
        
        private void unregister( final Task task, final Computer computer, final int workerProxyId )
        {
            readyTaskQ.add( task );
            workerMap.remove( workerProxyId );
            Logger.getLogger( getClass().getName() )
                  .log( Level.WARNING, "Computer {0}: Worker failed.", workerProxyId );
            if ( workerMap.isEmpty() )
            {
                computerProxies.remove( computer );
                Logger.getLogger( getClass().getName() )
                      .log( Level.WARNING, "Computer {0} failed.", computerId );
            }
        }
             
        private class WorkerProxy extends Thread
        {
            final private Integer id;
            
            private WorkerProxy( final int id ) { this.id = id; }
            
            @Override public void run()
            {
                while ( true )
                {
                    Task task = null;
                    try 
                    { 
                        task = readyTaskQ.take();
                        processResult( task, computer.execute( task, shared ) );
                    }
                    catch ( RemoteException ignore )
                    {
                        unregister( task, computer, id );
                        ignore.printStackTrace();
                        return;
                    } 
                    catch ( InterruptedException ex ) 
                    { 
                        Logger.getLogger( getClass().getName() )
                              .log( Level.INFO, null, ex ); 
                    }
                }
            }   
        }
    }
    
    private class ListenerProxy extends Thread
    {
        private final RemoteEventListener remoteListener;
        
        private ListenerProxy(final RemoteEventListener remoteListener )
        {
            this.remoteListener = remoteListener;
        }
        
        @Override public void run()
        {
            while ( true )
            {
                try   { remoteListener.accept( eventQ.take() ); } 
                catch ( InterruptedException | RemoteException ex ) 
                {
                    Logger.getLogger( getClass().getName() )
                          .log( Level.INFO, "Listener accept of event failed", ex );
                }
            }
        }
    }
}
