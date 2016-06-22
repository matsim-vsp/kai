/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.publicTransitMapping.mapping.pseudoRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.tools.CoordTools;

/**
 * A possible link for a stop facility. A LinkCandidate contains
 * theoretically a link and the parent stop facility. However, all
 * values besides Coord are stored as primitive/String since one might
 * be working with multiple mode separated networks.
 *
 * @author polettif
 */
public class LinkCandidateV2 implements LinkCandidate {

	private static PublicTransitMappingConfigGroup.TravelCostType travelCostType = PublicTransitMappingConfigGroup.TravelCostType.linkLength;

	private final String id;
	private final Id<TransitStopFacility> parentStopFacilityId;
	private final String scheduleTransportMode;

	private double priority;
	private final double stopFacilityDistance;
	private final double linkTravelCost;

	private final Id<Link> linkId;
	private final Id<Node> fromNodeId;
	private final Id<Node> toNodeId;

	private final Coord stopFacilityCoord;
	private final Coord fromNodeCoord;
	private final Coord toNodeCoord;
	private boolean loopLink;

	public LinkCandidateV2(Link link, TransitStopFacility parentStopFacility, String scheduleTransportMode) {
		this.id = parentStopFacility.getId().toString() + ".link:" + link.getId().toString();
		this.parentStopFacilityId = parentStopFacility.getId();
		this.scheduleTransportMode = scheduleTransportMode;

		this.linkId = link.getId();

		if(travelCostType.equals(PublicTransitMappingConfigGroup.TravelCostType.travelTime)) {
			this.linkTravelCost = link.getLength() / link.getFreespeed();
		} else {
			this.linkTravelCost = link.getLength();
		}

		this.fromNodeId = link.getFromNode().getId();
		this.toNodeId = link.getToNode().getId();
		this.stopFacilityCoord = parentStopFacility.getCoord();

		this.fromNodeCoord = link.getFromNode().getCoord();
		this.toNodeCoord = link.getToNode().getCoord();

		this.stopFacilityDistance = CoordUtils.distancePointLinesegment(fromNodeCoord, toNodeCoord, stopFacilityCoord);
		this.priority = 1/stopFacilityDistance;

		this.loopLink = link.getFromNode().getId().toString().equals(link.getToNode().getId().toString());
	}

	public static void setTravelCostType(PublicTransitMappingConfigGroup.TravelCostType travelCostType) {
		LinkCandidateV2.travelCostType = travelCostType;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Id<TransitStopFacility> getParentStopFacilityId() {
		return parentStopFacilityId;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public Id<Node> getToNodeId() {
		return toNodeId;
	}

	@Override
	public Id<Node> getFromNodeId() {
		return fromNodeId;
	}

	@Override
	public double getStopFacilityDistance() {
		return stopFacilityDistance;
	}

	@Override
	public double getLinkTravelCost() {
		return linkTravelCost;
	}

	@Override
	public String getScheduleTransportMode() {
		return null;
	}

	@Override
	public double getPriority() {
		return priority;
	}

	@Override
	public void setPriority(double priority) {
		this.priority = priority;
	}

	@Override
	public Coord getFromNodeCoord() {
		return fromNodeCoord;
	}

	@Override
	public Coord getToNodeCoord() {
		return toNodeCoord;
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public int compareTo(LinkCandidate other) {
		if(other.getId().equals(this.id)) {
			return 0;
		}
		int dCompare = Double.compare(stopFacilityDistance, other.getStopFacilityDistance());
		if(dCompare == 0) {
			return CoordTools.coordIsOnRightSideOfLine(stopFacilityCoord, fromNodeCoord, toNodeCoord) ? 1 : -1;
		} else {
			return dCompare;
		}
	}

	@Override
	public boolean isLoopLink() {
		return loopLink;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;

		LinkCandidateV2 other = (LinkCandidateV2) obj;
		if(id == null) {
			if(other.id != null)
				return false;
		} else if(!id.equals(other.id))
			return false;
		return true;
	}
}