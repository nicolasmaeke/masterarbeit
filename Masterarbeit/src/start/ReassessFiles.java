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
		ReassessInitialData test = new ReassessInitialData("C:\\Users\\Nicolas\\git\\masterarbeit\\Masterarbeit\\data\\full_sample_real_2633_SF_67_stoppoints.txt", 
				"C:\\Users\\Nicolas\\git\\masterarbeit\\Masterarbeit\\data\\full_sample_real_2633_SF_67_stoppoints.txt");

		System.out.println(test.avgDistanceDeadrun);
		System.out.println(test.avgRuntimeDeadrun);

		System.out.println(test.listStopPoints.size());
		System.out.println((test.listFromToDeadRun.size()));

	}

}
