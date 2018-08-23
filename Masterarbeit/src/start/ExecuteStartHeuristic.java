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
		ReadReassessedData data = new ReadReassessedData("/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/full_sample_real_867_SF_207_stoppoints.txt");
		data.assignDepot(3);
		//data.printDepots();
		
		Savings initialSolution = new Savings(data);
		initialSolution.startSavings();
		initialSolution.writeInitialSolution("/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/full_sample_real_867_SF_207_stoppoints_initialloesung.txt");
	}
}
