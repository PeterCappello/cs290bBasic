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

import api.ReturnValue;
import api.TaskCompose;

/**
 *
 * @author Peter Cappello
 */
public class MinTour extends TaskCompose<Tour>
{
    @Override
    public ReturnValue call() 
    {
        Tour shortestTour = args().remove( 0 );
        int numNodes = shortestTour.numNodes();
        int numPrunedNodes = shortestTour.numPrunedNodes();
        int totalPruneHeights = shortestTour.totalPruneHeights();
        for ( Tour tour : args() ) 
        {
            if ( tour.compareTo( shortestTour ) < 0 )
            {
                shortestTour = tour;
            }
            numNodes += tour.numNodes();
            numPrunedNodes += tour.numPrunedNodes();
            totalPruneHeights += tour.totalPruneHeights();
        }
        Tour minTour = new Tour( shortestTour.tour(), shortestTour.cost(),
        numNodes, numPrunedNodes, totalPruneHeights );
        return new ReturnValueTour( this, minTour );
    }
}
