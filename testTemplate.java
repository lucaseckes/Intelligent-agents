package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

//get out the reward and after that multiply by the marginal cost
import java.util.Arrays;


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
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		//List<Long> Bids = Arrays.asList(bids);
		//Long a = Bids.stream().max(Long::compare).get();
		if (winner == agent.id()) {
			currentCity = previous.deliveryCity;
		}
		
//		Vehicle vehicle0 = agent.vehicles().get(0);
//		Vehicle vehicle1 = agent.vehicles().get(0);
//		
//		Long price1 = askPriceVehicles(vehicle0, task);
//		Long price2 = askPriceVehicles(vehicle1, task);
	}
	
	@Override
	public Long askPrice(Task task) {

		if (vehicle.capacity() < task.weight)
			return null;

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask
				+ currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum
				* vehicle.costPerKm());

		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid2 = ratio * marginalCost;
		double bid = marginalCost + reward(task).get(0);
		System.out.println("Bid: "+bid+" marginal: "+marginalCost+" reward: "+reward(task).get(0));
		System.out.println("Bid 2: "+bid2);
		double ratio1 = marginalCost/reward(task).get(0) * 100;
		double ratio2 = bid2/reward(task).get(0) * 100;
		System.out.println("ratio 1: "+ratio1+" %");
		System.out.println("ratio 2: "+ratio2+" %");
		System.out.println("reward of task: "+task.reward);
		
		return (long) Math.round(bid);
	}
	
	public Long askPriceVehicles(Vehicle vehicle, Task task) {

		if (vehicle.capacity() < task.weight)
			return null;

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask
				+ currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum
				* vehicle.costPerKm());

		//double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		//double bid = ratio * marginalCost;
		double bid = marginalCost + reward(task).get(vehicle.id());

		return (long) Math.round(bid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);

		
		//Vehicle vehicle0 = agent.vehicles().get(0);
		Vehicle vehicle1 = agent.vehicles().get(0);
		
		//Plan planVehicle0 = naivePlan(vehicle0, tasks);
		Plan planVehicle1 = naivePlan(vehicle1, tasks);
		
		//Plan planVehicle1 = naivePlan(vehicle, tasks);
		
		
//		for (Task task : tasks) {
//			System.out.println("V0: "+askPriceVehicles(agent.vehicles().get(0), task));
//			System.out.println("V1: "+askPriceVehicles(agent.vehicles().get(1), task));
//			
//			System.out.println("allloooo   "+agent.vehicles().stream().map(v -> askPriceVehicles(v,task)).collect(Collectors.toList()));
//			//System.out.println("allloooo   "+agent.vehicles().stream().map(v -> askPriceVehicles(v,task)).max(Long::compare).get());
//			System.out.println(agent.vehicles().stream().map(v -> v.getCurrentCity()).collect(Collectors.toList()));
//		}
		
		//vehicle = agent.vehicles().get(1);
		//tasks.stream().map(t-> askPrice(t));
		//agent.vehicles().stream().map(v -> askPriceVehicles(v, ); 
		
		List<Plan> plans = new ArrayList<Plan>();
		//plans.add(planVehicle0);
		plans.add(planVehicle1);
		while (plans.size() < vehicles.size())
			plans.add(Plan.EMPTY);

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
	
	public List<Double> reward(Task task){
		List<Double> reward = new ArrayList<Double>(2);
		
		
		Vehicle vehicle0 = agent.vehicles().get(0);
		Vehicle vehicle1 = agent.vehicles().get(1);
		
		double  value0 = 0;
		double  value1 = 0;
		
		List<City> Cities = topology.cities(); //The list of cities are given by ascending ID number
		int size_cities = Cities.size();
		int actual_city_id0 = vehicle0.getCurrentCity().id;
		int actual_city_id1 = vehicle1.getCurrentCity().id;
		
		List<City> Neighbors_actual0 = Cities.get(actual_city_id0).neighbors();
		List<City> Neighbors_actual1 = Cities.get(actual_city_id1).neighbors();
		
		List<City> Neighbors_delivery = Cities.get(task.deliveryCity.id).neighbors();
		
		//Vehicle 0
		for(int i=0; i<Neighbors_actual0.size(); i++) {
			for(int j=0; j<size_cities; j++) {
				
				value0 -= distribution.probability(Neighbors_actual0.get(i), Cities.get(j))*(distribution.reward(Neighbors_actual0.get(i), Cities.get(j))-vehicle.costPerKm()*Cities.get(j).distanceTo(Neighbors_actual0.get(i)));
				
			}
		}
		
		//add the actual city 
		for(int j=0; j<size_cities; j++) {
		
			value0 -= distribution.probability(Cities.get(actual_city_id0), Cities.get(j))*(distribution.reward(Cities.get(actual_city_id0), Cities.get(j))-vehicle.costPerKm()*Cities.get(j).distanceTo(Cities.get(actual_city_id0)));
		}
		
		
		for(int i=0; i<Neighbors_delivery.size(); i++) {
			for(int j=0; j<size_cities; j++) {
				
				value0 += distribution.probability(Neighbors_delivery.get(i), Cities.get(j))*(distribution.reward(Neighbors_delivery.get(i), Cities.get(j)));
				//add the actual city 
			}
		}
		
		//add the delivery city 
		for(int j=0; j<size_cities; j++) {
			
			value0 += distribution.probability(Cities.get(task.deliveryCity.id), Cities.get(j))*(distribution.reward(Cities.get(task.deliveryCity.id), Cities.get(j)));
		}
		
		//Vehicle 1
		for(int i=0; i<Neighbors_actual1.size(); i++) {
			for(int j=0; j<size_cities; j++) {
				
				value1 -= distribution.probability(Neighbors_actual1.get(i), Cities.get(j))*(distribution.reward(Neighbors_actual1.get(i), Cities.get(j))-vehicle.costPerKm()*Cities.get(j).distanceTo(Neighbors_actual1.get(i)));
				
			}
		}
		
		//add the actual city 
		for(int j=0; j<size_cities; j++) {
		
			value1 -= distribution.probability(Cities.get(actual_city_id1), Cities.get(j))*(distribution.reward(Cities.get(actual_city_id1), Cities.get(j))-vehicle.costPerKm()*Cities.get(j).distanceTo(Cities.get(actual_city_id1)));
		}
		
		
		for(int i=0; i<Neighbors_delivery.size(); i++) {
			for(int j=0; j<size_cities; j++) {
				
				value1 += distribution.probability(Neighbors_delivery.get(i), Cities.get(j))*(distribution.reward(Neighbors_delivery.get(i), Cities.get(j)));
				
			}
		}
		
		//add the delivery city 
		for(int j=0; j<size_cities; j++) {
			
			value1 += distribution.probability(Cities.get(task.deliveryCity.id), Cities.get(j))*(distribution.reward(Cities.get(task.deliveryCity.id), Cities.get(j)));
		}

		
//		for (int s=0; s<size_cities; s++) {
//			List<City> Neighbors = Cities.get(s).neighbors();
//			for (int j = 0; j<size_cities; j++) {
//				//Calculate the average reward of taking the pickup action in this city
//				matrix += distribution.probability(Cities.get(s), Cities.get(j))*(distribution.reward(Cities.get(s), Cities.get(j))-vehicle.costPerKm()*Cities.get(s).distanceTo(Cities.get(j))); 
//			}
//			for (int k = 0; k<Neighbors.size(); k++) {
//				//Calculate the reward (negative value) of not taking the pickup action and going in another city 
//				matrix -= vehicle.costPerKm() * (Cities.get(s).distanceTo(Neighbors.get(k)));
//			}
//			matrix = matrix/Neighbors.size();
//		} 
		
		reward.add(0, value0);
		reward.add(1, value1);
		
		return reward;
	}
	
	
//	public double [][] rewardMatrix(){
//		double [][] matrix = new double[9][2];
//		List<City> Cities = topology.cities(); //The list of cities are given by ascending ID number
//		int size_cities = Cities.size();
//		
//		for (int s=0; s<size_cities; s++) {
//			List<City> Neighbors = Cities.get(s).neighbors();
//			for (int j = 0; j<size_cities; j++) {
//				//Calculate the average reward of taking the pickup action in this city
//				matrix[s][0] += distribution.probability(Cities.get(s), Cities.get(j))*(distribution.reward(Cities.get(s), Cities.get(j))-vehicle.costPerKm()*Cities.get(s).distanceTo(Cities.get(j))); 
//			}
//			for (int k = 0; k<Neighbors.size(); k++) {
//				//Calculate the reward (negative value) of not taking the pickup action and going in another city 
//				matrix[s][1] -= vehicle.costPerKm() * (Cities.get(s).distanceTo(Neighbors.get(k)));
//			}
//			matrix[s][1] = matrix[s][1]/Neighbors.size();
//		} 
//		return matrix;
//	}
	
//	public double [][][] transitionMatrix(){
//		List<City> Cities;	
//		Cities = topology.cities();
//		int size = topology.size();
//		double sizeNeighbors = 0;
//	
//		double [][][]matrix = new double [size][2][size];
//
//		for (int i=0; i<size; i++) {
//			sizeNeighbors = Cities.get(i).neighbors().size();
//			for (int j=0; j<2; j++) {
//				for (int k = 0; k<size; k++) {
//					
//					matrix[i][0][k] = distribution.probability(Cities.get(i), Cities.get(k));
//					
//					
//					if(!(Cities.get(i).hasNeighbor(Cities.get(k)))) {
//						matrix[i][1][k] = 0;
//					}
//					else {
//						matrix[i][1][k] = 1/sizeNeighbors;
//							
//					}
//				}	
//			}	
//		}
//
//		return matrix;	
//	}
//	
//	public double [] valueFunction() {
//		int niter =0;
//		double epsilon = 0.01;
//		int size = topology.size();
//		double [] old_value = new double [size];
//		double [] diff = new double [size];
//		for (int i = 0; i<size; i++) {
//			old_value[i] = 1;
//			diff[i] = 10000;
//		}
//		double [] new_value = new double [size];
//		double [][] rewardMatrix = rewardMatrix();
//		double [][][] transitionMatrix = transitionMatrix();
//		
//		double [][] qArray = new double[size][2];
//		while (Arrays.stream(diff).max().getAsDouble() > epsilon) {
//			for (int s = 0; s<9; s++) {
//				for (int a = 0; a<2; a++) {
//					qArray[s][a] = rewardMatrix[s][a];
//					for (int s_ = 0; s_<9; s_++) {
//						qArray[s][a] += 0.95*transitionMatrix[s][a][s_]*old_value[s_];
//						
//					}
//				}
//				//System.out.println("test   "+Arrays.stream(qArray[s]).max().getAsDouble());
//				new_value[s] = Arrays.stream(qArray[s]).max().getAsDouble();
//			}
//			for (int i=0; i<size; i++) {
//				//System.out.println(i+"  old " +old_value[i]+"  new " +new_value[i]);
//				diff[i] = Math.abs(new_value[i] - old_value[i]);
//			}
//			for (int i = 0; i<size; i++) {
//				old_value[i] = new_value[i];
//			}
//
//			niter++;
//		}
//		//System.out.println("V(s) converge after " + niter + " iterations");
//		//System.out.println("V(s) = " + Arrays.toString(new_value));
//		return new_value;
//	}
}
