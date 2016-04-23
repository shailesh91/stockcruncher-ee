package edu.rutgers.se.beans;

import java.util.Date;

public class InstStock {
	public Stock stock;
	public Date instDateTime;
	public double instPrice;
	public long volume;

	public Stock getStock() {
		return stock;
	}
	public void setStock(Stock _Stock) {
		stock = _Stock;
	}
	
	public Date getInstDateTime() {
		return instDateTime;
	}
	public void setInstDateTime(Date _instDateTime) {
		instDateTime = _instDateTime;
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
			" "+instDateTime+" "+instPrice+" "+volume;
	}
}
