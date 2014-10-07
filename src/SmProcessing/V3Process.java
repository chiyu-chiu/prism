/*
 * Copyright (C) 2014 jmjones
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package SmProcessing;

import COSMOSformat.V2Component;
import static SmConstants.VFileConstants.DELTA_T;
import static SmConstants.VFileConstants.MSEC_TO_SEC;
import static SmConstants.VFileConstants.NUM_T_PERIODS;
import static SmConstants.VFileConstants.V3_DAMPING_VALUES;
import SmException.FormatException;
import SmException.SmException;
import SmUtilities.SmErrorLogger;
import java.io.IOException;
import java.util.ArrayList;
/**
 *
 * @author jmjones
 */
public class V3Process {
    private final double EPSILON = 0.0001;
    private ArrayList<double[]> V3Data;
    private double[][][] spectra;
    private double[] T_periods;
    private double dtime;
    private double samplerate;
    private final double noRealVal;
    private double[] velocity;
    private double[] accel;
    private SpectraResources spec;
    private SmErrorLogger elog;
    private boolean writeArrays;
    
    public V3Process(final V2Component v2acc, final V2Component v2vel,
                      final V2Component v2dis) throws IOException, SmException, 
                                                                FormatException {

        this.elog = SmErrorLogger.INSTANCE;
        writeArrays = true;
        this.velocity = v2vel.getDataArray();
        this.accel = v2acc.getDataArray();
        this.V3Data = new ArrayList<>();
        this.noRealVal = v2vel.getNoRealVal();
        double delta_t = v2vel.getRealHeaderValue(DELTA_T);
        if ((Math.abs(delta_t - noRealVal) < EPSILON) || (delta_t < 0.0)){
            throw new SmException("Real header #62, delta t, is invalid: " + 
                                                                       delta_t);
        }
        //Get the periods to compute spectra and coeficients for the input
        //sampling interval.
        dtime = delta_t * MSEC_TO_SEC;  
        samplerate = 1.0 / dtime;
        spectra = new double[V3_DAMPING_VALUES.length][][];
        spec = new SpectraResources();
        T_periods = spec.getTperiods();
        for (int i = 0; i < V3_DAMPING_VALUES.length; i++) {
            spectra[i] = spec.getCoefArray(samplerate, V3_DAMPING_VALUES[i]);
        }
        //Add the T-periods to the V3 data list
        V3Data.add(T_periods);
    }
    
    public void processV3Data() {        
        //Calculate FFT for the velocity array.  Select magnitudes for the
        //given T values only.  (freq = 1/T)
        double pval;
        double interpolated;
        double dindex;
        int ulim;
        int llim;
        double uval;
        double lval;
        double scale;
        System.out.println("V3 process");
//        System.out.println("velocity length: " + velocity.length);
        FFourierTransform fft = new FFourierTransform();
        double[] velspec = fft.calculateFFT(velocity);
        double delta_f = 1.0 / (fft.getPowerLength() * dtime);
        
//        System.out.println("V3: powerlength= " + fft.getPowerLength());
//        System.out.println("return fft length: " + velspec.length);
//        System.out.println("delta_t = " + delta_t);
//        System.out.println("delta_f = " + delta_f);
        if (writeArrays) {
            elog.writeOutArray(velspec, "V3velocityFFT.txt");
        } 
        
        double[] velfftvals = new double[NUM_T_PERIODS];
        int ctr = 0;
        for (int f = NUM_T_PERIODS-1; f >=0; f--) {
            for (int arr = ctr; arr < velspec.length; arr++) {
                if ((arr*delta_f > (1.0/T_periods[f])) || 
                        (Math.abs(arr*delta_f - (1.0/T_periods[f])) < EPSILON)) {
                    ctr = arr;
                    break;
                }
            }
            if (ctr == 0) {
                velfftvals[f] = velspec[ctr];
            } else if (Math.abs(ctr*delta_f - (1.0/T_periods[f])) < EPSILON){
                velfftvals[f] = velspec[ctr];
            } else {
                ulim = ctr;
                llim = ctr - 1;
                uval = velspec[ulim];
                lval = velspec[llim];
                scale = ((1.0/T_periods[f])-(llim*delta_f)) /(delta_f*(ulim-llim));
                velfftvals[f] = lval + scale * (uval - lval);
            }
        }
        V3Data.add(velfftvals);
        
        //Calculate the spectra for each damping value
        double omega;
        int len = accel.length;
        double[] sd;
        double[] sv;
        double[] sa;
        double coef_a; double coef_b;
        double coef_c; double coef_d;
        double coef_e; double coef_f;
        
        for (int d = 0; d < V3_DAMPING_VALUES.length; d++) {
            sd = new double[NUM_T_PERIODS];
            sv = new double[NUM_T_PERIODS];
            sa = new double[NUM_T_PERIODS];

            for (int p = 0; p < T_periods.length; p++) {
                coef_a = spectra[d][p][0];
                coef_b = spectra[d][p][1];
                coef_c = spectra[d][p][2];
                coef_d = spectra[d][p][3];
                coef_e = spectra[d][p][4];
                coef_f = spectra[d][p][5];
                omega = (2.0 * Math.PI) / T_periods[p];
                double[][] y = new double[2][len];
                y[0][0] = 0.0;
                y[1][0] = 0.0;
                
                for(int k = 1; k < len; k++) {
                    y[0][k] = coef_a * y[0][k-1] + coef_b * y[1][k-1] + 
                                                              coef_e * accel[k];
                    y[1][k] = coef_c * y[0][k-1] + coef_d * y[1][k-1] + 
                                                              coef_f * accel[k];
                }
        
                //Get the relative displacement (cm)
                double[] disp = y[0];
//                System.out.println("\ndamping: " + V3_DAMPING_VALUES[d]);
//                System.out.println("period: " + T_periods[p]);
//                System.out.println("a:" +  coef_a + " b: " + coef_b + " c: " + coef_c);
//                System.out.println("d:" +  coef_d + " e: " + coef_e + " f: " + coef_f);
//                SmErrorLogger elog = SmErrorLogger.INSTANCE;
//                elog.writeOutArray(disp, "y0inV3.txt");
                ArrayStats stat = new ArrayStats(disp);
                sd[p] = Math.abs(stat.getPeakVal());
                sv[p] = sd[p] * omega;
                sa[p] = sv[p] * omega;
            }
            V3Data.add(sd);
            V3Data.add(sv);
            V3Data.add(sa);
        }
    }
    public double[] getV3Array(int arrnum) {
        return V3Data.get(arrnum);
    }
    public int getV3ListLength() {
        return V3Data.size();
    }
}
