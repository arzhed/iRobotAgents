import org.armedbear.lisp.Environment;

import casa.Agent;
import casa.CASAProcess;
import casa.LispAccessible;
import casa.ML;
import casa.Status;
import casa.TransientAgent;
import casa.URLDescriptor;
import casa.abcl.CasaLispOperator;
import casa.abcl.ParamsMap;
import casa.exceptions.URLDescriptorException;
import casa.ui.AgentUI;

/**
 * 1. use LISP command line (in casa chat) to do sample moves with the robot
 * 2. create a LISP accessible java method to (a) be called from (1), and then (b) execute more than 1 LISP command
 *	  have extra parameters, e.g. speed, duration, angle
 * 3. ("sleep" bad) drive robot and then create time event (for queuing) to stop robot (call event.start)
 * 4. new casa lisp operator (has optional & keyed parameters)
 * 5. having a request instead of LISP commands
 */
public class RobotAgent extends Agent {
	
	public static void main(String[] p) {

		SetEnvironment();
	}
	
	public static void SetEnvironment()
	{
		String cmd =
				"(let (\n" +
						"        (trace-tags \"info5,warning,msg,iRobot,-boundSymbols,-policies9,-commitments,-eventqueue,-conversations\")\n" +
						"        (trace-code 10) ; bit 1=off, 2=on, 4=to-monitor-window, 8=trace-to-file\n" +
						"        (sleep-time 2) ; time to sleep between starting dependent agents, adjust for slower machines\n" +
						"        )\n" +
						"  \n" +
						"        ; Set the options for the agent running the commandline\n" +
						"        (agent.options :options.tracing T)\n" +
						"        (agent.options :options.tracetags trace-tags)\n" +
						"  \n" +
						"        ;(new-agent \"casa.LAC\" \"ResearchLAC\" 9000 :process \"CURRENT\" :markup \"KQML\" :trace :traceFile :traceTags \"warning,msg,commitments,policies5\")\n" +
						"        (sleep 2)\n" +
						"  \n" +
						"        (agent.new-agent \"iRobotCreate.simulator.Environment\" \"RoomEnvironment\" 5780 :LAC 9000 :process \"CURRENT\" :trace trace-code :traceTags trace-tags :markup \"KQML\")\n" +
						"        (sleep 5)\n" +
						"        (declare (integer width))    (setq width 2304)\n" +
						"        (declare (integer height))   (setq height 1384)\n" +
						"        (declare (integer goalCtrX)) (setq goalCtrX (/ width 2))\n" +
						"        (agent.tell \":5780\" \"(iRobot-env.new-bounds 2304 1382)\") \n" +
						"        (agent.tell \":5780\" \"(iRobot-env.new \\\"goal0\\\" \\\"Rectangle2D\\\" goalCtrX 25            (floor (/ width 3)) 50 :paint T :corporeal NIL :color #x8888FF)\")\n" +
						"        (agent.tell \":5780\" \"(iRobot-env.new \\\"goal1\\\" \\\"Rectangle2D\\\" goalCtrX (- height 25) (floor (/ width 3)) 50 :paint T :corporeal NIL :color #xFFFF88)\")\n" +
						"  \n" +
						"        (sleep-ignoring-interrupts 2)\n" +
						"        (agent.tell \":5780\" \"(iRobot-env.puck :name \\\"puck\\\")\")\n" +
						"        (sleep-ignoring-interrupts 2)\n" +
						"        (agent.tell \":5780\" \"(iRobot-env.set \\\"puck\\\" :labeled NIL)\")\n" +
						"        (sleep-ignoring-interrupts 2)\n" +
						"        (agent.tell \":5780\" \"(iRobot-env.circle \\\"puck\\\" :color-name \\\"red\\\")\")\n" +
						"  \n" +
						"        (sleep-ignoring-interrupts 10)\n" +
						"        (agent.new-agent \"iRobotCreate.simulator.CameraSimulation\" \"camera\" 8995  :LAC 9000 :process \"CURRENT\" :trace trace-code :traceTags trace-tags :scale (/ 1280.0 width))\n" +
						"  \n" +
						"        (sleep-ignoring-interrupts 2)\n" +
						"        (agent.new-agent \"iRobotCreate.iRobotCreate\" \"Alice2\" 9100  :LAC 9000 :process \"CURRENT\" :trace trace-code :traceTags trace-tags :markup \"KQML\" :outstream \"Alice.out\" :instream \"Alice.in\")\n" +
						"        (agent.tell \":5780\" \"(iRobot-env.triangle \\\"Alice2\\\" :name \\\"red-tri\\\" :color-name \\\"purple\\\")\")\n" +
						//"        (agent.new-agent \"iRobotCreate.iRobotCreate\" \"Bob2\"   9101  :LAC 9000 :process \"CURRENT\" :trace trace-code :traceTags trace-tags :markup \"KQML\" :outstream \"Bob.out\" :instream \"Bob.in\")\n" +
						//"        (agent.tell \":5780\" \"(iRobot-env.triangle \\\"Bob2\\\" :name \\\"green-tri\\\" :color-name \\\"green\\\")\")\n" +
						//"        (agent.new-agent \"iRobotCreate.iRobotCreate\" \"Carol2\" 9102  :LAC 9000 :process \"CURRENT\" :trace trace-code :traceTags trace-tags :markup \"KQML\" :outstream \"Carol.out\" :instream \"Carol.in\")\n" +
						//"        (agent.tell \":5780\" \"(iRobot-env.triangle \\\"Carol2\\\" :name \\\"yellow-tri\\\" :color-name \\\"yellow\\\")\")\n" +
						"  \n" +
						"        (sleep-ignoring-interrupts 2)\n" +
						"        (agent.new-agent \"iRobotCreate.DefenseTop\" \"ControllerOfAlice\" 9200 :LAC 9000 :process \"CURRENT\" :trace trace-code :traceTags trace-tags :markup \"KQML\" :controls \":9100\" :color \"purple\" :ball-color \"red\" :scale (/ width 1280.0))\n" +
						//"        (agent.new-agent \"iRobotCreate.ScoreGoalTop\" \"ControllerOfBob\"   9201 :LAC 9000 :process \"CURRENT\" :trace trace-code :traceTags trace-tags :markup \"KQML\" :controls \":9101\" :color \"green\"  :ball-color \"red\" :scale (/ width 1280.0))\n" +
						//"        (agent.new-agent \"iRobotCreate.BallPusher\" \"ControllerOfCarol\" 9202 :LAC 9000 :process \"CURRENT\" :trace trace-code :traceTags trace-tags :markup \"KQML\" :controls \":9102\" :color \"yellow\" :ball-color \"red\" :scale (/ width 1280.0))\n" +
						"    ) ;let   ";

		CASAProcess.main(new String[] {"-LAC", cmd});
	}

	static {
		createCasaLispOperators( RobotAgent.class );
	}
	
	public RobotAgent(ParamsMap params, AgentUI ui) throws Exception {
		super(params, ui);
	}

	@LispAccessible(name="RobotAgent.poke", help="Make an iRobot turn around @ fixed speed and radius. All hard coded")
	public Status poke() {
		try {
			abclEval("(agent.tell \"6903\" \"(irobot.drive 20 50)\")", null);
			Thread.sleep(5000);
			abclEval("(agent.tell \"6903\" \"(irobot.drive 0)\")", null);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new Status(0);
	}
	
	@LispAccessible(name="RobotAgent.push", help="Make an iRobot turn around @ fixed speed and radius. All hard coded",
			arguments={@LispAccessible.Argument(name="speed", help="The speed at which the robot will move[-500,500]")})
	public Status push(Integer speed) {
		try {
			int theSpeed = speed <-500 ? -500 : speed> 500 ? 500 : speed;
			abclEval("(RobotAgent.tell \"6903\" \"(irobot.drive " +theSpeed+" 50)\")", null);
			Thread.sleep(5000);
			abclEval("(RobotAgent.tell \"6903\" \"(irobot.drive 0)\")", null);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new Status(0);
	}
	
	@LispAccessible(name="DrEvil.again", help="Make an iRobot turn around @ some speed and radius.",
			arguments={@LispAccessible.Argument(name="speed", help="The speed at which the robot will move[-500,500]"),
						@LispAccessible.Argument(name="radius", help="The radius at which the robot will move[-500,500]")})
	public Status again(Integer speed, Integer radius) {
		int theSpeed = speed <-500 ? -500 : speed> 500 ? 500 : speed;
		int theRadius = radius <-2000 ? -2000 : radius> 2000 ? 2000 : radius;
		abclEval("(agent.tell \"6903\" \"(irobot.drive " +theSpeed+' '+theRadius+")\")", null);
		defer(new Runnable(){
			@Override
			public void run() {
				abclEval("(agent.tell \"6903\" \"(irobot.drive 0)\")", null);
			}});
		return new Status(0);
	}
	
	/*private static final CasaLispOperator DREVIL__MORE =
			new CasaLispOperator("DREVIL.MORE", "\"!Makes an iRobot turn around @ some speed and radius.\" "
				+"&OPTIONAL"
				+"(SPEED 40) \"@java.lang.Integer\" \"!The speed in mm/s [-500,500]\" "
				+"(RADIUS 20) \"@java.lang.Integer\" \"!The radius in mm [-2000,2000]\" ", RobotAgent.class){
	public Status execute(TransientAgent agent, ParamsMap params, AgentUI ui, Environment env) {
		int theSpeed = (Integer)params.getJavaObject("SPEED");
		int theRadius = (Integer)params.getJavaObject("RADIUS");
		theSpeed = theSpeed <-500 ? -500 : theSpeed> 500 ? 500 : theSpeed;
		theRadius = theRadius <-2000 ? -2000 : theRadius> 2000 ? 2000 : theRadius;
		return ((RobotAgent)agent).again(theSpeed,theRadius);
	}};*/
	
}