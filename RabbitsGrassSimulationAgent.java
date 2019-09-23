import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int energy;
	private int stepsToLive;
	private static int IDNumber = 0;
	private int ID;
	
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
	
	public String getID(){
	    return "A-" + ID;
	  }

	  public int getEnergy(){
	    return energy;
	  }

	  public int getStepsToLive(){
	    return stepsToLive;
	  }

	  public void report(){
	    System.out.println(getID() + 
	                       " at " + 
	                       x + ", " + y + 
	                       " has " + 
	                       getEnergy() + " energy" + 
	                       " and " + 
	                       getStepsToLive() + " steps to live.");
	  }
	  
	  public void step(){
		    stepsToLive--;
		  }

}
