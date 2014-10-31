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
import static SmConstants.VFileConstants.*;
import SmConstants.VFileConstants.EventOnsetType;
import SmConstants.VFileConstants.MagnitudeType;
import static SmConstants.VFileConstants.MagnitudeType.MOMENT;
import static SmConstants.VFileConstants.MagnitudeType.M_LOCAL;
import static SmConstants.VFileConstants.MagnitudeType.M_OTHER;
import static SmConstants.VFileConstants.MagnitudeType.SURFACE;
import SmConstants.VFileConstants.V2DataType;
import SmException.SmException;
import SmUtilities.ConfigReader;
import static SmUtilities.SmConfigConstants.*;
import SmUtilities.SmErrorLogger;
import SmUtilities.SmTimeFormatter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author jmjones
 */
public class V2Process {
    //need 3 sets of these params, for each data type
    private double[] accel;
    private double ApeakVal;
    private int ApeakIndex;
    private double AavgVal;
    private final int acc_unit_code;
    private final String acc_units;
    
    private double[] velocity;
    private double VpeakVal;
    private int VpeakIndex;
    private double VavgVal;
    private final int vel_unit_code;
    private final String vel_units;
    
    private double[] displace;
    private double DpeakVal;
    private int DpeakIndex;
    private double DavgVal;
    private final int dis_unit_code;
    private final String dis_units;
    
    private final V1Component inV1;
    private final int data_unit_code;
    private final double dtime;
    private final double samplerate;
    private final double noRealVal;
    private final double lowcutoff;
    private final double highcutoff;
    private double lowcutadj;
    private double highcutadj;
    private double magnitude;
    private MagnitudeType magtype;
    
    private int pickIndex;
    private int startIndex;
    private double ebuffer;
    private EventOnsetType emethod;
    private final int numpoles;  // the filter order is 2*numpoles
    private double taperlength;
    
    private final double qavelocityinit;
    private final double qavelocityend;
    private final double qadisplacend;
    private V2Status procStatus;
    
    private ArrayList<String> errorlog;
    private boolean writeArrays;
    private SmErrorLogger elog;
    private String[] logstart;
    private final File V0name;
        
    public V2Process(final V1Component v1rec, File inName) throws SmException {
        double epsilon = 0.0001;
        this.inV1 = v1rec;
        this.lowcutadj = 0.0;
        this.highcutadj = 0.0;
        errorlog = new ArrayList<>();
        elog = SmErrorLogger.INSTANCE;
        ConfigReader config = ConfigReader.INSTANCE;
        writeArrays = false;
        this.V0name = inName;
        
        //Get config values to cm/sec2 (acc), cm/sec (vel), cm (dis)
        this.acc_unit_code = CMSQSECN;
        this.vel_unit_code = CMSECN;
        this.dis_unit_code = CMN;
        
        this.acc_units = CMSQSECT;
        this.vel_units = CMSECT;
        this.dis_units = CMT;
        this.pickIndex = 0;
        this.startIndex = 0;
        this.procStatus = V2Status.NOEVENT;
        
        SmTimeFormatter timer = new SmTimeFormatter();
        String logtime = timer.getGMTdateTime();
        logstart = new String[2];
        logstart[0] = "\n";
        logstart[1] = "Prism Error/Debug Log Entry: " + logtime;
        
        this.noRealVal = inV1.getNoRealVal();
        //verify that real header value delta t is defined and valid
        double delta_t = inV1.getRealHeaderValue(DELTA_T);
        if ((Math.abs(delta_t - noRealVal) < epsilon) || (delta_t < 0.0)){
            throw new SmException("Real header #62, delta t, is invalid: " + 
                                                                        delta_t);
        }
        boolean match = false;
        dtime = delta_t * MSEC_TO_SEC;    
        samplerate = 1.0 / dtime;
        for (double each : V3_SAMPLING_RATES) {
            if (Math.abs(each - samplerate) < epsilon) {
                match = true;
            }
        }
        if (!match) {
            throw new SmException("Real header #62, delta t value, " + 
                                        delta_t + " is out of expected range");
        }
        //Get the earthquake magnitude from the real header array.  The order of
        //precedence for magnitude values is MOMENT, LOCAL, SURFACE, OTHER.
        //If all values are undefined or invalid, flag as an error.
        this.magnitude = inV1.getRealHeaderValue(MOMENT_MAGNITUDE);
        if ((Math.abs(magnitude - noRealVal) < epsilon) || (magnitude < 0.0)){
            this.magnitude = inV1.getRealHeaderValue(LOCAL_MAGNITUDE);
            if ((Math.abs(magnitude - noRealVal) < epsilon) || (magnitude < 0.0)){
                this.magnitude = inV1.getRealHeaderValue(SURFACE_MAGNITUDE);
                if ((Math.abs(magnitude - noRealVal) < epsilon) || (magnitude < 0.0)){
                    this.magnitude = inV1.getRealHeaderValue(OTHER_MAGNITUDE);
                    if ((Math.abs(magnitude - noRealVal) < epsilon) || (magnitude < 0.0)){
                        throw new SmException("All earthquake magnitude real header values are invalid.");
                    } else {
                        this.magtype = M_OTHER;
                    }
                } else {
                    this.magtype = SURFACE;
                }
            } else {
                this.magtype = M_LOCAL;
            }
        } else {
            this.magtype = MOMENT;
        }
        
        try {
            String unitcode = config.getConfigValue(DATA_UNITS_CODE);
            this.data_unit_code = (unitcode == null) ? CMSQSECN : Integer.parseInt(unitcode);

            String lowcut = config.getConfigValue(BP_FILTER_CUTOFFLOW);
            this.lowcutoff = (lowcut == null) ? DEFAULT_LOWCUT : Double.parseDouble(lowcut);

            String highcut = config.getConfigValue(BP_FILTER_CUTOFFHIGH);
            this.highcutoff = (highcut == null) ? DEFAULT_HIGHCUT : Double.parseDouble(highcut);

            //The Butterworth filter implementation requires an even number of poles (and order)
            String filorder = config.getConfigValue(BP_FILTER_ORDER);
            this.numpoles = (filorder == null) ? DEFAULT_NUM_POLES : Integer.parseInt(filorder)/2;

            //The Butterworth filter taper length for the half cosine taper
            String taplen = config.getConfigValue(BP_TAPER_LENGTH);
            this.taperlength = (taplen == null) ? DEFAULT_TAPER_LENGTH : Double.parseDouble(taplen);
            
            String pbuf = config.getConfigValue(EVENT_ONSET_BUFFER);
            this.ebuffer = (pbuf == null) ? DEFAULT_EVENT_ONSET_BUFFER : Double.parseDouble(pbuf);
            
            String eventmethod = config.getConfigValue(EVENT_ONSET_METHOD);
            if (eventmethod == null) {
                this.emethod = DEFAULT_EVENT_ONSET_METHOD;
            } else if (eventmethod.equalsIgnoreCase("AIC")) {
                this.emethod = EventOnsetType.AIC;
            } else {
                this.emethod = EventOnsetType.DE;
            }
            
            String qainitvel = config.getConfigValue(QA_INITIAL_VELOCITY);
            this.qavelocityinit = (qainitvel == null) ? DEFAULT_QA_INITIAL_VELOCITY : Double.parseDouble(qainitvel);
            
            String qaendvel = config.getConfigValue(QA_RESIDUAL_VELOCITY);
            this.qavelocityend = (qaendvel == null) ? DEFAULT_QA_RESIDUAL_VELOCITY : Double.parseDouble(qaendvel);
            
            String qaenddis = config.getConfigValue(QA_RESIDUAL_DISPLACE);
            this.qadisplacend = (qaenddis == null) ? DEFAULT_QA_RESIDUAL_DISPLACE : Double.parseDouble(qaenddis);
            
            String debugon = config.getConfigValue(DEBUG_ARRAY_WRITE);
            this.writeArrays = debugon.equalsIgnoreCase(DEBUG_ARRAY_WRITE_ON);
            
        } catch (NumberFormatException err) {
            throw new SmException("Error extracting numeric values from configuration file");
        }
        this.ebuffer = (this.ebuffer < 0.0) ? DEFAULT_EVENT_ONSET_BUFFER : this.ebuffer;
        this.taperlength = (this.taperlength < 0.0) ? DEFAULT_TAPER_LENGTH : this.taperlength;
    }
    
    public V2Status processV2Data() throws SmException, IOException {  
        int dislen;
        double velstart;
        double velend;
        double disend = 999.0;
        boolean success = false;
        
        double[] accraw = new double[0];
        //save a copy of the original array for pre-mean removal
        double[] V1Array = inV1.getDataArray();
        //Check for units of g and adjust before proceeding.
        if (data_unit_code == CMSQSECN) {
            accraw = new double[V1Array.length];
            System.arraycopy( V1Array, 0, accraw, 0, V1Array.length);
        } else if (data_unit_code == GLN) {
            accraw = ArrayOps.convertArrayUnits(V1Array, FROM_G_CONVERSION);
        } else {
            throw new SmException("V1 file units are unsupported for processing");
        }
        double[] acc = new double[accraw.length];
        System.arraycopy( accraw, 0, acc, 0, accraw.length);
        
        //Pick P-wave and remove baseline
        errorlog.add("Start of V2 processing for " + V0name.toString());
        //remove linear trend before finding event onset
        errorlog.add(String.format("time per sample in sec %f",dtime));
        errorlog.add(String.format("sample rate (samp/sec): %f",samplerate));
        errorlog.add("Event detection: remove linear trend, filter, event onset detection");
        
        ArrayOps.removeLinearTrend( acc, dtime);
        
        //set up the filter coefficients and run
        ButterworthFilter filter = new ButterworthFilter();
        boolean valid = filter.calculateCoefficients(lowcutoff, highcutoff, 
                                                        dtime, numpoles, true);
        if (valid) {
            filter.applyFilter(acc, 0);  //filtered values are returned in acc
        } else {
            throw new SmException("Invalid bandpass filter input parameters");
        }
//        System.out.println("no pick index on filter");
        
//        errorlog.add("   f1: " + lowcutoff + " f2: " + highcutoff + " numpoles: " + numpoles);
//        double[] b1 = filter.getB1();
//        double[] b2 = filter.getB2();
//        double[] fact = filter.getFact();
//        for (int jj = 0; jj < b1.length; jj++) {
//            errorlog.add(String.format("   fact: %f  b1: %f  b2: %f%n", fact[jj],b1[jj],b2[jj]));
//        }
        //Find the start of the wave
        if (emethod == EventOnsetType.DE) {
            EventOnsetDetection depick = new EventOnsetDetection( dtime );
            pickIndex = depick.findEventOnset(acc);
            startIndex = depick.applyBuffer(ebuffer);
            errorlog.add("Event Detection algorithm: damping energy method");
        } else {
            AICEventDetect aicpick = new AICEventDetect();
            pickIndex = aicpick.calculateIndex(acc, "ToPeak");
            startIndex = aicpick.applyBuffer(ebuffer, dtime);
            errorlog.add("Event Detection algorithm: modified Akaike Information Criterion");
        }
        errorlog.add(String.format("pick index: %d,  pick buffer: %f,  start index: %d",
                                                pickIndex,ebuffer,startIndex));
        errorlog.add(String.format("pick time in seconds: %f, buffered time: %f",
                                          (pickIndex*dtime),(startIndex*dtime)));
        System.out.println(String.format("pick index: %d,  pick buffer: %f,  start index: %d",
                                                pickIndex,ebuffer,startIndex));


        if (pickIndex <= 0) {
            //No pick index detected, so skip all V2 processing
            procStatus  = V2Status.NOEVENT;
            System.out.println("V2process: exit staus = " + procStatus);
            return procStatus;
        }
        //Remove pre-event mean from acceleration record
        if (startIndex > 0) {
            double[] subset = Arrays.copyOfRange( accraw, 0, startIndex );
            ArrayStats accsub = new ArrayStats( subset );
            ArrayOps.removeValue(accraw, accsub.getMean());
            errorlog.add("Pre-event mean removed from uncorrected acceleration");
        }
        else {
            ArrayStats accmean = new ArrayStats( accraw );
            ArrayOps.removeValue(accraw, accmean.getMean());
            errorlog.add("Full array mean removed from uncorrected acceleration");
        }

        if (writeArrays) {
            elog.writeOutArray(accraw, "initialBaselineCorrection.txt");
        } 

        //Integrate the acceleration to get velocity.
        velocity = ArrayOps.Integrate( accraw, dtime);
        errorlog.add("acceleration integrated to velocity (trapezoidal method)");
        int vellen = velocity.length;
        if (writeArrays) {
           elog.writeOutArray(velocity, "afterIntegrationToVel.txt");
        }
        //Remove any linear trend from velocity
        ArrayOps.removeLinearTrend( velocity, dtime);
        errorlog.add("linear trend removed from velocity");
        if (writeArrays) {
           elog.writeOutArray(velocity, "LinearTrendRemovedVel.txt");
        }
        //Update Butterworth filter low and high cutoff thresholds for later
        FilterCutOffThresholds threshold = new FilterCutOffThresholds( magnitude );
        lowcutadj = threshold.getLowCutOff();
        highcutadj = threshold.getHighCutOff();

        //perform first QA check on velocity, check first and last sections of
        //velocity array - should be close to 0.0 with tolerances.  If not,
        //perform adaptive baseline correction.
//        int window5sec = (int)(5.0 / dtime);
        ///////////////////////////////
        //
        // First QC Test
        //
        ///////////////////////////////
        int window = (int)(pickIndex * 0.25);
        vellen = velocity.length;
        int velwindowstart = ArrayOps.findZeroCrossing(velocity, window, 1);
        int velwindowend = ArrayOps.findZeroCrossing(velocity, vellen-window, 0);
        if (startIndex > 0) {
            velstart = ArrayOps.findSubsetMean(velocity, 0, velwindowstart);
            velend = ArrayOps.findSubsetMean(velocity, velwindowend, vellen);
        } else {
            velstart = velocity[0];
            velend = velocity[vellen-1];
        }
        if ((Math.abs(velstart) > qavelocityinit) || 
                                         (Math.abs(velend) > qavelocityend)){
            errorlog.add("Velocity QA failed:");
            errorlog.add(String.format("   initial velocity: %f,  limit %f",
                                        Math.abs(velstart), qavelocityinit));
            errorlog.add(String.format("   final velocity: %f,  limit %f",
                                             Math.abs(velend), qavelocityend));
            errorlog.add("Adapive baseline correction beginning");
            System.out.println("failed QA1");
        ///////////////////////////////
        //
        // Adaptive Baseline Correction
        //
        ///////////////////////////////
            AdaptiveBaselineCorrection adapt = new AdaptiveBaselineCorrection(
                        dtime,velocity,lowcutadj,highcutadj,numpoles,pickIndex);
            procStatus = adapt.startIterations();
            
            //If unable to perform any iterations in ABC, just exit with no V2
            if (procStatus == V2Status.NOABC) {
                System.out.println("V2process: exit staus = " + procStatus);
                return procStatus;
            }
            int solution = adapt.getSolution();
            double[] parms = adapt.getSolutionParms(solution);
            velstart = parms[2];
            velend = parms[3];
            disend = parms[1];
            accel = adapt.getABCacceleration();
            velocity = adapt.getABCvelocity();
            displace = adapt.getABCdisplacement();
            adapt.clearParamsArray();
        } else {
            //determine new filter coefs based on earthquake magnitude
            filter = new ButterworthFilter();
            errorlog.add("Acausal bandpass filter:");
            errorlog.add("  earthquake magnitude is " + magnitude + " and M used is " + magtype);
            errorlog.add(String.format("  adjusted lowcut: %f and adjusted highcut: %f Hz",
                                                            lowcutadj, highcutadj));
            valid = filter.calculateCoefficients(lowcutadj, highcutadj, 
                                                dtime, DEFAULT_NUM_POLES, true);
            if (valid) {
                filter.applyFilter(velocity, pickIndex);
            } else {
                throw new SmException("Invalid bandpass filter calculated parameters");
            }
            if (writeArrays) {
               elog.writeOutArray(velocity, "finalV2velocity.txt");
            }
           //Integrate the velocity to get displacement.
            displace = ArrayOps.Integrate( velocity, dtime);
            errorlog.add("Velocity integrated to displacement (trapezoidal method)");

            //Differentiate velocity for final acceleration
            accel = ArrayOps.Differentiate(velocity, dtime);
            errorlog.add("Velocity differentiated to corrected acceleration");

            //perform second QA check on velocity and displacement, check first and 
            //last values of velocity array and last value of displacement array. 
            //They should be close to 0.0 with tolerances.  If not, flag as
            //needing additional processing.

            dislen = displace.length;
            velwindowstart = ArrayOps.findZeroCrossing(velocity, window, 1);
            velwindowend = ArrayOps.findZeroCrossing(velocity, vellen-window, 0);
            int diswindowend = ArrayOps.findZeroCrossing(displace, dislen-window, 0);
            if (startIndex > 0) {
                velstart = ArrayOps.findSubsetMean(velocity, 0, velwindowstart);
                velend = ArrayOps.findSubsetMean(velocity, velwindowend, vellen);
                disend = ArrayOps.findSubsetMean(displace, diswindowend, dislen);
            } else {
                velstart = velocity[0];
                velend = velocity[vellen-1];
                disend = displace[dislen-1];
            }
        ///////////////////////////////
        //
        // Second QC Test (also performed in ABC)
        //
        ///////////////////////////////
            success = (Math.abs(velstart) <= qavelocityinit) && 
                                (Math.abs(velend) <= qavelocityend) && 
                                              (Math.abs(disend) <= qadisplacend);
            procStatus = (success) ? V2Status.GOOD : V2Status.FAILQC;
        }
        if (procStatus == V2Status.FAILQC) {
            errorlog.add("Final QA failed - V2 processing unsuccessful:");
            errorlog.add(String.format("   initial velocity: %f, limit %f",
                                        Math.abs(velstart), qavelocityinit));
            errorlog.add(String.format("   final velocity: %f, limit %f",
                                  Math.abs(velend), qavelocityend));
            errorlog.add(String.format("   final displacement,: %f, limit %f",
                                  Math.abs(disend), qadisplacend));
            elog.writeToLog(logstart);
            String[] errorout = new String[errorlog.size()];
            errorout = errorlog.toArray(errorout);
            elog.writeToLog(errorout);
            errorlog.clear();
            System.out.println("failed QA2");
        }
        //calculate final array params for headers
        ArrayStats statVel = new ArrayStats( velocity );
        VpeakVal = statVel.getPeakVal();
        VpeakIndex = statVel.getPeakValIndex();
        VavgVal = statVel.getMean();

        ArrayStats statDis = new ArrayStats( displace );
        DpeakVal = statDis.getPeakVal();
        DpeakIndex = statDis.getPeakValIndex();
        DavgVal = statDis.getMean();

        ArrayStats statAcc = new ArrayStats( accel );
        ApeakVal = statAcc.getPeakVal();
        ApeakIndex = statAcc.getPeakValIndex();
        AavgVal = statAcc.getMean();

        System.out.println("V2process: exit staus = " + procStatus);
        return procStatus;
    }
    public double getPeakVal(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.ApeakVal;
        } else if (dType == V2DataType.VEL) {
            return this.VpeakVal;
        } else {
            return this.DpeakVal;
        }
    }
    public int getPeakIndex(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.ApeakIndex;
        } else if (dType == V2DataType.VEL) {
            return this.VpeakIndex;
        } else {
            return this.DpeakIndex;
        }
    }
    public double getAvgVal(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.AavgVal;
        } else if (dType == V2DataType.VEL) {
            return this.VavgVal;
        } else {
            return this.DavgVal;
        }
    }
    public double[] getV2Array(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.accel;
        } else if (dType == V2DataType.VEL) {
            return this.velocity;
        } else {
            return this.displace;
        }
    }
    public int getV2ArrayLength(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.accel.length;
        } else if (dType == V2DataType.VEL) {
            return this.velocity.length;
        } else {
            return this.displace.length;
        }
    }
    public int getDataUnitCode(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.acc_unit_code;
        } else if (dType == V2DataType.VEL) {
            return this.vel_unit_code;
        } else {
            return this.dis_unit_code;
        }
    }
    public String getDataUnits(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.acc_units;
        } else if (dType == V2DataType.VEL) {
            return this.vel_units;
        } else {
            return this.dis_units;
        }
    }
    public double getLowCut() {
        return this.lowcutadj;
    }
    public double getHighCut() {
        return this.highcutadj;
    }
    public V2Status getQCStatus() {
        return this.procStatus;
    }
    public int getPickIndex() {
        return this.pickIndex;
    }
    public int getStartIndex() {
        return this.startIndex;
    }
}
