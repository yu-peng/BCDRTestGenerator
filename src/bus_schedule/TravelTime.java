package bus_schedule;

public class TravelTime implements Comparable {
	
	String routeID;
	String fromStop;
	String toStop;
	int travelTime;
	int benchmarkTime = 0;
	
	int direction;
	long depTime;
	long arrTime;
	
	public TravelTime(String _routeID, String _fromStop, String _toStop, int _direction, long _depTime, long _arrTime, int _travelTime){
		routeID = _routeID;
		fromStop = _fromStop;
		toStop = _toStop;
		travelTime = _travelTime;
		
		direction = _direction;
		depTime = _depTime;
		arrTime = _arrTime;
	}
	
	public void setBenchmarkTime(int _benchmarkTime){
		benchmarkTime = _benchmarkTime;
	}

	@Override
	public int compareTo(Object obj) {
		// TODO Auto-generated method stub
		
		long result = depTime - ((TravelTime) obj).depTime;
		
		return (int) result;
	}

}
