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
	private int nb_states;
	private int nb_actions;

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
		this.nb_states = myTopology.size();
		this.nb_actions = 2;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		double [] policy = new double [2];
		if (availableTask != null ) {
			City currentCity = vehicle.getCurrentCity();
			for(int a = 0; a<nb_actions; a++) {
				policy[a] = rewardMatrix[currentCity.id][a];
				for (int s_ = 0; s_<nb_states; s_++) {
					policy[a] += pPickup*transitionMatrix[currentCity.id][a][s_]*valueFunction[currentCity.id];
				}
			}
			if (policy[0]>policy[1]) {
				action = new Pickup(availableTask);
			}
			else {
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
	
	public double [][] rewardMatrix(){
		double [][] matrix = new double[nb_states][nb_actions];
		List<City> Cities = myTopology.cities(); //The list of cities are given by ascending ID number
	
		for (int s=0; s<nb_states; s++) {
			List<City> Neighbors = Cities.get(s).neighbors();
			for (int j = 0; j<nb_states; j++) {
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
		double sum = 0;
		double min1 = 0;
		double sum1 =0;
		double min2 = 0;
		double sum2 =0;
		double [][][]matrix = new double [nb_states][nb_actions][nb_states];
		double []norm1 = new double [nb_states];
		double []norm2 = new double [nb_states];
		
		
		for (int i=0; i<nb_states; i++) {
			for (int j=0; j<nb_actions; j++) {
				for (int k = 0; k<nb_states; k++) {
					
					matrix[i][0][k] = myDistribution.probability(Cities.get(i), Cities.get(k));
					if(!(Cities.get(i).hasNeighbor(Cities.get(k)))) {
						matrix[i][1][k] = 0;
					}
					else {
						for (int n=0; n<nb_states; n++) {
							sum += myDistribution.probability(Cities.get(k), Cities.get(n));
							
						}
							
						matrix[i][1][k] = 1;
						
						
						
					}
					sum = 0;
				}
				
			}
			//add min-max normalization between 0 and 1 
			norm1 = matrix[i][0];
			norm2 = matrix[i][1];
			min1 =Arrays.stream(norm1).min().getAsDouble();
			sum1 =Arrays.stream(norm1).sum();
			min2 =Arrays.stream(norm2).min().getAsDouble();
			sum2 =Arrays.stream(norm2).sum();
			for (int k = 0; k<nb_states; k++) {
				norm1[k] = (norm1[k]- min1)/(sum1-min1);
				norm2[k] = (norm2[k]- min2)/(sum2-min2);
			}
			matrix[i][0] = norm1;
			matrix[i][1] = norm2;
		}
		
	
	
		return matrix;
		
	}
	
	public double [] valueFunction() {
		double epsilon = 1;
		double [] old_value = new double [nb_states];
		for (int i = 0; i<nb_states; i++) {
			old_value[i] = rewardMatrix[i][0];
		}
		double [] new_value = new double [nb_states];
		double [] diff = old_value;
		double [][] qArray = new double[nb_states][nb_actions];
		while (Arrays.stream(diff).max().getAsDouble() > epsilon) {
			for (int s = 0; s<nb_states; s++) {
				for (int a = 0; a<nb_actions; a++) {
					qArray[s][a] = rewardMatrix[s][a];
					for (int s_ = 0; s_<nb_states; s_++) {
						qArray[s][a] += pPickup*transitionMatrix[s][a][s_]*old_value[s_];
					}
				}
				new_value[s] = Arrays.stream(qArray[s]).max().getAsDouble();
			}
			for (int i=0; i<9; i++) {
				diff[i] = Math.abs(new_value[i] - old_value[i]);
			}
			old_value = new_value;
		}
		System.out.println(Arrays.toString(new_value));
		return new_value;
	}
		
	
	
}
