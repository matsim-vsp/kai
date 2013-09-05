/* *********************************************************************** *
 * project: org.matsim.*
 * DgAverageTravelTimeSpeed
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class TTTotalDelay implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler{

	private Network network;
	private Map<Id, LinkEnterEvent> linkEnterByPerson;
	private double totalDelay;

	public TTTotalDelay(Network network) {
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.linkEnterByPerson = new HashMap<Id, LinkEnterEvent>();
		this.totalDelay = 0.0;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		//calculate total delay for signalized links, so the delay caused by the signals
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			LinkEnterEvent linkEnterEvent = this.linkEnterByPerson.remove(event.getPersonId());
			Link link = this.network.getLinks().get(event.getLinkId());
			double freespeedTravelTime = link.getLength()/link.getFreespeed();
			if (linkEnterEvent != null) {
				this.totalDelay += event.getTime() - linkEnterEvent.getTime() - freespeedTravelTime;
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			this.linkEnterByPerson.put(event.getPersonId(), event);
		}
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		this.linkEnterByPerson.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.linkEnterByPerson.remove(event.getPersonId());		
	}

	
	public double getTotalDelay() {
		return totalDelay;
	}

}
