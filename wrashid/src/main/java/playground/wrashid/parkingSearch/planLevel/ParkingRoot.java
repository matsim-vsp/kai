package playground.wrashid.parkingSearch.planLevel;

import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkLayer;

import playground.wrashid.lib.GlobalRegistry;

public class ParkingRoot {

	private static ClosestParkingMatrix cpm = null;
	private static LinkParkingFacilityAssociation lpfa = null;

	public static void init(ActivityFacilitiesImpl facilities, NetworkLayer network) {
		cpm = new ClosestParkingMatrix(facilities, network);
		lpfa = new LinkParkingFacilityAssociation(facilities, network);
	}
	
	public static ClosestParkingMatrix getClosestParkingMatrix(){
		if (cpm==null){
			throw new Error("Please initialize the variales first.");
		}
		
		return cpm;
	}
	
	public static LinkParkingFacilityAssociation getLinkParkingFacilityAssociation(){
		if (lpfa==null){
			throw new Error("Please initialize the variales first.");
		}
		
		return lpfa;
	}	

}
