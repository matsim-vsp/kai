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
package playground.vsp.analysis.modules.simpleTripAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * A simple analysis-class for a very basic MATSim-Scenario, i.e it should be used 
 * with physical simulation of car-trips only. All other modes must be teleported. Thus,
 * this class will throw a runtime-exception when {@link ScenarioConfigGroup#isUseTransit()} is true. 
 * 
 * @author droeder
 *
 */
public class SimpleTripAnalyzerModule extends AbstractAnalyisModule{

	private SimpleTripAnalyzer analyzer;
	private Population p;
	private Map<String, Map<Integer, Integer>> dist;
	private Map<String, Map<Integer, Integer>> tt;

	public SimpleTripAnalyzerModule(Config c, Network net, Population p) {
		super("SimpleTripAnalyzer");
		this.analyzer = new SimpleTripAnalyzer(c, net); 
		this.p = p;
	}
	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(SimpleTripAnalyzerModule.class);

	

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> l = new ArrayList<EventHandler>();
		l.add(analyzer);
		return l;
	}

	@Override
	public void preProcessData() {
		analyzer.reset(-1);
	}

	@Override
	public void postProcessData() {
		analyzer.run(p);
		this.dist = calcWriteDistanceDistribution(analyzer.getTraveller());
		this.tt = calcAndWriteTTDistribution(analyzer.getTraveller());
	}

	@Override
	public void writeResults(String outputFolder) {
		analyzer.dumpData(new File(outputFolder).getAbsolutePath() + System.getProperty("file.separator"), null);
		dumpData(this.dist, new File(outputFolder).getAbsolutePath() + System.getProperty("file.separator") + "distanceShare.csv.gz");
		dumpData(this.tt, new File(outputFolder).getAbsolutePath() + System.getProperty("file.separator") + "ttShare.csv.gz");
	}
	
	/**
	 * @param traveller
	 */
	private Map<String, Map<Integer, Integer>> calcAndWriteTTDistribution(Map<Id, Traveller> traveller) {
		@SuppressWarnings("serial")
		List<Integer> distribution =  new ArrayList<Integer>(){{
			add(0);
			add(300);
			add(600);
			add(900);
			add(1200);
			add(1500);
			add(1800);
			add(2700);
			add(3600);
			add(5400);
			add(7200);
			add(Integer.MAX_VALUE);
		}};
		Map<String, Map<Integer, Integer>> map = new TreeMap<String, Map<Integer,Integer>>();
		for(Traveller t :traveller.values()){
			for(Trip trip : t.getTrips()){
				Map<Integer, Integer> temp = getColumn(map, distribution, trip.getMode());
				increase(temp, trip.getDuration());
			}
		}
		return map;
	}
	
	private Map<String, Map<Integer, Integer>> calcWriteDistanceDistribution(Map<Id, Traveller> traveller) {
		@SuppressWarnings("serial")
		List<Integer> distribution =  new ArrayList<Integer>(){{
			add(0);
			add(100);
			add(200);
			add(500);
			add(1000);
			add(2000);
			add(5000);
			add(10000);
			add(20000);
			add(50000);
			add(100000);
			add(Integer.MAX_VALUE);
		}};
		Map<String, Map<Integer, Integer>> map = new TreeMap<String, Map<Integer,Integer>>();
		for(Traveller t :traveller.values()){
			for(Trip trip : t.getTrips()){
				Map<Integer, Integer> temp = getColumn(map, distribution, trip.getMode());
				increase(temp, trip.getDist());
			}
		}
		return map;
	}
	
	/**
	 * @param map
	 * @param file
	 */
	private void dumpData(Map<String, Map<Integer, Integer>> map, String file) {
		BufferedWriter w = IOUtils.getBufferedWriter(file);
		Map<Integer, Integer> header = map.values().iterator().next();
		try {
			w.write(";");
			for(Integer i :header.keySet()){
				w.write(i + ";");
			}
			w.write("\n");
			for(Entry<String, Map<Integer, Integer>> e: map.entrySet()){
				w.write(e.getKey() +";");
				for(Integer i: e.getValue().values()){
					w.write(i + ";");
				}w.write("\n");
			}
			w.flush();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param temp
	 * @param dist
	 */
	private void increase(Map<Integer, Integer> temp, Double value) {
		for(Integer i : temp.keySet()){
			if(value <= i){
				temp.put(i, temp.get(i) + 1);
				return;
			}
		}
	}
	
	
	
	private Map<Integer, Integer> getColumn(Map<String, Map<Integer,Integer>> map, List<Integer> distribution, String mode){
		if(map.containsKey(mode)) return map.get(mode);
		Map<Integer, Integer> temp = new LinkedHashMap<Integer, Integer>();
		for(Integer i : distribution){
			temp.put(i, 0);
		}
		map.put(mode, temp);
		return temp;
	}

}

