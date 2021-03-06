/**
 * 
 */
package start;

import heuristic.Savings;
import parser.ReadReassessedData;

/**
 * @author nicolasmaeke
 *
 */
public class ExecuteStartHeuristic {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ReadReassessedData data = new ReadReassessedData("/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/data/full_sample_real_867_SF_207_stoppoints.txt");
		data.assignDepot(5);
		data.printDepots();
		
		Savings initialSolution = new Savings(data);
		initialSolution.startSavings();

		initialSolution.writeInitialSolution("/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/initial/full_sample_real_867_SF_207_stoppoints_initialloesung_5Depot.txt");

	}
}
