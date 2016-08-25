/*******************************************************************************
 * Name: Java class ArrayOpsTest.java
 * Project: PRISM strong motion record processing using COSMOS data format
 * Written by: Jeanne Jones, USGS, jmjones@usgs.gov
 * 
 * This software is in the public domain because it contains materials that 
 * originally came from the United States Geological Survey, an agency of the 
 * United States Department of Interior. For more information, see the official 
 * USGS copyright policy at 
 * http://www.usgs.gov/visual-id/credit_usgs.html#copyright
 * 
 * Date: first release date Feb. 2015
 ******************************************************************************/

package PRISMtest.Package;

import SmProcessing.ArrayOps;
import SmProcessing.ArrayStats;
import SmUtilities.TextFileReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jmjones
 */
public class ArrayOpsTest {
    double[] posconstant;
    double[] zeroconstant;
    double[] negconstant;
    double[] linecentered;
    double[] linepos;
    double[] lineneg;
    double[] lineinte;
    double[] poly;
    double[] polypos;
    double[] polysin;
    double[] polyline;
    double[] poly3order;
    double[] time;
    int[] counter;
    static int LENGTH = 100;
    static double EPSILON = 0.1;
    double BIG_EPSILON = 1.0;
    double SM_EPSILON = 0.001;
    double STEP = 1.0;
    double[] smooth;
    static double[] accel;
    static double[] vel;
    static double[] disp;
    static double[] velForDiff;
    static double[] accelForDiff;
    static String[] filecontents;
    
    static String accelfile = "/PRISMtest/Data/acceleration.txt";
    static String velfile = "/PRISMtest/Data/velocity.txt";
    static String dispfile = "/PRISMtest/Data/displacement.txt";
    static String velDiffile = "/PRISMtest/Data/velocityCentralDiff.txt";
    static String accDiffile = "/PRISMtest/Data/accel5_MATLAB.csv";
    
    static ArrayStats centerstat;
    static ArrayStats posstat;
    static ArrayStats negstat;
    static ArrayStats polystat;
    
    public ArrayOpsTest() {
        posconstant = new double[LENGTH];
        zeroconstant = new double[LENGTH];
        negconstant = new double[LENGTH];
        linecentered = new double[LENGTH];
        lineinte = new double[LENGTH];
        linepos = new double[LENGTH];
        lineneg = new double[LENGTH];
        polypos = new double[LENGTH];
        poly = new double[LENGTH];
        polysin = new double[LENGTH];
        polyline = new double[LENGTH];
        poly3order = new double[LENGTH];
        time = new double[LENGTH];
        counter = new int[LENGTH];
        smooth = new double[LENGTH];
        
        Arrays.fill(posconstant, 2.0);
        Arrays.fill(zeroconstant, 0.0);
        Arrays.fill(negconstant, -3.8);
        for (int i = 0; i < LENGTH; i++) {
            counter[i] = 2 * i;
            time[i] = i;
            linecentered[i] = (0.1 * i) - 5.0;
            linepos[i] = (0.1 * i);
            lineneg[i] = (0.1 * i) - 10.0;
            lineinte[i] = 2.0 * i;
            poly[i] = Math.pow(i, 2);
            polysin[i] = Math.sin((double)i);
            polypos[i] = polysin[i] + 4.0;
            polyline[i] = polysin[i] + (0.1 * i);
            poly3order[i] = Math.pow(i,3) + 2.5*Math.pow(i,2) - 8.9*i - 4.2;
        }
        Arrays.fill(smooth,2.0);
        smooth[9] = 2.25;
        smooth[10] = 2.5;
        smooth[11] = 2.25;

        centerstat = new ArrayStats( linecentered );
        posstat = new ArrayStats( linepos );
        negstat = new ArrayStats( lineneg );
        polystat = new ArrayStats( polypos );
    }
    
    @BeforeClass
    public static void setUp() throws IOException, URISyntaxException {
        File name;
        TextFileReader infile;

        int next = 0;
        URL url = ArrayOpsTest.class.getResource( accelfile );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
//            System.out.println("acclen: " + filecontents.length);
            accel = new double[filecontents.length];
            for (String num : filecontents) {
                accel[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( velfile );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
//            System.out.println("vellen: " + filecontents.length);
            vel = new double[filecontents.length];
            for (String num : filecontents) {
                vel[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( dispfile );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            disp = new double[filecontents.length];
            for (String num : filecontents) {
                disp[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( velDiffile );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            velForDiff = new double[filecontents.length];
            for (String num : filecontents) {
                velForDiff[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( accDiffile );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            accelForDiff = new double[filecontents.length];
            for (String num : filecontents) {
                accelForDiff[next++] = Double.parseDouble(num);
            }
        }
    }
    
     @Test
     public void testRemoveValue() throws IOException {

        double[] test = new double[LENGTH];
         System.arraycopy(zeroconstant, 0, test, 0, LENGTH);
         ArrayOps.removeValue(test, 0.0);
         org.junit.Assert.assertArrayEquals(zeroconstant, test, EPSILON);

         System.arraycopy(posconstant, 0, test, 0, LENGTH);
         ArrayOps.removeValue(test, 2.0);
         org.junit.Assert.assertArrayEquals(zeroconstant, test, EPSILON);
         
         System.arraycopy(negconstant, 0, test, 0, LENGTH);
         ArrayOps.removeValue(test, -3.8);
         org.junit.Assert.assertArrayEquals(zeroconstant, test, EPSILON);
         
         System.arraycopy(linecentered, 0, test, 0, LENGTH);
         ArrayOps.removeValue(test, centerstat.getMean());
         org.junit.Assert.assertArrayEquals(linecentered, test, EPSILON);
         
         System.arraycopy(linepos, 0, test, 0, LENGTH);
         ArrayOps.removeValue(test, posstat.getMean());
         org.junit.Assert.assertArrayEquals(linecentered, test, EPSILON);
         
         System.arraycopy(lineneg, 0, test, 0, LENGTH);
         ArrayOps.removeValue(test, negstat.getMean());
         org.junit.Assert.assertArrayEquals(linecentered, test, EPSILON);
         
         boolean result;
         System.arraycopy(polypos, 0, test, 0, LENGTH);
         result = ArrayOps.removeValue(test, polystat.getMean());
         org.junit.Assert.assertArrayEquals(polysin, test, EPSILON);
         org.junit.Assert.assertEquals(true, result);
         
         double[] test2 = null;
         result = ArrayOps.removeValue(test2, 1.0);
         org.junit.Assert.assertEquals(false, result);
         test2 = new double[0];
         result = ArrayOps.removeValue(test2, 1.0);
         org.junit.Assert.assertEquals(false, result);
     }
     @Test
     public void testRemoveLinearTrend() {
         double[] test = new double[LENGTH];
         System.arraycopy(linecentered, 0, test, 0, LENGTH);
         ArrayOps.removeLinearTrend(test, STEP);
         org.junit.Assert.assertArrayEquals(zeroconstant, test, EPSILON);
         
         System.arraycopy(linepos, 0, test, 0, LENGTH);
         ArrayOps.removeLinearTrend(test, STEP);
         org.junit.Assert.assertArrayEquals(zeroconstant, test, EPSILON);
         
         System.arraycopy(lineneg, 0, test, 0, LENGTH);
         ArrayOps.removeLinearTrend(test, STEP);
         org.junit.Assert.assertArrayEquals(zeroconstant, test, EPSILON);
         
         System.arraycopy(zeroconstant, 0, test, 0, LENGTH);
         ArrayOps.removeLinearTrend(test, STEP);
         org.junit.Assert.assertArrayEquals(zeroconstant, test, EPSILON);
         
         System.arraycopy(posconstant, 0, test, 0, LENGTH);
         ArrayOps.removeLinearTrend(test, STEP);
         org.junit.Assert.assertArrayEquals(zeroconstant, test, EPSILON);
         
         System.arraycopy(negconstant, 0, test, 0, LENGTH);
         ArrayOps.removeLinearTrend(test, STEP);
         org.junit.Assert.assertArrayEquals(zeroconstant, test, EPSILON);
         
         System.arraycopy(polyline, 0, test, 0, LENGTH);
         ArrayOps.removeLinearTrend(test, STEP);
         org.junit.Assert.assertArrayEquals(polysin, test, EPSILON);

         double[] test2 = null;
         boolean result;
         result = ArrayOps.removeLinearTrend(test2, STEP);
         org.junit.Assert.assertEquals(false, result);
         test2 = new double[0];
         result = ArrayOps.removeLinearTrend(test2, STEP);
         org.junit.Assert.assertEquals(false, result);
         System.arraycopy(polyline, 0, test, 0, LENGTH);
         result = ArrayOps.removeLinearTrend(test, STEP);
         org.junit.Assert.assertEquals(true, result);
         System.arraycopy(polyline, 0, test, 0, LENGTH);
         result = ArrayOps.removeLinearTrend(test, 0.0);
         org.junit.Assert.assertEquals(false, result);
    }
     @Test
     public void testIntegrateDifferentiate() {
         double[] test = new double[0];
         double[] test1 = new double[0];
         double[] test2 = null;
         org.junit.Assert.assertArrayEquals(lineinte, ArrayOps.differentiate(poly, STEP), BIG_EPSILON);
         org.junit.Assert.assertArrayEquals(posconstant, ArrayOps.differentiate(lineinte, STEP), EPSILON);
         org.junit.Assert.assertArrayEquals(test, ArrayOps.differentiate(lineinte, 0.0), EPSILON);
         org.junit.Assert.assertArrayEquals(test1, ArrayOps.differentiate(test, STEP), EPSILON);
         org.junit.Assert.assertArrayEquals(test1, ArrayOps.differentiate(test2, STEP), EPSILON);
         org.junit.Assert.assertArrayEquals(poly, ArrayOps.integrate(lineinte, STEP,0.0), EPSILON);
         org.junit.Assert.assertArrayEquals(lineinte, ArrayOps.integrate(posconstant, STEP,0.0), EPSILON);
         org.junit.Assert.assertArrayEquals(test, ArrayOps.integrate(lineinte, 0.0,0.0), EPSILON);
         org.junit.Assert.assertArrayEquals(test1, ArrayOps.integrate(test, STEP,0.0), EPSILON);
         org.junit.Assert.assertArrayEquals(test1, ArrayOps.integrate(test2, STEP,0.0), EPSILON);
     }
     @Test
     public void testMakeTimeArray() {
         org.junit.Assert.assertArrayEquals(time, ArrayOps.makeTimeArray(1.0, LENGTH), EPSILON);
         double[] test = new double[0];
         org.junit.Assert.assertArrayEquals(test, ArrayOps.makeTimeArray(1.0, 0), EPSILON);
         org.junit.Assert.assertArrayEquals(test, ArrayOps.makeTimeArray(0.0, LENGTH), EPSILON);
     }
     @Test
     public void testRemoveLinearTrendFromSubArray() {
         double[] test = new double[LENGTH];
         boolean result;
         System.arraycopy(polyline, 0, test, 0, LENGTH);
         result = ArrayOps.removeLinearTrendFromSubArray(test, linepos ,STEP);
         org.junit.Assert.assertArrayEquals(polysin, test, EPSILON);
         org.junit.Assert.assertEquals(true, result);
         double[] test2 = null;
         result = ArrayOps.removeLinearTrendFromSubArray(test2, linepos ,STEP);
         org.junit.Assert.assertEquals(false, result);
         result = ArrayOps.removeLinearTrendFromSubArray(test, test2 ,STEP);
         org.junit.Assert.assertEquals(false, result);
         test2 = new double[0];
         result = ArrayOps.removeLinearTrendFromSubArray(test2, linepos ,STEP);
         org.junit.Assert.assertEquals(false, result);
         result = ArrayOps.removeLinearTrendFromSubArray(test, test2 ,STEP);
         org.junit.Assert.assertEquals(false, result);
         result = ArrayOps.removeLinearTrendFromSubArray(test, linepos ,0.0);
         org.junit.Assert.assertEquals(false, result);
     }
     @Test
     public void testFindAndRemovePolynomialTrend() {
         double[] coefs;
         double[] test = new double[LENGTH];
         double[] empty = new double[0];
         boolean result;
         System.arraycopy(poly, 0, test, 0, LENGTH);
         coefs = ArrayOps.findPolynomialTrend(test, 2, STEP);
         result = ArrayOps.removePolynomialTrend(test, coefs, STEP);
         org.junit.Assert.assertArrayEquals(zeroconstant, test, EPSILON);
         org.junit.Assert.assertEquals(true, result);
         
         System.arraycopy(poly3order, 0, test, 0, LENGTH);
         coefs = ArrayOps.findPolynomialTrend(test, 3, STEP);
         result = ArrayOps.removePolynomialTrend(test, coefs, STEP);
         org.junit.Assert.assertArrayEquals(zeroconstant, test, EPSILON);
         org.junit.Assert.assertEquals(true, result);
         
         //time step = 0
         result = ArrayOps.removePolynomialTrend(test, coefs, 0.0);
         org.junit.Assert.assertEquals(false, result);
         org.junit.Assert.assertArrayEquals(empty,ArrayOps.findPolynomialTrend(poly, 2, 0.0), EPSILON);
         //no input array
         test = null;
         result = ArrayOps.removePolynomialTrend(test, coefs, STEP);
         org.junit.Assert.assertEquals(false, result);
         org.junit.Assert.assertArrayEquals(empty,ArrayOps.findPolynomialTrend(test, 2, 2.0), EPSILON);
         //zero length input array
         test = new double[0];
         result = ArrayOps.removePolynomialTrend(test, coefs, STEP);
         org.junit.Assert.assertEquals(false, result);
         org.junit.Assert.assertArrayEquals(empty,ArrayOps.findPolynomialTrend(test, 2, 2.0), EPSILON);
         //no coefs for remove trend
         test = new double[LENGTH];
         coefs = null;
         result = ArrayOps.removePolynomialTrend(test, coefs, STEP);
         org.junit.Assert.assertEquals(false, result);
         //zero length coefs array for remove trend
         coefs = new double[0];
         result = ArrayOps.removePolynomialTrend(test, coefs, STEP);
         org.junit.Assert.assertEquals(false, result);
         //invalid poly order for find trend
         org.junit.Assert.assertArrayEquals(empty,ArrayOps.findPolynomialTrend(poly, 0, 2.0), EPSILON);
     }
     @Test
     public void testFindTrendWithBestFit() {
         double[] coefs;
         double[] test;
         double[] empty = new double[0];
         coefs = ArrayOps.findPolynomialTrend(poly, 2, STEP);
         org.junit.Assert.assertArrayEquals(coefs, ArrayOps.findTrendWithBestFit(poly, STEP), EPSILON);

         coefs = ArrayOps.findPolynomialTrend(linecentered, 1, STEP);
         org.junit.Assert.assertArrayEquals(coefs, ArrayOps.findTrendWithBestFit(linecentered, STEP), EPSILON);

         org.junit.Assert.assertArrayEquals(empty, ArrayOps.findTrendWithBestFit(poly, 0.0), EPSILON);
         test = new double[0];
         org.junit.Assert.assertArrayEquals(empty, ArrayOps.findTrendWithBestFit(test, 2.0), EPSILON);
         test = null;
         org.junit.Assert.assertArrayEquals(empty, ArrayOps.findTrendWithBestFit(test, 2.0), EPSILON);
     }
     @Test
     public void testfindSubsetMean() {
         double val = ArrayOps.findSubsetMean(linepos, 0, 50);
         org.junit.Assert.assertEquals(2.5, val, EPSILON);
         org.junit.Assert.assertEquals(Double.MIN_VALUE, ArrayOps.findSubsetMean(linepos, -1, 50), EPSILON);
         org.junit.Assert.assertEquals(Double.MIN_VALUE, ArrayOps.findSubsetMean(linepos, 0, 2000), EPSILON);
         org.junit.Assert.assertEquals(Double.MIN_VALUE, ArrayOps.findSubsetMean(linepos, 10, 10), EPSILON);
         org.junit.Assert.assertEquals(Double.MIN_VALUE, ArrayOps.findSubsetMean(linepos, 10, 9), EPSILON);
         double[] test = null;
         org.junit.Assert.assertEquals(Double.MIN_VALUE, ArrayOps.findSubsetMean(test, 0, 50), EPSILON);
         test = new double[0];
         org.junit.Assert.assertEquals(Double.MIN_VALUE, ArrayOps.findSubsetMean(test, 0, 50), EPSILON);
     }
     @Test
     public void testRootMeanSquare() {
         double[] test1 = null;
         double[] test2 = new double[0];
         org.junit.Assert.assertEquals(0.0, ArrayOps.rootMeanSquare(posconstant,posconstant),EPSILON);
         org.junit.Assert.assertEquals(-1.0, ArrayOps.rootMeanSquare(test1,test1),EPSILON);
         org.junit.Assert.assertEquals(-1.0, ArrayOps.rootMeanSquare(test2,test2),EPSILON);
     }
     @Test
     public void testCountsToPhysicalValues() {
         int[] test1 = null;
         int[] test2 = new int[0];
         double[] testout = new double[0];
         org.junit.Assert.assertArrayEquals(time, ArrayOps.countsToPhysicalValues(counter,0.5), EPSILON);
         org.junit.Assert.assertArrayEquals(testout, ArrayOps.countsToPhysicalValues(test1,0.5), EPSILON);
         org.junit.Assert.assertArrayEquals(testout, ArrayOps.countsToPhysicalValues(test2,0.5), EPSILON);
     }
     @Test
     public void testConvertArrayUnits() {
         double[] test1 = null;
         double[] test2 = new double[0];
         double[] testout = new double[0];
         org.junit.Assert.assertArrayEquals(negconstant, ArrayOps.convertArrayUnits(posconstant,-1.9), EPSILON);
         org.junit.Assert.assertArrayEquals(testout, ArrayOps.convertArrayUnits(test1,-1.9), EPSILON);
         org.junit.Assert.assertArrayEquals(testout, ArrayOps.convertArrayUnits(test2,-1.9), EPSILON);
     }
     @Test
     public void testPerform3PtSmoothing() {
         double[] test1 = null;
         double[] test2 = new double[0];
         double[] testout = new double[0];
         org.junit.Assert.assertArrayEquals(posconstant, ArrayOps.perform3PtSmoothing(posconstant), EPSILON);
         posconstant[10] = 3.0;
         org.junit.Assert.assertArrayEquals(smooth, ArrayOps.perform3PtSmoothing(posconstant), EPSILON);
         
         org.junit.Assert.assertArrayEquals(testout, ArrayOps.perform3PtSmoothing(test1), EPSILON);
         org.junit.Assert.assertArrayEquals(testout, ArrayOps.perform3PtSmoothing(test2), EPSILON);
     }
     @Test
     public void testFindZeroCrossing() {
         double[] test1 = null;
         double[] test2 = new double[0];
         org.junit.Assert.assertEquals(9,ArrayOps.findZeroCrossing(polysin, 12, 0));
         org.junit.Assert.assertEquals(84,ArrayOps.findZeroCrossing(polysin, 84, 99));
         org.junit.Assert.assertEquals(-1,ArrayOps.findZeroCrossing(posconstant, 80, 0));
         org.junit.Assert.assertEquals(-1,ArrayOps.findZeroCrossing(posconstant, 20, 1));
         
         org.junit.Assert.assertEquals(-2,ArrayOps.findZeroCrossing(test1, 80, 0));
         org.junit.Assert.assertEquals(-2,ArrayOps.findZeroCrossing(test2, 80, 0));
         org.junit.Assert.assertEquals(-2,ArrayOps.findZeroCrossing(posconstant, -80, 0));
         org.junit.Assert.assertEquals(-2,ArrayOps.findZeroCrossing(posconstant, 800, 0));
     }
     @Test
     public void testCorrectForZeroInitialEstimate(){
         double[] test = new double[LENGTH];
         double[] fix = new double[LENGTH];
         for (int i=0; i<LENGTH; i++) {
             if (i < 10) {
                 test[i] = posconstant[i] * -1;
                 fix[i] = test[i] + 2.0;
             } else {
                 test[i] = posconstant[i];
                 fix[i] = test[i] + 2.0;
             }
         }
         ArrayOps.correctForZeroInitialEstimate( test, 12 );
         org.junit.Assert.assertArrayEquals(test,fix,SM_EPSILON);
         System.arraycopy(posconstant, 0, test, 0, LENGTH);
         ArrayOps.correctForZeroInitialEstimate( test, 12 );
         org.junit.Assert.assertArrayEquals(test,posconstant,SM_EPSILON);
     }
     @Test
     public void testRemoveTrendWithBestFit() {
         double[] test1 = null;
         double[] test2 = new double[0];
         double[] fit = new double[LENGTH];
         org.junit.Assert.assertEquals(-1, ArrayOps.removeTrendWithBestFit(test1, 2.0));
         org.junit.Assert.assertEquals(-1, ArrayOps.removeTrendWithBestFit(test2, 2.0));
         org.junit.Assert.assertEquals(-1, ArrayOps.removeTrendWithBestFit(linecentered, 0.0));
         
         System.arraycopy(linecentered, 0, fit, 0, LENGTH);
         org.junit.Assert.assertEquals(1, ArrayOps.removeTrendWithBestFit(fit, STEP));
         org.junit.Assert.assertArrayEquals(fit,zeroconstant,EPSILON);
         
         System.arraycopy(poly, 0, fit, 0, LENGTH);
         org.junit.Assert.assertEquals(2, ArrayOps.removeTrendWithBestFit(fit, STEP));
         org.junit.Assert.assertArrayEquals(fit,zeroconstant,EPSILON);
     }
     @Test
     public void testFindLinearTrend() {
         double[] test1 = null;
         double[] test2 = new double[0];
         double[] errreturn = new double[0];
         org.junit.Assert.assertArrayEquals(errreturn, ArrayOps.findLinearTrend(test1, 2.0),EPSILON);
         org.junit.Assert.assertArrayEquals(errreturn, ArrayOps.findLinearTrend(test2, 2.0),EPSILON);
         org.junit.Assert.assertArrayEquals(errreturn, ArrayOps.findLinearTrend(linecentered, 0.0),EPSILON);
         org.junit.Assert.assertArrayEquals(ArrayOps.findLinearTrend(polyline,STEP),linepos,EPSILON);
     }
     @Test
     public void testCentralDiff() {
         double[] empty = new double[0];
         org.junit.Assert.assertArrayEquals(accelForDiff, ArrayOps.centralDiff(velForDiff, 0.01, 5) ,SM_EPSILON);
         org.junit.Assert.assertArrayEquals(empty, ArrayOps.centralDiff(velForDiff, 0.01, 6) ,SM_EPSILON);
     }
     @Test
     public void testCompatibility() {
         org.junit.Assert.assertArrayEquals(ArrayOps.integrate(accel,0.005, 0.0007705),vel,SM_EPSILON);
         org.junit.Assert.assertArrayEquals(ArrayOps.integrate(vel,0.005, 0.00),disp,SM_EPSILON);
     }
}
