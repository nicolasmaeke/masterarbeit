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
		ReadReassessedData data = new ReadReassessedData("C:\\Users\\Nicolas\\git\\masterarbeit\\Masterarbeit\\data\\full_sample_real_2633_SF_67_stoppoints.txt");
		data.assignDepot(3);
		//data.printDepots();
		
		Savings initialSolution = new Savings(data);
		initialSolution.startSavings();
		initialSolution.writeInitialSolution("C:\\Users\\Nicolas\\git\\masterarbeit\\Masterarbeit\\data\\full_sample_real_2633_SF_67_stoppoints.txt_initialloesung");
	}
}
