package bus_schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class DataFilter {

	public static void main(String[] args) {
		// Trying to detect abnormal data points from the MBTA performance data
		String performanceDataPrefix = "data/Redline_performance/";
		checkHistoricalTravelTimes(performanceDataPrefix + "travel_times/");
	}
	
	public static void checkHistoricalTravelTimes(String travelDataPrefix){
		
		int badData = 0;
		int goodData = 0;
		
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
				    		
//				    		TravelTime time = new TravelTime(route,fromStopID,toStopID,travelDirection,departureTime,arrivalTime,travelTime);
//				    		time.setBenchmarkTime(benchmarkTime);
				    		
//				    		if (travelTime < 0.5 * benchmarkTime){
				    		if (fromStopID.equals("70081") && toStopID.equals("70083")){
				    			System.out.println(line);				    			
//				    			System.out.println(travelTime + ";");

				    			badData++;
				    		} else {
				    			goodData++;
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
		
		System.out.println("Good Data: " + goodData);
		System.out.println("Bad Data: " + badData);
		
	}

}
