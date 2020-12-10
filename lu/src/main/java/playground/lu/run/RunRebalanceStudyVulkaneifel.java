package playground.lu.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.lu.targetLinkSelection.PredeterminedLinkSelectionModule;
import playground.lu.unitCapacityMatching.SimpleUnitCapacityRequestInserterModule;

public class RunRebalanceStudyVulkaneifel {
	private static final Logger log = Logger.getLogger(RunRebalanceStudyVulkaneifel.class);
	private static final String OUTPUT_DIRECTORY_HEADING = "./output/RebalanceStrategyComparison/"; // TODO this will
																									// write to the
																									// github repository
	private static final String VEHICLE_FILE_HEADING = "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\drtVehicles\\snz-scenario_vulkaneifel";
	private static final String VEHICLE_FILE_ENDING = "veh_4seats.xml.gz";

	private final static int REBALANCE_INTERVAL = 120;

	private static final String[] REBLANCE_STRATEGIES = { "Feedforward", "PlusOne", "Adaptive", "MinCostFlow",
			"NoRebalance" };
	private static final String[] FLEET_SIZES = { "500" };

	public static void main(String[] args) {
		if (args.length == 0) {
			args = new String[] { "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\drtOnly.baseconfig.xml" };
		}

		for (int i = 0; i < REBLANCE_STRATEGIES.length; i++) {
			for (int j = 0; j < FLEET_SIZES.length; j++) {
				String rebalanceStrategy = REBLANCE_STRATEGIES[i];
				String fleetSize = FLEET_SIZES[j];

				Config config = ConfigUtils.loadConfig(args[0], new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
				config.controler().setOutputDirectory(OUTPUT_DIRECTORY_HEADING + rebalanceStrategy + "-" + fleetSize);

				for (DrtConfigGroup drtConfigGroup : MultiModeDrtConfigGroup.get(config).getModalElements()) {
					// Set Fleet size (i.e. set vehicles file)
					drtConfigGroup.setVehiclesFile(VEHICLE_FILE_HEADING + fleetSize + VEHICLE_FILE_ENDING);

					RebalancingParams rebalancingParams = drtConfigGroup.getRebalancingParams()
							.orElse(new RebalancingParams());
					rebalancingParams.setInterval(REBALANCE_INTERVAL);
					rebalancingParams.setMinServiceTime(3600);
					rebalancingParams.setMaxTimeBeforeIdle(120);

					// Set rebalancing Strategy
					switch (rebalanceStrategy) {
						case "Feedforward":
							log.info("Feedforward rebalancing strategy is used");
							RebalanceStudyUtils.prepareFeedforwardStrategy(rebalancingParams);
							break;
						case "PlusOne":
							log.info("Plus One rebalancing strategy is used");
							RebalanceStudyUtils.preparePlusOneStrategy(rebalancingParams);
							break;
						case "Adaptive":
							log.info("Adaptive Real Time rebalancing strategy is used");
							RebalanceStudyUtils.prepareAdaptiveRealTimeStrategy(rebalancingParams);
							break;
						default:
							log.info("No rebalancing strategy is used");
							RebalanceStudyUtils.prepareNoRebalance(rebalancingParams);
							break;
					}
				}

				MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
				DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(),
						config.plansCalcRoute());
				Scenario scenario = ScenarioUtils.createScenario(config);
				scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
						new DrtRouteFactory());
				ScenarioUtils.loadScenario(scenario);

				Controler controler = new Controler(scenario);
				controler.addOverridingModule(new DvrpModule());
				controler.addOverridingModule(new MultiModeDrtModule());
				controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

				for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
					double maxEuclideanDistance = 3000;
					if (rebalanceStrategy == "PlusOne") {
						maxEuclideanDistance = 100000;
					}
					controler.addOverridingQSimModule(
							new SimpleUnitCapacityRequestInserterModule(drtCfg, maxEuclideanDistance));
					controler.addOverridingModule(new PredeterminedLinkSelectionModule(drtCfg));
				}
				controler.run();
			}
		}

	}

}
