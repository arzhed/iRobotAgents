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
public class ScoreGoal extends StateBasedController {
	
	private String myColor = "yellow";
	private String ballColor = "red";
	
	Position topGoal = new Position(639,700);
	Position bottomGoal = new Position(639,0);
	boolean tGoal = false;
	Position posGoal = bottomGoal;
	
	float scaleFactor = 1;
	
	/**
	 * @param params
	 * @param ui
	 * @throws Exception
	 */
	public ScoreGoal(ParamsMap params, AgentUI ui) throws Exception {
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
		registerState(ballPushState);
		registerState(goodSideState);
		registerState(wrongSideState);
		registerState(idleState);
		setState(idleState);
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
	
	class RelativeBaring {
		public int distance, angle;
		RelativeBaring(int d, int a) {distance=d; angle=a;}
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
	RelativeBaring getRelativeBarring() {
		Position posTri = askCamera("triangle", myColor);
		Position posBall= askCamera("circle"  , ballColor);
		while (posTri==null || posBall==null || posTri.x==-1 || posBall.x==-1 || posTri.equals(posBall)) {
			println("error", "BallPusher.enterState() [state=finding]: Can't find "+myColor+" triangle or "+ballColor+" circle.");
			posTri = askCamera("triangle", myColor);
			posBall= askCamera("circle"  , ballColor);
			CASAUtil.sleepIgnoringInterrupts(3000, null);
		}
		// fix up by the scaleFactor
		posBall.x = (int)(scaleFactor*posBall.x);
		posBall.y = (int)(scaleFactor*posBall.y);
		posTri.x  = (int)(scaleFactor*posTri.x);
		posTri.y  = (int)(scaleFactor*posTri.y);
		
		int diffX = posBall.x-posTri.x;
		int diffY = posBall.y-posTri.y;
		int distance = (int)Math.sqrt(diffX*diffX + diffY*diffY);
		assert distance>0;
//		int angle = (int)(Math.atan((double)((diffY==0)?0:(diffY/diffX)))*180/Math.PI);
//		int turnAngle = posTri.a-angle;
//		int angleBall = (diffX==0)?90:(int)(Math.atan(((double)diffY)/(double)diffX)*180/Math.PI);
//		if (diffX<0) angleBall += 180;
		int angleBall = getAngle(posBall.x, posBall.y, posTri.x, posTri.y);
		int turnAngle = posTri.a-angleBall;
//		turnAngle %= 360;
		if (turnAngle>180) turnAngle -= 360;
	  else if (turnAngle<-180) turnAngle += 360;
		assert turnAngle<=180 && turnAngle>=-180;
		return new RelativeBaring(distance, turnAngle);
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
		
		a = (double)(posGoal.x - posBall.x) / (posGoal.y - posBall.y);
		b = (double)(posGoal.y - posGoal.x * a);
		
		x = (int)((posTri.y - b)/a);	
		
		return x;
	}
	
	//***********************************************
	//************ STATES ***************************
	//***********************************************

	IRobotState ballPushState = new IRobotState("ballPush") {
		
		boolean stop = false;
		boolean hit = false;
		
		class TempBlocker extends TimerTask {
			Timer t = null;
			long timeOut;
			TempBlocker(long time) {timeOut = time;}
			@Override public void run() {t.cancel(); t = null;}
			public boolean isFree() {return t==null;}
			public void start() {if (t!=null) return; t = new Timer(); t.schedule(this, timeOut);}
		}
		TempBlocker blocker = new TempBlocker(1000);
		
		@Override
		public void enterState() {
			stop = false;
			hit = false;
			makeSubthread(new Runnable() {
				@Override
				public void run() {
					try {
						System.out.println(getURL().getFile()+" enter state ballPush thread started.");
						tellRobot("(iRobot.drive 0 :emergency T)");
						while (!stop) {
							RelativeBaring baring = getRelativeBarring();
							assert baring.distance>0;
							assert baring.angle<=180 && baring.angle>=-180;
							if (baring.distance<(((int)iRobotCommands.chassisRadius)+100)) {
								hit = true;
								System.out.println(getURL().getFile()+" hit ball!");
							}
							System.out.println(getURL().getFile()+" moving "+baring.distance+"mm at "+baring.angle+" degrees.");
							if (baring.angle!=0) {
								tellRobot("(iRobot.LED 255 255)");
								tellRobot("(iRobot.rotate-deg "+baring.angle+")");
								sleepAngle(baring.angle);
								System.out.println(getURL().getFile()+" angle done.");
							}
							tellRobot("(iRobot.LED 0 255)");
							tellRobot("(iRobot.moveby "+(baring.distance/*-(int)iRobotCommands.chassisRadius*/)+")");
							sleepDistance(baring.distance);
							System.out.println(getURL().getFile()+" move done.");
						}
					} catch (Throwable e) {
						println("error", "ScoreGoal.enterState() [state=ballPush]: Unexpected error in state thread", e);
					}
					System.out.println(getURL().getFile()+" enter state ballPush thread ended.");
				}
			}).start();
		}
		
		@Override
		public void handleEvent(Sensor sensor, final short reading) {
			switch (sensor) {
			case Overcurrents:
				tellRobot("(iRobot.drive 0 :emergency T)");
			case BumpsAndWheelDrops:
				if (blocker.isFree()) { // stops a "simultaneous" overcurrent and bump from both executing.
					blocker.start();
					makeSubthread(new Runnable() {
						@Override
						public void run() {
							stop = true; //kill the thread started in the init of this state.
							if (reading!=0) {
								tellRobot("(iRobot.LED 128 255)");
								if (hit) { // if were get stopped and we already hit the ball, just quit
									tellRobot("(iRobot.drive 0 :emergency T)");
									System.out.println(getURL().getFile()+" done!");
									tellRobot("(agent.exit)");
									exit();
								}
								else { // if we hit something on the way to the cup, try again.
									System.out.println(getURL().getFile()+" resetting due to obstacle.");
									hit = false;
									CASAUtil.sleepIgnoringInterrupts(10000, null); // wait for things to settle
									System.out.println(getURL().getFile()+" obstacle avoidance routine.");
									int angle = (int)(Math.random()*360-180);
									tellRobot("(iRobot.rotate-deg "+angle+")");
									sleepAngle(angle);
									int distance = (int)(Math.random()*600);
									tellRobot("(iRobot.moveby "+distance+")");
									sleepDistance(distance);
									setState(ballPushState); // yes, this is setting to self-state, but that causes the state to reset and call enterState()
								}
							}
						}
					}).start();
				}
				break;
			}
		}
	};
	
	IRobotState goodSideState = new IRobotState("goodSide") {
		
		int xDistance=0;
		int turnAngle=0;
		
		@Override
		public void enterState() {
			makeSubthread(new Runnable() {
				@Override
				public void run() {
					try {
                        System.out.println(getURL().getFile()+" enter state goodSide thread started.");
                        Position posTri = askCamera("triangle", myColor);
                        Position posBall = askCamera("circle", ballColor);
						xDistance = xDistanceFinder();
                        xDistance = Math.abs(xDistance - posTri.x);
						if (posBall.x > posTri.x) {
                            if(posTri.a <180)
                                turnAngle = posTri.a;
                            else
                                turnAngle = posTri.a - 360;
                        }
                        else {
                            if(posTri.a <180)
                                turnAngle = 180 - posTri.a;
                            else
                                turnAngle = posTri.a - 180;
                        }

						System.out.println("Distance to go: " + xDistance);
						System.out.println("Angle to turn: " + turnAngle);
						tellRobot("(iRobot.LED 255 255)");
						tellRobot("(iRobot.rotate-deg "+ turnAngle +")");
						sleepAngle(turnAngle);
						tellRobot("(iRobot.LED 0 255)");
						tellRobot("(iRobot.moveby "+ xDistance +")");
						sleepDistance(xDistance);
						setState(ballPushState);
					}
					catch (Throwable e) {
						println("error", "ScoreGoal.enterState() [state=goodSide]: Unexpected error in state thread", e);
					}
					System.out.println(getURL().getFile()+" enter state goodSide thread ended.");
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
	
	IRobotState wrongSideState = new IRobotState("wrongSide") {
		
		int yDistance=0;
		int turnAngle=0;
		
		@Override
		public void enterState() {
			makeSubthread(new Runnable() {
				@Override
				public void run() {
					try {
						System.out.println(getURL().getFile()+" enter state wrongSide thread started.");
						Position posTri = askCamera("triangle", myColor);
						Position posBall= askCamera("circle"  , ballColor);
						
						yDistance= posBall.y  - (int)iRobotCommands.chassisRadius -50;
						
						turnAngle = posTri.a - getAngle(posTri.x, yDistance, posTri.x, posTri.y);
						if (turnAngle>180) turnAngle -= 360;
					    else if (turnAngle<-180) turnAngle += 360;
						yDistance = Math.abs(yDistance - posTri.y);
						
						System.out.println("Distance to go: " + yDistance);
						System.out.println("Angle to turn: " + turnAngle);
						tellRobot("(iRobot.LED 255 255)");
						tellRobot("(iRobot.rotate-deg "+ turnAngle +")");
						sleepAngle(turnAngle);
						tellRobot("(iRobot.LED 0 255)");
						tellRobot("(iRobot.moveby "+ yDistance +")");
						sleepDistance(yDistance);
                        CASAUtil.sleepIgnoringInterrupts(3000, null); // wait for things to settle
						setState(idleState);
					}
					catch (Throwable e) {
						println("error", "ScoreGoal.enterState() [state=wrongSide]: Unexpected error in state thread", e);
					}
					System.out.println(getURL().getFile()+" enter state wrongSide thread ended.");
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
	
	IRobotState idleState = new IRobotState("Idle") {
		
		@Override
		public void enterState() {
			makeSubthread(new Runnable() {
				@Override
				public void run() {
					try {
						System.out.println(getURL().getFile()+" enter state idle thread started.");
						Position posBall= askCamera("circle"  , ballColor);
						Position posTri = askCamera("triangle", myColor);
						if (tGoal) {
							if (posTri.y < posBall.y  - (int)iRobotCommands.chassisRadius) {
								setState(wrongSideState);
								System.out.println("WRONG SIDE");
							}else {
								setState(goodSideState);
								System.out.println("GOOD SIDE");
							}
						}
						else {
							System.out.println("y robot : " + posTri.y);
							System.out.println("x ball : " + posBall.x);
							System.out.println("y ball : " + posBall.y);
							System.out.println("y all : " + (posBall.y  - (int)iRobotCommands.chassisRadius));
                            System.out.println("a robot : " + posTri.a);

							if (posTri.y > posBall.y  - (int)iRobotCommands.chassisRadius ) {
                                System.out.println("WRONG SIDE");
                                System.out.println("getAngle" +getAngle(posTri.x, posBall.y  - (int)iRobotCommands.chassisRadius, posTri.x, posTri.y));
								setState(wrongSideState);
							}else {
                                System.out.println("GOOD SIDE");
                                System.out.println("xdistance " + xDistanceFinder());
                                System.out.println("getAngle "+getAngle(xDistanceFinder(), posTri.y, posTri.x, posTri.y));
								setState(goodSideState);
							}
						}
					}
					catch (Throwable e) {
						println("error", "ScoreGoal.enterState() [state=idle]: Unexpected error in state thread", e);
					}
					System.out.println(getURL().getFile()+" enter state idle thread ended.");
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
