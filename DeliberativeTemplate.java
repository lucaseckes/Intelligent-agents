package template;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR, NAIVE }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		System.out.println(algorithm);

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			
			plan = BFSPlan(vehicle, tasks);
			break;
		case NAIVE :
			plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}
	

	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		double distance = 0;
		for (Task task : tasks) {
			distance += current.distanceTo(task.pickupCity) + task.pickupCity.distanceTo(task.deliveryCity);
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
		System.out.println("The total distance is " + distance + " km");
		return plan;
	}
	
	private int factoriel(int x) {
		int fact = 1;
		while (x > 1) {
			fact *= x;
			x--;
		}
		return fact;
	}
	
	private int[] removefromarr(int [] arr, int index) {
		// Create another array of size one less 
        int[] anotherArray = new int[arr.length - 1]; 
  
        // Copy the elements except the index 
        // from original array to the other array 
        for (int i = 0, k = 0; i < arr.length; i++) { 
  
            // if the index is 
            // the removal element index 
            if (i == index) { 
                continue; 
            } 
  
            // if the index is not 
            // the removal element index 
            anotherArray[k++] = arr[i]; 
        } 
  
        // return the resultant array 
        return anotherArray; 
    } 
	
	private int amin(ArrayList<Double> arr) {
		int idx = 0;
		double min = arr.get(0);
		for (int i = 0; i<arr.size(); i++) {
			if (arr.get(i) < min) {
				min = arr.get(i);
				idx = i;
			}
		}
		return idx;
	}

	
	
	public ArrayList<Double> BFSMatrix(Vehicle vehicle, TaskSet tasks) {
		ArrayList<Double> matrix = new ArrayList<Double>();
		ArrayList<Double> intermediate_matrix = new ArrayList<Double>();
		City current = vehicle.getCurrentCity();
		ArrayList<TaskSet> actions = new ArrayList<TaskSet>();
		ArrayList<City> states = new ArrayList<City>();
		
		
		int sz = tasks.size();
		int compt = 0;
			
		for(Task task : tasks) {
			
			double cost = current.distanceTo(task.pickupCity) + task.pickupCity.distanceTo(task.deliveryCity);
			intermediate_matrix.add(cost);
			states.add(task.deliveryCity);
			TaskSet act = tasks.copyOf(tasks);
			act.remove(task);
			actions.add(act);

			compt ++;
		}
		
		while (sz > 1) {
			sz --;
			int node = 0;
			ArrayList<Double> new_matrix = new ArrayList<Double>();
			ArrayList<TaskSet> new_actions = new ArrayList<TaskSet>();
			ArrayList<City> new_states = new ArrayList<City>();
			for (TaskSet itr : actions) {
				int test = 0;
				for(Task task : itr) {
					City new_current = states.get(node);
					double cost = intermediate_matrix.get(node) + new_current.distanceTo(task.pickupCity) + task.pickupCity.distanceTo(task.deliveryCity);
					new_matrix.add(cost);
					new_states.add(task.deliveryCity);
					TaskSet act = itr.copyOf(itr);
					act.remove(task);
					new_actions.add(act);
					test++;
				}
				node ++;
			}
			intermediate_matrix = new ArrayList<Double>(new_matrix);
			actions = new ArrayList<TaskSet>(new_actions);
			states = new ArrayList<City>(new_states);
		}
		
		matrix = new ArrayList<Double>(intermediate_matrix);
		
		return matrix;
	}
	
	private Plan BFSPlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		ArrayList<Double> tree_matrix = BFSMatrix(vehicle, tasks);
		int sz = tasks.size();
		int[] array_nb = new int[sz];
		for (int i = 0; i < sz; ++i) {
	        array_nb[i] = i;
	    }
		int index = amin(tree_matrix);
		System.out.println("The total distance is " + tree_matrix.get(index) + " km");
		while (sz > 0) {
			int task_nb = index/factoriel(sz-1);
			int real_nb = array_nb[task_nb];
			int compt = 0;
			for (Task task : tasks) {
				if (compt == real_nb) {
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
				compt ++;
			}
			index -= task_nb*factoriel(sz-1);
			array_nb = removefromarr(array_nb, task_nb);
			sz --;
		}
		
		return plan;
	}
	

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}
