/*
 * Copyright 2018 Gunnar Flötteröd
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
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package gunnar.wum.analysis;

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import gunnar.wum.malin.LineUsageStatistics;
import gunnar.wum.malin.VehiclesPerLineIdentifier;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class MalinLineAnalyzer {

	static void savePopulationSubset(final String fromFile, final String toFile, final Set<Id<Person>> keepPersons) {
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(fromFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Set<Id<Person>> removePersonIds = new LinkedHashSet<>(scenario.getPopulation().getPersons().keySet());
		removePersonIds.removeAll(keepPersons);
		for (Id<Person> removeId : removePersonIds) {
			scenario.getPopulation().getPersons().remove(removeId);
		}
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
		writer.write(toFile);
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		final String baseCasePath = "C:\\Users\\GunnarF\\NoBackup\\data-workspace\\wum\\2019-11-16_base-case\\";
		final String malinPath = "C:\\Users\\GunnarF\\NoBackup\\data-workspace\\wum\\2019-11-16_malin-scenario\\";

		final Config config = ConfigUtils.createConfig();
		config.transit().setTransitScheduleFile(malinPath + "output_transitSchedule.xml.gz");
		config.transit().setVehiclesFile(malinPath + "output_transitVehicles.xml"); // replaced PC equiv. NaN by 1.0

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Set<Id<Vehicle>> vehicleIds = new LinkedHashSet<>();
		final VehiclesPerLineIdentifier vehicleIdentifier = new VehiclesPerLineIdentifier(scenario);
		vehicleIds.addAll(vehicleIdentifier.getVehicles("malin_2b"));
		System.out.println("Analyzing users of the following vehicles: " + vehicleIds);

		final LineUsageStatistics lineUsageStats = new LineUsageStatistics(vehId -> vehicleIds.contains(vehId),
				time_s -> true, personId -> !personId.toString().startsWith("pt_"), stopId -> true, scenario);

		final EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(lineUsageStats);
		EventsUtils.readEvents(manager, malinPath + "output_events.xml.gz");
		System.out.println(lineUsageStats.getTravelers().size() + " travelers:");
		for (Id<Person> lineUserId : lineUsageStats.getTravelers()) {
			System.out.println(lineUserId);
		}
		// StringBuffer allLineUsers = new StringBuffer();
		// final List<Id<Person>> lineUsersList = new
		// ArrayList<>(lineUsageStats.getTravelers());
		// if (lineUsageStats.getTravelers().size() > 0) {
		// allLineUsers.append(lineUsersList.get(0));
		// for (int i = 1; i < lineUsageStats.getTravelers().size(); i++) {
		// allLineUsers.append(",");
		// allLineUsers.append(lineUsersList.get(i));
		// }
		// }
		// System.out.println();
		// System.out.println(allLineUsers.toString());
		System.out.println();
		System.out.println(lineUsageStats.getEntryExitLog());

		savePopulationSubset(baseCasePath + "output_plans.xml.gz", baseCasePath + "malin_plans.xml.gz",
				lineUsageStats.getTravelers());
		savePopulationSubset(malinPath + "output_plans.xml.gz", malinPath + "malin_plans.xml.gz",
				lineUsageStats.getTravelers());

		System.out.println("... DONE");
	}

}
