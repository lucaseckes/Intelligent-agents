import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DTorus;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int vX;
	private int vY;
	private int energy;
	private int energyGrass;
	private static int IDNumber = 0;
	private int ID;
	private RabbitsGrassSimulationSpace rgsSpace;
	
	public RabbitsGrassSimulationAgent(int EnergyRabbits, int EnergyGrass){
	    x = -1;
	    y = -1;
	    energy = EnergyRabbits;
	    energyGrass = EnergyGrass;
	    setVxVy();
	    IDNumber++;
	    ID = IDNumber;
	  }
	
	private void setVxVy(){
		    vX = 0;
		    vY = 0;
		    while((vX == 0) && ( vY == 0)){
		      vX = (int)Math.floor(Math.random() * 3) - 1;
		      vY = (int)Math.floor(Math.random() * 3) - 1;
		    }
		  }

	
	public void draw(SimGraphics arg0) {
		 arg0.drawFastRoundRect(Color.white);
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
	
	public void setXY(int newX, int newY){
	  x = newX;
	  y = newY;
	}
	
	public void setRabbitsGrassSimulationSpace(RabbitsGrassSimulationSpace rgss){
	    rgsSpace = rgss;
	  }
	
	public String getID(){
	    return "A-" + ID;
	  }

	  public int getEnergy(){
	    return energy;
	  }
	  
	  public void setEnergy(int nr){
		    energy = nr;
	 }
	  
	  public void step(){
		  int newX = x + vX;
		    int newY = y + vY;

		    Object2DTorus grid = rgsSpace.getCurrentAgentSpace();
		    newX = (newX + grid.getSizeX()) % grid.getSizeX();
		    newY = (newY + grid.getSizeY()) % grid.getSizeY();
		    if(tryMove(newX, newY)){
		        energy += rgsSpace.takeGrassAt(x, y, energyGrass);
		      }
		      else{
		        setVxVy();
		      }
		    energy--;
		  }
	  
	  private boolean tryMove(int newX, int newY){
		    return rgsSpace.moveAgentAt(x, y, newX, newY);
		  }

}
