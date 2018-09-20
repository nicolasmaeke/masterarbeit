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
		ReassessInitialData test = new ReassessInitialData("/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/data/sample_real_3067_SF_209_stoppoints.txt", 
				"/Users/nicolasmaeke/gitproject/masterarbeit/Masterarbeit/data/data/full_sample_real_3067_SF_209_stoppoints.txt");

		System.out.println(test.avgDistanceDeadrun);
		System.out.println(test.avgRuntimeDeadrun);
		System.out.println(test.avgDistanceServicejourney);

		System.out.println(test.listStopPoints.size());
		System.out.println((test.listFromToDeadRun.size()));

	}

}
