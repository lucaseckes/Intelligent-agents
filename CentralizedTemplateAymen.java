package template;

//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
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

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

        List<Plan> plans = new ArrayList<Plan>();
        Variables vars = SelectInitialSolution(vehicles, tasks);
        int compt = 0;
        while (plans.size() < vehicles.size()) {
        	Plan planvehicle = VariablesToPlan(vehicles.get(compt), compt, tasks, vars);
            plans.add(planvehicle);
            compt++;
        }
        
        Boolean b = Constraints(vehicles,  tasks, vars);
        System.out.println("Constraints are satisfied ? " + b);
        
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
    
    class Variables {
    	public String[] index;
    	public String[] NextTasks;
    	public int[] time;
    	public int[] vehicles;
    	
    	Variables(String[] index, String[] next, int[] Time, int[] Vehicles) {
    		
    		this.index = index;
    		this.NextTasks = next;
    		this.time = Time;
    		this.vehicles = Vehicles;
    	}
    }
    
    private Plan VariablesToPlan(Vehicle vehicle, int vehicle_id, TaskSet tasks, Variables vars) {
    	City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);
        int sz_t = tasks.size();
        String first_task = vars.NextTasks[2*sz_t + vehicle_id];
        if (first_task.equals("NULL")){
        	plan = Plan.EMPTY;
        }
        while (first_task != "NULL") {
        	if (first_task.split("(?<=\\G.)")[0].equals("P") == true) {
        		int task_nb = Integer.parseInt(first_task.replace(first_task.split("(?<=\\G.)")[0], ""));
        		int compt = 1;
        		for (Task task : tasks) {
        			if(compt == task_nb) {
        				for (City city : current.pathTo(task.pickupCity)) {
        	                plan.appendMove(city);
        	            }
        				plan.appendPickup(task);
        				current = task.pickupCity;
        			}
        			compt ++;
        		}
        		first_task = vars.NextTasks[task_nb-1];
        	}
        	else if (first_task.split("(?<=\\G.)")[0].equals("D") == true) {
        		int task_nb = Integer.parseInt(first_task.replace(first_task.split("(?<=\\G.)")[0], ""));
        		int compt = 1;
        		for (Task task : tasks) {
        			if(compt == task_nb) {
        				for (City city : current.pathTo(task.deliveryCity)) {
        	                plan.appendMove(city);
        	            }
        				plan.appendDelivery(task);
        				current = task.deliveryCity;
        			}
        			compt ++;
        		}
        		first_task = vars.NextTasks[sz_t + task_nb - 1];
        	}
        }
        return plan;
    }
    
    private Variables SelectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
    	int sz_t = tasks.size();
    	int sz_v = vehicles.size();
    	String [] nextTasks = new String[2*sz_t + sz_v];
    	String [] index = new String[2*sz_t + sz_v];
    	int size_index = index.length;
    	int [] time = new int[2*sz_t];
    	int [] vehicle = new int[2*sz_t];
    	for (int i = 0; i<sz_t-1; i++) {
    		nextTasks[i] = "D" + Integer.toString(i+1);
    		time[i] = 2*i+1;
    		vehicle[i] = 1;
    		nextTasks[sz_t + i] = "P" + Integer.toString(i+2);
    		time[sz_t+i] = 2*(i+1);
    		vehicle[sz_t+i] = 1;
    	}
    	nextTasks[sz_t - 1] = "D" + Integer.toString(sz_t);
    	nextTasks[2*sz_t-1] = "NULL";
    	time[sz_t-1] = 2*sz_t-1;
    	time[2*sz_t-1] = 2*sz_t;
    	vehicle[2*sz_t-1] = 1;
    	vehicle[sz_t-1] = 1;
    	for (int j = 0; j<sz_v; j++) {
    		nextTasks[2*sz_t + j] = "NULL";
    	}
    	nextTasks[2*sz_t] = "P1";
    	System.out.println(Arrays.toString(nextTasks));
    	System.out.println(Arrays.toString(time));
    	System.out.println(Arrays.toString(vehicle));
    	
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
    	
    	System.out.println("Indexes: " + Arrays.toString(index));
    	Variables vars = new Variables(index, nextTasks, time, vehicle);
    	return vars;
    }
    
    //change value in an array at index idx with word 
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
    
    private boolean Constraints(List<Vehicle> vehicles, TaskSet tasks, Variables vars) {
    	
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
    			System.out.println("loop 1");
    			
    		}
    			
    	}
    	
    	//nextTask(vk) = tj ⇒ time(tj) = 1:
    	for(int i=0; i<num_vehicles; i++) {
    		String task = vars.NextTasks[2*sz_t+i];
    		index_ = findIndex(vars.index, task);
    		if(index_!=-1)
    			if(vars.time[index_]!=1) {
    				
    				Bool = false;
    				System.out.println("loop 2");
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
    					System.out.println("loop 3");
    					//System.out.println("indexes "+index_ +" "+index_2);
    					//System.out.println("tasks "+task+" "+task2+" times "+vars.time[index_]+" "+vars.time[index_2]);
    					//System.out.println("vehicles "+vars.vehicles[index_]+" "+vars.vehicles[index_2]);
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
    				System.out.println("loop 4");
    			}
    				
    	}
    	
    	//all tasks must be delivered: the set of values of the variables in the
    	//nextTask array must be equal to the set of tasks T plus NV times the
    	//value NULL
    	String[] values = vars.index;
    	values = ChangeFromArray(values, 2*sz_t, "NULL");
    	for(int i=0; i<size_next; i++) {
    		if(!(check(values, vars.NextTasks[i]))) {
    			Bool = false;
    			System.out.println("loop 5");
    		}
    			

    	}
    	if(occurence(vars.NextTasks, "NULL")!=num_vehicles) {
    		Bool = false;
    		System.out.println("loop 6");
    	}
    		
    	
    	//first task of vehicle must be different from a delivery action
    	for(int i=2*sz_t; i<size_index; i++) {
    		if(vars.NextTasks[i].split("(?<=\\G.)")[0].equals("D")) {
    			System.out.println("loop 6");
    			Bool = false;
    		}
    			
    	}
    	
    	//if load(ti) > capacity(vk) ⇒ vehicle(ti) 6= vk
    	
    		
    	return Bool;
    }
    
    
    
}
