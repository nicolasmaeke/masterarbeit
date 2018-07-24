/**
 * 
 */
package model;

/**
 * @author nicolasmaeke
 *
 */
public class Stoppoint {
	
	private final String id;
	private boolean isLoadingstation;
	private boolean isDepot; // falls true, dann isLoadingstation auch true
	private int frequency;

	public Stoppoint(String id) {
		this.id = id;
		setLoadingstation(false);
		setDepot(false);
		setFrequency(0);
	}

	public String getId() {
		return id;
	}
	
	public void setLoadingstation(boolean b) {
		isLoadingstation = b;
	}
	
	public int getFrequency(){
		return frequency;
	}

	public void setFrequency(int i) {
		frequency = i;
	}

	public boolean isLoadingstation() {
		return isLoadingstation;
	}

	public boolean isDepot() {
		return isDepot;
	}

	public void setDepot(boolean isDepot) {
		this.isDepot = isDepot;
		if(isDepot == true){
			this.setLoadingstation(true);
		}
	}

}
