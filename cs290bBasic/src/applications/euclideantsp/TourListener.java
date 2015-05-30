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

import api.RemoteEventListener;
import static applications.euclideantsp.TaskEuclideanTsp.CITIES;
import static applications.euclideantsp.ReturnValueTour.NUM_PIXELS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 *
 * @author Peter Cappello
 */
public class TourListener extends    JFrame 
                          implements RemoteEventListener<SharedTour>, 
                                     Runnable
{
    static private final List<SharedTour> tours = new ArrayList<>();
    static private final List<JLabel> tourLabels = new ArrayList<>();
    static private final BlockingQueue<SharedTour> eventQ = new LinkedBlockingQueue<>();
    static private final String title = "Sequence of tour discoveries";
    static private final JPanel controlPanel = new JPanel();
    static private final JLabel costLabel = new JLabel( "Cost: " );
    static private final JTextField costTextField = new JTextField();
    static private final JButton prevButton = new JButton();
    static private final JButton nextButton = new JButton();
    static private int currentIndex;
    
    final Container container = getContentPane();
    
    TourListener()
    {
        controlPanel.setLayout( new GridLayout() );
        controlPanel.add( prevButton );
        controlPanel.add( costLabel );
        controlPanel.add( costTextField );
        controlPanel.add( nextButton );
        setTitle( title );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        
        nextButton.addActionListener( actionEvent -> nextButtonActionPerformed( actionEvent ) );
        prevButton.addActionListener( actionEvent -> prevButtonActionPerformed( actionEvent ) );
    }
    /**
     *
     * @param sharedTour
     * @throws RemoteException
     */
    @Override public void accept( SharedTour sharedTour ) throws RemoteException 
    {
        tours.add( sharedTour );
        eventQ.add( sharedTour );
        currentIndex = tours.size() - 1;
    }
    
    @Override public void run()
    {
        container.setLayout( new BorderLayout() );
        container.add( controlPanel, BorderLayout.SOUTH );
        while ( true )
        {
            SharedTour sharedTour = null;
            try { sharedTour = eventQ.take(); } 
            catch (InterruptedException ex) {}
            costTextField.setText( new Double( sharedTour.cost() ).toString() );
            JLabel jLabel = view( sharedTour );
            tourLabels.add( tourLabels.size(), jLabel );
            container.add( new JScrollPane( jLabel ), BorderLayout.CENTER );
            pack();
            setVisible( true );
        }
    }
    
    public JLabel view( SharedTour sharedTour ) 
    {
        Logger.getLogger( getClass().getCanonicalName() )
              .log( Level.INFO, sharedTour.toString() );
        List<Integer> cityList = sharedTour.tour();
        Integer[] tour = cityList.toArray( new Integer[0] );

        // display the graph graphically, as it were
        // get minX, maxX, minY, maxY, assuming 0.0 <= mins
        double minX = CITIES[0][0], maxX = CITIES[0][0];
        double minY = CITIES[0][1], maxY = CITIES[0][1];
        for ( double[] cities : CITIES ) 
        {
            if ( cities[0] < minX ) 
                minX = cities[0];
            if ( cities[0] > maxX ) 
                maxX = cities[0];
            if ( cities[1] < minY ) 
                minY = cities[1];
            if ( cities[1] > maxY ) 
                maxY = cities[1];
        }

        // scale points to fit in unit square
        final double side = Math.max( maxX - minX, maxY - minY );
        double[][] scaledCities = new double[CITIES.length][2];
        for ( int i = 0; i < CITIES.length; i++ )
        {
            scaledCities[i][0] = ( CITIES[i][0] - minX ) / side;
            scaledCities[i][1] = ( CITIES[i][1] - minY ) / side;
        }

        final Image image = new BufferedImage( NUM_PIXELS, NUM_PIXELS, BufferedImage.TYPE_INT_ARGB );
        final Graphics graphics = image.getGraphics();

        final int margin = 10;
        final int field = NUM_PIXELS - 2*margin;
        // draw edges
        graphics.setColor( Color.BLUE );
        int x1, y1, x2, y2;
        int city1 = tour[0], city2;
        x1 = margin + (int) ( scaledCities[city1][0]*field );
        y1 = margin + (int) ( scaledCities[city1][1]*field );
        for ( int i = 1; i < CITIES.length; i++ )
        {
            city2 = tour[i];
            x2 = margin + (int) ( scaledCities[city2][0]*field );
            y2 = margin + (int) ( scaledCities[city2][1]*field );
            graphics.drawLine( x1, y1, x2, y2 );
            x1 = x2;
            y1 = y2;
        }
        city2 = tour[0];
        x2 = margin + (int) ( scaledCities[city2][0]*field );
        y2 = margin + (int) ( scaledCities[city2][1]*field );
        graphics.drawLine( x1, y1, x2, y2 );

        // draw vertices
        final int VERTEX_DIAMETER = 6;
        graphics.setColor( Color.RED );
        for ( int i = 0; i < CITIES.length; i++ )
        {
            int x = margin + (int) ( scaledCities[i][0]*field );
            int y = margin + (int) ( scaledCities[i][1]*field );
            graphics.fillOval( x - VERTEX_DIAMETER/2,
                               y - VERTEX_DIAMETER/2,
                              VERTEX_DIAMETER, VERTEX_DIAMETER);
            graphics.drawString( new Integer( i ).toString(), x - 1, y - 1);
        }
        final ImageIcon imageIcon = new ImageIcon( image );
        return new JLabel( imageIcon );
    }
    
    private void nextButtonActionPerformed( ActionEvent actionEvent )
    {
        if ( currentIndex < tours.size() - 1 )
        {
            currentIndex++;
            if ( currentIndex == tours.size() - 1 )
            {
                nextButton.setEnabled( false );
            }
            prevButton.setEnabled( true );
            costTextField.setText( new Double( tours.get( currentIndex ).cost() ).toString() );
            container.add( new JScrollPane( tourLabels.get( currentIndex ) ), BorderLayout.CENTER );
            pack();
            setVisible( true );
        }
    }
    
    private void prevButtonActionPerformed( ActionEvent actionEvent )
    {
        if ( currentIndex > 0 )
        {
            currentIndex--;
            if ( currentIndex == 0 )
            {
                prevButton.setEnabled( false );
            }
            nextButton.setEnabled( true );
            costTextField.setText( new Double( tours.get( currentIndex ).cost() ).toString() );
            container.add( new JScrollPane( tourLabels.get( currentIndex ) ), BorderLayout.CENTER );
            pack();
            setVisible( true );
        }
    }
}
