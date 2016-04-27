package edu.rutgers.se.components;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;

import edu.rutgers.se.beans.HistStock;
import edu.rutgers.se.beans.InstStock;
import edu.rutgers.se.beans.KFPredictionResults;
import edu.rutgers.se.beans.Stock;
import edu.rutgers.se.config.DatabaseManager;
import edu.rutgers.se.rsi.KalmanPredictor;
import edu.rutgers.se.rsi.RSI;
import edu.rutgers.se.stockdownloader.InitializeStock;

@Component
public class StockService implements IStockService{
	@Override
	@Async
	public void initializeStock(String stock_symbol,Integer stockid) throws InterruptedException, ExecutionException{
		//trigger
		InitializeStock.collectHistoricData(stock_symbol, stockid);
		InitializeStock.collectRealtimeData(stock_symbol, stockid);
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connection;
			connection = DriverManager.getConnection(DatabaseManager.URL + DatabaseManager.DATABASE_NAME, DatabaseManager.USER_NAME, DatabaseManager.PASSWORD);
			Statement statement = connection.createStatement();
			statement.executeUpdate("UPDATE stocks SET init=1 WHERE id = "+stockid);
			connection.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public List<HistStock> getHistoricalQuote(Stock st, Date begin, Date end){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String beginTime = sdf.format(begin);
		String endTime = sdf.format(end);
		String query = "SELECT * FROM hist_data i WHERE i.stock_id='"+st.getId()+"' AND i.hist_date between \""+beginTime+"\" AND \""+endTime+"\"";
		List<HistStock> stockList = new ArrayList<HistStock>();
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connection;
			connection = DriverManager.getConnection(DatabaseManager.URL + DatabaseManager.DATABASE_NAME, DatabaseManager.USER_NAME, DatabaseManager.PASSWORD);
			Statement s = connection.createStatement();
			s.execute(query);
			ResultSet results = s.getResultSet();
			while(results.next()) {
				HistStock sq = new HistStock();
				sq.stock = st;
				sq.open = results.getDouble("open_price");
				sq.close = results.getDouble("close_price");
				sq.max = results.getDouble("max_price");
				sq.min = results.getDouble("min_price");
				sq.volume = results.getLong("volume");
				sq.hist_date = results.getDate("hist_date");
				stockList.add(sq);
			}
		} catch(Exception e) { 
			e.printStackTrace();
		}
		return stockList;
	}
	
	@Override
	public List<InstStock> getRealtimeQuote(Stock st, Date begin, Date end){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String beginTime = sdf.format(begin);
		String endTime = sdf.format(end);
		//System.out.println(beginTime +"-"+endTime);
		String query = "SELECT * FROM inst_data WHERE stock_id="+st.getId()+" AND inst_datetime between \""+beginTime+"\" AND \""+endTime+"\"";
		//System.out.println(query);
		List<InstStock> stockList = new ArrayList<InstStock>();
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connection;
			connection = DriverManager.getConnection(DatabaseManager.URL + DatabaseManager.DATABASE_NAME, DatabaseManager.USER_NAME, DatabaseManager.PASSWORD);
			Statement s = connection.createStatement();
			s.execute(query);
			ResultSet results = s.getResultSet();
			while(results.next()) {
				InstStock sq = new InstStock();
				sq.stock = st;
				sq.instPrice = results.getDouble("inst_price");
				sq.volume = results.getLong("volume");
				sq.instDateTime = results.getTimestamp("inst_datetime");
				stockList.add(sq);
			}
			connection.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return stockList;
	}
	
	public KFPredictionResults doPrediction(List<InstStock> allData) throws java.io.IOException
	{
		KFPredictionResults pr = new KFPredictionResults();
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
				pr.predictionPrices = pre; 

				RSI rsi = RSI.GetInstance();
				double[] RSIvalues = rsi.myRSIarray(allClosingPrices, allClosingPrices.length, 25);
				double predict = RSIvalues[RSIvalues.length-1];
				
				pr.predict=predict;
				
				if(predict>70){
					pr.sbh = "BUY";
				}
				else if(predict<30){
					pr.sbh = "SELL";
				}
				else{
					pr.sbh = "HOLD";
				}
				
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
		
		List<InstStock> prices = new ArrayList<InstStock>();
		try {
			Statement statement = connection.createStatement();
			String query = "select hist_date, close_price from hist_data where stock_id="+st.getId()+" ORDER BY hist_date DESC";
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
