/**
 * 
 */
package start;

import parser.ReassessInitialData;

/**
 * @author nicolasmaeke
 *
 */
public class ReassessFiles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//Lese Daten ein (fuer den Pfad siehe data --> Rechtsklick auf die gewuenschte Datei --> Properties)
		ReassessInitialData test = new ReassessInitialData("/Users/nicolasmaeke/Documents/workspace/Masterarbeit/data/sample_real_867_SF_207_stoppoints.txt", 
				"/Users/nicolasmaeke/Documents/workspace/Masterarbeit/data/full_sample_real_867_SF_207_stoppoints.txt");

		System.out.println(test.avgDistanceDeadrun);
		System.out.println(test.avgRuntimeDeadrun);

		System.out.println(test.listStopPoints.size());
		System.out.println((test.listFromToDeadRun.size()));

	}

}
