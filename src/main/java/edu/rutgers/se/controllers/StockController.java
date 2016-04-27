package edu.rutgers.se.controllers;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.CrossOrigin;

import edu.rutgers.se.ann.ANN;
import edu.rutgers.se.beans.HistStock;
import edu.rutgers.se.beans.InstStock;
import edu.rutgers.se.beans.KFPredictionResults;
import edu.rutgers.se.beans.Status;
import edu.rutgers.se.beans.Stock;
import edu.rutgers.se.components.IStockService;
import edu.rutgers.se.ma.MovingAverage;
import edu.rutgers.se.rsi.RSI;
import edu.rutgers.se.svm.SVMMain;
import libsvm.svm_model;

@CrossOrigin
@RestController
@RequestMapping("/data")
public class StockController {
	@Autowired
	private IStockService stockService;
	
	@RequestMapping(value = "/initstock", method=RequestMethod.GET)
	public @ResponseBody Status initStock(
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
	public @ResponseBody Status getRealTimeData(
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
	public @ResponseBody Status getHistoricalData(
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
	
	//Prediction Part
	
	
	
	@RequestMapping(value = "/getKFPrediction", method=RequestMethod.GET)
	public @ResponseBody Status getKFPrediction(
			@RequestParam(value = "stockid",required = true) Integer stockid, 
			@RequestParam(value = "symbol",required = true) String symbol
			) throws InterruptedException, ExecutionException{
		Stock st = new Stock();
		st.setId(stockid);
		st.setSymbol(symbol);
		String jsonInString="";
		try{
			KFPredictionResults pr = null;
			List<InstStock> stockData = stockService.getDailyClosingPrices(st);
			pr = stockService.doPrediction(stockData);
			ObjectMapper mapper = new ObjectMapper();
			//Object to JSON in String
			jsonInString = mapper.writeValueAsString(pr);
		}catch(Exception e){
			e.printStackTrace();
		}
		Status s = new Status();
		s.setId(200);
		s.setMessage(jsonInString);
		return s;		
	}
	
	@RequestMapping(value = "/getSVMPrediction", method=RequestMethod.GET)
	public @ResponseBody Status getSVMPrediction(
			@RequestParam(value = "stockid",required = true) Integer stockid, 
			@RequestParam(value = "symbol",required = true) String symbol
			) throws InterruptedException, ExecutionException{
		Stock st = new Stock();
		st.setId(stockid);
		st.setSymbol(symbol);
		StringBuffer sb = new StringBuffer();
		try{
			List<InstStock> allData = stockService.getDailyClosingPrices(st);
			double[] allClosingPrices=new double[allData.size()];
			for(int i=0; i<allData.size(); i++){
				allClosingPrices[i]=allData.get(i).instPrice;
			}
			
			if (allData.size()>32){
				SVMMain svmPredict = SVMMain.GetInstance();
				int[] patternsHappening=new int[7];
				int pointer=0;
						
				Iterator it = svmPredict.getGmodels().entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry pair = (Map.Entry)it.next();
					int range = 150;
					if (allClosingPrices.length<range) range=allClosingPrices.length;
					double isThePattern = svmPredict.svmTest(allClosingPrices, (svm_model)pair.getValue() ,21, range);
					        
					if (isThePattern==1){
						patternsHappening[pointer]=Integer.parseInt(pair.getKey().toString());
						
						pointer++;
					}    
				}
					    
				double buyConfidence = 0;
				double sellConfidence = 0;
				double holdConfidence = 0;
					    
				for (int j=0; j< patternsHappening.length; j++){
					switch (patternsHappening[j]){
					case 1:
						sellConfidence=sellConfidence+1;
						break;
						//"sell"
					case 2:
						buyConfidence=buyConfidence+1;
						break;
						//"buy"
					case 3:
						//"sell"
						sellConfidence=sellConfidence+1;
						break;
					case 4:
						//"hold"
						holdConfidence=holdConfidence+1;
						break;
					case 5:
						//"buy"
						buyConfidence=buyConfidence+1;
						break;
					case 6:
						//gLogger.log("IM sixxxxxxxx");
						holdConfidence=holdConfidence+1;
						break;
						//"hold"
					case 7:
						holdConfidence=holdConfidence+1;
						break;
						//"hold"
					}
				}
					    
				double totalConfidence=buyConfidence+sellConfidence+holdConfidence;
				double buy = (buyConfidence/totalConfidence)*100;
				double sell = (sellConfidence/totalConfidence)*100;
				double hold = (holdConfidence/totalConfidence)*100;
				sb.append("{'values':['buy':"+buy+",'sell':"+sell+",'hold':"+hold+"]}");
			}
		}catch(Exception e){
			
		}
		Status s = new Status();
		s.setId(200);
		s.setMessage(sb.toString());
		return s;	
	}
	
	@RequestMapping(value = "/getAnnPrediction", method=RequestMethod.GET)
	public @ResponseBody Status getAnnPrediction(
			@RequestParam(value = "stockid",required = true) Integer stockid, 
			@RequestParam(value = "symbol",required = true) String symbol
			) throws InterruptedException, ExecutionException{
		Stock st = new Stock();
		st.setId(stockid);
		st.setSymbol(symbol);
		ANN ann = new ANN();
		String sb = ann.predictHistory(st);
		Status s = new Status();
		s.setId(200);
		s.setMessage(sb);
		return s;
	}
}
