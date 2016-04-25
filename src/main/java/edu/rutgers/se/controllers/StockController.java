package edu.rutgers.se.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import edu.rutgers.se.beans.HistStock;
import edu.rutgers.se.beans.InstStock;
import edu.rutgers.se.beans.Status;
import edu.rutgers.se.beans.Stock;
import edu.rutgers.se.components.IStockService;
import edu.rutgers.se.ma.MovingAverage;
import edu.rutgers.se.rsi.RSI;

@CrossOrigin
@RestController
@RequestMapping("/data")
public class StockController {
	@Autowired
	private IStockService stockService;
	
	@RequestMapping(value = "/initstock", method=RequestMethod.GET)
	public Status initStock(
			@RequestParam(value = "stockid",required = true) Integer stockid, 
			@RequestParam(value = "symbol",required = true) String symbol
			) throws InterruptedException, ExecutionException{
		Status s = new Status();
		stockService.initializeStock(symbol,stockid);
		s.setId(200);
		s.setMessage("Stock Initialization Started");
		return s;
	}	
	
	@RequestMapping(value = "/getRealTimeData", method=RequestMethod.GET)
	public Status getRealTimeData(
		       @RequestParam("stockid") int stockid,
		       @RequestParam("symbol") String symbol,
		       @RequestParam("startDate") long startDate,
		       @RequestParam("endDate") long endDate
		       ) {
		Stock st = new Stock();
		st.setId(stockid);
		st.setSymbol(symbol);
		
		List<InstStock> l = stockService.getRealtimeQuote(st,new Date(startDate),new Date(endDate));
		System.out.println(l.size());
		
		StringBuffer sb = new StringBuffer();
		int numOutputElems = 400;
		int numJumps = l.size()/numOutputElems;
		int x;
		sb.append("[{ 'key':'Volume','bar':true,'values':[");

		x = 0;
		for(InstStock i : l) {
			if(x++%numJumps == 0) {
				sb.append("["+i.instDateTime.getTime()+","+i.volume+"],");
			}
		}

		sb.setLength(sb.length() - 1);
		sb.append("]},{ 'key':'Price','values':[");

		x = 0;
		for(InstStock i : l) {
			if(x++%numJumps == 0)
				sb.append("["+i.instDateTime.getTime()+","+i.instPrice+"],");
		}

		sb.setLength(sb.length() - 1);

		sb.append("] }]");
		Status s = new Status();
		s.setId(200);
		s.setMessage(sb.toString());
		return s;	
	}
	
	@RequestMapping(value = "/getHistoricalData", method=RequestMethod.GET)
	public Status getHistoricalData(
				       @RequestParam("stockid") int stockid,
				       @RequestParam("symbol") String symbol,
				       @RequestParam("startDate") long startDate,
				       @RequestParam("endDate") long endDate,
				       @RequestParam("indicator") String indicator,
				       @RequestParam("maWindow") int maWindow
				       ) {
		
		Stock st = new Stock();
		st.setId(stockid);
		st.setSymbol(symbol);
		
		List<HistStock> lh = stockService.getHistoricalQuote(st,new Date(startDate),new Date(endDate));
		
		double[] ma = MovingAverage.myAverage(lh, maWindow);
		double[] ema = MovingAverage.myEMAverage(lh, maWindow);

		RSI rsi_ = RSI.GetInstance();
		double[] rsi = rsi_.myRSI(lh, maWindow);
		
		int numOutputElems = 400;
		int numJumps = lh.size()/numOutputElems;
		int x;
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
		
		StringBuffer sb = new StringBuffer();
		sb.append("{'values':[");

		x = 0;
		numJumps = 1;
		for(HistStock i : lh) {
			if(x%numJumps == 0) {
				sb.append("['");
				sb.append(sdf.format(i.hist_date));
				sb.append("',");
				if(!indicator.equals("rsi")) {
					if(i.open > i.close) {
						sb.append(i.min);
						sb.append(",");
						sb.append(i.open);
						sb.append(",");
						sb.append(i.close);
						sb.append(",");
						sb.append(i.max);
					} else {
						sb.append(i.max);
						sb.append(",");
						sb.append(i.close);
						sb.append(",");
						sb.append(i.open);
						sb.append(",");
						sb.append(i.min);
					}
				} else {
					sb.append("0,0,0,0");
				}
				
				if(indicator.equals("ma")) {
					if(x<maWindow-1) {
						sb.append(","+i.close);
					} else {
						sb.append(","+ma[x-maWindow+1]);
					}
				} else if(indicator.equals("ema")) {
					if(x<maWindow-1) {
						sb.append(","+i.close);
					} else {
						sb.append(","+ema[x-maWindow+1]);
					}
				} else if(indicator.equals("rsi")) {
					if(x<maWindow-1) {
						sb.append(",50");

					} else {
						sb.append(","+rsi[x-maWindow+1]);
					}
				}
				sb.append("],");
			}
			x++;
		}
		sb.setLength(sb.length() - 1);
		sb.append("]}");
		
		Status s = new Status();
		s.setId(200);
		s.setMessage(sb.toString());
		return s;
	
	}
	
}
