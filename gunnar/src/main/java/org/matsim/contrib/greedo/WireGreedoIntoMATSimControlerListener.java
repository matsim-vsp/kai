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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.greedo.listeners.SlotUsageListener;
import org.matsim.contrib.greedo.logging.AgePercentile;
import org.matsim.contrib.greedo.logging.AvgAge;
import org.matsim.contrib.greedo.logging.AvgAgeWeight;
import org.matsim.contrib.greedo.logging.AvgExpectedDeltaUtilityAccelerated;
import org.matsim.contrib.greedo.logging.AvgExpectedDeltaUtilityTotal;
import org.matsim.contrib.greedo.logging.AvgExpectedDeltaUtilityUniform;
import org.matsim.contrib.greedo.logging.AvgNonReplannerSize;
import org.matsim.contrib.greedo.logging.AvgNonReplannerUtilityChange;
import org.matsim.contrib.greedo.logging.AvgRealizedDeltaUtility;
import org.matsim.contrib.greedo.logging.AvgRealizedUtility;
import org.matsim.contrib.greedo.logging.AvgReplannerSize;
import org.matsim.contrib.greedo.logging.BetaScaled;
import org.matsim.contrib.greedo.logging.LambdaBar;
import org.matsim.contrib.greedo.logging.LambdaRealized;
import org.matsim.contrib.greedo.logging.LogDataWrapper;
import org.matsim.contrib.greedo.logging.NormalizedUnweightedCountDifferences2;
import org.matsim.contrib.greedo.logging.NormalizedUnweightedNonReplannerCountDifferences2;
import org.matsim.contrib.greedo.logging.NormalizedUnweightedReplannerCountDifferences2;
import org.matsim.contrib.greedo.logging.NormalizedWeightedCountDifferences2;
import org.matsim.contrib.greedo.logging.NormalizedWeightedNonReplannerCountDifferences2;
import org.matsim.contrib.greedo.logging.NormalizedWeightedReplannerCountDifferences2;
import org.matsim.contrib.greedo.logging.RelativeSlotVariability;
import org.matsim.contrib.greedo.logging.RelativeUtilityEfficiency;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.selectors.BestPlanSelector;

import com.google.inject.Inject;
import com.google.inject.Provider;

import ch.ethz.matsim.ier.replannerselection.ReplannerSelector;
import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;
import utils.ReplanningEfficiencyEstimator;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
@Singleton
public class WireGreedoIntoMATSimControlerListener implements Provider<EventHandler>, ReplannerSelector {

	// -------------------- MEMBERS --------------------

	private final MatsimServices services;

	private final GreedoConfigGroup greedoConfig;

	private final StatisticsWriter<LogDataWrapper> statsWriter;

	private final Utilities utilities;

	private final Ages ages;

	private final SlotUsageListener physicalSlotUsageListener;

	private final List<SlotUsageListener> hypotheticalSlotUsageListeners = new LinkedList<>();

	private Plans lastPhysicalPopulationState = null;

	// below only for logging

	private Double realizedUtilityChangeSum = null;

	private Double realizedUtilitySum = null;

	// TODO HARDWIRED
	private final ReplanningEfficiencyEstimator replanningEfficiencyEstimator = new ReplanningEfficiencyEstimator(0.01,
			0.01);

	private ReplannerIdentifier.LastExpectations lastExpectations = new ReplannerIdentifier.LastExpectations(null, null,
			null, null, null, null, null, null, null, null, null, null);

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public WireGreedoIntoMATSimControlerListener(final MatsimServices services) {

		this.services = services;
		this.greedoConfig = ConfigUtils.addOrGetModule(this.services.getConfig(), GreedoConfigGroup.class);
		this.utilities = new Utilities();
		this.ages = new Ages(services.getScenario().getPopulation().getPersons().keySet(), this.greedoConfig);
		this.physicalSlotUsageListener = new SlotUsageListener(this.greedoConfig.newTimeDiscretization(),
				this.ages.getWeights(), this.greedoConfig.getConcurrentLinkWeights(),
				this.greedoConfig.getConcurrentTransitVehicleWeights());

		this.statsWriter = new StatisticsWriter<>(
				new File(services.getConfig().controler().getOutputDirectory(), "acceleration.log").toString(), false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new LambdaRealized());
		this.statsWriter.addSearchStatistic(new LambdaBar());
		this.statsWriter.addSearchStatistic(new BetaScaled());
		this.statsWriter.addSearchStatistic(new AvgAge());
		this.statsWriter.addSearchStatistic(new AvgAgeWeight());
		this.statsWriter.addSearchStatistic(new AvgReplannerSize());
		this.statsWriter.addSearchStatistic(new AvgNonReplannerSize());
		this.statsWriter.addSearchStatistic(new NormalizedUnweightedCountDifferences2());
		this.statsWriter.addSearchStatistic(new NormalizedUnweightedReplannerCountDifferences2());
		this.statsWriter.addSearchStatistic(new NormalizedUnweightedNonReplannerCountDifferences2());
		this.statsWriter.addSearchStatistic(new NormalizedWeightedCountDifferences2());
		this.statsWriter.addSearchStatistic(new NormalizedWeightedReplannerCountDifferences2());
		this.statsWriter.addSearchStatistic(new NormalizedWeightedNonReplannerCountDifferences2());
		this.statsWriter.addSearchStatistic(new AvgNonReplannerUtilityChange());
		this.statsWriter.addSearchStatistic(new AvgRealizedUtility());
		this.statsWriter.addSearchStatistic(new AvgRealizedDeltaUtility());
		this.statsWriter.addSearchStatistic(new AvgExpectedDeltaUtilityTotal());
		this.statsWriter.addSearchStatistic(new AvgExpectedDeltaUtilityUniform());
		this.statsWriter.addSearchStatistic(new AvgExpectedDeltaUtilityAccelerated());
		this.statsWriter.addSearchStatistic(new RelativeUtilityEfficiency());
		this.statsWriter.addSearchStatistic(new RelativeSlotVariability());
		for (int percent = 5; percent <= 95; percent += 5) {
			this.statsWriter.addSearchStatistic(new AgePercentile(percent));
		}
	}

	// -------------------- IMPLEMENTATION OF ReplannerSelector --------------------

	private LogDataWrapper lastLogDataWrapper = null;

	@Override
	public EventHandlerProvider prepareReplanningAndGetEventHandlerProvider() {

		this.lastPhysicalPopulationState = new Plans(this.services.getScenario().getPopulation());
		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
			// TODO Hedge against selectedPlan == null ?
			this.utilities.updateRealizedUtility(person.getId(),
					this.lastPhysicalPopulationState.getSelectedPlan(person.getId()).getScore());
		}

		final int popSize = this.services.getScenario().getPopulation().getPersons().size();
		final Utilities.SummaryStatistics utilityStats = this.utilities.newSummaryStatistics();
		if (utilityStats.validExpectedUtilityCnt == popSize) {
			this.realizedUtilitySum = utilityStats.realizedUtilitySum;
		}
		if (utilityStats.validExpectedUtilityChangeCnt == popSize) {
			this.realizedUtilityChangeSum = utilityStats.realizedUtilityChangeSum;
		}
		if ((utilityStats.validExpectedUtilityChangeCnt == popSize)
				&& (utilityStats.validRealizedUtilityChangeCnt == popSize)) {
			this.replanningEfficiencyEstimator.update(utilityStats.realizedUtilityChangeSum,
					utilityStats.expectedUtilityChangeSum);
		}

		this.lastLogDataWrapper = new LogDataWrapper(this, this.lastExpectations);
		this.statsWriter.writeToFile(this.lastLogDataWrapper);

		this.hypotheticalSlotUsageListeners.clear(); // Redundant, see afterReplanning().
		return new EventHandlerProvider() {
			@Override
			public synchronized EventHandler get(final Set<Id<Person>> personIds) {
				final SlotUsageListener listener = new SlotUsageListener(greedoConfig.newTimeDiscretization(),
						ages.getWeights().entrySet().stream().filter(entry -> personIds.contains(entry.getKey()))
								.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())),
						greedoConfig.getConcurrentLinkWeights(), greedoConfig.getConcurrentTransitVehicleWeights());
				listener.resetOnceAndForAll(iteration());
				hypotheticalSlotUsageListeners.add(listener);
				return listener;
			}
		};
	}

	// private Double beta = null;
	private Double betaNumerator = null;
	private Double betaDenominator = null;

	private Double tryMSAUpdate(final Double oldMean, final Double innovation, final int iteration) {
		if (innovation == null) {
			return oldMean;
		} else if (oldMean == null) {
			return innovation;
		} else {
			final double innoWeight = 1.0 / (1.0 + iteration);
			return (1.0 - innoWeight) * oldMean + innoWeight * innovation;
		}
	}

	@Override
	public void afterReplanning() {

		if (this.greedoConfig.getAdjustStrategyWeights()) {
			final BestPlanSelector<Plan, Person> bestPlanSelector = new BestPlanSelector<>();
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				person.setSelectedPlan(bestPlanSelector.selectPlan(person));
				PersonUtils.removeUnselectedPlans(person);
			}
		}

		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
			this.utilities.updateExpectedUtility(person.getId(), person.getSelectedPlan().getScore());
		}
		final Utilities.SummaryStatistics utilityStats = this.utilities.newSummaryStatistics();

		// >>>>> SANITY TEST >>>>>
		final double val1 = utilityStats.expectedUtilityChangeSum;
		final double val2 = utilityStats.personId2expectedUtilityChange.values().stream().mapToDouble(x -> x).sum();
		Logger.getLogger(this.getClass()).info("val1 = " + val1 + ", val2 = " + val2);
		// <<<<< SANITY TEST <<<<<

		final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> hypotheticalSlotUsageIndicators = new LinkedHashMap<>();
		for (SlotUsageListener listener : this.hypotheticalSlotUsageListeners) {
			hypotheticalSlotUsageIndicators.putAll(listener.getNewIndicatorView());
		}
		this.hypotheticalSlotUsageListeners.clear(); // Release as soon as possible.

		// final double meanLambda = Double.NaN;
		// if
		// (GreedoConfigGroup.StepSizeType.adaptive.equals(this.greedoConfig.getStepSizeField()))
		// {
		// Logger.getLogger(this.getClass())
		// .info("relativeSlotVariability = " +
		// this.lastLogDataWrapper.getRelativeSlotVariability());
		// Logger.getLogger(this.getClass())
		// .info("relativeUtilityEfficiency = " +
		// this.lastLogDataWrapper.getRelativeUtilityEfficiency());
		// // meanLambda = this.replanningEfficiencyEstimator.getLambda();
		// if ((this.lastLogDataWrapper.getRelativeSlotVariability() != null)
		// && (this.lastLogDataWrapper.getRelativeUtilityEfficiency() != null)) {
		// meanLambda = Math.min(1.0, Math.max(0.0, 0.5 *
		// this.lastLogDataWrapper.getRelativeSlotVariability()
		// / this.lastLogDataWrapper.getRelativeUtilityEfficiency()));
		// } else {
		// meanLambda = 0.5;
		// }
		// Logger.getLogger(this.getClass()).info("meanLambda = " + meanLambda);
		// } else if
		// (GreedoConfigGroup.StepSizeType.exogenous.equals(this.greedoConfig.getStepSizeField()))
		// {
		// meanLambda = this.greedoConfig.getExogenousReplanningRate(this.iteration());
		// } else {
		// throw new RuntimeException("Unknown step size logic: " +
		// this.greedoConfig.getStepSizeField());
		// }

		this.betaNumerator = this.tryMSAUpdate(this.betaNumerator, this.lastLogDataWrapper.getEstimatedBetaNumerator(),
				this.iteration());
		this.betaDenominator = this.tryMSAUpdate(this.betaDenominator,
				this.lastLogDataWrapper.getEstimatedBetaDenominator(), this.iteration());
		final Double betaScaled;
		if ((this.betaNumerator != null) && (this.betaDenominator != null)) {
			betaScaled = this.greedoConfig.getBetaScale() * (this.betaNumerator / Math.max(this.betaDenominator, 1e-8));
		} else {
			betaScaled = null;
		}

		final Double overrideLambda;
		if (GreedoConfigGroup.StepSizeType.exogenous.equals(this.greedoConfig.getStepSizeField())) {
			overrideLambda = this.greedoConfig.getExogenousReplanningRate(this.iteration());
		} else {
			overrideLambda = null;
		}

		final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(overrideLambda, betaScaled,
				this.greedoConfig, this.iteration(), this.physicalSlotUsageListener.getNewIndicatorView(),
				hypotheticalSlotUsageIndicators, this.services.getScenario().getPopulation(),
				utilityStats.personId2expectedUtilityChange);
		final Set<Id<Person>> replannerIds = replannerIdentifier.drawReplanners();
		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
			if (!replannerIds.contains(person.getId())) {
				this.lastPhysicalPopulationState.set(person);
			}
		}

		this.lastExpectations = replannerIdentifier.getLastExpectations();
		this.ages.update(replannerIds);
		this.physicalSlotUsageListener.updatePersonWeights(this.ages.getWeights());
	}

	// --------------- IMPLEMENTATION OF Provider<EventHandler> ---------------

	@Override
	public EventHandler get() {
		// Expecting this to be called only once; returning always the same instance.
		return this.physicalSlotUsageListener;
	}

	// -------------------- GETTERS, MAINLY FOR LOGGING --------------------

	private int iteration() {
		return this.physicalSlotUsageListener.getLastResetIteration();
	}

	public Double getRealizedUtilityChangeSum() {
		return this.realizedUtilityChangeSum;
	}

	public Double getRealizedUtilitySum() {
		return this.realizedUtilitySum;
	}

	public List<Integer> getSortedAgesView() {
		return this.ages.getSortedAges();
	}

	public Double getAveragAge() {
		return this.ages.getAverageAge();
	}

	public Double getAverageWeight() {
		return this.ages.getAverageWeight();
	}
}
