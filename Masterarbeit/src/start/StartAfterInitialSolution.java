package start;

import heuristic.VNSwoShaking;
import heuristic.VariableNeighborhoodSearch;
import parser.ReadInitialSolution;

public class StartAfterInitialSolution {

	public static void main(String[] args) {
<<<<<<< HEAD
		ReadInitialSolution data = new ReadInitialSolution("/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/full_sample_real_1296_SF_88_stoppoints_initialloesung.txt");
=======
		ReadInitialSolution data = new ReadInitialSolution("C:\\Users\\Nicolas\\git\\masterarbeit\\Masterarbeit\\data\\full_sample_real_867_SF_207_stoppoints_initialloesung_5Depot");
>>>>>>> branch 'master' of https://github.com/nicolasmaeke/masterarbeit.git
		//data.printInitialSolution();
		data.defineDepotCapacity();
		data.setInitialSolutionVariables();
		
		VariableNeighborhoodSearch improvement = new VariableNeighborhoodSearch(data);
<<<<<<< HEAD
		improvement.startVNS(20000, 50, "/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/full_sample_real_1296_SF_88_stoppoints_solution.txt");
=======
		improvement.startVNS(20000, 50, "C:\\Users\\Nicolas\\git\\masterarbeit\\Masterarbeit\\data\\full_sample_real_867_SF_207_stoppoints_solution_5Depot");
>>>>>>> branch 'master' of https://github.com/nicolasmaeke/masterarbeit.git
		/**
		VNSwoShaking improvement = new VNSwoShaking(data);
		improvement.startVNS(10000, 50);
		*/
	}

}
