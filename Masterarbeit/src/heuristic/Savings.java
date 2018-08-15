/**
 * 
 */
package heuristic;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import helper.FeasibilityHelper;
import java.util.Map.Entry;
import model.Deadruntime;
import model.Depot;
import model.Journey;
import model.Roundtrip;
import model.Schedule;
import model.Servicejourney;
import model.Stoppoint;
import parser.ReadReassessedData;

/**
 * @author nicolasmaeke
 *
 */
public class Savings {

	private ArrayList<Roundtrip> roundtrips = new ArrayList<Roundtrip>();
	private HashMap<String, Stoppoint> stoppoints;
	private HashMap<String, Deadruntime> deadruntimes;
	private HashMap<String, Servicejourney> servicejourneys;
	private List <Depot> depots;
	private Schedule umlaufplan;

	public Savings(ReadReassessedData data){
		this.servicejourneys = data.getServicejourneys();
		this.deadruntimes = data.getDeadruntimes();
		this.stoppoints = data.getStoppoints();
		this.depots = data.getDepots();
		this.umlaufplan  = new Schedule(roundtrips, servicejourneys, depots, stoppoints);
	}

	public void startSavings(){
		createPendeltours();
		double valueSaving = 0.0;
		//HashMap <String, Double> savingsMatrix = initialSavingsMatrix();
		HashMap <String, Double> savingsMatrix = new HashMap<String, Double>();
		int iteration = 0;
		
		do {
			savingsMatrix = neuerUmlaufplan(savingsMatrix);
			valueSaving = 0.0;
			for (Entry<String, Double> e: savingsMatrix.entrySet()){ 
				if(e.getValue() > valueSaving){
					valueSaving = e.getValue();
				}
			}
			iteration ++;
			System.out.println(iteration);
			umlaufplan.setUmlaufplan(roundtrips);
			if(!umlaufplan.isFeasible()){
				System.err.println("Attention, not feasible!");
			}
			printInitialSolution();
			// Terminierungskriterium: Die Savings-Matrix ist leer oder es sind keine positiven Savings mehr vorhanden
		}while(!savingsMatrix.isEmpty() && !(valueSaving <= 0)) ;
		umlaufplan.setNumberOfLoadingStations(umlaufplan.getStoppointsWithLoadingStations().size());
		umlaufplan.calculateCosts();
		umlaufplan.printUmlaufplan();
	}

	private HashMap<String, Double> savingsMatrix(HashMap<String, Double> savingsMatrix) {
		// Key: IDs der beiden Servicefahrten, die zusammengelegt werden sollen
		// Value: Savings, falls die beiden Servicefahrten zusammengelegt werden
		HashMap <String, Double> savings = new HashMap <String, Double>();
		Servicejourney sfLast = null;
		Servicejourney sfFirst = null;
		Deadruntime newDeadrun = null; // die Leerfahrt, die in den Fahrzeugumlauf hinzugefuegt werden muss, um die beiden Servicefahrten zu verbinden

		//  die doppelte for-Schleife ueberprueft fuer alle Paare von unterschiedlichen Roundtrips, ob diese zusammengelegt werden koennen
		for (int i = 0; i < roundtrips.size(); i++) {
			Vector<Servicejourney> listOfSf1 = new Vector<Servicejourney>();
			for (int x = 0; x < roundtrips.get(i).getJourneys().size(); x++) {
				if(roundtrips.get(i).getJourneys().get(x) instanceof Servicejourney){
					listOfSf1.add((Servicejourney) roundtrips.get(i).getJourneys().get(x));
				}
			}
			sfLast = listOfSf1.lastElement(); // letzte SF von Roundtrip i
			for (int j = 0; j < roundtrips.size(); j++) {
				Vector<Servicejourney> listOfSf2 = new Vector<Servicejourney>();
				for (int y = 0; y < roundtrips.get(j).getJourneys().size(); y++) {
					if(roundtrips.get(j).getJourneys().get(y) instanceof Servicejourney){
						listOfSf2.add((Servicejourney) roundtrips.get(j).getJourneys().get(y));
					}
				}
				sfFirst = listOfSf2.firstElement(); // erste SF von Roundtrip j
				if(i != j){ // handelt es sich um unterschieldiche Roundtrips?
					if (savingsMatrix.containsKey(""+ sfLast.getId() + sfFirst.getId())) {
						// LF zw. der Endhaltestelle der letzten SF von Roundtrip j und der Starthaltestelle der ersten SF von Roundtrip i
						newDeadrun = deadruntimes.get("" + sfLast.getToStopId() + sfFirst.getFromStopId());
						savings.put(""+sfLast.getId()+sfFirst.getId(), calculateSavings(roundtrips.get(i), sfLast, roundtrips.get(j), sfFirst, newDeadrun)); 
					}
				}
			}
		}
		return savings;
	}


	private ArrayList<Roundtrip> createPendeltours(){

		int counter = 1; // Zaehler fuer die ID der Umlaeufe (wird in jedem Schleifendurchlauf inkrementiert)

		for (Entry<String, Servicejourney> i: servicejourneys.entrySet()){ //fuer jede Servicefahrt i
			Roundtrip pendelTour = new Roundtrip(counter); //erstelle einen neuen Fahrzeugumlauf mit dieser Servicefahrt

			Servicejourney firstSf = servicejourneys.get(i.getKey()); 
			Depot depot = findNearestDepot(firstSf);

			String key = depot.getId() + firstSf.getFromStopId(); // key ist die ID des Depots + die ID der Starthaltestelle von Servicefahrt i 

			pendelTour.addFahrtAfterFahrt(0, deadruntimes.get(key)); // die erste Fahrt ist eine Leerfahrt

			pendelTour.addFahrtAfterFahrt(1, firstSf); // dann folgt die Servicefahrt

			key = firstSf.getToStopId() + depot.getId(); // key ist die ID der Endhaltestelle von Servicefahrt i + dir ID des Depots

			pendelTour.addFahrtAfterFahrt(pendelTour.getJourneys().size(), deadruntimes.get(key));

			boolean feasibility = true;
			double verbrauch = 0;
			for (int j = 0; j < pendelTour.getJourneys().size(); j++) {
				verbrauch = verbrauch + pendelTour.getJourneys().get(j).getEnergyConsumption();
			}
			if(verbrauch > Roundtrip.getCapacity()){
				feasibility = false;
			}
			if (!feasibility) {
				System.out.println("Kapazitaet reicht nicht fuer Pendeltouren aus. Deswegen Ladestation bauen!");
				for (Map.Entry<String, Stoppoint> e: stoppoints.entrySet()){
					Stoppoint i1 = stoppoints.get(e.getKey());
					if (i1.getId().equals(firstSf.getToStopId())){
						i1.setLoadingstation(true);
						i1.setFrequency(1);
					}
				}
			}
			roundtrips.add(pendelTour); // fuege den Fahrzeugumlauf j zu der Gesamtliste 
			counter ++;
		}
		return roundtrips; // Gesamtliste von Fahrzeugumlaeufen zurueckgeben
	}

	/**
	 * 
	 * @param s Servicefahrt, zu der das naechstgelegene Depot gesucht wird
	 * @return das naechstgelegene Depot zur Servicefahrt s
	 */
	private Depot findNearestDepot(Servicejourney s){
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

	public ArrayList<Roundtrip> getRoundtrips() {
		return roundtrips;
	}

	public void setRoundtrips(ArrayList<Roundtrip> roundtrips) {
		this.roundtrips = roundtrips;
	}

	/**
	 * Methode erstellt eine Saving-Matrix als HashMap. 
	 * Darin werden die Einsparpotenziale gespeichert, fuer den Fall, 
	 * dass zwei Servicefahrten in einem Fahrzeugumlauf bedient werden koennen.
	 * @param deadruntimes Die Liste aller Leerfahrten
	 * @return eine Hashmap mit den Einsparpotenzialen, falls zwei Servicefahrten hintereinadner von einem Fahrzeug bedient werden koennen
	 */
	private HashMap<String, Double> initialSavingsMatrix(){
		// Key: IDs der beiden Servicefahrten, die zusammengelegt werden sollen
		// Value: Savings, falls die beiden Servicefahrten zusammengelegt werden
		HashMap <String, Double> savings = new HashMap<String, Double>();
		Servicejourney sfLast = null;
		Servicejourney sfFirst = null;
		Deadruntime newDeadrun = null; // die Leerfahrt, die in den Fahrzeugumlauf hinzugefuegt werden muss, um die beiden Servicefahrten zu verbinden

		int size = roundtrips.size();
		String key = "";

		//  die doppelte for-Schleife ueberprueft fuer alle Paare von unterschiedlichen Roundtrips, ob diese zusammengelegt werden koennen
		for (int i = 0; i < size; i++) {
			key = "";
			sfLast = (Servicejourney)roundtrips.get(i).getJourneys().get(roundtrips.get(i).getJourneys().size()-2); // letzte SF von Roundtrip i
			for (int j = 0; j < size; j++) {
				key = "";
				sfFirst = (Servicejourney)roundtrips.get(j).getJourneys().get(1); // erste SF von Roundtrip j
				key = key + sfLast.getId()+sfFirst.getId();
				if(i != j){ // handelt es sich um unterschieldiche Roundtrips?
					if(FeasibilityHelper.compatibility(sfLast, sfFirst, deadruntimes)){
						// LF zw. der Endhaltestelle der letzten SF von Roundtrip i und der Starthaltestelle der ersten SF von Roundtrip j
						newDeadrun = deadruntimes.get("" + sfLast.getToStopId() + sfFirst.getFromStopId());
						savings.put(key, calculateSavings(roundtrips.get(i), sfLast, roundtrips.get(j), sfFirst, newDeadrun)); 
					}
				}
			}
		}
		return savings;
	}

	/**
	 * Methode zum Erstellen eines neuen Umlaufplans, indem ein Umlauf veraendert wird und ein anderen entfernt wird
	 * 
	 * @param savings
	 * @param deadruntimes
	 * @param stoppoints
	 * @param servicejourneys
	 * @return eine veraenderte Savings Matrix
	 */
	private HashMap <String, Double> neuerUmlaufplan(HashMap <String, Double> savingsMatrixOld){
		HashMap <String, Double> currenntSavingsMatrix = null;
		if (savingsMatrixOld.isEmpty()) {
			currenntSavingsMatrix = initialSavingsMatrix();
		}
		else{
			currenntSavingsMatrix = savingsMatrix(savingsMatrixOld);
		}
		String currentKey = null; // Schluessel des aktuell behandelten Savings
		List<String> keys = new ArrayList<String>(); // Schluessel aller savings, die schon betrachtet wurden
		LinkedList<Journey> currentNew = null; //  Container fuer den neu zusammengelegten Umlauf
		//HashMap <String, ArrayList<Stoppoint>> numberOfnewLoadingstations = null; // Container fuer die Haltestellen, an denen Ladestationen gebaut werden muessen, um die Savings realisiern zu koennen
		ArrayList<ArrayList<Stoppoint>> list = new ArrayList<ArrayList<Stoppoint>>();
		if(currenntSavingsMatrix.isEmpty()){ 
			return currenntSavingsMatrix; // in diesem Fall (es gibt keine Savings) werden die Pendeltouren zurueckgegeben
		}
		do {
			currentKey = getHighestSaving(currenntSavingsMatrix); 
			if(currenntSavingsMatrix.get(currentKey) <= 0.0){ // falls der groesste Saving ≤ 0
				return currenntSavingsMatrix; // hoert auf & aktuelle Fahrzeugumlaeufe zurueckgeben
			}
			if(!keys.contains(currentKey)){ // falls diese Savings noch nicht betrachtet worden sind
				do{
					currentNew = umlaeufeZusammenlegen(currentKey); // lege die beiden Umlaeufe mit den groessten Savings zusammen
					if(currentNew == null){
						currenntSavingsMatrix.remove(currentKey);
					}
				} while (currentNew == null);
				//numberOfnewLoadingstations = newLoadingstations(currentNew, currentKey); 
				if(currentKey.equals("0294912101114121")){
					System.out.println();
				}
				list = FeasibilityHelper.newLoadingstations(currentNew, stoppoints, deadruntimes, servicejourneys);

				if (list == null){ // falls der Umlauf nicht moeglich ist
					currenntSavingsMatrix.remove(currentKey); // aktuell betrachtete Savings werden auf 0 gesetzt
				}else if(list.get(0).isEmpty()){ // der neue Umlauf ist ohne den Bau von Ladestationen moeglich
					continue;
				}else{ // falls Ladestationen gebaut werden koennen
					int kosten = list.get(0).size() * 250000; // berechne Fixkosten von Ladestationen
					double neueSavings = currenntSavingsMatrix.get(currentKey) - kosten; // aktualisiere Saving von currentKey
					currenntSavingsMatrix.replace(currentKey, neueSavings); // aktualisiere die Savings Matrix
				}
				keys.add(currentKey); // currentKey wird als betrachtet gespeichert 
			}
			else {
				currentNew = umlaeufeZusammenlegen(currentKey);
				list = FeasibilityHelper.newLoadingstations(currentNew, stoppoints, deadruntimes, servicejourneys);
				}
		} while (currentKey != getHighestSaving(currenntSavingsMatrix)); // solange die Savings Matrix veraendert wird bzw. die vormal groessten Savings auf Grund der Veranedrung nicht mehr die groessten Savings sind

		if(currentKey.equals("0294912101114121")){
			System.out.println();
		}
		if(!umlaufplan.isFeasible()){
			System.err.println("Fehler: Umlaufplan nicht möglich!");
			System.out.println(currentKey);
		}

		for (int i = 0; i < list.get(0).size(); i++) {
			Stoppoint x = (Stoppoint) list.get(0).get(i);
			if(x.isLoadingstation() == false){
				x.setLoadingstation(true); // Setzen der Ladestationen an den betroffenen Haltestellen
				umlaufplan.getStoppointsWithLoadingStations().put(x.getId(), x);
				System.out.println("An Haltestelle " + x.getId() + " wurde ein Ladestation gebaut.");
			}
		}
		
		/**
		 * Fahrzeugumlaeufe aktualisieren:
		 * Der zusammengelegte Umlauf ueberschreibt den ersten der beiden alten Umlaeufe, aus der er besteht.
		 * Der zweite wird aus der Liste der Umlaeufe geloescht.
		 */
		ArrayList<Integer> umlaeufe = umlaeufeFinden(currentKey);
		for (int i = 0; i < roundtrips.size(); i++) {
			if (roundtrips.get(i).getId() == umlaeufe.get(0)) {
				roundtrips.get(i).setJourneys(currentNew);
				roundtrips.get(i).setStellen(list.get(1));
				roundtrips.get(i).setLaden(list.get(1));
			break;
			}
		}

		for (int j = 0; j < roundtrips.size(); j++) {
			if (roundtrips.get(j).getId() == umlaeufe.get(1)) {
				roundtrips.remove(j);
			}
		}

		if(currentKey.equals("0294912101114121")){
			System.out.println();
		}
		if(!umlaufplan.isFeasible()){
			System.err.println("Fehler: Umlaufplan nicht möglich!");
			System.out.println(currentKey);
		}
		
		currenntSavingsMatrix.remove(currentKey);
		return currenntSavingsMatrix; 
	}

	/**
	 * Methode zum Berechnen der Savings, falls zwei Servicefahrten in einem Fahrzeugumlauf bedient werden.
	 * 
	 * @param i Fahrzeugumlauf i
	 * @param j Fahrzeugumlauf j
	 * @param deadrun Leerfahrt, die die beiden Fahrzeugumlaeufe verbindet
	 * @return Die Einsparungen, falls die Fahrzeugumlaeufe zusammengelegt werden koennen
	 */
	private double calculateSavings(Roundtrip one, Servicejourney last, Roundtrip two, Servicejourney first, Deadruntime deadrun) {
		double saving = 0;
		double zeitpuffer = FeasibilityHelper.zeitpufferZwischenServicefahrten(last, first, deadrun);
		double relZeitpuffer = zeitpuffer / 1000;
		double d1 = one.getJourneys().getLast().getDistance(); // Distanz zwischen letzter Servicefahrt und Depot
		double d2 = two.getJourneys().getFirst().getDistance(); // Distanz zwischen Depot und erster Servicefahrt
		// Savings: eingesparte Fahrten (d1,d2) minus zusaetzliche Fahrt (deadrun) plus Einsparung eines Fahrzeugs (400.000)
		// zeitpuffer/1000 sind Strafkosten fuer die Wartezeit zw. den beiden SF (kurze Wartezeit soll belohnt werden)
		saving = d1 + d2 - deadrun.getDistance() + 400000 - relZeitpuffer; // zeitpuffer/1000 sind Strafkosten dafuer, dass zwei sehr weit auseinader liegende SF zusammengelegt werdern
		return saving;
	}

	/**
	 * Methode zum Zusammenlegen von Fahrzeugumlaeufen
	 * @param key Schluessel vom groessten Saving
	 * @return zusammengelegter Umlauf als Liste von Fahrten (Depot - SF1 - SF2 - Depot)
	 */	
	private LinkedList<Journey> umlaeufeZusammenlegen(String key){

		LinkedList<Journey> neu = null; // Container fuer den neu zu erstellenden Umlauf

		
		ArrayList<Integer> umlaeufe = umlaeufeFinden(key); // Die beiden Umlaeufe, die zusammengelegt werden sollen

		LinkedList<Journey> eins = new LinkedList<Journey>();
		for (int i = 0; i < roundtrips.size(); i++) {
			if(roundtrips.get(i).getId() == umlaeufe.get(0)){
				eins.addAll(roundtrips.get(i).getJourneys());
				break;
			}
		}
		LinkedList<Journey> zwei = new LinkedList<Journey>(); 
		for (int j = 0; j < roundtrips.size(); j++) {
			if(roundtrips.get(j).getId() == umlaeufe.get(1)){
				zwei.addAll(roundtrips.get(j).getJourneys());
				break;
			}
		}
		
		eins.removeLast(); // loesche letzte Fahrt von eins (Einrueckfahrt)
		zwei.removeFirst(); // loesche erste Fahrt von zwei (Ausrueckfahrt)
		if(zwei.getFirst() instanceof Deadruntime){ // zwei hatte zu Beginn einen Umweg
			zwei.removeFirst(); // damit nicht drei Leerfahrten aufeinander folgen können
		}
		neu = eins;
		if(neu.getLast() instanceof Deadruntime){
			neu.removeLast();
		}
		neu.add(deadruntimes.get(eins.getLast().getToStopId()+zwei.getFirst().getFromStopId())); // neu entstehende Leerfahrt zwischen eins und zwei
		neu.addAll(zwei);

		ArrayList<Servicejourney> listOfSf = new ArrayList<Servicejourney>();
		for (int i = 0; i < neu.size(); i++) {
			if(neu.get(i) instanceof Servicejourney){
				listOfSf.add((Servicejourney) neu.get(i));
			}	
		}

		Servicejourney first = listOfSf.get(0);
		Servicejourney last = listOfSf.get(listOfSf.size()-1);

		Stoppoint depotOne = stoppoints.get(neu.get(0).getFromStopId());
		Stoppoint depotTwo = stoppoints.get(neu.get(neu.size()-1).getToStopId());

		if(!neu.get(0).getFromStopId().equals(neu.get(neu.size()-1).getToStopId())){ // falls zwei Umlaeufe mit unterschiedlichen Depots zusammengelegt werden sollen
			double distanceDepotOne = deadruntimes.get("" + depotOne.getId() + first.getFromStopId()).getDistance() + deadruntimes.get("" + last.getToStopId() + depotOne.getId()).getDistance(); // Summe der Ein- und Ausrueckfahrt zum ersten Depot
			double distanceDepotTwo = deadruntimes.get("" + depotTwo.getId() + first.getFromStopId()).getDistance() + deadruntimes.get("" + last.getToStopId() + depotTwo.getId()).getDistance(); // Summe der Ein- und Ausrueckfahrt zum zweiten Depot
			if(distanceDepotOne <= distanceDepotTwo){ // das erste Depot ist besser
				neu.removeLast(); // letzte LF aendern
				neu.add(deadruntimes.get("" + last.getToStopId() + depotOne.getId()));
			}
			else{ // das zweite Depot ist besser
				neu.removeFirst();
				neu.addFirst(deadruntimes.get("" + depotTwo.getId() + first.getFromStopId()));
			}
		}
		return neu; // der zusammengelegte Umlauf
	}

	private ArrayList<Integer> umlaeufeFinden(String key) {
		int n = key.length()/2;
		String key1 = key.substring(0, n); //erster Umlauf von diesem Schluessel
		String key2 = key.substring(n, key.length()); //zweiter Umlauf von diesem Schluessel
		Roundtrip eins = null;
		Roundtrip zwei = null;
		ArrayList<Integer> umlaeufe = new ArrayList<Integer>(); // eine ArrayList der Indizes der Fahrzeugumlaeufe

		for (int i = 0; i < roundtrips.size(); i++) { // fuer jeden Fahrzeugumlauf i
			for (int j = 0; j < roundtrips.get(i).getJourneys().size(); j++) { // fuer jede Fahrt j im Fahrzeugumlauf i 
				if(roundtrips.get(i).getJourneys().get(j).getId().equals(key1)){  
					//umlaeufe.add(i);
					eins = roundtrips.get(i); // erster Umlauf beinhaltet key1
					break; // wenn die SF in diesem Umlauf gefunden wurde, innere for-Schleife beenden, weil die andere SF nicht im gleichen Umlauf sein kann
				}
				if(roundtrips.get(i).getJourneys().get(j).getId().equals(key2)){
					//umlaeufe.add(i);
					zwei = roundtrips.get(i); // zweiter Umlauf beinhaltet key2
					break; // wenn die SF in diesem Umlauf gefunden wurde, innere for-Schleife beenden, weil die andere SF nicht im gleichen Umlauf sein kann
				}
			}
			// wenn beide Umlaeufe gefunden wurden, beende die aeussere for-Schleife
			if(eins != null && zwei != null){
				break;
			}
		}
		umlaeufe.add(eins.getId());
		umlaeufe.add(zwei.getId());
		return umlaeufe;
	}

	/** 
	 * Methode zum Finden des Schluessels vom groessten Saving
	 * @param savings
	 * @return Schluessel vom groessten Saving als String
	 */
	private String getHighestSaving(HashMap<String, Double> savings){
		double temp = -1000000;
		String key = "";
		for (Entry<String, Double> e: savings.entrySet()){ 
			if(e.getValue() > temp){
				temp = e.getValue();
				key = e.getKey();
			}
		}
		return key;
	}

	

	public void printInitialSolution(){
		for (int i = 0; i < roundtrips.size(); i++) {
			System.out.println(roundtrips.get(i).printRoundtrip());
		}
		System.out.println("In der Initialloesung gibt es " + roundtrips.size() + " Fahrzeugumlaeufe.");
	}
	
	public void writeInitialSolution(String path){
		FileWriter fw = null;
		BufferedWriter bw = null;
		PrintWriter pw = null;
		
		try {
			// waehle Zielpfad und Name der Ergebnis-Datei aus
			fw = new FileWriter(path, true);
		} catch (IOException e1) {
			e1.printStackTrace();
		} 
		bw = new BufferedWriter(fw); 
		pw = new PrintWriter(bw);
		

		/**
		 * Erzeuge in der Datei eine neue Relation fuer die Depots
		 */
		pw.println("$DEPOT:ID;capacity");
		
		for (int i = 0; i < depots.size(); i++) {
			String id = depots.get(i).getId();
			int capacity = 0;
			for (int j = 0; j < roundtrips.size(); j++) {
				if(roundtrips.get(j).getJourneys().getFirst().getFromStopId().equals(id)){
					capacity ++;
				}
			}
			pw.println(id + ";" + capacity);
			pw.flush();
		}

		/**
		 * Erzeuge in der Datei eine neue Relation fuer die Haltestellen
		 */
		pw.println("$INITIALSTOPPOINT:ID;isLoadingstation;frequency");
		
		/**
		 * Durchlaufe alle Haltestellen und pruefe, ob eine Ladestation gebaut wurde
		 * und wie die Frequentierung an dieser Ladestation ist.
		 * Erzeuge daraus String, um sie in die Datei zu schreiben.
		 */
		for(Entry <String, Stoppoint> e: stoppoints.entrySet()){
			Stoppoint i1 = stoppoints.get(e.getKey());
			String stoppointId = i1.getId();
			String isLoadingstation;
			int counter = 0;
			String frequency = "0";
			if (i1.isLoadingstation()) {
				isLoadingstation = "true";
				for (int i = 0; i < umlaufplan.getUmlaufplan().size(); i++) {
					for (int j = 0; j < umlaufplan.getUmlaufplan().get(i).getLaden().size(); j++) {
						if(umlaufplan.getUmlaufplan().get(i).getLaden().get(j).getId().equals(i1.getId())){
							counter ++;
							i1.setFrequency(i1.getFrequency() + 1);
						}
					}
				}
				if(i1.getFrequency() == 0){ // Ladestation wird nicht mehr gebraucht
					i1.setLoadingstation(false);
					isLoadingstation = "false";
				}
			}
			else{
				isLoadingstation = "false";
			}
			frequency = "" + counter + "";
			pw.println(stoppointId + ";" + isLoadingstation + ";" + frequency);
			pw.flush();
		}
		
		/**
		 * Erzeuge in der Datei eine neue Relation für den Umlaufplan der Initialloesung
		 */
		pw.println("$Umlauf:ID;Fahrten;;;;;;;;");
		
		for (int j = 0; j < umlaufplan.getUmlaufplan().size(); j++) {
			String umlaufId = String.valueOf(j);
			pw.println(umlaufId + ";" + umlaufplan.getUmlaufplan().get(j).toStringIds() + ";" + umlaufplan.getUmlaufplan().get(j).getLadenString());
			pw.flush();
		}
	}
	
}
