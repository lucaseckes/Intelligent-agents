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
        Variables vars2 = ChangingVehicle(vars, vehicles.get(0), vehicles.get(1));
        Variables vars3 = ChangingTaskOrder(vars2, vehicles.get(0), 1, 3);
        System.out.println(Arrays.toString(vars2.NextTasks));
        System.out.println(Arrays.toString(vars3.NextTasks));
        int compt = 0;
        while (plans.size() < vehicles.size()) {
        	Plan planvehicle = VariablesToPlan(vehicles.get(compt), compt, tasks, vars2);
            plans.add(planvehicle);
            compt++;
        }
        Boolean b = Constraints(vehicles,  tasks, vars2);
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
     			System.out.println("Warning: nextTask(t) = t");
     		}	
     	}

      	//vehicles.stream().map(v -> v.getCurrentTasks()
     			//.stream().map(t->t.path()
     			//.stream().filter(p->p.name=="Bourdo")))
     			//.collect(Collectors.toList());

      	//nextTask(vk) = tj ⇒ time(tj) = 1:
     	for(int i=0; i<num_vehicles; i++) {
     		String task = vars.NextTasks[2*sz_t+i];
     		index_ = findIndex(vars.index, task);
     		if(index_!=-1)
     			if(vars.time[index_]!=1) {

      				Bool = false;
     				System.out.println("loop 2");
     				System.out.println("Warning: nextTask(vk) = tj !⇒ time(tj) = 1");
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
     					System.out.println("Warning: nextTask(ti) = tj !⇒ time(tj) = time(ti) + 1 or nextTask(ti) = tj !⇒ vehicle(tj) = vehicle(ti) ");
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
     				System.out.println("Warning: nextTask(vk) = tj !⇒ vehicle(tj) = vk");
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
     			System.out.println("Warning: ");
     		}

      	}
     	if(occurence(vars.NextTasks, "NULL")!=num_vehicles) {
     		Bool = false;
     		System.out.println("loop 6");
     		System.out.println("Warning: ");
     	}

      	//first task of vehicle must be different from a delivery action
     	for(int i=2*sz_t; i<size_index; i++) {
     		if(vars.NextTasks[i].split("(?<=\\G.)")[0].equals("D")) {
     			System.out.println("loop 7");
     			System.out.println("Warning: first task of vehicle must be different from a delivery action");
     			Bool = false;
     		}	
     	}

      	//if load(ti) > capacity(vk) ⇒ vehicle(ti) = vk
     	int[] load = new int[4];
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
             					System.out.println("for loop 8");
             					System.out.println("Warning: Vehicle "+i+" doesnt have capacity to take the task");
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
             	//task_ = vars.NextTasks[task_nb-1];
     			//load[i] = 
     		}
     	}

 
      	//the vehicle that picks up a task must deliver it 
     	//if vehicle(Pi) = vk ⇒ vehicle(Di) = vk 
     	for(int i=0; i<sz_t; i++) {
     		if(vars.vehicles[i]!=vars.vehicles[i+sz_t]) {
     			System.out.println("for loop 9");
     			System.out.println("Warning: The vehicle that picks up a task must deliver it");
     			Bool = false;
     		}
     	}
     	return Bool;
     }
    
    
    private Variables SelectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
    	int sz_t = tasks.size();
    	int sz_v = vehicles.size();
    	String [] nextTasks = new String[2*sz_t + sz_v];
    	int [] time = new int[2*sz_t];
    	int [] vehicle = new int[2*sz_t];
    	String [] index = new String[2*sz_t + sz_v];
    	int size_index = index.length;
    	
    	for (int i = 0; i<sz_t-1; i++) {
    		nextTasks[i] = "D" + Integer.toString(i+1); //After the pickup, the vehicle directly make the delivery
    		time[i] = 2*i+1;
    		vehicle[i] = 1; // The first vehicle make all the tasks
    		nextTasks[sz_t + i] = "P" + Integer.toString(i+2); //After the delivery, the vehicle directly make the pickup
    		time[sz_t+i] = 2*(i+1);
    		vehicle[sz_t+i] = 1;
    	}
    	nextTasks[sz_t - 1] = "D" + Integer.toString(sz_t);
    	nextTasks[2*sz_t-1] = "NULL"; // After deliver the last task, the vehicle stops
    	time[sz_t -1] = 2*sz_t-1;
    	time[2*sz_t-1] = 2*sz_t;
    	vehicle[sz_t - 1] = 1;
    	vehicle[2*sz_t-1] = 1;
    	for (int j = 0; j<sz_v; j++) {
    		nextTasks[2*sz_t + j] = "NULL";
    	}
    	nextTasks[2*sz_t] = "P1"; //The first task of the vehicle is P1
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
    
    private ArrayList<Variables> ChooseNeighbors (Variables vars, TaskSet tasks, List<Vehicle> vehicles) {
    	ArrayList<Variables> N = new ArrayList<Variables>(); //N = {}
    	Vehicle vehicle1 = vehicles.get(0);
    	// vi = random(v1..vNV ) such that Aold(nextTask(vi )) != NULL
    	do {
    		int rand = Math.toIntExact(Math.round(Math.random()*3));
    		vehicle1 = vehicles.get(rand);
    	} while (vars.NextTasks[findIndex(vars.index, "v" + Integer.toString(vehicle1.id()+1))] != "NULL");
    	for (int i = 0; i<4; i++) {
    		Vehicle vehicle2 = vehicles.get(i);
    		if (vehicle1.id() != vehicle2.id()) {
    			int count = 0;
    			Variables neighbors = new Variables(vars.index, vars.NextTasks, vars.time, vars.vehicles);
    			while(count < 5 && Constraints(vehicles, tasks, neighbors) == false) {
    				neighbors = ChangingVehicle(vars, vehicle1, vehicle2); //A = ChangingVehicle(Aold, vi, vj )
    				count++;
    			}
    			N.add(neighbors); // N = N ∪ {A}
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
    		for (int tIdx1 = 0; tIdx1<length; tIdx1++) {
    			for (int tIdx2 = tIdx1+1; tIdx2<length; tIdx2 ++) {
    				int count = 0;
    				Variables neighbors = new Variables(vars.index, vars.NextTasks, vars.time, vars.vehicles);
    				while(count < 5 && Constraints(vehicles, tasks, neighbors) == false) {
        				neighbors = ChangingTaskOrder(vars, vehicle1, tIdx1, tIdx2); //A = ChangingTaskOrder(Aold, vi, tIdx1, tIdx2)
        				count++;
        			}
    				N.add(neighbors); //N = N ∪ {A}
    			}
    		}
    	}
    	return N;
    }

    
    private Variables ChangingVehicle(Variables vars, Vehicle vehicle1, Vehicle vehicle2) {
    	Variables changed = new Variables(vars.index, vars.NextTasks, vars.time, vars.vehicles); //A1 = A
    	String task = vars.NextTasks[findIndex(vars.index, "v" + Integer.toString(vehicle1.id()+1))]; // t = nextTask(v1)
    	changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(vars.index, "v" + Integer.toString(vehicle1.id()+1)), changed.NextTasks[findIndex(vars.index, task)]); // A1nextTask(v1) =A1nextTask(t)
    	changed.NextTasks = ChangeFromArray(changed.NextTasks, findIndex(vars.index, task), changed.NextTasks[ findIndex(vars.index, "v" + Integer.toString(vehicle2.id()+1))]); // A1nextTask(t) =A1nextTask(v2)
    	changed.NextTasks = ChangeFromArray(changed.NextTasks,  findIndex(vars.index, "v" + Integer.toString(vehicle2.id()+1)),  task); // A1nextTask(v2) =t
    	changed = UpdateTime(changed, vehicle1);
    	changed = UpdateTime(changed, vehicle2);
    	changed.vehicles[findIndex(vars.index, task)] = vehicle2.id()+1; //A1vehicle(t) = v2
    	return changed;
    }
    
    private Variables ChangingTaskOrder(Variables vars, Vehicle vehicle, int tidx1, int tidx2) {
    	Variables changed = new Variables(vars.index, vars.NextTasks, vars.time, vars.vehicles); //A1 = A
    	String tPre1 = "v" + Integer.toString(vehicle.id()+1);
    	String task1 = changed.NextTasks[findIndex(vars.index, tPre1)]; //t1 = A1nextT ask(tP re1 ) // task1
    	System.out.println(task1);
    	int count = 1;
    	while (count < tidx1) {
    		String replace = task1;
    		tPre1 = tPre1.replace(tPre1, task1); //tPre1 =t1
    		task1 = vars.NextTasks[findIndex(changed.index, task1)];//t1 =A1nextTask(t1)
    		count++;
    	}
    	String tPost1 = changed.NextTasks[findIndex(changed.index, task1)];//tPost1 = A1nextTask(t1 ) // the task delivered after t1
    	String tPre2 = task1;//tPre2 = t1 // previous task of task2
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
    
    private Variables UpdateTime (Variables vars, Vehicle vehicle1) {
    	String task = vars.NextTasks[findIndex(vars.index, "v" + Integer.toString(vehicle1.id()+1))]; //ti =AnextTask(vi)
    	if (task != "NULL") {
    		vars.time[findIndex(vars.index, task)] = 1;
    		do {
    			String next_task = vars.NextTasks[findIndex(vars.index, task)]; //tj =AnextTask(ti)
    			if (next_task != "NULL") {
    				vars.time[findIndex(vars.index, next_task)] = vars.time[findIndex(vars.index, task)] + 1; //Atime(tj ) = Atime(ti ) + 1
    				String replace = next_task;
    				task = task.replace(task, replace);
    			}
    		} while(vars.NextTasks[findIndex(vars.index, task)] != "NULL");
    	}
    	return vars;
    }
}
