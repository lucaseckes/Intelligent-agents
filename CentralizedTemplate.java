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
    	public String[] NextTasks;
    	public int[] time;
    	public int[] vehicles;
    	
    	Variables(String[] next, int[] Time, int[] Vehicles) {
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
    	for (int j = 0; j<sz_v; j++) {
    		nextTasks[2*sz_t + j] = "NULL";
    	}
    	nextTasks[2*sz_t] = "P1";
    	System.out.println(Arrays.toString(nextTasks));
    	Variables vars = new Variables(nextTasks, time, vehicle);
    	return vars;
    }
}
