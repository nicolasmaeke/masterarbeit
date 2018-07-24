/**
 * 
 */
package helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Vector;

import model.Deadruntime;
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
					if(kapazitaet < neu.get(i).getEnergyConsumption()){
						return null;
					}
				}
				else if(i == 1){ // wegen Umweg zwei LF zu Beginn des Umlaufs
					if(stoppoints.get(neu.get(i).getFromStopId()).isLoadingstation()){
						kapazitaet = Roundtrip.getCapacity() - neu.get(i).getEnergyConsumption();
						continue;
					}
				}
				else if(i == neu.size()-1){ // letzte Fahrt im Umlauf
					if(kapazitaet < neu.get(i).getEnergyConsumption()){
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
		return result;
	}
}
