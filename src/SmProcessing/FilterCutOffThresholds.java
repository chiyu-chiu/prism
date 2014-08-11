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

/**
 *
 * @author jmjones
 */
public class FilterCutOffThresholds {
        private final double epsilon = 0.001;
        private double f1; //low cutoff
        private double f2; //high cutoff
        private final double ML;
        
        private final double low = 3.5;
        private final double mid = 4.5;
        private final double high = 5.5;
    
    public FilterCutOffThresholds( double MomentMag ) {
        this.ML = MomentMag;
        if ((ML > high) || (Math.abs(ML - high) < epsilon)){
            f1 = 0.1;
            f2 = 40.0;
        } else if ((ML > mid) || (Math.abs(ML - mid) < epsilon)) {
            f1 = 0.2;
            f2 = 35.0;
        } else if ((ML > low) || (Math.abs(ML - low) < epsilon)) {
            f1 = 0.3;
            f2 = 35.0;
        } else {  //ML < low
            f1 = 0.5;
            f2 = 25.0;
        }
    }
    public double getLowCutOff() {
        return f1;
    }
    public double getHighCutOff() {
        return f2;
    }
    public double getMomentMag() {
        return ML;
    }
}
