/*
 * The MIT License
 *
 * Copyright 2015 peter.
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

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Peter Cappello
 */
public class Tour implements Comparable<Tour>, Serializable
{
    final private List<Integer> tour;
    final private double cost;
    final private int numNodes;
    final private int numPrunedNodes;
    final private int totalPruneHeights;
    
    /**
     * Return container for TaskEuclideanTsp.
     * @param tour
     * @param cost
     * @param numNodes
     * @param numPrunedNodes
     * @param totalPruneHeights
     */
    public Tour( List<Integer> tour, double cost, int numNodes, int numPrunedNodes, int totalPruneHeights )
    {
        this.tour = tour;
        this.cost = cost;
        this.numNodes = numNodes;
        this.numPrunedNodes = numPrunedNodes;
        this.totalPruneHeights = totalPruneHeights;
    } 
 
    public double cost() { return cost; }
    public List<Integer> tour() { return tour; }
    public int numNodes() { return numNodes; }
    public int numPrunedNodes() { return numPrunedNodes; }
    public int totalPruneHeights() { return totalPruneHeights; }

    @Override
    public int compareTo( Tour tour )
    { 
        return this.cost < tour.cost ? -1 : this.cost > tour.cost ? 1 : 0;
    }
    
    @Override
    public String toString() { return tour.toString() + "\n\tCost: " + cost; }
}
