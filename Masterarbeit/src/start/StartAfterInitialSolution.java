package start;

import heuristic.VariableNeighborhoodSearch;
import parser.ReadInitialSolution;

public class StartAfterInitialSolution {

	public static void main(String[] args) {

		ReadInitialSolution data = new ReadInitialSolution("/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/initial/full_sample_real_867_SF_207_stoppoints_initialloesung_3Depot.txt");

		//data.printInitialSolution();
		data.defineDepotCapacity();
		data.setInitialSolutionVariables();
		
		VariableNeighborhoodSearch improvement = new VariableNeighborhoodSearch(data);

		improvement.startVNS(30000, 50, "/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/solution/full_sample_real_867_SF_207_stoppoints_solution_3Depot.txt");

		/**
		VNSwoShaking improvement = new VNSwoShaking(data);
		improvement.startVNS(10000, 50);
		*/
	}

}
