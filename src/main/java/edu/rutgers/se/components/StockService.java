package edu.rutgers.se.components;

import java.util.concurrent.ExecutionException;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.Statement;

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
	
}
