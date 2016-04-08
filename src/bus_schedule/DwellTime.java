package bus_schedule;

public class DwellTime implements Comparable {
	
	String routeID;
	String stop;
	int dwellTime;
	int direction;
	
	long depTime;
	long arrTime;
	
	long timeOfDay;
	
	public DwellTime(String _routeID, String _stop, int _direction, long _arrTime, long _depTime, int _dwellTime){
		routeID = _routeID;
		stop = _stop;
		dwellTime = _dwellTime;
		
		direction = _direction;
		depTime = _depTime;
		arrTime = _arrTime;
	}
	
	public void setTimeOfDay(long time){
		timeOfDay = time;
	}

	@Override
	public int compareTo(Object obj) {
		// TODO Auto-generated method stub
		
		long result = arrTime - ((DwellTime) obj).arrTime;
		
		return (int) result;
	}

}
