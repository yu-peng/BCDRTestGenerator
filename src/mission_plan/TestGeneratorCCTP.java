package mission_plan;

import cctp.Assignment;
import cctp.CCTP;
import cctp.DecisionVariable;
import cctp.Episode;
import cctp.Event;
import io.IO_CCTP;


public class TestGeneratorCCTP {
	
    static double rewardLimit = 5000; // max reward per decision made
    static double activityCostLimit = 2.0; // cost of tightening survey duration at locations. unit $/ 1 minute
    static double missionCostLimit = 2.0;  // cost of extending mission durations. unit $/ 1 minute 

    static double explorationLimit = 90; // maximum stay at a location
    static double missionLimit = 3000; // maximum length of a mission
    static double preparationLimit = 10; // maximum preparation time

    // alternatives per activity
    // source of disjunctions/decisions
    static int optionLimitL = 1;
    static int optionLimitU = 1;
    
    // number of activities in a dive 
    // makes the problem very 'long'
    static int actLimitL = 1;
    static int actLimitU = 4;
    
    int auvIdx = 1;
    int missionIdx = 1;
    int actIdx = 1;
    int optIdx = 1;
    
    double centerLat = 0;
    double centerLon = 0;
    double missionRadius = 10; // kilometers
    
    // # of parallel vehicles operations 
	// makes the problem very 'wide'
    static int Nvehicles = 30;
    
    // # of dives (one after another) per vehicle
    static int Nmissions = 2;

    static int TestsPerCategory = 1;
    
    // traversal speed
    static double minSpeed = 10.0; // Speed. km/h
    static double maxSpeed = 20.0; // Speed. km/h

    static long startTime = System.currentTimeMillis();
        
    // Where to put the output files
    static String OutputFolder = "tests/AUV/";
//    static String OutputFolder = "F:/BenchmarkCases/JAIR 2015/AUV/";
    static String OutputFileHeader = "AUV";
    
    
	public static void main(String[] args) throws Exception{
		
		TestGeneratorCCTP newGenerator = new TestGeneratorCCTP(33.251060, -121.555237, 100);
		newGenerator.generate();
	}

	public TestGeneratorCCTP(double _centerLat, double _centerLon, double _missionRadius){		

		centerLat = _centerLat;
		centerLon = _centerLon;
		missionRadius = _missionRadius;
		
	}
    
	public void generate() throws Exception{		
				
		System.out.println("Test counts: " +(Nvehicles*Nmissions*TestsPerCategory)+"\tNvehicless: "+Nvehicles+"\tNmissions: "+Nmissions+"\tTestsPerCategory: "+TestsPerCategory);
		System.out.println("ExplorationLimit < " +explorationLimit+"min\tMissionLimit < "+missionLimit+"min\tStandbyLimit < "+preparationLimit+"min\tOptionLimit: ["+optionLimitL+","+optionLimitU+"]\tActLimit: ["+actLimitL+","+actLimitU+"]");
		System.out.println("RewardLimit < " +rewardLimit+"\tActivityCostLimit < "+activityCostLimit+"\tMissionCostLimit < "+missionCostLimit+"\tSpeedLimit: ["+minSpeed + "," + maxSpeed+"] km/h");
		System.out.println("Idx\tVehicle #\tMission #\tAct #\tOpt #\tEpisode #");


        int testCount = 1;
        
        for (int i = 1;i<Nvehicles+1;i++){
        	
        	for (int j=1;j<Nmissions+1;j++){
        	
	        	for (int k=0;k<TestsPerCategory;k++){
	        			        		
	        	    auvIdx = 1;
	        	    missionIdx = 1;
	        	    actIdx = 1;
	        	    optIdx = 1;
	        	    
	        	    Event start = new Event("Start");
	        	    Event end = new Event("End");
	        		
	                CCTP newCTPP = new CCTP("main");      
	                for (int car=0;car<i;car++){
	            		addVehicle(newCTPP,j,start,end);
	                }
	        		newCTPP.initialize();
	        		newCTPP.findStartEvent();
	        		newCTPP.findEndEvent();
	        		newCTPP.startEvent.setExecuted(startTime);
	        		
	        		IO_CCTP.saveCCTP(newCTPP, OutputFolder+"/"+OutputFileHeader+"-"+testCount+".cctp");
	        		IO_CCTP.saveCCTPasTPN(newCTPP, OutputFolder+"/"+OutputFileHeader+"-"+testCount+".tpn");

	        		System.out.println(testCount+"\t"+(auvIdx-1)+"\t"+(missionIdx-1)+"\t"+(actIdx-1)+"\t"+(optIdx-1)+"\t"+((optIdx-1)*4+(missionIdx-1)));
	        		
	        		testCount++;        		
	        		
	        	}     
        	
        	}
    		
        }
        
	}
	
	

	public void addVehicle(CCTP newCTPP, int missions, Event start, Event end){
		
		Location startLoc = getRandomLocation();
		Location endLoc = getRandomLocation();
		
		Event leaveStart = new Event("AUV"+auvIdx+"-"+"Leave:"+startLoc.name);
		Event arriveEnd = new Event("AUV"+auvIdx+"-"+"Arrive:"+endLoc.name);

		Event startEvent = leaveStart;
		Location startLocation = startLoc;		
	    
		Episode connector1 = new Episode("Start-Connector:"+auvIdx,0,Double.POSITIVE_INFINITY,false,false,start,leaveStart,"Constraint");
		Episode connector2 = new Episode("End-Connector:"+auvIdx,0,Double.POSITIVE_INFINITY,false,false,arriveEnd,end,"Constraint");
		
		newCTPP.addEpisode(connector1);
		newCTPP.addEpisode(connector2);
		
		for(int i=0;i<missions;i++){
			
			if (i != (missions-1)){
				Location waypoint = getRandomLocation();
				Event ArriveWaypoint = new Event("Res"+missionIdx+"-"+"Arrive:"+waypoint.name);
				addMission(startEvent,ArriveWaypoint,startLocation,waypoint,newCTPP);
				startLocation = waypoint;
				startEvent = ArriveWaypoint;
			}else{				
				addMission(startEvent,arriveEnd,startLocation,endLoc,newCTPP);				
			}
		}
		
		auvIdx++;
	}
	
	

	public void addMission(Event leaveStart, Event arriveEnd, Location start, Location end, CCTP newCTPP){
		
		int activities = getRandomInt(actLimitL,actLimitU);
		Event startEvent = leaveStart;
		Location startNode = start;
		
		for(int i=0;i<activities;i++){
			
			if (i != (activities-1)){
				Location waypoint = getRandomLocation();
				Event arriveWaypoint = new Event("Activity"+actIdx+"-"+"Arrive:"+waypoint.name);
				addActivity(startEvent,arriveWaypoint,startNode,waypoint,newCTPP);
				startNode = waypoint;
				startEvent = arriveWaypoint;
			}else{				
				addActivity(startEvent,arriveEnd,startNode,end,newCTPP);				
			}
			
			
		}
		double[] mission = getMissionDuration(activities);

		Episode reservation = new Episode("Mission"+missionIdx+"-"+"Duration",mission[0],mission[1],false,true,leaveStart,arriveEnd,Double.POSITIVE_INFINITY,getMissionDurationRelaxationCost(),"Constraint");
		newCTPP.addEpisode(reservation);
		missionIdx++;
	}
	
	public void addActivity(Event leaveStart, Event arriveEnd, Location start,Location end, CCTP newCTPP){
		
        String actName = "Activity-"+actIdx;
        DecisionVariable newAct = new DecisionVariable(actName);
        int alternatives = getRandomInt(optionLimitL,optionLimitU);
		
        for (int i=0;i<alternatives;i++){
        	
        	addOption(newAct,leaveStart,arriveEnd,start,end,newCTPP);
        	
        }
        
        newCTPP.addDecisionVariable(newAct);
        actIdx++;
	}
	
	public void addOption(DecisionVariable act, Event leaveStart, Event arriveEnd, Location start,Location end, CCTP newCTPP){
		
		// Get a destination
        Location place = getRandomLocation();
        Assignment goPlace = new Assignment(act,optIdx+"-"+place.name,getAssignmentReward());
        act.addDomainAssignment(goPlace);
        
		// Create required events
		Event arrivePlace = new Event(optIdx+"-"+"Arrive:"+place.name);
		Event leavePlace = new Event(optIdx+"-"+"Leave:"+place.name);
		Event approachEnd = new Event(optIdx+"-"+"Standby:"+end.name);

		// Sample duration
		double[] explore = getExplorationDuration();
		double[] go = getTraversalDuration(start,place);
		double[] back = getTraversalDuration(place,end);
		double[] standby = getStandbyDuration();
		
		if (go[0] == go[1]){
			go[1] += 0.01;
		}
		
		if (back[0] == back[1]){
			back[1] += 0.01;
		}
		
		if (explore[0] > explore[1]){
			System.err.println("Stay LB > UB" + explore[0] +","+ explore[1]);
		} else if (go[0] > go[1]){
			System.err.println("go LB > UB" + go[0] +","+ go[1]);
		} else if (back[0] > back[1]){
			System.err.println("back LB > UB" + back[0] +","+ back[1]);
		}else if (standby[0] > standby[1]){
			System.err.println("prepare LB > UB" + standby[0] +","+ standby[1]);
		}	
		
		
		// create constraints for it
		Episode moveStartPlace = new Episode(optIdx+"-"+"Move:"+start.name+"-"+place.name,go[0],go[1],true,true,leaveStart,arrivePlace,getTraversalRelaxationCost(),getTraversalRelaxationCost(),goPlace,"Uncontrollable;Activity");
		Episode explorePlace = new Episode(optIdx+"-"+"Explore:"+place.name,explore[0],explore[1],true,true,arrivePlace,leavePlace,getActivityRelaxationCost(),getActivityRelaxationCost(),goPlace,"Controllable;Activity");
		Episode movePlaceEnd = new Episode(optIdx+"-"+"Move:"+place.name+"-"+end.name,back[0],back[1],true,true,leavePlace,approachEnd,getTraversalRelaxationCost(),getTraversalRelaxationCost(),goPlace,"Uncontrollable;Activity");
		Episode standbyEnd = new Episode(optIdx+"-"+"Standby:"+end.name,standby[0],standby[1],false,true,approachEnd,arriveEnd,getActivityRelaxationCost(),getActivityRelaxationCost(),goPlace,"Controllable;Activity");

		moveStartPlace.mean = (go[0]+go[1])/2.0;
		moveStartPlace.variance = (go[1]-go[0])/6;
		movePlaceEnd.mean = (back[0]+back[1])/2.0;
		movePlaceEnd.variance = (back[1]-back[0])/6;
		
		newCTPP.addEpisode(moveStartPlace);
		newCTPP.addEpisode(explorePlace);
		newCTPP.addEpisode(movePlaceEnd);
		newCTPP.addEpisode(standbyEnd);
		optIdx++;
	}
	
	public double[] getTraversalDuration(Location start,Location end){

		// Try both ways
		
		double dist = distBetween(start.lat,start.lon,end.lat,end.lon); // in kilometers
		double min = dist / maxSpeed *60; // from hours to minutes
		double max = dist / minSpeed *60; // from hours to minutes
			
		return new double[]{min,max};
	}
	
	
	public static double[] getExplorationDuration(){		
		
		double lb = getRandomDouble(0,explorationLimit); 
		double ub = getRandomDouble(lb,explorationLimit); 
				
    	return new double[]{lb,ub};

	}
	
	public static double[] getStandbyDuration(){
		
		double lb = 0;
		double ub = getRandomDouble(lb,preparationLimit); // convert minutes to milliseconds
				
    	return new double[]{lb,ub};

	}

	public static double[] getMissionDuration(int activities){
		
		double lb = 0;
		double ub = getRandomDouble(activities*explorationLimit/2,Math.max(missionLimit,activities*explorationLimit/2)); // convert minutes to milliseconds
		
    	return new double[]{lb,ub};

	}
	

	
	public static double getActivityRelaxationCost(){
		
		double cost = getRandomDouble(0,activityCostLimit);
		//System.out.println(cost);
		
    	return cost;

	}
	
	public static double getTraversalRelaxationCost(){
		
		double cost = getRandomDouble(0,5.0*activityCostLimit);
		//System.out.println(cost);
		
    	return cost;

	}
	
	public static double getMissionDurationRelaxationCost(){
		
    	
		double cost = getRandomDouble(0,missionCostLimit);
		//System.out.println(cost);
		return cost;
    	
	}
	
	public static double getAssignmentReward(){
		
    	return getRandomDouble(0,rewardLimit);
	}
	
	public static double distBetween(double lat1, double lon1, double lat2, double lon2) {
		double earthRadius = 3958.75;
		double dLat = Math.toRadians(lat2-lat1);
		double dLng = Math.toRadians(lon2-lon1);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
				Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
				Math.sin(dLng/2) * Math.sin(dLng/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double dist = earthRadius * c;

		double kilometerConversion = 1.609;

		return dist * kilometerConversion;
	}
	
	public Location getRandomLocation(){
		
		double earthRadius = 6371;
		
		double nsOffset = getRandomDouble(-1*missionRadius, missionRadius);
		double ewOffset = getRandomDouble(-1*missionRadius, missionRadius);
		
		double latOffset = nsOffset/(2*Math.PI*earthRadius)*360;
		double lonOffset = ewOffset/(2*Math.PI*earthRadius)*360;
		
    	return new Location("Random Location",centerLat + latOffset, centerLon + lonOffset);       
	}
	
	public static double getRandomDouble(double Min, double Max){
		
		
		if (Min >= Max){
			
			return Min;
			
		} else {
			
			double scale = 1000.0/(Max - Min);
			double result = getRandomInt((int) Math.ceil(Min*scale), (int) Math.ceil(Max*scale))/scale;
			
			return result;
		}

	}
	
	public static int getRandomInt(int Min, int Max){
		
    	return Min + (int)(Math.random() * ((Max - Min ) + 1));

	}

}
