package playground.pieter.singapore.utils;

import java.io.File;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;



public class FacilitiesToSQL {
	DataBaseAdmin dba;
	ScenarioImpl scenario;
	
	public FacilitiesToSQL(DataBaseAdmin dba, ScenarioImpl scenario) {
		super();
		this.dba = dba;
		this.scenario = scenario;
	}
	public void createShortSQLFacilityList(String tableName) throws SQLException, NoConnectionException{
		dba.executeStatement(String.format("DROP TABLE IF EXISTS %s;",tableName));
		dba.executeStatement(String.format("CREATE TABLE %s(" +
				"id VARCHAR(45)," +
				"x_utm48n real," +
				"y_utm48n real," +
				"description VARCHAR(255)" +			
				")",tableName));
		ActivityFacilitiesImpl facs = scenario.getActivityFacilities();
		for(ActivityFacility fac:facs.getFacilities().values()){
			ActivityFacilityImpl fi = (ActivityFacilityImpl) fac;
			String id = fi.getId().toString();
			double x =fi.getCoord().getX();
			double y = fi.getCoord().getY();
			String description = fi.getDesc();
			String sqlInserter = "INSERT INTO %s " +
					"VALUES(\'%s\',%f,%f,\'%s\');";
			dba.executeUpdate(String.format(sqlInserter,tableName,id,x,y,description));

		}
	}
	
	
	public void createShortSQLFacilityListPostgres(String tableName) {
		try {
			dba.executeStatement(String.format("DROP TABLE IF EXISTS %s;",
					tableName));
			dba.executeStatement(String.format("CREATE TABLE %s("
					+ "id VARCHAR(45)," + "x_utm48n real," + "y_utm48n real" +
					// "description VARCHAR(255)" +
					")", tableName));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Filling the table");
		ActivityFacilitiesImpl facs = scenario.getActivityFacilities();
		int modfactor = 1;
		int counter = 0;
		int lineCounter = 0;
		int batchSize = 200000;
		StringBuilder sb = new StringBuilder();
		CopyManager cpManager;
		try {
			cpManager = ((PGConnection) dba.getConnection()).getCopyAPI();
			PushbackReader reader = new PushbackReader(new StringReader(""),
					100000000);
			System.out.println("processing a total of "
					+ facs.getFacilities().size());
			for (ActivityFacility fac : facs.getFacilities().values()) {
				ActivityFacilityImpl fi = (ActivityFacilityImpl) fac;
				String id = fi.getId().toString();
				double x = fi.getCoord().getX();
				double y = fi.getCoord().getY();
				// String description = fi.getDesc();
				String sqlInserter = "'%s\',%f,%f\n";
				sb.append(String.format(sqlInserter, id, x, y));
				if (lineCounter % batchSize == 0) {
					reader.unread(sb.toString().toCharArray());
					cpManager.copyIn("COPY " + tableName
							+ " FROM STDIN WITH CSV", reader);
					sb.delete(0, sb.length());
				}
				counter++;
				if (counter >= modfactor && counter % modfactor == 0) {
					System.out.println("Processed facility no " + counter);
					modfactor = counter;
				}

			}
			// write out the rest
			reader.unread(sb.toString().toCharArray());
			cpManager.copyIn("COPY " + tableName + " FROM STDIN WITH CSV",
					reader);
			sb.delete(0, sb.length());
			System.out.println("Processed facility no " + counter);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		dba.executeStatement("SELECT AddGeometryColumn( '"
//				+ tableName.split("\\.")[0] + "', '"
//				+ tableName.split("\\.")[1]
//				+ "', 'geom_utm48n', 32648, 'POINT', 2 ); ");
//		dba.executeStatement("update  "
//				+ tableName
//				+ " set geom_utm48n=ST_SetSRID(ST_Point( x_utm48n, y_utm48n),32648) ;");
//		dba.executeStatement("CREATE INDEX idx" + tableName.split("\\.")[1]
//				+ " ON " + tableName + " USING GIST(geom_utm48n);");
//		dba.executeStatement("CREATE INDEX idx_id_" + tableName.split("\\.")[1]
//				+ " ON " + tableName + "(id);");
	}

	public void createCompleteFacilityAndActivityTable(String tableName) throws SQLException, NoConnectionException{
		dba.executeStatement(String.format("DROP TABLE IF EXISTS %s;",tableName));
		dba.executeStatement(String.format("CREATE TABLE %s(" +
				"id VARCHAR(45)," +
				"x_utm48n REAL," +
				"y_utm48n REAL," +
				"description VARCHAR(255)," +
				"actType VARCHAR(45)," +
				"capacity REAL," +
				"day VARCHAR(45)," +
				"startTime REAL," +
				"endTime REAL" +				
				")",tableName));
		System.out.println("Filling the table");
		ActivityFacilitiesImpl facs = scenario.getActivityFacilities();
		int modfactor = 1;
		int counter = 0;
		int lineCounter = 0;
		int batchSize = 1000;
		StringBuilder sb = new StringBuilder();
		for(ActivityFacility fac:facs.getFacilities().values()){
			ActivityFacilityImpl fi = (ActivityFacilityImpl) fac;
			String id = fi.getId().toString();
			double x =fi.getCoord().getX();
			double y = fi.getCoord().getY();
			String description = fi.getDesc();
			for(ActivityOption ao:fi.getActivityOptions().values()){
				ActivityOptionImpl aoi = (ActivityOptionImpl) ao;
				String actType =aoi.getType();
				double capacity = aoi.getCapacity();
				for(DayType dayType : aoi.getOpeningTimes().keySet()){
					String day = dayType.toString();
					aoi.getOpeningTimes();
					for(OpeningTime ot: aoi.getOpeningTimes(dayType)){
						double startTime = ot.getStartTime();
						double endTime = ot.getStartTime();
						String sqlInserter = "INSERT INTO %s " +
								"VALUES(\'%s\',%f,%f,\'%s\',\'%s\',%f,\'%s\',%f,%f);";
						sb.append(String.format(sqlInserter,tableName,id,x,y,description,actType,capacity,day,startTime,endTime));
					}
					if (lineCounter % batchSize == 0){
						dba.executeStatement(sb.toString());
						sb.delete(0,sb.length());
					}
				}
			}
			if(++counter >= modfactor && counter % modfactor == 0){
				System.out.println("Processed facility no "+modfactor);
				modfactor = counter;
			}
			
		}
		//write out the rest
		dba.executeStatement(sb.toString());
		sb.delete(0,sb.length());
		System.out.println("Processed facility no " + counter);
		//add geometry and spatial index to the table]\
		dba.executeStatement("SELECT AddGeometryColumn( '" +tableName.split(".")[0]+
				"', '" +tableName.split(".")[1]+
				"', 'geom_utm48n', 32648, 'POINT', 2 ); ");
		dba.executeStatement("update  " +tableName+
				" set geom_utm48n=ST_SetSRID(ST_Point( x_utm48n, y_utm48n),32648) ;");
		dba.executeStatement("CREATE INDEX idx"+tableName.split(".")[1]+
				" ON "+ tableName + " USING GIST(geom_utm48n);");
	}
	
	public void createCompleteFacilityAndActivityTablePostgres(String tableName) throws SQLException, NoConnectionException{
		dba.executeStatement(String.format("DROP TABLE IF EXISTS %s cascade;",tableName));
		dba.executeStatement(String.format("CREATE TABLE %s(" +
				"id VARCHAR(45)," +
				"x_utm48n REAL," +
				"y_utm48n REAL," +
				"description VARCHAR(255)," +
				"actType VARCHAR(45)," +
				"capacity REAL," +
				"day VARCHAR(45)," +
				"startTime REAL," +
				"endTime REAL" +				
				")",tableName));
		dba.executeStatement(String.format("DROP TABLE IF EXISTS %s;",
				tableName+"_sh"));
		dba.executeStatement(String.format("CREATE TABLE %s("
				+ "id VARCHAR(45)," + "x_utm48n real," + "y_utm48n real" +
				// "description VARCHAR(255)" +
				")", tableName+"_sh"));
		System.out.println("Filling the table");
		ActivityFacilitiesImpl facs = scenario.getActivityFacilities();
		int modfactor = 1;
		int counter = 0;
		int lineCounter = 0;
		int batchSize = 1000;
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		CopyManager cpManager = ((PGConnection)dba.getConnection()).getCopyAPI();
		PushbackReader reader = new PushbackReader( new StringReader(""), 100000000 );
		PushbackReader reader2 = new PushbackReader( new StringReader(""), 100000000 );
		System.out.println("processing a total of "+facs.getFacilities().size());
		for(ActivityFacility fac:facs.getFacilities().values()){
			ActivityFacilityImpl fi = (ActivityFacilityImpl) fac;
			String id = fi.getId().toString();
//			if(id.startsWith("home_17710"))
//				System.out.println(id);
			double x =fi.getCoord().getX();
			double y = fi.getCoord().getY();
			String description = fi.getDesc();
			String sqlInserter2 = "\"%s\",%f,%f\n";
			sb2.append(String.format(sqlInserter2,id,x,y));
			for(ActivityOption ao:fi.getActivityOptions().values()){
				ActivityOptionImpl aoi = (ActivityOptionImpl) ao;
				String actType =aoi.getType();
				double capacity = aoi.getCapacity();
				for(DayType dayType : aoi.getOpeningTimes().keySet()){
					String day = dayType.toString();
					aoi.getOpeningTimes();
					for(OpeningTime ot: aoi.getOpeningTimes(dayType)){
						double startTime = ot.getStartTime();
						double endTime = ot.getStartTime();
						String sqlInserter = "\"%s\",%f,%f,\"%s\",\"%s\",%f,\"%s\",%f,%f\n";
						sb.append(String.format(sqlInserter,id,x,y,description,actType,capacity,day,startTime,endTime));
						lineCounter++;
					}
					if (lineCounter % batchSize == 0){
						try {
							reader.unread( sb.toString().toCharArray() );
							reader2.unread( sb2.toString().toCharArray() );
							cpManager.copyIn("COPY "+tableName+" FROM STDIN WITH CSV", reader );
							sb.delete(0,sb.length());
							cpManager.copyIn("COPY "+tableName+"_sh"+" FROM STDIN WITH CSV", reader2 );
							sb2.delete(0,sb2.length());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			counter++;
			if(counter >= modfactor && counter % modfactor == 0){
				System.out.println("Processed facility no "+counter);
				modfactor = counter;
			}
			
		}
		//write out the rest
		try {
			reader.unread( sb.toString().toCharArray() );
			reader2.unread( sb2.toString().toCharArray() );
			cpManager.copyIn("COPY "+tableName+" FROM STDIN WITH CSV", reader );
			sb.delete(0,sb.length());
			cpManager.copyIn("COPY "+tableName+"_sh"+" FROM STDIN WITH CSV", reader2 );
			sb2.delete(0,sb2.length());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Processed facility no "+counter);
		//add geometry and spatial index to the table]\
		dba.executeStatement("SELECT AddGeometryColumn( '" +tableName.split("\\.")[0]+
				"', '" +tableName.split("\\.")[1]+"_sh"+
				"', 'geom_utm48n', 32648, 'POINT', 2 ); ");
		dba.executeStatement("update  " +tableName+"_sh"+
				" set geom_utm48n=ST_SetSRID(ST_Point( x_utm48n, y_utm48n),32648) ;");
		dba.executeStatement("CREATE INDEX idx"+tableName.split("\\.")[1]+"_sh"+
				" ON "+ tableName +"_sh" + " USING GIST(geom_utm48n);");
		dba.executeStatement("CREATE INDEX idx_id_"+tableName.split("\\.")[1]+
				" ON "+ tableName + "(id);");
	}
	
	/**
	 * @param args
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoConnectionException 
	 * @deprecated
	 * 
	 * Oh no, cannot set the activity type for activities at a facility anymore
	 */
	
	public void mapActFromSQLtoXML(ResultSet rs) throws SQLException{
		rs.beforeFirst();
		while(rs.next()){
			String id = rs.getString("id");
			String actType = rs.getString("classification");
			ActivityFacility facility = this.scenario.getActivityFacilities().
					getFacilities().get(new IdImpl(id));
			Iterator<ActivityOption> ao =facility.getActivityOptions().values().iterator();
			while(ao.hasNext()){
				ActivityOptionImpl i = (ActivityOptionImpl) ao.next();
				i.setType(actType);
			}
		}
	}
	
	public void mapTimesFromSQLtoXML(ResultSet rs) throws SQLException{
//		rs.beforeFirst();
		while(rs.next()){
			String id = rs.getString("id");
			double start = rs.getDouble("starttime");
			double end = rs.getDouble("endtime");
			String actType = rs.getString("acttype");
			String dayType = rs.getString("day");
			ActivityFacility facility = this.scenario.getActivityFacilities().
					getFacilities().get(new IdImpl(id));
			Iterator<ActivityOption> ao =facility.getActivityOptions().values().iterator();
			while(ao.hasNext()){
				ActivityOptionImpl i = (ActivityOptionImpl) ao.next();
				if(i.getType().equals(actType)){
					
					i.getOpeningTimes().get(DayType.wkday).clear();
					i.getOpeningTimes().get(DayType.wkday).add(new OpeningTimeImpl(DayType.wkday, start, end));
				}
					
			}
		}
	}
//	function specifically created to strip the leading number from facility description
	public void stripDescription(){
		for(ActivityFacility fac:scenario.getActivityFacilities().getFacilities().values()){
			ActivityFacilityImpl f = (ActivityFacilityImpl) fac;
			String s = f.getDesc();
			f.setDesc(s.replaceFirst("[0-9]*:", ""));
		}
	}
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
//		DataBaseAdmin dba = new DataBaseAdmin(new File("data/matsim2.properties"));
		DataBaseAdmin dba = new DataBaseAdmin(new File("data/matsim2postgres.properties"));
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimFacilitiesReader fcr = new MatsimFacilitiesReader(scenario);
		fcr.readFile(args[0]);
		FacilitiesToSQL f2sql = new FacilitiesToSQL(dba, scenario);
//		f2sql.stripDescription();
//		f2sql.createCompleteFacilityAndActivityTable("full_facility_list");
//		f2sql.createShortSQLFacilityListPostgres(args[1]+"_short");
		f2sql.createCompleteFacilityAndActivityTablePostgres(args[1]);
//		f2sql.createShortSQLFacilityList("full_facility_list");
		
//		ResultSet rs = dba.executeQuery("select distinct id, starttime, endtime, acttype,day from u_fouriep.edu_facility_detail_08112012");
//		f2sql.mapTimesFromSQLtoXML(rs);
//		
//		FacilitiesWriter fcw =  new FacilitiesWriter(f2sql.scenario.getActivityFacilities());
//		String completeFacilitiesXMLFile = args[2];
//		fcw.write(completeFacilitiesXMLFile);
	}

}
