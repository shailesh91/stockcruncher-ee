package edu.rutgers.se.beans;

import java.util.Date;

public class InstStock {
	public Stock stock;
	public Date entryDate;
	public double instPrice;
	public long volume;

	public Stock getTicker() {
		return stock;
	}
	public void setTicker(Stock _Stock) {
		stock = _Stock;
	}
	
	public Date getEntryDate() {
		return entryDate;
	}
	public void setEntryDate(Date _EntryDate) {
		entryDate = _EntryDate;
	}

	
	public double getInstPrice() {
		return instPrice;
	}
	public void setInstPrice(double _InstPrice) {
		instPrice = _InstPrice;
	}

	public long getVolume() {
		return volume;
	}
	public void setVolume(long _Volume) {
		volume = _Volume;
	}

	public String toString() {
		return "InstStock "+stock.getSymbol()+
			" "+entryDate+" "+instPrice+" "+volume;
	}
}
