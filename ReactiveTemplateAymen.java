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
	private int numActions;
	private Agent myAgent;
	private TaskDistribution myDistribution;
	private Topology myTopology;
	private double [][] rewardMatrix;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		this.myDistribution = td;
		this.myTopology = topology;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

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
	
	
	
	//iterator = myTopology.iterator();
	//while(iterator.hasNext) 
	
	public double [][] rewardMatrix(){
		List<City> Cities;	
		Cities = myTopology.cities();
		int size = myTopology.size();
		double [][]matrix = new double [size][2];
		
		for (int i=0; i < size; i++ ) {
			List<City> Neighbors = Cities.get(i).neighbors();
			for (int j=0; j < size; j++ ) {
				matrix[i][0] += myDistribution.probability(Cities.get(i), Cities.get(j)) * myDistribution.reward(Cities.get(i), Cities.get(j)) ; 
				matrix[i][1] += 1;
				
			}
			for (int k = 0; k<Neighbors.size(); k++) {
				//Calculate the reward (negative value) of not taking the pickup action and going in another city 
				matrix[i][1] -= Cities.get(i).distanceTo(Neighbors.get(k));
			}
			matrix[i][1] = matrix[i][1]/Neighbors.size();
			
			
		}
	
		return matrix;
		
	}
	
	
	public double [][][] stateTransitionMatrix(){
		List<City> Cities;	
		Cities = myTopology.cities();
		int size = myTopology.size();
		double sum = 0;
		double min = 0;
		double max =0;
		double [][][]matrix = new double [size][2][size];
		double []norm = new double [size];
		
		
		for (int i=0; i<size; i++) {
			for (int j=0; j<2; j++) {
				for (int k = 0; k<size; k++) {
					
					matrix[i][0][k] = myDistribution.probability(Cities.get(i), Cities.get(k));
					if(!(Cities.get(i).hasNeighbor(Cities.get(k)))) {
						matrix[i][1][k] = 0;
					}
					else {
						for (int n=0; n<size; n++) {
							sum += myDistribution.probability(Cities.get(k), Cities.get(n));
							
						}
							
						matrix[i][1][k] = sum;
						
						
						
					}
					sum = 0;
				}
				//add min-max normalization between 0 and 1 
				norm = matrix[i][1];
				for (int k = 0; k<size; k++) {
					min =Arrays.stream(norm).min().getAsDouble();
					max =Arrays.stream(norm).max().getAsDouble();
					
					norm[k] = (norm[k]- min)/(max-min);
				}
				
				
			}
		}
		
	
	
		return matrix;
		
	}
	
	
	
	//public List<City> Cities;	
	//Cities = myTopology.cities;	
	

	
	
}
