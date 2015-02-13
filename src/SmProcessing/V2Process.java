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
import SmConstants.VFileConstants.V2DataType;
import SmException.SmException;
import SmUtilities.ConfigReader;
import static SmUtilities.SmConfigConstants.*;
import SmUtilities.SmDebugLogger;
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
    
    private double initialVel;
    private double initialDis;
    
    private final V1Component inV1;
    private final int data_unit_code;
    private final double dtime;
    private final double samplerate;
    private final double noRealVal;
    private final double lowcutoff;
    private final double highcutoff;
    private double lowcutadj;
    private double highcutadj;
    private double mmag;
    private double lmag;
    private double smag;
    private double omag;
    private double magnitude;
    private MagnitudeType magtype;
    private BaselineType basetype;
    
    private int pickIndex;
    private int startIndex;
    private double ebuffer;
    private EventOnsetType emethod;
    private final int numpoles;  // the filter order is numpoles
    private double taperlength;
    private double preEventMean;
    private int trendRemovalOrder;
    private int calculated_taper;
    private boolean strongMotion;
    private boolean needsABC;
    private String logtime;
    
    private V2Status procStatus;
    private QCcheck qcchecker;
    
    private ArrayList<String> errorlog;
    private boolean writeDebug;
    private boolean writeBaseline;
    private SmDebugLogger elog;
    private String[] logstart;
    private final File V0name;
    private final String channel;
    private String eventID;
    private double QCvelinitial;
    private double QCvelresidual;
    private double QCdisresidual;
    private int ABCnumparams;
    private int ABCwinrank;
    private int ABCpoly1;
    private int ABCpoly2;
    private int ABCbreak1;
    private int ABCbreak2;
    
    private double bracketedDuration;
    private double AriasIntensity;
    private double HousnerIntensity;
    private double channelRMS;
    private double durationInterval;
    private double cumulativeAbsVelocity;
        
    public V2Process(final V1Component v1rec, File inName, String logtime) 
                                                            throws SmException {
        double epsilon = 0.0001;
        this.inV1 = v1rec;
        this.lowcutadj = 0.0;
        this.highcutadj = 0.0;
        errorlog = new ArrayList<>();
        elog = SmDebugLogger.INSTANCE;
        ConfigReader config = ConfigReader.INSTANCE;
        writeDebug = false;
        writeBaseline = false;
        this.V0name = inName;
        this.channel = inV1.getChannel();
        this.eventID = inV1.getEventID();
        this.logtime = logtime;
        basetype = BaselineType.SIMPLE;
        
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
        
        this.bracketedDuration = 0.0;
        this.AriasIntensity = 0.0;
        this.HousnerIntensity = 0.0;
        this.channelRMS = 0.0;
        this.durationInterval = 0.0;
        this.cumulativeAbsVelocity = 0.0;
        this.initialVel = 0.0;
        this.initialDis = 0.0;
        this.preEventMean = 0.0;
        this.trendRemovalOrder = 0;
        this.calculated_taper = 0;
        this.strongMotion = false;
        this.needsABC = false;
        this.QCvelinitial = 0.0;
        this.QCvelresidual = 0.0;
        this.QCdisresidual = 0.0;
        this.ABCnumparams = 0;
        this.ABCwinrank = 0;
        this.ABCpoly1 = 0;
        this.ABCpoly2 = 0;
        this.ABCbreak1 = 0;
        this.ABCbreak2 = 0;
        
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
        //Get the earthquake magnitude from the real header array.
        this.mmag = inV1.getRealHeaderValue(MOMENT_MAGNITUDE);
        this.lmag = inV1.getRealHeaderValue(LOCAL_MAGNITUDE);
        this.smag = inV1.getRealHeaderValue(SURFACE_MAGNITUDE);
        this.omag = inV1.getRealHeaderValue(OTHER_MAGNITUDE);
        
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
            //this taperlength is the value in seconds from the configuration file
            String taplen = config.getConfigValue(BP_TAPER_LENGTH);
            this.taperlength = (taplen == null) ? DEFAULT_TAPER_LENGTH : Double.parseDouble(taplen);
            this.taperlength = (this.taperlength < 0.0) ? DEFAULT_TAPER_LENGTH : this.taperlength;  
            
            String pbuf = config.getConfigValue(EVENT_ONSET_BUFFER);
            this.ebuffer = (pbuf == null) ? DEFAULT_EVENT_ONSET_BUFFER : Double.parseDouble(pbuf);
            this.ebuffer = (this.ebuffer < 0.0) ? DEFAULT_EVENT_ONSET_BUFFER : this.ebuffer;
            
            String eventmethod = config.getConfigValue(EVENT_ONSET_METHOD);
            if (eventmethod == null) {
                this.emethod = DEFAULT_EVENT_ONSET_METHOD;
            } else if (eventmethod.equalsIgnoreCase("AIC")) {
                this.emethod = EventOnsetType.AIC;
            } else {
                this.emethod = EventOnsetType.DE;
            }
        } catch (NumberFormatException err) {
            throw new SmException("Error extracting numeric values from configuration file");
        }
        qcchecker = new QCcheck();
        if (!qcchecker.validateQCvalues()){
            throw new SmException("Error extracting numeric values from configuration file");
        }
        String debugon = config.getConfigValue(DEBUG_TO_LOG);
        this.writeDebug = (debugon == null) ? false : 
                                        debugon.equalsIgnoreCase(DEBUG_TO_LOG_ON);
        
        String baselineon = config.getConfigValue(WRITE_BASELINE_FUNCTION);
        this.writeBaseline = (baselineon == null) ? false : 
                                    baselineon.equalsIgnoreCase(BASELINE_WRITE_ON);
    }
    
    public V2Status processV2Data() throws SmException, IOException {  
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
        
        System.out.println("Start of V2 processing for " + V0name.toString() + " and channel " + channel);
        //Pick P-wave and remove baseline
        if (writeDebug) {
            errorlog.add("Start of V2 processing for " + V0name.toString() + " and channel " + channel);
            errorlog.add(String.format("EventID: %s",eventID));
            errorlog.add(String.format("time per sample in sec %4.3f",dtime));
            errorlog.add(String.format("sample rate (samp/sec): %4.1f",samplerate));
            errorlog.add(String.format("length of acceleration array: %d",accraw.length));
            errorlog.add("Event detection: remove linear trend, filter, event onset detection");
        }
        ///////////////////////////////
        //
        // Remove trend and filter before event onset detection
        //
        ///////////////////////////////
        ArrayOps.removeLinearTrend( acc, dtime);
        
        //set up the filter coefficients and run
        ButterworthFilter filter = new ButterworthFilter();
        boolean valid = filter.calculateCoefficients(lowcutoff, highcutoff, 
                                                        dtime, numpoles, true);
        if (valid) {
            int calcSec = (int)(taperlength * dtime);
            filter.applyFilter(acc, taperlength, calcSec);  //filtered values are returned in acc
        } else {
            throw new SmException("Invalid bandpass filter input parameters");
        }
        if (writeDebug) {
            errorlog.add(String.format("filtering before event onset detection, taperlength: %d", 
                                                        filter.getTaperlength()));
        }
        ///////////////////////////////
        //
        // Find event onset
        //
        ///////////////////////////////
        //Find the start of the event
        if (emethod == EventOnsetType.DE) {
            EventOnsetDetection depick = new EventOnsetDetection( dtime );
            pickIndex = depick.findEventOnset(acc);
            startIndex = depick.applyBuffer(ebuffer);
            if (writeDebug) {
                errorlog.add("Event Detection algorithm: damping energy method");
            }
        } else {
            AICEventDetect aicpick = new AICEventDetect();
            pickIndex = aicpick.calculateIndex(acc, "ToPeak");
            startIndex = aicpick.applyBuffer(ebuffer, dtime);
            if (writeDebug) {
                errorlog.add("Event Detection algorithm: modified Akaike Information Criterion");
            }
        }
        if (writeDebug) {
            errorlog.add(String.format("pick index: %d, start index: %d",
                                                        pickIndex,startIndex));
            errorlog.add(String.format("pick time in seconds: %8.3f, buffered time: %8.3f",
                                          (pickIndex*dtime),(startIndex*dtime)));
        }
        if (pickIndex <= 0) {
            //No pick index detected, so skip all V2 processing
            procStatus  = V2Status.NOEVENT;
            errorlog.add("V2process: exit status = " + procStatus);
//            System.out.println("V2process: exit status = " + procStatus);
            writeOutErrorDebug();
            return procStatus;
        }
        ///////////////////////////////
        //
        // Remove pre-event mean from acceleration
        //
        ///////////////////////////////
        //Remove pre-event mean from acceleration record
        if (startIndex > 0) {
            double[] subset = Arrays.copyOfRange( accraw, 0, startIndex );
            ArrayStats accsub = new ArrayStats( subset );
            preEventMean = accsub.getMean();
            ArrayOps.removeValue(accraw, preEventMean);
            if (writeDebug) {
                errorlog.add(String.format("Pre-event mean of %10.6e removed from uncorrected acceleration",preEventMean));
            }
        }
//        if (writeDebug) {
//            elog.writeOutArray(accraw, V0name.getName() + "_" + channel + "_initialBaselineCorrection.txt");
//        } 

        ///////////////////////////////
        //
        // Integrate Acceleration to Velocity
        //
        ///////////////////////////////
        //Integrate the acceleration to get velocity, using 0 as first value estimate
        velocity = ArrayOps.Integrate( accraw, dtime, 0.0);
        //Now correct for unknown initial value by removing preevent mean (minus first val.)
        double[] velset = Arrays.copyOfRange( velocity, 1, startIndex );
        ArrayStats velsub = new ArrayStats( velset );
        ArrayOps.removeValue(velocity, velsub.getMean());
        
        if (writeDebug) {
            errorlog.add("acceleration integrated to velocity");
        }
        if (writeDebug) {
           elog.writeOutArray(velocity, V0name.getName() + "_" + channel + "_afterIntegrationToVel.txt");
        }
        ///////////////////////////////
        //
        // Remove Trend with Best Fit
        //
        ///////////////////////////////
        //Remove any linear or 2nd order polynomial trend from velocity
        double[] veltest = new double[velocity.length];
        System.arraycopy( velocity, 0, veltest, 0, velocity.length);
        trendRemovalOrder = ArrayOps.removeTrendWithBestFit( veltest, dtime);
        if (writeDebug) {
            errorlog.add(String.format("Best fit trend of order %d removed from velocity", trendRemovalOrder));
        }
        //Update Butterworth filter low and high cutoff thresholds for later
        FilterCutOffThresholds threshold = new FilterCutOffThresholds();
        magtype = threshold.SelectMagAndThresholds(mmag, lmag, smag, omag, noRealVal);
        if (magtype == MagnitudeType.INVALID) {
            throw new SmException("All earthquake magnitude real header values are invalid.");
        }
        lowcutadj = threshold.getLowCutOff();
        highcutadj = threshold.getHighCutOff();
        magnitude = threshold.getMagnitude();
        if (writeDebug) {
            errorlog.add(String.format("earthquake magnitude: %3.2f",magnitude));
        }
        //perform first QA check on velocity, check first and last sections of
        //velocity array - should be close to 0.0 with tolerances.  If not,
        //perform adaptive baseline correction.
        ///////////////////////////////
        //
        // First QC Test
        //
        ///////////////////////////////
        qcchecker.findWindow(lowcutadj, samplerate, pickIndex);
        boolean passedQC = qcchecker.qcVelocity(veltest);
        if ( !passedQC ){
            errorlog.add("Velocity QC1 failed:");
            errorlog.add(String.format("   initial velocity: %f,  limit %f",
                                        Math.abs(qcchecker.getInitialVelocity()),
                                              qcchecker.getInitVelocityQCval()));
            errorlog.add(String.format("   final velocity: %f,  limit %f",
                                  Math.abs(qcchecker.getResidualVelocity()), 
                                            qcchecker.getResVelocityQCval()));
            errorlog.add("Adaptive baseline correction beginning");
//            System.out.println("failed QC1");
            ///////////////////////////////
            //
            // Baseline Correction
            //
            ///////////////////////////////
            needsABC = true;            
            System.out.println("ABC needed");
            AdaptiveBaselineCorrection adaptABC = new AdaptiveBaselineCorrection(
            dtime,velocity,lowcutadj,highcutadj,numpoles,pickIndex,taperlength);
            procStatus = adaptABC.findFit();
            basetype = BaselineType.ABC;
            int solution = adaptABC.getSolution();
            double[] baseline = adaptABC.getBaselineFunction();
            double[] goodrun = adaptABC.getSolutionParms(solution);
            calculated_taper = adaptABC.getCalculatedTaperLength();
            ABCnumparams = adaptABC.getNumRuns();
            ABCwinrank = solution;
            adaptABC.clearParamsArray();
            accel = adaptABC.getABCacceleration();
            velocity = adaptABC.getABCvelocity();
            displace = adaptABC.getABCdisplacement();
            
            //If unable to perform any iterations in ABC, just exit with no V2
            if (procStatus == V2Status.NOABC) {
//                System.out.println("V2process: exit status = " + procStatus);
                errorlog.add("V2process: exit status = " + procStatus);
                writeOutErrorDebug();
                return procStatus;
            }
            QCvelinitial = goodrun[2];
            QCvelresidual = goodrun[3];
            QCdisresidual = goodrun[1];
            ABCpoly1 = (int)goodrun[6];
            ABCpoly2 = (int)goodrun[7];
            ABCbreak1 = (int)goodrun[4];
            ABCbreak2 = (int)goodrun[5];
            if (writeBaseline) { 
                //the velocity here hasn't been updated by baseline correction yet
                //and contains the array as it goes into baseline correction,
                //where the baseline function is determined and applied
                elog.writeOutArray(baseline, (V0name.getName() + "_" + channel + "_baseline.txt"));
                elog.writeOutArray(velocity, V0name.getName() + "_" + channel + "_BestFitTrendRemovedVel.txt");
            } 
            if (writeDebug) {
                errorlog.add("    length of ABC params: " + ABCnumparams);
                errorlog.add("    ABC: found passing solution");
                errorlog.add("    ABC: winning rank: " + ABCwinrank);
                errorlog.add("    ABC: poly1 order: " + ABCpoly1);
                errorlog.add("    ABC: poly2 order: " + ABCpoly2);
                errorlog.add("    ABC: start: " + ABCbreak1 + "  stop: " + ABCbreak2);
                errorlog.add(String.format("    ABC: velstart: %f,  limit %f", 
                                QCvelinitial,qcchecker.getInitVelocityQCval()));
                errorlog.add(String.format("    ABC: velend: %f,  limit %f",QCvelresidual, 
                                            qcchecker.getResVelocityQCval()));
                errorlog.add(String.format("    ABC: disend: %f,  limit %f",QCdisresidual, 
                                                qcchecker.getResDisplaceQCval()));
                errorlog.add(String.format("    ABC: calc. taperlength: %d", 
                                                            calculated_taper));
            }
            initialVel = velocity[0];
            initialDis = displace[0];
        } else {
            ///////////////////////////////
            //
            // Passed first QC, so filter velocity
            //
            ///////////////////////////////
            velocity = veltest;
            double[] paddedvelocity;
            filter = new ButterworthFilter();
            if (writeDebug) {
                errorlog.add("Acausal bandpass filter:");
                errorlog.add(String.format("  earthquake magnitude is %4.2f and M used is %s",
                                                            magnitude,magtype));
                errorlog.add(String.format("  adjusted lowcut: %4.2f and adjusted highcut: %4.2f Hz",
                                                        lowcutadj, highcutadj));
            }
            valid = filter.calculateCoefficients(lowcutadj, highcutadj, 
                                                dtime, DEFAULT_NUM_POLES, true);
            if (valid) {
                paddedvelocity = filter.applyFilter(velocity, taperlength, pickIndex);
            } else {
                throw new SmException("Invalid bandpass filter calculated parameters");
            }
            calculated_taper = filter.getTaperlength();
            if (writeDebug) {
                errorlog.add(String.format("filtering after 1st QC, taperlength: %d", 
                                                            filter.getTaperlength()));
            }
//            if (writeDebug) {
//               elog.writeOutArray(velocity, V0name.getName() + "_" + channel + "_velocityAfterFiltering.txt");
//            }
//            if (writeDebug) {
//               elog.writeOutArray(paddedvelocity, V0name.getName() + "_" + channel + "_paddedVelocityAfterFiltering.txt");
//            }
            //The velocity array was updated with the filtered values in the 
            //apply filter call
            initialVel = velocity[0];
            double[] paddeddisplace;
            displace = new double[velocity.length];
            ///////////////////////////////
            //
            // Integrate padded velocity and extract displacement
            //
            ///////////////////////////////
            paddeddisplace = ArrayOps.Integrate( paddedvelocity, dtime, 0.0);
            System.arraycopy(paddeddisplace, filter.getPadLength(), displace, 0, displace.length);
            initialDis = displace[0];
//            System.out.println("initial Velocity: " + initialVel);
//            System.out.println("initial Displace: " + initialDis);
//            System.out.println("pad length: " + filter.getPadLength());
            if (writeDebug) {
                errorlog.add("Velocity integrated to displacement");
            }
            ///////////////////////////////
            //
            // Differentiate velocity to corrected acceleration
            //
            ///////////////////////////////
            //Differentiate velocity for final acceleration
            accel = ArrayOps.Differentiate(velocity, dtime);
            if (writeDebug) {
                errorlog.add("Velocity differentiated to corrected acceleration");
            }
            ///////////////////////////////
            //
            // Second QC Test (also performed in ABC)
            //
            ///////////////////////////////
            boolean success = qcchecker.qcVelocity(velocity) && 
                                            qcchecker.qcDisplacement(displace);
            procStatus = (success) ? V2Status.GOOD : V2Status.FAILQC;
            QCvelinitial = Math.abs(qcchecker.getInitialVelocity());
            QCvelresidual = Math.abs(qcchecker.getResidualVelocity());
            QCdisresidual = Math.abs(qcchecker.getResidualDisplacement());
        }
        if (procStatus == V2Status.FAILQC) {
            errorlog.add("Final QC failed - V2 processing unsuccessful:");
            errorlog.add(String.format("   initial velocity: %f, limit %f",
                                        Math.abs(qcchecker.getInitialVelocity()),
                                              qcchecker.getInitVelocityQCval()));
            errorlog.add(String.format("   final velocity: %f, limit %f",
                                  Math.abs(qcchecker.getResidualVelocity()), 
                                            qcchecker.getResVelocityQCval()));
            errorlog.add(String.format("   final displacement,: %f, limit %f",
                                  Math.abs(qcchecker.getResidualDisplacement()),
                                            qcchecker.getResDisplaceQCval()));
//            System.out.println("failed QC2");
        }
//        System.out.println("V2process: exit status = " + procStatus);
        if (writeDebug) {
            errorlog.add("V2process: exit status = " + procStatus);
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

        if (writeDebug) {
            errorlog.add(String.format("Peak Velocity: %f",VpeakVal));
        }
        //if status is GOOD, calculate computed parameters for headers
        if (procStatus == V2Status.GOOD) {
            ComputedParams cp = new ComputedParams(accel, dtime);
            strongMotion = cp.calculateComputedParameters();
            if (strongMotion) {
                bracketedDuration = cp.getBracketedDuration();
                AriasIntensity = cp.getAriasIntensity();
                HousnerIntensity = cp.getHousnerIntensity();
                channelRMS = cp.getChannelRMS();
                durationInterval = cp.getDurationInterval();
                cumulativeAbsVelocity = cp.getCumulativeAbsVelocity();
                if (writeDebug) {
                    errorlog.add("Strong motion record");
                }
            }
        }
        if ((writeDebug) || (procStatus != V2Status.GOOD)) {
            writeOutErrorDebug();
            makeDebugCSV();
        }
        return procStatus;
    }
    public void writeOutErrorDebug() throws IOException {
        elog.writeToLog(logstart);
        String[] errorout = new String[errorlog.size()];
        errorout = errorlog.toArray(errorout);
        elog.writeToLog(errorout);
        errorlog.clear();
    }
    public void makeDebugCSV() throws IOException {
        String[] headerline = {"EVENT","MAG","NAME","CHANNEL",
            "ARRAY LENGTH","SAMP INTERVAL(SEC)","PICK INDEX","PICK TIME(SEC)",
            "PEAK VEL(CM/SEC)","TAPERLENGTH","PRE-EVENT MEAN","PEAK ACC(G)",
            "STRONG MOTION","EXIT STATUS","VEL INITIAL(CM/SEC)",
            "VEL RESIDUAL(CM/SEC)","DIS RESIDUAL(CM)","BASELINE CORRECTION",
            "ABC POLY1","ABC POLY2","ABC 1ST BREAK","ABC 2ND BREAK",
            "ABC PARM LENGTH","ABC WIN RANK"};
        ArrayList<String> data = new ArrayList<>();
        data.add(eventID);                                  //event id
        data.add(String.format("%4.2f",magnitude));         //event magnitude
        data.add(V0name.getName());                        //record file name
        data.add(channel);                                  //channel number
        data.add(String.format("%d",velocity.length));      //length of array
        data.add(String.format("%5.3f",dtime));             //sample interval
        data.add(String.format("%d",pickIndex));            //event onset index
        data.add(String.format("%8.3f", pickIndex*dtime));  //event onset time
        data.add(String.format("%8.5f",VpeakVal));          //peak velocity
        data.add(String.format("%d",calculated_taper));     //filter taperlength
        data.add(String.format("%8.6f",preEventMean));      //preevent mean removed
        data.add(String.format("%5.4f",ApeakVal*TO_G_CONVERSION)); //peak acc in g
        if (strongMotion) {
            data.add("YES");
        } else if (procStatus != V2Status.GOOD) {
            data.add("--");
        } else {
            data.add("NO");
        }
        data.add(procStatus.name());                       //exit status
        data.add(String.format("%8.6f",QCvelinitial));     //QC value initial velocity
        data.add(String.format("%8.6f",QCvelresidual));    //QC value residual velocity
        data.add(String.format("%8.6f",QCdisresidual));    //QC value residual displace
        data.add(basetype.name());
        data.add((ABCpoly1 > 0) ? String.format("%d",ABCpoly1) : "");
        data.add((ABCpoly2 > 0) ? String.format("%d",ABCpoly2) : "");
        data.add((ABCbreak1 > 0) ? String.format("%d",ABCbreak1) : "");
        data.add((ABCbreak2 > 0) ? String.format("%d",ABCbreak2) : "");
        data.add((ABCnumparams > 0) ? String.format("%d",ABCnumparams) : "");
        data.add((ABCwinrank > 0) ? String.format("%d",ABCwinrank) : "");
        
        elog.writeToCSV(data, headerline, "parameterLog.csv");
        data.clear();
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
    public double getBracketedDuration() {
        return bracketedDuration;
    }
    public double getAriasIntensity() {
        return AriasIntensity;
    }
    public double getHousnerIntensity() {
        return HousnerIntensity;
    }
    public double getChannelRMS() {
        return channelRMS;
    }
    public double getDurationInterval() {
        return durationInterval;
    }
    public double getCumulativeAbsVelocity() {
        return cumulativeAbsVelocity;
    }
    public double getInitialVelocity() {
        return initialVel;
    }
    public double getInitialDisplace() {
        return initialDis;
    }
}
