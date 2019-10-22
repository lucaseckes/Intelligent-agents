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
		
		System.out.println("Agent "+ agent.name());
		
	}
	
	
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		System.out.println(algorithm);
		System.out.println(vehicle);

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			plan = AstarPlan(vehicle, tasks);
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
			System.out.println(task);
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
					System.out.println(task);
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
	
	private static double maxValue(ArrayList<Double> arr) {
	    double max = arr.get(0);
	    for (int ktr = 0; ktr < arr.size(); ktr++) {
	        if (arr.get(ktr) > max) {
	            max = arr.get(ktr);
	        }
	    }
	    return max;
	}
	
	//Function to sum element by element of two arrays
	private ArrayList<Double> applyOn2Arrays(ArrayList<Double> a, ArrayList<Double> b){
		
		ArrayList<Double> c = new ArrayList<Double>();
		for (int i = 0; i < a.size(); ++i) {
		    c.add(a.get(i) + b.get(i));
		}
		
		return c;
	}
	
	
	
	
	public ArrayList<Double> AstarMatrix(Vehicle vehicle, TaskSet tasks, City current, Boolean[] agent_tasks) {
		
		//cost of best path from node to node 
		//heuristic estimate function Euclidian or Manhattan distance 
		//g:distance from start node
		//h:distance from end node  
		
		Plan plan = new Plan(current);
		double num_cities = 0;
		double heuristic = Double.POSITIVE_INFINITY;
		
		ArrayList<Double> Gn = new ArrayList<Double>();
		ArrayList<Double> Hn = new ArrayList<Double>();
		ArrayList<Double> Fn = new ArrayList<Double>();
		
		//num_cities = topology.size();
		
		for (City city: topology.cities()){
			
			//compute the heuristic function
			//add  function to define minimization between the nearest task and
			//actual delivery city 
			
			for(Task task : tasks) {
				//Take the nearest task distance 
				if(city.distanceTo(task.pickupCity)+ task.pickupCity.distanceTo(task.deliveryCity)< heuristic) {
					
					heuristic = city.distanceTo(task.pickupCity) +  task.pickupCity.distanceTo(task.deliveryCity);
				}
				if((city.distanceTo(task.deliveryCity) < heuristic)&(agent_tasks[task.id]==true)) {
					
					heuristic = city.distanceTo(task.deliveryCity);
				}
				
			}
			if(!((taskInCity(city, tasks)) & (deliveryCity(city, tasks))))
				heuristic = Double.POSITIVE_INFINITY;
			
			Hn.add(heuristic);
			heuristic = Double.POSITIVE_INFINITY;
			
			//compute g(n)
			//distance from start city (current) to each city in the map
			Gn.add(city.distanceTo(current));

		}
		
		//Fn = Hn + Gn
		Fn = applyOn2Arrays(Hn, Gn);
		
		
		return Fn;
	}
	
	//function that returns if there is a task in a given city 
	private Boolean taskInCity(City city, TaskSet tasks) {
		Boolean bool = false;
		for(Task task : tasks) {
			if(task.pickupCity == city)
				bool = true;
		}
		return bool;
	}
	
	//function that returns if a given city is a delivery city or not 
	private Boolean deliveryCity(City city, TaskSet tasks) {
		Boolean bool = false;
		for(Task task : tasks) {
			if(task.deliveryCity == city)
				bool = true;
		}
		return bool;
	}
	
	private Boolean availableTask(Task task, TaskSet act) {
		Boolean bool = false;
		for(Task task_ : act) {
			if(task_ == task)
				bool = true;
		}
		return bool;
	}
	
	
	private Plan AstarPlan(Vehicle vehicle, TaskSet tasks) {
		
		int test = 0;
		int num_tasks = 0;
		Task task_;
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		//ArrayList<City> deliveryCities = new ArrayList<City>();
		
		double distance = 0;
		TaskSet act = tasks.copyOf(tasks);
		
		Boolean[] agent_tasks = new Boolean[tasks.size()];
		for(int i=0; i<tasks.size(); ++i)
			agent_tasks[i] = false;
		
		ArrayList<Double> Fn = AstarMatrix(vehicle, tasks, current, agent_tasks);
		int index = amin(Fn);
		City goal_city = topology.cities().get(index);
		int sz = tasks.size();
		//int[] array_nb = new int[sz];
		//while(!(act.isEmpty()))
		while(sz > 0){
			System.out.println("goal city :"+goal_city);
			for (Task task : tasks) {
				
				ArrayList<City> deliveryCities = new ArrayList<City>();
				ArrayList<Task> deliveryTasks = new ArrayList<Task>();
				if ((task.pickupCity == goal_city)&(num_tasks<1)&(availableTask(task, act))){
					test++;
					System.out.println("test   "+test);
					distance += current.distanceTo(task.pickupCity) + task.pickupCity.distanceTo(task.deliveryCity);
					
					//deliveryCities.add(task.deliveryCity);
					deliveryTasks.add(task);
					
					// move: current city => pickup location
					for (City city : current.pathTo(task.pickupCity))
						plan.appendMove(city);
					
					plan.appendPickup(task);
					agent_tasks[task.id] = true;

					// move: pickup location => delivery location
					for (City city : task.path())
						plan.appendMove(city);

					plan.appendDelivery(task);
					agent_tasks[task.id] = false;
						
							

					//plan.appendDelivery(task);

					// set current city
					current = task.deliveryCity;
					System.out.println("current city :"+current);
					
					
					act.remove(task);
					sz--;
					task_ = task;
					System.out.println(sz+"    "+task_);
					num_tasks++;
					
				}
			
			}
			num_tasks = 0;
			//tasks.remove(task_);
			goal_city = current;
			// if there is no task in current city 
			if(!((taskInCity(goal_city, act))&(deliveryCity(goal_city, act)))){
				Fn = AstarMatrix(vehicle, act, current, agent_tasks);
				index = amin(Fn);
				goal_city = topology.cities().get(index);
				
			}
	
			//sz--;
		}
		System.out.println(distance);
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