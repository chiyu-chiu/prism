/*******************************************************************************
 * Name: Java class VFileConstants.java
 * Project: PRISM strong motion record processing using COSMOS data format
 * Written by: Jeanne Jones, USGS, jmjones@usgs.gov
 * 
 * Date: first release date Feb. 2015
 ******************************************************************************/

package SmConstants;

/**
 * This class defines the constants used during strong motion processing, including
 * constants from the tables defined in the COSMOS Strong Motion Data Format document.
 * @author jmjones
 */
public final class VFileConstants {
    //text header markers
    public static final int HEADLINE_1_LENGTH = 59;
    public static final int END_OF_DATATYPE = 24;
    public static final int NUM_HEAD_START = 46;
    public static final int NUM_HEAD_END = 48;
    
    public static final int MAX_LINE_LENGTH = 80;
    public static final int SENSOR_LOCATION_LINE = 8;
    public static final int SENSOR_LOCATION_START = 46;
    public static final int NODATA_LINE = 12;
    public static final int END_OF_DATA_CHAN = 17;
    
    //data arrays, display default parameters
    public static final int DEFAULT_NOINTVAL = -999;
    public static final double DEFAULT_NOREALVAL = -999.0;
    public static final int DEFAULT_INT_FIELDWIDTH = 8;
    public static final int DEFAULT_REAL_FIELDWIDTH = 15;
    public static final int DEFAULT_REAL_PRECISION = 6;
    public static final int REAL_FIELDWIDTH_V1 = 12;
    public static final int REAL_PRECISION_V1 = 3;
    public static final int REAL_FIELDWIDTH_V2 = 12;
    public static final int REAL_PRECISION_V2 = 3;
    public static final int REAL_FIELDWIDTH_V3 = 15;
    public static final int REAL_PRECISION_V3 = 3;
    public static final String DEFAULT_REAL_DISPLAYTYPE = "F";
    public static final String DEFAULT_INT_DISPLAYTYPE = "I";
    
    //event date time text header entries
    public static final int START_TIME_YEAR = 39;
    public static final int START_TIME_MONTH = 41;
    public static final int START_TIME_DAY = 42;
    public static final int START_TIME_HOUR = 43;
    public static final int START_TIME_MIN = 44;
    public static final int START_TIME_SEC = 29;
    
    //V2 processing header markers
    public static final int LOW_FREQ_FILTER_TYPE = 60;
    public static final int HIGH_FREQ_FILTER_TYPE = 61;
    public static final int LOW_FREQ_CORNER = 53;
    public static final int HIGH_FREQ_CORNER = 56;
    public static final int FILTER_DOMAIN_FLAG = 63;
    public static final int INITIAL_VELOCITY_VAL = 67;
    public static final int INITIAL_DISPLACE_VAL = 68;
    public static final int FILTER_OPERATOR_LENGTH = 59;
    
    //data product names
    public static final String RAWACC =   "Raw acceleration counts  ";
    public static final String UNCORACC = "Uncorrected acceleration ";
    public static final String CORACC =   "Corrected acceleration   ";
    public static final String VELOCITY = "Velocity data            ";
    public static final String DISPLACE = "Displacement data        ";
    public static final String SPECTRA =  "Response spectra         ";
    
    public enum V2DataType { ACC, VEL, DIS };
    public enum V2Status { GOOD, FAILQC, NOEVENT, NOABC };
    
    //units names and codes
    public static final String SECT = "sec";
    public static final int SECN = 1;
    public static final String CMSQSECT = "cm/sec2";
    public static final int CMSQSECN = 4;
    public static final String CMSECT = "cm/sec";
    public static final int CMSECN = 5;
    public static final String CMT = "cm";
    public static final int CMN = 6;
    public static final String GUNITST = "g";
    public static final int GLN = 2;
    public static final String COUNTTEXT = "counts";
    public static final int CNTN = 50;
    
    //filter code
    public static final int BUTTER_A_CODE = 5;
    public static final int TIME_DOMAIN = 1;
    
    public enum SmArrayStyle { SINGLE_COLUMN, PACKED };
    
    public static final String DEFAULT_ARRAY_STYLE = "singleColumn";
    
    //table 1 data physical parameter codes
    public static final int ACC_PARM_CODE = 1;
    public static final int VEL_PARM_CODE = 2;
    public static final int DIS_ABS_PARM_CODE = 3;
    
    //int header index values
    public static final int STATION_CHANNEL_NUMBER = 49;
    public static final int PROCESSING_STAGE_INDEX = 0;
    public static final int DATA_PHYSICAL_PARAM_CODE = 1;
    public static final int V_UNITS_INDEX = 2;
    public static final int PROCESSING_AGENCY = 13;
    
    //real header index values
    public static final int MEAN_ZERO = 35;
    public static final int DELTA_T = 61;
    public static final int PEAK_VAL = 63;
    public static final int PEAK_VAL_TIME = 64;
    public static final int AVG_VAL = 65;
    public static final int RECORER_LSB = 21;  //recorder least significant bit in microvolts
    public static final int SENSOR_SENSITIVITY = 41; //in volts per g
    public static final int SCALING_FACTOR = 87;
    
    public enum MagnitudeType { INVALID, MOMENT, M_LOCAL, SURFACE, M_OTHER };
    
    public static final int LOCAL_MAGNITUDE = 14;
    public static final int MOMENT_MAGNITUDE = 12;
    public static final int SURFACE_MAGNITUDE = 13;
    public static final int OTHER_MAGNITUDE = 15;
    
    //V2 computed parameters codes
    public static final int BRACKETED_DURATION = 79;
    public static final int DURATION_INTERVAL = 80;
    public static final int CHANNEL_RMS = 81;
    public static final int CUMULATIVE_ABS_VEL = 82;
    public static final int HOUSNER_INTENSITY = 83;
    public static final int ARIAS_INTENSITY = 84;
    
    //V3 response spectrum parameter codes
    public static final int NUM_SPECTRA_PERIODS = 69;
    public static final int NUM_DAMPING_VALUES = 70;
    public static final int VALUE_SA_0P2 = 69;
    public static final int VALUE_SA_0P3 = 70;
    public static final int VALUE_SA_1P0 = 71;
    public static final int VALUE_SA_3P0 = 72;
    public static final int MAX_SA_SPECTRUM = 73;
    public static final int PERIOD_OF_MAX = 74;
    public static final int TIME_OF_MAX = 75;
    
    //Processing stages
    public static final int V1_STAGE = 1;
    public static final int V2_STAGE = 2;
    public static final int V3_STAGE = 3;
    
    //misc. constants
    public static final double MSEC_TO_SEC = 0.001; //milliseconds to seconds
    public static final double FROM_G_CONVERSION = 980.665; //cm per sq. sec per g
    public static final double TO_G_CONVERSION = 0.0010197;  //g per cm per sq. sec
    
    public enum EventOnsetType{ AIC, DE };
    
    //event onset constants
    public static final double DEFAULT_EVENT_ONSET_BUFFER = 0.0;
    public static final EventOnsetType DEFAULT_EVENT_ONSET_METHOD = EventOnsetType.DE;

    //filtering constants
    public static final int DEFAULT_NUM_POLES = 2;
    public static final double DEFAULT_HIGHCUT = 0.5;
    public static final double DEFAULT_LOWCUT = 30.0;
    public static final double DEFAULT_TAPER_LENGTH = 2.0;
    
    //adaptive baseline correction constants
    public static final int DEFAULT_1ST_POLY_ORD_LOWER = 1;
    public static final int DEFAULT_1ST_POLY_ORD_UPPER = 2;
    public static final int DEFAULT_2ND_POLY_ORD_LOWER = 1;
    public static final int DEFAULT_2ND_POLY_ORD_UPPER = 2;
    
    //QC check constants
    public static final double DEFAULT_QA_INITIAL_VELOCITY = 0.1;
    public static final double DEFAULT_QA_RESIDUAL_VELOCITY = 0.1;
    public static final double DEFAULT_QA_RESIDUAL_DISPLACE = 0.1;
    
    public static final String DEBUG_TO_LOG_ON = "On";
    public static final String BASELINE_WRITE_ON = "On";
    
    //V3 processing
    public static final int NUM_COEF_VALS = 6;
    public static final double[] V3_DAMPING_VALUES = {0.00, 0.02, 0.05, 0.10, 0.20};
    public static final double[] V3_SAMPLING_RATES = {50.0, 100.0, 200.0, 500.0};
    public static final int NUM_T_PERIODS = 91;
    //1 array for periods, 1 for fft, 3 for each of 5 damping values = 17
    public static final int NUM_V3_SPECTRA_ARRAYS = 15;
}
