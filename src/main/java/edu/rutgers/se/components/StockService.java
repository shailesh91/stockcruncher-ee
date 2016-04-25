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
import edu.rutgers.se.beans.Stock;
import edu.rutgers.se.config.DatabaseManager;
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
		String query = "SELECT * FROM inst_data i WHERE i.stock_id='"+st.getId()+"' AND i.inst_datetime between \""+beginTime+"\" AND \""+endTime+"\"";
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
	
}
