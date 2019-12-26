package template;

//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import template.CentralizedTemplate.Variables;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	double [] valueFunction;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
		this.valueFunction = valueFunction();
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			currentCity = previous.deliveryCity;
		}
	}
	
	@Override
	public Long askPrice(Task task) {

		if (vehicle.capacity() < task.weight)
			return null;

		double actual_reward = valueFunction[currentCity.id];
		double future_reward = valueFunction[task.deliveryCity.id];

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask
				+ currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum
				* vehicle.costPerKm());

	
		double bid = marginalCost - (future_reward-actual_reward);
		System.out.println("bid : " + bid);
		System.out.println("reward : " + task.reward);

		return (long) Math.round(bid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
		long time_start = System.currentTimeMillis();
        
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);
        List<Plan> plans = new ArrayList<Plan>();
        
        if (tasks.isEmpty()) {
        	while (plans.size() < vehicles.size())
    			plans.add(Plan.EMPTY);
        }
        else {
        	CentralizedTemplate centralized = new CentralizedTemplate();
        	centralized.setup(topology, distribution, agent);
        	plans = centralized.plan(vehicles, tasks);
        }
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");
        
        return plans;
	}
	

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
	
	public double [] pickup_proba() {
		List<City> Cities = topology.cities(); //The list of cities are given by ascending ID number
		int size_cities = Cities.size();
		
		double [] matrix = new double[size_cities];
		for (int s=0; s<size_cities; s++) {
			for (int j = 0; j<size_cities; j++) {
			matrix[s] += distribution.probability(Cities.get(s), Cities.get(j));
			}
		}
		
		return matrix;
	}
	
	public double [][] rewardMatrix(){
		
		List<City> Cities = topology.cities(); //The list of cities are given by ascending ID number
		int size_cities = Cities.size();
		
		double [][] matrix = new double[size_cities][2];
		double [] pickup_matrix = pickup_proba();
		
		for (int s=0; s<size_cities; s++) {
			List<City> Neighbors = Cities.get(s).neighbors();
			for (int j = 0; j<size_cities; j++) {
				//Calculate the average reward of taking the pickup action in this city
				matrix[s][0] -= pickup_matrix[j]*vehicle.costPerKm()*Cities.get(s).distanceTo(Cities.get(j)); 
				for (int k = 0; k<size_cities; k++) {
					matrix[s][0] += distribution.probability(Cities.get(j), Cities.get(k))*vehicle.costPerKm()*Cities.get(j).distanceTo(Cities.get(k));
				}
			}
			for (int k = 0; k<Neighbors.size(); k++) {
				//Calculate the reward (negative value) of not taking the pickup action and going in another city 
				matrix[s][1] = 0;
			}
			matrix[s][1] = matrix[s][1]/Neighbors.size();
		} 
		return matrix;
	}
	
	public double [][][] transitionMatrix(){
		List<City> Cities;	
		Cities = topology.cities();
		int size = topology.size();
		double [] pickup_matrix = pickup_proba();
	
		double [][][]matrix = new double [size][2][size];
		double sum = 0;

		for (int i=0; i<size; i++) {
			sum = 0;
				for (int k = 0; k<size; k++) {
					for (int j=0; j<size; j++) {
						matrix[i][0][k] += pickup_matrix[j]*distribution.probability(Cities.get(j), Cities.get(k));
						sum+=matrix[i][0][k];
					}
					
					if(i == k) {
						matrix[i][1][k] = 1;
					}
					else {
						matrix[i][1][k] = 0;
							
					}
			}
			for (int k = 0; k<size; k++) {
				matrix[i][0][k] = matrix[i][0][k]/sum;
			}
				
		}

		return matrix;
		
	}
	
	public double [] valueFunction() {
		
		List<City> Cities = topology.cities(); //The list of cities are given by ascending ID number
		int size_cities = Cities.size();
		
		int niter =0;
		double epsilon = 0.01;
		int size = topology.size();
		double [] old_value = new double [size];
		double [] diff = new double [size];
		for (int i = 0; i<size; i++) {
			old_value[i] = 1;
			diff[i] = 10000;
		}
		double [] new_value = new double [size];
		double [][] rewardMatrix = rewardMatrix();
		double [][][] transitionMatrix = transitionMatrix();
		
		double [][] qArray = new double[size][2];
		while (Arrays.stream(diff).max().getAsDouble() > epsilon) {
			for (int s = 0; s<size_cities; s++) {
				for (int a = 0; a<2; a++) {
					qArray[s][a] = rewardMatrix[s][a];
					for (int s_ = 0; s_<size_cities; s_++) {
						qArray[s][a] += 0.95*transitionMatrix[s][a][s_]*old_value[s_];
						
					}
				}
				//System.out.println("test   "+Arrays.stream(qArray[s]).max().getAsDouble());
				new_value[s] = Arrays.stream(qArray[s]).max().getAsDouble();
			}
			for (int i=0; i<size; i++) {
				//System.out.println(i+"  old " +old_value[i]+"  new " +new_value[i]);
				diff[i] = Math.abs(new_value[i] - old_value[i]);
			}
			for (int i = 0; i<size; i++) {
				old_value[i] = new_value[i];
			}

			niter++;
		}
		//System.out.println("V(s) converge after " + niter + " iterations");
		//System.out.println("V(s) = " + Arrays.toString(new_value));
		return new_value;
	}
}
