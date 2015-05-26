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
package applications.euclideantsp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import static util.EuclideanGraph.distance;

/**
 *
 * @author Peter Cappello
 */
final public class LowerBoundNearestNeighbors implements LowerBound 
{
    static final private Integer    EMPTY = -1;
    static final private double[][] CITIES = TaskEuclideanTsp.CITIES;
           final private List<Deque<Integer>> nearestNeighborsList;
           final private double lowerBound;
    
    public LowerBoundNearestNeighbors()
    {
        nearestNeighborsList = initializeNearestNeighbors();
        lowerBound = initializeLowerBound();
    }
    
    public LowerBoundNearestNeighbors( final List<Deque<Integer>> nearestNeighbors, final double lowerBound ) 
    {
        List<Deque<Integer>> copyNearestNeighbors = new ArrayList<>( CITIES.length );
        for ( int city = 0; city < nearestNeighbors.size(); city++ )
        {
            Deque<Integer> deque = new ArrayDeque<>( 2 );
            Integer[] array = nearestNeighbors.get( city ).toArray( new Integer[ 0 ] );
            for ( int neighbor = 0; neighbor < array.length; neighbor++ )
            {
                deque.add( array[ neighbor ] ); 
            }
            copyNearestNeighbors.add( deque );
        }
        this.nearestNeighborsList = copyNearestNeighbors;
        this.lowerBound = lowerBound;
    }
    
    private double initializeLowerBound()
    {
        double bound = 0.0;
        for ( int city = 0; city < CITIES.length; city++ )
        {
            final Deque<Integer> deque = nearestNeighborsList.get( city );
            bound += distance( CITIES[ city ], CITIES[ deque.peekFirst() ] );
            bound += distance( CITIES[ city ], CITIES[ deque.peekLast()  ] );
        }
        return bound / 2.0;
    }
    
    private List<Deque<Integer>> initializeNearestNeighbors()
    {
        final List<Deque<Integer>> neighbors = new ArrayList<>( CITIES.length );
        for ( int city = 0; city < CITIES.length; city++ )
        {
            Deque<Integer> cityNearestNeighbors = new ArrayDeque<>( 2 );
            cityNearestNeighbors.add( EMPTY );
            cityNearestNeighbors.add( EMPTY );
            for ( int neighbor = 0; neighbor < CITIES.length; neighbor++ )
            {
                if ( neighbor != city )
                {
                    if ( cityNearestNeighbors.peekFirst().equals( EMPTY ) || distance( CITIES[ city ], CITIES[ neighbor ] ) < distance( CITIES[ city ], CITIES[ cityNearestNeighbors.peekFirst() ] ) )
                    {
                        cityNearestNeighbors.removeLast();
                        cityNearestNeighbors.addFirst( neighbor );
                    }
                    else if ( cityNearestNeighbors.peekLast().equals( EMPTY ) || distance( CITIES[ city ], CITIES[ neighbor ] ) < distance( CITIES[ city ], CITIES[ cityNearestNeighbors.peekLast() ] ) )
                    {
                        cityNearestNeighbors.removeLast();
                        cityNearestNeighbors.addLast( neighbor );
                    }
                }
            }
            assert ! cityNearestNeighbors.peekFirst().equals( EMPTY );
            assert ! cityNearestNeighbors.peekLast().equals(  EMPTY );
            assert distance( CITIES[ city ], CITIES[ cityNearestNeighbors.peekFirst() ] ) <= distance( CITIES[ city ], CITIES[ cityNearestNeighbors.peekLast() ] );
            neighbors.add( cityNearestNeighbors );
        }
        return neighbors;
    }

    @Override
    public double cost() { return lowerBound; }
    
    @Override
    public LowerBound make( TaskEuclideanTsp parentTask, Integer newCity ) 
    {
        // make a copy of nearestNeighbors: List<Deque<Integer>>
        final List<Deque<Integer>> copyNearestNeighbors = new ArrayList<>( CITIES.length );
        for ( Deque<Integer> nearestNeighbors : nearestNeighborsList )
        {
            Deque<Integer> deque = new ArrayDeque<>( 2 );
            Integer[] array = nearestNeighbors.toArray( new Integer[ 0 ] );
            deque.addAll( Arrays.asList( array ) );
            copyNearestNeighbors.add( deque );
        }
        // oldCity is the end of the existing path nearestNeighborsList incrementally with newCity
        final Integer oldCity = parentTask.tour().get( parentTask.tour().size() - 1 );
        
        // update nearestNeighborsList incrementally: replace old & new path endpoints' virtual edge w/ actual edge
        final Integer oldCitysVirtualEndpoint = updateEndpoint( copyNearestNeighbors, oldCity, newCity );
        assert copyNearestNeighbors.get( oldCity ).size() < 2 : "from " + oldCity + " to " + newCity + " " + copyNearestNeighbors.get( oldCity );
        final Integer newCitysVirtualEndpoint = updateEndpoint( copyNearestNeighbors, newCity, oldCity );
        assert copyNearestNeighbors.get( newCity ).size() < 2 : "from " + newCity + " to " + oldCity + " " + copyNearestNeighbors.get( newCity );
        
        // update lowerBound incrementally
        double newLowerBound = lowerBound
                + distance( CITIES[ oldCity ], CITIES[ newCity ] )
                - (  distance( CITIES[ oldCity ], CITIES[ oldCitysVirtualEndpoint ] )
                   + distance( CITIES[ newCity ], CITIES[ newCitysVirtualEndpoint ] )
                  ) / 2.0;
        if ( parentTask.unvisitedCities().size() == 1 )
        {
            // tour is complete: make lower bound equal the cost of the tour: tourDistance( CITIES, partialTour );
            assert copyNearestNeighbors.get( 0 ).size() == 1 : copyNearestNeighbors.get( 0 );
            assert copyNearestNeighbors.get( newCity ).size() == 1 : copyNearestNeighbors.get( newCity ) + " newCity: " + newCity + " oldCity: " + oldCity + " unvisited: " + parentTask.unvisitedCities() + " tour: " + parentTask.tour();
            newLowerBound += distance( CITIES[ 0 ], CITIES[ newCity ] ); 
            newLowerBound -= ( distance( CITIES[ 0 ], CITIES[ copyNearestNeighbors.get( 0 ).removeFirst() ] )
                               + distance( CITIES[ newCity ], CITIES[ copyNearestNeighbors.get( newCity ).removeFirst() ] ) 
                             ) / 2.0;
        }
        return new LowerBoundNearestNeighbors( copyNearestNeighbors, newLowerBound );
    }
    /**
     * Update the nearestNeighbors data structure with new actual edge 
     * (from path's old endpoint to its new endpoint city).
     * @param nn nearestNeighbor data structure
     * @param fromCity endpoint of old path
     * @param toCity endpoint of new, extended path
     * @return the city that is the endpoint of the virtual edge to be replaced by the actual edge.
     */
    private Integer updateEndpoint( final List<Deque<Integer>> nn, final Integer fromCity, final Integer toCity )
    {
        return  toCity.equals( nn.get( fromCity ).peekFirst() )
                ? nn.get( fromCity ).removeFirst()
                : nn.get( fromCity ).removeLast();
    }
}
