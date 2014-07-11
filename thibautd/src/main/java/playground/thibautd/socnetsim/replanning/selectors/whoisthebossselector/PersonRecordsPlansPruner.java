/* *********************************************************************** *
 * project: org.matsim.*
 * PersonRecordsPlansPruner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning.selectors.whoisthebossselector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;

import playground.ivt.utils.MapUtils;

/**
 * Class responsible of deleting inplausible plans to reduce the search space
 * @author thibautd
 */
final class PersonRecordsPlansPruner {
	private PersonRecordsPlansPruner() {}

	public static void prunePlans(
			final IncompatiblePlanRecords incompatiblePlans,
			final Map<Id, PersonRecord> persons) {
		for ( PersonRecord person : persons.values() ) {
			pruneAtIndividualLevel( incompatiblePlans , person );
		}
	}

	private static void pruneAtIndividualLevel(
			final IncompatiblePlanRecords incompatiblePlans,
			final PersonRecord person) {
		final Map<Set<Id>, Set<Set<Id>>> knownBranches = new HashMap<Set<Id>, Set<Set<Id>>>();
		double lastRecordWeight = Double.POSITIVE_INFINITY;
		final Iterator<PlanRecord> iterator = person.prunedPlans.iterator();
		while ( iterator.hasNext() ) {
			final PlanRecord r = iterator.next();
			assert r.individualPlanWeight <= lastRecordWeight : person.plans;
			lastRecordWeight = r.individualPlanWeight;

			final Set<Id> cotravs = r.jointPlan == null ?
				Collections.<Id>emptySet() :
				r.jointPlan.getIndividualPlans().keySet();
			// only consider the best plan of each structure for each set of
			// incompatible plans.
			if ( !MapUtils.getSet( cotravs , knownBranches ).add( incompatiblePlans.getIncompatibilityGroups( r ) ) ) {
				iterator.remove();
			}
		}
	}
}
