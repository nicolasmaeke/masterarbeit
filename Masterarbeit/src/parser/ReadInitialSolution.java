package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import model.Deadruntime;
import model.Depot;
import model.Roundtrip;
import model.Schedule;
import model.Servicejourney;
import model.Stoppoint;

public class ReadInitialSolution {

	public File f;
	public String output;
	public String input;
	Date start;
	Date ende;
	public double durchschnittsdistance;
	public double durchschnittsruntime;
	public double maxloading;

	// 1.1 Variablen fuer Streckennetz und Ladeinfrastruktur erstellen

	// 1.1.1 Stoppoints
	public HashMap<String, Stoppoint> stoppoints = new HashMap<String, Stoppoint>();

	// 1.1.2 Servicefahrten
	public HashMap<String, Servicejourney> servicejourneys = new HashMap<String, Servicejourney>();

	// 1.1.3 Verbindungen (Deadruntime)
	public HashMap<String, Deadruntime> deadruntimes = new HashMap<String, Deadruntime>();

	// 1.1.4 Depots
	public ArrayList<Depot> depots = new ArrayList<Depot>();
	
	// 1.1.5 Fahrzeugumlaeufe
	public ArrayList<Roundtrip> roundtrips = new ArrayList<Roundtrip>();

	Schedule initialSolution;
	
	/**
	 * Konstruktor: liest die Daten-Datei zeilenweise aus 
	 * und speichert die Instanzen in den zuvor erstellten und instanziierten Variablen
	 */
	public ReadInitialSolution(String path) {

		this.f = new File(path);

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(f));
		} catch (IOException e) {
			System.out.println(e);
		}

		// 1.2 Zeilen auslesen und in Variablen schreiben

		try {
			String temp = reader.readLine(); // erste Zeile
			String BlockBegin;
			String ersteszeichen;

			while (temp != null) // lese Datei zeilenweise aus
			{
				
				BlockBegin = temp.split(":")[0]; // erster Teil der Zeile bis zum ":"

				if (BlockBegin.equals("$STOPPOINT")) // 1. Relation: Stoppoint
				{

					temp = reader.readLine(); // nächste Zeile
					ersteszeichen = temp.substring(0, 1); // erstes Zeichen

					while (temp != null && !ersteszeichen.equals("$")) {

						Stoppoint neu = new Stoppoint(temp.split(";")[0]);
						stoppoints.put(neu.getId(), neu);

						temp = reader.readLine(); // nächste Zeile
						ersteszeichen = temp.substring(0, 1); // erstes Zeichen
					} // end while
					continue;
				} // end if


				if (BlockBegin.equals("$SERVICEJOURNEY")) // 2. Relation: Servicefahrten
				{

					temp = reader.readLine(); // nächste Zeile
					ersteszeichen = temp.substring(0, 1); // erstes Zeichen

					while (temp != null && !ersteszeichen.equals("$")) {

						String sfId= (temp.split(";")[0]); // ID
						String sfFromStopID = (temp.split(";")[1]); // Starthaltestelle
						String sfToStopID = (temp.split(";")[2]); // Endhaltestelle
						String sfDepTime = temp.split(";")[3]; // Abfahrtszeit
						String sfArrTime = temp.split(";")[4]; // Ankunftszeit
						int sfDistance = Integer.parseInt(temp.split(";")[5]); 

						Servicejourney neu = new Servicejourney(sfId, sfFromStopID, sfToStopID, sfDepTime, sfArrTime, sfDistance);
						servicejourneys.put(neu.getId(), neu);

						temp = reader.readLine(); // nächste Zeile
						ersteszeichen = temp.substring(0, 1); // erstes Zeichen

					} // end while
					continue;
				} // end if

				if (BlockBegin.equals("$DEADRUNTIME")) // 3. Relation: Servicefahrten
				{

					temp = reader.readLine(); // nächste Zeile
					ersteszeichen = temp.substring(0, 1); // erstes Zeichen
					
					while (temp != null && !ersteszeichen.equals("$")) {

						String fromStopID = (temp.split(";")[0]); // ID
						String toStopID = (temp.split(";")[1]); // ID
						int distance = Integer.parseInt(temp.split(";")[2]); 
						int runtime = Integer.parseInt(temp.split(";")[3]); 

						Deadruntime neu = new Deadruntime(fromStopID, toStopID, distance, runtime);
						deadruntimes.put(neu.getId(), neu);

						temp = reader.readLine();
						ersteszeichen = temp.substring(0, 1); // erstes Zeichen
						
					} // end while
					continue;
				} // end if
				
				if (BlockBegin.equals("$DEPOT")) // 4. Relation: Depots (inkl. wie viel sie in der Initialloesung genutzt werden)
				{
					temp = reader.readLine(); // nächste Zeile
					ersteszeichen = temp.substring(0, 1); // erstes Zeichen
					
					while (temp != null && !ersteszeichen.equals("$")) {
						String id = temp.split(";")[0];
                    	int capacity = Integer.parseInt(temp.split(";")[1]);
                    	
                    	Depot neu = new Depot(id);
                    	neu.setCapacity(capacity);
                    	depots.add(neu);
                    	
                    	temp = reader.readLine(); // nächste Zeile
                        ersteszeichen = temp.substring(0, 1); // erstes Zeichen
					}
					continue;
				}
				
				if (BlockBegin.equals("$INITIALSTOPPOINT")) // 5. Relation: Nutzung der Haltestellen (Ladestation? / Ladefrequenz)
				{
					temp = reader.readLine(); // nächste Zeile
					ersteszeichen = temp.substring(0, 1); // erstes Zeichen
					
					while (temp != null && !ersteszeichen.equals("$")) {
						String id = temp.split(";")[0];
                    	String isLoadingstation = temp.split(";")[1];
                    	boolean isLoadingstation1 = false;
                    	if(isLoadingstation.equals("true")){
                    		isLoadingstation1 = true;
                    	}
                    	int frequency = Integer.parseInt(temp.split(";")[2]);
                    	
                    	stoppoints.get(id).setLoadingstation(isLoadingstation1);
                    	stoppoints.get(id).setFrequency(frequency);
                    	
                    	temp = reader.readLine(); // nächste Zeile
                        ersteszeichen = temp.substring(0, 1); // erstes Zeichen
					}
					continue;
				}
				
				if (BlockBegin.equals("$Umlauf")) // 6. Relation: Fahrzeugumlaeufe nach der Eroeffnungsheuristik
				{
					temp = reader.readLine(); // nächste Zeile
					
					while (temp != null) {
						int id = Integer.parseInt(temp.split(";")[0]); // ID
                        String fahrten = (temp.split(";")[1]);
                        fahrten = fahrten.substring(1, fahrten.length()-1); // schneidet die Klammern ab
                        String[] ids = fahrten.split(", ");
                        String stopps = temp.split(";")[2];
                        stopps = stopps.substring(1, stopps.length()-1); // schneidet die Klammern ab
                        String[] idStoppoints = stopps.split(", ");
                        
                        Roundtrip neu = new Roundtrip(id);
                        for (int i = 0; i < ids.length; i++) {
							if(ids[i].length() == 10){
								neu.addFahrtAfterFahrt(i, (deadruntimes.get(ids[i])));
							}
							else{
								neu.addFahrtAfterFahrt(i, (servicejourneys.get(ids[i])));
							}		
						}
                        ArrayList<Stoppoint> laden = new ArrayList<Stoppoint>();
                        for (int i = 0; i < idStoppoints.length; i++) {
							laden.add((stoppoints.get(idStoppoints[i])));
						}
                        neu.setLaden(laden);
                        neu.setStellen(laden);
                        
                        roundtrips.add(neu);
						
						temp = reader.readLine(); // nächste Zeile
					}
					continue;
				}
			} // end outer while
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	private void printDepots(){
		for (int i = 0; i < depots.size(); i++) {
			System.out.println("Haltestelle " + depots.get(i).getId() + " ist ein Depot mit der Kapazität: " + depots.get(i).getCapacity());
		}
		System.out.println();
	}
	
	public HashMap<String, Stoppoint> getStoppoints() {
		return stoppoints;
	}


	public HashMap<String, Servicejourney> getServicejourneys() {
		return servicejourneys;
	}


	public HashMap<String, Deadruntime> getDeadruntimes() {
		return deadruntimes;
	}


	public ArrayList<Depot> getDepots() {
		return depots;
	}
	
	public void printInitialSolution(){
		for (int i = 0; i < roundtrips.size(); i++) {
			System.out.println(roundtrips.get(i).printRoundtrip());
		}
		System.out.println("In der Initialloesung gibt es " + roundtrips.size() + " Fahrzeugumlaeufe.");
		printDepots();
	}

	public ArrayList<Roundtrip> getRoundtrips() {
		return roundtrips;
	}

	public void setRoundtrips(ArrayList<Roundtrip> roundtrips) {
		this.roundtrips = roundtrips;
	}
	
	public void defineDepotCapacity(){
		int capacity = 0;
		for (int i = 0; i < depots.size(); i++) {
			if(depots.get(i).getCapacity() > capacity){
				capacity = depots.get(i).getCapacity();
			}
		}
		System.out.println("Die maximale Kapazität eines Depots beträgt: " + capacity + " Fahrzeuge.");
		for (int i = 0; i < depots.size(); i++) {
			depots.get(i).setCapacity(capacity);
		}
	}
	
	public void setInitialSolutionVariables(){
		initialSolution = new Schedule(roundtrips, servicejourneys, depots, stoppoints);
		int counter = 0;
		HashMap<String, Stoppoint> list = new HashMap<String, Stoppoint>();
		for (Entry<String, Stoppoint> e: stoppoints.entrySet()) {
			if (e.getValue().isLoadingstation()) {
				counter ++;
				list.put(e.getKey(), e.getValue());
			}
		}
		initialSolution.setNumberOfLoadingStations(counter);
		initialSolution.setStoppointsWithLoadingStations(list);
		initialSolution.setUmlaufplan(roundtrips);
		initialSolution.calculateCosts();
	}

	public Schedule getInitialSolution() {
		return initialSolution;
	}

	public void setInitialSolution(Schedule initialSolution) {
		this.initialSolution = initialSolution;
	}
}
