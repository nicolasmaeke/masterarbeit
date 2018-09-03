package start;

import heuristic.VNSwoShaking;
import heuristic.VariableNeighborhoodSearch;
import parser.ReadInitialSolution;

public class StartAfterInitialSolution {

	public static void main(String[] args) {
		ReadInitialSolution data = new ReadInitialSolution("/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/full_sample_real_1296_SF_88_stoppoints_initialloesung.txt");
		//data.printInitialSolution();
		data.defineDepotCapacity();
		data.setInitialSolutionVariables();
		
		VariableNeighborhoodSearch improvement = new VariableNeighborhoodSearch(data);
		improvement.startVNS(20000, 50, "/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/full_sample_real_1296_SF_88_stoppoints_solution.txt");
		/**
		VNSwoShaking improvement = new VNSwoShaking(data);
		improvement.startVNS(10000, 50);
		*/
	}

}
