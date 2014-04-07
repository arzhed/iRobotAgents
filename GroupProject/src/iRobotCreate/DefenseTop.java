/**
 * <p>Title: CASA Agent Infrastructure</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2007, Knowledge Science Group, University of Calgary. 
 * Permission to use, copy, modify, distribute and sell this software and its
 * documentation for any purpose is hereby granted without fee, provided that
 * the above copyright notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting documentation.  
 * The  Knowledge Science Group makes no representations about the suitability
 * of  this software for any purpose.  It is provided "as is" without express
 * or implied warranty.</p>
 * <p>Company: Knowledge Science Group, University of Calgary</p>
 * @author <a hef="http://pages.cpsc.ucalgary.ca/~kremer/">Rob Kremer</a>
 * @version 0.17
 */
package iRobotCreate;

import iRobotCreate.ScoreGoalTop.Position;
import iRobotCreate.iRobotCommands.Sensor;

import java.util.Timer;
import java.util.TimerTask;

import casa.ML;
import casa.MLMessage;
import casa.URLDescriptor;
import casa.abcl.ParamsMap;
import casa.exceptions.URLDescriptorException;
import casa.ui.AgentUI;
import casa.util.CASAUtil;

/**
 * @author <a hef="http://pages.cpsc.ucalgary.ca/~kremer/">Rob Kremer</a>
 *
 */
public class DefenseTop extends StateBasedController {
	
	private String myColor = "yellow";
	private String ballColor = "red";
	
	Position topGoal = new Position(639,0);
	Position bottomGoal = new Position(639,700);
	boolean tGoal = true;
	Position posGoal = topGoal;
	
	float scaleFactor = 1;
	
	/**
	 * @param params
	 * @param ui
	 * @throws Exception
	 */
	public DefenseTop(ParamsMap params, AgentUI ui) throws Exception {
		super(params, ui);
		if (params.containsKey("COLOR"))
			myColor = (String)params.getJavaObject("COLOR");
		if (params.containsKey("BALL-COLOR"))
			ballColor = (String)params.getJavaObject("BALL-COLOR");
		if (params.containsKey("SCALE")) {
			try {
				scaleFactor = (Float)params.getJavaObject("SCALE");
			} catch (Throwable e) {
				System.out.println("Illegal value for key SCALE ("+params.getJavaObject("SCALE")+"). Using value 1:"+e);
			}
		}
	}
	
	@Override
	public void initializeAfterRegistered(boolean registered) {
		super.initializeAfterRegistered(registered);
		registerState(goToGoalState);
		registerState(scanState);
		setState(goToGoalState);
	}
	
	class Position {
		public int x, y, a;
		Position(String parsable) throws NumberFormatException, IllegalArgumentException {
			String content[] = parsable.split(",");
			if (content.length!=4) 
				throw new IllegalArgumentException("BallPusher.Position("+parsable+"): Expected a comma-separted list of length 4.");
			x = Integer.parseInt(content[1]);
			y = Integer.parseInt(content[2]);
			a = Integer.parseInt(content[3]);
			
		}
		Position(int x_, int y_) {
			x = x_;
			y = y_;
		}
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Position))
				return false;
			Position p = (Position)obj;
			return x==p.x && y==p.y && a==p.a;
		}
	}
	
	protected Position askCamera(String shape, String color) {
		URLDescriptor url;
		try {
			url = URLDescriptor.make(8995);
		} catch (URLDescriptorException e) {
			println("error", "ScoreGoal.askCamera(): unexpected exception", e);
			return null;
		}
		MLMessage reply = sendRequestAndWait(ML.REQUEST, "get-color-position", url, ML.CONTENT, shape+","+color);
		if (reply!=null && isA(reply.getParameter(ML.PERFORMATIVE),ML.PROPOSE)) {
			Position P = new Position((String)reply.getParameter(ML.CONTENT));
			return P;
		}
		return null;
	}
	
	protected void tellRobot(String command) {
		try {
			sendMessage(ML.REQUEST, ML.EXECUTE, URLDescriptor.make(9100+getURL().getPort()%100), ML.CONTENT, command);
		} catch (URLDescriptorException e) {
			println("error", "ScoreGoal.tellRobot(): unexpected exception", e);
		}
	}
	
	/**
	 * 
	 * @param xGoal
	 * @param yGoal
	 * @param xRobot
	 * @param yRobot
	 * @return
	 * @author Tristan A.
	 */
			
	public static int getAngle(int xGoal, int yGoal, int xRobot, int yRobot){
		int degrees;
		double deltaX = xGoal-xRobot, deltaY = yGoal-yRobot;							//get the difference in X and Y
		if(deltaX==0){																		//if deltaX is 0
			if(deltaY>0){																		//if deltaY is 0
				degrees = 90;																		//set to 90 degrees
			} else {																			//otherwise
				degrees = 270;	}																	//set to 270 degrees
		} else if (deltaX>0){																//if deltaX is positive
			if(deltaY>0){																		//if deltaY is positive
				degrees = (int)(Math.atan(deltaY/deltaX)*180/Math.PI);								//calculate the degrees
			} else if (deltaY==0) {																//if deltaY is 0
				degrees = 0;																		//degrees are 0
			} else {																			//if deltaY is negative
				degrees = (int)(Math.atan(deltaY/deltaX)*180/Math.PI) + 360;	}					//calculate the degrees and add 360 due to negative value
		} else {																			//else deltaX is negative
			if (deltaY==0) {																	//if deltaY is 0
				degrees = 180;																		//degrees are 180
			} else {																			//else deltaY is not 0
				degrees = (int)(Math.atan(deltaY/deltaX)*180/Math.PI) + 180;	}					//calculate the degrees and add 180 (whether + or -)
		}
		
		return degrees;
	}
	
	protected void sleepDistance(int d) {
		CASAUtil.sleepIgnoringInterrupts((Math.abs(d)*15)+2000, null);
	}
	
	protected void sleepAngle(int a) {
		CASAUtil.sleepIgnoringInterrupts(Math.abs(a)*2000/36+2000, null);
	}
	
	int xDistanceFinder() {
		Position posTri = askCamera("triangle", myColor);
		Position posBall= askCamera("circle"  , ballColor);
		int x=0;
		double a=.0,b=.0;
		
		a = ((double)posGoal.y - (double)posBall.y) / ((double)posGoal.x - (double)posBall.x);
		b = (double)posGoal.y - (double)posGoal.x * a;
		
		x = (int)(((double)posTri.y - b)/a);	
		
		return x;
	}
	
	//***********************************************
	//************ STATES ***************************
	//***********************************************
	
	IRobotState goToGoalState = new IRobotState("goToGoal") {
		
		int yDistance=0;
		int xDistance=0;
		int turnAngle=0;
		
		@Override
		public void enterState() {
			makeSubthread(new Runnable() {
				@Override
				public void run() {
					try {
                        System.out.println(getURL().getFile()+" enter state goToGoal thread started.");
                        Position posTri = askCamera("triangle", myColor);
						turnAngle = posTri.a - getAngle(posTri.x, 0, posTri.x, posTri.y);
						if (turnAngle>180) turnAngle -= 360;
					    else if (turnAngle<-180) turnAngle += 360;
						if (Math.abs(turnAngle)>3) {
							System.out.println("Angle to turn: " + turnAngle);
							tellRobot("(iRobot.rotate-deg "+ turnAngle +")");
							sleepAngle(turnAngle);
						}
						
						
						while (posTri.y>(int)iRobotCommands.chassisRadius + 20){
							if(posTri.y<(int)iRobotCommands.chassisRadius + 120) {
								yDistance = posTri.y - (int)iRobotCommands.chassisRadius - 20;
								System.out.println("Distance to go: " + yDistance);
								tellRobot("(iRobot.moveby " + yDistance +")");
								sleepDistance(yDistance);
							}
							else {
								yDistance = posTri.y/2;;
								System.out.println("Distance to go: " + yDistance);
								tellRobot("(iRobot.moveby " + yDistance +")");
								sleepDistance(yDistance);
							}
							posTri = askCamera("triangle", myColor);
						}
						
						Position posBall = askCamera("circle", ballColor);
						if(posTri.x > 852)
							turnAngle = 90;
						else if(posTri.x < 426)
							turnAngle = -90;
						else if(posTri.x < posBall.x)
							turnAngle = -90;
						else turnAngle = 90;
						
						/*
						xDistance = posTri.x - posGoal.x;
						if (xDistance<0)
							turnAngle=-90;
						else
							turnAngle=90;
						*/
						System.out.println("Angle to turn: " + turnAngle);
						tellRobot("(iRobot.rotate-deg "+ turnAngle +")");
						sleepAngle(turnAngle);
						/*
						xDistance = Math.abs (xDistance);
						System.out.println("DDDDDistance to go: " + xDistance);
						tellRobot("(iRobot.moveby " + xDistance +")");
						sleepDistance(xDistance);
						*/
						
						setState(scanState);
					}
					catch (Throwable e) {
						println("error", "ScoreGoal.enterState() [state=goToGoal]: Unexpected error in state thread", e);
					}
					System.out.println(getURL().getFile()+" enter state goToGoal thread ended.");
				}
			}).start();
		}
		
		@Override
		public void handleEvent(Sensor sensor, final short reading) {
			switch (sensor) {
			case Overcurrents:
				//tellRobot("(iRobot.drive 0 :emergency T)");
			case BumpsAndWheelDrops:
			}
		}
	};
	
IRobotState scanState = new IRobotState("scan") {
		
		int xDistance=0;
		int turnAngle=0;
		int goalSize=426;
		
		@Override
		public void enterState() {
			makeSubthread(new Runnable() {
				@Override
				public void run() {
					try {
                        System.out.println(getURL().getFile()+" enter state scan thread started.");
                        Position posBall = askCamera("circle", ballColor);
                        Position posTri = askCamera("triangle", myColor);
                        if ( posBall.x < posGoal.x - goalSize/2)
                        	xDistance = posTri.x - (posGoal.x - goalSize/2);
                        else if (posBall.x > posGoal.x + goalSize/2)
                        	xDistance = posTri.x - (posGoal.x + goalSize/2);
                        else
                        	xDistance = posTri.x - posBall.x;
                        if (xDistance<0)
                        	turnAngle = posTri.a;
                        else
                        	turnAngle = posTri.a - 180;
                        xDistance = Math.abs(xDistance);
                        if (Math.abs(turnAngle)>3) {
							System.out.println("Angle to turn: " + turnAngle);
							tellRobot("(iRobot.rotate-deg "+ turnAngle +")");
							sleepAngle(turnAngle);
                        }
                        if (xDistance>5) {
							System.out.println("Distance to go: " + xDistance);
							tellRobot("(iRobot.moveby " + xDistance +")");
							sleepDistance(xDistance);
                        }
						setState(scanState);
					}
					catch (Throwable e) {
						println("error", "ScoreGoal.enterState() [state=scan]: Unexpected error in state thread", e);
					}
					System.out.println(getURL().getFile()+" enter state scan thread ended.");
				}
			}).start();
		}
		
		@Override
		public void handleEvent(Sensor sensor, final short reading) {
			switch (sensor) {
			case Overcurrents:
				//tellRobot("(iRobot.drive 0 :emergency T)");
			case BumpsAndWheelDrops:
			}
		}
	};
}
