import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;


/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {	
	
	// Default Values
	private static final int TORUSSIZE = 20;
	private static final int NUMINITRABBITS = 8;
	private static final int ENERGYRABBITS = 5;
	private static final int BIRTHTHRESHOLD = 20;
	private static final int NUMINITGRASS = 10;
	private static final int GRASSGROWTHRATE = 3;

	
	private Schedule schedule;
	int torusSize = TORUSSIZE;
	private int numInitRabbits = NUMINITRABBITS;
	private int energyRabbits = ENERGYRABBITS;
	private int birthThreshold = BIRTHTHRESHOLD;
	private int numInitGrass = NUMINITGRASS;
	private int grassGrowthRate = GRASSGROWTHRATE;
	
	private RabbitsGrassSimulationSpace rgsSpace;
	
	private ArrayList agentList;
	
	private DisplaySurface displaySurf;
	
	private OpenSequenceGraph amountInSpace;

	  class grassInSpace implements DataSource, Sequence {

	    public Object execute() {
	      return new Double(getSValue());
	    }

	    public double getSValue() {
	      return (double)rgsSpace.getTotalGrass();
	    }
	  }
	  
	  class rabbitsInSpace implements DataSource, Sequence {

		    public Object execute() {
		      return new Double(getSValue());
		    }

		    public double getSValue() {
		      return (double) countLivingRabbits();
		    }
		  }

		public static void main(String[] args) {
			
			System.out.println("Rabbit skeleton");

			SimInit init = new SimInit();
			RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
			// Do "not" modify the following lines of parsing arguments
			if (args.length == 0) // by default, you don't use parameter file nor batch mode 
				init.loadModel(model, "", false);
			else
				init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));
			
		}
		
		public void begin() {
			buildModel();
		    buildSchedule();
		    buildDisplay();
		    
		    displaySurf.display();
		    amountInSpace.display();
		}
		
		public void buildModel(){
			System.out.println("Running BuildModel");
		    rgsSpace = new RabbitsGrassSimulationSpace(torusSize);
		    rgsSpace.spreadGrass(numInitGrass);
		    
		    for(int i = 0; i < numInitRabbits; i++){
		        addNewRabbit();
		      }
		    for(int i = 0; i < agentList.size(); i++){
		        RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentList.get(i);
		        rgsa.report();
		      }
		}

		public void buildSchedule(){
			System.out.println("Running BuildSchedule");
			
			class RabbitsGrassSimulationStep extends BasicAction {
			      public void execute() {
			        SimUtilities.shuffle(agentList);
			        for(int i =0; i < agentList.size(); i++){
			          RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentList.get(i);
			          rgsa.step();
			        }
			        
			        reapDeadRabbits();
			        addNewbornRabbit();
			        
			        displaySurf.updateDisplay();
			      }
			    }

			    schedule.scheduleActionBeginning(0, new RabbitsGrassSimulationStep());
			    
			    class RabbitsGrassSimulationCountLiving extends BasicAction {
			        public void execute(){
			          countLivingRabbits();
			        }
			      }

			      schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationCountLiving());

			      class RabbitsGrassSimulationUpdateGrassInSpace extends BasicAction {
			        public void execute(){
			        	addNewGrass();
			          amountInSpace.step();
			        }
			      }

			      schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationUpdateGrassInSpace());
			    }
		

		public void buildDisplay(){
			System.out.println("Running BuildDisplay");

		    ColorMap map = new ColorMap();

		    for(int i = 1; i<16; i++){
		      map.mapColor(i, new Color((int)0, (i * 8 + 127), 0));
		    }
		    map.mapColor(0, Color.black);

		    Value2DDisplay displayGrass = 
		        new Value2DDisplay(rgsSpace.getCurrentGrassSpace(), map);
		    
		    Object2DDisplay displayAgents = new Object2DDisplay(rgsSpace.getCurrentAgentSpace());
		    displayAgents.setObjectList(agentList);

		    displaySurf.addDisplayable(displayGrass, "Grass");
		    displaySurf.addDisplayable(displayAgents, "Rabbits");
		    
		    amountInSpace.addSequence("Grass In Space", new grassInSpace());
		    amountInSpace.addSequence("Rabbits In Space", new rabbitsInSpace());
		}

		public String[] getInitParam() {
			// TODO Auto-generated method stub
			// Parameters to be set by users via the Repast UI slider bar
			// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
			String[] params = { "TorusSize", "NumInitRabbits", "EnergyRabbits", "BirthThreshold",  "NumInitGrass", "GrassGrowthRate"};
			return params;
		}

		public String getName() {
			return "Rabbit population simulation";
		}
		
		private void addNewbornRabbit(){
			 for(int i = (agentList.size() - 1); i >= 0 ; i--){
				 RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentList.get(i);
				 if(rgsa.getEnergy() > birthThreshold){
					 RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(energyRabbits);
					 agentList.add(a);
					 rgsSpace.addAgent(a);
				 }
			 }
		}
		
		private void addNewRabbit(){
		    RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(energyRabbits);
		    agentList.add(a);
		    rgsSpace.addAgent(a);
		  }
		
		private void addNewGrass(){
			  rgsSpace.addGrass(grassGrowthRate);
			}
		
		private void reapDeadRabbits(){
		    for(int i = (agentList.size() - 1); i >= 0 ; i--){
		      RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentList.get(i);
		      if(rgsa.getEnergy() < 0){
		        rgsSpace.removeRabbitAt(rgsa.getX(), rgsa.getY());
		        agentList.remove(i);
		      }
		    }
		  }

		public Schedule getSchedule() {
			return schedule;
		}

		public void setup() {
			System.out.println("Running setup");
			rgsSpace = null;
			agentList = new ArrayList();
			schedule = new Schedule(1);
			
			if (displaySurf != null){
			    displaySurf.dispose();
			  }
			  displaySurf = null;
			  
			  if (amountInSpace != null){
			      amountInSpace.dispose();
			    }
			    amountInSpace = null;


			  displaySurf = new DisplaySurface(this, "Rabbit Grass Simulation Model Window 1");
			  amountInSpace = new OpenSequenceGraph("Amount In Space",this);

			  registerDisplaySurface("Rabbit Grass Simulation Model Window 1", displaySurf);
			  this.registerMediaProducer("Plot", amountInSpace);
		}
		
		private int countLivingRabbits(){
		    int livingAgents = 0;
		    for(int i = 0; i < agentList.size(); i++){
		      RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentList.get(i);
		      if(rgsa.getEnergy() > 0) livingAgents++;
		    }

		    return livingAgents;
		  }
		
		public int getTorusSize(){
		    return torusSize;
		}

		public void setTorusSize(int ts){
			torusSize = ts;
		}
		
		public int getNumInitRabbits(){
		    return numInitRabbits;
		}

		public void setNumInitRabbits(int nir){
			numInitRabbits = nir;
		}
		
		public int getEnergyRabbits(){
		    return energyRabbits;
		}

		public void setEnergyRabbits(int er){
			energyRabbits = er;
		}
		
		public int getBirthThreshold(){
		    return birthThreshold;
		}

		public void setBirthThreshold(int bt){
			energyRabbits = bt;
		}
		
		public int getNumInitGrass(){
		    return numInitGrass;
		}

		public void setNumInitGrass(int nig){
			numInitGrass = nig;
		}
		
		public int getGrassGrowthRate(){
		    return grassGrowthRate;
		}

		public void setGrassGrowthRate(int ggr){
			energyRabbits = ggr;
		}
		
}
