package edu.rutgers.se.components;

import java.util.concurrent.ExecutionException;

public interface IStockService {
	public void initializeStock(String stock_symbol,Integer stockid) throws InterruptedException, ExecutionException;
}
