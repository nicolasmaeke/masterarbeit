package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import model.Deadruntime;
import model.Depot;
import model.Servicejourney;
import model.Stoppoint;

/**
 * @author nicolasmaeke
 * Diese Klasse liest aus einer bereinigten Text-Datei Parameter aus und speichert diese als Variablen.
 */

public class ReadReassessedData {

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

	/**
	 * Konstruktor: liest die Daten-Datei zeilenweise aus 
	 * und speichert die Instanzen in den zuvor erstellten und instanziierten Variablen
	 */
	public ReadReassessedData(String path) {

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
					
					while (temp != null) {

						String fromStopID = (temp.split(";")[0]); // ID
						String toStopID = (temp.split(";")[1]); // ID
						int distance = Integer.parseInt(temp.split(";")[2]); 
						int runtime = Integer.parseInt(temp.split(";")[3]); 

						Deadruntime neu = new Deadruntime(fromStopID, toStopID, distance, runtime);
						deadruntimes.put(neu.getId(), neu);

						temp = reader.readLine();

					} // end while
					continue;
				} // end if
			} // end outer while
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	
	// vielleicht doch lieber nicht mit Zufall, sondern direkt in die Datei schreiben???
	// nein, dann kann die Anzahl der Depots nicht variabel beim Programmstart bestimmt werden!
	// besser: die zufällig gewählten Depots mit in die Initiallösung schreiben!
	public void assignDepot(int numberOfDepots){
		int counter = 0;
		for (Map.Entry<String, Stoppoint> entry : stoppoints.entrySet()) {
			if(entry.getValue().getId().startsWith("0000")){ // zuerst die Depots, die in der Instanz als solche ausgewiesen sind
				Depot d = new Depot(entry.getKey());
				depots.add(d);
				counter ++;
			}
		}
		if(counter < numberOfDepots){
			boolean flag = true;
			for (Map.Entry<String, Stoppoint> entry : stoppoints.entrySet()) { // dann zufaellig weitere Depots
				Depot d = new Depot(entry.getKey());
				for (int i = 0; i < depots.size(); i++) {
					if(depots.get(i).getId().equals(d.getId())){
						flag = false;
					}
				}
				if(flag == true){
					depots.add(d);
					stoppoints.get(entry.getKey()).setDepot(true);
					counter ++;
				}
				if (counter == numberOfDepots) {
					break;
				}
			}
		}
	}

	public void printDepots(){
		for (int i = 0; i < depots.size(); i++) {
			System.out.println("Haltestelle " + depots.get(i).getId() + " ist ein Depot.");
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
}


