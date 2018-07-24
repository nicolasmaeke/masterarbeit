/**
 * 
 */
package model;

/**
 * @author nicolasmaeke
 *
 */
public interface Journey {
	
	String getType();

	double getEnergyConsumption();
	
	double getRuntime();
	
	double getDistance();

	String getId();

	String getToStopId();

	String getFromStopId();

}
