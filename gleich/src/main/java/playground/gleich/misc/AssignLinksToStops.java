package playground.gleich.misc;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class AssignLinksToStops {

	public static void main(String[] args) {
		String networkFile = "/home/gregor/git/matsim/examples/scenarios/mielec/network.xml";
		String inputScheduleFile = "/home/gregor/git/matsim/examples/scenarios/mielec/drtstops.xml";
		String outputScheduleFile = "/home/gregor/git/matsim/examples/scenarios/mielec/drtstops_wLinkIds_lessStops.xml";
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(inputScheduleFile);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		
		List<TransitStopFacility> toBeRemoved = new ArrayList<>();
		
		for (TransitStopFacility stop: scenario.getTransitSchedule().getFacilities().values()) {
			if (stop.getLinkId() == null) {
				stop.setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), stop.getCoord()).getId());
			}
			
			if (Math.random() < 0.5) {
				toBeRemoved.add(stop);
			}
		}
		
		for (TransitStopFacility stop: toBeRemoved) {
			scenario.getTransitSchedule().removeStopFacility(stop);
		}
		
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(outputScheduleFile);
	}

}
