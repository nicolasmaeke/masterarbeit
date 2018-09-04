package start;

import heuristic.VNSwoShaking;
import heuristic.VariableNeighborhoodSearch;
import parser.ReadInitialSolution;

public class StartAfterInitialSolution {

	public static void main(String[] args) {

		ReadInitialSolution data = new ReadInitialSolution("/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/initial/full_sample_real_867_SF_207_stoppoints_initialloesung_1Depot.txt");

		//data.printInitialSolution();
		data.defineDepotCapacity();
		data.setInitialSolutionVariables();
		
		VariableNeighborhoodSearch improvement = new VariableNeighborhoodSearch(data);

		improvement.startVNS(20000, 50, "/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/solution/full_sample_real_867_SF_207_stoppoints_solution_1Depot.txt");

		/**
		VNSwoShaking improvement = new VNSwoShaking(data);
		improvement.startVNS(10000, 50);
		*/
	}

}
