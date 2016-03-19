package bus_schedule;

import java.util.ArrayList;
import java.util.HashMap;

public class PTTrip {
	
	String tripID;
	String shapeID;
	ArrayList<Integer> stop_sequences = new ArrayList<Integer>();
	HashMap<Integer,String> stop_ids = new HashMap<Integer,String>();
	HashMap<Integer,Long> arrival_times = new HashMap<Integer,Long>();
	HashMap<Integer,Long> departure_times = new HashMap<Integer,Long>();
	
	public PTTrip(String _tripID,String _shapeID){
		tripID = _tripID;
		shapeID = _shapeID;
	}
	
	public void add_stop(int stop_sequence, String stop_id, long arrival_time, long departure_time){
		stop_sequences.add(stop_sequence);
		stop_ids.put(stop_sequence, stop_id);
		arrival_times.put(stop_sequence, arrival_time);
		departure_times.put(stop_sequence, departure_time);
	}

}
