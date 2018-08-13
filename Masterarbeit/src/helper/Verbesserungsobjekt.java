package helper;

import model.Roundtrip;

public class Verbesserungsobjekt {
	
	double savings = 0;
	Roundtrip eins = null;
	Roundtrip zwei = null;
	//int indexAltEins = 0;
	//int indexAltZwei = 0;
	Roundtrip altEins = null;
	Roundtrip altZwei = null;

	public Verbesserungsobjekt(double savings, Roundtrip eins, Roundtrip zwei, Roundtrip altEins, Roundtrip altZwei) {
		this.savings = savings;
		this.eins = eins;
		this.zwei = zwei;
		this.altEins = altEins;
		this.altZwei = altZwei;
	}

	public double getSavings() {
		return savings;
	}

	public void setSavings(double savings) {
		this.savings = savings;
	}

	public Roundtrip getEins() {
		return eins;
	}

	public void setEins(Roundtrip eins) {
		this.eins = eins;
	}

	public Roundtrip getZwei() {
		return zwei;
	}

	public void setZwei(Roundtrip zwei) {
		this.zwei = zwei;
	}

	/**
	public int getIndexAltEins() {
		return indexAltEins;
	}

	public void setIndexAltEins(int indexAltEins) {
		this.indexAltEins = indexAltEins;
	}

	public int getIndexAltZwei() {
		return indexAltZwei;
	}

	public void setIndexAltZwei(int indexAltZwei) {
		this.indexAltZwei = indexAltZwei;
	}
	*/

	public Roundtrip getAltEins() {
		return altEins;
	}

	public void setAltEins(Roundtrip altEins) {
		this.altEins = altEins;
	}

	public Roundtrip getAltZwei() {
		return altZwei;
	}

	public void setAltZwei(Roundtrip altZwei) {
		this.altZwei = altZwei;
	}


}
