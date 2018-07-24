package heuristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import com.rits.cloning.Cloner;
import helper.FeasibilityHelper;
import helper.Verbesserungsobjekt;
import model.Deadruntime;
import model.Roundtrip;
import model.Schedule;
import model.Servicejourney;
import model.Stoppoint;
import parser.ReadInitialSolution;

public class VariableNeighborhoodSearch {

	Schedule localBest; // lokal beste Loesung: die beste Loesung der aktuellen Iteration
	Schedule globalBest; // global beste Loesung: die beste jemals gefundene Loesung
	Schedule shaking; // Loesung nach dem Shaking: Verschlechterung gegenueber localBest moeglich
	Cloner clone = new Cloner();
	public HashMap<String, Deadruntime> deadruntimes;
	public HashMap<String, Servicejourney> servicejourneys;
	public HashMap<String, Stoppoint> stoppoints;

	public VariableNeighborhoodSearch(ReadInitialSolution data){
		globalBest = data.getInitialSolution();
		deadruntimes = data.getDeadruntimes();
		servicejourneys = data.getServicejourneys();
		stoppoints = data.stoppoints;
		globalBest.printUmlaufplan();
		globalBest.printLoadingstations();
	}
	
	public void startVNS(int iterations, int kMax){
		int i = 1;
		while(i <= iterations){
			shaking();
			bestImprovement(kMax);
			if(localBest.getTotalCosts() <= globalBest.getTotalCosts()){
				globalBest = clone.deepClone(localBest);
				globalBest.printLoadingstations();
				globalBest.printUmlaufplan();
			}
			System.out.println(i);
			i++;
		}
	}

	/** Shaking: Stochastisches Element der VNS: 	
	 * 	zufaellig die aktuelle Loesung (den aktuellen Umlaufplan) manipulieren (Verschlechterung wird zugelassen)
	 * @return gibt die veraenderte Loesung zurueck  
	 */
	public Schedule shaking(){
		shaking = clone.deepClone(globalBest);
		int counter = 0;
		boolean condition = true;
		while(condition) {
			// Zwei beliebige unterschiedliche Umlaeufe aus localBest auswaehlen
			int random1 = (int)(Math.random()*shaking.getUmlaufplan().size());
			int random2 = (int)(Math.random()*shaking.getUmlaufplan().size()); 
			while(random1 == random2){ // es muessen zwei verschiedene Umlaeufe sein
				random2 = (int)(Math.random()*shaking.getUmlaufplan().size()); 
			}
			Roundtrip eins = shaking.getUmlaufplan().get(random1);
			Roundtrip zwei = shaking.getUmlaufplan().get(random2);

			// Zwei beliebige Servicefahrten aus den zwei gewaehlten Umlaefen 
			int randomI = (int)((Math.random()*eins.getJourneys().size())); 
			while(!(eins.getJourneys().get(randomI) instanceof Servicejourney)){ 
				randomI = (int)(Math.random()*eins.getJourneys().size());
			}
			int randomJ = (int)((Math.random()*zwei.getJourneys().size())); 
			while(!(zwei.getJourneys().get(randomJ) instanceof Servicejourney)){ 
				randomJ = (int)(Math.random()*zwei.getJourneys().size());
			}

			Roundtrip einsNeu = new Roundtrip(eins.getId());
			Roundtrip zweiNeu = new Roundtrip(zwei.getId());

			einsNeu.getJourneys().addAll(zwei.getFahrtenVonBis(0, randomJ));
			einsNeu.getJourneys().add(deadruntimes.get(zwei.getJourneys().get(randomJ).getToStopId() + eins.getJourneys().get(randomI).getFromStopId()));
			einsNeu.getJourneys().addAll(eins.getFahrtenVonBis(randomI, eins.getJourneys().size() - 1));

			String startDepotEins = einsNeu.getJourneys().getFirst().getFromStopId();
			String EndDepotEins = einsNeu.getJourneys().getLast().getToStopId();

			if (!startDepotEins.equals(EndDepotEins)){ // der neue Umlauf hat unterschiedliche Depots
				String bestDepot = searchBestDepot(einsNeu);
				if(bestDepot.equals(startDepotEins)){ // das erste Depot ist besser
					einsNeu.getJourneys().removeLast(); // letzte LF aendern
					if(einsNeu.getJourneys().getLast() instanceof Deadruntime){
						einsNeu.getJourneys().removeLast();
					}
					einsNeu.getJourneys().addLast(deadruntimes.get(einsNeu.getJourneys().getLast().getToStopId() + startDepotEins));
				}
				else{ // das zweite Depot ist besser
					einsNeu.getJourneys().removeFirst(); // erste LF aendern
					if(einsNeu.getJourneys().getFirst() instanceof Deadruntime){
						einsNeu.getJourneys().removeFirst();
					}
					einsNeu.getJourneys().addFirst(deadruntimes.get(EndDepotEins + einsNeu.getJourneys().getFirst().getFromStopId()));
				}
			}
			if (einsNeu.isFeasible()) {
				ArrayList<ArrayList<Stoppoint>> listEins = new ArrayList<ArrayList<Stoppoint>>();
				listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), stoppoints, deadruntimes, servicejourneys);
				if(listEins == null){ // Umlauf aufgrund Kapazitaet nicht moeglich
					continue;
				}
				einsNeu.setStellen(listEins.get(1));
				einsNeu.setLaden(listEins.get(1));

				zweiNeu.getJourneys().addAll(eins.getFahrtenVonBis(0, randomI-2));
				zweiNeu.getJourneys().add(deadruntimes.get(eins.getJourneys().get(randomI).getToStopId() + zwei.getJourneys().get(randomJ).getFromStopId()));
				zweiNeu.getJourneys().addAll(zwei.getFahrtenVonBis(randomJ+2, zwei.getJourneys().size() - 1));

				if(zweiNeu.getJourneys().size() < 3){
					System.out.println();
				}
				
				String startDepotZwei = zweiNeu.getJourneys().getFirst().getFromStopId();
				String EndDepotZwei = zweiNeu.getJourneys().getLast().getToStopId();

				if (!startDepotZwei.equals(EndDepotZwei)){ // der Umlauf hat unterschiedliche Depots
					String bestDepot = searchBestDepot(zweiNeu);
					if(bestDepot.equals(startDepotZwei)){ // das erste Depot ist besser
						zweiNeu.getJourneys().removeLast(); // letzte LF aendern
						if(zweiNeu.getJourneys().getLast() instanceof Deadruntime){
							zweiNeu.getJourneys().removeLast();
						}
						zweiNeu.getJourneys().addLast(deadruntimes.get(zweiNeu.getJourneys().getLast().getFromStopId() + startDepotZwei));
					}
					else{ // das zweite Depot ist besser
						zweiNeu.getJourneys().removeFirst(); // erste LF aendern
						if(zweiNeu.getJourneys().getFirst() instanceof Deadruntime){
							zweiNeu.getJourneys().removeFirst();
						}
						zweiNeu.getJourneys().addFirst(deadruntimes.get(EndDepotZwei + zweiNeu.getJourneys().getFirst().getToStopId()));
					}
				}
				if(zweiNeu.isFeasible()){
					ArrayList<ArrayList<Stoppoint>> listZwei = new ArrayList<ArrayList<Stoppoint>>();
					listZwei = FeasibilityHelper.howIsRoundtourFeasible(zweiNeu.getJourneys(), stoppoints, deadruntimes, servicejourneys);
					if (listZwei == null) { // Umlauf aufgrund Kapazitaet nicht moeglich
						continue;
					}
					zweiNeu.setStellen(listZwei.get(1));
					zweiNeu.setLaden(listZwei.get(1));

					if(!(eins.equals(einsNeu) && zwei.equals(zweiNeu))){
						Roundtrip randomEins = null;
						Roundtrip randomZwei = null;
						ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
						for (int i = 0; i < shaking.getUmlaufplan().size(); i++) {
							if (shaking.getUmlaufplan().get(i).getId() == random1) {
								randomEins = shaking.getUmlaufplan().get(i);
								if (!randomEins.getLaden().contains(null)) {
									decreaseFrequencyAt.addAll(randomEins.getLaden());
								}
							}
							if (shaking.getUmlaufplan().get(i).getId() == random2) {
								randomZwei = shaking.getUmlaufplan().get(i);
								if (!randomZwei.getLaden().contains(null)) {
									decreaseFrequencyAt.addAll(randomZwei.getLaden());
								}
							}
						}
						shaking.getUmlaufplan().remove(randomEins);
						shaking.getUmlaufplan().remove(randomZwei);
						shaking.getUmlaufplan().add(einsNeu);
						shaking.getUmlaufplan().add(zweiNeu);

						if(shaking.isFeasible()){ // 1. Abbruchkriterium: es wurde eine zulaessige zufaellige Loesung gefunden
							condition = false;
							for (int i = 0; i < listEins.get(0).size(); i++) {
								Stoppoint x = (Stoppoint) listEins.get(0).get(i);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									shaking.getStoppointsWithLoadingStations().put(x.getId(), x);
									System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
								}
								else{
									x.setFrequency(x.getFrequency()+1);
								}
							}
							for (int i = 0; i < listZwei.get(0).size(); i++) {
								Stoppoint x = (Stoppoint) listZwei.get(0).get(i);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									shaking.getStoppointsWithLoadingStations().put(x.getId(), x);
									System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
								}
								else{
									x.setFrequency(x.getFrequency()+1);
								}
							}
							for (int i = 0; i < decreaseFrequencyAt.size(); i++) {
								if (decreaseFrequencyAt.contains(null)) {
									System.err.println("Attention!");
								}
								Stoppoint current = stoppoints.get(decreaseFrequencyAt.get(i).getId());
								current.setFrequency(current.getFrequency()-1);
								if(current.getFrequency() == 0){
									current.setLoadingstation(false);
									shaking.getStoppointsWithLoadingStations().remove(current);
									System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
								}
							}
							//shaking.printLoadingstations();
						}
						else{ // Kantentausch wird rueckgaengig gemacht
							//System.out.println("Shaking not feasible!");
							shaking = clone.deepClone(globalBest);
						}
					}
				}
			}
			counter ++;
			//System.out.println(counter);
			if (counter >= 10000) { // 2. Abbruchkriterium (um Laufzeit zu begrenzen)
				condition = false; 
			}
		}
		shaking.setNumberOfLoadingStations(shaking.getStoppointsWithLoadingStations().size());
		//System.out.println(shaking.getNumberOfLoadingStations());
		//shaking.calculateCosts();
		//shaking.printUmlaufplan();
		return shaking;
	}

	/** BestImprovement: Methode zum Bestimmen der best moeglichen Verbesserung innerhalb eines Umlaufplans
	 * @param kMax - maximale Groesse der Nachbarschaft
	 * @return gibt die neue lokal beste Loesung zurueck
	 */
	public Schedule bestImprovement(int kMax){
		localBest = clone.deepClone(shaking);
		if(kMax > localBest.getUmlaufplan().size()){
			kMax = localBest.getUmlaufplan().size();
		}
		List<Roundtrip> minimal = listOfMinimalRoundtrips(localBest,5); // kann auch als Parameter uebergeben werden
		int randomMinimal = (int)(Math.random()*minimal.size()); // waehle zufaellig Umlauf aus der Liste der minimalen Umlaeufe
		for (int i = 0; i < localBest.getUmlaufplan().size(); i++) {
			// finde den Index des minimalen Umlaufs in der Liste aller Umlaeufe
			if(minimal.get(randomMinimal).getId() == (localBest.getUmlaufplan().get(i).getId())){
				randomMinimal = i;
				break;
			}
		}
		ArrayList<Integer> randoms = new ArrayList<Integer>(); // Liste der Indizes der zufaellig gezogenen Umlaeufe
		randoms.add(randomMinimal); 
		Verbesserungsobjekt bestZweiOpt = new Verbesserungsobjekt(0.0, null, null, 0, 0); 
		Verbesserungsobjekt bestSfUmlegen = new Verbesserungsobjekt(0.0, null, null, 0, 0);
		int nachbarschaft = 2; // initialisiere die Nachbarschaftsgroesse mit 2
		while(nachbarschaft <= kMax){ // solange Abbruchkriterium nicht erreicht
			// index eines zweiten beliebigen Umlaufs aus dem geshakten Umlaufplan
			int randomNeu = (int)(Math.random()*localBest.getUmlaufplan().size()); 	
			while(randoms.contains(randomNeu)){
				randomNeu = (int)(Math.random()*localBest.getUmlaufplan().size()); 
			}
			randoms.add(randomNeu); 

			// setze Methode Zweioptverbesserung fuer alle Paare von zufaellig gezogenen Umlaeufen ein
			for (int i = 0; i < randoms.size()-1; i++) {
				Verbesserungsobjekt tempZweiOpt = zweiOpt(randoms.get(i), randoms.get(randoms.size()-1)); 
				if(tempZweiOpt != null){ // falls eine Verbesserung vorhanden ist
					if(tempZweiOpt.getSavings() > bestZweiOpt.getSavings()){ // falls diese besser als die bisher beste Verbesserung ist
						bestZweiOpt = tempZweiOpt; 
					}
				}
			}
			// setze Methode sfUmlegen fuer alle Paare von zufaellig gezogenen Umlaeufen ein
			for (int i = 0; i < randoms.size()-1; i++) {
				Verbesserungsobjekt tempSfUmlegen = sfUmlegen(randoms.get(i), randoms.get(randoms.size()-1));
				if(tempSfUmlegen != null){ // falls eine Verbesserung vorhanden ist
					if(tempSfUmlegen.getSavings() > bestSfUmlegen.getSavings()){ // falls diese besser als die bisher beste Verbesserung ist
						bestSfUmlegen = tempSfUmlegen; 
					}
				}
			}
			if(bestZweiOpt.getSavings() == 0 && bestSfUmlegen.getSavings() == 0){ // wenn in der aktuelle Nachbarschaft nichts gespart wird
				System.out.println(nachbarschaft);
				nachbarschaft++; // erhoehe die Nachbarschaft um 1
			}
			else if(bestZweiOpt.getSavings() >= bestSfUmlegen.getSavings()){ // wenn durch das ZweiOpt mehr gespart wird als durch SfUmlegen
				Roundtrip einsNeu = bestZweiOpt.getEins();
				Roundtrip zweiNeu = bestZweiOpt.getZwei();
				/**
				ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), stoppoints, deadruntimes, servicejourneys);
				if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
					continue;
				}
				ArrayList<ArrayList<Stoppoint>> listZwei = FeasibilityHelper.howIsRoundtourFeasible(zweiNeu.getJourneys(), stoppoints, deadruntimes, servicejourneys);
				if (listZwei == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
					continue;
				}
				ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();

				if (!localBest.getUmlaufplan().get(bestZweiOpt.getIndexAltEins()).getLaden().contains(null)) {
					decreaseFrequencyAt.addAll(localBest.getUmlaufplan().get(bestZweiOpt.getIndexAltEins()).getLaden());
				}
				if (!localBest.getUmlaufplan().get(bestZweiOpt.getIndexAltZwei()).getLaden().contains(null)) {
					decreaseFrequencyAt.addAll(localBest.getUmlaufplan().get(bestZweiOpt.getIndexAltZwei()).getLaden());
				}
				**/
				int id = localBest.getUmlaufplan().get(bestZweiOpt.getIndexAltZwei()).getId();
				localBest.getUmlaufplan().remove(bestZweiOpt.getIndexAltEins()); // entferne altEins aus der lokal besten Loesung
				for (int i = 0; i < localBest.getUmlaufplan().size(); i++) { 
					if(localBest.getUmlaufplan().get(i).getId() == (id)){
						localBest.getUmlaufplan().remove(i); // entferne altZwei aus der lokal besten Loesung
						break; // hoert auf sobald altZwei gefunden wird
					}
				}
				if(einsNeu != null){ // wenn null, dann ist der kleiner Umlauf weggefallen
					localBest.getUmlaufplan().add(einsNeu); // fuege Eins in Lokalbest hinzu
				}
				localBest.getUmlaufplan().add(zweiNeu); // fuege Zwei in Lokalbest hinzu

				if(localBest.isFeasible()){
					System.out.println("ZweiOpt mit Nachbarschaft: " + nachbarschaft);
					/**
					for (int i1 = 0; i1 < listEins.get(0).size(); i1++) {
						Stoppoint x = (Stoppoint) listEins.get(0).get(i1);
						if(x.isLoadingstation() == false){
							x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
							x.setFrequency(1);
							localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
							System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
						}
						else{
							x.setFrequency(x.getFrequency()+1);
						}
					}
					einsNeu.setStellen(listEins.get(1));
					einsNeu.setLaden(listEins.get(1));
					for (int i1 = 0; i1 < listZwei.get(0).size(); i1++) {
						Stoppoint x = (Stoppoint) listZwei.get(0).get(i1);
						if(x.isLoadingstation() == false){
							x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
							x.setFrequency(1);
							localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
							System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
						}
						else{
							x.setFrequency(x.getFrequency()+1);
						}
					}
					zweiNeu.setStellen(listZwei.get(1));
					zweiNeu.setLaden(listZwei.get(1));
					*/
					break;
				}
				else{
					localBest = clone.deepClone(shaking); // localBest zuruecksetzen
				}
			}
			else{ // wenn durch das SfUmlegen mehr gespart wird als durch ZweiOpt
				Roundtrip einsNeu = bestSfUmlegen.getEins();
				Roundtrip zweiNeu = bestSfUmlegen.getZwei();
				/**
				ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), stoppoints, deadruntimes, servicejourneys);
				if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
					continue;
				}
				ArrayList<ArrayList<Stoppoint>> listZwei = FeasibilityHelper.howIsRoundtourFeasible(zweiNeu.getJourneys(), stoppoints, deadruntimes, servicejourneys);
				if (listZwei == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
					continue;
				}
				ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();

				if (!localBest.getUmlaufplan().get(bestSfUmlegen.getIndexAltEins()).getLaden().contains(null)) {
					decreaseFrequencyAt.addAll(localBest.getUmlaufplan().get(bestSfUmlegen.getIndexAltEins()).getLaden());
				}
				if (!localBest.getUmlaufplan().get(bestSfUmlegen.getIndexAltZwei()).getLaden().contains(null)) {
					decreaseFrequencyAt.addAll(localBest.getUmlaufplan().get(bestSfUmlegen.getIndexAltZwei()).getLaden());
				}
				*/
				int id = localBest.getUmlaufplan().get(bestSfUmlegen.getIndexAltZwei()).getId();
				localBest.getUmlaufplan().remove(bestSfUmlegen.getIndexAltEins()); // entferne altEins aus der lokal besten Loesung
				for (int i = 0; i < localBest.getUmlaufplan().size(); i++) { 
					if(localBest.getUmlaufplan().get(i).getId() == (id)){
						localBest.getUmlaufplan().remove(i); // entferne altZwei aus der lokal besten Loesung
						break; // hoert auf sobald altZwei gefunden wird
					}
				}
				if(einsNeu != null){ // wenn null, dann ist der kleiner Umlauf weggefallen
					localBest.getUmlaufplan().add(einsNeu); // fuege Eins in Lokalbest hinzu
				}
				localBest.getUmlaufplan().add(zweiNeu); // fuege Zwei in Lokalbest hinzu

				if(localBest.isFeasible()){
					System.out.println("SfUmlegen mit Nachbarschaft: " + nachbarschaft);
					/**
					for (int i1 = 0; i1 < listEins.get(0).size(); i1++) {
						Stoppoint x = (Stoppoint) listEins.get(0).get(i1);
						if(x.isLoadingstation() == false){
							x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
							x.setFrequency(1);
							localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
							System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
						}
						else{
							x.setFrequency(x.getFrequency()+1);
						}
					}
					einsNeu.setStellen(listEins.get(1));
					einsNeu.setLaden(listEins.get(1));
					for (int i1 = 0; i1 < listZwei.get(0).size(); i1++) {
						Stoppoint x = (Stoppoint) listZwei.get(0).get(i1);
						if(x.isLoadingstation() == false){
							x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
							x.setFrequency(1);
							localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
							System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
						}
						else{
							x.setFrequency(x.getFrequency()+1);
						}
					}
					zweiNeu.setStellen(listZwei.get(1));
					zweiNeu.setLaden(listZwei.get(1));
					*/
					break;
				}
				else{
					localBest = clone.deepClone(shaking); // localBest zuruecksetzen
				}
			}
		}
		localBest.setNumberOfLoadingStations(localBest.getStoppointsWithLoadingStations().size());
		//System.out.println(localBest.getNumberOfLoadingStations());
		//localBest.calculateCosts();
		//localBest.printUmlaufplan();
		return localBest;
	}

	/** sfUmlegen: Methode in der eine Servicefahrt von einem kleinen Umlauf in einen groesseren Umlaeuf umgelegt wird.
	 *  Im Idealfall hat der kleinere Umlauf danach keine SF mehr und kann komplett eingespart werden.
	 * @param index1 - kleinerer Umlauf
	 * @param index2 - groesserer Umlauf
	 * @return ein Verbesserungsobjekt
	 */
	private Verbesserungsobjekt sfUmlegen(Integer index1, Integer index2) {

		double savings = 0.0;
		Verbesserungsobjekt result = new Verbesserungsobjekt(savings, null, null, 0, 0); // Initiales Verbesserungsobjekt

		Roundtrip small;
		Roundtrip big;

		if(localBest.getUmlaufplan().get(index1).getJourneys().size() < localBest.getUmlaufplan().get(index2).getJourneys().size()){
			small = localBest.getUmlaufplan().get(index1); 
			big = localBest.getUmlaufplan().get(index2); 
			result.setIndexAltEins(index1);
			result.setIndexAltZwei(index2);
		}else{
			small = localBest.getUmlaufplan().get(index2); 
			big = localBest.getUmlaufplan().get(index1); 
			result.setIndexAltEins(index2);
			result.setIndexAltZwei(index1);
		}

		ArrayList<Servicejourney> sjOfSmall = new ArrayList<Servicejourney>(); // Liste aller SF vom kleineren Umlauf
		for (int i = 1; i < small.getJourneys().size()-1; i++) {
			if(small.getAtIndex(i) instanceof Servicejourney){
				sjOfSmall.add((Servicejourney)small.getAtIndex(i)); 
			}
		}
		ArrayList<Servicejourney> sjOfBig = new ArrayList<Servicejourney>(); // Liste aller SF vom kleineren Umlauf
		for (int i = 1; i < big.getJourneys().size()-1; i++) {
			if(big.getAtIndex(i) instanceof Servicejourney){
				sjOfBig.add((Servicejourney)big.getAtIndex(i)); 
			}
		}

		for (int k = 0; k < sjOfSmall.size(); k++) { // fuer jede SF aus dem kleineren Umlauf 
			Servicejourney kleinSf = sjOfSmall.get(k);
			for (int i = 1; i < sjOfBig.size()-1; i++) { // alle SF aus dem grossen Umlauf ausser erste und letzte
				Servicejourney grossSf = sjOfBig.get(i);
				int indexKlein = small.getIndexOfJourney(kleinSf); // index der gewaehlten SF im kleineren Umlauf
				Roundtrip newBig = new Roundtrip(big.getId());
				Roundtrip newSmall = new Roundtrip(small.getId());
				Servicejourney grossPrev = sjOfBig.get(i-1); 
				if(kleinSf.getSfArrTime().getTime() <= grossSf.getSfDepTime().getTime()){ // passen die Servicefahrten zeitlich?
					Deadruntime nachLf = deadruntimes.get(kleinSf.getToStopId() + grossSf.getFromStopId()); // Nach-Leerfahrt
					if (kleinSf.getSfArrTime().getTime()+nachLf.getRuntime() <= grossSf.getSfDepTime().getTime()) { // passt die SF + die Leerfahrt danach zeitlich?
						Deadruntime vorLf = deadruntimes.get(grossPrev.getToStopId() + kleinSf.getFromStopId()); // Vor-Leerfahrt
						if (grossPrev.getSfArrTime().getTime() + vorLf.getRuntime() <= kleinSf.getSfDepTime().getTime()) { // passt die Leerfahrt davor + die SF zeitlich
							newBig.getJourneys().addAll(big.getFahrtenVonBis(0, big.getIndexOfJourney(grossPrev))); // Anfang big bis einschließlich i-2
							newBig.getJourneys().add(vorLf); // Vor-Leerfahrt
							newBig.getJourneys().add(kleinSf); // SF aus small
							newBig.getJourneys().add(nachLf); // Nach-Leerfahrt
							newBig.getJourneys().addAll(big.getFahrtenVonBis(big.getIndexOfJourney(grossSf), big.getJourneys().size()-1)); // i bis Ende von big
							if(sjOfSmall.size() > 1){ // newSmall wird nur gebaut wenn small mehr als eine SF hat
								if(indexKlein >= 3 && indexKlein <= small.getJourneys().size()-3){ // falls eine mittlere SF geloescht wird
									int smallPrev = small.getIndexOfJourney(sjOfSmall.get(k-1));
									int smallNext = small.getIndexOfJourney(sjOfSmall.get(k+1));
									newSmall.getJourneys().addAll(small.getFahrtenVonBis(0, smallPrev)); // Anfang bis einschließlich vorherige SF aus small
									newSmall.getJourneys().add(deadruntimes.get(small.getAtIndex(smallPrev).getToStopId() + small.getAtIndex(smallNext).getFromStopId())); // Leerfahrt
									newSmall.getJourneys().addAll(small.getFahrtenVonBis(smallNext, small.getJourneys().size()-1)); // naechste SF bis Ende small

									if(!newSmall.isFeasible()){ 
										break; // break wenn newSmall nicht feasible
									}
									else{
										ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(newSmall.getJourneys(), stoppoints, deadruntimes, servicejourneys);
										if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
											continue;
										}
										ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
										if (!small.getLaden().contains(null)) {
											decreaseFrequencyAt.addAll(small.getLaden());
										}
										for (int i1 = 0; i1 < listEins.get(0).size(); i1++) {
											Stoppoint x = (Stoppoint) listEins.get(0).get(i1);
											if(x.isLoadingstation() == false){
												x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
												x.setFrequency(1);
												localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
												System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
											}
											else{
												x.setFrequency(x.getFrequency()+1);
											}
											for (int i11 = 0; i11 < decreaseFrequencyAt.size(); i11++) {
												if (decreaseFrequencyAt.contains(null)) {
													System.err.println("Attention!");
												}
												Stoppoint current = stoppoints.get(decreaseFrequencyAt.get(i11).getId());
												current.setFrequency(current.getFrequency()-1);
												if(current.getFrequency() == 0){
													current.setLoadingstation(false);
													shaking.getStoppointsWithLoadingStations().remove(current);
													System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
												}
											}
										}
										newSmall.setStellen(listEins.get(1));
										newSmall.setLaden(listEins.get(1));
										result.setEins(newSmall);
									}
								}
								else if(indexKlein == 1){ // falls die erste SF geloescht wird
									int smallNext = small.getIndexOfJourney(sjOfSmall.get(k+1));
									newSmall.getJourneys().add(deadruntimes.get(small.getJourneys().getFirst().getFromStopId() + small.getAtIndex(smallNext).getFromStopId())); // erste Leerfahrt von Depot zur naechsten SF
									newSmall.getJourneys().addAll(small.getFahrtenVonBis(smallNext, small.getJourneys().size()-1)); // bis Ende small

									if(!newSmall.isFeasible()){
										break; // break wenn newSmall nicht feasible
									}
									else{
										ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(newSmall.getJourneys(), stoppoints, deadruntimes, servicejourneys);
										if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
											continue;
										}
										ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
										if (!small.getLaden().contains(null)) {
											decreaseFrequencyAt.addAll(small.getLaden());
										}
										for (int i1 = 0; i1 < listEins.get(0).size(); i1++) {
											Stoppoint x = (Stoppoint) listEins.get(0).get(i1);
											if(x.isLoadingstation() == false){
												x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
												x.setFrequency(1);
												localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
												System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
											}
											else{
												x.setFrequency(x.getFrequency()+1);
											}
										}
										for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
											if (decreaseFrequencyAt.contains(null)) {
												System.err.println("Attention!");
											}
											Stoppoint current = stoppoints.get(decreaseFrequencyAt.get(i1).getId());
											current.setFrequency(current.getFrequency()-1);
											if(current.getFrequency() == 0){
												current.setLoadingstation(false);
												shaking.getStoppointsWithLoadingStations().remove(current);
												System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
											}
										}
										newSmall.setStellen(listEins.get(1));
										newSmall.setLaden(listEins.get(1));
										result.setEins(newSmall);
									}
								}
								else if(indexKlein == small.getJourneys().size()-2){ // falls die letzte SF geloescht wird
									int smallPrev = small.getIndexOfJourney(sjOfSmall.get(k-1));
									newSmall.getJourneys().addAll(small.getFahrtenVonBis(0, smallPrev)); // Anfang small bis vorherige SF
									newSmall.getJourneys().add(deadruntimes.get((small.getAtIndex(smallPrev)).getToStopId() + small.getJourneys().getLast().getToStopId())); // Leerfahrt

									if(!newSmall.isFeasible()){
										break; // break wenn newSmall nicht feasible
									}
									else{
										ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(newSmall.getJourneys(), stoppoints, deadruntimes, servicejourneys);
										if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
											continue;
										}
										ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
										if (!small.getLaden().contains(null)) {
											decreaseFrequencyAt.addAll(small.getLaden());
										}
										for (int i1 = 0; i1 < listEins.get(0).size(); i1++) {
											Stoppoint x = (Stoppoint) listEins.get(0).get(i1);
											if(x.isLoadingstation() == false){
												x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
												x.setFrequency(1);
												localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
												System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
											}
											else{
												x.setFrequency(x.getFrequency()+1);
											}
										}
										for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
											if (decreaseFrequencyAt.contains(null)) {
												System.err.println("Attention!");
											}
											Stoppoint current = stoppoints.get(decreaseFrequencyAt.get(i1).getId());
											current.setFrequency(current.getFrequency()-1);
											if(current.getFrequency() == 0){
												current.setLoadingstation(false);
												shaking.getStoppointsWithLoadingStations().remove(current);
												System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
											}
										}
										newSmall.setStellen(listEins.get(1));
										newSmall.setLaden(listEins.get(1));
										result.setEins(newSmall);
									}
								}
								else{
									break;
								} 		
							}
							if(newBig.isFeasible()){ // wenn newBig feasible ist
								ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(newBig.getJourneys(), stoppoints, deadruntimes, servicejourneys);
								if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
									continue;
								}

								ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();

								if (!big.getLaden().contains(null)) {
									decreaseFrequencyAt.addAll(big.getLaden());
								}

								for (int i1 = 0; i1 < listEins.get(0).size(); i1++) {
									Stoppoint x = (Stoppoint) listEins.get(0).get(i1);
									if(x.isLoadingstation() == false){
										x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
										x.setFrequency(1);
										localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
										System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
									}
									else{
										x.setFrequency(x.getFrequency()+1);
									}
								}
								for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
									if (decreaseFrequencyAt.contains(null)) {
										System.err.println("Attention!");
									}
									Stoppoint current = stoppoints.get(decreaseFrequencyAt.get(i1).getId());
									current.setFrequency(current.getFrequency()-1);
									if(current.getFrequency() == 0){
										current.setLoadingstation(false);
										shaking.getStoppointsWithLoadingStations().remove(current);
										System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
									}
								}
								newBig.setStellen(listEins.get(1));
								newBig.setLaden(listEins.get(1));
								
								result.setZwei(newBig);
								savings = result.getEins().getKostenMitLadestationen() + result.getZwei().getKostenMitLadestationen();
								result.setSavings(savings);

								return result;
							}
						}
					}
				}
			}
		}
		return null;
	}

	private Verbesserungsobjekt zweiOpt(Integer index1, Integer index2) {

		double savings = 0.0;
		Verbesserungsobjekt result = new Verbesserungsobjekt(savings, null, null, 0, 0); // Initiales Verbesserungsobjekt

		Roundtrip eins = localBest.getUmlaufplan().get(index1); // 
		Roundtrip zwei = localBest.getUmlaufplan().get(index2); // 

		double currentCostValue = eins.getKostenMitLadestationen() + zwei.getKostenMitLadestationen(); //aktuelle Gesamtkosten von Fahrzeugumlauf eins und zwei
		double initialCostValue = currentCostValue;

		Roundtrip betterEins = null;
		Roundtrip betterZwei = null;

		LinkedList<Servicejourney> sfEins = new LinkedList<Servicejourney>();
		LinkedList<Servicejourney> sfZwei = new LinkedList<Servicejourney>();

		for (int i = 0; i < eins.getJourneys().size(); i++) { 
			if(eins.getJourneys().get(i) instanceof Servicejourney){ // es werden nur Servicefahrten betrachtet
				sfEins.add((Servicejourney) eins.getJourneys().get(i));
			}
		}
		for (int j = 0; j < zwei.getJourneys().size(); j++) { 
			if(zwei.getJourneys().get(j) instanceof Servicejourney){ // es werden nur Servicefahrten betrachtet
				sfZwei.add((Servicejourney) zwei.getJourneys().get(j));
			}
		}

		// versuche Kantentausch mit allen Paaren von inneren Servicefahrten (i nach j)
		for (int i = 1; i < sfEins.size()-1; i++) { 
			for (int j = 1; j < sfZwei.size()-1; j++) {
				if(FeasibilityHelper.compatibility(sfEins.get(i), sfZwei.get(j), deadruntimes) 
						&& FeasibilityHelper.compatibility(sfZwei.get(j-1), sfEins.get(i+1), deadruntimes)){

					Roundtrip einsNeu = new Roundtrip(eins.getId());
					einsNeu.getJourneys().addAll(eins.getFahrtenVonBis(0, eins.getIndexOfJourney(sfEins.get(i))));
					String deadruntimeId = eins.getAtIndex(i).getToStopId() + zwei.getAtIndex(j).getFromStopId(); 
					einsNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
					einsNeu.getJourneys().addAll(zwei.getFahrtenVonBis(zwei.getIndexOfJourney(sfZwei.get(j)), zwei.getJourneys().size()-1));
					if(einsNeu.isFeasible()){ // zeitliche Sequenzen sind zulaessig
						Roundtrip zweiNeu = new Roundtrip(zwei.getId());
						zweiNeu.getJourneys().addAll(zwei.getFahrtenVonBis(0, zwei.getIndexOfJourney(sfZwei.get(j-1))));
						deadruntimeId = zwei.getAtIndex(j-1).getToStopId() + eins.getAtIndex(i+1).getFromStopId(); 
						zweiNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
						zweiNeu.getJourneys().addAll(eins.getFahrtenVonBis(eins.getIndexOfJourney(sfEins.get(i+1)), eins.getJourneys().size()-1));
						if(zweiNeu.isFeasible()){
							ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), stoppoints, deadruntimes, servicejourneys);
							if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
								continue;
							}
							ArrayList<ArrayList<Stoppoint>> listZwei = FeasibilityHelper.howIsRoundtourFeasible(zweiNeu.getJourneys(), stoppoints, deadruntimes, servicejourneys);
							if (listZwei == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
								continue;
							}
							ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();

							if (!eins.getLaden().contains(null)) {
								decreaseFrequencyAt.addAll(eins.getLaden());
							}
							if (!zwei.getLaden().contains(null)) {
								decreaseFrequencyAt.addAll(zwei.getLaden());
							}
							for (int i1 = 0; i1 < listEins.get(0).size(); i1++) {
								Stoppoint x = (Stoppoint) listEins.get(0).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
								}
								else{
									x.setFrequency(x.getFrequency()+1);
								}
							}
							for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
								if (decreaseFrequencyAt.contains(null)) {
									System.err.println("Attention!");
								}
								Stoppoint current = stoppoints.get(decreaseFrequencyAt.get(i1).getId());
								current.setFrequency(current.getFrequency()-1);
								if(current.getFrequency() == 0){
									current.setLoadingstation(false);
									shaking.getStoppointsWithLoadingStations().remove(current);
									System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
								}
							}
							einsNeu.setStellen(listEins.get(1));
							einsNeu.setLaden(listEins.get(1));
							for (int i1 = 0; i1 < listZwei.get(0).size(); i1++) {
								Stoppoint x = (Stoppoint) listZwei.get(0).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
								}
								else{
									x.setFrequency(x.getFrequency()+1);
								}
							}
							for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
								if (decreaseFrequencyAt.contains(null)) {
									System.err.println("Attention!");
								}
								Stoppoint current = stoppoints.get(decreaseFrequencyAt.get(i1).getId());
								current.setFrequency(current.getFrequency()-1);
								if(current.getFrequency() == 0){
									current.setLoadingstation(false);
									shaking.getStoppointsWithLoadingStations().remove(current);
									System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
								}
							}
							zweiNeu.setStellen(listZwei.get(1));
							zweiNeu.setLaden(listZwei.get(1));
							
							// neue Umlaeufe speichern, falls besser
							double newCostValue = einsNeu.getKostenMitLadestationen() + zweiNeu.getKostenMitLadestationen();
							if(newCostValue < currentCostValue){
								currentCostValue = newCostValue;
								betterEins = einsNeu;
								betterZwei = zweiNeu;
							}
						}	
					}
				}
			}
		}
		// Kopie von oben für die Betrachtung aller Rueckwartskanten (j nach i)
		for (int i = 1; i < sfEins.size()-1; i++) { 
			for (int j = 1; j < sfZwei.size()-1; j++) {
				if(FeasibilityHelper.compatibility(sfEins.get(i-1), sfZwei.get(j+1), deadruntimes) 
						&& FeasibilityHelper.compatibility(sfZwei.get(j), sfEins.get(i), deadruntimes)){

					Roundtrip einsNeu = new Roundtrip(eins.getId());
					einsNeu.getJourneys().addAll(eins.getFahrtenVonBis(0, eins.getIndexOfJourney(sfEins.get(i-1))));
					String deadruntimeId = eins.getAtIndex(i-1).getToStopId() + zwei.getAtIndex(j+1).getFromStopId(); 
					einsNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
					einsNeu.getJourneys().addAll(zwei.getFahrtenVonBis(zwei.getIndexOfJourney(sfZwei.get(j+1)), zwei.getJourneys().size()-1));
					if(einsNeu.isFeasible()){
						Roundtrip zweiNeu = new Roundtrip(zwei.getId());
						zweiNeu.getJourneys().addAll(zwei.getFahrtenVonBis(0, zwei.getIndexOfJourney(sfZwei.get(j))));
						deadruntimeId = zwei.getAtIndex(j).getToStopId() + eins.getAtIndex(i).getFromStopId(); 
						zweiNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
						zweiNeu.getJourneys().addAll(eins.getFahrtenVonBis(eins.getIndexOfJourney(sfEins.get(i)), eins.getJourneys().size()-1));
						if(zweiNeu.isFeasible()){
							ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), stoppoints, deadruntimes, servicejourneys);
							if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
								continue;
							}
							ArrayList<ArrayList<Stoppoint>> listZwei = FeasibilityHelper.howIsRoundtourFeasible(zweiNeu.getJourneys(), stoppoints, deadruntimes, servicejourneys);
							if (listZwei == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
								continue;
							}
							ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();

							if (!eins.getLaden().contains(null)) {
								decreaseFrequencyAt.addAll(eins.getLaden());
							}
							if (!zwei.getLaden().contains(null)) {
								decreaseFrequencyAt.addAll(zwei.getLaden());
							}
							for (int i1 = 0; i1 < listEins.get(0).size(); i1++) {
								Stoppoint x = (Stoppoint) listEins.get(0).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
								}
								else{
									x.setFrequency(x.getFrequency()+1);
								}
							}
							einsNeu.setStellen(listEins.get(1));
							einsNeu.setLaden(listEins.get(1));
							for (int i1 = 0; i1 < listZwei.get(0).size(); i1++) {
								Stoppoint x = (Stoppoint) listZwei.get(0).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
								}
								else{
									x.setFrequency(x.getFrequency()+1);
								}
							}
							zweiNeu.setStellen(listZwei.get(1));
							zweiNeu.setLaden(listZwei.get(1));
							
							// neue Umlaeufe speichern, falls besser
							double newCostValue = einsNeu.getKostenMitLadestationen() + zweiNeu.getKostenMitLadestationen();
							if(newCostValue < currentCostValue){
								currentCostValue = newCostValue;
								betterEins = einsNeu;
								betterZwei = zweiNeu;
							}
						}	
					}
				}
			}
		}

		if(!eins.equals(betterEins) && betterEins != null){ // falls mindestens eine Verbesserung vorhanden ist, wird die Beste zurueckgegeben

			savings = initialCostValue - currentCostValue;
			result.setSavings(savings);
			result.setEins(betterEins);
			result.setZwei(betterZwei);
			result.setIndexAltEins(index1);
			result.setIndexAltZwei(index2);
		}

		return result;
	}

	/**
	 * Methode erstellt eine Liste der minimalen Umlaeufe der uebergebenen Loesung
	 * (d.h. der Umlaeufe mit den wenigsten Fahrten)
	 * @param size: Groesse der Liste
	 * @return Liste der minimalen Umlaeufe
	 */
	private List<Roundtrip> listOfMinimalRoundtrips(Schedule schedule, int size){
		List<Roundtrip> minimal = new ArrayList<Roundtrip>();
		for (int i = 0; i < size; i++) {
			minimal.add(schedule.getUmlaufplan().get(i));
		}
		for (int i = 5; i < schedule.getUmlaufplan().size(); i++) {
			for (int j = 0; j < minimal.size(); j++) {
				if(schedule.getUmlaufplan().get(i).getJourneys().size() < minimal.get(j).getJourneys().size()){
					if(!minimal.contains(schedule.getUmlaufplan().get(i))){
						minimal.remove(j);
						minimal.add(schedule.getUmlaufplan().get(i));	
					}			
				}
			}
		}
		return minimal;
	}
	
	private String searchBestDepot(Roundtrip trip){
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
		String sfLastToStop = trip.getJourneys().get(trip.getJourneys().size()-1).getToStopId();
		if(trip.getJourneys().get(1) instanceof Deadruntime){
			sfFirstFromStop = trip.getJourneys().get(2).getFromStopId();
		}
		if(trip.getJourneys().get(trip.getJourneys().size()-2) instanceof Deadruntime){
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
}
