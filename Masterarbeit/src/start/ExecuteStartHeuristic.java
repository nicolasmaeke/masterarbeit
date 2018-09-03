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
		ReadReassessedData data = new ReadReassessedData("/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/full_sample_real_1135_SF_101_stoppoints.txt");
		data.assignDepot(1);
		data.printDepots();
		
		Savings initialSolution = new Savings(data);
		initialSolution.startSavings();
		initialSolution.writeInitialSolution("/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/full_sample_real_1135_SF_101_stoppoints_initialloesung_1Depot.txt");
	}
}
