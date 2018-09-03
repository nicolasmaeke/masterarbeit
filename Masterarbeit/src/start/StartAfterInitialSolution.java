package start;

import heuristic.VNSwoShaking;
import heuristic.VariableNeighborhoodSearch;
import parser.ReadInitialSolution;

public class StartAfterInitialSolution {

	public static void main(String[] args) {
		ReadInitialSolution data = new ReadInitialSolution("C:\\Users\\Nicolas\\git\\masterarbeit\\Masterarbeit\\data\\full_sample_real_867_SF_207_stoppoints_initialloesung_5Depot");
		//data.printInitialSolution();
		data.defineDepotCapacity();
		data.setInitialSolutionVariables();
		
		VariableNeighborhoodSearch improvement = new VariableNeighborhoodSearch(data);
		improvement.startVNS(20000, 50, "C:\\Users\\Nicolas\\git\\masterarbeit\\Masterarbeit\\data\\full_sample_real_867_SF_207_stoppoints_solution_5Depot");
		/**
		VNSwoShaking improvement = new VNSwoShaking(data);
		improvement.startVNS(10000, 50);
		*/
	}

}
