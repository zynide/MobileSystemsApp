package edu.illinois.mitra.demo.projectapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.objects.*;
import edu.illinois.mitra.starl.motion.*;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;

public class ProjectApp extends LogicThread {
    private static final boolean RANDOM_DESTINATION = false;
    public static final int ARRIVED_MSG = 22;
    private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
    private volatile MotionParameters param = DEFAULT_PARAMETERS;
    // this is an ArrayList of HashMap. Each HashMap element in the array will contain one set of waypoints
    final ArrayList<HashMap<String, ItemPosition>> destinations = new ArrayList<>();
    private int numSetsWaypoints = 4;
    int robotIndex;

    // used to find path through obstacles
    Stack<ItemPosition> pathStack;
    RRTNode kdTree = new RRTNode();

    ObstacleList obsList;
    //obsList is a local map each robot has, when path planning, use this map
    ObstacleList obEnvironment;
    //obEnvironment is the physical environment, used when calculating collisions

    ItemPosition currentDestination, preDestination;

    private LeaderElection le;
    //	private String leader = null;
    private boolean iamleader = false;

    private enum Stage {
        PICK, GO, DONE, ELECT, HOLD, MIDWAY
    };

    private Stage stage = Stage.PICK;

    public ProjectApp(GlobalVarHolder gvh) {
        super(gvh);
        for(int i = 0; i< gvh.gps.getPositions().getNumPositions(); i++){
            if(gvh.gps.getPositions().getList().get(i).name == name){
                robotIndex = i;
                break;
            }

        }

        // instantiates each HashMap object in the array
        for(int i = 0; i < numSetsWaypoints; i++) {
            destinations.add(new HashMap<String, ItemPosition>());
        }
        le = new RandomLeaderElection(gvh);


        MotionParameters.Builder settings = new MotionParameters.Builder();
//		settings.ROBOT_RADIUS(400);
        settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLBACK);
        MotionParameters param = settings.build();
        gvh.plat.moat.setParameters(param);

        // this loop gets add each set of waypoints i to the hashmap at destinations(i)
        for(ItemPosition i : gvh.gps.getWaypointPositions()) {
            String setNumStr = i.getName().substring(0,1);
            int setNum = Integer.parseInt(setNumStr);
            destinations.get(setNum).put(i.getName(), i);
        }


        //point the environment to internal data, so that we can update it
        obEnvironment = gvh.gps.getObspointPositions();

        //download from environment here so that all the robots have their own copy of visible ObstacleList
        obsList = gvh.gps.getViews().elementAt(robotIndex) ;

        gvh.comms.addMsgListener(this, ARRIVED_MSG);
    }

    @Override
    public List<Object> callStarL() {
        int i = 0;
        while(true) {
            obEnvironment.updateObs();

            obsList.updateObs();
            if((gvh.gps.getMyPosition().type == 0) || (gvh.gps.getMyPosition().type == 1)){

                switch(stage) {
                    case ELECT:
					/*
					le.elect();
					if(le.getLeader() != null) {
						results[1] = le.getLeader();
					}
					*/
                        stage = Stage.PICK;

                        break;
                    case PICK:
                        if(destinations.get(i).isEmpty()) {
                            if(i+1 >= numSetsWaypoints) {
                                stage = Stage.DONE;
                            }
                            else {
                                i++;
                            }
                        } else
                        {

                            //			RobotMessage informleader = new RobotMessage("ALL", name, 21, le.getLeader());
                            //			gvh.comms.addOutgoingMessage(informleader);

                            //			iamleader = le.getLeader().equals(name);
                            iamleader = true;

                            if(iamleader)
                            {
                                currentDestination = getRandomElement(destinations.get(i));

                                RRTNode path = new RRTNode(gvh.gps.getPosition(name).x, gvh.gps.getPosition(name).y);
                                pathStack = path.findRoute(currentDestination, 5000, obsList, 5000, 3000, (gvh.gps.getPosition(name)), (int) (gvh.gps.getPosition(name).radius*0.8));

                                kdTree = RRTNode.stopNode;
                                //wait when can not find path
                                if(pathStack == null){
                                    stage = Stage.HOLD;
                                }
                                else{
                                    preDestination = null;
                                    stage = Stage.MIDWAY;
                                }
                            }

						/*
						else
						{
						currentDestination = gvh.gps.getPosition(le.getLeader());
						currentDestination1 = new ItemPosition(currentDestination);
						int newx, newy;
						if(gvh.gps.getPosition(name).getX() < currentDestination1.getX())
						{
							newx = gvh.gps.getPosition(name).getX() - currentDestination1.getX()/8;
						}
						else
						{
							newx = gvh.gps.getPosition(name).getX() + currentDestination1.getX()/8;
						}
						if(gvh.gps.getPosition(name).getY() < currentDestination1.getY())
						{
							newy = gvh.gps.getPosition(name).getY() - currentDestination1.getY()/8;
						}
						else
						{
							newy = gvh.gps.getPosition(name).getY() + currentDestination1.getY()/8;
						}
						currentDestination1.setPos(newx, newy, (currentDestination1.getAngle()));
		//				currentDestination1.setPos(currentDestination);
						gvh.plat.moat.goTo(currentDestination1, obsList);
						stage = Stage.HOLD;
						}
						*/
                        }
                        break;


                    case MIDWAY:
                        if(!gvh.plat.moat.inMotion) {
                            if(pathStack == null){
                                stage = Stage.HOLD;
                                // if can not find a path, wait for obstacle map to change
                                break;
                            }
                            if(!pathStack.empty()){
                                //if did not reach last midway point, go back to path planning
                                if(preDestination != null){
                                    if((gvh.gps.getPosition(name).distanceTo(preDestination)>param.GOAL_RADIUS)){
                                        pathStack.clear();
                                        stage = Stage.PICK;
                                        break;
                                    }
                                    preDestination = pathStack.peek();
                                }
                                else{
                                    preDestination = pathStack.peek();
                                }
                                ItemPosition goMidPoint = pathStack.pop();
                                gvh.plat.moat.goTo(goMidPoint, obsList);
                                stage = Stage.MIDWAY;
                            }
                            else{
                                if((gvh.gps.getPosition(name).distanceTo(currentDestination)>param.GOAL_RADIUS)){
                                    pathStack.clear();
                                    stage = Stage.PICK;
                                }
                                else{
                                    if(currentDestination != null){
                                        destinations.get(i).remove(currentDestination.getName());
                                       // RobotMessage inform = new RobotMessage("ALL", name, ARRIVED_MSG, currentDestination.getName());
                                      //  gvh.comms.addOutgoingMessage(inform);
                                        stage = Stage.PICK;
                                    }
                                }
                            }
                        }
                        break;

                    case GO:
                        if(!gvh.plat.moat.inMotion) {
                            if(currentDestination != null)
                                destinations.get(i).remove(currentDestination.getName());
                            //RobotMessage inform = new RobotMessage("ALL", name, ARRIVED_MSG, currentDestination.getName());
                            //gvh.comms.addOutgoingMessage(inform);
                            stage = Stage.PICK;
                        }

                        break;
                    case HOLD:
                        //			if(gvh.gps.getMyPosition().distanceTo(gvh.gps.getPosition(le.getLeader())) < 1000 )
                        //			{
                        //			stage = Stage.PICK;
                        //		    }
                        //			else
                    {
                        gvh.plat.moat.motion_stop();
                    }
                    break;

                    case DONE:
                        gvh.plat.moat.motion_stop();
                        return null;
                }
            }
            else{
                currentDestination = getRandomElement(destinations.get(i));
                gvh.plat.moat.goTo(currentDestination, obsList);
            }
            sleep(100);
        }
    }

    @Override
    protected void receive(RobotMessage m) {
        String posName = m.getContents(0);
        if(destinations.get(0).containsKey(posName))
            destinations.get(0).remove(posName);

        if(currentDestination.getName().equals(posName)) {
            gvh.plat.moat.cancel();
            stage = Stage.PICK;
        }

    }

    private static final Random rand = new Random();

    @SuppressWarnings("unchecked")
    private <X, T> T getRandomElement(Map<X, T> map) {
        if(RANDOM_DESTINATION)
            return (T) map.values().toArray()[rand.nextInt(map.size())];
        else
            return (T) map.values().toArray()[0];
    }
}