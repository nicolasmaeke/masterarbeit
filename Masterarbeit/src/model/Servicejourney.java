package model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author nicolasmaeke
 *
 */
public class Servicejourney implements Journey{

	// eingelesene Werte 
	private final String sfId;
	private final String fromStopId;
	private final String toStopId;
	private final String DepTime;
	private final String ArrTime;
	private final int sfDistance;

	// zu berechnende Werte
	private Date sfDepTime;
	private Date sfArrTime;
	private double sfRuntime;
	private double sfEnergyConsumption;

	// Konstante
	public static final int BATTERY_CAPACITY = 80; // kWh

	// Hilfsvariablen zur Berechnung der Runtime
	public DateFormat zformat = new SimpleDateFormat("HH:mm:ss");
	private String help;
	private Date zeit;

	public Servicejourney(String sfId, String sfFromStopID, String sfToStopID, String sfDepTime, String sfArrTime,
			int sfDistance) {
		this.sfId = sfId;
		this.fromStopId = sfFromStopID;
		this.toStopId = sfToStopID;
		this.DepTime = sfDepTime;
		this.ArrTime = sfArrTime;
		this.sfDistance = sfDistance;
		this.setSfRuntime();
		this.sfEnergyConsumption = sfDistance/1000 * 2.0;// Annahme: der Verbrauch einer Leerfahrt ist 2kWh pro Kilometer;
	}

	/**
	 * Methode berechnet Fahrzeit einer Leerfahrt in Milisekunden
	 * und wandelt die Abafahrts- und Ankunftszeiten in das Datumsformat um 
	 */
	public void setSfRuntime(){
		help = DepTime.split(":")[1] + ":" + DepTime.split(":")[2] + ":" + DepTime.split(":")[3];
		zeit = null;

		try {
			zeit = zformat.parse(help);
		} catch (Exception e) {
			System.out.println(e);
		}
		this.sfDepTime = zeit;

		zeit = null;
		help = ArrTime;

		try {
			zeit = zformat.parse(help.split(":")[1] + ":" + help.split(":")[2] + ":" + help.split(":")[3]);
		} catch (Exception e) {
			System.out.println(e);
		}
		this.sfArrTime = zeit;
		if(sfDepTime.getTime() > sfArrTime.getTime()){
			sfArrTime.setTime(sfArrTime.getTime() + 24*60*60*1000);
		}
		this.sfRuntime = ((sfArrTime.getTime() - sfDepTime.getTime()));
		if(sfRuntime < 0){
			System.out.println();
		}
	}

	public String getId() {
		return sfId;
	}

	public String getFromStopId() {
		return fromStopId;
	}

	public String getToStopId() {
		return toStopId;
	}

	public double getSfEnergyConsumption() {
		return sfEnergyConsumption;
	}

	public Date getSfDepTime() {
		return sfDepTime;
	}

	public Date getSfArrTime() {
		return sfArrTime;
	}

	public double getDistance() {
		return sfDistance;
	}

	public double getEnergyConsumption() {
		return sfEnergyConsumption;
	}

	public double getRuntime() {
		return sfRuntime;
	}

	public String getType() {
		return "Servicejourney";
	}
}
