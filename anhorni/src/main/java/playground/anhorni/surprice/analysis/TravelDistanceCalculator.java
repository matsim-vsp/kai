package playground.anhorni.surprice.analysis;

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

import java.util.ArrayList;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class TravelDistanceCalculator extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private double sumLength = 0.0;
	private int cntTrips = 0;
	private final Network network;
	private final ArrayList<Double> travelDistances = new ArrayList<Double>();

	public TravelDistanceCalculator(final Network network) {
		this.network = network;
	}

	@Override
	public void run(final Person person) {
		this.run(person.getSelectedPlan());
	}

	@Override
	public void run(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				Route route = leg.getRoute();
								
				if (route != null && route instanceof  NetworkRoute) {
					double dist = RouteUtils.calcDistance((NetworkRoute) route, this.network);
					if (route.getEndLinkId() != null && route.getStartLinkId() != route.getEndLinkId()) {
						double d = this.network.getLinks().get(route.getEndLinkId()).getLength();
						dist += d;
						this.travelDistances.add(d);
					}
					this.sumLength += dist;
					this.cntTrips++;
				}
			}
		}
	}

	public double getAverageTripLength() {
		if (this.cntTrips == 0) {
			return 0;
		}
		return (this.sumLength / this.cntTrips);
	}

	public ArrayList<Double> getTravelDistances() {
		return travelDistances;
	}
}

