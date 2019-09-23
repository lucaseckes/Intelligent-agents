import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
	private Object2DGrid grassSpace;
	private Object2DGrid agentSpace;
	
	  public RabbitsGrassSimulationSpace(int Size){
		    grassSpace = new Object2DGrid(Size, Size);
		    agentSpace = new Object2DGrid(Size, Size);
		    for(int i = 0; i < Size; i++){
		      for(int j = 0; j < Size; j++){
		        grassSpace.putObjectAt(i,j,new Integer(0));
		      }
		    }
		  }
	  
	  public void spreadGrass(int numInitGrass){
		    // Randomly place money in moneySpace
		    for(int i = 0; i < numInitGrass; i++){

		      // Choose coordinates
		      int x = (int)(Math.random()*(grassSpace.getSizeX()));
		      int y = (int)(Math.random()*(grassSpace.getSizeY()));

		      // Get the value of the object at those coordinates
		      int currentValue = getGrassAt(x, y);
		      // Replace the Integer object with another one with the new value
		      grassSpace.putObjectAt(x,y,new Integer(currentValue + 1));
		    }
		  }
	  
	  public int getGrassAt(int x, int y){
		    int i;
		    if(grassSpace.getObjectAt(x,y)!= null){
		      i = ((Integer)grassSpace.getObjectAt(x,y)).intValue();
		    }
		    else{
		      i = 0;
		    }
		    return i;
		  }
	  
	  public Object2DGrid getCurrentGrassSpace(){
		    return grassSpace;
		  }
	  
	  public Object2DGrid getCurrentAgentSpace(){
		    return agentSpace;
		  }
	  
	  public boolean isCellOccupied(int x, int y){
		    boolean retVal = false;
		    if(agentSpace.getObjectAt(x, y)!=null) retVal = true;
		    return retVal;
		  }

		  public boolean addAgent(RabbitsGrassSimulationAgent agent){
		    boolean retVal = false;
		    int count = 0;
		    int countLimit = 10 * agentSpace.getSizeX() * agentSpace.getSizeY();

		    while((retVal==false) && (count < countLimit)){
		      int x = (int)(Math.random()*(agentSpace.getSizeX()));
		      int y = (int)(Math.random()*(agentSpace.getSizeY()));
		      if(isCellOccupied(x,y) == false){
		        agentSpace.putObjectAt(x,y,agent);
		        agent.setXY(x,y);
		        retVal = true;
		      }
		      count++;
		    }

		    return retVal;
		  }
}
