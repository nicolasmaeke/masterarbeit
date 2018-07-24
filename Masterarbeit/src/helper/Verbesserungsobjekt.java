package helper;

import model.Roundtrip;

public class Verbesserungsobjekt {
	
	double savings = 0;
	Roundtrip eins = null;
	Roundtrip zwei = null;
	int indexAltEins = 0;
	int indexAltZwei = 0;

	public Verbesserungsobjekt(double savings, Roundtrip eins, Roundtrip zwei, int indexAltEins, int indexAltZwei) {
		this.savings = savings;
		this.eins = eins;
		this.zwei = zwei;
		this.indexAltEins = indexAltEins;
		this.indexAltZwei = indexAltZwei;
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


}
