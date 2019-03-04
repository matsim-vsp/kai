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
package org.matsim.contrib.greedo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.pseudosimulation.transit.NoTransitEmulator;
import org.matsim.contrib.pseudosimulation.transit.TransitEmulator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Singleton;

import ch.ethz.matsim.ier.IERModule;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Greedo {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(Greedo.class);

	// -------------------- MEMBERS --------------------

	private Config config = null;

	private Scenario scenario = null;

	// -------------------- CONSTRUCTION --------------------

	public Greedo() {
	}

	// -------------------- WIRE GREEDO INTO MATSim --------------------

	public void meet(final Config config) {

		if (this.config != null) {
			throw new RuntimeException("Have already met a config.");
		}
		this.config = config;

		/*
		 * Ensure a valid acceleration configuration; fall back to default values if not
		 * available.
		 */
		if (!config.getModules().containsKey(GreedoConfigGroup.GROUP_NAME)) {
			log.warn("Config module " + GreedoConfigGroup.GROUP_NAME + " is missing, falling back to default values.");
		}
		final GreedoConfigGroup greedoConfig = ConfigUtils.addOrGetModule(config, GreedoConfigGroup.class);

		/*
		 * Ensure that the simulation starts at iteration 0. One could relax this at the cost
		 * of probably somewhat messier code.
		 * 
		 * TODO Probably no longer necessary.
		 */
		if (config.controler().getFirstIteration() != 0) {
			config.controler()
					.setLastIteration(config.controler().getLastIteration() - config.controler().getFirstIteration());
			config.controler().setFirstIteration(0);
			log.warn("Setting firstIteration = " + config.controler().getFirstIteration());
			log.warn("Setting lastIteration = " + config.controler().getLastIteration());
		}

		/*
		 * Preliminary analysis of innovation strategies.
		 */
		int expensiveStrategyCnt = 0;
		int cheapStrategyCnt = 0;
		double cheapStrategyWeightSum = 0.0;
		for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
			final String strategyName = strategySettings.getStrategyName();
			if (strategySettings.getWeight() > 0) {
				if (greedoConfig.getExpensiveStrategyList().contains(strategyName)) {
					expensiveStrategyCnt++;
				} else if (greedoConfig.getCheapStrategyList().contains(strategyName)) {
					cheapStrategyCnt++;
					cheapStrategyWeightSum += strategySettings.getWeight();
				} 
			}
		}

		/*
		 * Adjust number of emulated iterations per cycle to number and type of
		 * innovation strategies.
		 */
		{
			final int originalIterationsPerCycle = greedoConfig.getIterationsPerCycle();
			if (cheapStrategyCnt > 0) {
				// Make sure that every strategy can be used used on average at least once.
				greedoConfig.setIterationsPerCycle(
						Math.max(expensiveStrategyCnt + cheapStrategyCnt, originalIterationsPerCycle));
			} else {
				if (expensiveStrategyCnt > 0) {
					// Only best-response strategies: every best-response strategy is used on
					// average exactly once.
					greedoConfig.setIterationsPerCycle(expensiveStrategyCnt);
				} else {
					// No innovation strategies at all!
					log.warn("No relevant strategies recognized.");
				}
			}
			if (greedoConfig.getIterationsPerCycle() != originalIterationsPerCycle) {
				log.warn("Adjusted number of emulated iterations per cycle from " + originalIterationsPerCycle + " to "
						+ greedoConfig.getIterationsPerCycle() + ".");
			}
		}

		/*
		 * Adjust iteration numbers to pSim iteration overhead.
		 */
		// config.controler()
		// .setLastIteration(config.controler().getLastIteration() *
		// accelerationConfig.getIterationsPerCycle());
		// config.controler().setWriteEventsInterval(
		// config.controler().getWriteEventsInterval() *
		// accelerationConfig.getIterationsPerCycle());
		// config.controler().setWritePlansInterval(
		// config.controler().getWritePlansInterval() *
		// accelerationConfig.getIterationsPerCycle());
		// config.controler().setWriteSnapshotsInterval(
		// config.controler().getWriteSnapshotsInterval() *
		// accelerationConfig.getIterationsPerCycle());
		// log.warn("Adjusting iteration numbers in config.controler() "
		// + "under the assumption that pSim iteration were so far not accounted for:");
		// log.warn(" lastIteration = " + config.controler().getLastIteration());
		// log.warn(" writeEventsInterval = " +
		// config.controler().getWriteEventsInterval());
		// log.warn(" writeSnapshotsInterval = " +
		// config.controler().getWriteSnapshotsInterval());
		// log.warn(" writePlansInterval = " +
		// config.controler().getWritePlansInterval());

		if (greedoConfig.getAdjustStrategyWeights()) {

			/*
			 * Use minimal choice set and always remove the worse plan. This probably as
			 * close as it can get to best-response in the presence of random innovation
			 * strategies.
			 */
			config.strategy().setMaxAgentPlanMemorySize(1);
			config.strategy().setPlanSelectorForRemoval("WorstPlanSelector");
			log.warn("Approximating a best-response simulation through the following settings:");
			log.warn("  maxAgentPlanMemorySize = 1");
			log.warn("  planSelectorForRemoval = worstPlanSelector");

			/*
			 * Keep only plan innovation strategies. Re-weight for maximum emulation efficiency.
			 */
			final double singleExpensiveStrategyProba = 1.0 / greedoConfig.getIterationsPerCycle();
			final double cheapStrategyProbaSum = 1.0 - singleExpensiveStrategyProba * expensiveStrategyCnt;
			final double cheapStrategyWeightFactor = cheapStrategyProbaSum / cheapStrategyWeightSum;
			double probaSum = 0;
			for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
				final String strategyName = strategySettings.getStrategyName();
				if (greedoConfig.getExpensiveStrategyList().contains(strategyName)) {
					strategySettings.setWeight(singleExpensiveStrategyProba);
				} else if (greedoConfig.getCheapStrategyList().contains(strategyName)) {
					strategySettings.setWeight(cheapStrategyWeightFactor * strategySettings.getWeight());
				} else {
					strategySettings.setWeight(0.0); // i.e., dismiss
				}
				log.warn("Setting weight of strategy " + strategyName + " to " + strategySettings.getWeight() + ".");
				probaSum += strategySettings.getWeight();
			}
			if (Math.abs(1.0 - probaSum) >= 1e-8) {
				throw new RuntimeException("The sum of all strategy probabilities is " + probaSum + ".");
			}
		}
	}

	public void meet(final Scenario scenario) {

		if (this.config == null) {
			throw new RuntimeException("First meet the config.");
		} else if (this.scenario != null) {
			throw new RuntimeException("Have already met the scenario.");
		}
		this.scenario = scenario;

		ConfigUtils.addOrGetModule(this.config, GreedoConfigGroup.class).configure(this.scenario,
				this.scenario.getNetwork().getLinks().keySet(),
				this.scenario.getTransitVehicles().getVehicles().keySet());
	}

	public AbstractModule[] getModules() {
		final AbstractModule greedoModule = new AbstractModule() {
			@Override
			public void install() {
				// TODO For now only car traffic!
				// if (this.config.transit().isUseTransit()) {
				// // TODO See warning below.
				// log.warn("Transit is included -- this is only tested with deterministic SBB
				// transit.");
				// this.bind(FifoTransitPerformance.class);
				// this.addEventHandlerBinding().to(FifoTransitPerformance.class);
				// this.bind(TransitEmulator.class).to(FifoTransitEmulator.class);
				// } else {
				log.warn("Experimental code; no transit emulation!");
				this.bind(TransitEmulator.class).to(NoTransitEmulator.class);
				// }

				this.bind(WireGreedoIntoMATSimControlerListener.class).in(Singleton.class); // is a singleton anyway
				this.addEventHandlerBinding().to(WireGreedoIntoMATSimControlerListener.class);
			}			
		};
		final AbstractModule ierModule = new IERModule(WireGreedoIntoMATSimControlerListener.class);
		return new AbstractModule[] {greedoModule, ierModule}; // TODO Is it not possible to combine them?
	}
}
