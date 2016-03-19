package bus_schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
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

public class BusTestGenerator {
	
	public static HashSet<String> serviceIds = new HashSet<String>();
	public static HashMap<String,String> tripRouteMap = new HashMap<String,String>();
	public static HashMap<String,String> tripServiceIdMap = new HashMap<String,String>();
	public static HashMap<String,String> tripHeadSignMap = new HashMap<String,String>();
	public static HashMap<String,String> tripDirectionMap = new HashMap<String,String>();
	public static HashMap<String,String> stopNameMap = new HashMap<String,String>();
	public static HashMap<String,PTTrip> ptTrips = new HashMap<String,PTTrip>();
	public static HashSet<String> route_1_trips = new HashSet<String>();

	public static String date = "2015-03-23";
	public static int day = 20150323;
	public static int day_of_week = 2; // Sunday is 1
	
	public static double minDwell = 0.5;
	public static double maxDwell = 0.5;
	public static double traversalUncertainty = 1;
	
	public static double defaultScheduleTolerance = 0.1;
	public static double defaultHeadwayTolerance = 0.5;
	public static double scheduleCost = 10;
	public static double headwayCost = 100;

	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
		// Generate a CCTP problem from the schedule of MBTA route 1
		
		int max_stops = 28;
		int max_trips = 5; 
		
		String routeDataPrefix = "data/MBTA_GTFS/";
		
		System.out.println("MaxStops: "+max_stops+"\tMaxTrips: "+max_trips+"\tDate: "+"2015-03-23"+"\tDayofWeek: "+day_of_week);
		System.out.println("DwellTime ["+minDwell+","+maxDwell+"] min\tMoveUncertainty +"+traversalUncertainty+"min");
		System.out.println("ScheduleTolerance "+defaultScheduleTolerance + "\tScheduleCost "+scheduleCost + "\tHeadwayTolerance "+defaultHeadwayTolerance + "\tHEadwayTolerance "+headwayCost);
		System.out.println("Name\tStops #\tBuses #\tEpisode #");
		
		
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
		route_1_trips = new HashSet<String>();	
		
		parseTrip(routeDataPrefix+"trips.txt",routeDataPrefix+"calendar.txt", routeDataPrefix+"calendar_dates.txt",
				day_of_week,day);
		parseStopTime(routeDataPrefix+"stop_times.txt.small");
		parseStop(routeDataPrefix+"stops.txt");
		
		int count = 0;
		for (int trips = 1; trips <= max_trips; trips++){
			for (int stops = 2; stops <= max_stops; stops++){
				count++;
				CCTP newCCTP = generateCCTP(stops,trips,routeDataPrefix);
				
				//System.out.println("Saved to " + "tests/MBTA/Route1_"+stops+"_"+trips+".cctp");
				IO_CCTP.saveCCTP(newCCTP, "tests/MBTA/Route1_"+stops+"_"+trips+".cctp");
//				IO_CCTP.saveCCTPasTPN(newCCTP, "tests/MBTA/Route1_"+stops+"_"+trips+".tpn");
        		System.out.println("Route1_"+stops+"_"+trips+"\t"+stops+"\t"+trips+"\t"+newCCTP.episodes.size());
			}
		}
	}

	
	public static CCTP generateCCTP(int max_stops, int max_trips, String routeDataPrefix) throws IOException, ParseException{
		
		ArrayList<PTTrip> route_1 = new ArrayList<PTTrip>();
		
		// Add route 1 trips to the list and sort based on their departure time
		for (String trip_id : route_1_trips){
			
			PTTrip trip = ptTrips.get(trip_id);
			
			if (route_1.size() == 0){
				route_1.add(trip);
			} else {
				for (int idx = 0; idx < route_1.size(); idx++){		
					if (trip.arrival_times.get(1) < route_1.get(idx).arrival_times.get(1)){
						route_1.add(idx, trip);
						break;
					}					
				}
			}			
		}		
		
//		String trip_id = "26160302";
//		
//		PTTrip trip = ptTrips.get(trip_id);
		
		CCTP newCCTP = new CCTP("Bus Test Route 1");
		Event start = new Event("Start");
		Event end = new Event("End");
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");	
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date d = sdf.parse(date);						
		long reference_day_time = d.getTime();
		start.setExecuted(reference_day_time);
		
		HashMap<Integer,Event> stopEventMap = new HashMap<Integer,Event>();
		double headway = 0;
		
		// go through the list of trips
		for (int idx = 0; idx < route_1.size(); idx++){	
			
			if (idx >= max_trips){
				break;
			}
			
			PTTrip trip = route_1.get(idx);
			String trip_id = trip.tripID;
			
			// get headway
			if (idx > 0){
				PTTrip prev_trip = route_1.get(idx-1);
				headway = (trip.arrival_times.get(1) - prev_trip.arrival_times.get(1))/60000.0;
			}
			
//			System.out.println("Using trip: " + trip_id + ":" + trip.arrival_times.get(1) + "/" + trip.departure_times.get(1));
			Collections.sort(trip.stop_sequences);
			
			Event departure_origin = new Event("Wait for Departure Origin");
			Episode wait_origin = createEpisode(new Episode("Wait for Departure Origin",0,0.01
					,false,true,start,departure_origin,"Controllable;Activity"),newCCTP);

			Event current_event = departure_origin;
			
			for (Integer stop_seq : trip.stop_sequences){
				
//				System.out.println("Stop seq: " + stop_seq);
				
				if (stop_seq > max_stops){
					break;
				}
				
				String stop_id = trip.stop_ids.get(stop_seq);
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
					
					headway_constraint.setLBRelaxRatio(headwayCost);
					headway_constraint.setUBRelaxRatio(headwayCost);
					
				}
				
				stopEventMap.put(stop_seq, current_event);
				
				// First, passenger on/off
				Event dwellComplete = new Event("Dwell Complete at " + stop_id + " ("+stopNameMap.get(stop_id)+")");
				Episode dwell = createEpisode(new Episode("Dwell at " + stop_id + " ("+stopNameMap.get(stop_id)+")",minDwell,maxDwell
						,false,false,current_event,dwellComplete,"Uncontrollable;Activity"),newCCTP);
				
				if (trip.stop_ids.containsKey(stop_seq+1)){
					
					String next_stop_id = trip.stop_ids.get(stop_seq+1);
					long next_arrival_time = trip.arrival_times.get(stop_seq+1);
					double duration = (next_arrival_time-departure_time)/60000.0;
					
					
					// Second, wait for departure
					Event departure = new Event("Leave stop " + stop_id);
					Episode wait = createEpisode(new Episode(trip_id + " Wait for departure at " + stop_id,0,0.01
							,false,true,dwellComplete,departure,"Controllable;Constraint"),newCCTP);
					wait.setUBRelaxRatio(0.1);
					
					// Third, traversal to the next stop
					Event arrival = new Event("Arrive at Stop " + stop_id+ " ("+stopNameMap.get(next_stop_id)+")");
					Episode traversal = createEpisode(new Episode("Going to " + next_stop_id + " ("+stopNameMap.get(next_stop_id)+")",duration,duration+traversalUncertainty
							,false,false,departure,arrival,"Uncontrollable;Activity"),newCCTP);
					current_event = arrival;

				} else {
					
					// End of route
					current_event = dwellComplete;

				}
				
//				System.out.println(stop_seq + ":" + trip.stop_ids.get(stop_seq) + 
//						" [" + trip.arrival_times.get(stop_seq) + "," + trip.departure_times.get(stop_seq) + "]");		
				
			}
			
			Episode trip_completion = createEpisode(new Episode("Trip Complete",0,Double.POSITIVE_INFINITY
					,false,false,current_event,end,"Controllable;Constraint"),newCCTP);
			
		}
		
		newCCTP.setStartEvent(start);
		newCCTP.setEndEvent(end);
		
		return newCCTP;
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
					
					if (elements[1].equals("1") && elements[10].substring(1,2).equals("1")){
						route_1_trips.add(tripID);
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
			
			if (tripRouteMap.containsKey(tripID)){
				
				String stop_id = elements[7];
				int stop_sequence = Integer.parseInt(elements[8].split(",")[1]);
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
			
//			if (route_1_trips.contains(tripID)){
//				writer.println(line);
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
