package start;

import parser.ReadFinalSolution;

public class CheckSolution {
	
	public static void main(String[] args) {

	ReadFinalSolution data = new ReadFinalSolution("/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/initial/full_sample_real_867_SF_207_stoppoints_initialloesung_5Depot.txt");
	data.setInitialSolutionVariables();
	
	System.out.println(data.getInitialSolution().isFeasible());
	
	}
}
