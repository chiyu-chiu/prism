/*******************************************************************************
 * Name: Java class SmConfigConstants.java
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

package SmUtilities;

/**
 * This class holds the constants to access the configuration file values.
 * @author jmjones
 */
public class SmConfigConstants {
    public static final String CONFIG_XSD_VALIDATOR = "xsd/prism_config.xsd";
    
    public static final String PROC_AGENCY_CODE = "PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyCode";
    public static final String PROC_AGENCY_NAME = "PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyFullName";
    public static final String PROC_AGENCY_ABBREV = "PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyAbbreviation";
    public static final String PROC_AGENCY_IRIS = "PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyIRISCode";
    
    public static final String OUT_ARRAY_FORMAT = "PRISM/OutputArrayFormat";
    
    public static final String DATA_UNITS_CODE = "PRISM/DataUnitsForCountConversion/DataUnitCodes/DataUnitCode";
    public static final String DATA_UNITS_NAME = "PRISM/DataUnitsForCountConversion/DataUnitCodes/DataUnitName";
    
    public static final String QC_INITIAL_VELOCITY = "PRISM/QCparameters/InitialVelocity";
    public static final String QC_RESIDUAL_VELOCITY = "PRISM/QCparameters/ResidualVelocity";
    public static final String QC_RESIDUAL_DISPLACE = "PRISM/QCparameters/ResidualDisplacement";
    
    public static final String STATION_FILTER_TABLE = "PRISM/StationFilterTable";
    
    public static final String BP_FILTER_ORDER = "PRISM/BandPassFilterParameters/BandPassFilterOrder";
    public static final String BP_TAPER_LENGTH = "PRISM/BandPassFilterParameters/BandPassTaperLength";
    public static final String BP_FILTER_CUTOFFHIGH = "PRISM/BandPassFilterParameters/BandPassFilterCutoff/CutoffHigh";
    public static final String BP_FILTER_CUTOFFLOW = "PRISM/BandPassFilterParameters/BandPassFilterCutoff/CutoffLow";
    
    public static final String SM_THRESHOLD = "PRISM/StrongMotionThreshold";
    
    public static final String EVENT_ONSET_BUFFER = "PRISM/EventOnsetBufferAmount";
    public static final String EVENT_ONSET_METHOD = "PRISM/EventDetectionMethod";
    
    public static final String DELETE_V0 = "PRISM/DeleteInputV0";
    public static final String DEBUG_TO_LOG = "PRISM/DebugToLog";
    public static final String WRITE_BASELINE_FUNCTION = "PRISM/WriteBaselineFunction";
    
    public static final String FIRST_POLY_ORDER_LOWER  = "PRISM/AdaptiveBaselineCorrection/FirstPolyOrder/LowerLimit";
    public static final String FIRST_POLY_ORDER_UPPER  = "PRISM/AdaptiveBaselineCorrection/FirstPolyOrder/UpperLimit";
    public static final String THIRD_POLY_ORDER_LOWER = "PRISM/AdaptiveBaselineCorrection/ThirdPolyOrder/LowerLimit";
    public static final String THIRD_POLY_ORDER_UPPER = "PRISM/AdaptiveBaselineCorrection/ThirdPolyOrder/UpperLimit";
    
    public static final String DIFFERENTIATION_ORDER = "PRISM/DifferentiationOrder";
}
