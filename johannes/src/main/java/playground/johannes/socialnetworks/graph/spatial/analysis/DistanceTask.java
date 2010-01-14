/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzerTask;
import playground.johannes.socialnetworks.graph.analysis.GraphPropertyFactory;
import playground.johannes.socialnetworks.graph.spatial.Distance;

/**
 * @author illenberger
 *
 */
public class DistanceTask implements GraphAnalyzerTask {

	private static final Logger logger = Logger.getLogger(DistanceTask.class);
	
	public static final String MEAN_EDGE_LENGTH = "d_mean";
	
	public static final String MIN_EDGE_LENGTH = "d_min";
	
	public static final String MAX_EDGE_LENGTH = "d_max";
	
	public static final String MEAN_EDGE_LENGTH_I = "d_i_mean";
	
	public static final String MIN_EDGE_LENGTH_I = "d_i_min";
	
	public static final String MAX_EDGE_LENGTH_I = "d_i_max";
	
	@SuppressWarnings("unchecked")
	@Override
	public void analyze(Graph graph, GraphPropertyFactory factory, Map<String, Double> stats) {
		if(factory instanceof SpatialGraphPropertyFactory) {
			Distance distance = ((SpatialGraphPropertyFactory)factory).newDistance();
			
			Distribution distr = distance.distribution((Set<? extends SpatialVertex>) graph.getVertices());
			double d_mean = distr.mean();
			double d_max = distr.max();
			double d_min = distr.min();
			stats.put(MEAN_EDGE_LENGTH, d_mean);
			stats.put(MAX_EDGE_LENGTH, d_max);
			stats.put(MIN_EDGE_LENGTH, d_min);
			
			logger.info(String.format("d_mean = %1$.4f, d_max = %2$.4f, d_min = %3$.4f", d_mean, d_max, d_min));
			
			distr = distance.vertexAccumulatedDistribution((Set<? extends SpatialVertex>) graph.getVertices());
			double d_i_mean = distr.mean();
			double d_i_max = distr.max();
			double d_i_min = distr.min();
			stats.put(MEAN_EDGE_LENGTH_I, d_i_mean);
			stats.put(MAX_EDGE_LENGTH_I, d_i_max);
			stats.put(MIN_EDGE_LENGTH_I, d_i_min);
			
			logger.info(String.format("d_i_mean = %1$.4f, d_i_max = %2$.4f, d_i_min = %3$.4f", d_i_mean, d_i_max, d_i_min));
		} else {
			logger.warn("This is no spatial graph property factory! Cannot analyze spatial indicators.");
		}

	}

}
