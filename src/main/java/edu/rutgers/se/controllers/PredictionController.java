package edu.rutgers.se.controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import libsvm.svm_model;
import edu.rutgers.se.beans.HistStock;
import edu.rutgers.se.beans.InstStock;
import edu.rutgers.se.beans.Stock;
import edu.rutgers.se.config.DatabaseManager;
import edu.rutgers.se.rsi.KalmanPredictor;
import edu.rutgers.se.rsi.RSI;
import edu.rutgers.se.svm.SVMMain;

@RestController
@RequestMapping("/data")
public class PredictionController {
	
	private static class PredictionResults implements Comparable {
		public double[] predictionPrices;
		public String goesUpOrDownForDay;
		public double nextDayDifference;
		public double nextDayPercentageDifference;
		public String goesUpOrDownFor5Day;
		public double difference;
		public double fiveDayPercentageDifference;
		public double buy;
		public double sell;
		public double hold;
		public double predict;

		public double max10days;
		public double ave1year;
		public double min1year;


		List<InstStock> allInstData;
		List<HistStock> allHistData;
		public Stock ticker;
		public static Type compareBy;
		public static enum Type {
			nextDayPercentageDifference,
			fiveDayPercentageDifference,
			buy,
			sell,
			hold,
			predict
		};
		PredictionResults() {
			compareBy = Type.nextDayPercentageDifference;
		}
		public int compareTo(Object _o) {
			PredictionResults o = (PredictionResults) _o;
			switch(compareBy) {
			case nextDayPercentageDifference:
				return Double.compare(nextDayPercentageDifference,o.nextDayPercentageDifference)>0?-1:1;
			case fiveDayPercentageDifference:
				return Double.compare(fiveDayPercentageDifference,o.fiveDayPercentageDifference)>0?-1:1;
			case buy:
				return Double.compare(buy,o.buy)>0?-1:1;
			case sell:
				return Double.compare(sell,o.sell)>0?-1:1;
			case hold:
				return Double.compare(hold,o.hold)>0?-1:1;
			case predict:
				return Double.compare(predict,o.predict)>0?-1:1;
			default:
				System.err.println("ERROOOOOOOR in PredictionResults");
				return 0;
			}
		}
	}
	
	
	@RequestMapping("/stockPrediction")
	public PredictionResults getStockPrediction(@RequestParam(value = "stockid",required = true) Integer stockid, @RequestParam(value = "symbol",required = true) String symbol) throws InterruptedException, ExecutionException{
		Stock st = new Stock();
		st.setId(stockid);
		st.setSymbol(symbol);
		PredictionResults pr = null;
		try{
			List<InstStock> stockData = getDailyClosingPrices(st);
			pr = doPrediction(stockData);
		}catch(Exception e){
			
		}
		return pr;
	}
	
	private PredictionResults doPrediction(List<InstStock> allData) throws java.io.IOException
	{
		PredictionResults pr = new PredictionResults();
		try{
		

		double[] allClosingPrices=new double[allData.size()];
		for(int i=0; i<allData.size(); i++){
			allClosingPrices[i]=allData.get(i).instPrice;
		}
			
		if (allData.size()>32){
			double[] prices = new double[32];
			int indx=0;
			for(int i=31; i>=0; i--){
				prices[indx]=allData.get(i).instPrice;
				indx++;
			}
			
			
			KalmanPredictor kp = new KalmanPredictor();
			double[] u = kp.DWaveletT(prices, 32);
			double[] inter_signal = kp.interpolator(prices, u);
			double[] pre = kp.KalmanFilter(inter_signal, 32);
			pr.predictionPrices = pre;//model.addObject("predictionPrices", pre); 

			RSI rsi = RSI.GetInstance();
			double[] RSIvalues = rsi.myRSIarray(allClosingPrices, allClosingPrices.length, 25);
			double predict = RSIvalues[RSIvalues.length-1];
			//gLogger.log("RSI IS===="+predict);

			pr.predict=predict;
			
			//get the difference
			double difference = pre[0] - allData.get(0).instPrice;
			int toBeRounded = (int) (difference * 100);
			double rounded = (double) toBeRounded;
			difference = rounded/100;
		            
			if (difference>0) pr.goesUpOrDownForDay="1"; 
			else pr.goesUpOrDownForDay="0"; 
					
			double percentageDifference = (Math.abs(allData.get(0).instPrice-pre[0])/((allData.get(0).instPrice+pre[0])/2))*100;
			toBeRounded = (int) (percentageDifference * 100);
			rounded = (double) toBeRounded;
			percentageDifference = rounded/100;
					
					
			pr.nextDayDifference = difference; 
			pr.nextDayPercentageDifference = percentageDifference; 
					
			difference = pre[4] - allData.get(0).instPrice;
			int toBeRounded2 = (int) (difference * 100);
			double rounded2 = (double) toBeRounded2;
			difference = rounded2/100;
		            
			if (difference>0) pr.goesUpOrDownFor5Day="1"; 
			else pr.goesUpOrDownFor5Day="0"; 
			
			percentageDifference = (Math.abs(allData.get(0).instPrice-pre[4])/((allData.get(0).instPrice+pre[4])/2))*100;
			toBeRounded = (int) (percentageDifference * 100);
			rounded = (double) toBeRounded;
			percentageDifference = rounded/100;
					
			pr.difference=difference; 
			pr.fiveDayPercentageDifference=percentageDifference; 
					
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

			pr.buy=buy;
			pr.sell=sell;
			pr.hold=hold;

		}
		}catch(Exception e){
			e.printStackTrace();
		}
		return pr;
	}
	
	public List<InstStock> getDailyClosingPrices(Stock st) throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		Connection connection;
		connection = DriverManager.getConnection(DatabaseManager.URL + DatabaseManager.DATABASE_NAME, DatabaseManager.USER_NAME, DatabaseManager.PASSWORD);
		System.out.println(st.getId());
		
		List<InstStock> prices = new ArrayList<InstStock>();
		try {
			Statement statement = connection.createStatement();
			String query = "select hist_date, close_price from hist_data where stock_id="+st.getId()+" ORDER BY hist_date DESC";
			System.out.println(query);
			ResultSet res = statement.executeQuery(query);
			while(res.next()) {
				InstStock stock = new InstStock();
				stock.setStock(st);
				stock.setInstDateTime(res.getDate("hist_date"));
				stock.setInstPrice(res.getDouble("close_price"));
				prices.add(stock);
			}
			connection.close();
		} catch (Exception e) {
			
		}
		return prices;
	}

}
