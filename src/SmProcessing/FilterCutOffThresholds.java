/*******************************************************************************
 * Name: Java class FilterCutOffThresholds.java
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

package SmProcessing;

import SmConstants.VFileConstants.MagnitudeType;
import static SmConstants.VFileConstants.MagnitudeType.MOMENT;
import static SmConstants.VFileConstants.MagnitudeType.M_LOCAL;
import static SmConstants.VFileConstants.MagnitudeType.M_OTHER;
import static SmConstants.VFileConstants.MagnitudeType.SURFACE;
/**
 * This class chooses the Butterworth filter cutoff thresholds to use, based on
 * the magnitude of the earthquake.  It also determines which magnitude from
 * the COSMOS header to use, based on which value is defined in the header.  The
 * order of selection is moment, local, surface, and other.  The header values
 * are determined to be defined if they are not equal to the Real Header NoData
 * value and are non-negative.
 * @author jmjones
 */
public class FilterCutOffThresholds {
    private final double epsilon = 0.001;
    private double f1; //low cutoff
    private double f2; //high cutoff
    private double ML;

    private final double low = 3.5;
    private final double mid = 4.5;
    private final double high = 5.5;
/**
 * Constructor just initializes the thresholds and magnitude to 0.
 */
    public FilterCutOffThresholds() {
        this.ML = 0.0;
        this.f1 = 0.0;
        this.f2 = 0.0;
    }
    /**
     * Determines the high and low filter cutoff thresholds to use based on
     * the earthquake magnitude.  There are 4 earthquake values that may be
     * defined in the COSMOS header, and the order for selection is moment,
     * local, surface, and other.  If no earthquake parameters are defined, the
     * return value will be set to INVALID.  Earthquake parameters are considered
     * valid if they are greater than zero and not equal to the COSMOS nodata
     * value, given in the noval parameter.  A typical COSMOS nodata value is
     * -999.99.  To use this class with an already validated magnitude, just set
     * the first value to the validated magnitude and set all other input 
     * parameters to -999.99.
     * @param mm moment magnitude from the COSMOS header
     * @param lm local magnitude from the COSMOS header
     * @param sm surface magnitude from the COSMOS header
     * @param om other magnitude from the COSMOS header
     * @param noval No Data value for the COSMOS real header
     * @return the magnitude type that was used to select the high and low 
     * cutoff values, or INVALID if no earthquake values have been defined.
     */
    public MagnitudeType SelectMagAndThresholds( double mm, double lm, double sm,
                                                         double om, double noval) {
        MagnitudeType magtype;
        if ((Math.abs(mm - noval) < epsilon) || (mm < 0.0)){
            if ((Math.abs(lm - noval) < epsilon) || (lm < 0.0)){
                if ((Math.abs(sm - noval) < epsilon) || (sm < 0.0)){
                    if ((Math.abs(om - noval) < epsilon) || (om < 0.0)){
                        magtype = MagnitudeType.INVALID;
                        return magtype;
                    } else {
                        magtype = M_OTHER;
                        ML = om;
                    }
                } else {
                    magtype = SURFACE;
                    ML = sm;
                }
            } else {
                magtype = M_LOCAL;
                ML = lm;
            }
        } else {
            magtype = MOMENT;
            ML = mm;
        }
        if ((ML > high) || (Math.abs(ML - high) < epsilon)){
            f1 = 0.1;
            f2 = 40.0;
        } else if ((ML > mid) || (Math.abs(ML - mid) < epsilon)) {
            f1 = 0.3;
            f2 = 35.0;
        } else if ((ML > low) || (Math.abs(ML - low) < epsilon)) {
            f1 = 0.3;
            f2 = 35.0;
        } else {  //ML < low
            f1 = 0.5;
            f2 = 25.0;
        }
        return magtype;
    }
    /**
     * Getter for the filter low cutoff threshold
     * @return the low cutoff threshold
     */
    public double getLowCutOff() {
        return f1;
    }
    /**
     * Getter for the filter high cutoff threshold
     * @return the high cutoff threshold
     */
    public double getHighCutOff() {
        return f2;
    }
    /**
     * Getter for the earthquake magnitude used for selection
     * @return the earthquake magnitude
     */
    public double getMagnitude() {
        return ML;
    }
}
