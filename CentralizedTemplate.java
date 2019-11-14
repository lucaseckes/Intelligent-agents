package template;

//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import logist.LogistSettings;
import java.io.File;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
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
public class CentralizedTemplate implements CentralizedBehavior {
	
	// Default Values
	private static final double CONSERVATIVE_RATE = 0.4;
	private static final int CYCLE_NB = 1000;

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private double conservative_rate = CONSERVATIVE_RATE;
    private int cycle_nb = CYCLE_NB;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }
    
    public void change_cycle (int cycle) {
    	this.cycle_nb = cycle;
    }
    
    public String[] getInitParam(){
        String[] initParams = { "Conservative probability" , "number of iterations"};
        return initParams;
      }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

        List<Plan> plans = new ArrayList<Plan>();
        Variables init = SelectInitialSolution(vehicles, tasks);
        Variables choice =  StochasticLocalSearch(vehicles,tasks);
        //System.out.println("Random initial solution: " + Arrays.toString(init.NextTasks));
        //System.out.println("The total distance of the random initial solution is: " + CalculateCost(vehicles, tasks, init) + " km");
        //System.out.println("Final choice: " + Arrays.toString(choice.NextTasks));
        System.out.println("The total distance for the final choice is: " + CalculateCost(vehicles, tasks, choice) + " km");
        
        int compt = 0;
        while (plans.size() < vehicles.size()) {
        	Plan planvehicle = VariablesToPlan(vehicles.get(compt), compt, tasks, choice);
            plans.add(planvehicle);
            compt++;
        }
        Boolean b = Constraints(vehicles,  tasks, choice);
        //System.out.println("Are the constraints satisfied for the final solution? " + b);
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        //System.out.println("The plan was generated in " + duration + " milliseconds.");
 
        return plans;
    }
    
    public Variables StochasticLocalSearch(List<Vehicle> vehicles, TaskSet tasks) {
    	Variables vars = SelectInitialSolution(vehicles, tasks);
    	//List to store the costs for each iteration
    	ArrayList<Double> costs = new ArrayList<Double>();
    	int count = 0;
    	do {
    		Variables old = new Variables(vars.index, vars.NextTasks, vars.time, vars.vehicles);
    		ArrayList<Variables> neigh = ChooseNeighbours(old, tasks, vehicles);
    		vars = LocalChoice(neigh, vehicles, tasks);
    		double cost = CalculateCost(vehicles, tasks, vars);
    		costs.add(cost);
    		//System.out.println("Cycle : " + count);
    		count ++;
    	} while(count<cycle_nb);
    	return vars;
    }

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }
    
    //Method to change the string value of an array knowing the index idx by the string word
    private String[] ChangeFromArray(String[] str, int idx, String word) {
    	String [] new_str = new String[str.length];
    	for (int i = 0; i<str.length; i++) {
    		if (i == idx) {
    			new_str[i] = word;
    		}
    		else {
    			new_str[i] = str[i];
    		}
    	}
    	return new_str;
    }
    
  //Method to change the string value of an array knowing the index idx by the string word
    private int[] ChangeFromArrayInt(int[] arr, int idx, int number) {
    	int [] new_arr = new int[arr.length];
    	for (int i = 0; i<arr.length; i++) {
    		if (i == idx) {
    			new_arr[i] = number;
    		}
    		else {
    			new_arr[i] = arr[i];
    		}
    	}
    	return new_arr;
    }
    
  //Remove an integer value of a matrix knowing the index
  	private int[] RemoveFromArrInt(int [] arr, int index) {
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
  	
  //Remove an integer value of a matrix knowing the integer value
  	private int[] RemoveZerosFromArr(int [] arr) {
  		int targetIndex = 0;
  		for( int sourceIndex = 0;  sourceIndex < arr.length;  sourceIndex++ )
  		{
  		    if( arr[sourceIndex] != 0 )
  		        arr[targetIndex++] = arr[sourceIndex];
  		}
  		int[] newArray = new int[targetIndex];
  		System.arraycopy( arr, 0, newArray, 0, targetIndex );
  		return newArray;
      } 
    
 // Linear-search function to find the index of an element 
    public static int findIndex(String arr[], String t) 
    { 
        // if array is Null 
        if (arr == null) { 
            return -1; 
        } 
        // find length of array 
        int len = arr.length; 
        int i = 0; 
  
        // traverse in the array 
        while (i < len) { 
  
            // if the i-th element is t 
            // then return the index 
            if (arr[i].equals(t)) { 
                return i; 
            } 
            else { 
                i = i + 1; 
            } 
        } 
        return -1; 
    }
    
    class Variables {
    	public String[] index;
    	public String[] NextTasks;
    	public int[] time;
    	public int[] vehicles;
    	
    	Variables(String[] idx, String[] next, int[] Time, int[] Vehicles) {
    		this.index = idx;
    		this.NextTasks = next;
    		this.time = Time;
    		this.vehicles = Vehicles;
    	}
    }
    
    private Plan VariablesToPlan(Vehicle vehicle, int vehicle_id, TaskSet tasks, Variables vars) {
    	City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);
        int sz_t = tasks.size(); // size of tasks
        String first_task = vars.NextTasks[2*sz_t + vehicle_id]; //The first task of the vehicle
        if (first_task.equals("NULL")){
        	plan = Plan.EMPTY;
        }
        while (first_task != "NULL") {
        	// Case for a Pickup action
        	if (first_task.split("(?<=\\G.)")[0].equals("P") == true) {
        		int task_nb = Integer.parseInt(first_task.replace(first_task.split("(?<=\\G.)")[0], "")); //The index of the pickup action
        		int compt = 1;
        		for (Task task : tasks) {
        			if(compt == task_nb) { //Pickup the task
        				for (City city : current.pathTo(task.pickupCity)) {
        	                plan.appendMove(city);
        	            }
        				plan.appendPickup(task);
        				current = task.pickupCity;
        			}
        			compt ++;
        		}
        		first_task = vars.NextTasks[task_nb-1]; // The next task after finishing the current action
        	}
        	// Case for a delivery action
        	else if (first_task.split("(?<=\\G.)")[0].equals("D") == true) {
        		int task_nb = Integer.parseInt(first_task.replace(first_task.split("(?<=\\G.)")[0], ""));
        		int compt = 1;
        		for (Task task : tasks) {
        			if(compt == task_nb) { //Deliver the task
        				for (City city : current.pathTo(task.deliveryCity)) {
        	                plan.appendMove(city);
        	            }
        				plan.appendDelivery(task);
        				current = task.deliveryCity;
        			}
        			compt ++;
        		}
        		first_task = vars.NextTasks[sz_t + task_nb - 1]; // The next task after finishing the current action
        	}
        }
        return plan;
    }
    
    public double CalculateCost(List<Vehicle> vehicles, TaskSet tasks, Variables vars) {
    	double cost = 0;
    	for (int i = 0; i<vehicles.size(); i++) {
    		Vehicle vehicle = vehicles.get(i);
    		City current = vehicle.getCurrentCity();
    		int sz_t = tasks.size(); // size of tasks
    		String first_task = vars.NextTasks[findIndex(vars.index, "v" + Integer.toString(vehicle.id()+1))]; //The first task of the vehicle
    		if (first_task.equals("NULL")){
    			cost += 0;
    		}
    		while (first_task != "NULL") {
    			// Case for a Pickup action
    			if (first_task.split("(?<=\\G.)")[0].equals("P") == true) {
    				int task_nb = Integer.parseInt(first_task.replace(first_task.split("(?<=\\G.)")[0], "")); //The index of the pickup action
    				int compt = 1;
    				for (Task task : tasks) {
    					if(compt == task_nb) { //Pickup the task
    						cost += current.distanceTo(task.pickupCity);
    						current = task.pickupCity;
    					}
    					compt ++;
    				}
    				first_task = vars.NextTasks[task_nb-1]; // The next task after finishing the current action
    			}
    			// Case for a delivery action
    			else if (first_task.split("(?<=\\G.)")[0].equals("D") == true) {
    				int task_nb = Integer.parseInt(first_task.replace(first_task.split("(?<=\\G.)")[0], ""));
    				int compt = 1;
    				for (Task task : tasks) {
    					if(compt == task_nb) { //Deliver the task
    						cost += current.distanceTo(task.deliveryCity);
    						current = task.deliveryCity;
    					}
    					compt ++;
    				}
    				first_task = vars.NextTasks[sz_t + task_nb - 1]; // The next task after finishing the current action
    			}
    		}
    	}
    	return cost;
    }
    
    private boolean check(String[] arr, String toCheckValue) 
    { 
        // check if the specified element 
        // is present in the array or not 
        // using Linear Search method 
        boolean test = false; 
        for (String element : arr) { 
            if (element.equals(toCheckValue)) { 
                test = true; 
                return test;
            }
        } 
        return test;
    } 
    private int occurence(String[] arr, String toCheckValue) 
    { 
        // check how many times the specified
    	// element is present in the array
        int test = 0; 
        for (String element : arr) { 
            if (element.equals(toCheckValue)) { 
                test += 1; 
            }
        } 
        return test;
    }
    
    private boolean Constraints(List<Vehicle> vehicles, TaskSet tasks, Variables vars) {
    	// function for the constraints check 
    	// you can uncomment the warnings to see where the problem comes from
    	
      	boolean Bool = true; 
     	int num_vehicles = vehicles.size();
     	int sz_t = tasks.size();
     	int size_index = vars.index.length;
     	int index_ = 0;
     	int size_next = vars.NextTasks.length; //size of nextTasks

      	//check if nextTask(t) != t
     	for(int i=0; i<size_index; i++) {
     		if(vars.index[i]==vars.NextTasks[i]) {
     			Bool = false;
     			//System.out.println("Warning1: nextTask(t) = t");
     		}	
     	}
     	
      	//nextTask(vk) = tj ⇒ time(tj) = 1:
     	for(int i=0; i<num_vehicles; i++) {
     		String task = vars.NextTasks[2*sz_t+i];
     		index_ = findIndex(vars.index, task);
     		if(index_!=-1)
     			if(vars.time[index_]!=1) {

      				Bool = false;
     				//System.out.println("Warning2: nextTask(vk) = tj !⇒ time(tj) = 1");
     			}	
     	}

      	//nextTask(ti) = tj ⇒ time(tj) = time(ti) + 1 & nextTask(ti) = tj ⇒ vehicle(tj) = vehicle(ti):
     	for(int i=0; i<2*sz_t; i++) {
     		String task = vars.NextTasks[i];
     		index_ = findIndex(vars.index, task);
     		if(index_!=-1) {
     			String task2 = vars.NextTasks[index_];
     			int index_2 = findIndex(vars.index, task2);
     			if(index_2!=-1)
     				if((vars.time[index_2]!=vars.time[index_]+1)||(vars.vehicles[index_]!=vars.vehicles[index_2])) {
     					Bool = false;
     					//System.out.println("Warning3: nextTask(ti) = tj !⇒ time(tj) = time(ti) + 1 or nextTask(ti) = tj !⇒ vehicle(tj) = vehicle(ti) ");
     				}
     		}
     	}

      	//nextTask(vk) = tj ⇒ vehicle(tj) = vk:
     	for(int i=0; i<num_vehicles; i++) {
     		String task = vars.NextTasks[2*sz_t+i];
     		index_ = findIndex(vars.index, task);
     		if(index_!=-1)
     			if(vars.vehicles[index_]!=i+1) {
     				Bool = false;
     				//System.out.println("Warning4: nextTask(vk) = tj !⇒ vehicle(tj) = vk");
     			}

      	}

      	//all tasks must be delivered: the set of values of the variables in the
     	//nextTask array must be equal to the set of tasks T plus NV times the value NULL
     	String[] values = vars.index;
     	values = ChangeFromArray(values, 2*sz_t, "NULL");
     	for(int i=0; i<size_next; i++) {
     		if(!(check(values, vars.NextTasks[i]))) {
     			Bool = false;
     			//System.out.println("Warning5:all tasks must be delivered ");
     		}

      	}
     	if(occurence(vars.NextTasks, "NULL")!=num_vehicles) {
     		Bool = false;
     		//System.out.println("Warning6: all tasks must be delivered ");
     	}

      	//first task of vehicle must be different from a delivery action
     	for(int i=2*sz_t; i<size_index; i++) {
     		if(vars.NextTasks[i].split("(?<=\\G.)")[0].equals("D")) {
     			//System.out.println("Warning7: first task of vehicle must be different from a delivery action");
     			Bool = false;
     		}	
     	}

      	//if load(ti) > capacity(vk) ⇒ vehicle(ti) = vk
     	int[] load = new int[num_vehicles];
     	for(int i=0; i<num_vehicles; i++) {
     		Vehicle current_vehicle = vehicles.get(i);
     		String task_ = vars.NextTasks[2*sz_t+i];
     		while(!(task_.equals("NULL"))){
             	int task_nb = Integer.parseInt(task_.replace(task_.split("(?<=\\G.)")[0], ""));
             	int compt = 1;
             	for (Task task : tasks) {
             		if(compt == task_nb) {
             			if (task_.split("(?<=\\G.)")[0].equals("P"))
             				load[i] += task.weight;
             				if(current_vehicle.capacity() < load[i]) {
             					//System.out.println("Warning8: Vehicle "+i+" doesnt have capacity to take the task");
             					Bool = false;
             				}

              			if (task_.split("(?<=\\G.)")[0].equals("D"))
             				load[i] -= task.weight;
             		}
             		compt ++;	
             	}

              	// go to next task of vehicle i
             	// until this task is not null 
             	index_ = findIndex(vars.index, task_);
             	task_ = vars.NextTasks[index_];
             	
     		}
     	}

 
      	//the vehicle that picks up a task must deliver it 
     	//if vehicle(Pi) = vk ⇒ vehicle(Di) = vk 
     	for(int i=0; i<sz_t; i++) {
     		if(vars.vehicles[i]!=vars.vehicles[i+sz_t]) {
     			//System.out.println("Warning9: The vehicle that picks up a task must deliver it");
     			Bool = false;
     		}
     	}
     	
     	for(int i=0; i<sz_t; i++) {
    		if(vars.time[i]>vars.time[i+sz_t]) {
    			//System.out.println("Warning10: A delivery action must come after a pickup action");
    			Bool = false;
    		}
    	}
     	return Bool;
     }
    
    private Variables SelectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
    	int sz_t = tasks.size();
    	int sz_v = vehicles.size();
    	String[] NextTasks = new String[2*sz_t+sz_v];
    	int[] Time = new int[2*sz_t];
    	int[] Vehicle = new int [2*sz_t]; 
    	int[] task_arr = new int[sz_t];
    	for (int i = 0; i<sz_t; i++) {
    		task_arr[i] = i;
    	}
    	int time = 0;
    	String [] index = new String[2*sz_t + sz_v];
    	int size_index = index.length;
    	
    	//construct the index 
    	for(int i=0; i<sz_t;i++) {
    		index[i] = "P" + Integer.toString(i+1);
    		index[i+sz_t] = "D" + Integer.toString(i+1);
    	}
    	int j = 1;
    	for(int i=2*sz_t; i<size_index;i++) {
    		
    		index[i] = "v" + Integer.toString(j);
    		j+=1;
    	}
    	
    	// if the number of tasks is less than the number of vehicles, all the tasks will be given to the first vehicle
    	if (sz_t < sz_v) {
    		for (int i = 0; i<sz_t-1; i++) {
        		NextTasks[i] = "D" + Integer.toString(i+1);
        		Time[i] = 2*i+1;
        		Vehicle[i] = 1;
        		NextTasks[sz_t + i] = "P" + Integer.toString(i+2);
        		Time[sz_t+i] = 2*(i+1);
        		Vehicle[sz_t+i] = 1;
        	}
        	NextTasks[sz_t - 1] = "D" + Integer.toString(sz_t);
        	NextTasks[2*sz_t-1] = "NULL";
        	Time[sz_t-1] = 2*sz_t-1;
        	Time[2*sz_t-1] = 2*sz_t;
        	Vehicle[2*sz_t-1] = 1;
        	Vehicle[sz_t-1] = 1;
        	for (int l = 0; l<sz_v; l++) {
        		NextTasks[2*sz_t + l] = "NULL";
        	}
        	NextTasks[2*sz_t] = "P1";
    	}
    	
    	else {
    		int[] chose_task = new int[sz_t+sz_v];
    		while (task_arr.length != 0) {
    			for (int k = 0; k<sz_v; k++) {
    				int random_task = Math.toIntExact(Math.round(Math.random()*(task_arr.length-1)));
    				int task_nb = task_arr[random_task] + 1; //One of the remaining task is chosen randomly and is assigned to a vehicle
    				chose_task[(time*sz_v)/2+k] = task_nb;
    				if (time == 0) {
    					NextTasks = ChangeFromArray(NextTasks, findIndex(index, "v"+ Integer.toString(vehicles.get(k).id()+1)), "P"+Integer.toString(task_nb)); //ANextTask(vi) = P_tasknb
    					NextTasks = ChangeFromArray(NextTasks, findIndex(index, "P"+ Integer.toString(task_nb)), "D"+Integer.toString(task_nb)); //ANextTask(P_tasknb) = D_tasknb
    					Time = ChangeFromArrayInt(Time, findIndex(index, "P"+ Integer.toString(task_nb)), time+1); //Set time for ATime(P_tasknb) 
    					Time = ChangeFromArrayInt(Time, findIndex(index, "D"+ Integer.toString(task_nb)), time+2); //Set time for ATime(D_tasknb)
    					Vehicle = ChangeFromArrayInt(Vehicle, findIndex(index, "P" + Integer.toString(task_nb)), k+1); //Set AVehicle(P_tasknb) with the vehicle chosen
    					Vehicle = ChangeFromArrayInt(Vehicle, findIndex(index, "D" + Integer.toString(task_nb)), k+1); //Set AVehicle(D_tasknb) with the vehicle chosen
    				}
    				else {
    					NextTasks = ChangeFromArray(NextTasks, findIndex(index, "D" + Integer.toString(chose_task[((time-2)*sz_v)/2+k])), "P" + Integer.toString(task_nb)); //ANextTask(Dpre) = P_tasknb
    					NextTasks = ChangeFromArray(NextTasks, findIndex(index, "P" + Integer.toString(task_nb)), "D"+Integer.toString(task_nb)); //AnextTask(P_tasknb) = D_tasknb
    					Time = ChangeFromArrayInt(Time, findIndex(index, "P"+ Integer.toString(task_nb)), time+1); //Set time for ATime(P_tasknb) 
    					Time = ChangeFromArrayInt(Time, findIndex(index, "D"+ Integer.toString(task_nb)), time+2); //Set time for ATime(D_tasknb)
    					Vehicle = ChangeFromArrayInt(Vehicle, findIndex(index, "P" + Integer.toString(task_nb)), k+1); //Set AVehicle(P_tasknb) with the vehicle chosen
    					Vehicle = ChangeFromArrayInt(Vehicle, findIndex(index, "D" + Integer.toString(task_nb)), k+1); //Set AVehicle(D_tasknb) with the vehicle chosen
    				}
    				task_arr = RemoveFromArrInt(task_arr, random_task);
    				if (task_arr.length == 0) {
    					k = sz_v;
    				}
    			}
    			time += 2;
    		}
    		chose_task = RemoveZerosFromArr(chose_task);
    		for (int k = 0; k<sz_v; k++) {
    			NextTasks = ChangeFromArray(NextTasks, findIndex(index, "D" + Integer.toString(chose_task[chose_task.length-k-1])), "NULL"); //The nextTask of the last task delivery will be equal to NULL
    		}
    	}
    	Variables vars = new Variables(index, NextTasks, Time, Vehicle);
    	return vars;
    }
    
    private ArrayList<Variables> ChooseNeighbours (Variables vars, TaskSet tasks, List<Vehicle> vehicles) {
    	ArrayList<Variables> N = new ArrayList<Variables>(); //N = {}
    	Vehicle vehicle1 = vehicles.get(0);
    	N.add(vars);
    	// vi = random(v1..vNV ) such that Aold(nextTask(vi )) != NULL
    	do {
    		int rand = Math.toIntExact(Math.round(Math.random()*(vehicles.size()-1)));
    		vehicle1 = vehicles.get(rand);
    	} while (vars.NextTasks[findIndex(vars.index, "v" + Integer.toString(vehicle1.id()+1))] == "NULL");
    	for (int i = 0; i<vehicles.size(); i++) {
    		Vehicle vehicle2 = vehicles.get(i);
    		if (vehicle1.id() != vehicle2.id()) {
    			int count = 0;
    			Variables neighbors = new Variables(vars.index, vars.NextTasks, vars.time, vars.vehicles);
    			neighbors = ChangingVehicle(neighbors, vehicle1, vehicle2); //A = ChangingVehicle(Aold, vi, vj )
    			if (Constraints(vehicles, tasks, neighbors)) {
					N.add(neighbors); //N = N ∪ {A}
				}
    		}
    	}
    	// Applying the Changing task order operator : 
    	// compute the number of tasks of the vehicle
    	int length = 0;
    	String task = "v" + Integer.toString(vehicle1.id()+1); //t = vi // current task in the list
    	do {
    		task = vars.NextTasks[findIndex(vars.index, task)]; //t = AoldnextTask(t)
    		length++;
    	} while(task != "NULL");
    	if (length >= 2) {
    		for (int tIdx1 = 1; tIdx1<length; tIdx1++) {
    			for (int tIdx2 = tIdx1+1; tIdx2<length; tIdx2 ++) {
    				int count = 0;
    				Variables neighbors = new Variables(vars.index, vars.NextTasks, vars.time, vars.vehicles);
        			neighbors = ChangingTaskOrder(neighbors, vehicle1, tIdx1, tIdx2); //A = ChangingTaskOrder(Aold, vi, tIdx1, tIdx2)
    				if (Constraints(vehicles, tasks, neighbors)) {
    					N.add(neighbors); //N = N ∪ {A}
    				}
    			}
    		}
    	}
    	
    	// Applying the Changing task order operator : 
    	// compute the number of tasks of the vehicle
    		for (int tIdx = 1; tIdx<tasks.size()+1; tIdx++) {
    				Variables neighbors = new Variables(vars.index, vars.NextTasks, vars.time, vars.vehicles);
        			neighbors = ChangingTaskOrderBlock(neighbors, vehicle1, tIdx); //A = ChangingTaskOrder(Aold, vi, tIdx1, tIdx2)
    				if (Constraints(vehicles, tasks, neighbors)) {
    					N.add(neighbors); //N = N ∪ {A}
    				}
    			}
    	
    	return N;
    }

    
    private Variables ChangingVehicle(Variables vars, Vehicle vehicle1, Vehicle vehicle2) {
    	Variables changed = new Variables(vars.index, vars.NextTasks, vars.time, vars.vehicles); //A1 = A
    	String picktask = changed.NextTasks[findIndex(changed.index, "v" + Integer.toString(vehicle1.id()+1))]; // t = nextTask(v1)
    	
    	
    	String nextTask = changed.NextTasks[findIndex(changed.index, picktask)]; //A1nextTask(t)
    	String futureTask = changed.NextTasks[findIndex(changed.index, nextTask)]; //A1nextTask(nextTask)
    	
    	//Find the corresponding delivery task of the new first task of vehicle 2
		int task_nb = Integer.parseInt(picktask.replace(picktask.split("(?<=\\G.)")[0], ""));
		int index = 0;
		for (int i = 0; i<changed.NextTasks.length; i++) {
			if (changed.NextTasks[i] != "NULL") {
				int number = Integer.parseInt(changed.NextTasks[i].replace(changed.NextTasks[i].split("(?<=\\G.)")[0], ""));
				if (number == task_nb && changed.NextTasks[i].split("(?<=\\G.)")[0].equals("D")) {
					index = i;
				}
			}
		}
	
		//tpre is the action before the selected delivery and tpost the action after
	
		String tpre = changed.index[index];
		String deltask = changed.NextTasks[findIndex(changed.index, tpre)];
		String tpost = changed.NextTasks[findIndex(changed.index, deltask)];
    	
    	if (nextTask.equals(deltask)) {
    		changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, "v" + Integer.toString(vehicle1.id()+1)), futureTask); // A1nextTask(v1) =A1nextTask(nexttask) = futuretask
    		changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, nextTask), changed.NextTasks[ findIndex(changed.index, "v" + Integer.toString(vehicle2.id()+1))]); // A1nextTask(deltask) =A1nextTask(v2)
        	changed.NextTasks = ChangeFromArray(changed.NextTasks,  findIndex(changed.index, "v" + Integer.toString(vehicle2.id()+1)),  picktask); // A1nextTask(v2) =picktask
        	changed.NextTasks = ChangeFromArray(changed.NextTasks,  findIndex(changed.index, picktask),  deltask); // A1nextTask(picktask) =deltask
    	}
    	
    	else {
    		changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, "v" + Integer.toString(vehicle1.id()+1)), nextTask); // A1nextTask(v1) =nexttask
    		changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, tpre), tpost); // A1nextTask(tpre) = tpost
    		changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, deltask), changed.NextTasks[ findIndex(changed.index, "v" + Integer.toString(vehicle2.id()+1))]); // A1nextTask(deltask) =A1nextTask(v2)
    		changed.NextTasks = ChangeFromArray(changed.NextTasks,  findIndex(changed.index, "v" + Integer.toString(vehicle2.id()+1)),  picktask); // A1nextTask(v2) =picktask
    		changed.NextTasks = ChangeFromArray(changed.NextTasks,  findIndex(changed.index, picktask),  deltask); // A1nextTask(picktask) =deltask
    	}
    	
    	changed = UpdateTime(changed, vehicle1);
    	changed = UpdateTime(changed, vehicle2);
    	changed.vehicles = ChangeFromArrayInt(changed.vehicles, findIndex(changed.index, picktask), vehicle2.id()+1); //A1vehicle(picktask) = v2
    	changed.vehicles = ChangeFromArrayInt(changed.vehicles, findIndex(changed.index, deltask), vehicle2.id()+1); //A1vehicle(deltask) = v2;
    	return changed;
    }
    
    private Variables ChangingTaskOrder(Variables vars, Vehicle vehicle, int tidx1, int tidx2) {
    	Variables changed = new Variables(vars.index, vars.NextTasks, vars.time, vars.vehicles); //A1 = A
    	String tPre1 = "v" + Integer.toString(vehicle.id()+1);
    	String tPre2 = "";
    	String task1 = vars.NextTasks[findIndex(vars.index, tPre1)]; //t1 = A1nextT ask(tP re1 ) // task1
    	int count = 1;
    	while (count < tidx1) {
    		String replace = task1;
    		tPre1 = tPre1.replace(tPre1, task1); //tPre1 =t1
    		task1 = vars.NextTasks[findIndex(changed.index, task1)];//t1 =A1nextTask(t1)
    		count++;
    	}
    	String tPost1 = changed.NextTasks[findIndex(changed.index, task1)];//tPost1 = A1nextTask(t1 ) // the task delivered after t1
    	tPre2 = tPre2.replace(tPre2, task1);//tPre2 = t1 // previous task of task2
    	String task2 = changed.NextTasks[findIndex(changed.index, tPre2)];//t2 = A1nextTask(tPre2 ) // task2
    	count++;
    	while (count < tidx2) {
    		String replace = task2; 
    		tPre2 = tPre2.replace(tPre2, replace); //tPre2 =t2
    		task2 = changed.NextTasks[findIndex(changed.index, task2)];//t2 =A1nextTask(t2)
    		count++;
    	}
    	String tPost2 = changed.NextTasks[findIndex(changed.index, task2)];//tPost2 = A1nextTask(t2) // the task delivered after t2
    	// exchanging two tasks
    	if(tPost1.equals(task2)) {
    		// the task t2 is delivered immediately after t1
    		changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, tPre1), task2);  //A1nextTask(tPre1) =t2
    		changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, task2), task1); //A1nextTask(t2) =t1
    		changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, task1), tPost2);//A1nextTask(t1) =tPost2
    	}
    	else {
    		changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, tPre1), task2);//A1nextTask(tPre1) =t2
    		changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, tPre2), task1); //A1nextTask(tPre2) =t1
    		changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, task2), tPost1);//A1nextTask(t2) =tPost1
    		changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, task1), tPost2);//A1nextTask(t1) =tPost2
    	}
    	changed = UpdateTime(changed, vehicle);
    	return changed;
    }
    
    private Variables ChangingTaskOrderBlock(Variables vars, Vehicle vehicle, int tidx) {
    	Variables changed = new Variables(vars.index, vars.NextTasks, vars.time, vars.vehicles); //A1 = A
    	
    	String task = "v" + Integer.toString(vehicle.id()+1); //t = vi // current task in the list
    	Boolean task_exist = false;
    	while(task != "NULL") {
    		task = vars.NextTasks[findIndex(vars.index, task)]; //t = AoldnextTask(t)
    		if (task!="NULL") {
    			int number = Integer.parseInt(task.replace(task.split("(?<=\\G.)")[0], ""));
    			if (number == tidx) {
    				task_exist = true;
    			}
    		}
    	} 
    	
    	if (task_exist == false) {
    		return changed;
    	}
    	
    	else {
    	
    	//Find the corresponding delivery task of the new first task of vehicle 2
		int pickindex = 0;
		for (int i = 0; i<changed.NextTasks.length; i++) {
			if (changed.NextTasks[i] != "NULL") {
				int number = Integer.parseInt(changed.NextTasks[i].replace(changed.NextTasks[i].split("(?<=\\G.)")[0], ""));
				if (number == tidx && changed.NextTasks[i].split("(?<=\\G.)")[0].equals("P")) {
					pickindex = i;
				}
			}
		}
    	
    	//Find the corresponding delivery task of the new first task of vehicle 2
    			int delindex = 0;
    			for (int i = 0; i<changed.NextTasks.length; i++) {
    				if (changed.NextTasks[i] != "NULL") {
    					int number = Integer.parseInt(changed.NextTasks[i].replace(changed.NextTasks[i].split("(?<=\\G.)")[0], ""));
    					if (number == tidx && changed.NextTasks[i].split("(?<=\\G.)")[0].equals("D")) {
    						delindex = i;
    					}
    				}
    			}
    			
    			String tpredel = changed.index[delindex];
    			String deltask1 = changed.NextTasks[findIndex(changed.index, tpredel)];
    			String tpostdel = changed.NextTasks[findIndex(changed.index, deltask1)];
    			
    			String tprepick = changed.index[pickindex];
    			String picktask1 = changed.NextTasks[findIndex(changed.index, tprepick)];
    			String tpostpick = changed.NextTasks[findIndex(changed.index, picktask1)];
 
    			String nexttask = changed.NextTasks[findIndex(changed.index, "v" + (vehicle.id()+1))];
    			
    			if (picktask1.equals(nexttask)) {
  				
    				if (deltask1.equals(tpostpick)) {
    					
    					changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, "v" + (vehicle.id()+1)), picktask1); //Anexttask(vi) = picktask
            			changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, picktask1), deltask1);
            			changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, deltask1), tpostdel);
    				}
    				else {
    				
    					changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, "v" + (vehicle.id()+1)), picktask1); //Anexttask(vi) = picktask
    					changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, picktask1), deltask1);
    					changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, deltask1), tpostpick);
    					changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, tpredel), tpostdel);
    				}
    			
    			}
    			
    			else {
    				
    				changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, "v" + (vehicle.id()+1)), picktask1); //Anexttask(vi) = picktask
    				changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, picktask1), deltask1);
    				changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, deltask1), nexttask);
    				changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, tprepick), tpostpick);
    				changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(changed.index, tpredel), tpostdel);
    			}
    	
    	changed = UpdateTime(changed, vehicle);
    	return changed;
    	}
    }
    
    private Variables LocalChoice (ArrayList<Variables> neighbors_vars, List<Vehicle> vehicles, TaskSet tasks) {
    	double proba = Math.random();
    	//System.out.println("The number of neighbours is : " + (neighbors_vars.size()-1));
    	double cost = CalculateCost(vehicles, tasks, neighbors_vars.get(1));
    	int index = 1;
    	for (int i= 1; i<neighbors_vars.size(); i++) {
    		double new_cost = CalculateCost(vehicles, tasks, neighbors_vars.get(i));
    		if (new_cost < cost) {
    			cost = new_cost;
    			index = i;
    		}
    	}
    	
    		if(proba > conservative_rate) 
    			return neighbors_vars.get(1);
    		else
    			return neighbors_vars.get(index);
    }
    
    private Variables UpdateTime (Variables vars, Vehicle vehicle1) {
    	Variables changed = new Variables(vars.index, vars.NextTasks, vars.time, vars.vehicles);
    	String task = changed.NextTasks[findIndex(changed.index, "v" + Integer.toString(vehicle1.id()+1))]; //ti =AnextTask(vi)
    	if (task != "NULL") {
    		changed.time = ChangeFromArrayInt(changed.time, findIndex(changed.index, task),  1);
    		do {
    			String next_task = changed.NextTasks[findIndex(changed.index, task)]; //tj =AnextTask(ti)
    			if (next_task != "NULL") {
    				changed.time = ChangeFromArrayInt(changed.time, findIndex(changed.index, next_task), (changed.time[findIndex(changed.index, task)] + 1)); //Atime(tj ) = Atime(ti ) + 1
    				String replace = next_task;
    				task = task.replace(task, replace);
    			}
    		} while(changed.NextTasks[findIndex(changed.index, task)] != "NULL");
    	}
    	return changed;
    }
}
