/**
 * 
 */
package helper;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import model.Deadruntime;
import model.Depot;
import model.Journey;
import model.Roundtrip;
import model.Servicejourney;
import model.Stoppoint;

/**
 * @author nicolasmaeke
 *
 */
public class FeasibilityHelper {

	/**
	 * 
	 * @param i  Servicejourney i
	 * @param j  Servicejourney j
	 * @return can Servicejourney j be served after Servicejourney i by the same vehicle?
	 */
	public static boolean compatibility(Servicejourney i, Servicejourney j, HashMap<String, Deadruntime> deadruntimes){
		boolean result = true;
		Deadruntime deadrun = deadruntimes.get("" + i.getToStopId() + j.getFromStopId()); // finde die Leerfahrt zwischen i und j 
		if(!(i.getSfArrTime().getTime() + deadrun.getRuntime() <= j.getSfDepTime().getTime())){
			result = false;
		}
		return result;

	}

	/**
	 * 
	 * @param i  Servicejourney i
	 * @param j  Servicejourney j
	 * @param h  Stoppoint h
	 * @return can Servicejourney j be served after Servicejourney i even if the battery is recharged between the two trips at Stoppoint h?
	 */
	public static boolean compatibilityWithLoading(Servicejourney i, Servicejourney j, Stoppoint h, double batteryLevel, HashMap<String, Deadruntime> deadruntimes){
		boolean result = false;
		double chargingTime =  (Servicejourney.BATTERY_CAPACITY - batteryLevel) / ((7.5 / 60) / 1000); //in Milisekunden
		Deadruntime deadrunToLoad = deadruntimes.get("" + i.getToStopId() + h.getId()); // finde die Leerfahrt zwischen SF i und der Ladestation 
		Deadruntime deadrunFromLoad = deadruntimes.get("" + h.getId() + j.getFromStopId()); // finde die Leerfahrt zwischen der Ladestation und SF j 
		if (i.getSfArrTime().getTime() + deadrunToLoad.getRuntime() + chargingTime + deadrunFromLoad.getRuntime() <= j.getSfDepTime().getTime()) {
			result = true;
		}
		return result;

	}

	/**
	 * Methode prueft, ob zwei Servicefahrten nacheinander in einem Fahrzeugumlauf
	 * erledigt werden können, ohne dass Verspaetungen entstehen 
	 * @param i
	 * @param j
	 * @param deadruntimes
	 * @param servicejourney
	 * @return
	 */
	public static double zeitpufferZwischenServicefahrten(Servicejourney eins, Servicejourney zwei, Deadruntime deadrun){
		return (zwei.getSfDepTime().getTime() - eins.getSfArrTime().getTime()) - deadrun.getRuntime();
	}

	/**
	 * Methode prueft, ob genug Zeit zum Laden zwischen zwei SF vorhanden ist, wenn eine Ladestation gebaut wird
	 * 
	 * @return
	 */
	public static boolean zeitpufferFuerLadezeit(String i, String j, HashMap<String, Deadruntime> deadruntimes, HashMap<String, Servicejourney> servicejourneys, double restkapazitaet){
		double result = 0;
		double ladezeit = (Roundtrip.getCapacity() - restkapazitaet) / ((7.5 / 60) / 1000); //in Milisekunden
		Servicejourney eins = servicejourneys.get(i);
		Servicejourney zwei = servicejourneys.get(j);
		Deadruntime deadrun = deadruntimes.get(""+eins.getToStopId()+zwei.getFromStopId());
		result = (zwei.getSfDepTime().getTime() - eins.getSfArrTime().getTime()) - deadrun.getRuntime() - ladezeit;
		return result >= 0;
	}

	public static boolean zeitpufferFuerUmweg(Deadruntime detourOne, Deadruntime detourTwo, Servicejourney one, Servicejourney two, double restkapazitaet){
		double result = 0;
		double ladezeit = (Roundtrip.getCapacity() - restkapazitaet) / ((7.5 / 60) / 1000); //in Milisekunden
		result = (two.getSfDepTime().getTime() - one.getSfArrTime().getTime()) - detourOne.getRuntime() - detourTwo.getRuntime() - ladezeit;
		return result >= 0;
	}


	public static boolean isScheduleFeasible(Vector<Roundtrip> roundtrips) {
		boolean result = true;
		ArrayList<String> ids = new ArrayList<String>();
		for (int i = 0; i < roundtrips.size(); i++) {
			for (int j = 0; j < roundtrips.get(i).getJourneys().size(); j++) {
				if (roundtrips.get(i).getJourneys().get(j) instanceof Servicejourney) {
					if(!ids.contains(roundtrips.get(i).getJourneys().get(j).getId())){
						ids.add(roundtrips.get(i).getJourneys().get(j).getId());
					}
					else{
						result = false;
						break;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param s Servicefahrt, zu der das naechstgelegene Depot gesucht wird
	 * @return das naechstgelegene Depot zur Servicefahrt s
	 */
	public static Depot findNearestDepot(Servicejourney s, HashMap<String, Deadruntime> deadruntimes, List<Depot> depots){
		Depot currentDepot = depots.get(0);  // das erste Depot in der Liste wird initial ausgewaehlt
		double distanceBest = deadruntimes.get("" + currentDepot.getId() + s.getFromStopId()).getDistance() + deadruntimes.get("" + s.getToStopId() + currentDepot.getId()).getDistance();
		double distanceTemp = 0;
		// ueberpruefe, ob ein anderes Depot einen kuerzeren Weg zu firstSf hat
		for (int j = 1; j < depots.size(); j++) {
			distanceTemp = deadruntimes.get("" + depots.get(j).getId() + s.getFromStopId()).getDistance() + deadruntimes.get("" + s.getToStopId() + depots.get(j).getId()).getDistance();
			if(distanceTemp < distanceBest){
				currentDepot = depots.get(j);
			}
		}
		return currentDepot;
	}
	
	public static String searchBestDepot(Roundtrip trip, HashMap<String, Deadruntime> deadruntimes){
		String startDepot = trip.getJourneys().getFirst().getFromStopId();
		String endDepot = trip.getJourneys().getLast().getToStopId();
		if(trip.getJourneys().size() <= 3){
			double distanceDepotOne = trip.getJourneys().getFirst().getDistance();
			double distanceDepotTwo = trip.getJourneys().getLast().getDistance();
			if(distanceDepotOne <= distanceDepotTwo){
				return startDepot;
			}
			else{
				return endDepot;
			}
		}
		String sfFirstFromStop = trip.getJourneys().get(1).getFromStopId();
		String sfLastToStop = trip.getJourneys().get(trip.getJourneys().size()-2).getToStopId();
		if(trip.getJourneys().get(1) instanceof Deadruntime){ // falls zu Beginn zwei LF sind
			sfFirstFromStop = trip.getJourneys().get(2).getFromStopId();
		}
		if(trip.getJourneys().get(trip.getJourneys().size()-2) instanceof Deadruntime){ // falls am Ende zwei LF sind
			sfFirstFromStop = trip.getJourneys().get(trip.getJourneys().size()-3).getToStopId();
		}
		// berechne die Gesamtdistanz von Ein- und Ausrueckfahrt zu beiden Depots
		double distanceDepotOne = deadruntimes.get(startDepot + sfFirstFromStop).getDistance() + deadruntimes.get(sfLastToStop + startDepot).getDistance();
		double distanceDepotTwo = deadruntimes.get(endDepot + sfFirstFromStop).getDistance() + deadruntimes.get(sfLastToStop + endDepot).getDistance();
		if(distanceDepotOne <= distanceDepotTwo){
			return startDepot;
		}
		else{
			return endDepot;
		}
	}
	
	public static void assignBestDepot(Roundtrip trip, String bestDepot, HashMap<String, Deadruntime> deadruntimes) {
		String startDepot = trip.getJourneys().getFirst().getFromStopId();
		String endDepot = trip.getJourneys().getLast().getToStopId();
		if(bestDepot.equals(startDepot)){ // das erste Depot ist besser
			trip.getJourneys().removeLast(); // letzte LF aendern
			if(trip.getJourneys().getLast() instanceof Deadruntime){
				trip.getJourneys().removeLast();
			}
			trip.getJourneys().addLast(deadruntimes.get(trip.getJourneys().getLast().getToStopId() + startDepot));
		}
		else{ // das zweite Depot ist besser
			trip.getJourneys().removeFirst(); // erste LF aendern
			if(trip.getJourneys().getFirst() instanceof Deadruntime){
				trip.getJourneys().removeFirst();
			}
			trip.getJourneys().addFirst(deadruntimes.get(endDepot + trip.getJourneys().getFirst().getFromStopId()));
		}
	}
	
	public static Roundtrip roundtripWithCharging(LinkedList<Journey> neu, HashMap<String, Stoppoint> stoppoints, HashMap<String, Deadruntime> deadruntimes, HashMap<String, Servicejourney> servicejourneys, int id){

		Roundtrip result = new Roundtrip(id);
		for (int i = 0; i < neu.size(); i++) {
			if (neu.get(i) instanceof Servicejourney) {
				result.getJourneys().add(neu.get(i)); // alles SF in den neuen Umlauf einbauen
			}
		}
		for (int i = 1; i < result.getJourneys().size(); i++) {
			Servicejourney current = (Servicejourney)result.getJourneys().get(i);
			Servicejourney prev = (Servicejourney)result.getJourneys().get(i-1);
			if (current.getSfDepTime().getTime() < prev.getSfArrTime().getTime()) {
				return null;
			}
		}
		result.addFahrtAfterFahrt(0, deadruntimes.get(neu.getFirst().getFromStopId() + result.getJourneys().getFirst().getFromStopId())); // Ausrueckfahrt einfuegen
		for (int i = 1; i < result.getJourneys().size()-1; i = i + 2) {
			if(result.getAtIndex(i) instanceof Deadruntime){
				break;
			}
			result.addFahrtAfterFahrt(i+1, deadruntimes.get(result.getJourneys().get(i).getToStopId() + result.getJourneys().get(i+1).getFromStopId())); // alle inneren LF einfuegen
		}
		result.getJourneys().add(deadruntimes.get(result.getJourneys().getLast().getToStopId() + neu.getLast().getToStopId())); // Einrueckfahrt einfuegen
	
		String startDepot = result.getJourneys().getFirst().getFromStopId();
		String EndDepot = result.getJourneys().getLast().getToStopId();
		if (!startDepot.equals(EndDepot)){ // der Umlauf hat unterschiedliche Depots
			assignBestDepot(result, searchBestDepot(result, deadruntimes), deadruntimes);
		}

		double capacity = Roundtrip.getCapacity(); // Batteriekapazitaet in kWh 
		ArrayList<Stoppoint> newStations = new ArrayList<Stoppoint>(); // Liste von Haltestellen, die neu gebaut werden
		ArrayList<Stoppoint> existingStations = new ArrayList<Stoppoint>(); // Liste der Haltestellen, an denen in dem Umlauf geldaen wird
		int detourCount = 0; // Variable für zusaetzliche LF im Umlauf aufgrund von Umwegen

		Servicejourney currentSf = null;
		Stoppoint fromStop = null;
		Servicejourney previousSf = null;

		Deadruntime currentDr = null;
		Servicejourney beforeDr = null;
		Servicejourney afterDr = null;

		Stoppoint currentStoppoint = null;

		for (int i = 0; i < result.getJourneys().size(); i++) { // fuer jede Fahrt i im zusammengesetzten Fahrzeugumlauf

			if(result.getJourneys().get(i) instanceof Deadruntime){

				currentDr = (Deadruntime) result.getJourneys().get(i);

				if(i == 0){ // erste Fahrt im Umlauf
					if(capacity < currentDr.getEnergyConsumption()){
						return null;
					}
				}
				/**
				else if(i == 1){ // wegen Umweg zwei LF zu Beginn des Umlaufs
					capacity = Roundtrip.getCapacity() - neu.get(i).getEnergyConsumption();
					continue;
				}
				*/
				else if(i == result.getJourneys().size()-1){ // letzte Fahrt im Umlauf
					if(capacity < currentDr.getEnergyConsumption()){
						currentStoppoint = stoppoints.get(result.getJourneys().get(i-1).getToStopId());
						if(currentStoppoint.isLoadingstation()){
							existingStations.add(currentStoppoint);
							break;
						}
						else if(!(result.getJourneys().get(i-1) instanceof Deadruntime)){
							// prüfen ob eine Ladestation auf einem Umweg vorhanden ist
							Deadruntime bestDetourOne = null;
							Deadruntime bestDetourTwo = null;
							Deadruntime currentDetourOne = null;
							Deadruntime currentDetourTwo = null;
							Double costOfDetour = 100000000.0;	
							Double minimalCosts = 100000000.0;
							Stoppoint load = null;
							for(Entry <String, Stoppoint> e: stoppoints.entrySet()){
								if(!(e.getKey().equals(result.getJourneys().get(i-1).getToStopId()) || e.getKey().equals(result.getJourneys().get(i).getToStopId()))){
									if(e.getValue().isLoadingstation()){ // suche eine Haltestelle an der schon eine Ladestation ist
										currentDetourOne = deadruntimes.get("" + result.getJourneys().get(i-1).getToStopId() + e.getValue().getId());
										currentDetourTwo = deadruntimes.get("" + e.getValue().getId() + result.getJourneys().get(i).getToStopId());
										// Ueberpruefe ob der Umweg von der Kapazitaet her zulaessig ist
										if(capacity >= currentDetourOne.getEnergyConsumption()){
											costOfDetour = currentDetourTwo.getDistance(); // Distanz die nach dem Laden im Umweg noch zurueckgelegt werden muss
											if(costOfDetour < minimalCosts){ // ist der Umweg kuerzer als bisher gefundene Umwege?									minimalCosts = costOfDetour; 
												minimalCosts = costOfDetour; 
												bestDetourOne = currentDetourOne;
												bestDetourTwo = currentDetourTwo;
												load = e.getValue(); // speicher die Haltestelle an der mit Umweg geladen werden kann
											}
										}
									}
								}
							}
							if(load != null){ // es wurde ein zulaessiger Umweg gefunden
								// ist die Kapazitaet beim Fahren des Umwegs aufgrund des Ladens groesser als die Kapazitaet beim Fahren des regulaeren Wegs ohne Laden?
								if((capacity - result.getJourneys().get(i).getEnergyConsumption()) < (capacity - bestDetourTwo.getEnergyConsumption())){ 
									// der Umweg lohnt sich
									result.getJourneys().remove(i); // entferne die direkte LF 
									result.getJourneys().add(i, bestDetourTwo);
									result.getJourneys().add(i, bestDetourOne);
									existingStations.add(load);
									capacity = Roundtrip.getCapacity() - result.getJourneys().get(i).getEnergyConsumption();
									detourCount++; // der Umlauf hat jetzt eine LF mehr als vorher
									continue;
								}
							}
							else{ // baue Ladestation 
								newStations.add(currentStoppoint);
								existingStations.add(currentStoppoint);
								continue;
							}
						}
					}
				}
				/**
				else if(i == neu.size()-2){ // vorletzte Fahrt im Umlauf ist wegen eines Umwegs auch eine LF
					currentStoppoint = stoppoints.get(neu.get(i-1).getToStopId());
					if(currentStoppoint.isLoadingstation()){
						//listOfStations.add(currentStoppoint);
						//indexOfLoad.add(i);
						existingStations.add(currentStoppoint);
						break;
					}
					else{ // baue Ladestation 
						newStations.add(currentStoppoint);
						//indexOfLoad.add(i);
						existingStations.add(currentStoppoint);
						continue;
					}
				} 
				*/
				else{ // alle anderen LF 
					if(detourCount == 0){
						beforeDr = (Servicejourney) result.getJourneys().get(i-1);
						afterDr = (Servicejourney) result.getJourneys().get(i+1);
					}
					else{
						if(result.getJourneys().get(i-1) instanceof Deadruntime){
							beforeDr = (Servicejourney) result.getJourneys().get(i-2);
							afterDr = (Servicejourney) result.getJourneys().get(i+1);
						}
						else if(result.getJourneys().get(i+1) instanceof Deadruntime){
							beforeDr = (Servicejourney) result.getJourneys().get(i-1);
							afterDr = (Servicejourney) result.getJourneys().get(i+2);
						}
						else{
							beforeDr = (Servicejourney) result.getJourneys().get(i-1);
							afterDr = (Servicejourney) result.getJourneys().get(i+1);
						}
					}
					if(FeasibilityHelper.zeitpufferFuerLadezeit(beforeDr.getId(), afterDr.getId(), deadruntimes, servicejourneys, capacity)){
						// ist an der Endhaltestelle der vorherigen SF eine Ladestation?
						currentStoppoint = stoppoints.get(beforeDr.getToStopId());
						if(currentStoppoint.isLoadingstation()){
							existingStations.add(currentStoppoint);
							capacity = Roundtrip.getCapacity() - currentDr.getEnergyConsumption();
							continue;
						}
						if((result.getJourneys().get(i-1) instanceof Servicejourney) && (result.getJourneys().get(i+1) instanceof Servicejourney)){
							// prüfen ob eine Ladestation auf einem Umweg vorhanden ist
							Deadruntime bestDetourOne = null;
							Deadruntime bestDetourTwo = null;
							Deadruntime currentDetourOne = null;
							Deadruntime currentDetourTwo = null;
							Double costOfDetour = 100000000.0;	
							Double minimalCosts = 100000000.0;
							Stoppoint load = null;
							for(Entry <String, Stoppoint> e: stoppoints.entrySet()){
								if(!(e.getKey().equals(beforeDr.getToStopId()) || e.getKey().equals(afterDr.getFromStopId()))){
									if(e.getValue().isLoadingstation()){ // suche eine Haltestelle an der schon eine Ladestation ist
										currentDetourOne = deadruntimes.get("" + beforeDr.getToStopId() + e.getValue().getId());
										currentDetourTwo = deadruntimes.get("" + e.getValue().getId() + afterDr.getFromStopId());
										// Ueberpruefe ob der Umweg von der Kapazitaet her zulaessig ist
										if(capacity >= currentDetourOne.getEnergyConsumption()){
											// ueberpruefen ob Umweg zeitlich zulässig ist
											if(FeasibilityHelper.zeitpufferFuerUmweg(currentDetourOne, currentDetourTwo, beforeDr, afterDr, capacity)){
												costOfDetour = currentDetourTwo.getDistance(); // Distanz die nach dem Laden im Umweg noch zurueckgelegt werden muss
												if(costOfDetour < minimalCosts){ // ist der Umweg kuerzer als bisher gefundene Umwege?									minimalCosts = costOfDetour; 
													minimalCosts = costOfDetour; 
													bestDetourOne = currentDetourOne;
													bestDetourTwo = currentDetourTwo;
													load = e.getValue(); // speicher die Haltestelle an der mit Umweg geladen werden kann
												}
											}
										}
									}
								}
							}
							if(load != null){ // es wurde ein zulaessiger Umweg gefunden
								// ist die Kapazitaet beim Fahren des Umwegs aufgrund des Ladens groesser als die Kapazitaet beim Fahren des regulaeren Wegs ohne Laden?
								if((capacity - currentDr.getEnergyConsumption()) < (capacity - bestDetourTwo.getEnergyConsumption())){ 
									// der Umweg lohnt sich
									result.getJourneys().remove(i); // entferne die direkte LF 
									result.getJourneys().add(i, bestDetourTwo);
									result.getJourneys().add(i, bestDetourOne);
									existingStations.add(load);
									capacity = Roundtrip.getCapacity() - currentDr.getEnergyConsumption();
									detourCount++; // der Umlauf hat jetzt eine LF mehr als vorher
									continue;
								}
							}
						}
						else{ // baue Ladestation 
							newStations.add(currentStoppoint);
							existingStations.add(currentStoppoint);
							break;
						}
					}

				}
			}

			if(result.getJourneys().get(i) instanceof Servicejourney){ // falls i eine SF ist
				
				currentSf = (Servicejourney) result.getJourneys().get(i);
				fromStop = stoppoints.get(currentSf.getFromStopId());

				if(i == 1){ // i ist 1. SF
					// falls schon Ladestation an Starthaltestelle vorhanden
					if(fromStop.isLoadingstation()){
						existingStations.add(fromStop);
						capacity = Roundtrip.getCapacity() - currentSf.getEnergyConsumption();
						continue;
					}
					// prüfen ob eine Ladestation auf einem Umweg vorhanden ist
					Deadruntime bestDetourOne = null;
					Deadruntime bestDetourTwo = null;
					Deadruntime currentDetourOne = null;
					Deadruntime currentDetourTwo = null;
					Double costOfDetour = 100000000.0;	
					Double minimalCosts = 100000000.0;
					Stoppoint load = null;
					for(Entry <String, Stoppoint> e: stoppoints.entrySet()){
						if(!(e.getKey().equals(result.getJourneys().get(0).getFromStopId()) || e.getKey().equals(result.getJourneys().get(0).getToStopId()))){
							if(e.getValue().isLoadingstation()){ // suche eine Haltestelle an der schon eine Ladestation ist
								currentDetourOne = deadruntimes.get("" + result.getJourneys().get(0).getFromStopId() + e.getValue().getId());
								currentDetourTwo = deadruntimes.get("" + e.getValue().getId() + result.getJourneys().get(0).getToStopId());
								// Ueberpruefe ob der Umweg von der Kapazitaet her zulaessig ist
								if(capacity >= currentDetourOne.getEnergyConsumption()){
									costOfDetour = currentDetourTwo.getDistance(); // Distanz die nach dem Laden im Umweg noch zurueckgelegt werden muss
									if(costOfDetour < minimalCosts){ // ist der Umweg kuerzer als bisher gefundene Umwege?								
										minimalCosts = costOfDetour; 
										bestDetourOne = currentDetourOne;
										bestDetourTwo = currentDetourTwo;
										load = e.getValue(); // speicher die Haltestelle an der mit Umweg geladen werden kann
									}
								}
							}
						}
					}
					if(load != null){ // es wurde ein zulaessiger Umweg gefunden
						// ist die Kapazitaet beim Fahren des Umwegs aufgrund des Ladens groesser als die Kapazitaet beim Fahren des regulaeren Wegs ohne Laden?
						if((capacity - result.getJourneys().get(i-1).getEnergyConsumption()) < (capacity - bestDetourTwo.getEnergyConsumption())){ 
							// der Umweg lohnt sich
							result.getJourneys().remove(i-1); // entferne die direkte LF 
							result.getJourneys().add(i-1, bestDetourTwo);
							result.getJourneys().add(i-1, bestDetourOne);
							existingStations.add(load);
							capacity = Roundtrip.getCapacity() - bestDetourTwo.getEnergyConsumption();
							detourCount ++; // der Umlauf hat jetzt eine LF mehr als vorher
							continue;
						}
					}
					// es konnte nicht geladen werden
					if(capacity >= currentSf.getEnergyConsumption()){ 
						// die SF kann auch ohne Laden gefahren werden
						capacity = capacity - currentSf.getEnergyConsumption();
						continue;
					}
					else{ // baue Ladestation an Starthaltestelle von i
						newStations.add(fromStop);
						existingStations.add(fromStop);
						capacity = Roundtrip.getCapacity() - currentSf.getEnergyConsumption();
						continue;
					}
				}

				else{ // ab der 2. SF
					if(i == 2){ // i ist die erste SF aber es gibt einen Umweg davor
						// falls schon Ladestation an Starthaltestelle vorhanden
						if(fromStop.isLoadingstation()){
							existingStations.add(fromStop);
							capacity = Roundtrip.getCapacity() - currentSf.getEnergyConsumption();
							continue;
						}
						// es konnte nicht geladen werden
						if(capacity >= currentSf.getEnergyConsumption()){ 
							// die SF kann auch ohne Laden gefahren werden
							capacity = capacity - currentSf.getEnergyConsumption();
							continue;
						}
						else{ // baue Ladestation an Starthaltestelle von i
							newStations.add(fromStop);
							existingStations.add(fromStop);
							capacity = Roundtrip.getCapacity() - currentSf.getEnergyConsumption();
							continue;
						}
					}
					if(detourCount == 0){
						previousSf = (Servicejourney) result.getJourneys().get(i-2);
					}
					else{
						if(result.getJourneys().get(i-2) instanceof Servicejourney){
							previousSf = (Servicejourney) result.getJourneys().get(i-2);
						}
						else{
							previousSf = (Servicejourney) result.getJourneys().get(i-3);
						}
					}
					// Fall 1: ist es zeitlich moeglich vor der SF zu laden?
					if(FeasibilityHelper.zeitpufferFuerLadezeit(previousSf.getId(), currentSf.getId(), deadruntimes, servicejourneys, capacity)){
						// ist an der Starthaltestelle der SF eine Ladestation?
						if(fromStop.isLoadingstation()){
							existingStations.add(fromStop);
							capacity = Roundtrip.getCapacity() - currentSf.getEnergyConsumption();
							continue;
						}
						// prüfen ob eine Ladestation auf einem Umweg vorhanden ist
						Deadruntime bestDetourOne = null;
						Deadruntime bestDetourTwo = null;
						Deadruntime currentDetourOne = null;
						Deadruntime currentDetourTwo = null;
						Double costOfDetour = 100000000.0;	
						Double minimalCosts = 100000000.0;
						Stoppoint load = null;
						if(result.getJourneys().get(i-2) instanceof Servicejourney){ // wenn vor der SF noch kein Umweg ist
							for(Entry <String, Stoppoint> e: stoppoints.entrySet()){
								if(!(e.getKey().equals(previousSf.getToStopId()) || e.getKey().equals(currentSf.getFromStopId()))){
									if(e.getValue().isLoadingstation()){ // suche eine Haltestelle an der schon eine Ladestation ist
										currentDetourOne = deadruntimes.get("" + previousSf.getToStopId() + e.getValue().getId());
										currentDetourTwo = deadruntimes.get("" + e.getValue().getId() + currentSf.getFromStopId());
										// Ueberpruefe ob der Umweg von der Kapazitaet her zulaessig ist
										if(capacity >= currentDetourOne.getEnergyConsumption()){
											// Ueberpruefe ob der Umweg zeitlich zulässig ist
											if(FeasibilityHelper.zeitpufferFuerUmweg(currentDetourOne, currentDetourTwo, previousSf, currentSf, capacity)){
												costOfDetour = currentDetourTwo.getDistance(); // Distanz die nach dem Laden im Umweg noch zurueckgelegt werden muss
												if(costOfDetour < minimalCosts){ // ist der Umweg kuerzer als bisher gefundene Umwege?
													minimalCosts = costOfDetour; 
													bestDetourOne = currentDetourOne;
													bestDetourTwo = currentDetourTwo;
													load = e.getValue(); // speicher die Haltestelle an der mit Umweg geladen werden kann
												}
											}
										}
									}
								}
							}
						}
						if(load != null){ // es wurde ein zulaessiger Umweg gefunden
							// ist die Kapazitaet beim Fahren des Umwegs aufgrund des Ladens groesser als die Kapazitaet beim Fahren des regulaeren Wegs ohne Laden?
							if((capacity - result.getJourneys().get(i-1).getEnergyConsumption()) < (capacity - bestDetourTwo.getEnergyConsumption())){
								result.getJourneys().remove(i-1); // entferne die direkte LF 
								result.getJourneys().add(i-1, bestDetourTwo);
								result.getJourneys().add(i-1, bestDetourOne);
								existingStations.add(load);
								detourCount ++; // der Umlauf hat jetzt eine LF mehr als vorher
								capacity = Roundtrip.getCapacity() - bestDetourTwo.getEnergyConsumption() - currentSf.getEnergyConsumption();
								continue;
							}
						}
					}

					// Fall 2: Es konnte nicht geladen werden; 
					// kann die SF auch ohne Laden gefahren werden?
					if(capacity >= currentSf.getEnergyConsumption()){ 
						capacity = capacity - currentSf.getEnergyConsumption();
						continue;
					}

					// Fall 3: Es konnte nicht geladen werden und die SF konnte nicht ohne Laden gefahren werden;
					// kann eine Ladestation an der Starthaltestelle der SF gebaut werden?
					if(FeasibilityHelper.zeitpufferFuerLadezeit(previousSf.getId(), currentSf.getId(), deadruntimes, servicejourneys, capacity)){
						newStations.add(fromStop);
						existingStations.add(fromStop);
						capacity = Roundtrip.getCapacity() - currentSf.getEnergyConsumption();
						continue;
					}
					else{
						return null; // Umlauf nicht moeglich
					}
				}
			}	
			capacity = capacity - result.getJourneys().get(i).getEnergyConsumption();
			if(capacity < 0){
				return null;
			}
		}
		result.setBuild(newStations);
		result.setCharge(existingStations);
		return result;
	}
	
	public static ArrayList<ArrayList<Stoppoint>> howIsRoundtourFeasible(LinkedList<Journey> neu, HashMap<String, Stoppoint> stoppoints, HashMap<String, Deadruntime> deadruntimes, HashMap<String, Servicejourney> servicejourneys){
		ArrayList<ArrayList<Stoppoint>> result = new ArrayList<ArrayList<Stoppoint>>();
		double kapazitaet = Roundtrip.getCapacity(); // Batteriekapazitaet in kWh 
		ArrayList<Stoppoint> listOfNewStations = new ArrayList<Stoppoint>(); // Liste von Haltestellen, die neu gebaut werden
		//ArrayList<Integer> indexOfLoad = new ArrayList<Integer>(); // Liste der Indizes, an denen in dem Umlauf geladen wird
		ArrayList<Stoppoint> listOfUsedStations = new ArrayList<Stoppoint>(); // Liste der Haltestellen, an denen in dem Umlauf geldaen wird
		int u = 0; // Variable für zusaetzliche LF im Umlauf aufgrund von Umwegen
		for (int i = 1; i < neu.size(); i++) {
			if((neu.get(i) instanceof Deadruntime) && (neu.get(i-1) instanceof Deadruntime)){
				u ++;
			}
		}
		Servicejourney currentSf = null;
		Stoppoint fromStop = null;
		Servicejourney previousSf = null;

		Deadruntime currentDr = null;
		Servicejourney beforeDr = null;
		Servicejourney afterDr = null;

		Stoppoint currentStoppoint = null;

		for (int i = 0; i < neu.size(); i++) { // fuer jede Fahrt i im zusammengesetzten Fahrzeugumlauf

			if(neu.get(i) instanceof Deadruntime){

				currentDr = (Deadruntime) neu.get(i);

				if(i == 0){ // erste Fahrt im Umlauf
					if(kapazitaet < currentDr.getEnergyConsumption()){
						return null;
					}
				}
				else if(i == 1){ // wegen Umweg zwei LF zu Beginn des Umlaufs
					kapazitaet = Roundtrip.getCapacity() - neu.get(i).getEnergyConsumption();
					continue;
				}
				else if(i == neu.size()-1){ // letzte Fahrt im Umlauf
					if(kapazitaet < currentDr.getEnergyConsumption()){
						currentStoppoint = stoppoints.get(neu.get(i-1).getToStopId());
						if(currentStoppoint.isLoadingstation()){
							//listOfStations.add(currentStoppoint);
							//indexOfLoad.add(i);
							listOfUsedStations.add(currentStoppoint);
							break;
						}
						else if(!(neu.get(i-1) instanceof Deadruntime)){
							// prüfen ob eine Ladestation auf einem Umweg vorhanden ist
							Deadruntime bestDetourOne = null;
							Deadruntime bestDetourTwo = null;
							Deadruntime currentDetourOne = null;
							Deadruntime currentDetourTwo = null;
							Double costOfDetour = 100000000.0;	
							Double minimalCosts = 100000000.0;
							Stoppoint load = null;
							for(Entry <String, Stoppoint> e: stoppoints.entrySet()){
								if(!(e.getKey().equals(neu.get(i-1).getToStopId()) || e.getKey().equals(neu.get(i).getToStopId()))){
									if(e.getValue().isLoadingstation()){ // suche eine Haltestelle an der schon eine Ladestation ist
										currentDetourOne = deadruntimes.get("" + neu.get(i-1).getToStopId() + e.getValue().getId());
										currentDetourTwo = deadruntimes.get("" + e.getValue().getId() + neu.get(i).getToStopId());
										// Ueberpruefe ob der Umweg von der Kapazitaet her zulaessig ist
										if(kapazitaet >= currentDetourOne.getEnergyConsumption()){
											costOfDetour = currentDetourTwo.getDistance(); // Distanz die nach dem Laden im Umweg noch zurueckgelegt werden muss
											if(costOfDetour < minimalCosts){ // ist der Umweg kuerzer als bisher gefundene Umwege?									minimalCosts = costOfDetour; 
												minimalCosts = costOfDetour; 
												bestDetourOne = currentDetourOne;
												bestDetourTwo = currentDetourTwo;
												load = e.getValue(); // speicher die Haltestelle an der mit Umweg geladen werden kann
											}
										}
									}
								}
							}
							if(load != null){ // es wurde ein zulaessiger Umweg gefunden
								// ist die Kapazitaet beim Fahren des Umwegs aufgrund des Ladens groesser als die Kapazitaet beim Fahren des regulaeren Wegs ohne Laden?
								if((kapazitaet - neu.get(i).getEnergyConsumption()) < (kapazitaet - bestDetourTwo.getEnergyConsumption())){ 
									// der Umweg lohnt sich
									neu.remove(i); // entferne die direkte LF 
									neu.add(i, bestDetourTwo);
									neu.add(i, bestDetourOne);
									//listOfStations.add(load);
									//indexOfLoad.add(i+1);
									listOfUsedStations.add(load);
									kapazitaet = Roundtrip.getCapacity() - neu.get(i).getEnergyConsumption();
									u++; // der Umlauf hat jetzt eine LF mehr als vorher
									continue;
								}
							}
							else{ // baue Ladestation 
								listOfNewStations.add(currentStoppoint);
								//indexOfLoad.add(i);
								listOfUsedStations.add(currentStoppoint);
								continue;
							}
						}
					}
				}
				else if(i == neu.size()-2){ // vorletzte Fahrt im Umlauf ist wegen eines Umwegs auch eine LF
					currentStoppoint = stoppoints.get(neu.get(i-1).getToStopId());
					if(currentStoppoint.isLoadingstation()){
						//listOfStations.add(currentStoppoint);
						//indexOfLoad.add(i);
						listOfUsedStations.add(currentStoppoint);
						break;
					}
					else{ // baue Ladestation 
						listOfNewStations.add(currentStoppoint);
						//indexOfLoad.add(i);
						listOfUsedStations.add(currentStoppoint);
						continue;
					}
				} 
				else{ // alle anderen LF 
					if(u == 0){
						beforeDr = (Servicejourney) neu.get(i-1);
						afterDr = (Servicejourney) neu.get(i+1);
					}
					else{
						if(neu.get(i-1) instanceof Deadruntime){
							beforeDr = (Servicejourney) neu.get(i-2);
							afterDr = (Servicejourney) neu.get(i+1);
						}
						else if(neu.get(i+1) instanceof Deadruntime){
							beforeDr = (Servicejourney) neu.get(i-1);
							afterDr = (Servicejourney) neu.get(i+2);
						}
						else{
							beforeDr = (Servicejourney) neu.get(i-1);
							afterDr = (Servicejourney) neu.get(i+1);
						}
					}
					if(FeasibilityHelper.zeitpufferFuerLadezeit(beforeDr.getId(), afterDr.getId(), deadruntimes, servicejourneys, kapazitaet)){
						// ist an der Endhaltestelle der vorherigen SF eine Ladestation?
						currentStoppoint = stoppoints.get(beforeDr.getToStopId());
						if(currentStoppoint.isLoadingstation()){
							//listOfStations.add(currentStoppoint);
							//indexOfLoad.add(i);
							listOfUsedStations.add(currentStoppoint);
							kapazitaet = Roundtrip.getCapacity() - currentDr.getEnergyConsumption();
							continue;
						}
						else if((neu.get(i-1) instanceof Servicejourney) && (neu.get(i+1) instanceof Servicejourney)){
							// prüfen ob eine Ladestation auf einem Umweg vorhanden ist
							Deadruntime bestDetourOne = null;
							Deadruntime bestDetourTwo = null;
							Deadruntime currentDetourOne = null;
							Deadruntime currentDetourTwo = null;
							Double costOfDetour = 100000000.0;	
							Double minimalCosts = 100000000.0;
							Stoppoint load = null;
							for(Entry <String, Stoppoint> e: stoppoints.entrySet()){
								if(!(e.getKey().equals(beforeDr.getToStopId()) || e.getKey().equals(afterDr.getFromStopId()))){
									if(e.getValue().isLoadingstation()){ // suche eine Haltestelle an der schon eine Ladestation ist
										currentDetourOne = deadruntimes.get("" + beforeDr.getToStopId() + e.getValue().getId());
										currentDetourTwo = deadruntimes.get("" + e.getValue().getId() + afterDr.getFromStopId());
										// Ueberpruefe ob der Umweg von der Kapazitaet her zulaessig ist
										if(kapazitaet >= currentDetourOne.getEnergyConsumption()){
											// ueberpruefen ob Umweg zeitlich zulässig ist
											if(FeasibilityHelper.zeitpufferFuerUmweg(currentDetourOne, currentDetourTwo, beforeDr, afterDr, kapazitaet)){
												costOfDetour = currentDetourTwo.getDistance(); // Distanz die nach dem Laden im Umweg noch zurueckgelegt werden muss
												if(costOfDetour < minimalCosts){ // ist der Umweg kuerzer als bisher gefundene Umwege?									minimalCosts = costOfDetour; 
													minimalCosts = costOfDetour; 
													bestDetourOne = currentDetourOne;
													bestDetourTwo = currentDetourTwo;
													load = e.getValue(); // speicher die Haltestelle an der mit Umweg geladen werden kann
												}
											}
										}
									}
								}
							}
							if(load != null){ // es wurde ein zulaessiger Umweg gefunden
								// ist die Kapazitaet beim Fahren des Umwegs aufgrund des Ladens groesser als die Kapazitaet beim Fahren des regulaeren Wegs ohne Laden?
								if((kapazitaet - currentDr.getEnergyConsumption()) < (kapazitaet - bestDetourTwo.getEnergyConsumption())){ 
									// der Umweg lohnt sich
									neu.remove(i); // entferne die direkte LF 
									neu.add(i, bestDetourTwo);
									neu.add(i, bestDetourOne);
									//listOfStations.add(load);
									//indexOfLoad.add(i+1);
									listOfUsedStations.add(load);
									kapazitaet = Roundtrip.getCapacity() - currentDr.getEnergyConsumption();
									u++; // der Umlauf hat jetzt eine LF mehr als vorher
									continue;
								}
							}
						}
						else{ // baue Ladestation 
							listOfNewStations.add(currentStoppoint);
							//indexOfLoad.add(i);
							listOfUsedStations.add(currentStoppoint);
							break;
						}
					}

				}
			}

			if(neu.get(i) instanceof Servicejourney){ // falls i eine SF ist
				currentSf = (Servicejourney) neu.get(i);
				fromStop = stoppoints.get(currentSf.getFromStopId());

				if(i == 1){ // i ist 1. SF
					// falls schon Ladestation an Starthaltestelle vorhanden
					if(fromStop.isLoadingstation()){
						// lade vor der 1. SF
						//listOfStations.add(fromStop);
						//((indexOfLoad.add(i);
						listOfUsedStations.add(fromStop);
						kapazitaet = Roundtrip.getCapacity() - currentSf.getEnergyConsumption();
						continue;
					}
					// prüfen ob eine Ladestation auf einem Umweg vorhanden ist
					Deadruntime bestDetourOne = null;
					Deadruntime bestDetourTwo = null;
					Deadruntime currentDetourOne = null;
					Deadruntime currentDetourTwo = null;
					Double costOfDetour = 100000000.0;	
					Double minimalCosts = 100000000.0;
					Stoppoint load = null;
					for(Entry <String, Stoppoint> e: stoppoints.entrySet()){
						if(!(e.getKey().equals(neu.get(0).getFromStopId()) || e.getKey().equals(neu.get(0).getToStopId()))){
							if(e.getValue().isLoadingstation()){ // suche eine Haltestelle an der schon eine Ladestation ist
								currentDetourOne = deadruntimes.get("" + neu.get(0).getFromStopId() + e.getValue().getId());
								currentDetourTwo = deadruntimes.get("" + e.getValue().getId() + neu.get(0).getToStopId());
								// Ueberpruefe ob der Umweg von der Kapazitaet her zulaessig ist
								if(kapazitaet >= currentDetourOne.getEnergyConsumption()){
									costOfDetour = currentDetourTwo.getDistance(); // Distanz die nach dem Laden im Umweg noch zurueckgelegt werden muss
									if(costOfDetour < minimalCosts){ // ist der Umweg kuerzer als bisher gefundene Umwege?								
										minimalCosts = costOfDetour; 
										bestDetourOne = currentDetourOne;
										bestDetourTwo = currentDetourTwo;
										load = e.getValue(); // speicher die Haltestelle an der mit Umweg geladen werden kann
									}
								}
							}
						}
					}
					if(load != null){ // es wurde ein zulaessiger Umweg gefunden
						// ist die Kapazitaet beim Fahren des Umwegs aufgrund des Ladens groesser als die Kapazitaet beim Fahren des regulaeren Wegs ohne Laden?
						if((kapazitaet - neu.get(i-1).getEnergyConsumption()) < (kapazitaet - bestDetourTwo.getEnergyConsumption())){ 
							// der Umweg lohnt sich
							neu.remove(i-1); // entferne die direkte LF 
							neu.add(i-1, bestDetourTwo);
							neu.add(i-1, bestDetourOne);
							//listOfStations.add(load);
							//indexOfLoad.add(i);
							listOfUsedStations.add(load);
							kapazitaet = Roundtrip.getCapacity() - bestDetourTwo.getEnergyConsumption();
							u ++; // der Umlauf hat jetzt eine LF mehr als vorher
							continue;
						}
					}

					// es konnte nicht geladen werden
					if(kapazitaet >= currentSf.getEnergyConsumption()){ 
						// die SF kann auch ohne Laden gefahren werden
						kapazitaet = kapazitaet - currentSf.getEnergyConsumption();
						continue;
					}
					else{ // baue Ladestation an Starthaltestelle von i
						listOfNewStations.add(fromStop);
						//indexOfLoad.add(i);
						listOfUsedStations.add(fromStop);
						kapazitaet = Roundtrip.getCapacity() - currentSf.getEnergyConsumption();
						continue;
					}
				}

				else{ // ab der 2. SF
					if(i == 2){ // i ist die erste SF aber es gibt einen Umweg davor
						// falls schon Ladestation an Starthaltestelle vorhanden
						if(fromStop.isLoadingstation()){
							// lade vor der 1. SF
							//listOfStations.add(fromStop);
							//indexOfLoad.add(i);
							listOfUsedStations.add(fromStop);
							kapazitaet = Roundtrip.getCapacity() - currentSf.getEnergyConsumption();
							continue;
						}
						// es konnte nicht geladen werden
						if(kapazitaet >= currentSf.getEnergyConsumption()){ 
							// die SF kann auch ohne Laden gefahren werden
							kapazitaet = kapazitaet - currentSf.getEnergyConsumption();
							continue;
						}
						else{ // baue Ladestation an Starthaltestelle von i
							listOfNewStations.add(fromStop);
							//indexOfLoad.add(i);
							listOfUsedStations.add(fromStop);
							kapazitaet = Roundtrip.getCapacity() - currentSf.getEnergyConsumption();
							continue;
						}
					}
					if(u == 0){
						previousSf = (Servicejourney) neu.get(i-2);
					}
					else{
						if(neu.get(i-2) instanceof Servicejourney){
							previousSf = (Servicejourney) neu.get(i-2);
						}
						else{
							previousSf = (Servicejourney) neu.get(i-3);
						}
					}
					// Fall 1: ist es zeitlich moeglich vor der SF zu laden?
					if(FeasibilityHelper.zeitpufferFuerLadezeit(previousSf.getId(), currentSf.getId(), deadruntimes, servicejourneys, kapazitaet)){
						// ist an der Starthaltestelle der SF eine Ladestation?
						if(fromStop.isLoadingstation()){
							//listOfStations.add(fromStop);
							//indexOfLoad.add(i);
							listOfUsedStations.add(fromStop);
							kapazitaet = Roundtrip.getCapacity() - currentSf.getEnergyConsumption();
							continue;
						}
						// prüfen ob eine Ladestation auf einem Umweg vorhanden ist
						Deadruntime bestDetourOne = null;
						Deadruntime bestDetourTwo = null;
						Deadruntime currentDetourOne = null;
						Deadruntime currentDetourTwo = null;
						Double costOfDetour = 100000000.0;	
						Double minimalCosts = 100000000.0;
						Stoppoint load = null;
						if(neu.get(i-2) instanceof Servicejourney){ // wenn vor der SF noch kein Umweg ist
							for(Entry <String, Stoppoint> e: stoppoints.entrySet()){
								if(!(e.getKey().equals(previousSf.getToStopId()) || e.getKey().equals(currentSf.getFromStopId()))){
									if(e.getValue().isLoadingstation()){ // suche eine Haltestelle an der schon eine Ladestation ist
										currentDetourOne = deadruntimes.get("" + previousSf.getToStopId() + e.getValue().getId());
										currentDetourTwo = deadruntimes.get("" + e.getValue().getId() + currentSf.getFromStopId());
										// Ueberpruefe ob der Umweg von der Kapazitaet her zulaessig ist
										if(kapazitaet >= currentDetourOne.getEnergyConsumption()){
											// Ueberpruefe ob der Umweg zeitlich zulässig ist
											if(FeasibilityHelper.zeitpufferFuerUmweg(currentDetourOne, currentDetourTwo, previousSf, currentSf, kapazitaet)){
												costOfDetour = currentDetourTwo.getDistance(); // Distanz die nach dem Laden im Umweg noch zurueckgelegt werden muss
												if(costOfDetour < minimalCosts){ // ist der Umweg kuerzer als bisher gefundene Umwege?
													minimalCosts = costOfDetour; 
													bestDetourOne = currentDetourOne;
													bestDetourTwo = currentDetourTwo;
													load = e.getValue(); // speicher die Haltestelle an der mit Umweg geladen werden kann
												}
											}
										}
									}
								}
							}
						}
						if(load != null){ // es wurde ein zulaessiger Umweg gefunden
							// ist die Kapazitaet beim Fahren des Umwegs aufgrund des Ladens groesser als die Kapazitaet beim Fahren des regulaeren Wegs ohne Laden?
							if((kapazitaet - neu.get(i-1).getEnergyConsumption()) < (kapazitaet - bestDetourTwo.getEnergyConsumption())){
								neu.remove(i-1); // entferne die direkte LF 
								neu.add(i-1, bestDetourTwo);
								neu.add(i-1, bestDetourOne);
								//listOfStations.add(load);
								//indexOfLoad.add(i);
								listOfUsedStations.add(load);
								u ++; // der Umlauf hat jetzt eine LF mehr als vorher
								kapazitaet = Roundtrip.getCapacity() - bestDetourTwo.getEnergyConsumption() - currentSf.getEnergyConsumption();
								continue;
							}
						}
					}

					// Fall 2: Es konnte nicht geladen werden; 
					// kann die SF auch ohne Laden gefahren werden?
					if(kapazitaet >= currentSf.getEnergyConsumption()){ 
						kapazitaet = kapazitaet - currentSf.getEnergyConsumption();
						continue;
					}

					// Fall 3: Es konnte nicht geladen werden und die SF konnte nicht ohne Laden gefahren werden;
					// kann eine Ladestation an der Starthaltestelle der SF gebaut werden?
					if(FeasibilityHelper.zeitpufferFuerLadezeit(previousSf.getId(), currentSf.getId(), deadruntimes, servicejourneys, kapazitaet)){
						listOfNewStations.add(fromStop);
						//indexOfLoad.add(i);
						listOfUsedStations.add(fromStop);
						kapazitaet = Roundtrip.getCapacity() - currentSf.getEnergyConsumption();
						continue;
					}
					else{
						return null; // Umlauf nicht moeglich
					}
				}
			}	
			kapazitaet = kapazitaet - neu.get(i).getEnergyConsumption();
			if(kapazitaet < 0){
				return null;
			}
		}
		result.add(listOfNewStations);
		result.add(listOfUsedStations);
		if(result.get(0) == null){
			System.out.println();
		}
		if(result.get(1) == null){
			System.out.println();
		}
		return result;
	}

	public static ArrayList<ArrayList<Stoppoint>> newLoadingstations(LinkedList<Journey> neu, HashMap<String, Stoppoint> stoppoints, HashMap<String, Deadruntime> deadruntimes, HashMap<String, Servicejourney> servicejourneys){

		double kapazitaet = 80.0; // Batteriekapazitaet in kWh 
		ArrayList<ArrayList<Stoppoint>> numberOfNewStations = new ArrayList<ArrayList<Stoppoint>>();
		ArrayList<Stoppoint> listOfNewStations = new ArrayList<Stoppoint>(); // Liste von Haltestellen, die neu gebaut werden
		ArrayList<Stoppoint> listOfUsedStations = new ArrayList<Stoppoint>(); // Liste von Haltestellen
		int letzteLadung = 0; // ID der Fahrt im Fahrzeugumlauf, wo zuletzt geladen wird

		for (int i = 0; i < neu.size(); i++) { // fuer jede Fahrt i im zusammengesetzten Fahrzeugumlauf

			if (kapazitaet - neu.get(i).getEnergyConsumption() < 0){ // falls Verbrauch von Fahrt i die Restkapazitaet nicht abdeckt

				if(neu.get(i) instanceof Servicejourney){ // falls Fahrt i eine Servicefahrt ist 
					int x = 0;
					while((i-2-x) > letzteLadung){ //solange wir nicht die erste SF oder die LetzteLadung erreichen
						if(FeasibilityHelper.zeitpufferFuerLadezeit(neu.get(i-2-x).getId(), neu.get(i-x).getId(), deadruntimes, servicejourneys, kapazitaet)){
							//wenn genug Zeit zum Laden vorhanden ist
							if(x==0){ //falls direkt bei der betroffenen SF geladen werden kann
								if (!stoppoints.get(neu.get(i).getFromStopId()).isLoadingstation()){ //falls noch keine Ladestation an dieser Stelle vorhanden ist
									listOfNewStations.add(stoppoints.get(neu.get(i).getFromStopId())); //fuege die betroffene Haltestelle in die Liste hinzu
									kapazitaet = 80; // Kapazitaet wieder voll geladen
									letzteLadung = i; // merkt sich, an i die letzte Ladung erfolgt ist
									// stoppoints.get(neu.get(i).getFromStopId()).setLadestation(true); // setzt eine Ladestation an der Starthaltestelle von SF i
									break; 
								}else{ // es ist schon eine Ladestation vorhanden an Haltestelle i - x 
									listOfUsedStations.add(stoppoints.get(neu.get(i).getFromStopId()));
									kapazitaet = 80; // Kapazitaet wieder voll geladen
									letzteLadung = i; // merkt sich, an i die letzte Ladung erfolgt ist
									break;
								} 
							}else{ // falls nicht direkt in i geladen werden kann und damit die vorherigen SF anschauen muss
								if (!stoppoints.get(neu.get(i-2-x).getToStopId()).isLoadingstation()){ // 
									listOfNewStations.add(stoppoints.get(neu.get(i-2-x).getToStopId()));
									kapazitaet = 80; // Kapazitaet wieder voll geladen
									letzteLadung = i - 2 - x; // merkt sich, an welcher Stelle die letzte Ladung erfolgt ist
									// stoppoints.get(neu.get(i-x).getFromStopId()).setLadestation(true); //setzt eine Ladestation an der Starthaltestelle von SF i-x ### 1 mal drin
									i = letzteLadung + 1; // i muss zurueckgesetzt werden, um dort zu starten, wo die Kapazitaet wieder bei 80 ist
									break;
								}else{ // es ist schon eine Ladestation vorhanden an Haltestelle i - x
									listOfUsedStations.add(stoppoints.get(neu.get(i-2-x).getToStopId()));
									kapazitaet = 80; // Kapazitaet wieder voll geladen
									letzteLadung = i - 2 - x; 
									i = letzteLadung + 1; // Verbrauch ab Leerfahrt nach SF i
									break;
								} 
							}
						}
						x = x + 2;
						//System.out.println("x = " + x + "; i = " + i);
					}
					if(kapazitaet != 80){ // wenn nicht geladen werden konnte, dann lade vor Servicefahrt 1 (da geht es zeitlich immer)
						if(letzteLadung == 0){ // schon einmal vor Servicefahrt 1 geladen?
							if (!stoppoints.get(neu.get(1).getFromStopId()).isLoadingstation()){ // falls vor SF1 noch keine Ladestation gebaut wird
								listOfNewStations.add(stoppoints.get(neu.get(1).getFromStopId()));
								kapazitaet = 80;
								letzteLadung = 1;
								// stoppoints.get(neu.get(1).getFromStopId()).setLadestation(true);
								i = 1;

							}else{  // an der Haltestelle ist schon eine Ladestation -> Laden
								listOfUsedStations.add(stoppoints.get(neu.get(1).getFromStopId()));
								kapazitaet = 80;
								i = 1;
								letzteLadung = 1;

							}
						}
						else{ // es wird zum zweiten mal versucht an der gleichen Haltestelle zu laden --> Endlosschleife: Fahrzeugumlauf nicht moeglich
							return null;  
						}
					}
				}	

				if(neu.get(i) instanceof Deadruntime){ // falls Fahrt i eine Leerfahrt ist
					int x = 0;

					while(((i - x - 1) > letzteLadung)){ //solange die LetzteLadung nicht wieder erreicht wird
						if(i == neu.size()-1){ //falls i die letzte Leerfahrt ist
							if (!stoppoints.get(neu.get(i-1).getToStopId()).isLoadingstation()){ //falls keine Ladestation vorhanden an Endhaltestelle von SF (i-1)
								listOfNewStations.add(stoppoints.get(neu.get(i-1).getToStopId()));
								kapazitaet = 80;
								letzteLadung = i - 1;
								// stoppoints.get(neu.get(i-1).getToStopId()).setLadestation(true); #### 7 Mal drin
								//i = i - 1;
								break;
							}else{ // es ist schon eine Ladestation vorhanden an Endhaltestelle von SF (i-1)
								listOfUsedStations.add(stoppoints.get(neu.get(i-1).getToStopId()));
								kapazitaet = 80;
								letzteLadung = i - 1;
								//i = i - 1;
								break;
							} 
						}
						else if(x==0){
							if(FeasibilityHelper.zeitpufferFuerLadezeit(neu.get(i-1).getId(), neu.get(i+1).getId(), deadruntimes, servicejourneys, kapazitaet)){					
								if (!stoppoints.get(neu.get(i-1).getToStopId()).isLoadingstation()){ 
									listOfNewStations.add(stoppoints.get(neu.get(i-1).getToStopId()));
									kapazitaet = 80;
									letzteLadung = i - 1;
									//i = i - 1;
									// stoppoints.get(neu.get(i-1).getToStopId()).setLadestation(true);
									break;
								}else{ // es ist schon eine Ladestation vorhanden an Haltestelle i 
									listOfUsedStations.add(stoppoints.get(neu.get(i-1).getToStopId()));
									kapazitaet = 80;
									letzteLadung = i - 1;
									break;
								} 
							}
							x = x + 2;
						}else{
							if(FeasibilityHelper.zeitpufferFuerLadezeit(neu.get(i-2-x+1).getId(), neu.get(i-x+1).getId(), deadruntimes, servicejourneys, kapazitaet)){
								if (!stoppoints.get(neu.get(i-x-1).getToStopId()).isLoadingstation()){ // i - x ist die Starthaltestelle der Servicefahrt i
									listOfNewStations.add(stoppoints.get(neu.get(i-x-1).getToStopId()));
									kapazitaet = 80;
									letzteLadung = i - x - 1; // merkt sich, an welcher Stelle die letzte Ladung erfolgt ist
									i = i - x; // i muss zurueckgesetzt werden, um dort zu starten, wo die Kapazitaet wieder bei 80 ist
									//stoppoints.get(neu.get(i-x-1).getToStopId()).setLadestation(true);
									break;
								}else{ // es ist schon eine Ladestation vorhanden an Haltestelle i - x
									listOfUsedStations.add(stoppoints.get(neu.get(i-x-1).getToStopId()));
									kapazitaet = 80;
									letzteLadung = i - x - 1;
									i = i - x;
									break;
								} 
							}
							x = x + 2;
						}
						//System.out.println("x = " + x + "; i = " + i);
					}	
					if(kapazitaet != 80){ // wenn nicht geladen werden konnte, dann lade vor Servicefahrt 1 (da geht es zeitlich immer)
						if(letzteLadung == 0){ // schon einmal vor Servicefahrt 1 geladen?
							if (!stoppoints.get(neu.get(1).getFromStopId()).isLoadingstation()){
								listOfNewStations.add(stoppoints.get(neu.get(1).getFromStopId()));
								kapazitaet = 80;
								i = 1;
								letzteLadung = 1;
								//stoppoints.get(neu.get(1).getFromStopId()).setLadestation(true);
							}else{ // an der Haltestelle ist schon eine Ladestation: Laden
								listOfUsedStations.add(stoppoints.get(neu.get(1).getFromStopId()));
								kapazitaet = 80;
								i = 1;
								letzteLadung = 1;
							}
						}
						else{
							return null;
						}
					}
				}	
			}
			kapazitaet = kapazitaet - neu.get(i).getEnergyConsumption(); // aktualisiere die Kapazitaet nach Fahrt i, falls Fahrt i noch gefahren werden kann
		}
		numberOfNewStations.add(listOfNewStations); 
		numberOfNewStations.add(listOfUsedStations); 
		return numberOfNewStations;	
	}

}
