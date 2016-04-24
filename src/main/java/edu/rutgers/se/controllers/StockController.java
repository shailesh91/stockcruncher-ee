package edu.rutgers.se.controllers;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.rutgers.se.ann.ANN;
import edu.rutgers.se.beans.Status;
import edu.rutgers.se.beans.Stock;
import edu.rutgers.se.components.IStockService;

@RestController
@RequestMapping("/data")
public class StockController {
	@Autowired
	private IStockService stockService;
	
	@RequestMapping("/initstock")
	public Status initStock(@RequestParam(value = "stockid",required = true) Integer stockid, @RequestParam(value = "symbol",required = true) String symbol) throws InterruptedException, ExecutionException{
		Status s = new Status();
		stockService.initializeStock(symbol,stockid);
		s.setId(200);
		s.setMessage("Stock Initialization Started");
		return s;
	}	
	
	@RequestMapping("/ann")
	public void annPrediction(@RequestParam(value = "stockid",required = true) Integer stockid, @RequestParam(value = "symbol",required = true) String symbol) throws InterruptedException, ExecutionException{
		Stock st = new Stock();
		st.setId(stockid);
		st.setSymbol(symbol);
		ANN ann = new ANN();
		ann.predictHistory(st);
	}
	
}
