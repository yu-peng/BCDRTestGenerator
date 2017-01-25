# Test Generator
Temporal problem generator for evaluating the BCDR/CDRU implementations. Currently it supports the creation of three types of problems:

1. AUV: multi-vehicle scheduling problems from autonomous underwater vehicle mission scenarios. It can be configured to support ser-bounded and probabilistic durations, chance constraints, and relaxable constraints.

2. PSP: scheduling problems from partial ordered activities for Resource-Constrained Project Scheduling Problems. Currently it only supports dynamic controllability model. The relaxable version of this problem uses the maximum flexibility objective.
 
3. Vehicle dispatch scheduling problems generated from Boston's Red Line subway schedule. It can be solved with either string or dynamic controllability model, and the relaxable version uses the minimal cost objective.

The datasets used by this generator are:

1. MBTA GTFS data: http://www.mbta.com/rider_tools/developers/default.asp?id=21895. Unzip the GTFS files and place it under the data/MBTA_GTFS directory.

2. Partially-Scheduled RCPSPs (included in this repo): kindly provided by Patrik Haslum (http://users.cecs.anu.edu.au/~patrik/).


## Quickstart

1. AUV: open class `TestGeneratorCCTP` in package `mission_plan`. The scale of the generated problems are controlled by the following parameters:

	* `Nvehicles`: number of parallel vehicle operations. Larger value means more parallel threads, which makes the problem look `wide`.
	
	* `Nmissions`: number of missions per vehicle. Larger value means more sequential constraints, which makes the problem look `long`.
	
	* `OutputFolder` and `OutputFileHeader`: specifies the output problem location and file prefixes.

	* Leave both `optionLimitL` and `optionLimitL` to be 1 if no choices are allowed in the output problems.
	
	* To generate *.cctp files, you may use the `IO_CCTP.saveCCTP` function.

    * To generate **.tpn filess, you may use the `IO_CCTP.saveCCTPasTPN` function.

2. PSP: open class `TestGeneratorMaxFlexibility` in package `rcpsp`. It will convert the RCPSP files under the data/RCPSP_data/J10 directory to scheduling problems in either CCTP or TPN format.

    * By default the output problem contains relaxable durations. To disable them, locate line 91 and change both boolean parameters in function `Episode newActivity = createEpisode(...)` to be false.

    * Similarly, to generate *.cctp files, you may use the `IO_CCTP.saveCCTP` function. To generate **.tpn filess, you may use the `IO_CCTP.saveCCTPasTPN` function.
 
3. Transit Vehicle Dispatch: open class `TestGenerator` in package `bus_schedule`. It extracts route schedule from MBTA's GTFS data and generate scheduling problems for maintaining adequate headways. The following are the key parameters for this generator.

    * `date`, `day` and `day_of_week`: they define the schedule of the date from which the problems are created. Please make sure that the String `data` is consistent with the integer `day`, and `day_of_week`. Also when selecting the date, make sure that it is within the range of dates covered by th GTFS you downloaded from MBTA's website.
    
    * `route_id` and `direction`: they define the route and direction from which the scheduling problems are generated from. You may find all route_id values in the `routes.txt` file. In most scenarios, values for `direction` is either 0 or 1. You may looks up the meaning for a specific route in `trips.txt` file.
    
    * `stop_ids`: a list of unique identifier for transit stops. Only stops in this list will be included in the generated problems.
    
    * `max_trips` and `max_stops`: these parameters control the scale of the generated problems. The larger their values are, the larger the output problems. `max_trips` specifies how many vehicles along the route should be included in the problem. It makes the output problems look wider. While `max_stops` specifies how many stops should be included for a route, and makes the output problems look longer. For example, for the RedLine operation during a weekday, the `max_trips` can be as large as 150 (total number of trains from morning to evening), while the `max_stops` is capped at 22. Note that by default `max_trips * max_stops` problems will be generated to cover all possible size variations under the limit.
    
    * `outFoldername` and `outFilename`: they specify where the output problems should be saved.
    
    * Similar to the other generators, to create *.cctp files, you may use the `IO_CCTP.saveCCTP` function. To create **.tpn filess, you may use the `IO_CCTP.saveCCTPasTPN` function.

