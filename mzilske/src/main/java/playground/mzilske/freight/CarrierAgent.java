package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mzilske.freight.Tour.TourElement;

public class CarrierAgent {
	
	public interface CostCalculator {
		
		public Double getCost(Id from, Id to, int size);
		
		public void memorizeCost(Id from, Id to, int size, double cost);
		
	}
	
	public class CostTable implements CostCalculator {

		private Map<CostTableKey,Double> costMap = new HashMap<CarrierAgent.CostTableKey, Double>();
		
		@Override
		public Double getCost(Id from, Id to, int size) {
			Double cost = costMap.get(makeKey(from,to,size));
			return cost;
		}

		private CostTableKey makeKey(Id from, Id to, int size) {
			CostTableKey key = new CostTableKey(from, to, size);
			return key;
		}

		@Override
		public void memorizeCost(Id from, Id to, int size, double cost) {
			costMap.put(makeKey(from,to,size), cost);
		}
	}
	
	public class CostTableKey {

		private Id from;
		private Id to;
		private int size;

		public CostTableKey(Id from, Id to, int size) {
			this.from = from;
			this.to = to;
			this.size = size;
		}

		@Override
		public boolean equals(Object obj) {
			CostTableKey other = (CostTableKey) obj;
			return from.equals(other.from) && to.equals(other.to) && size == other.size;
		}

		@Override
		public int hashCode() {
			return from.hashCode() + to.hashCode() + size;
		}

	}

	private static final double EPSILON = 0.0001;

	private CarrierImpl carrier;
	
	private Collection<Id> driverIds = new ArrayList<Id>();

	private int nextId = 0;

	private PlanAlgorithm router;
	
	private Map<Id, CarrierDriverAgent> carrierDriverAgents = new HashMap<Id, CarrierDriverAgent>();
	
	private CostAllocatorImpl costAllocator = null;

	private Map<Id, ScheduledTour> driverTourMap = new HashMap<Id, ScheduledTour>();

	private CarrierAgentTracker tracker;

	private CostCalculator costTable;
	
	private static Logger logger = Logger.getLogger(CarrierAgent.class);

	private CarrierCostFunction costFunction;

	private Network network;

	private OfferMaker offerMaker;
	
	public void setOfferMaker(OfferMaker offerMaker) {
		this.offerMaker = offerMaker;
	}

	class CarrierDriverAgent {
		
		private int activityCounter = 0;
		
		private Id driverId;
		
		double distance = 0.0;
		
		double time = 0.0;
		
		double startTime = 0.0;
		

		private CarrierVehicle carrierVehicle;

		CarrierDriverAgent(Id driverId, CarrierVehicle carrierVehicle) {
			this.setCarrierVehicle(carrierVehicle);
			this.driverId = driverId;
		}

		public void activityEndOccurs(String activityType, double time) {
			Tour tour = driverTourMap.get(driverId).getTour();
			if (FreightConstants.START.equals(activityType)){
				startTime = time;
			}
			if (FreightConstants.PICKUP.equals(activityType)) {
				TourElement tourElement = tour.getTourElements().get(activityCounter);
				tracker.notifyPickup(tourElement.getShipment(), time);
				activityCounter++;
			} else if (FreightConstants.DELIVERY.equals(activityType)) {
				TourElement tourElement = tour.getTourElements().get(activityCounter);
				tracker.notifyDelivery(tourElement.getShipment(), time);
				activityCounter++;
			}
		}

		public void activityStartOccurs(String activityType, double time) {
			if(FreightConstants.END.equals(activityType)){
				time += time - startTime;
			}
			
		}

		public void tellDistance(double distance) {
			this.distance += distance;
		}
		
		public double getDistance(){
			return distance;
		}

		private void setCarrierVehicle(CarrierVehicle carrierVehicle) {
			this.carrierVehicle = carrierVehicle;
		}

		private CarrierVehicle getCarrierVehicle() {
			return carrierVehicle;
		}

		public void tellTraveltime(double time) {
			this.time += time;
		}

		double getTime() {
			return time;
		}
		
		
		
	}

	public CarrierAgent(CarrierAgentTracker carrierAgentTracker, CarrierImpl carrier, PlanAlgorithm router) {
		this.tracker = carrierAgentTracker;
		this.carrier = carrier;
		this.router = router;
	}

	public List<Plan> createFreightDriverPlans() {
		clear();
		List<Plan> plans = new ArrayList<Plan>();
		if(carrier.getSelectedPlan() == null){
			return plans;
		}
		for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
			Plan plan = new PlanImpl();
			Activity startActivity = new ActivityImpl(FreightConstants.START, scheduledTour.getVehicle().getLocation());
			startActivity.setEndTime(scheduledTour.getDeparture());
			plan.addActivity(startActivity);
			Leg startLeg = new LegImpl(TransportMode.car);
			plan.addLeg(startLeg);
			for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {
				Activity tourElementActivity = new ActivityImpl(tourElement.getActivityType(), tourElement.getLocation());
//				((ActivityImpl) tourElementActivity).setMaximumDuration(tourElement.getDuration());
//				((ActivityImpl) tourElementActivity).setEndTime(3600*24);
				((ActivityImpl) tourElementActivity).setEndTime(tourElement.getTimeWindow().getStart());
				plan.addActivity(tourElementActivity);
				Leg leg = new LegImpl(TransportMode.car);
				plan.addLeg(leg);
			}
			Activity endActivity = new ActivityImpl(FreightConstants.END, scheduledTour.getVehicle().getLocation());
			plan.addActivity(endActivity);
			Id driverId = createDriverId();
			Person driverPerson = createDriverPerson(driverId);
			plan.setPerson(driverPerson);
			route(plan);
			plans.add(plan);
			CarrierDriverAgent carrierDriverAgent = new CarrierDriverAgent(driverId, scheduledTour.getVehicle());
			carrierDriverAgents.put(driverId, carrierDriverAgent);
			driverTourMap.put(driverId, scheduledTour);
		}
		return plans;
	}
	
	private void clear() {
		carrierDriverAgents.clear();
		driverTourMap.clear();
		driverIds.clear();
		nextId=0;
		
	}

	public void setCostAllocator(CostAllocatorImpl costAllocator) {
		this.costAllocator = costAllocator;
	}

	public Collection<Id> getDriverIds() {
		return Collections.unmodifiableCollection(driverIds);
	}

	public void activityStartOccurs(Id personId, String activityType, double time) {
		carrierDriverAgents.get(personId).activityStartOccurs(activityType, time);
//		logger.info("driver had a start of an activity " + activityType + ", time=" + time);
	}

	public void activityEndOccurs(Id personId, String activityType, double time) {
		carrierDriverAgents.get(personId).activityEndOccurs(activityType, time);
//		logger.info("driver had an end of an activity " + activityType + ", time=" + time);
	}
	
	public void tellDistance(Id personId, double distance) {
		carrierDriverAgents.get(personId).tellDistance(distance);
	}
	
	public void tellTraveltime(Id personId, double time){
		carrierDriverAgents.get(personId).tellTraveltime(time);
	}

	public void scoreSelectedPlan() {
		double cost = calculateCost();
		carrier.getSelectedPlan().setScore(cost * (-1));
	}
	
	private double calculateCost() {
		costFunction.init(carrier);
		double cost = 0.0;
		for (Id driverId : getDriverIds()) {
			CarrierDriverAgent driver = carrierDriverAgents.get(driverId);
			cost += costFunction.calculateCost(driver.getCarrierVehicle(), driver.getDistance(), driver.getTime());
		}
		return cost;
	}

	public List<Tuple<Shipment, Double>> calculateCostsPerShipment() {
		logger.info("carrierId="+carrier.getId());
		List<Tuple<Shipment,Double>> listOfCostPerShipment = new ArrayList<Tuple<Shipment,Double>>();
		for(Id driverId : driverIds){
			ScheduledTour tour = driverTourMap.get(driverId);
			costFunction.init(carrier);
			CarrierDriverAgent carrierDriverAgent = carrierDriverAgents.get(driverId);
			double cost = costFunction.calculateCost(carrierDriverAgent.getCarrierVehicle(), carrierDriverAgent.getDistance(), carrierDriverAgent.getTime());
			List<Tuple<Shipment,Double>> listOfCostPerShipmentPerDriver = costAllocator.allocateCost(tour.getTour().getShipments(), cost);
			listOfCostPerShipment.addAll(listOfCostPerShipmentPerDriver);
		}
		if (! listOfCostPerShipment.isEmpty()) {
			assertSum(listOfCostPerShipment, calculateCost());
		}
		for (Tuple<Shipment,Double> t : listOfCostPerShipment) {
			memorizeCost(t.getFirst().getFrom(), t.getFirst().getTo(), t.getFirst().getSize(), t.getSecond());
			logger.info("Ich bin carrier " + carrier.getId() + " and memorize the cost of shipment="+t.getFirst()+", cost="+t.getSecond());
		}
		return listOfCostPerShipment;
	}

	private void memorizeCost(Id from, Id to, int size, Double cost) {
		if(costTable.getCost(from, to, size) == null){
			costTable.memorizeCost(from, to, size, cost);
		}
		else{
			if(costTable.getCost(from,to,size) > cost){
				costTable.memorizeCost(from, to, size, cost);
			}
		}
	}

	private void assertSum(List<Tuple<Shipment, Double>> listOfCostPerShipment, double calculateCostOfSelectedPlan) {
		double sum = 0.0;
		for (Tuple<Shipment, Double> t : listOfCostPerShipment) {
			sum += t.getSecond();
		}
		if ( Math.abs(calculateCostOfSelectedPlan - sum) > EPSILON) {
			throw new RuntimeException ("For the moment, we want the total cost to be the sum of the costs per shipment.");
		}
	}

	private Person createDriverPerson(Id driverId) {
		Person person = new PersonImpl(driverId);
		return person;
	}

	private void route(Plan plan) {
		router.run(plan);
	}

	private Id createDriverId() {
		IdImpl id = new IdImpl("fracht_"+carrier.getId()+"_"+nextId);
		driverIds.add(id);
		++nextId;
		return id;
	}

	public Offer requestOffer(Id linkId, Id linkId2, int shipmentSize, double startPickup, double endPickup, double startDelivery, double endDelivery) {
		if(requestIsNoGo(linkId,linkId2)){
			return new NoOffer();
		}
//		Double memorizedPrice = costTable.getCost(linkId, linkId2, shipmentSize);
		return offerMaker.requestOffer(linkId,linkId2,shipmentSize,startPickup,endPickup,startDelivery,endDelivery,null);
	}
	
	public Offer requestOffer(Id linkId, Id linkId2, int shipmentSize) {
		if(requestIsNoGo(linkId,linkId2)){
			return new NoOffer();
		}
		Offer offer = new Offer();
		offer.setCarrierId(carrier.getId());
		Double memorizedPrice = costTable.getCost(linkId, linkId2, shipmentSize);
		return offerMaker.requestOffer(linkId,linkId2,shipmentSize,memorizedPrice);
	}

	private boolean requestIsNoGo(Id linkId, Id linkId2) {
		if(carrier.getKnowledge().getNoGoLocations().contains(linkId2) || carrier.getKnowledge().getNoGoLocations().contains(linkId)){
			return true;
		}
		return false;
	}

	public void setCostFunction(CarrierCostFunction costFunction) {
		this.costFunction = costFunction;
	}

	public void setNetwork(Network network) {
		this.network = network;
		
	}

}
