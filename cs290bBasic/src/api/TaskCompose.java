/*
 * The MIT License
 *
 * Copyright 2015 Peter Cappello.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
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
package api;

import system.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import system.SpaceImpl;

/**
 *
 * @author Peter Cappello
 * @param <I> input type.
 */
public abstract class TaskCompose<I> extends Task
{
    private int numUnsetArgs;
    private List<I> args;
    private long decomposeTaskRunTime;
    private long sumChildT1;
    private long maxChildTinf;
    
    @Override
    abstract public ReturnValue call();
    
    /**
     *
     * @return the List of inputs.
     */
    synchronized public List<I> args() { return args; }
    
    /**
     * Set one of this task's inputs.
     * @param argNum the index of this input.
     * @param argValue the value of this input.
     * @param space if this is the last input this task is waiting for, put
     * the task in the space's ready task queue; remove it from the waiting task map.
     */
    synchronized public void arg( final int argNum, final I argValue, SpaceImpl space ) 
    { 
        assert numUnsetArgs > 0 &&  argValue != null && args.get( argNum ) == null; 
        args.set( argNum, argValue );
        if ( --numUnsetArgs == 0 )
        {
            space.putReadyTask( this );
            space.removeWaitingTask( id() );
        }
    }
    
    synchronized public void numArgs( int numArgs )
    {
        assert numArgs >= 0;
        numUnsetArgs = numArgs;
        args = Collections.synchronizedList( new ArrayList<>( numArgs ) ) ;
        for ( int i = 0; i < numArgs; i++ )
        {
            args.add( null );
        }
        assert args.size() == numArgs;
    }
        
    public void decomposeTaskRunTime( long time ) { decomposeTaskRunTime = time; }
    public long decomposeTaskRunTime() { return decomposeTaskRunTime; }
    
    public long sumChildT1() { return sumChildT1; }
    public void sumChildT1( long time ) { sumChildT1 +=  time; }
    
    public long maxChildTInf() { return maxChildTinf; }
    public void maxChildTInf( long time ) { maxChildTinf = maxChildTinf < time ? time : maxChildTinf; }
}
