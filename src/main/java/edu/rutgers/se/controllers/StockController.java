package edu.rutgers.se.controllers;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
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
		
		//System.out.println(startDate+"-"+endDate);
		
		List<InstStock> l = stockService.getRealtimeQuote(st,new Date(startDate),new Date(endDate));
		
		StringBuffer sb = new StringBuffer();
		StringBuffer volume = new StringBuffer();
		StringBuffer price = new StringBuffer();
		
		for(InstStock i : l) {
			if(i.instPrice!=0){
				volume.append("["+i.instDateTime.getTime()+","+i.volume+"],");
				price.append("["+i.instDateTime.getTime()+","+i.instPrice+"],");
			}
		}

		sb.append("[{ 'key':'Volume','bar':true,'values':[");
		sb.append(volume);
		sb.setLength(sb.length() - 1);
		sb.append("]},{ 'key':'Price','values':[");
		sb.append(price);
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
				sb.append(","+i.volume);
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
	
	/*@RequestMapping(value = "/getRealTimeQuote", method=RequestMethod.GET)
	public Status getRealTimeQuote(
		       @RequestParam("stockid") int stockid,
		       @RequestParam("symbol") String symbol
		       ) throws ParseException, IOException{
		
		String url = "http://finance.yahoo.com/d/quotes.csv?s=" + symbol + "&f=sd1t1l1v&e=.csv";
		URL yahoolive = new URL(url);
		URLConnection datalive = yahoolive.openConnection();
		Scanner input = new Scanner(datalive.getInputStream());
		StringBuffer sb = new StringBuffer();
		StringBuffer volume = new StringBuffer();
		StringBuffer price = new StringBuffer();
		while (input.hasNext()) {
			String line = input.nextLine();
			String[] tokenslive = line.split(",");
			SimpleDateFormat from = new SimpleDateFormat("MM/dd/yyyy h:mma");
			//SimpleDateFormat to = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = from.parse(tokenslive[1].substring(1, tokenslive[1].length() - 1) + " " + tokenslive[2].substring(1, tokenslive[2].length() - 1));    
			volume.append("["+date.getTime()+","+tokenslive[4]+"],");
			price.append("["+date.getTime()+","+tokenslive[3]+"],");
		}
		sb.append("[{ 'key':'Volume','bar':true,'values':[");
		sb.append(volume);
		sb.setLength(sb.length() - 1);
		sb.append("]},{ 'key':'Price','values':[");
		sb.append(price);
		sb.setLength(sb.length() - 1);
		sb.append("] }]");
		input.close();
		Status s = new Status();
		s.setId(200);
		s.setMessage(sb.toString());
		return s;	
	}*/
}
