package de.open4me.depot.jfreechart;

public class BestandsItem {
	
	private final int id;
	private final String name;
	
	private double anzahl=0;
	private double kosten=0;

	private double wert;

	public double getWert() {
		return wert;
	}

	public BestandsItem(int id, Object name) {
		this.name=name.toString();
		this.id=id;
	}

	public double getAnzahl() {
		return anzahl;
	}

	public void addAnzahl(double anzahl) {
		this.anzahl += anzahl;
	}

	public double getKosten() {
		return kosten;
	}

	public void addKosten(double kosten) {
		this.kosten += kosten;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public double wert(double kurs) {
		wert = anzahl*kurs;
		return wert;
	}
	
	public double gewinn(double kurs) {
		return wert(kurs)+kosten;
	}
	
	public double gewinnRelative(double kurs) {
		return gewinn(kurs)/(-1*kosten);
	}
	
	public boolean isRelevant() {
		return anzahl!=0;
	}

}
