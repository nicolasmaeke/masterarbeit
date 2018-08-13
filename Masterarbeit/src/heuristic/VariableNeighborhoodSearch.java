package heuristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

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
	//public HashMap<String, Stoppoint> stoppoints;

	public VariableNeighborhoodSearch(ReadInitialSolution data){
		globalBest = data.getInitialSolution();
		deadruntimes = data.getDeadruntimes();
		servicejourneys = data.getServicejourneys();
		//stoppoints = data.stoppoints;
		globalBest.printUmlaufplan();
		globalBest.printLoadingstations();
	}

	public void startVNS(int iterations, int kMax){
		int i = 1;
		int countLoading = 0;
		for(Entry <String, Stoppoint> e: globalBest.getStoppointsWithLoadingStations().entrySet()){
			countLoading = countLoading + e.getValue().getFrequency();
		}
		while(i <= iterations){
			shaking();

			bestImprovement(kMax);

			if(localBest.getTotalCosts() <= globalBest.getTotalCosts()){
				if(localBest.getTotalCosts() <= shaking.getTotalCosts()){ 
					globalBest = clone.deepClone(localBest);
					globalBest.calculateCosts();
					globalBest.printLoadingstations();
					globalBest.printUmlaufplan();
				}
				else{ // falls die shaking-Loesung zufaellig besser war
					globalBest = clone.deepClone(shaking);
					globalBest.printLoadingstations();
					globalBest.printUmlaufplan();
				}
			}
			System.out.println("Iteration: " + i);
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
			//Roundtrip eins = shaking.getUmlaufplan().get(47);
			//Roundtrip zwei = shaking.getUmlaufplan().get(64);
			Roundtrip einsNeu = new Roundtrip(eins.getId());
			Roundtrip zweiNeu = new Roundtrip(zwei.getId());
			ArrayList<Servicejourney> sfOfEins = new ArrayList<Servicejourney>();
			ArrayList<Servicejourney> sfOfZwei = new ArrayList<Servicejourney>();
			ArrayList<ArrayList<Stoppoint>> listEins = new ArrayList<ArrayList<Stoppoint>>();
			ArrayList<ArrayList<Stoppoint>> listZwei = new ArrayList<ArrayList<Stoppoint>>();
			Roundtrip randomEins = shaking.getUmlaufplan().get(random1);
			Roundtrip randomZwei = shaking.getUmlaufplan().get(random2);

			for (int i = 0; i < eins.getJourneys().size(); i++) {
				if (eins.getJourneys().get(i) instanceof Servicejourney) {
					Servicejourney toAdd = (Servicejourney) eins.getJourneys().get(i);
					sfOfEins.add(toAdd);
				}
			}
			for (int i = 0; i < zwei.getJourneys().size(); i++) {
				if (zwei.getJourneys().get(i) instanceof Servicejourney) {
					sfOfZwei.add((Servicejourney) zwei.getJourneys().get(i));
				}
			}
			int randomI = (int)((Math.random()*sfOfEins.size())); 
			int randomJ = (int)((Math.random()*sfOfZwei.size())); 

			// einsNeu bauen
			for (int i = 0; i <= randomJ; i++) {
				einsNeu.getJourneys().add(sfOfZwei.get(i));	// alle SF aus zwei bis randomJ einfuegen
			}
			for (int i = randomI; i < sfOfEins.size(); i++) {
				einsNeu.getJourneys().add(sfOfEins.get(i));	// alle SF aus eins beginnend bei randomI einfuegen
			}
			einsNeu.addFahrtAfterFahrt(0, deadruntimes.get(zwei.getDepot() + einsNeu.getJourneys().getFirst().getFromStopId())); // Ausrueckfahrt einfuegen
			for (int i = 1; i < einsNeu.getJourneys().size()-1; i = i + 2) {
				if(einsNeu.getAtIndex(i) instanceof Deadruntime){
					break;
				}
				einsNeu.addFahrtAfterFahrt(i+1, deadruntimes.get(einsNeu.getJourneys().get(i).getFromStopId() + einsNeu.getJourneys().get(i+1).getToStopId())); // alle inneren LF einfuegen
			}
			einsNeu.getJourneys().add(deadruntimes.get(einsNeu.getJourneys().getLast().getToStopId() + eins.getDepot())); // Einrueckfahrt einfuegen

			if (einsNeu.isFeasible()) {

				// zweiNeu bauen
				for (int i = 0; i <= randomI-1; i++) {
					zweiNeu.getJourneys().add(sfOfEins.get(i));	// alle SF aus eins bis randomI-1 einfuegen
				}
				for (int i = randomJ+1; i < sfOfZwei.size(); i++) {
					zweiNeu.getJourneys().add(sfOfZwei.get(i));	// alle SF aus zwei beginnend bei randomJ+1 einfuegen
				}
				if(zweiNeu.getJourneys().size() >= 1){
					zweiNeu.addFahrtAfterFahrt(0, deadruntimes.get(eins.getDepot() + zweiNeu.getJourneys().getFirst().getFromStopId())); // Ausrueckfahrt einfuegen
					for (int i = 1; i < zweiNeu.getJourneys().size()-1; i = i + 2) {
						if(zweiNeu.getAtIndex(i) instanceof Deadruntime){
							break;
						}
						zweiNeu.addFahrtAfterFahrt(i + 1, deadruntimes.get(zweiNeu.getJourneys().get(i).getFromStopId() + zweiNeu.getJourneys().get(i+1).getToStopId())); // alle inneren LF einfuegen
					}
					zweiNeu.getJourneys().add(deadruntimes.get(zweiNeu.getJourneys().getLast().getToStopId() + zwei.getDepot())); // Einrueckfahrt einfuegen

					String startDepotZwei = zweiNeu.getJourneys().getFirst().getFromStopId();
					String EndDepotZwei = zweiNeu.getJourneys().getLast().getToStopId();
					if (!startDepotZwei.equals(EndDepotZwei)){ // der Umlauf hat unterschiedliche Depots
						String bestDepot = searchBestDepot(zweiNeu);
						assignBestDepot(zweiNeu, bestDepot);
					}
					if(zweiNeu.isFeasible()){

						if(!(eins.equals(einsNeu) && zwei.equals(zweiNeu))){
							shaking.getUmlaufplan().remove(randomEins);
							shaking.getUmlaufplan().remove(randomZwei);
							shaking.getUmlaufplan().add(einsNeu);
							shaking.getUmlaufplan().add(zweiNeu);
						}
					}
					else{
						continue;
					}
					if(shaking.isFeasible()){ // 1. Abbruchkriterium: es wurde eine zulaessige zufaellige Loesung gefunden

						condition = false;

						ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
						if (!randomEins.getLaden().contains(null)) {
							decreaseFrequencyAt.addAll(randomEins.getLaden());
						}
						if (!randomZwei.getLaden().contains(null)) {
							decreaseFrequencyAt.addAll(randomZwei.getLaden());
						}
						for (int i = 0; i < decreaseFrequencyAt.size(); i++) {
							if (decreaseFrequencyAt.contains(null)) {
								System.err.println("Attention!");
							}
							Stoppoint current = decreaseFrequencyAt.get(i);
							current.setFrequency(current.getFrequency()-1);
							if(current.getFrequency() == 0){
								current.setLoadingstation(false);
								shaking.getStoppointsWithLoadingStations().remove(current.getId());
								for (int j = 0; j < shaking.getUmlaufplan().size(); j++) {
									for (int j2 = 0; j2 < shaking.getUmlaufplan().get(j).getLaden().size(); j2++) {
										if (shaking.getUmlaufplan().get(j).getLaden().get(j2) != null){
											if (shaking.getUmlaufplan().get(j).getLaden().get(j2).getId().equals(current.getId())) {
												shaking.getUmlaufplan().get(j).getLaden().remove(j2);
											}
										}
									}
								}
								//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
							}
						}

						listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), shaking.getStoppoints(), deadruntimes, servicejourneys);
						if(listEins == null){ // Umlauf aufgrund Kapazitaet nicht moeglich
							shaking = clone.deepClone(globalBest);
							continue;
						}
						einsNeu.setStellen(listEins.get(1));
						einsNeu.setLaden(listEins.get(1));

						listZwei = FeasibilityHelper.howIsRoundtourFeasible(zweiNeu.getJourneys(), shaking.getStoppoints(), deadruntimes, servicejourneys);
						if (listZwei == null) { // Umlauf aufgrund Kapazitaet nicht moeglich
							shaking = clone.deepClone(globalBest);
							continue;
						}
						zweiNeu.setStellen(listZwei.get(1));
						zweiNeu.setLaden(listZwei.get(1));

						for (int i = 0; i < listEins.get(1).size(); i++) {
							Stoppoint x = (Stoppoint) listEins.get(1).get(i);
							if(x.isLoadingstation() == false){
								x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
								x.setFrequency(1);
								shaking.getStoppointsWithLoadingStations().put(x.getId(), x);
								//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
							}
							else{
								x.setFrequency(x.getFrequency()+1);
							}
						}
						for (int i = 0; i < listZwei.get(1).size(); i++) {
							Stoppoint x = (Stoppoint) listZwei.get(1).get(i);
							if(x.isLoadingstation() == false){
								x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
								x.setFrequency(1);
								shaking.getStoppointsWithLoadingStations().put(x.getId(), x);
								//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
							}
							else{
								x.setFrequency(x.getFrequency()+1);
							}
						}
						//shaking.printLoadingstations();
					}
					else{ // Kantentausch wird rueckgaengig gemacht
						shaking = clone.deepClone(globalBest);
					}
				}
			}
			counter ++;
			if (counter >= 1000) { // 2. Abbruchkriterium (um Laufzeit zu begrenzen)
				condition = false; 
				shaking = clone.deepClone(globalBest);
			}
		}
		shaking.setNumberOfLoadingStations(shaking.getStoppointsWithLoadingStations().size());
		shaking.calculateCosts();
		return shaking;
	}

	/** BestImprovement: Methode zum Bestimmen der best moeglichen Verbesserung innerhalb eines Umlaufplans
	 * @param kMax - maximale Groesse der Nachbarschaft
	 * @return gibt die neue lokal beste Loesung zurueck
	 */
	public Schedule bestImprovement(int kMax){
		localBest = clone.deepClone(shaking);
		//localBest = clone.deepClone(globalBest);
		if(kMax > localBest.getUmlaufplan().size()){
			kMax = localBest.getUmlaufplan().size();
		}
		double savings = 0.0;
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
		Verbesserungsobjekt bestZweiOpt = new Verbesserungsobjekt(0.0, null, null, null, null); 
		Verbesserungsobjekt bestSfUmlegen = new Verbesserungsobjekt(0.0, null, null, null, null);
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
					else{
						localBest = clone.deepClone(shaking);
					}
				}
			}	
			
			// setze Methode sfUmlegen fuer alle Paare von zufaellig gezogenen Umlaeufen ein
			for (int i = 0; i < randoms.size()-1; i++) {
				Verbesserungsobjekt tempSfUmlegen = sfUmlegen(randoms.get(i), randoms.get(randoms.size()-1));
				if(tempSfUmlegen.getSavings() != 0.0){ // falls eine Verbesserung vorhanden ist
					if(tempSfUmlegen.getSavings() > bestSfUmlegen.getSavings()){ // falls diese besser als die bisher beste Verbesserung ist
						bestSfUmlegen = tempSfUmlegen; 
					}
					else{
						localBest = clone.deepClone(shaking);
					}
				}
			}

			if(bestZweiOpt.getSavings() == 0 && bestSfUmlegen.getSavings() == 0){ // wenn in der aktuelle Nachbarschaft nichts gespart wird
				System.out.println("Groesse der Nachbarschaft: " + nachbarschaft);
				nachbarschaft++; // erhoehe die Nachbarschaft um 1
			}
			else if(bestZweiOpt.getSavings() >= bestSfUmlegen.getSavings()){ // wenn durch das ZweiOpt mehr gespart wird als durch SfUmlegen

				savings = bestZweiOpt.getSavings();
				Roundtrip altEins = bestZweiOpt.getAltEins();
				Roundtrip altZwei = bestZweiOpt.getAltZwei();
				Roundtrip einsNeu = bestZweiOpt.getEins();
				Roundtrip zweiNeu = bestZweiOpt.getZwei();

				for (int i = 0; i < localBest.getUmlaufplan().size(); i++) {
					if(localBest.getUmlaufplan().get(i).getId() == (altEins.getId())){
						localBest.getUmlaufplan().remove(i);
						break;
					}
				}
				for (int i = 0; i < localBest.getUmlaufplan().size(); i++) {
					if(localBest.getUmlaufplan().get(i).getId() == (altZwei.getId())){
						localBest.getUmlaufplan().remove(i);
						break;
					}
				}

				if(einsNeu != null){ // wenn null, dann ist der kleiner Umlauf weggefallen
					localBest.getUmlaufplan().add(einsNeu); // fuege Eins in Lokalbest hinzu
				}
				localBest.getUmlaufplan().add(zweiNeu); // fuege Zwei in Lokalbest hinzu

				if(localBest.isFeasible()){
					System.out.println("ZweiOpt mit Nachbarschaft: " + nachbarschaft);
					localBest.setNumberOfLoadingStations(localBest.getStoppointsWithLoadingStations().size());
					localBest.calculateCosts();
					break;
				}
				else{
					localBest = clone.deepClone(shaking);
				}
			}
			else{ // wenn durch das SfUmlegen mehr gespart wird als durch ZweiOpt

				savings = bestSfUmlegen.getSavings();
				Roundtrip altEins = bestSfUmlegen.getAltEins();
				Roundtrip altZwei = bestSfUmlegen.getAltZwei();
				Roundtrip einsNeu = bestSfUmlegen.getEins();
				Roundtrip zweiNeu = bestSfUmlegen.getZwei();

				for (int i = 0; i < localBest.getUmlaufplan().size(); i++) {
					if(localBest.getUmlaufplan().get(i).getId() == (altEins.getId())){
						localBest.getUmlaufplan().remove(i);
						break;
					}
				}
				for (int i = 0; i < localBest.getUmlaufplan().size(); i++) {
					if(localBest.getUmlaufplan().get(i).getId() == (altZwei.getId())){
						localBest.getUmlaufplan().remove(i);
						break;
					}
				}

				if(einsNeu != null){ // wenn null, dann ist der kleiner Umlauf weggefallen
					localBest.getUmlaufplan().add(einsNeu); // fuege Eins in Lokalbest hinzu
				}
				else{
					System.out.println();
				}
				localBest.getUmlaufplan().add(zweiNeu); // fuege Zwei in Lokalbest hinzu

				if(localBest.isFeasible()){
					System.out.println("SfUmlegen mit Nachbarschaft: " + nachbarschaft);
					localBest.setNumberOfLoadingStations(localBest.getStoppointsWithLoadingStations().size());
					//localBest.setTotalCosts(shaking.getTotalCosts() - savings);
					localBest.calculateCosts();
					break;
				}
				else{
					localBest = clone.deepClone(shaking);
				}
			}
		}
		return localBest;
	}

	/** sfUmlegen: Methode in der eine Servicefahrt von einem kleinen Umlauf in einen groesseren Umlaeuf umgelegt wird.
	 *  Im Idealfall hat der kleinere Umlauf danach keine SF mehr und kann komplett eingespart werden.
	 * @param index1 - kleinerer Umlauf
	 * @param index2 - groesserer Umlauf
	 * @return ein Verbesserungsobjekt
	 */
	private Verbesserungsobjekt sfUmlegen(Integer index1, Integer index2) {

		int countLoading = 0;
		for(Entry <String, Stoppoint> e: localBest.getStoppointsWithLoadingStations().entrySet()){
			countLoading = countLoading + e.getValue().getFrequency();
		}
		Verbesserungsobjekt result = new Verbesserungsobjekt(0.0, null, null, null, null); // Initiales Verbesserungsobjekt

		Roundtrip small;
		Roundtrip big;
		int flag;
		boolean setRoundtripsAgain = false;

		if(localBest.getUmlaufplan().get(index1).getJourneys().size() < localBest.getUmlaufplan().get(index2).getJourneys().size()){
			small = localBest.getUmlaufplan().get(index1); 
			big = localBest.getUmlaufplan().get(index2); 
			result.setAltEins(localBest.getUmlaufplan().get(index1));
			result.setAltZwei(localBest.getUmlaufplan().get(index2));
			flag = 0;
		}else{
			small = localBest.getUmlaufplan().get(index2); 
			big = localBest.getUmlaufplan().get(index1); 
			result.setAltEins(localBest.getUmlaufplan().get(index2));
			result.setAltZwei(localBest.getUmlaufplan().get(index1));
			flag = 1;
		}

		//double initialCosts = small.getKostenMitLadestationen() + big.getKostenMitLadestationen();
		double initialCosts = small.getKosten() + big.getKosten();

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

		ArrayList<ArrayList<Stoppoint>> listEins = new ArrayList<ArrayList<Stoppoint>>();
		ArrayList<ArrayList<Stoppoint>> listZwei = new ArrayList<ArrayList<Stoppoint>>();

		for (int k = 0; k < sjOfSmall.size(); k++) { // fuer jede SF aus dem kleineren Umlauf 
			Servicejourney kleinSf = sjOfSmall.get(k);
			for (int i = 1; i < sjOfBig.size()-1; i++) { // alle SF aus dem grossen Umlauf ausser erste und letzte
				if(setRoundtripsAgain == true){
					if(flag == 0){
						for (int j = 0; j < localBest.getUmlaufplan().size(); j++) {
							if(localBest.getUmlaufplan().get(j).getId() == small.getId()){
								small = localBest.getUmlaufplan().get(j); 
							}
							if(localBest.getUmlaufplan().get(j).getId() == big.getId()){
								big = localBest.getUmlaufplan().get(j); 
							}
						}
					}
					else{
						for (int j = 0; j < localBest.getUmlaufplan().size(); j++) {
							if(localBest.getUmlaufplan().get(j).getId() == small.getId()){
								small = localBest.getUmlaufplan().get(j); 
							}
							if(localBest.getUmlaufplan().get(j).getId() == big.getId()){
								big = localBest.getUmlaufplan().get(j); 
							}
						}
					}
					sjOfSmall.clear(); // Liste aller SF vom kleineren Umlauf
					for (int i1 = 1; i1 < small.getJourneys().size()-1; i1++) {
						if(small.getAtIndex(i1) instanceof Servicejourney){
							sjOfSmall.add((Servicejourney)small.getAtIndex(i1)); 
						}
					}
					sjOfBig.clear();; // Liste aller SF vom kleineren Umlauf
					for (int i1 = 1; i1 < big.getJourneys().size()-1; i1++) {
						if(big.getAtIndex(i1) instanceof Servicejourney){
							sjOfBig.add((Servicejourney)big.getAtIndex(i1)); 
						}
					}
					kleinSf = sjOfSmall.get(k);
					setRoundtripsAgain = false;
				}
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
									if(k == sjOfSmall.size()-1){
										newSmall.getJourneys().addAll(small.getFahrtenVonBis(0, smallPrev)); // Anfang bis einschließlich vorherige SF aus small
										newSmall.getJourneys().add(deadruntimes.get(small.getAtIndex(smallPrev).getToStopId() + small.getDepot())); // Einrueckfahrt
									}
									else{
										int smallNext = small.getIndexOfJourney(sjOfSmall.get(k+1));
										newSmall.getJourneys().addAll(small.getFahrtenVonBis(0, smallPrev)); // Anfang bis einschließlich vorherige SF aus small
										newSmall.getJourneys().add(deadruntimes.get(small.getAtIndex(smallPrev).getToStopId() + small.getAtIndex(smallNext).getFromStopId())); // Leerfahrt
										newSmall.getJourneys().addAll(small.getFahrtenVonBis(smallNext, small.getJourneys().size()-1)); // naechste SF bis Ende small
									}

									if(!newSmall.isFeasible()){ 
										localBest = clone.deepClone(shaking);
										setRoundtripsAgain = true;
										break; // break wenn newSmall nicht feasible
									}
									else{

										ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
										if (!small.getLaden().contains(null)) {
											decreaseFrequencyAt.addAll(small.getLaden());
										}
										for (int i11 = 0; i11 < decreaseFrequencyAt.size(); i11++) {
											if (decreaseFrequencyAt.contains(null)) {
												System.err.println("Attention!");
											}
											Stoppoint current = decreaseFrequencyAt.get(i11);
											current.setFrequency(current.getFrequency()-1);
											if(current.getFrequency() == 0){
												current.setLoadingstation(false);
												localBest.getStoppointsWithLoadingStations().remove(current.getId());
												for (int j = 0; j < localBest.getUmlaufplan().size(); j++) {
													for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j).getLaden().size(); j2++) {
														if (localBest.getUmlaufplan().get(j).getLaden().get(j2) != null){
															if (localBest.getUmlaufplan().get(j).getLaden().get(j2).getId().equals(current.getId())) {
																localBest.getUmlaufplan().get(j).getLaden().remove(j2);
															}
														}
													}
												}
												//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
											}
										}

										listEins = FeasibilityHelper.howIsRoundtourFeasible(newSmall.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
										if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
											localBest = clone.deepClone(shaking);
											countLoading = 0;
											setRoundtripsAgain = true;
											continue;
										}
										for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
											Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
											if(x.isLoadingstation() == false){
												x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
												x.setFrequency(1);
												localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
												//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
											}
											else{
												x.setFrequency(x.getFrequency()+1);
											}
										}
										countLoading = 0;
										for(Entry <String, Stoppoint> e: localBest.getStoppointsWithLoadingStations().entrySet()){
											countLoading = countLoading + e.getValue().getFrequency();
										}
										newSmall.setStellen(listEins.get(1));
										newSmall.setLaden(listEins.get(1));
									}
								}
								else if(indexKlein == 1){ // falls die erste SF geloescht wird
									if(k == sjOfSmall.size()-1){ // nur eine einzige SF in small
										System.out.println();;
									}
									int smallNext = small.getIndexOfJourney(sjOfSmall.get(k+1));
									newSmall.getJourneys().add(deadruntimes.get(small.getJourneys().getFirst().getFromStopId() + small.getAtIndex(smallNext).getFromStopId())); // erste Leerfahrt von Depot zur naechsten SF
									newSmall.getJourneys().addAll(small.getFahrtenVonBis(smallNext, small.getJourneys().size()-1)); // bis Ende small

									if(!newSmall.isFeasible()){
										localBest = clone.deepClone(shaking);
										setRoundtripsAgain = true;
										break; // break wenn newSmall nicht feasible
									}
									else{

										ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
										if (!small.getLaden().contains(null)) {
											decreaseFrequencyAt.addAll(small.getLaden());
										}
										for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
											if (decreaseFrequencyAt.contains(null)) {
												System.err.println("Attention!");
											}
											Stoppoint current = decreaseFrequencyAt.get(i1);
											current.setFrequency(current.getFrequency()-1);
											if(current.getFrequency() == 0){
												current.setLoadingstation(false);
												localBest.getStoppointsWithLoadingStations().remove(current.getId());
												for (int j = 0; j < localBest.getUmlaufplan().size(); j++) {
													for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j).getLaden().size(); j2++) {
														if (localBest.getUmlaufplan().get(j).getLaden().get(j2) != null){
															if (localBest.getUmlaufplan().get(j).getLaden().get(j2).getId().equals(current.getId())) {
																localBest.getUmlaufplan().get(j).getLaden().remove(j2);
															}
														}
													}
												}
												//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
											}
										}

										listEins = FeasibilityHelper.howIsRoundtourFeasible(newSmall.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
										if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
											localBest = clone.deepClone(shaking);
											countLoading = 0;
											setRoundtripsAgain = true;
											continue;
										}
										for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
											Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
											if(x.isLoadingstation() == false){
												x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
												x.setFrequency(1);
												localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
												//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
											}
											else{
												x.setFrequency(x.getFrequency()+1);
											}
										}
										countLoading = 0;
										for(Entry <String, Stoppoint> e: localBest.getStoppointsWithLoadingStations().entrySet()){
											countLoading = countLoading + e.getValue().getFrequency();
										}
										newSmall.setStellen(listEins.get(1));
										newSmall.setLaden(listEins.get(1));
									}
								}
								else if(indexKlein == small.getJourneys().size()-2){ // falls die letzte SF geloescht wird
									int smallPrev = small.getIndexOfJourney(sjOfSmall.get(k-1));
									newSmall.getJourneys().addAll(small.getFahrtenVonBis(0, smallPrev)); // Anfang small bis vorherige SF
									newSmall.getJourneys().add(deadruntimes.get((small.getAtIndex(smallPrev)).getToStopId() + small.getJourneys().getLast().getToStopId())); // Leerfahrt

									if(!newSmall.isFeasible()){
										localBest = clone.deepClone(shaking);
										setRoundtripsAgain = true;
										break; // break wenn newSmall nicht feasible
									}
									else{

										ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
										if (!small.getLaden().contains(null)) {
											decreaseFrequencyAt.addAll(small.getLaden());
										}
										for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
											if (decreaseFrequencyAt.contains(null)) {
												System.err.println("Attention!");
											}
											Stoppoint current = decreaseFrequencyAt.get(i1);
											current.setFrequency(current.getFrequency()-1);
											if(current.getFrequency() == 0){
												current.setLoadingstation(false);
												localBest.getStoppointsWithLoadingStations().remove(current.getId());
												for (int j = 0; j < localBest.getUmlaufplan().size(); j++) {
													for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j).getLaden().size(); j2++) {
														if (localBest.getUmlaufplan().get(j).getLaden().get(j2) != null){
															if (localBest.getUmlaufplan().get(j).getLaden().get(j2).getId().equals(current.getId())) {
																localBest.getUmlaufplan().get(j).getLaden().remove(j2);
															}
														}
													}
												}
												//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
											}
										}

										listEins = FeasibilityHelper.howIsRoundtourFeasible(newSmall.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
										if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
											localBest = clone.deepClone(shaking);
											setRoundtripsAgain = true;
											continue;
										}
										for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
											Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
											if(x.isLoadingstation() == false){
												x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
												x.setFrequency(1);
												localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
												//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
											}
											else{
												x.setFrequency(x.getFrequency()+1);
											}
										}
										countLoading = 0;
										for(Entry <String, Stoppoint> e: localBest.getStoppointsWithLoadingStations().entrySet()){
											countLoading = countLoading + e.getValue().getFrequency();
										}
										newSmall.setStellen(listEins.get(1));
										newSmall.setLaden(listEins.get(1));
									}
								}
								else{
									break;
								} 
							}
							if(newBig.isFeasible()){ // wenn newBig feasible ist

								ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
								decreaseFrequencyAt.addAll(big.getLaden());
								for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
									if (decreaseFrequencyAt.contains(null)) {
										System.err.println("Attention!");
									}
									Stoppoint current = decreaseFrequencyAt.get(i1);
									current.setFrequency(current.getFrequency()-1);
									if(current.getFrequency() == 0){
										current.setLoadingstation(false);
										localBest.getStoppointsWithLoadingStations().remove(current.getId());
										for (int j = 0; j < localBest.getUmlaufplan().size(); j++) {
											for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j).getLaden().size(); j2++) {
												if (localBest.getUmlaufplan().get(j).getLaden().get(j2) != null){
													if (localBest.getUmlaufplan().get(j).getLaden().get(j2).getId().equals(current.getId())) {
														localBest.getUmlaufplan().get(j).getLaden().remove(j2);
													}
												}
											}
										}
										//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
									}
								}

								listZwei = FeasibilityHelper.howIsRoundtourFeasible(newBig.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
								if (listZwei == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
									localBest = clone.deepClone(shaking);
									setRoundtripsAgain = true;
									continue;
								}
								for (int i1 = 0; i1 < listZwei.get(1).size(); i1++) {
									Stoppoint x = (Stoppoint) listZwei.get(1).get(i1);
									if(x.isLoadingstation() == false){
										x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
										x.setFrequency(1);
										localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
										//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
									}
									else{
										x.setFrequency(x.getFrequency()+1);
									}
								}
								countLoading = 0;
								for(Entry <String, Stoppoint> e: localBest.getStoppointsWithLoadingStations().entrySet()){
									countLoading = countLoading + e.getValue().getFrequency();
								}
								newBig.setStellen(listZwei.get(1));
								newBig.setLaden(listZwei.get(1));

								double newCosts = 0.0;
								double fictiveSavings = 0.0;
								double savings = 0.0;
								double verbrauchsKosten = 0.0;
								double personalkosten = 0.0;

								for (int i1 = 0; i1 < newSmall.getJourneys().size(); i1++) {
									verbrauchsKosten = verbrauchsKosten + newSmall.getJourneys().get(i1).getEnergyConsumption();
									personalkosten = personalkosten + newSmall.getJourneys().get(i1).getRuntime();
								}
								personalkosten = personalkosten * Schedule.STAFF_COSTS / 1000 / 60 / 60;
								verbrauchsKosten = verbrauchsKosten * Schedule.LOADING_COSTS;

								fictiveSavings = 400000/(small.getNumberOfServicejourneys()) - (verbrauchsKosten + personalkosten);

								if(newSmall.getJourneys().size() == 0){
									//newCosts = newBig.getKostenMitLadestationen();
									newCosts = newBig.getKosten();
								}
								else{
									//newCosts = newSmall.getKostenMitLadestationen() + newBig.getKostenMitLadestationen();
									newCosts = newSmall.getKosten() + newBig.getKosten();
								}

								savings = (initialCosts - newCosts) + fictiveSavings;

								if(savings > 0 && savings  > result.getSavings()){
									result.setSavings(savings);
									result.setZwei(newBig);
									if(newSmall.getJourneys().size() != 0){
										result.setEins(newSmall);
									}
								}

								countLoading = 0;
								for(Entry <String, Stoppoint> e: localBest.getStoppointsWithLoadingStations().entrySet()){
									countLoading = countLoading + e.getValue().getFrequency();
								}
								return result;
							}
							else{
								countLoading = 0;
								for(Entry <String, Stoppoint> e: localBest.getStoppointsWithLoadingStations().entrySet()){
									countLoading = countLoading + e.getValue().getFrequency();
								}
								localBest = clone.deepClone(shaking);
								countLoading = 0;
								for(Entry <String, Stoppoint> e: localBest.getStoppointsWithLoadingStations().entrySet()){
									countLoading = countLoading + e.getValue().getFrequency();
								}
								setRoundtripsAgain = true;
								//localBest = clone.deepClone(globalBest);
							}
						}
					}
				}
			}
		}
		return result;
	}

	private Verbesserungsobjekt zweiOpt(Integer index1, Integer index2) {

		double savings = 0.0;
		Verbesserungsobjekt result = new Verbesserungsobjekt(savings, null, null, null, null); // Initiales Verbesserungsobjekt

		Roundtrip eins = localBest.getUmlaufplan().get(index1); // 
		Roundtrip zwei = localBest.getUmlaufplan().get(index2); // 

		double currentCostValue = eins.getKostenMitLadestationen() + zwei.getKostenMitLadestationen(); //aktuelle Gesamtkosten von Fahrzeugumlauf eins und zwei
		double initialCostValue = currentCostValue;

		Roundtrip betterEins = null;
		Roundtrip betterZwei = null;

		LinkedList<Servicejourney> sfEins = new LinkedList<Servicejourney>();
		LinkedList<Servicejourney> sfZwei = new LinkedList<Servicejourney>();

		boolean setRoundtripsAgain = false;

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

		
		// versuche erste SF von Eins mit erster SF von Zwei zu vertauschen
		if(FeasibilityHelper.compatibility(sfEins.getFirst(), sfZwei.getFirst(), deadruntimes)){
			if(sfEins.size() == 1){ // es wuerde ein Umlauf wegfallen
				Roundtrip einsNeu = new Roundtrip(eins.getId());
				int indexOfSfEins = eins.getIndexOfJourney(sfEins.getFirst());
				int indexOfSfZwei = zwei.getIndexOfJourney(sfZwei.getFirst());
				einsNeu.getJourneys().addAll(eins.getFahrtenVonBis(0, indexOfSfEins));
				String deadruntimeId = eins.getAtIndex(indexOfSfEins).getToStopId() + zwei.getAtIndex(indexOfSfZwei).getFromStopId(); 
				einsNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
				einsNeu.getJourneys().addAll(zwei.getFahrtenVonBis(indexOfSfZwei, zwei.getJourneys().size()-1));
				if(einsNeu.isFeasible()){ // zeitliche Sequenzen sind zulaessig
					ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
					if (!eins.getLaden().contains(null)) {
						decreaseFrequencyAt.addAll(eins.getLaden());
					}
					if (!zwei.getLaden().contains(null)) {
						decreaseFrequencyAt.addAll(zwei.getLaden());
					}
					for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
						if (decreaseFrequencyAt.contains(null)) {
							System.err.println("Attention!");
						}
						Stoppoint current = decreaseFrequencyAt.get(i1);
						current.setFrequency(current.getFrequency()-1);
						if(current.getFrequency() == 0){
							current.setLoadingstation(false);
							localBest.getStoppointsWithLoadingStations().remove(current.getId());
							for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
								for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j1).getLaden().size(); j2++) {
									if (localBest.getUmlaufplan().get(j1).getLaden().get(j2) != null){
										if (localBest.getUmlaufplan().get(j1).getLaden().get(j2).getId().equals(current.getId())) {
											localBest.getUmlaufplan().get(j1).getLaden().remove(j2);
										}
									}
								}
							}
							//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
						}
					}
					ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
					if (listEins == null) { // einer der Umlaeufe aufgrund der Kapazitaet nicht moeglich
						localBest = clone.deepClone(shaking);
						setRoundtripsAgain = true;
					}
					else{
						for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
							Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
							if(x.isLoadingstation() == false){
								x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
								x.setFrequency(1);
								localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
								//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
							}
							else{
								x.setFrequency(x.getFrequency()+1);
							}
						}
						einsNeu.setStellen(listEins.get(1));
						einsNeu.setLaden(listEins.get(1));
						
						// neue Umlaeufe speichern, falls besser
						double newCostValue = einsNeu.getKostenMitLadestationen();
						if(newCostValue < currentCostValue){
							currentCostValue = newCostValue;
							betterEins = einsNeu;
						}
						else{
							localBest = clone.deepClone(shaking);
							setRoundtripsAgain = true;
						}
					}
				}
			}
			else{
				Roundtrip einsNeu = new Roundtrip(eins.getId());
				int indexOfSfEins = eins.getIndexOfJourney(sfEins.getFirst());
				int indexOfSfZwei = zwei.getIndexOfJourney(sfZwei.getFirst());
				einsNeu.getJourneys().addAll(eins.getFahrtenVonBis(0, indexOfSfEins));
				String deadruntimeId = eins.getAtIndex(indexOfSfEins).getToStopId() + zwei.getAtIndex(indexOfSfZwei).getFromStopId(); 
				einsNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
				einsNeu.getJourneys().addAll(zwei.getFahrtenVonBis(indexOfSfZwei, zwei.getJourneys().size()-1));
				if(einsNeu.isFeasible()){ // zeitliche Sequenzen sind zulaessig
					Roundtrip zweiNeu = new Roundtrip(zwei.getId());
					deadruntimeId = zwei.getDepot() + eins.getAtIndex(eins.getIndexOfJourney(sfEins.get(1))).getFromStopId(); 
					// neue Ausrueckfahrt hinzufuegen
					zweiNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
					zweiNeu.getJourneys().addAll(eins.getFahrtenVonBis(eins.getIndexOfJourney(sfEins.get(1)), eins.getJourneys().size()-1));
					if(zweiNeu.isFeasible()){
						ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
						if (!eins.getLaden().contains(null)) {
							decreaseFrequencyAt.addAll(eins.getLaden());
						}
						if (!zwei.getLaden().contains(null)) {
							decreaseFrequencyAt.addAll(zwei.getLaden());
						}
						for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
							if (decreaseFrequencyAt.contains(null)) {
								System.err.println("Attention!");
							}
							Stoppoint current = decreaseFrequencyAt.get(i1);
							current.setFrequency(current.getFrequency()-1);
							if(current.getFrequency() == 0){
								current.setLoadingstation(false);
								localBest.getStoppointsWithLoadingStations().remove(current.getId());
								for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
									for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j1).getLaden().size(); j2++) {
										if (localBest.getUmlaufplan().get(j1).getLaden().get(j2) != null){
											if (localBest.getUmlaufplan().get(j1).getLaden().get(j2).getId().equals(current.getId())) {
												localBest.getUmlaufplan().get(j1).getLaden().remove(j2);
											}
										}
									}
								}
								//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
							}
						}

						ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
						ArrayList<ArrayList<Stoppoint>> listZwei = FeasibilityHelper.howIsRoundtourFeasible(zweiNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
						if (listZwei == null || listEins == null) { // einer der Umlaeufe aufgrund der Kapazitaet nicht moeglich
							localBest = clone.deepClone(shaking);
							setRoundtripsAgain = true;
						}
						else{
							for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
								Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
								}
								else{
									x.setFrequency(x.getFrequency()+1);
								}
							}
							einsNeu.setStellen(listEins.get(1));
							einsNeu.setLaden(listEins.get(1));
							for (int i1 = 0; i1 < listZwei.get(1).size(); i1++) {
								Stoppoint x = (Stoppoint) listZwei.get(1).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
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
							else{
								localBest = clone.deepClone(shaking);
								setRoundtripsAgain = true;
							}
						}
			}
			
				}
			}
		}
		
		// versuche letzte SF von Eins mit letzter SF von Zwei zu vertauschen
		if(FeasibilityHelper.compatibility(sfEins.getLast(), sfZwei.getLast(), deadruntimes)){
			if(setRoundtripsAgain == true){
				for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
					if(localBest.getUmlaufplan().get(j1).getId() == eins.getId()){
						eins = localBest.getUmlaufplan().get(j1); 
					}
					if(localBest.getUmlaufplan().get(j1).getId() == zwei.getId()){
						zwei = localBest.getUmlaufplan().get(j1); 
					}
				}
				sfEins.clear();
				for (int i1 = 0; i1 < eins.getJourneys().size(); i1++) { 
					if(eins.getJourneys().get(i1) instanceof Servicejourney){ // es werden nur Servicefahrten betrachtet
						sfEins.add((Servicejourney) eins.getJourneys().get(i1));
					}
				}
				sfZwei.clear();
				for (int j1 = 0; j1 < zwei.getJourneys().size(); j1++) { 
					if(zwei.getJourneys().get(j1) instanceof Servicejourney){ // es werden nur Servicefahrten betrachtet
						sfZwei.add((Servicejourney) zwei.getJourneys().get(j1));
					}
				}
				setRoundtripsAgain = false;
			}
			if(sfZwei.size() == 1){ // in diesem Fall wuerde ein Umlauf wegfallen
				Roundtrip einsNeu = new Roundtrip(eins.getId());
				int indexOfSfEins = eins.getIndexOfJourney(sfEins.getLast());
				int indexOfSfZwei = zwei.getIndexOfJourney(sfZwei.getLast());
				einsNeu.getJourneys().addAll(eins.getFahrtenVonBis(0, indexOfSfEins));
				String deadruntimeId = eins.getAtIndex(indexOfSfEins).getToStopId() + zwei.getAtIndex(indexOfSfZwei).getFromStopId(); 
				einsNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
				einsNeu.getJourneys().addAll(zwei.getFahrtenVonBis(indexOfSfZwei, zwei.getJourneys().size()-1));
				if(einsNeu.isFeasible()){ // zeitliche Sequenzen sind zulaessig
					ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
					if (!eins.getLaden().contains(null)) {
						decreaseFrequencyAt.addAll(eins.getLaden());
					}
					if (!zwei.getLaden().contains(null)) {
						decreaseFrequencyAt.addAll(zwei.getLaden());
					}
					for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
						if (decreaseFrequencyAt.contains(null)) {
							System.err.println("Attention!");
						}
						Stoppoint current = decreaseFrequencyAt.get(i1);
						current.setFrequency(current.getFrequency()-1);
						if(current.getFrequency() == 0){
							current.setLoadingstation(false);
							localBest.getStoppointsWithLoadingStations().remove(current.getId());
							for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
								for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j1).getLaden().size(); j2++) {
									if (localBest.getUmlaufplan().get(j1).getLaden().get(j2) != null){
										if (localBest.getUmlaufplan().get(j1).getLaden().get(j2).getId().equals(current.getId())) {
											localBest.getUmlaufplan().get(j1).getLaden().remove(j2);
										}
									}
								}
							}
							//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
						}
					}
					ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
					if (listEins == null) { // einer der Umlaeufe aufgrund der Kapazitaet nicht moeglich
						localBest = clone.deepClone(shaking);
						setRoundtripsAgain = true;
					}
					else{
						for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
							Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
							if(x.isLoadingstation() == false){
								x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
								x.setFrequency(1);
								localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
								//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
							}
							else{
								x.setFrequency(x.getFrequency()+1);
							}
						}
						einsNeu.setStellen(listEins.get(1));
						einsNeu.setLaden(listEins.get(1));
						
						// neue Umlaeufe speichern, falls besser
						double newCostValue = einsNeu.getKostenMitLadestationen();
						if(newCostValue < currentCostValue){
							currentCostValue = newCostValue;
							betterEins = einsNeu;
						}
						else{
							localBest = clone.deepClone(shaking);
							setRoundtripsAgain = true;
						}
					}
				}
			}
			else{
				Roundtrip einsNeu = new Roundtrip(eins.getId());
				int indexOfSfEins = eins.getIndexOfJourney(sfEins.getLast());
				int indexOfSfZwei = zwei.getIndexOfJourney(sfZwei.getLast());
				einsNeu.getJourneys().addAll(eins.getFahrtenVonBis(0, indexOfSfEins));
				String deadruntimeId = eins.getAtIndex(indexOfSfEins).getToStopId() + zwei.getAtIndex(indexOfSfZwei).getFromStopId(); 
				einsNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
				einsNeu.getJourneys().addAll(zwei.getFahrtenVonBis(indexOfSfZwei, zwei.getJourneys().size()-1));
				if(einsNeu.isFeasible()){ // zeitliche Sequenzen sind zulaessig
					Roundtrip zweiNeu = new Roundtrip(zwei.getId());
					zweiNeu.getJourneys().addAll(zwei.getFahrtenVonBis(0, zwei.getIndexOfJourney(sfZwei.get(sfZwei.size()-2))));
					deadruntimeId = zwei.getAtIndex(zwei.getIndexOfJourney(sfZwei.get(sfZwei.size()-2))).getToStopId() + zwei.getDepot(); 
					// neue Einrueckfahrt hinzufuegen
					zweiNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
					if(zweiNeu.isFeasible()){
						ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
						if (!eins.getLaden().contains(null)) {
							decreaseFrequencyAt.addAll(eins.getLaden());
						}
						if (!zwei.getLaden().contains(null)) {
							decreaseFrequencyAt.addAll(zwei.getLaden());
						}
						for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
							if (decreaseFrequencyAt.contains(null)) {
								System.err.println("Attention!");
							}
							Stoppoint current = decreaseFrequencyAt.get(i1);
							current.setFrequency(current.getFrequency()-1);
							if(current.getFrequency() == 0){
								current.setLoadingstation(false);
								localBest.getStoppointsWithLoadingStations().remove(current.getId());
								for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
									for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j1).getLaden().size(); j2++) {
										if (localBest.getUmlaufplan().get(j1).getLaden().get(j2) != null){
											if (localBest.getUmlaufplan().get(j1).getLaden().get(j2).getId().equals(current.getId())) {
												localBest.getUmlaufplan().get(j1).getLaden().remove(j2);
											}
										}
									}
								}
								//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
							}
						}

						ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
						ArrayList<ArrayList<Stoppoint>> listZwei = FeasibilityHelper.howIsRoundtourFeasible(zweiNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
						if (listZwei == null || listEins == null) { // einer der Umlaeufe aufgrund der Kapazitaet nicht moeglich
							localBest = clone.deepClone(shaking);
							setRoundtripsAgain = true;
						}
						else{
							for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
								Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
								}
								else{
									x.setFrequency(x.getFrequency()+1);
								}
							}
							einsNeu.setStellen(listEins.get(1));
							einsNeu.setLaden(listEins.get(1));
							for (int i1 = 0; i1 < listZwei.get(1).size(); i1++) {
								Stoppoint x = (Stoppoint) listZwei.get(1).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
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
							else{
								localBest = clone.deepClone(shaking);
								setRoundtripsAgain = true;
							}
						}
					}
			}
			
			}
		}
		
		// versuche Kantentausch mit allen Paaren von inneren Servicefahrten (i nach j)
		for (int i = 0; i < sfEins.size()-1; i++) { 
			for (int j = 1; j < sfZwei.size(); j++) {
				if(setRoundtripsAgain == true){
					for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
						if(localBest.getUmlaufplan().get(j1).getId() == eins.getId()){
							eins = localBest.getUmlaufplan().get(j1); 
						}
						if(localBest.getUmlaufplan().get(j1).getId() == zwei.getId()){
							zwei = localBest.getUmlaufplan().get(j1); 
						}
					}
					sfEins.clear();
					for (int i1 = 0; i1 < eins.getJourneys().size(); i1++) { 
						if(eins.getJourneys().get(i1) instanceof Servicejourney){ // es werden nur Servicefahrten betrachtet
							sfEins.add((Servicejourney) eins.getJourneys().get(i1));
						}
					}
					sfZwei.clear();
					for (int j1 = 0; j1 < zwei.getJourneys().size(); j1++) { 
						if(zwei.getJourneys().get(j1) instanceof Servicejourney){ // es werden nur Servicefahrten betrachtet
							sfZwei.add((Servicejourney) zwei.getJourneys().get(j1));
						}
					}
					setRoundtripsAgain = false;
				}
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

							ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
							if (!eins.getLaden().contains(null)) {
								decreaseFrequencyAt.addAll(eins.getLaden());
							}
							if (!zwei.getLaden().contains(null)) {
								decreaseFrequencyAt.addAll(zwei.getLaden());
							}
							for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
								if (decreaseFrequencyAt.contains(null)) {
									System.err.println("Attention!");
								}
								Stoppoint current = decreaseFrequencyAt.get(i1);
								current.setFrequency(current.getFrequency()-1);
								if(current.getFrequency() == 0){
									current.setLoadingstation(false);
									localBest.getStoppointsWithLoadingStations().remove(current.getId());
									for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
										for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j1).getLaden().size(); j2++) {
											if (localBest.getUmlaufplan().get(j1).getLaden().get(j2) != null){
												if (localBest.getUmlaufplan().get(j1).getLaden().get(j2).getId().equals(current.getId())) {
													localBest.getUmlaufplan().get(j1).getLaden().remove(j2);
												}
											}
										}
									}
									//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
								}
							}

							ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
							if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
								localBest = clone.deepClone(shaking);
								setRoundtripsAgain = true;
								continue;
							}
							ArrayList<ArrayList<Stoppoint>> listZwei = FeasibilityHelper.howIsRoundtourFeasible(zweiNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
							if (listZwei == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
								localBest = clone.deepClone(shaking);
								setRoundtripsAgain = true;
								continue;
							}
							for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
								Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
								}
								else{
									x.setFrequency(x.getFrequency()+1);
								}
							}
							einsNeu.setStellen(listEins.get(1));
							einsNeu.setLaden(listEins.get(1));
							for (int i1 = 0; i1 < listZwei.get(1).size(); i1++) {
								Stoppoint x = (Stoppoint) listZwei.get(1).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
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
							else{
								localBest = clone.deepClone(shaking);
								setRoundtripsAgain = true;
							}
						}	
					}
				}
			}
		}
		
		// Kopie von oben fuer Rueckwaertskanten: versuche erste SF von Zwei mit erster SF von Eins zu vertauschen
		if(FeasibilityHelper.compatibility(sfZwei.getFirst(), sfEins.getFirst(), deadruntimes)){
			if(setRoundtripsAgain == true){
				for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
					if(localBest.getUmlaufplan().get(j1).getId() == eins.getId()){
						eins = localBest.getUmlaufplan().get(j1); 
					}
					if(localBest.getUmlaufplan().get(j1).getId() == zwei.getId()){
						zwei = localBest.getUmlaufplan().get(j1); 
					}
				}
				sfEins.clear();
				for (int i1 = 0; i1 < eins.getJourneys().size(); i1++) { 
					if(eins.getJourneys().get(i1) instanceof Servicejourney){ // es werden nur Servicefahrten betrachtet
						sfEins.add((Servicejourney) eins.getJourneys().get(i1));
					}
				}
				sfZwei.clear();
				for (int j1 = 0; j1 < zwei.getJourneys().size(); j1++) { 
					if(zwei.getJourneys().get(j1) instanceof Servicejourney){ // es werden nur Servicefahrten betrachtet
						sfZwei.add((Servicejourney) zwei.getJourneys().get(j1));
					}
				}
				setRoundtripsAgain = false;
			}
			if(sfZwei.size() == 1){ // es wuerde ein Umlauf wegfallen
				Roundtrip einsNeu = new Roundtrip(eins.getId());
				int indexOfSfEins = eins.getIndexOfJourney(sfEins.getFirst());
				int indexOfSfZwei = zwei.getIndexOfJourney(sfZwei.getFirst());
				einsNeu.getJourneys().addAll(zwei.getFahrtenVonBis(0, indexOfSfZwei));
				String deadruntimeId = zwei.getAtIndex(indexOfSfZwei).getToStopId() + eins.getAtIndex(indexOfSfEins).getFromStopId(); 
				einsNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
				einsNeu.getJourneys().addAll(eins.getFahrtenVonBis(indexOfSfEins, eins.getJourneys().size()-1));
				if(einsNeu.isFeasible()){ // zeitliche Sequenzen sind zulaessig
					ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
					if (!eins.getLaden().contains(null)) {
						decreaseFrequencyAt.addAll(eins.getLaden());
					}
					if (!zwei.getLaden().contains(null)) {
						decreaseFrequencyAt.addAll(zwei.getLaden());
					}
					for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
						if (decreaseFrequencyAt.contains(null)) {
							System.err.println("Attention!");
						}
						Stoppoint current = decreaseFrequencyAt.get(i1);
						current.setFrequency(current.getFrequency()-1);
						if(current.getFrequency() == 0){
							current.setLoadingstation(false);
							localBest.getStoppointsWithLoadingStations().remove(current.getId());
							for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
								for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j1).getLaden().size(); j2++) {
									if (localBest.getUmlaufplan().get(j1).getLaden().get(j2) != null){
										if (localBest.getUmlaufplan().get(j1).getLaden().get(j2).getId().equals(current.getId())) {
											localBest.getUmlaufplan().get(j1).getLaden().remove(j2);
										}
									}
								}
							}
							//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
						}
					}

					ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
					if (listEins == null) { // einer der Umlaeufe aufgrund der Kapazitaet nicht moeglich
						localBest = clone.deepClone(shaking);
						setRoundtripsAgain = true;
					}
					else{
						for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
							Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
							if(x.isLoadingstation() == false){
								x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
								x.setFrequency(1);
								localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
								//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
							}
							else{
								x.setFrequency(x.getFrequency()+1);
							}
						}
						einsNeu.setStellen(listEins.get(1));
						einsNeu.setLaden(listEins.get(1));
						
						// neue Umlaeufe speichern, falls besser
						double newCostValue = einsNeu.getKostenMitLadestationen();
						if(newCostValue < currentCostValue){
							currentCostValue = newCostValue;
							betterEins = einsNeu;
						}
						else{
							localBest = clone.deepClone(shaking);
							setRoundtripsAgain = true;
						}
					}
				}
			}
			else{
				Roundtrip einsNeu = new Roundtrip(eins.getId());
				int indexOfSfEins = eins.getIndexOfJourney(sfEins.getFirst());
				int indexOfSfZwei = zwei.getIndexOfJourney(sfZwei.getFirst());
				einsNeu.getJourneys().addAll(zwei.getFahrtenVonBis(0, indexOfSfZwei));
				String deadruntimeId = zwei.getAtIndex(indexOfSfZwei).getToStopId() + eins.getAtIndex(indexOfSfEins).getFromStopId(); 
				einsNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
				einsNeu.getJourneys().addAll(eins.getFahrtenVonBis(indexOfSfEins, eins.getJourneys().size()-1));
				if(einsNeu.isFeasible()){ // zeitliche Sequenzen sind zulaessig
					Roundtrip zweiNeu = new Roundtrip(zwei.getId());
					deadruntimeId = zwei.getDepot() + zwei.getAtIndex(zwei.getIndexOfJourney(sfZwei.get(1))).getFromStopId(); 
					// neue Ausrueckfahrt hinzufuegen
					zweiNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
					zweiNeu.getJourneys().addAll(zwei.getFahrtenVonBis(zwei.getIndexOfJourney(sfZwei.get(1)), zwei.getJourneys().size()-1));
					if(zweiNeu.isFeasible()){
						ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
						if (!eins.getLaden().contains(null)) {
							decreaseFrequencyAt.addAll(eins.getLaden());
						}
						if (!zwei.getLaden().contains(null)) {
							decreaseFrequencyAt.addAll(zwei.getLaden());
						}
						for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
							if (decreaseFrequencyAt.contains(null)) {
								System.err.println("Attention!");
							}
							Stoppoint current = decreaseFrequencyAt.get(i1);
							current.setFrequency(current.getFrequency()-1);
							if(current.getFrequency() == 0){
								current.setLoadingstation(false);
								localBest.getStoppointsWithLoadingStations().remove(current.getId());
								for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
									for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j1).getLaden().size(); j2++) {
										if (localBest.getUmlaufplan().get(j1).getLaden().get(j2) != null){
											if (localBest.getUmlaufplan().get(j1).getLaden().get(j2).getId().equals(current.getId())) {
												localBest.getUmlaufplan().get(j1).getLaden().remove(j2);
											}
										}
									}
								}
								//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
							}
						}

						ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
						ArrayList<ArrayList<Stoppoint>> listZwei = FeasibilityHelper.howIsRoundtourFeasible(zweiNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
						if (listZwei == null || listEins == null) { // einer der Umlaeufe aufgrund der Kapazitaet nicht moeglich
							localBest = clone.deepClone(shaking);
							setRoundtripsAgain = true;
						}
						else{
							for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
								Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
								}
								else{
									x.setFrequency(x.getFrequency()+1);
								}
							}
							einsNeu.setStellen(listEins.get(1));
							einsNeu.setLaden(listEins.get(1));
							for (int i1 = 0; i1 < listZwei.get(1).size(); i1++) {
								Stoppoint x = (Stoppoint) listZwei.get(1).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
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
							else{
								localBest = clone.deepClone(shaking);
								setRoundtripsAgain = true;
							}
						}
					}
			}
			
			}
		}
		
		//  Kopie von oben fuer Rueckwaertskanten: versuche letzte SF von Zwei mit letzter SF von Eins zu vertauschen
		if(FeasibilityHelper.compatibility(sfZwei.getLast(), sfEins.getLast(), deadruntimes)){
			if(setRoundtripsAgain == true){
				for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
					if(localBest.getUmlaufplan().get(j1).getId() == eins.getId()){
						eins = localBest.getUmlaufplan().get(j1); 
					}
					if(localBest.getUmlaufplan().get(j1).getId() == zwei.getId()){
						zwei = localBest.getUmlaufplan().get(j1); 
					}
				}
				sfEins.clear();
				for (int i1 = 0; i1 < eins.getJourneys().size(); i1++) { 
					if(eins.getJourneys().get(i1) instanceof Servicejourney){ // es werden nur Servicefahrten betrachtet
						sfEins.add((Servicejourney) eins.getJourneys().get(i1));
					}
				}
				sfZwei.clear();
				for (int j1 = 0; j1 < zwei.getJourneys().size(); j1++) { 
					if(zwei.getJourneys().get(j1) instanceof Servicejourney){ // es werden nur Servicefahrten betrachtet
						sfZwei.add((Servicejourney) zwei.getJourneys().get(j1));
					}
				}
				setRoundtripsAgain = false;
			}
			if(sfEins.size() == 1){ // in diesem Fall wuerde ein Umlauf wegfallen
				Roundtrip einsNeu = new Roundtrip(eins.getId());
				int indexOfSfEins = eins.getIndexOfJourney(sfEins.getLast());
				int indexOfSfZwei = zwei.getIndexOfJourney(sfZwei.getLast());
				einsNeu.getJourneys().addAll(zwei.getFahrtenVonBis(0, indexOfSfZwei));
				String deadruntimeId = zwei.getAtIndex(indexOfSfZwei).getToStopId() + eins.getAtIndex(indexOfSfEins).getFromStopId(); 
				einsNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
				einsNeu.getJourneys().addAll(eins.getFahrtenVonBis(indexOfSfEins, eins.getJourneys().size()-1));
				if(einsNeu.isFeasible()){ // zeitliche Sequenzen sind zulaessig
					ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
					if (!eins.getLaden().contains(null)) {
						decreaseFrequencyAt.addAll(eins.getLaden());
					}
					if (!zwei.getLaden().contains(null)) {
						decreaseFrequencyAt.addAll(zwei.getLaden());
					}
					for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
						if (decreaseFrequencyAt.contains(null)) {
							System.err.println("Attention!");
						}
						Stoppoint current = decreaseFrequencyAt.get(i1);
						current.setFrequency(current.getFrequency()-1);
						if(current.getFrequency() == 0){
							current.setLoadingstation(false);
							localBest.getStoppointsWithLoadingStations().remove(current.getId());
							for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
								for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j1).getLaden().size(); j2++) {
									if (localBest.getUmlaufplan().get(j1).getLaden().get(j2) != null){
										if (localBest.getUmlaufplan().get(j1).getLaden().get(j2).getId().equals(current.getId())) {
											localBest.getUmlaufplan().get(j1).getLaden().remove(j2);
										}
									}
								}
							}
							//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
						}
					}

					ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
					if (listEins == null) { // einer der Umlaeufe aufgrund der Kapazitaet nicht moeglich
						localBest = clone.deepClone(shaking);
						setRoundtripsAgain = true;
					}
					else{
						for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
							Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
							if(x.isLoadingstation() == false){
								x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
								x.setFrequency(1);
								localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
								//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
							}
							else{
								x.setFrequency(x.getFrequency()+1);
							}
						}
						einsNeu.setStellen(listEins.get(1));
						einsNeu.setLaden(listEins.get(1));
						
						// neue Umlaeufe speichern, falls besser
						double newCostValue = einsNeu.getKostenMitLadestationen();
						if(newCostValue < currentCostValue){
							currentCostValue = newCostValue;
							betterEins = einsNeu;
						}
						else{
							localBest = clone.deepClone(shaking);
							setRoundtripsAgain = true;
						}
					}
				}
			}
			else{
				Roundtrip einsNeu = new Roundtrip(eins.getId());
				int indexOfSfEins = eins.getIndexOfJourney(sfEins.getLast());
				int indexOfSfZwei = zwei.getIndexOfJourney(sfZwei.getLast());
				einsNeu.getJourneys().addAll(zwei.getFahrtenVonBis(0, indexOfSfZwei));
				String deadruntimeId = zwei.getAtIndex(indexOfSfZwei).getToStopId() + eins.getAtIndex(indexOfSfEins).getFromStopId(); 
				einsNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
				einsNeu.getJourneys().addAll(eins.getFahrtenVonBis(indexOfSfEins, eins.getJourneys().size()-1));
				if(einsNeu.isFeasible()){ // zeitliche Sequenzen sind zulaessig
					Roundtrip zweiNeu = new Roundtrip(zwei.getId());
					zweiNeu.getJourneys().addAll(eins.getFahrtenVonBis(0, eins.getIndexOfJourney(sfEins.get(sfEins.size()-2))));
					deadruntimeId = eins.getAtIndex(eins.getIndexOfJourney(sfEins.get(sfEins.size()-2))).getToStopId() + eins.getDepot(); 
					// neue Einrueckfahrt hinzufuegen
					zweiNeu.getJourneys().add(deadruntimes.get(deadruntimeId));
					if(zweiNeu.isFeasible()){
						ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
						if (!eins.getLaden().contains(null)) {
							decreaseFrequencyAt.addAll(eins.getLaden());
						}
						if (!zwei.getLaden().contains(null)) {
							decreaseFrequencyAt.addAll(zwei.getLaden());
						}
						for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
							if (decreaseFrequencyAt.contains(null)) {
								System.err.println("Attention!");
							}
							Stoppoint current = decreaseFrequencyAt.get(i1);
							current.setFrequency(current.getFrequency()-1);
							if(current.getFrequency() == 0){
								current.setLoadingstation(false);
								localBest.getStoppointsWithLoadingStations().remove(current.getId());
								for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
									for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j1).getLaden().size(); j2++) {
										if (localBest.getUmlaufplan().get(j1).getLaden().get(j2) != null){
											if (localBest.getUmlaufplan().get(j1).getLaden().get(j2).getId().equals(current.getId())) {
												localBest.getUmlaufplan().get(j1).getLaden().remove(j2);
											}
										}
									}
								}
								//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
							}
						}

						ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
						ArrayList<ArrayList<Stoppoint>> listZwei = FeasibilityHelper.howIsRoundtourFeasible(zweiNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
						if (listZwei == null || listEins == null) { // einer der Umlaeufe aufgrund der Kapazitaet nicht moeglich
							localBest = clone.deepClone(shaking);
							setRoundtripsAgain = true;
						}
						else{
							for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
								Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
								}
								else{
									x.setFrequency(x.getFrequency()+1);
								}
							}
							einsNeu.setStellen(listEins.get(1));
							einsNeu.setLaden(listEins.get(1));
							for (int i1 = 0; i1 < listZwei.get(1).size(); i1++) {
								Stoppoint x = (Stoppoint) listZwei.get(1).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
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
							else{
								localBest = clone.deepClone(shaking);
								setRoundtripsAgain = true;
							}
						}
					}
			}
			}
		}
		// Kopie von oben für die Betrachtung aller Rueckwartskanten (j nach i)
		for (int i = 1; i < sfEins.size()-1; i++) { 
			for (int j = 1; j < sfZwei.size()-1; j++) {
				if(setRoundtripsAgain == true){
					for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
						if(localBest.getUmlaufplan().get(j1).getId() == eins.getId()){
							eins = localBest.getUmlaufplan().get(j1); 
						}
						if(localBest.getUmlaufplan().get(j1).getId() == zwei.getId()){
							zwei = localBest.getUmlaufplan().get(j1); 
						}
					}
					sfEins.clear();
					for (int i1 = 0; i1 < eins.getJourneys().size(); i1++) { 
						if(eins.getJourneys().get(i1) instanceof Servicejourney){ // es werden nur Servicefahrten betrachtet
							sfEins.add((Servicejourney) eins.getJourneys().get(i1));
						}
					}
					sfZwei.clear();
					for (int j1 = 0; j1 < zwei.getJourneys().size(); j1++) { 
						if(zwei.getJourneys().get(j1) instanceof Servicejourney){ // es werden nur Servicefahrten betrachtet
							sfZwei.add((Servicejourney) zwei.getJourneys().get(j1));
						}
					}
					setRoundtripsAgain = false;
				}
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

							ArrayList<Stoppoint> decreaseFrequencyAt = new ArrayList<Stoppoint>();
							if (!eins.getLaden().contains(null)) {
								decreaseFrequencyAt.addAll(eins.getLaden());
							}
							if (!zwei.getLaden().contains(null)) {
								decreaseFrequencyAt.addAll(zwei.getLaden());
							}
							for (int i1 = 0; i1 < decreaseFrequencyAt.size(); i1++) {
								if (decreaseFrequencyAt.contains(null)) {
									System.err.println("Attention!");
								}
								Stoppoint current = decreaseFrequencyAt.get(i1);
								current.setFrequency(current.getFrequency()-1);
								if(current.getFrequency() == 0){
									current.setLoadingstation(false);
									localBest.getStoppointsWithLoadingStations().remove(current.getId());
									for (int j1 = 0; j1 < localBest.getUmlaufplan().size(); j1++) {
										for (int j2 = 0; j2 < localBest.getUmlaufplan().get(j1).getLaden().size(); j2++) {
											if (localBest.getUmlaufplan().get(j1).getLaden().get(j2) != null){
												if (localBest.getUmlaufplan().get(j1).getLaden().get(j2).getId().equals(current.getId())) {
													localBest.getUmlaufplan().get(j1).getLaden().remove(j2);
												}
											}
										}
									}
									//System.out.println("An Haltestelle " + current.getId() + " wurde ein Ladestation entfernt.");
								}
							}

							ArrayList<ArrayList<Stoppoint>> listEins = FeasibilityHelper.howIsRoundtourFeasible(einsNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
							if (listEins == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
								localBest = clone.deepClone(shaking);
								setRoundtripsAgain = true;
								continue;
							}
							ArrayList<ArrayList<Stoppoint>> listZwei = FeasibilityHelper.howIsRoundtourFeasible(zweiNeu.getJourneys(), localBest.getStoppoints(), deadruntimes, servicejourneys);
							if (listZwei == null) { // Umlauf aufgrund der Kapazitaet nicht moeglich
								localBest = clone.deepClone(shaking);
								setRoundtripsAgain = true;
								continue;
							}
							for (int i1 = 0; i1 < listEins.get(1).size(); i1++) {
								Stoppoint x = (Stoppoint) listEins.get(1).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
								}
								else{
									x.setFrequency(x.getFrequency()+1);
								}
							}
							einsNeu.setStellen(listEins.get(1));
							einsNeu.setLaden(listEins.get(1));
							for (int i1 = 0; i1 < listZwei.get(1).size(); i1++) {
								Stoppoint x = (Stoppoint) listZwei.get(1).get(i1);
								if(x.isLoadingstation() == false){
									x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
									x.setFrequency(1);
									localBest.getStoppointsWithLoadingStations().put(x.getId(), x);
									//System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
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
							else{
								localBest = clone.deepClone(shaking);
								setRoundtripsAgain = true;
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
			result.setAltEins(localBest.getUmlaufplan().get(index1));
			result.setAltZwei(localBest.getUmlaufplan().get(index2));
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

	private void assignBestDepot(Roundtrip trip, String bestDepot) {
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
}
