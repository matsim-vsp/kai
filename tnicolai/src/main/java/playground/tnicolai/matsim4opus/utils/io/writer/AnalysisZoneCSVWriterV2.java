package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.InternalConstants;

public class AnalysisZoneCSVWriterV2 {

	private static final Logger log = Logger.getLogger(AnalysisZoneCSVWriterV2.class);
	private static BufferedWriter zoneCSVWriter = null;
	public static final String FILE_NAME= "zones_complete.csv";
	
	/**
	 * writes the header of accessibility data csv file
	 */
	public static void initAccessiblityWriter(){
		try{
			log.info("Initializing AnalysisZoneCSVWriterV2 ...");
			zoneCSVWriter = IOUtils.getBufferedWriter( InternalConstants.MATSIM_4_OPUS_TEMP + FILE_NAME );
			log.info("Writing data into " + InternalConstants.MATSIM_4_OPUS_TEMP + FILE_NAME + " ...");
			
			// create header
			zoneCSVWriter.write( InternalConstants.ZONE_ID + "," +
								 InternalConstants.ZONE_CENTROID_X_COORD + "," +
								 InternalConstants.ZONE_CENTROID_Y_COORD + "," +
								 InternalConstants.NEARESTNODE_X_COORD + "," +
								 InternalConstants.NEARESTNODE_Y_COORD + "," +
								 InternalConstants.ACCESSIBILITY_BY_CAR + "," +
								 InternalConstants.ACCESSIBILITY_BY_WALK);
			zoneCSVWriter.newLine();
			
			log.info("... done!");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * writing the accessibility measures into csv file
	 * 
	 * @param zoneID
	 * @param zoneCentroid
	 * @param nearestNode
	 * @param carAccessibility
	 * @param walkAccessibility
	 */
	public static void write(Id zoneID,
							 Coord zoneCentroid,
							 Coord nearestNode,
							 double carAccessibility, 
							 double walkAccessibility){
		
		try{
			assert(AnalysisZoneCSVWriterV2.zoneCSVWriter != null);
			zoneCSVWriter.write( zoneID + "," + 
								 zoneCentroid.getX() + "," + 
								 zoneCentroid.getY() + "," + 
								 nearestNode.getX() + "," + 
								 nearestNode.getY() + "," + 
								 carAccessibility + "," + 
								 walkAccessibility );
			zoneCSVWriter.newLine();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * finalize and close csv file
	 */
	public static void close(){
		try {
			log.info("Closing AnalysisZoneCSVWriterV2 ...");
			assert(AnalysisZoneCSVWriterV2.zoneCSVWriter != null);
			zoneCSVWriter.flush();
			zoneCSVWriter.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
