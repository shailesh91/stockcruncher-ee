package edu.rutgers.se.components;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.rutgers.se.beans.HistStock;
import edu.rutgers.se.beans.InstStock;
import edu.rutgers.se.beans.Stock;

public interface IStockService {
	public void initializeStock(String stock_symbol,Integer stockid) throws InterruptedException, ExecutionException;
	public List<HistStock> getHistoricalQuote(Stock stock, Date begin, Date end);
	public List<InstStock> getRealtimeQuote(Stock stock, Date begin, Date end);
	
}
