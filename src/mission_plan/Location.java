package mission_plan;

/*

Copyright (c) 2013 Peng Yu, MIT.

This software may not be redistributed, and can only be retained
and used with the explicit written consent of the author, Peng Yu.

This software is made available as is; no guarantee is provided
with respect to performance or correct behavior.

This software may only be used for non-commercial, non-profit,
research activities.

*/

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
