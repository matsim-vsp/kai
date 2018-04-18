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
package cba.trianglenet;

import java.util.ArrayList;
import java.util.List;
import floetteroed.utilities.Units;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TourSequence {

	final List<Tour> tours = new ArrayList<>();

	TourSequence() {
	}

	List<Tour.Act> getTourPurposes() {
		final List<Tour.Act> purposes = new ArrayList<>(this.tours.size());
		for (Tour tour : this.tours) {
			purposes.add(tour.act);
		}
		return purposes;
	}

	synchronized Plan asPlan(final Scenario scenario, final Id<Link> homeLinkId, final Person person) {

		final Plan result = scenario.getPopulation().getFactory().createPlan();

		/*-
		 * Initialize within uniform departure times. To ensure uniformity also for the 
		 * 24h wrap-around, set the first departure to half the departure time interval.
		 * 
		 * On tour:   |-------- dpt1a --------------- dpt1b -------|  => 24h / 2
		 * 
		 * Two tours: |-- dpt1a ---- dpt1b ---- dpt2a ---- dpt2b --|  => 24h / 4
		 * 
		 * N tours: 24h / (2N).
		 */
		final int dptTimeInc_s = (int) (Units.S_PER_D / (2 * this.tours.size()));
		int dptTime_s = dptTimeInc_s / 2;

		// first home activity
		final Activity home = scenario.getPopulation().getFactory().createActivityFromLinkId(Tour.Act.home.toString(),
				homeLinkId);
		home.setEndTime(dptTime_s);
		result.addActivity(home);
		dptTime_s += dptTimeInc_s;

		// tours, with *intermediate* home2 stops
		for (int i = 0; i < this.tours.size(); i++) {
			final Tour tour = this.tours.get(i);

			result.addLeg(scenario.getPopulation().getFactory().createLeg(tour.mode.toString()));

			final Activity tourAct = scenario.getPopulation().getFactory().createActivityFromLinkId(tour.act.toString(),
					tour.destination.getId());
			tourAct.setEndTime(dptTime_s);
			result.addActivity(tourAct);
			dptTime_s += dptTimeInc_s;

			result.addLeg(scenario.getPopulation().getFactory().createLeg(tour.mode.toString()));

			if (i < this.tours.size() - 1) {
				final Activity home2 = scenario.getPopulation().getFactory()
						.createActivityFromLinkId(Tour.Act.home2.toString(), homeLinkId);
				home2.setEndTime(dptTime_s);
				result.addActivity(home2);
				dptTime_s += dptTimeInc_s;
			}
		}

		// last home activity
		result.addActivity(
				scenario.getPopulation().getFactory().createActivityFromLinkId(Tour.Act.home.toString(), homeLinkId));
		result.setPerson(person);

		// >>>>>>>>>> TODO working original >>>>>>>>>>
		//
		// for (Tour tour : this.tours) {
		//
		// final Activity home = scenario.getPopulation().getFactory()
		// .createActivityFromLinkId(Tour.Act.home.toString(), homeLinkId);
		// home.setEndTime(dptTime_s);
		// // home.setEndTime(dptTimes_s.removeFirst());
		// result.addActivity(home);
		// dptTime_s += dptTimeInc_s;
		//
		// result.addLeg(scenario.getPopulation().getFactory().createLeg(tour.mode.toString()));
		//
		// final Activity tourAct =
		// scenario.getPopulation().getFactory().createActivityFromLinkId(tour.act.toString(),
		// tour.destination.getId());
		// tourAct.setEndTime(dptTime_s);
		// result.addActivity(tourAct);
		// dptTime_s += dptTimeInc_s;
		//
		// result.addLeg(scenario.getPopulation().getFactory().createLeg(tour.mode.toString()));
		// }
		//
		// result.addActivity(
		// scenario.getPopulation().getFactory().createActivityFromLinkId(Tour.Act.home.toString(),
		// homeLinkId));
		// result.setPerson(person);
		//
		// <<<<<<<<<< TODO working original <<<<<<<<<<

		return result;
	}
}
