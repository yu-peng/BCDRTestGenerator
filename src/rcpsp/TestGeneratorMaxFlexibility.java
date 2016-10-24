package rcpsp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import cctp.CCTP;
import cctp.Episode;
import cctp.Event;
import io.IO_CCTP;
import utils.NumUtils;

public class TestGeneratorMaxFlexibility {
	
	public static void main(String[] args){
		
		String inputDataPath = "data/RCPSP_data/";
		String inputSubFolder = "J10";
		String outputProblemPath = "tests/RCPSP_MaxFlexibility/";
		
		System.out.println("InputData: "+inputDataPath+inputSubFolder);
		System.out.println("Name\tEpisode #");
		
		generateCCTPs(inputDataPath,inputSubFolder,outputProblemPath);
	}

	public static void generateCCTPs(String inputDataPath, String inputSubFolder, String outputProblemPath){
					
		File folder = new File(inputDataPath);

	    for (final File subfolder : folder.listFiles()) {
	    		    	
	        if (subfolder.isDirectory() && subfolder.getName().equals(inputSubFolder)) {
	    	    for (final File inputfile : subfolder.listFiles()) {
    	
	    			String filename =  inputDataPath + "/" + subfolder.getName() + "/" + inputfile.getName();
	    			String outputFolder =  outputProblemPath;

	    			if (filename.contains(".pos")){
	    				
	    				CCTP newCCTP = new CCTP("J10 Max Flexibility");
	    				HashMap<String,Event> eventMap= new HashMap<String,Event>();
	    				HashMap<String,Episode> activityKeyMap = new HashMap<String,Episode>();
	    				ArrayList<Episode> uncontrollableEpisodes = new ArrayList<Episode>();
	    				
	    				
	    				try {
	    					
	    					BufferedReader br = new BufferedReader(new FileReader(filename));
	    					String line;

	    					while ((line = br.readLine()) != null) {

	    						String[] elements = line.split("(\\s)");
//	    						System.out.println(elements.length);
	    						
    							for (int i = 0; i < elements.length; i++){
    								elements[i] = elements[i].replaceAll("(\\[|\\])", "");
    							}

	    						
	    						if (elements.length >= 4){
	    							
	    							
	    							
//	    							System.out.println(elements[0]+";"+elements[1]+";"+elements[2]+";"+elements[3]+";"+elements[4]+";"+elements[5]+";"+elements[6]+";"+elements[7]+";"+elements[8]+";"+elements[9]+";"+elements[10]+";"+elements[11]+";"+elements[12]+";");
	    						
	    							// First four elements represent an uncontrollable duration
	    							
	    							Event start = eventMap.get(elements[0]+"-start");
	    							Event end = eventMap.get(elements[0]+"-end");
	    							
	    							if (start == null){
	    								start = new Event(elements[0]+"-start");
	    								eventMap.put(elements[0]+"-start", start);
	    							}
	    							
	    							if (end == null){
	    								end = new Event(elements[0]+"-end");
	    								eventMap.put(elements[0]+"-end", end);
	    							}
	    							
	    							String key = start.name+" -> "+end.name;
	    							double min_duration = NumUtils.getDoublefromString(elements[1]);

	    							if (!activityKeyMap.containsKey(key) && min_duration > 0.00001){
	    								Episode newActivity = createEpisode(new Episode(key,min_duration,10000,false,true,start,end,0,0.00001,"Uncontrollable;Activity"),newCCTP);

	    								double mean_duration = NumUtils.getDoublefromString(elements[1]);
		    							double var_duration = 0.01*mean_duration*mean_duration;
	    								
	    								newActivity.mean = mean_duration;
	    								newActivity.variance = var_duration;
	    								newActivity.distribution = "Normal";
	    								
	    								activityKeyMap.put(key,newActivity);
	    								uncontrollableEpisodes.add(newActivity);
	    							}
	    							
	    							
	    							
	    							// The following elements represent the links between activities
	    							for (int i = 0; i < (elements.length-4)/2; i++){
	    								
	    								int idx = i+4;
	    								
	    								double bound = NumUtils.getDoublefromString(elements[idx+(elements.length-4)/2]);
		    							boolean isLowerbound = false;
	    								
		    							if (bound >= 0){
		    								isLowerbound = true;
		    							}
	    								
	
		    							
		    							if (isLowerbound){
		    								
			    							Event constraintStart = eventMap.get(elements[0]+"-end");
			    							Event constraintEnd = eventMap.get(elements[idx]+"-start");
			    							
			    							if (constraintStart == null){
			    								constraintStart = new Event(elements[0]+"-end");
			    								eventMap.put(elements[0]+"-end", constraintStart);
			    							}
			    							
			    							if (constraintEnd == null){
			    								constraintEnd = new Event(elements[idx]+"-start");
			    								eventMap.put(elements[idx]+"-start", constraintEnd);
			    							}
			    							
			    							String constraintKey = constraintStart.name+" -> "+constraintEnd.name;
			    							Episode newConstraint = activityKeyMap.get(constraintKey);	
							
			    							if (newConstraint == null){
			    								newConstraint = createEpisode(new Episode(constraintKey,bound,Double.POSITIVE_INFINITY,false,false,constraintStart,constraintEnd,"Controllable;Constraint"),newCCTP);	    								
			    								activityKeyMap.put(constraintKey, newConstraint);
			    							} else {
			    								newConstraint.setLB(bound);
			    							}
			    							

		    							} else {
		    								
		    								Event constraintStart = eventMap.get(elements[idx]+"-end");
			    							Event constraintEnd = eventMap.get(elements[0]+"-start");
			    							
			    							if (constraintStart == null){
			    								constraintStart = new Event(elements[idx]+"-end");
			    								eventMap.put(elements[idx]+"-end", constraintStart);
			    							}
			    							
			    							if (constraintEnd == null){
			    								constraintEnd = new Event(elements[0]+"-start");
			    								eventMap.put(elements[0]+"-start", constraintEnd);
			    							}
			    							
			    							String constraintKey = constraintStart.name+" -> "+constraintEnd.name;
			    							Episode newConstraint = activityKeyMap.get(constraintKey);	

			    							if (newConstraint == null){
			    								newConstraint = createEpisode(new Episode(constraintKey,Double.NEGATIVE_INFINITY,-1*bound,false,false,constraintStart,constraintEnd,"Controllable;Constraint"),newCCTP);	    								
			    								activityKeyMap.put(constraintKey, newConstraint);
			    							} else {
			    								newConstraint.setUB(-1*bound);
			    							}
			    							
		    							}
	    								
	    							}
	    							
	    						}			

	    					}
	    					
	    					br.close();

	    				} catch (IOException e) {
	    					// TODO Auto-generated catch block
	    					e.printStackTrace();
	    				}
	    				
	    				for (Episode uncontrollableEpisode : uncontrollableEpisodes){
	    					for (Episode outEpisode : uncontrollableEpisode.endEvent.outgoingEpisodes){
	    						outEpisode.setLB(outEpisode.lb - uncontrollableEpisode.lb);
	    						outEpisode.setUB(outEpisode.ub - uncontrollableEpisode.lb);

	    						
//    							System.out.println(outEpisode.name + " : [" + outEpisode.getLB() + "," + outEpisode.getUB()+"]");
	    					}
	    				}
	    				
	    				for (Episode episode : newCCTP.episodes.values()){
	    					if (episode.isControllable()){
	    						if (Double.isInfinite(episode.ub)){
	    							episode.ub = 1000000.0;
	    						}
	    						
	    						if (Double.isInfinite(episode.lb)){
	    							episode.lb = -1000000.0;
	    						}
//	    						System.out.println(episode.startEvent.name+"->"+episode.endEvent.name+"["+episode.lb+","+episode.ub+"]");
	    					}
	    				}
	    				
	    				newCCTP.findStartEvent();		
	    				newCCTP.findEndEvent();
	    				newCCTP.startEvent.setExecuted(1394175600000.0);
	    				
	    				String outputCCTPName = outputFolder + "/" + subfolder.getName() + "/" + inputfile.getName().replace("_data", "_cctp").replace(".pos", ".cctp");
	    				String outputTPNName = outputFolder + "/" + subfolder.getName() + "/" + inputfile.getName().replace("_data", "_cctp").replace(".pos", ".tpn");

	    				IO_CCTP.saveCCTP(newCCTP, outputCCTPName);
	    				IO_CCTP.saveCCTPasTPN(newCCTP, outputTPNName);
	    				System.out.println(inputfile.getName().replace("_data", "").replace(".pos", "") + "\t" + newCCTP.episodes.size());
	    				
	    				
	    			}
	    	    }
	        }
	    }	
		
	}

	public static Episode createEpisode(Episode newEpisode, CCTP mission){
		
		mission.addEpisode(newEpisode);
		
		return newEpisode;
	}

}
