import uchicago.src.sim.space.Object2DTorus;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
	private Object2DTorus grassSpace;
	private Object2DTorus agentSpace;
	
	  public RabbitsGrassSimulationSpace(int Size){
		    grassSpace = new Object2DTorus(Size, Size);
		    agentSpace = new Object2DTorus(Size, Size);
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
		      if(currentValue == 0){
		    	  grassSpace.putObjectAt(x,y,new Integer(currentValue + 1));
		      }
		      else {
		    	  i--;
		      }
		    }
		  }
	  
	  public Object2DTorus addGrass(int grassGrowthRate) {
		  for(int i = 0; i < grassGrowthRate; i++){
			
			  // Choose coordinates
		      int x = (int)(Math.random()*(grassSpace.getSizeX()));
		      int y = (int)(Math.random()*(grassSpace.getSizeY()));

		      // Get the value of the object at those coordinates
		      int currentValue = getGrassAt(x, y);
		      // Replace the Integer object with another one with the new value
		      grassSpace.putObjectAt(x,y,new Integer(currentValue + 1));
		  }
		  return grassSpace;
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
	  
	  public Object2DTorus getCurrentGrassSpace(){
		    return grassSpace;
		  }
	  
	  public Object2DTorus getCurrentAgentSpace(){
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
		        agent.setRabbitsGrassSimulationSpace(this);
		        retVal = true;
		      }
		      count++;
		    }

		    return retVal;
		  }
		  
		  public void removeRabbitAt(int x, int y){
			    agentSpace.putObjectAt(x, y, null);
			  }
		  
		  public int takeGrassAt(int x, int y){
			    int energy = getGrassAt(x, y);
			    grassSpace.putObjectAt(x, y, new Integer(0));
			    return energy;
			  }
		  
		  public boolean moveAgentAt(int x, int y, int newX, int newY){
			    boolean retVal = false;
			    if(!isCellOccupied(newX, newY)){
			      RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentSpace.getObjectAt(x, y);
			      removeRabbitAt(x,y);
			      rgsa.setXY(newX, newY);
			      agentSpace.putObjectAt(newX, newY, rgsa);
			      retVal = true;
			    }
			    return retVal;
			  }
		  
		  public int getTotalGrass(){
			    int totalMoney = 0;
			    for(int i = 0; i < agentSpace.getSizeX(); i++){
			      for(int j = 0; j < agentSpace.getSizeY(); j++){
			        totalMoney += getGrassAt(i,j);
			      }
			    }
			    return totalMoney;
			  }
}
