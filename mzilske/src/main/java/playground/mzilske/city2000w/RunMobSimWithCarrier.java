package playground.mzilske.city2000w;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.CarrierPlanReader;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.mobsim.CarrierAgentTracker;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PlanAlgorithm;


public class RunMobSimWithCarrier implements StartupListener, BeforeMobsimListener, ScoringListener {

    private static Logger logger = Logger.getLogger(RunMobSimWithCarrier.class);
	
	private static String NETWORK_FILENAME;
	
	private ScenarioImpl scenario;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	public static void main(String[] args) {
		RunMobSimWithCarrier mobSim = new RunMobSimWithCarrier();
		mobSim.run();
	}
	
	private void run(){
		logger.info("run");
		init();
		Config config = new Config();
		config.addCoreModules();
		config.global().setCoordinateSystem("EPSG:32632");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.addQSimConfigGroup(new QSimConfigGroup());
//		config.simulation().setEndTime(12*3600);
		scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		readNetwork(NETWORK_FILENAME);
		Controler controler = new Controler(scenario);
		controler.setCreateGraphs(false);
		controler.addControlerListener(this);
		controler.setOverwriteFiles(true);
		
		controler.run();
	}

	private void init() {
		logger.info("initialise model");
		NETWORK_FILENAME = "../playgrounds/sschroeder/input/grid.xml";
	}

	private void readNetwork(String networkFilename) {
		new MatsimNetworkReader(scenario).readFile(networkFilename);
	}

	public void notifyStartup(StartupEvent event) {
        Carriers carriers = new Carriers();
        new CarrierPlanReader(carriers).read("../playgrounds/sschroeder/input/carrierPlans.xml");
		PlanAlgorithm router = event.getControler().createRoutingAlgorithm();
		carrierAgentTracker = new CarrierAgentTracker(carriers, scenario.getNetwork());
		// City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, carrierAgentTracker.createPlans());
		// mobsimFactory.setUseOTFVis(true);

        City2000WQSimFactory mobsimFactory = new City2000WQSimFactory(carrierAgentTracker);

		event.getControler().setMobsimFactory(mobsimFactory);
	}
	
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().addHandler(carrierAgentTracker);
        carrierAgentTracker.createPlans();
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		carrierAgentTracker.calculateCosts();
	}

}
