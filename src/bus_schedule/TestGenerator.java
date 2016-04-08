package bus_schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import io.IO_CCTP;
import cctp.CCTP;
import cctp.Candidate;
import cctp.Episode;
import cctp.Event;

public class TestGenerator {
	
	public static HashSet<String> serviceIds = new HashSet<String>();
	public static HashMap<String,String> tripRouteMap = new HashMap<String,String>();
	public static HashMap<String,String> tripServiceIdMap = new HashMap<String,String>();
	public static HashMap<String,String> tripHeadSignMap = new HashMap<String,String>();
	public static HashMap<String,String> tripDirectionMap = new HashMap<String,String>();
	public static HashMap<String,String> stopNameMap = new HashMap<String,String>();
	public static HashMap<String,PTTrip> ptTrips = new HashMap<String,PTTrip>();
	public static HashSet<String> route_trips = new HashSet<String>();
	public static HashMap<String,ArrayList<TravelTime>> travelTimes = new HashMap<String,ArrayList<TravelTime>>();
	public static HashMap<String,ArrayList<DwellTime>> dwellTimes = new HashMap<String,ArrayList<DwellTime>>();
	public static int categories = 72;
	public static int segmentSize = 86400/categories;

	public static String date = "2016-03-30";
	public static int day = 20160330;
	public static int day_of_week = 5; // Sunday is 1
	
	public static double minDwell = 0.5;
	public static double maxDwell = 0.5;
	public static double traversalUncertainty = 1;
	
	public static double defaultScheduleTolerance = 0.1;
	public static double defaultHeadwayTolerance = 1.0;
	public static double scheduleCost = 10;
	public static double headwayCost = 100;
	
	public static String route_id = "Red";
	public static String direction = "0";
	
	
	public static String[] stop_ids = new String[]{"70061","70063","70065","70067","70069","70071","70073","70075","70077","70079","70081","70083","70085","70095"};

	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
		// Generate a CCTP problem from the schedule of MBTA route 1
		
		int max_trips = 10; 
		
		String routeDataPrefix = "data/MBTA_GTFS/";
		String performanceDataPrefix = "data/Redline_performance/";
		
		System.out.println("Route: "+route_id+"\tDirection: "+direction+"\tDate: "+date+"\tDayofWeek: "+day_of_week);
		System.out.println("MaxTrips: "+max_trips+"\tStops: "+Arrays.toString(stop_ids));
		System.out.println("ScheduleTolerance "+defaultScheduleTolerance + "\tScheduleCost "+scheduleCost + "\tHeadwayTolerance "+defaultHeadwayTolerance + "\tHeadwayCost "+headwayCost);
		System.out.println("Name\tStops #\tTrips #\tEpisode #");		
		
		// We first parse the trips
		// then retrieve their stops times
		// then construct a CCTP using it
		
		serviceIds = new HashSet<String>();
		tripRouteMap = new HashMap<String,String>();
		tripServiceIdMap = new HashMap<String,String>();
		tripHeadSignMap = new HashMap<String,String>();
		tripDirectionMap = new HashMap<String,String>();
		stopNameMap = new HashMap<String,String>();
		ptTrips = new HashMap<String,PTTrip>();
		route_trips = new HashSet<String>();	
		
		parseTrip(routeDataPrefix+"trips.txt",routeDataPrefix+"calendar.txt", routeDataPrefix+"calendar_dates.txt",
				day_of_week,day);
		parseStopTime(routeDataPrefix+"stop_times.txt");
		parseStop(routeDataPrefix+"stops.txt");
		parseHistoricalTravelTimes(performanceDataPrefix + "travel_times/");
		parseHistoricalDwellTimes(performanceDataPrefix + "dwell_times/");
		
//		System.out.println("Route "+route_id+" trips (D1): " + route_trips.size() + "/" + ptTrips.size());
		
		for (int trips = 1; trips <= max_trips; trips++){
				
				CCTP newCCTP = generateCCTP(trips,routeDataPrefix);	
				
				String outFilename = "tests/MBTA/Route_"+route_id+"_"+trips;
				
//				System.out.println("Saved to " + outFilename);
				IO_CCTP.saveCCTP(newCCTP, outFilename+".cctp");
				IO_CCTP.saveCCTPasTPN(newCCTP, outFilename+".tpn");
        		System.out.println("Route_"+route_id+"_"+trips+"\t"+stop_ids.length+"\t"+trips+"\t"+newCCTP.episodes.size());
		}
	}

	
	public static CCTP generateCCTP(int max_trips, String routeDataPrefix) throws IOException, ParseException{
		
		ArrayList<PTTrip> route = new ArrayList<PTTrip>();
		
		// Add route trips to the list and sort based on their departure time
		for (String trip_id : route_trips){
			
			PTTrip trip = ptTrips.get(trip_id);
			
			if (route.size() == 0){
				route.add(trip);
			} else {
				for (int idx = 0; idx < route.size(); idx++){		
					if (trip.arrival_times.get(1) < route.get(idx).arrival_times.get(1)){
						route.add(idx, trip);
						break;
					}					
				}
			}			
		}
		
		CCTP newCCTP = new CCTP("Transit Test Route " + route_id);
		Event start = new Event("Start");
		Event end = new Event("End");
		
		newCCTP.setStartEvent(start);
		newCCTP.setEndEvent(end);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");	
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date d = sdf.parse(date);						
		long reference_day_time = d.getTime();
		start.setExecuted(reference_day_time);
		
		HashMap<Integer,Event> stopEventMap = new HashMap<Integer,Event>();
		double headway = 0;
		
		// go through the list of trips
		for (int idx = 0; idx < route.size(); idx++){	
			
			if (idx >= max_trips){
				break;
			}
			
			PTTrip trip = route.get(idx);
			String trip_id = trip.tripID;
			
			// get headway
			if (idx > 0){
				PTTrip prev_trip = route.get(idx-1);
				headway = (trip.arrival_times.get(1) - prev_trip.arrival_times.get(1))/60000.0;
			}
			
//			System.out.println("Using trip: " + trip_id + ":" + trip.arrival_times.get(1) + "/" + trip.departure_times.get(1));
			Collections.sort(trip.stop_sequences);
			
			Event departure_origin = new Event("Wait for Departure Origin");
			Episode wait_origin = createEpisode(new Episode("Wait for Departure Origin",0,0.01
					,false,true,start,departure_origin,"Controllable;Activity"),newCCTP);

			Event current_event = departure_origin;
			
			for (int stopIdx = 0; stopIdx < trip.stop_sequences.size(); stopIdx++){
				
//				System.out.println("Stop seq: " + stop_seq);
				int stop_seq = trip.stop_sequences.get(stopIdx);
				String stop_id = trip.stop_ids.get(stop_seq);
				
				if (!Arrays.asList(stop_ids).contains(stop_id)){
					continue;
				}
				
				long arrival_time = trip.arrival_times.get(stop_seq);
				long departure_time = trip.departure_times.get(stop_seq);
				
//				System.out.println("Adding stop: " + stop_id);
				
				// Arrival schedule constraint for this stop
				long scheduled_arrival = arrival_time - reference_day_time;
				
				Episode arrival_constraint = createEpisode(new Episode("Trip " + trip_id + " Arrive at stop " + stop_id + " on " + convertToSimpleTime(scheduled_arrival),
						scheduled_arrival/60000.0-defaultScheduleTolerance,scheduled_arrival/60000.0+defaultScheduleTolerance
						,true,true,start,current_event,"Controllable;Constraint"),newCCTP);
				
				arrival_constraint.setLBRelaxRatio(scheduleCost);
				arrival_constraint.setUBRelaxRatio(scheduleCost);

				// Add an additional constraint to maintain headway between trips
				if (stopEventMap.containsKey(stop_seq)){
					
					Event prev_arrival = stopEventMap.get(stop_seq);
					
					Episode headway_constraint = createEpisode(new Episode("Headway " + headway,
							headway-defaultHeadwayTolerance,headway+defaultHeadwayTolerance
							,true,true,prev_arrival,current_event,"Controllable;Constraint"),newCCTP);
					
//					headway_constraint.setLBRelaxRatio(headwayCost);
//					headway_constraint.setUBRelaxRatio(headwayCost);
					
				}
				
				stopEventMap.put(stop_seq, current_event);
				
				// First, passenger on/off
				Event dwellComplete = new Event("Dwell Complete at " + stop_id + " ("+stopNameMap.get(stop_id)+")");
				Episode dwell = createEpisode(new Episode("Dwell at " + stop_id + " ("+stopNameMap.get(stop_id)+")",minDwell,maxDwell
						,false,false,current_event,dwellComplete,"Uncontrollable;Activity"),newCCTP);
				
				setDwellTime(dwell,stop_id,arrival_time);
				
				current_event = dwellComplete;
				
				if (stopIdx+1 < trip.stop_sequences.size()){
					int next_stop_seq = trip.stop_sequences.get(stopIdx+1);
					String next_stop_id = trip.stop_ids.get(next_stop_seq);
					
					if (Arrays.asList(stop_ids).contains(next_stop_id)){
						long next_arrival_time = trip.arrival_times.get(next_stop_seq);
						double duration = (next_arrival_time-departure_time)/60000.0;
						
						
						// Second, wait for departure
						Event departure = new Event("Leave stop " + stop_id);
						Episode wait = createEpisode(new Episode(trip_id + " Wait for departure at " + stop_id,0,0.01
								,false,true,dwellComplete,departure,"Controllable;Constraint"),newCCTP);
						wait.setUBRelaxRatio(0.1);
						
						// Third, traversal to the next stop
						Event arrival = new Event("Arrive at Stop " + stop_id+ " ("+stopNameMap.get(next_stop_id)+")");
						Episode traversal = createEpisode(new Episode("From " + stop_id + " to " + next_stop_id + " ("+stopNameMap.get(next_stop_id)+")",duration,duration+traversalUncertainty
								,false,false,departure,arrival,"Uncontrollable;Activity"),newCCTP);
						current_event = arrival;
						setTravelTime(traversal, stop_id, next_stop_id, arrival_time);
						
					}

				}
				
//				System.out.println(stop_seq + ":" + trip.stop_ids.get(stop_seq) + 
//						" [" + trip.arrival_times.get(stop_seq) + "," + trip.departure_times.get(stop_seq) + "]");		
				
			}
			
			Episode trip_completion = createEpisode(new Episode("Trip Complete",0,Double.POSITIVE_INFINITY
					,false,false,current_event,end,"Controllable;Constraint"),newCCTP);
			
		}
		
		return newCCTP;
	}
	
	
	public static void parseHistoricalTravelTimes(String travelDataPrefix){
		File dir = new File(travelDataPrefix);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				// Do something with child
				
				try (BufferedReader br = new BufferedReader(new FileReader(child))) {
					
				    String line;
				    while ((line = br.readLine()) != null) {
				       
				    	String[] elements = line.split(",");
				    	if (elements.length == 8){
				    		
				    		String fromStopID = elements[0];
				    		String toStopID = elements[1];
				    		String route = elements[2];
				    		int travelTime = Integer.parseInt(elements[3]);
				    		int travelDirection = Integer.parseInt(elements[4]);
				    		long departureTime = Integer.parseInt(elements[5]);
				    		long arrivalTime = Integer.parseInt(elements[6]);
				    		int benchmarkTime = Integer.parseInt(elements[7]);
				    		
				    		TravelTime time = new TravelTime(route,fromStopID,toStopID,travelDirection,departureTime,arrivalTime,travelTime);
				    		time.setBenchmarkTime(benchmarkTime);
				    		
				    		
				    		String key = ((departureTime % 86400) / segmentSize) + ":" +fromStopID + "-" + toStopID;
				    		
				    		if (travelTimes.containsKey(key)){
				    			
				    			ArrayList<TravelTime> times = travelTimes.get(key);
				    			times.add(time);
				    			
				    		} else {				    			
//				    			System.out.println("Adding Travel Time " + key);
				    			ArrayList<TravelTime> times = new ArrayList<TravelTime>();
				    			times.add(time);
				    			travelTimes.put(key,times);
				    		}
				    	}
				    }
				    
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		}
	}
	
	
	public static void parseHistoricalDwellTimes(String travelDataPrefix){
		File dir = new File(travelDataPrefix);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				// Do something with child
				
				try (BufferedReader br = new BufferedReader(new FileReader(child))) {
					
				    String line;
				    while ((line = br.readLine()) != null) {
				       
				    	String[] elements = line.split(",");
				    	if (elements.length == 6){
				    		
				    		String stopID = elements[0];
				    		int dwellTime = Integer.parseInt(elements[1]);
				    		String route = elements[2];
				    		int travelDirection = Integer.parseInt(elements[3]);
				    		long arrivalTime = Integer.parseInt(elements[4]);
				    		long departureTime = Integer.parseInt(elements[5]);
				    		
				    		DwellTime time = new DwellTime(route,stopID,travelDirection,arrivalTime,departureTime,dwellTime);				
				    		
				    		String key = ((departureTime % 86400) / segmentSize)+":"+stopID;
				    		
//				    		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");	
//			    			System.out.println("Adding Dwell Time " + sdf2.format(d));
				    		if (dwellTimes.containsKey(key)){
				    			
				    			ArrayList<DwellTime> times = dwellTimes.get(key);
				    			times.add(time);
				    			
				    		} else {

				    			ArrayList<DwellTime> times = new ArrayList<DwellTime>();
				    			times.add(time);
				    			dwellTimes.put(key,times);
				    		}
				    	}
				    }
				    
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		}
	}
	
	
	public static void setDwellTime(Episode dwellEpisode, String stopId, long time){
		
		boolean historicalDataFound = false;
		
		long seconds = time / 1000;
		long segment = (seconds % 86400) / segmentSize;
		long nextSegment = segment + 1;
		if (nextSegment >= categories){
			nextSegment = 0;
		}
		long prevSegment = segment - 1;
		if (prevSegment < 0){
			prevSegment = categories-1;
		}		
		
//		double mean = 0;
		for (long segmentIdx : new long[]{prevSegment,segment,nextSegment}){
			ArrayList<DwellTime> times = dwellTimes.get(segmentIdx + ":" + stopId);

			if (times != null){
				for (DwellTime dwellTime : times){				
					if (Math.abs((seconds % 86400) - (dwellTime.arrTime % 86400)) < segmentSize 
							|| Math.abs((seconds % 86400)+86400 - (dwellTime.arrTime % 86400)) < segmentSize 
							|| Math.abs((seconds % 86400) - (dwellTime.arrTime % 86400) - 86400) < segmentSize){					
//						mean += dwellTime.dwellTime;
						dwellEpisode.addHistoricalData(dwellTime.dwellTime/60.0);
						historicalDataFound = true;
					}
				}
			}		
		}
		
		if (historicalDataFound){
//			mean = mean / 60.0 / dwellEpisode.historical_durations.size();
//			
//	        double var = 0;
//	        for(double a :dwellEpisode.historical_durations){
//	        	var += (mean-a)*(mean-a);
//	        }        	
//	        var = var / dwellEpisode.historical_durations.size();
//			dwellEpisode.setLB(mean-2*Math.sqrt(var));
//			dwellEpisode.setUB(mean+2*Math.sqrt(var));
			
			Collections.sort(dwellEpisode.historical_durations);
			
			dwellEpisode.setLB(dwellEpisode.historical_durations.get(0));	        
	        dwellEpisode.setUB(dwellEpisode.historical_durations.get((int) Math.floor(0.98*dwellEpisode.historical_durations.size())));
			
//	        System.out.println("Mean dwell at " + stopId + ":[" + dwellEpisode.getLB() + " ," + dwellEpisode.getUB() + "] from size: " + ((int) Math.floor(0.98*dwellEpisode.historical_durations.size()))+"/"+dwellEpisode.historical_durations.size());
		}		
	}
	
	public static void setTravelTime(Episode travelEpisode, String fromStopId, String toStopId, long time){
		
		boolean historicalDataFound = false;
		
		long seconds = time / 1000;
		long segment = (seconds % 86400) / segmentSize;
		long nextSegment = segment + 1;
		if (nextSegment >= categories){
			nextSegment = 0;
		}
		long prevSegment = segment - 1;
		if (prevSegment < 0){
			prevSegment = categories-1;
		}		
		
//		double mean = 0;
		for (long segmentIdx : new long[]{prevSegment,segment,nextSegment}){
			ArrayList<TravelTime> times = travelTimes.get(segmentIdx + ":" + fromStopId + "-" + toStopId);

			if (times != null){
				for (TravelTime travelTime : times){				
					if (Math.abs((seconds % 86400) - (travelTime.depTime % 86400)) < segmentSize 
							|| Math.abs((seconds % 86400)+86400 - (travelTime.depTime % 86400)) < segmentSize 
							|| Math.abs((seconds % 86400) - (travelTime.depTime % 86400) - 86400) < segmentSize){					
//						mean += travelTime.travelTime;
						travelEpisode.addHistoricalData(travelTime.travelTime/60.0);
						historicalDataFound = true;
					}
				}
			}		
		}
		
		if (historicalDataFound){
//			mean = mean / 60.0 / travelEpisode.historical_durations.size();
			
//	        double var = 0;
//	        for(double a :travelEpisode.historical_durations){
//	        	var += (mean-a)*(mean-a);
//	        }        	
//	        var = var / travelEpisode.historical_durations.size();
			
			Collections.sort(travelEpisode.historical_durations);
			
	        travelEpisode.setLB(travelEpisode.historical_durations.get(0));	        
	        travelEpisode.setUB(travelEpisode.historical_durations.get((int) Math.floor(0.98*travelEpisode.historical_durations.size())));
			
//			System.out.println("Mean travel " + fromStopId + "-" + toStopId + ":[" + travelEpisode.getLB() + " ," + travelEpisode.getUB() + "] from size: " + ((int) Math.floor(0.98*travelEpisode.historical_durations.size()))+"/"+travelEpisode.historical_durations.size());
		}		
	}
	
	public static void parseTrip(String tripFileName, String calendarFileName, String calendarDateFileName,
			int currentWeekDay, int currentDay) throws IOException{
		
		BufferedReader br;
		String line;
		int startDate;//for calendar
		int endDate;//for calendar

		//parse calendar to see which service ids are used
		br = new BufferedReader(new FileReader(calendarFileName));
		line = br.readLine();
		
		// Only consider services that are running on the specified day
		while ((line = br.readLine()) != null){
			String[] elements = line.split("\"");
			if (elements[2].substring(2*currentWeekDay-1,2*currentWeekDay).equals("1")){
				startDate=Integer.parseInt(elements[3]);
				endDate=Integer.parseInt(elements[5]);
				
				
				if (currentDay>=startDate&&currentDay<=endDate){
					serviceIds.add(elements[1]);
				}
			}
		}
		br.close();

		//parse calendar dates to add/minus special cases
		br = new BufferedReader(new FileReader(calendarDateFileName));
		line = br.readLine();
		
		while ((line = br.readLine()) != null){
			String[] elements = line.split("\"");
			//System.out.println(Integer.parseInt(elements[3]));
			if (Integer.parseInt(elements[3])==currentDay){
				if (elements[4].equals(",2")){
					serviceIds.remove(elements[1]);
					//System.out.println(elements[1]);
				} else{
					serviceIds.add(elements[1]);
				}
			}
		}
		br.close();

		br = new BufferedReader(new FileReader(tripFileName));
		line = br.readLine();
		line = br.readLine();
		while (line != null){
			
			String[] elements = line.split("\"");
			
			//don't consider boat etc
			if (elements.length > 11){
				if (serviceIds.contains(elements[3])){
					
					String tripID = elements[5];
					
					tripRouteMap.put(tripID,elements[1]);
					tripHeadSignMap.put(tripID, elements[7]);
					tripDirectionMap.put(tripID,elements[10].substring(1,2));
					
					// Only add this trip if its direction is from harvard to Boston
//					System.out.println("Route id: " + elements[1] + "/" + elements[10].substring(1,2));

					if (elements[1].equals(route_id) && elements[10].substring(1,2).equals(direction)){
						route_trips.add(tripID);
					}
				}
			}			
			
			line = br.readLine();
		}
		
		br.close();		
	}
	
	public static void parseStop(String filename) throws IOException{
		
		BufferedReader br;
		String line;
		int expNum;
		br = new BufferedReader(new FileReader(filename));
		line = br.readLine();
		line = br.readLine();
		
		while (line != null){
			String[] elements = line.split("\"");
			
			String stopId = elements[1];			
			String stopName = elements[5];

			stopNameMap.put(stopId, stopName);
//			System.out.println(stopId + "--" + stopName);
			line = br.readLine();
		}
		br.close();

	}
	
	public static void parseStopTime(String filename) throws IOException{
		
		BufferedReader br;
		String line;
		String currentTrip=null;

		// write a short version for future access
//		PrintWriter writer = new PrintWriter(filename+".small", "UTF-8");

		
		//create stop route adjacency matrix
		br = new BufferedReader(new FileReader(filename));
		line = br.readLine();
//		line = br.readLine();
		while (line != null){
			String[] elements = line.split("\"");
			
			
			String tripID = elements[1];
			String shapeID = elements[7];
			
			if (tripID.equalsIgnoreCase("trip_id")){
				line = br.readLine();
				continue;
			}
			
			if (tripRouteMap.containsKey(tripID)){
				String stop_id = elements[7];
				
				int stop_sequence = Integer.parseInt(elements[9]);
				String arrival_time = elements[3];
				String departure_time = elements[5];
								
				try {
					
					SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");	
				    f.setTimeZone(TimeZone.getTimeZone("UTC"));
					Date d = f.parse(date+" " +arrival_time);						
					long arrival_time_ms = d.getTime();
					d = f.parse(date+" " +departure_time);						
					long departure_time_ms = d.getTime();
					
					if (ptTrips.containsKey(tripID)){

						PTTrip trip = ptTrips.get(tripID);
						trip.add_stop(stop_sequence, stop_id, arrival_time_ms, departure_time_ms);
						
					} else {
						
						PTTrip trip = new PTTrip(tripID,shapeID);
						trip.add_stop(stop_sequence, stop_id, arrival_time_ms, departure_time_ms);
						ptTrips.put(tripID, trip);
						
					}
		
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			
//			if (route_trips.contains(tripID)){
//				System.out.println(line);
//			}
			
			line = br.readLine();
		}
		br.close();

//		writer.close();
	}
	
	public static Episode createEpisode(Episode newEpisode, CCTP mission){
		
		mission.addEpisode(newEpisode);
		
		return newEpisode;
	}

	public static String convertToSimpleTime(long millisecond){
		
		StringBuilder sb = new StringBuilder();		

		int minutes = (int) ((millisecond / (1000*60)) % 60);
		int hours   = (int) ((millisecond / (1000*60*60)) % 24);
		
		if (hours < 10){
			sb.append("0" + hours + ":");
		} else {
			sb.append(hours + ":");
		}
		
		if (minutes < 10){
			sb.append("0" + minutes);
		} else {
			sb.append(minutes);
		}
		
		return sb.toString();
	}
	
}
