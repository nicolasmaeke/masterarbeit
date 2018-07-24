package model;

/**
 * @author nicolasmaeke
 * 
 */
public class Depot {
	
	private final String id;
	private int capacity; // z.B. die Anzahl der Umlaeufe in der Initialloesung geteilt durch die Anzahl der Depots

	public Depot(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}


}
