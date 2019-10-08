package template;

import java.util.List;
import java.util.Random;
import java.util.Arrays;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private double cost_per_km;
	private int numActions;
	private Agent myAgent;
	private TaskDistribution myDistribution;
	private Topology myTopology;
	private double [][] rewardMatrix;
	private double [][][] transitionMatrix;
	private double [] valueFunction;
	private boolean dummy1;
	private boolean dummy2;
	private int compt;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = 0.95;
		this.cost_per_km = 5;
		this.numActions = 0;
		this.myAgent = agent;
		this.myDistribution = td;
		this.myTopology = topology;
		this.rewardMatrix = rewardMatrix();
		this.transitionMatrix = transitionMatrix();
		this.valueFunction = valueFunction();
		this.dummy1 = false;
		this.dummy2 = false;
		this.compt = 0;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		if (dummy1 == true && dummy2 == true) {
			System.out.println("Error : Need to choose between one of the two dummies");
			action = new Move(vehicle.getCurrentCity());
			return action;
		}
		else if (dummy1 == true) {
			if (availableTask == null || random.nextDouble() > pPickup) {
				City currentCity = vehicle.getCurrentCity();
				action = new Move(currentCity.randomNeighbor(random));
			} else {
				action = new Pickup(availableTask);
			}
			
			if (numActions >= 1) {
				System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
			}
			numActions++;
			
			return action;
		}
		else if (dummy2 == true) {
			if (availableTask == null) {
				City currentCity = vehicle.getCurrentCity();
				action = new Move(currentCity.randomNeighbor(random));
			} else {
				action = new Pickup(availableTask);
			}
			
			if (numActions >= 1) {
				System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
			}
			numActions++;
			
			return action;
		}
		else {
			double [] policy = new double [2];
			if (availableTask != null ) {
				City currentCity = vehicle.getCurrentCity();
				for(int a = 0; a<2; a++) {
					policy[a] = rewardMatrix[currentCity.id][a];
					for (int s_ = 0; s_<9; s_++) {
						policy[a] += pPickup*transitionMatrix[currentCity.id][a][s_]*valueFunction[currentCity.id];
					}
				}
				System.out.println(policy[0]);
				System.out.println(policy[1]);
				if (policy[0]>policy[1]) {
					action = new Pickup(availableTask);
				}
				else {
					compt += 1;
					System.out.println("Number of tasks refused = " + compt);
					action = new Move(currentCity.randomNeighbor(random));
				}
			} else {
				City currentCity = vehicle.getCurrentCity();
				action = new Move(currentCity.randomNeighbor(random));
			}
		
			if (numActions >= 1) {
				System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
			}
			numActions++;
		
			return action;
		}
	}
	
	public double [][] rewardMatrix(){
		double [][] matrix = new double[9][2];
		List<City> Cities = myTopology.cities(); //The list of cities are given by ascending ID number
	
		for (int s=0; s<9; s++) {
			List<City> Neighbors = Cities.get(s).neighbors();
			for (int j = 0; j<9; j++) {
				//Calculate the average reward of taking the pickup action in this city
				matrix[s][0] += myDistribution.probability(Cities.get(s), Cities.get(j))*(myDistribution.reward(Cities.get(s), Cities.get(j))-cost_per_km*Cities.get(s).distanceTo(Cities.get(j))); 
			}
			for (int k = 0; k<Neighbors.size(); k++) {
				//Calculate the reward (negative value) of not taking the pickup action and going in another city 
				matrix[s][1] -= cost_per_km * (Cities.get(s).distanceTo(Neighbors.get(k)));
			}
			matrix[s][1] = matrix[s][1]/Neighbors.size();
		} 
		return matrix;
	}
	
	public double [][][] transitionMatrix(){
		List<City> Cities;	
		Cities = myTopology.cities();
		int size = myTopology.size();
		double sum = 0;
		double min = 0;
		double []norm = new double [size];
		int sizeNeighbors = 0;
	
		double [][][]matrix = new double [size][2][size];

		for (int i=0; i<size; i++) {
			
			sizeNeighbors = Cities.get(i).neighbors().size();
			for (int j=0; j<2; j++) {
				for (int k = 0; k<size; k++) {
					
					matrix[i][0][k] = myDistribution.probability(Cities.get(i), Cities.get(k));
					
					
					if(!(Cities.get(i).hasNeighbor(Cities.get(k)))) {
						matrix[i][1][k] = 0;
					}
					else {
						matrix[i][1][k] = 1/sizeNeighbors;
							
					}
				}
					
			}
			for (int l=0; l<size; l++) {
				norm[l] = matrix[i][0][l];
			}
			min =Arrays.stream(norm).min().getAsDouble();
			sum =Arrays.stream(norm).sum();
			for (int k = 0; k<size; k++) {
				norm[k] = (norm[k]- min)/(sum-min);
			}
			for (int m = 0; m<size; m++) {
				matrix[i][0][m] = norm[m];
			}
				
		}

		return matrix;
		
	}
	
	public double [] valueFunction() {
		int niter =0;
		double epsilon = 0.01;
		int size = myTopology.size();
		double [] old_value = new double [size];
		double [] diff = new double [size];
		for (int i = 0; i<size; i++) {
			old_value[i] = 1;
			diff[i] = 10000;
		}
		double [] new_value = new double [size];
		
		double [][] qArray = new double[size][2];
		while (Arrays.stream(diff).max().getAsDouble() > epsilon) {
			for (int s = 0; s<9; s++) {
				for (int a = 0; a<2; a++) {
					qArray[s][a] = rewardMatrix[s][a];
					for (int s_ = 0; s_<9; s_++) {
						qArray[s][a] += pPickup*transitionMatrix[s][a][s_]*old_value[s_];
						
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
		System.out.println("V(s) converge after " + niter + " iterations");
		System.out.println("V(s) = " + Arrays.toString(new_value));
		return new_value;
	}
		
	
	
}