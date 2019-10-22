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
import java.util.List;
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
	
	//Compute the factorial of a positive integer number
	private int factoriel(int x) {
		int fact = 1;
		while (x > 1) {
			fact *= x;
			x--;
		}
		return fact;
	}
	
	//Remove an integer value of a matrix knowing the index
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
	
	//Find the index of the minimum value of an ArrayList
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
	
	//Allow to return a pair of objects (the matrix of cost and the references list) in the method BFSMatrix
	class Pair { 
		ArrayList<Double> matrix;  
		ArrayList<String> refs;  
	    Pair(ArrayList<Double> a, ArrayList<String> b) 
	    { 
	        matrix = a;
	        refs = b;
	    } 
	} 

	
	//Calculate a matrix of all possibilities with the cost associated to it
	public Pair BFSMatrix(Vehicle vehicle, TaskSet tasks) {
		ArrayList<Double> matrix = new ArrayList<Double>(); //The matrix with cost values
		ArrayList<Double> intermediate_matrix = new ArrayList<Double>();
		ArrayList<String> references = new ArrayList<String>(); //The references corresponding to all the actions made by the agent
		City current = vehicle.getCurrentCity();
		double load = vehicle.capacity(); 
		ArrayList<Integer> current_capacity = new ArrayList<Integer>();
		ArrayList<TaskSet> current_tasks = new ArrayList<TaskSet>();
		ArrayList<TaskSet> actions = new ArrayList<TaskSet>();
		ArrayList<City> states = new ArrayList<City>();
		
		int sz = 2*tasks.size(); //The depth of the Breadth first search
		int compt = 1; //the index of the tasks iterated
			
		//First level of the Breadth First Search. The branches correspond to all possible tasks
		for(Task task : tasks) {
			
			double cost = current.distanceTo(task.pickupCity);
			current_capacity.add(task.weight);
			intermediate_matrix.add(cost);
			references.add("P" + Integer.toString(compt));
			states.add(task.pickupCity);
			TaskSet other_tasks = tasks.copyOf(tasks);
			other_tasks.remove(task);
			actions.add(other_tasks);
			TaskSet loading = tasks.copyOf(tasks);
			loading.clear();
			loading.add(task);
			current_tasks.add(loading);

			compt ++;
		}
		
		//One loop in the while loop represents one level of depthness
		while (sz>1) {
			sz --;
			int node = 0;
			ArrayList<Double> new_matrix = new ArrayList<Double>();
			ArrayList<TaskSet> new_actions = new ArrayList<TaskSet>();
			ArrayList<TaskSet> new_current_tasks = new ArrayList<TaskSet>();
			ArrayList<City> new_states = new ArrayList<City>();
			ArrayList<Integer> weights = new ArrayList<Integer>();
			ArrayList<String> new_references = new ArrayList<String>();
			//itr represents all the tasks that are not picked up in one particular node
			for (TaskSet itr : actions) {
				int has_done = 0;
				int comptD = 1;
				ArrayList<Task> has_done_actions = new ArrayList<Task>();
				//current_tasks represents all the tasks that are picked up but not delivered in a particular node
				for(Task task : current_tasks.get(node)) {
					City new_current = states.get(node);
					TaskSet point_actions = itr.copyOf(itr);
					List<City> path = new_current.pathTo(task.deliveryCity);
					TaskSet loading = current_tasks.get(node).copyOf(current_tasks.get(node));
					//Test if one of the task that were not picked up are on the path of the delivery city
					int comptP = 1;
					for(Task task_path : itr) {
						if (path.contains(task_path.pickupCity) & has_done < current_tasks.get(node).size()) {
							//Test if picking up the task will not exceed the load of the agent
							int total_weight = current_capacity.get(node) + task_path.weight;
							if (total_weight < load) {
								point_actions.remove(task_path);
								double cost = intermediate_matrix.get(node)+ new_current.distanceTo(task_path.pickupCity);
								new_matrix.add(cost);
								new_actions.add(point_actions);
								new_states.add(task_path.pickupCity);
								loading.add(task_path);
								has_done_actions.add(task_path);
								new_current_tasks.add(loading);
								weights.add(current_capacity.get(node)+task_path.weight);
								new_references.add(references.get(node) + "P" + Integer.toString(comptP));
								has_done ++;
							}
						}
						comptP++;
					}
					
					if (has_done < current_tasks.get(node).size()) {
						new_actions.add(point_actions);
						double cost = intermediate_matrix.get(node)+ new_current.distanceTo(task.deliveryCity);
						new_matrix.add(cost);
						new_states.add(task.deliveryCity);
						loading.remove(task);
						new_current_tasks.add(loading);
						weights.add(current_capacity.get(node)-task.weight);
						new_references.add(references.get(node) + "D" + Integer.toString(comptD));
						comptD++;
						has_done ++;
					}
				}
				compt = 1;
				for(Task task : itr) {
					City new_current = states.get(node);
					int total_weight = current_capacity.get(node) + task.weight;
					if (total_weight < load & has_done_actions.contains(task) == false) {
						double cost = intermediate_matrix.get(node) + new_current.distanceTo(task.pickupCity);
						new_matrix.add(cost);
						new_states.add(task.pickupCity);
						TaskSet act = itr.copyOf(itr);
						act.remove(task);
						new_actions.add(act);
						TaskSet loading_pick = current_tasks.get(node).copyOf(current_tasks.get(node));
						loading_pick.add(task);
						new_current_tasks.add(loading_pick);
						weights.add(total_weight);
						new_references.add(references.get(node) + "P" + Integer.toString(compt));
						compt++;
					}
				}
				node ++;
			}
			intermediate_matrix = new ArrayList<Double>(new_matrix);
			System.out.println(intermediate_matrix.size());
			current_tasks = new ArrayList<TaskSet>(new_current_tasks);
			actions = new ArrayList<TaskSet>(new_actions);
			states = new ArrayList<City>(new_states);
			current_capacity = new ArrayList<Integer>(weights);
			references = new ArrayList<String>(new_references);
		}
		
		matrix = new ArrayList<Double>(intermediate_matrix);
		
		return new Pair(matrix,references);
	}
	
	private Plan BFSPlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		Pair tree_matrix = BFSMatrix(vehicle, tasks);
		System.out.println(tree_matrix.matrix.size());
		int sz = tasks.size();
		int[] pick_nb = new int[sz];
		int[] del_nb = new int[sz];
		for (int i = 0; i < sz; ++i) {
	        pick_nb[i] = i;
	        del_nb[i] = i;
	    }
		int index = amin(tree_matrix.matrix);
		String best_ref = tree_matrix.refs.get(index);
		System.out.println(best_ref);
		System.out.println("The total distance is " + tree_matrix.matrix.get(index) + " km");
		String[] best_arr = best_ref.split("(?<=\\G.)");
		for (int i = 0; i<best_arr.length; i++) {
			if (best_arr[i].equals("P") == true) {
				int compt = 0;
				for (Task task : tasks) {
					if (compt == pick_nb[Integer.parseInt(best_arr[i+1])-1]) {
						// move: current city => pickup location
						for (City city : current.pathTo(task.pickupCity))
							plan.appendMove(city);
						
						plan.appendPickup(task);
						current = task.pickupCity;
					}
					compt++;
				}
				pick_nb = removefromarr(pick_nb,Integer.parseInt(best_arr[i+1])-1);
			}
			if (best_arr[i].equals("D") == true) {
				int compt = 0;
				for (Task task : tasks) {
					if (compt == del_nb[Integer.parseInt(best_arr[i+1])-1]) {
						// move: current city => pickup location
						for (City city : current.pathTo(task.deliveryCity))
							plan.appendMove(city);
						
						plan.appendDelivery(task);
						current = task.deliveryCity;
					}
					compt++;
				}
				del_nb = removefromarr(del_nb,Integer.parseInt(best_arr[i+1])-1);
			}
			i++;
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