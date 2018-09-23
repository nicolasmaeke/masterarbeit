package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import helper.FeasibilityHelper;

import java.util.Map.Entry;

public class Schedule {

	private ArrayList<Roundtrip> umlaufplan;
	private double totalCosts; 
	private int numberOfLoadingStations; 
	private double variableCosts;
	private double fixCosts;
	private final List<Depot> depots;
	private final HashMap<String, Stoppoint> stoppoints;
	private final HashMap<String, Servicejourney> servicejourneys;
	private final int numberOfServiceJourneys; 
	private ArrayList<Stoppoint> stoppointsWithLoadingStations = new ArrayList<Stoppoint>();
	public static final double VEHICLE_COSTS = 400000; // fix costs for a vehicle in a planning period
	public static final double LOADINGSTATION_COSTS = 250000; // fix costs for a loadingstation in a planning period
	public static final double LOADING_COSTS = 0.1; // electricity price per kWh
	public static final double STAFF_COSTS = 20; // staff costs per vehicle per hour
	
	public Schedule(ArrayList<Roundtrip> umlaufplan, HashMap<String, Servicejourney> servicejourneys, List<Depot> depots, HashMap<String, Stoppoint> stoppoints){
		this.umlaufplan = umlaufplan;
		this.servicejourneys = servicejourneys;
		this.numberOfServiceJourneys = servicejourneys.size();
		this.depots = depots;
		this.stoppoints = stoppoints;
		/** die Depots sollen nicht mitgezaehlt werden
		for (int i = 0; i < depots.size(); i++){
			this.stoppointsWithLoadingStations.add(stoppoints.get(depots.get(i).getId()));
		}
		*/
	}

	public ArrayList<Roundtrip> getUmlaufplan() {
		return umlaufplan;
	}
	public void setUmlaufplan(ArrayList<Roundtrip> umlaufplan) {
		this.umlaufplan = umlaufplan;
	}
	public double getTotalCosts() {
		return totalCosts;
	}
	public void setTotalCosts(double totalCosts) {
		this.totalCosts = totalCosts;
	}
	public int getNumberOfLoadingStations() {
		return numberOfLoadingStations;
	}
	public void setNumberOfLoadingStations(int numberOfLoadingStations) {
		this.numberOfLoadingStations = numberOfLoadingStations;
	}
	public double getVariableCosts() {
		return variableCosts;
	}
	public void setVariableCosts(double variableCosts) {
		this.variableCosts = variableCosts;
	}
	public double getFixCosts() {
		return fixCosts;
	}
	public void setFixCosts(double fixCosts) {
		this.fixCosts = fixCosts;
	}

	public void calculateCosts(){
		int anzahlFahrzeuge = umlaufplan.size();
		double vehicleCosts = anzahlFahrzeuge * VEHICLE_COSTS;
		int anzahlLoadingStations = numberOfLoadingStations; 
		double loadingStationCosts = anzahlLoadingStations * LOADINGSTATION_COSTS;
		double fix = vehicleCosts + loadingStationCosts;
		double variable = 0.0;
		double energycosts = 0.0;
		double staffcosts = 0.0;
		double energyConsumption = 0.0;
		double runtimeInHours = 0.0;
		for (int i = 0; i < umlaufplan.size(); i++) {
			for (int j = 0; j < umlaufplan.get(i).getJourneys().size(); j++) {
				energyConsumption = umlaufplan.get(i).getJourneys().get(j).getEnergyConsumption();
				energycosts = energyConsumption * LOADING_COSTS;
				runtimeInHours = (umlaufplan.get(i).getJourneys().get(j).getRuntime() * 0.001) / 60 / 60;
				staffcosts = runtimeInHours * STAFF_COSTS;
				variable = variable + staffcosts + energycosts;
			}
		}
		setFixCosts(fix);
		setVariableCosts(variable);
		setTotalCosts(variable + fix);
	}
	
	public boolean isFeasible(){
		int count = 0;
		String startDepot = "";
		String EndDepot = "";
		Roundtrip current = null;
		Journey temp = null;
		Journey next = null;
		double runtime = 0.0;
		int size = umlaufplan.size();
		
		for (int i = 0; i < size; i++) {
			double zeitpuffer = 0.0;
			double verbrauch = 0.0;
			temp = null;
			next = null;
			runtime = 0.0;
			current = umlaufplan.get(i);
			if(current.getJourneys() == null){
				System.err.println();
			}
			startDepot = current.getJourneys().getFirst().getFromStopId();
			EndDepot = current.getJourneys().getLast().getToStopId();
			if(!startDepot.equals(EndDepot)){
				//System.err.println("Start- und Enddepot sind nicht gleich");
				return false;
			}
			if (!(current.getJourneys().get(0) instanceof Deadruntime) || !(current.getJourneys().getLast() instanceof Deadruntime)){
				//System.err.println("Beginnt oder endet nicht mit Leerfahrt");
				return false;
			}
			for (int j = 0; j < current.getJourneys().size(); j++) {
				if(j >= 1 && j < current.getJourneys().size()-3){
					if(current.getJourneys().get(j) instanceof Deadruntime && current.getJourneys().get(j+1) instanceof Deadruntime){
						if(current.getJourneys().get(j-1) instanceof Deadruntime || current.getJourneys().get(j+2) instanceof Deadruntime){
							//System.err.println("Drei LF hintereinander");
							return false;
						}
					}
					temp = current.getAtIndex(j);
					verbrauch = verbrauch + temp.getEnergyConsumption();
					if(temp instanceof Servicejourney){
						next = current.getAtIndex(j+2);
						if(next instanceof Servicejourney){
							runtime = current.getAtIndex(j+1).getRuntime();
							if(!(temp.getToStopId().equals(current.getAtIndex(j+1).getFromStopId()) || !(current.getAtIndex(j+1).getToStopId().equals(next.getFromStopId())))){
								//System.err.println("Falsche Leerfahrt");
								return false;
							}
							if((((Servicejourney) temp).getSfArrTime().getTime() + runtime > ((Servicejourney) next).getSfDepTime().getTime())){
								//System.err.println("Zeitlich nicht zulässig");
								return false;
							}
							zeitpuffer = zeitpuffer + ((Servicejourney) next).getSfDepTime().getTime() - ((Servicejourney) temp).getSfArrTime().getTime() + runtime;
						}
						else{
							next = current.getAtIndex(j+3);
							if(next instanceof Servicejourney){
								runtime = current.getAtIndex(j+1).getRuntime() + current.getAtIndex(j+2).getRuntime();
								if(!(temp.getToStopId().equals(current.getAtIndex(j+1).getFromStopId()) || !(current.getAtIndex(j+2).getToStopId().equals(next.getFromStopId())))){
									//System.err.println("Falsche Leerfahrt");
									return false;
								}
								if((((Servicejourney) temp).getSfArrTime().getTime() + runtime > ((Servicejourney) next).getSfDepTime().getTime())){
									//System.err.println("Zeitlich nicht zulässig");
									return false;
								}
								zeitpuffer = zeitpuffer + ((Servicejourney) next).getSfDepTime().getTime() - ((Servicejourney) temp).getSfArrTime().getTime() + runtime;
							}
						}
					}
				}
				if (current.getJourneys().get(j) instanceof Servicejourney){
					count ++;
				}
			}
			if ((verbrauch / ((7.5 / 60) / 1000)) >= zeitpuffer) { // die gesamte Ladezeit ist groesser als der Zeitpuffer
				System.out.println();
			}
		}
		if(count != numberOfServiceJourneys){
			if(count < numberOfServiceJourneys){
				//System.err.println("Es fehlen Servicefahrten");
			}
			else{
				//System.err.println("Es werden Servicefahrten doppelt gefahren");
			}
			return false;
		}
		return true;
	}

	public void printUmlaufplan(){
		String result = "";
		for (int i = 0; i < umlaufplan.size(); i++) {
			result = result + umlaufplan.get(i).printRoundtrip();
		}
		result = result + "Fahrzeuge: " + umlaufplan.size() + "\n" + "Servicefahrten: " + getNumberOfServiceJourneys() + "\n" + "Ladestationen: " + getNumberOfLoadingStations() + "\n" 
		+ "Fixkosten: " + getFixCosts() + "\n" + "Variable Kosten: " + getVariableCosts() + "\n" + "Gesamtkosten: " + getTotalCosts();
		
		System.out.println(result);
	}

	public int getNumberOfServiceJourneys() {
		return numberOfServiceJourneys;
	}

	public HashMap<String, Servicejourney> getServicejourneys() {
		return servicejourneys;
	}

	public ArrayList<Stoppoint> getStoppointsWithLoadingStations() {
		return stoppointsWithLoadingStations;
	}

	public void setStoppointsWithLoadingStations(ArrayList<Stoppoint> stoppointsWithLoadingStations) {
		this.stoppointsWithLoadingStations = stoppointsWithLoadingStations;
	}

	public List<Depot> getDepots() {
		return depots;
	}

	public HashMap<String, Stoppoint> getStoppoints() {
		return stoppoints;
	}
	
	public void printLoadingstations(){
		int counter = 0;
		for (int i = 0; i < stoppointsWithLoadingStations.size(); i++) {
			counter = counter + stoppointsWithLoadingStations.get(i).getFrequency();
			if(stoppointsWithLoadingStations.get(i).getFrequency() == 0){
				stoppointsWithLoadingStations.remove(i);
			}else{
				System.out.println("Loadingstation " + stoppointsWithLoadingStations.get(i).getId() + " has frequency: " + stoppointsWithLoadingStations.get(i).getFrequency());
			}
		}
		System.out.println("Gesamtanzahl Ladevorgaenge: " + counter);
	}

	/**
	public void setFrequencies(HashMap<String, Deadruntime> deadruntimes) {
		ArrayList<Stoppoint> newStoppoints = new ArrayList<>();
		for (int i = 0; i < umlaufplan.size(); i++) {
			for (int j = 0; j < umlaufplan.get(i).getLaden().size(); j++) {
				if (!newStoppoints.contains(umlaufplan.get(i).getLaden().get(j))) {
					newStoppoints.add(umlaufplan.get(i).getLaden().get(j));
				}
				
			}
		}
		// Stoppoints zuruecksetzen
		for(Entry<String, Stoppoint> e: stoppoints.entrySet()){
			if (!e.getValue().isDepot()) {
				e.getValue().setLoadingstation(false);
			}
			e.getValue().setFrequency(0);
		}
	}
	*/

}
