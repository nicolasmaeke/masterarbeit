/**
 * 
 */
package model;

/**
 * @author nicolasmaeke
 *
 */
public class Deadruntime implements Journey{
	
	private final String id;
	private final String fromStopId;
	private final String toStopId;
	private final int distance;
	private double runtime;
	private double energyConsumption;

	public Deadruntime(String fromStopID, String toStopID, int distance, int runtime) {
		this.fromStopId = fromStopID;
		this.toStopId = toStopID;
		this.id = "" + fromStopId + toStopId;
		this.distance = distance; // in Meter
		this.runtime = runtime*1000; // von eingelesenen Sekunden in Millisekunden umwandeln
    	this.energyConsumption = (distance*0.001) * 1.5; // Annahme: 1,5kWh/km
	}

	public String getId() {
		return id;
	}

	public String getFromStopId() {
		return fromStopId;
	}

	public String getToStopId() {
		return toStopId;
	}

	public double getRuntime() {
		return runtime;
	}

	public double getEnergyConsumption() {
		return energyConsumption;
	}

	public double getDistance() {
		return distance;
	}
	
	public String getType() {
		return "Deadruntime";
	}

}
