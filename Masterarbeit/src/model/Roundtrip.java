/**
 * 
 */
package model;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author nicolasmaeke
 * Klasse repraesentiert einen Fahrzeugumlauf
 *
 */
public class Roundtrip {

	private final int id;
	private LinkedList<Journey> journeys;
	private double operativeCosts;
	private ArrayList<Stoppoint> laden; // Ladestationen, die der Umlauf nutzt
	private ArrayList<Stoppoint> stellen; // An welcher Stelle im Umlauf wird die Ladestation genutzt (Stelle i bedeutet: zwischen Fahrt (i-1) und Fahrt i) 
	private static final double CAPACITY = 80.0; // Batteriekapazitaet in kWh

	public Roundtrip(int id){
		this.id = id;
		this.journeys = new LinkedList<Journey>();
		this.laden = new ArrayList<Stoppoint>();
		this.stellen = new ArrayList<Stoppoint>();
	}

	public double getOperativeCosts() {
		return operativeCosts;
	}

	public void setOperativeCosts(double operativeCosts) {
		this.operativeCosts = operativeCosts;
	}

	public int getId() {
		return id;
	}

	public void addFahrtAfterFahrt(int i, Journey j) {
		journeys.add(i, j);
	}

	public LinkedList<Journey> getJourneys() {
		return journeys;
	}

	public void setJourneys(LinkedList<Journey> journeys) {
		this.journeys = journeys;
	}

	public String printRoundtrip(){
		String result = "";
		double energyConsumption = 0.0;
		for (int j = 0; j < journeys.size(); j++) {
			result = result + journeys.get(j).getType() + " " + journeys.get(j).getId() + " - ";
			energyConsumption = energyConsumption + journeys.get(j).getEnergyConsumption();
		}
		result = result + "Der Fahrzeugumlauf benötigt " + energyConsumption + " kWh Energie. \n";
		return result;
	}

	public Journey getAtIndex(int index){
		Journey result = journeys.get(index);
		return result;
	}

	public ArrayList<Stoppoint> getLaden() {
		return laden;
	}

	public ArrayList<Stoppoint> getStellen() {
		return stellen;
	}

	public void setStellen(ArrayList<Stoppoint> stellen){
		this.stellen = stellen;
	}

	public void setLaden(ArrayList<Stoppoint> laden){
		this.laden = laden;
	}

	public static double getCapacity() {
		return CAPACITY;
	}

	public String toStringIds() {
		String result = "[";
		for (int i = 0; i < journeys.size(); i++) {
			result = result + journeys.get(i).getId() + ", ";
		}
		result = result.substring(0, result.length()-2);
		result = result + "]";
		return result;
	}

	public String getLadenString() {
		String result = "[";
		for (int i = 0; i < laden.size(); i++) {
			result = result + laden.get(i).getId() + ", ";
		}
		if(result.length() > 1){
			result = result.substring(0, result.length()-2);
		}
		result = result + "]";
		return result;
	}

	/**
	 * 
	 * @param index1
	 * @param index2
	 * @return list of journeys inclusive index1 and index2
	 */
	public LinkedList<Journey> getFahrtenVonBis(int index1, int index2){
		LinkedList<Journey> fahrten = new LinkedList<Journey>();
		for (int i = index1; i <= index2; i++) {
			fahrten.add(journeys.get(i));
		}
		return fahrten;
	}

	public boolean isFeasible(){

		String startDepot = "";
		String EndDepot = "";
		Journey temp = null;
		Journey next = null;
		double runtime = 0.0;

		temp = null;
		next = null;
		runtime = 0.0;

		startDepot = journeys.getFirst().getFromStopId();
		EndDepot = journeys.getLast().getToStopId();
		if(!startDepot.equals(EndDepot)){
			//System.err.println("Start- und Enddepot sind nicht gleich");
			return false;
		}
		if (!(journeys.get(0) instanceof Deadruntime) || !(journeys.getLast() instanceof Deadruntime)){
			//System.err.println("Beginnt oder endet nicht mit Leerfahrt");
			return false;
		}
		for (int j = 0; j < journeys.size(); j++) {
			if(j >= 1 && j < journeys.size()-3){
				if(journeys.get(j) instanceof Deadruntime && journeys.get(j+1) instanceof Deadruntime){
					if(journeys.get(j-1) instanceof Deadruntime || journeys.get(j+2) instanceof Deadruntime){
						//System.err.println("Drei LF hintereinander");
						return false;
					}
				}
				temp = this.getAtIndex(j);
				if(temp instanceof Servicejourney){
					next = this.getAtIndex(j+2);
					if(next instanceof Servicejourney){
						runtime = this.getAtIndex(j+1).getRuntime();
						if((((Servicejourney) temp).getSfArrTime().getTime() + runtime > ((Servicejourney) next).getSfDepTime().getTime())){
							//System.err.println("Zeitlich nicht zulässig");
							return false;
						}
					}
					else{
						next = this.getAtIndex(j+3);
						if(next instanceof Servicejourney){
							runtime = this.getAtIndex(j+1).getRuntime() + this.getAtIndex(j+2).getRuntime();
							if((((Servicejourney) temp).getSfArrTime().getTime() + runtime > ((Servicejourney) next).getSfDepTime().getTime())){
								//System.err.println("Zeitlich nicht zulässig");
								return false;
							}
						}
					}
				}
			}

		}
		return true;
	}

	public double getKosten() {
		
		double verbrauchsKosten = 0.0;
		double personalkosten = 0.0;
		
		for (int i = 0; i < journeys.size(); i++) {
			verbrauchsKosten = verbrauchsKosten + journeys.get(i).getEnergyConsumption();
			personalkosten = personalkosten + journeys.get(i).getRuntime();
		}
		return (verbrauchsKosten * 0.1) + ((personalkosten * 0.001) / 60 / 60 * 20);
	}
	
	public double getKostenMitLadestationen() {
		
		double verbrauchsKosten = 0.0;
		double ladestationsAnteil = 0.0;

		for (int i = 0; i < this.getLaden().size(); i++) {

			if(!this.getLaden().contains(null)){
				int test = this.getLaden().get(i).getFrequency();
				if(test == 0){
					this.getLaden().remove(i);
					//System.err.println("Frequenz = 0 an Ladestation " + this.getLaden().get(i).getId());
				}
				double divisor = 1.0 / test;
				ladestationsAnteil = ladestationsAnteil + 250000 * (divisor);// Kosten fuer Ladestationen werden anteilig auf die nutzenden Fahrzeugumlaeufe verteilt
			}	 
		}
		double personalkosten = 0.0;
		for (int i = 0; i < journeys.size(); i++) {
			verbrauchsKosten = verbrauchsKosten + journeys.get(i).getEnergyConsumption();
			personalkosten = personalkosten + journeys.get(i).getRuntime();
		}
		personalkosten = (personalkosten * 0.001) / 60 / 60 * 20;
		verbrauchsKosten = verbrauchsKosten * 0.1;
		return verbrauchsKosten + personalkosten + ladestationsAnteil + 400000;
	}

	public int getIndexOfJourney(Journey j){
		int result = 0;
		for (int i = 0; i < journeys.size(); i++) {
			if (journeys.get(i).getId().equals(j.getId())) {
				break;
			}
			result ++;
		}
		return result;
	}
	
	public String getDepot(){
		if(!(journeys.getFirst().getFromStopId().equals(journeys.getLast().getToStopId()))){
			System.err.println("Start- und Enddepot sind nicht gleich!");
		}
		return journeys.getFirst().getFromStopId();
	}
	
	public int getNumberOfServicejourneys(){
		int result = 0;
		for (int i = 0; i < journeys.size(); i++) {
			if(journeys.get(i) instanceof Servicejourney){
				result ++;
			}
		}
		return result;
	}
}
