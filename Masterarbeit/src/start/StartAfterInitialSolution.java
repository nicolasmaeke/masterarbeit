package start;

import heuristic.VariableNeighborhoodSearch;
import parser.ReadInitialSolution;

public class StartAfterInitialSolution {

	public static void main(String[] args) {
		ReadInitialSolution data = new ReadInitialSolution("/Users/nicolasmaeke/Documents/workspace/Masterarbeit/data/full_sample_real_867_SF_207_stoppoints_initialloesung.txt");
		//data.printInitialSolution();
		data.defineDepotCapacity();
		data.setInitialSolutionVariables();
		
		VariableNeighborhoodSearch improvement = new VariableNeighborhoodSearch(data);
		improvement.startVNS(100, 50);
	}

}
