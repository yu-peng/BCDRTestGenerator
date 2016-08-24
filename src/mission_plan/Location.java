package mission_plan;

import java.util.UUID;

public class Location{

	/**
	 * 
	 */
	public String id;
	public String name;
	public double lat = 999;
	public double lon = 999;
		
	public Location(String _name, double _lat, double _lon){
		
		id = UUID.randomUUID().toString();
		name = _name;
		
		lat = _lat;
		lon = _lon;
	}
	
	public void print(){
		
		System.out.println("NODE NAME: " + name);
		System.out.println("LAT: " + lat);
		System.out.println("LON: " + lon);

	}
}
