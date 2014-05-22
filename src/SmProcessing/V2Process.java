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

import COSMOSformat.V1Component;
import static COSMOSformat.VFileConstants.DELTA_T;
import SmException.SmException;
import SmUtilities.ConfigReader;
import static SmUtilities.SmConfigConstants.DATA_UNITS_CODE;

/**
 *
 * @author jmjones
 */
public class V2Process {
    //need 3 sets of these params, for each data type
    private double[] accel;
    private double AmaxVal;
    private int AmaxIndex;
    private double AavgVal;
    
    private double[] velocity;
    private double VmaxVal;
    private int VmaxIndex;
    private double VavgVal;
    
    private double[] displace;
    private double DmaxVal;
    private int DmaxIndex;
    private double DavgVal;
    
    private final V1Component inV1;
    private final int data_unit_code;
    private DataVals result;
    private final double delta_t;
    private final double noRealVal;
    
    public V2Process(final V1Component v1rec, final ConfigReader config) throws SmException {
        double epsilon = 0.0001;
        double nodata = v1rec.getNoRealVal();
        this.inV1 = v1rec;
        
        //Get config values with a default of cm/sec2 if not defined
        String unitcode = config.getConfigValue(DATA_UNITS_CODE);
        this.data_unit_code = (unitcode == null) ? 4 : Integer.parseInt(unitcode);
        
        this.noRealVal = inV1.getNoRealVal();
        //verify that real header value delta t is defined and valid
        delta_t = inV1.getRealHeaderValue(DELTA_T);
        if (((delta_t - noRealVal) < epsilon) || (delta_t < 0.0)){
            throw new SmException("Real header #62, delta t, is invalid: " + 
                                                                        delta_t);
        }        
    }
    
    public void processV2Data() {        
        //Integrate the V1 acceleration to get velocity.  Scale results for
        //correct units if necessary.!!!
        result = SmIntegrate( inV1.getDataArray(), delta_t);
        velocity = result.array;
        VmaxVal = result.max;
        VmaxIndex = result.maxIndex;
        VavgVal = result.mean;
        
        //Integrate the velcity to get displacement.
        result = SmIntegrate( velocity, delta_t);
        displace = result.array;
        DmaxVal = result.max;
        DmaxIndex = result.maxIndex;
        DavgVal = result.mean;

        //Differentiate the velocity to get corrected acceleration.
        result = SmDifferentiate( velocity, delta_t);
        accel = result.array;
        AmaxVal = result.max;
        AmaxIndex = result.maxIndex;
        AavgVal = result.mean;    
    }
    private DataVals SmIntegrate( final double[] inArray, final double deltat ){
        double max  = 0.0;
        double mean = 0.0;
        int index = 0;
        double total = 0.0;
        double[] result = new double[ inArray.length ];
        double dt2 = deltat / 2.0;
        result[0] = 0.0;
        for (int i = 1; i < result.length; i++) {
            result[i] = result[i-1] + (inArray[i-1] + inArray[i])*dt2;
            total = total + result[i];
            if (result[i] > max){
                max = result[i];
                index = i;
            }
        }
        mean = total / result.length;
        return (new DataVals(result, mean, max, index ));
    }
    private DataVals SmDifferentiate( final double[] inArray, final double deltat ){
        int len = inArray.length;
        double[] result = new double[ len ];
        result[0] = (inArray[1] - inArray[0]) / deltat;
        double max  = result[0];
        double mean = result[0];
        int index = 0;
        double total = result[0];
        for (int i = 1; i < len-2; i++) {
            result[i] = (inArray[i+1] - inArray[i-1]) / (deltat * 2.0);
            total = total + result[i];
            if (result[i] > max){
                max = result[i];
                index = i;
            }
        }
        result[len-1] = (inArray[len-1] - inArray[len-2]) / deltat;
        total = total + result[len-1];
        if (result[len-1] > max) {
            max = result[len-1];
            index = len-1;
            
        }
        mean = total / result.length;
        return (new DataVals(result, mean, max, index ));
    }
    class DataVals {
        public final double[] array;
        public final double max;
        public final double mean;
        public final int maxIndex;

        public DataVals(double[] inArray, double inMean, double inMax, int inIndex) {
            max = inMax;
            mean = inMean;
            maxIndex = inIndex;
            array = inArray;
        }
    }
}
