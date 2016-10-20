package utils;

import cctp.CCTP;
import cctp.Episode;
import io.IO_CCTP;

public class PrecisionReduction {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String path = "tests/MBTA/Headway/Route_Red_Headway_6_Stop_8.cctp";
		System.out.println(path);
		CCTP newCCTP = IO_CCTP.loadCCTPFromFile(path);

		for (Episode episode : newCCTP.episodes.values()){
			episode.setLB(Math.round(episode.lb*100)/100.0);
			episode.setUB(Math.round(episode.ub*100)/100.0);
		}
		
		IO_CCTP.saveCCTPasTPN(newCCTP, "tests/MBTA/Headway/Route_Red_Headway_6_Stop_8.cctp.tpn");
	}

}
