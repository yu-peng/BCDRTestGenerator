package bus_schedule;

import cctp.CCTP;
import io.IO_CCTP;

public class ProblemEvaluation {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		CCTP problem = IO_CCTP.loadCCTPFromFile("tests/MBTA/Headway/"+"Route_Red_Headway_"+9 + "_Stop_"+9+".cctp");
		CCTP problem = IO_CCTP.loadCCTPFromFile("tests/AUV/AUV-2400.cctp");
		
		System.out.println("Events: " + problem.getEvents().size());
		System.out.println("Constraints: " + problem.getEpisodes().size());

	}

}
