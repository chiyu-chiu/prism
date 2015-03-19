/*******************************************************************************
 * Name: Java class SmQueue.java
 * Project: PRISM strong motion record processing using COSMOS data format
 * Written by: Jeanne Jones, USGS, jmjones@usgs.gov
 * 
 * Date: first release date Feb. 2015
 ******************************************************************************/

package SmControl;

import COSMOSformat.COSMOScontentFormat;
import COSMOSformat.V0Component;
import COSMOSformat.V1Component;
import COSMOSformat.V2Component;
import COSMOSformat.V3Component;
import static SmConstants.VFileConstants.*;
import SmConstants.VFileConstants.V2DataType;
import SmException.FormatException;
import SmException.SmException;
import SmProcessing.V1Process;
import SmProcessing.V2Process;
import SmProcessing.V3Process;
import SmUtilities.TextFileReader;
import java.io.*;
import java.util.ArrayList;

/**
 *  This class builds a queue of all the records in one input V0 file.  The file
 * may contain only one channel or could have mulitple channels bundled together.
 * The contents of the queue are processed to create V1 - V3 records, complete
 * with their text files ready for writing out to file.
 * @author jmjones
 */
public class SmQueue {
    private final File fileName; //input file name and path
    private ArrayList<COSMOScontentFormat> smlist;  //holds each channel as a record
    private String[] fileContents;  // the input file contents by line
    private String logtime;
    /**
     * Constructor for SmQueue
     * @param inFileName input file name
     * @param logtime time processing started (not used?)
     */
    public SmQueue (File inFileName, String logtime){
        this.fileName = inFileName;
        this.logtime = logtime;
    }
    /**
     * This method reads in the input text file
     * @param filename input file name
     * @throws IOException if unable to read the file
     */
    public void readInFile(File filename) throws IOException{
        TextFileReader infile = new TextFileReader( filename );
        fileContents = infile.readInTextFile();
    }
    /**
     * Start with the COSMOS text file in an array of strings.  Create a record for
     * each channel in the file and fill the record with the header and data
     * arrays.  Let the record object determine how much of the file goes into
     * each channel record.  Create an arrayList of all the records contained
     * in the file so they can be processed individually.  Keeping them in
     * the list will also facilitate writing out the results either individually
     * or bundled.
     * 
     * @param dataType the type of file read in (V0, V1, etc.)
     * @return the number of records in the queue
     * @throws FormatException if unable to parse the file due to unexpected formatting
     * @throws NumberFormatException if unable to convert text to expected numeric
     * @throws SmException if unable to parse the file, see log file
     */
    public int parseVFile(String dataType) throws FormatException, 
                                        NumberFormatException, SmException {
        int currentLine = 0;
        int returnLine;
        smlist = new ArrayList<>();
        
        while (currentLine < fileContents.length) {
            if (dataType.equals( RAWACC )) {
                V0Component rec = new V0Component( dataType );
                returnLine = rec.loadComponent(currentLine, fileContents);
                currentLine = (returnLine > currentLine) ? returnLine : fileContents.length;
                smlist.add(rec);
            } else if (dataType.equals( UNCORACC )){
                V1Component rec = new V1Component( dataType );
                returnLine = rec.loadComponent(currentLine, fileContents);
                currentLine = (returnLine > currentLine) ? returnLine : fileContents.length;
                smlist.add(rec);                
            } else if ((dataType.equals( CORACC )) || (dataType.equals( VELOCITY )) ||
                                                 (dataType.equals( DISPLACE ))) {
                //Look at current line to see what piece of V2 is next.
                if (fileContents[currentLine].matches("(?s).*(?i)Velocity.*")) {
                    dataType = VELOCITY;
                } else if (fileContents[currentLine].matches("(?s).*(?i)Displace.*"))  {
                    dataType = DISPLACE;
                } else {
                    dataType = CORACC;
                }
                V2Component rec = new V2Component( dataType );
                returnLine = rec.loadComponent(currentLine, fileContents);
                currentLine = (returnLine > currentLine) ? returnLine : fileContents.length;
                smlist.add(rec);                
            } else {
                throw new FormatException("Invalid file data type: " + dataType);
            }
        }
        return smlist.size();
    }
    /**
     * This method processes each record in the queue and hands the products off
     * to the product object.
     * @param Vprod the product queue object that will receive the processed results
     * @throws FormatException if a called method is unable to format, such as text to numerics
     * @throws SmException if a called method found a processing error such as an
     * invalid header parameter
     * @throws IOException if unable to create directories, etc.
     */
    public void processQueueContents(SmProduct Vprod) 
                                throws FormatException, SmException, IOException {

        V2Component V2acc;
        V2Component V2vel;
        V2Component V2dis;
        
        for (COSMOScontentFormat rec : smlist) {
            //declare rec as a V0 channel record
            V0Component v0rec = (V0Component)rec;
            v0rec.updateV0(this.fileName.toString());
            
            //create the V1 processing object and do the processing          
            V1Process v1val = new V1Process(v0rec);
            v1val.processV1Data();
            
            //create a V1 component to get the processing results
            V1Component v1rec = new V1Component( UNCORACC, v0rec);
            v1rec.buildV1(v1val);
           
            //Create the V2 processing object and do the processing.  V2 processing
            //produces 3 V2 objects: corrected acceleration, velocity, and displacement
            V2Process v2val = new V2Process(v1rec, this.fileName, this.logtime);
            V2Status V2result = v2val.processV2Data();
            
            Vprod.setDirectories(v0rec.getRcrdId(),v0rec.getSCNLauth(), 
                                                v1rec.getEventDateTime(),V2result);
            Vprod.addProduct(v0rec, "V0");
            Vprod.addProduct(v1rec, "V1");
            
            if ((V2result == V2Status.GOOD) || (V2result == V2Status.FAILQC)) {
                //create the V2 components to get the processing results
                V2acc = new V2Component( CORACC, v1rec );
                V2acc.buildV2(V2DataType.ACC, v2val);
                V2vel = new V2Component( VELOCITY, v1rec );
                V2vel.buildV2(V2DataType.VEL, v2val);
                V2dis = new V2Component( DISPLACE, v1rec );
                V2dis.buildV2(V2DataType.DIS, v2val);
                Vprod.addProduct(V2acc, "V2");
                Vprod.addProduct(V2vel, "V2");
                Vprod.addProduct(V2dis, "V2");
                if (V2result == V2Status.GOOD) {
                    //Create the V3 processing object and do the processing.  V3
                    //processing produces 1  V3 object: response spectra.
                    V3Process v3val = new V3Process(V2acc, v2val);
                    v3val.processV3Data();
                    V3Component V3rec = new V3Component( SPECTRA, V2acc, V2vel, V2dis);
                    V3rec.buildV3(v3val);
                    V3rec.updateUploadParms();
                    Vprod.addProduct(V3rec, "V3");
                }
            }
        }
    }
    /**
     * Getter for the queue of records from the file
     * @return the queue of cosmos objects
     */
    public ArrayList<COSMOScontentFormat> getSmList() {
        return smlist;
    }
}