package edu.rutgers.se.controllers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.rutgers.se.beans.HistStock;
import edu.rutgers.se.beans.InstStock;
import edu.rutgers.se.beans.Stock;
import edu.rutgers.se.rsi.KalmanPredictor;
import edu.rutgers.se.rsi.RSI;
import edu.rutgers.se.svm.SVMMain;

import libsvm.svm_model;

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
	
	
	private PredictionResults doPrediction(List<InstStock> allData) throws java.io.IOException
	{
		PredictionResults pr = new PredictionResults();

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

			KalmanPredictor kp = KalmanPredictor.GetInstance();
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
		            
			if (difference>0) pr.goesUpOrDownForDay="1";//model.addObject("goesUpOrDownForDay", "1"); 
			else pr.goesUpOrDownForDay="0";//model.addObject("goesUpOrDownForDay", "0"); 
					
			double percentageDifference = (Math.abs(allData.get(0).instPrice-pre[0])/((allData.get(0).instPrice+pre[0])/2))*100;
			toBeRounded = (int) (percentageDifference * 100);
			rounded = (double) toBeRounded;
			percentageDifference = rounded/100;
					
					
			pr.nextDayDifference = difference;//model.addObject("nextDayDifference", difference); 
			pr.nextDayPercentageDifference = percentageDifference;//model.addObject("nextDayPercentageDifference", percentageDifference); 
					
			difference = pre[4] - allData.get(0).instPrice;
			int toBeRounded2 = (int) (difference * 100);
			double rounded2 = (double) toBeRounded2;
			difference = rounded2/100;
		            
			if (difference>0) pr.goesUpOrDownFor5Day="1";//model.addObject("goesUpOrDownFor5Day", "1"); 
			else pr.goesUpOrDownFor5Day="0";//model.addObject("goesUpOrDownFor5Day", "0"); 
			
			percentageDifference = (Math.abs(allData.get(0).instPrice-pre[4])/((allData.get(0).instPrice+pre[4])/2))*100;
			toBeRounded = (int) (percentageDifference * 100);
			rounded = (double) toBeRounded;
			percentageDifference = rounded/100;
					
			pr.difference=difference;//model.addObject("fiveDayDifference", difference); 
			pr.fiveDayPercentageDifference=percentageDifference;//model.addObject("fiveDayPercentageDifference", percentageDifference); 
					
			//Pari's code
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
					//gLogger.log("I DETECTED"+pair.getKey());
					pointer++;
				}    
			}
				    
			double buyConfidence = 0;
			double sellConfidence = 0;
			double holdConfidence = 0;
				    
			for (int j=0; j< patternsHappening.length; j++){
				//gLogger.log("IM="+patternsHappening[j]);
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

			pr.buy=buy;//model.addObject("buy", buy);
			pr.sell=sell;//model.addObject("sell",sell);
			pr.hold=hold;//model.addObject("hold", hold);

		}

		return pr;
	}

}
