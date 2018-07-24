/**
 * 
 */
package parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;


import model.Deadruntime;
import model.Servicejourney;
import model.Stoppoint;

/**
 * @author nicolasmaeke
 * Diese Klasse liest eine Text-Datei ein, entfernt alle unnoetigen Zeilen und ergaenzt fehlende Daten.
 */
public class ReassessInitialData {

	public File f;
	public BufferedWriter output;
	public PrintWriter writer;

	public int avgDistanceDeadrun;
	public int avgRuntimeDeadrun;
	public int avgDistanceServicejourney;
	public int totalRuntime;
	public int totalDistance;
	public int totalDistanceServicejourney;

	public Vector<String> listStopPoints = new Vector<String>();
	public Vector<String> listServiceJourney = new Vector<String>();
	public Vector<String> listFromToServiceJourney = new Vector<String>();
	public Vector<Integer> listDistanceServiceJourney = new Vector<Integer>();
	public Vector<String> listDeadRun = new Vector<String>();
	public Vector<String> listFromToDeadRun = new Vector<String>();
	public Vector<Integer> listDistanceDeadRun = new Vector<Integer>();
	public Vector<Integer> listRunTimeDeadRun = new Vector<Integer>();


	// factorial function

	int factorial(int n) {
		if (n == 1)
			return 1;
		else
			return n * factorial(n - 1);
	}

	// Methode liest die Daten-Datei zeilenweise aus und fügt Zeilen hinzu, wenn Daten fehlen

	public ReassessInitialData(String oldPath, String newPath) {

		this.f = new File(oldPath);

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(f));
		} catch (IOException e) {
			System.out.println(e);
		}


		try {
			writer = new PrintWriter(new FileOutputStream(newPath, false));
		} catch (FileNotFoundException e1) {
			System.out.println(e1);
		}


		// Zeilen auslesen

		try {
			String temp = reader.readLine(); // temp = erste Zeile
			String BlockBegin;
			String ersteszeichen;

			while (temp != null) // lese Datei zeilenweise aus
			{
				// writer.println(temp); // schreibe jede Zeile in die neue Datei
				//writer.flush();
				temp = reader.readLine(); // lese nächste Zeile

				BlockBegin = temp.split(":")[0]; // erster Teil der Zeile bis zum ":" 

				if (BlockBegin.equals("$STOPPOINT")) // 1. Relation: Stoppoint 
				{
					writer.println(temp.split(";")[0]); 
					// schreibe die Parameter des Typs in die neue Datei 
					// (die Parameter des Typs Stoppoint sind redundant, deswegen nur der erste Parameter geschrieben)
					writer.flush();
					temp = reader.readLine(); // nächste Zeile (das erste Objekt des Typs Stoppoint)
					ersteszeichen = temp.substring(0, 1); // erstes Zeichen                 

					while (temp != null && !ersteszeichen.equals("*")) { 
						// Abbruchkriterium der Schleife: die Zeile ist leer oder es handelt sich um eine Trennzeile (beginnt mit "*")

						String spId = (temp.split(";")[0]) ; // die Parameter eines Objekts werden mit Semikolom getrennt

						// solange Anzahl Zeichen von ID < 5, füge "0" vorne hinzu, damit der Parameter immer gleich viele Zeichen hat
						do {
							spId = "0" + spId;
						} while (spId.length() < 5);

						listStopPoints.add(spId);

						temp = spId; // ersetze neue ID mit 5-Ziffern
						temp = reader.readLine(); // nächste Zeile (das nächste Objekt des Typs Stoppoint)
						ersteszeichen = temp.substring(0, 1); // erstes Zeichen
						
					} // end while
					Collections.shuffle(listStopPoints); // zufaellig die Liste der Haltestellen mischen
					for (int i = 0; i < listStopPoints.size(); i++) {
						writer.println(listStopPoints.get(i)); // das Objekt wird in die neue Datei geschrieben
						writer.flush();
					}
					continue;
				} // end if


				if (BlockBegin.equals("$SERVICEJOURNEY")) // 2. Relation: Servicefahrten
				{
					temp = temp.split(";")[0] + ";" + temp.split(";")[2] + ";" + temp.split(";")[3] + ";" + temp.split(";")[4] + ";" + temp.split(";")[5] + ";" + temp.split(";")[11];
					// schreibe die relevaten Parameter des Typs Servicejourney
					writer.println(temp);
					writer.flush();
					temp = reader.readLine(); // nächste Zeile (das erste Objekt des Typs Servicejourney)
					ersteszeichen = temp.substring(0, 1); // erstes Zeichen                   

					while (temp != null && !ersteszeichen.equals("*")) {

						String sfId = (temp.split(";")[0]); // ID
						String sfFromStopID = (temp.split(";")[2]); // Starthaltestelle
						String sfToStopID = (temp.split(";")[3]); // Endhaltestelle
						String sfDepTime = temp.split(";")[4]; // Abfahrtszeit
						String sfArrTime = temp.split(";")[5]; // Ankunftszeit
						int sfDistance = Integer.parseInt(temp.split(";")[11]); // Distanz in Meter

						while (sfId.length() < 8) {
							sfId = "0" + sfId;
						}; // solange Ziffern von sfID < 8, füge 0 vorne hinzu 

						while (sfFromStopID.length() < 5) {
							sfFromStopID = "0" + sfFromStopID;
						}; // solange Ziffern von FromStopID < 5, füge 0 vorne hinzu

						while (sfToStopID.length() < 5) {
							sfToStopID = "0" + sfToStopID;
						};  // solange Ziffern von ToStopID < 5, füge 0 vorne hinzu

						listFromToServiceJourney.add(sfFromStopID + sfToStopID);
						listDistanceServiceJourney.add(sfDistance);

						temp = sfId + ";" + sfFromStopID + ";" + sfToStopID + ";" + sfDepTime + ";" + sfArrTime + ";" + sfDistance; 
						// schreibe aktualisierte Zeile in die neue Datei
						writer.println(temp); // schreibe Zeile mit neuen IDs
						writer.flush();
						temp = reader.readLine(); // lese nächste Zeile
						ersteszeichen = temp.substring(0, 1); // erstes Zeichen  

					} // end while         

					continue;
				} // end if

				if (BlockBegin.equals("$DEADRUNTIME")) // 3. Relation: Leerfahrten
				{
					temp = temp.split(";")[0] + ";" + temp.split(";")[1] + ";" + temp.split(";")[4] + ";" + temp.split(";")[5];
					// schreibe die relevanten Parameter des Typs Deadruntime in die neue Datei
					writer.println(temp);
					writer.flush();
					temp = reader.readLine(); // nächste Zeile (das erste Objekt des Typs Deadruntime)
					ersteszeichen = temp.substring(0, 1); // erstes Zeichen

					while (temp != null && !ersteszeichen.equals("*")) {

						String fromStopID = (temp.split(";")[0]); // ID der Starthaltestelle
						String toStopID = (temp.split(";")[1]); // ID der Endhaltestelle
						String distance = (temp.split(";")[4]); // Distanz in Meter
						int runtime = Integer.parseInt(temp.split(";")[5]); // Fahrtzeit in Sekunden

						while (fromStopID.length() < 5) {
							fromStopID = "0" + fromStopID;
						}; 

						while (toStopID.length() < 5) {
							toStopID = "0" + toStopID;
						}; 

						listDeadRun.add(fromStopID);
						listRunTimeDeadRun.add(runtime);
						listDistanceDeadRun.add(Integer.valueOf(distance));
						listFromToDeadRun.add(fromStopID+toStopID);                 
						temp = fromStopID + ";" + toStopID + ";" + distance + ";" + runtime; 
						// schreibe aktualisierte Zeile in die neue Datei
						writer.println(temp);
						writer.flush();
						temp = reader.readLine();

					} // end while

					for(int i = 0; i < listRunTimeDeadRun.size(); i++) {
						totalRuntime += listRunTimeDeadRun.get(i); 
					}
					avgRuntimeDeadrun = totalRuntime / listRunTimeDeadRun.size();


					for(int i = 0; i < listDistanceDeadRun.size(); i++) {
						totalDistance += listDistanceDeadRun.get(i);
					} 
					avgDistanceDeadrun = totalDistance / listDistanceDeadRun.size();                     

					if (listFromToDeadRun.size() < listStopPoints.size()*listStopPoints.size()){ 
						// wenn Bedingung zutrifft, fehlen Leerfahrten
						for (String i : listStopPoints){
							for (String j : listStopPoints){
								// durchlaufe alle Paare von Haltestellen und prüfe, ob es zwischen ihnen eine LF gibt
								if (!listFromToDeadRun.contains(i+j) && (!i.equals(j))) {
									// Falls keine LF zwischen unterschiedlichen Haltestellen existiert, erstelle eine neue LF mit Durchschnittswerten
									String fromStopID = i;
									String toStopID = j;
									int distance = avgDistanceDeadrun;
									int runtime = avgRuntimeDeadrun;                   				

									listFromToDeadRun.add(fromStopID+toStopID);	
									listDistanceDeadRun.add(distance);
									listRunTimeDeadRun.add(runtime);

									writer.println(fromStopID + ";" + toStopID + ";" + avgDistanceDeadrun + ";" + avgRuntimeDeadrun);
									writer.flush();
								}
								else if (!listFromToDeadRun.contains(i+j) && (i.equals(j))) {
									// Falls keine LF zwischen gleichen Haltestellen existiert, erstelle eine neue LF mit Distance und Fahrzeit = 0
									String fromStopID = i;
									String toStopID = j;
									int distance = 0;
									int runtime = 0;                   				

									listFromToDeadRun.add(fromStopID+toStopID);	
									listDistanceDeadRun.add(distance);
									listRunTimeDeadRun.add(runtime);

									writer.println(fromStopID + ";" + toStopID + ";" + "0" + ";" + "0");
									writer.flush();
								}
							}                    	  	
						}  
					}
				}
				continue;
				// end if

			} // end outer while
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}
