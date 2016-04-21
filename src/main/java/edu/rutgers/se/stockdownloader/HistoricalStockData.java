package edu.rutgers.se.stockdownloader;

import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.time.DateUtils;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

import edu.rutgers.se.config.DatabaseManager;
import scala.annotation.meta.getter;

public class HistoricalStockData {
	public static void collectData() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connection;
			connection = DriverManager.getConnection(DatabaseManager.URL + DatabaseManager.DATABASE_NAME,
					DatabaseManager.USER_NAME, DatabaseManager.PASSWORD);
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT id, stock_symbol FROM stocks WHERE init = 0 AND id IN (SELECT DISTINCT stock_id FROM portfolio_items)");
			while(rs.next()){
				Date end = new Date();
				
				Calendar calendar = new GregorianCalendar();
				calendar.setTime(end);
				int yearEnd = calendar.get(Calendar.YEAR);
				int monthEnd = calendar.get(Calendar.MONTH) + 1;
				int dayEnd = calendar.get(Calendar.DAY_OF_MONTH);
				
				calendar.setTime(DateUtils.addDays(end, -365));
				int yearStart = calendar.get(Calendar.YEAR);
				int monthStart = calendar.get(Calendar.MONTH) + 1;
				int dayStart = calendar.get(Calendar.DAY_OF_MONTH);
				
			    int stockid = rs.getInt("id");
				String symbol = rs.getString("stock_symbol");
				
				String url = "http://real-chart.finance.yahoo.com/table.csv?" + "s=" + symbol + "&d=" + monthEnd
						+ "&e=" + dayEnd + "&f=" + yearEnd + "&g=d" + "&a=" + monthStart + "&b="
						+ dayStart + "&c=" + yearStart + "&ignore=.csv";
				
				URL yahoofin = new URL(url);
				URLConnection data = yahoofin.openConnection();
				Scanner input = new Scanner(data.getInputStream());
				input.nextLine();
				while (input.hasNext()) {
					String line = input.nextLine();
					String[] tokens = line.split(",");
					String query = "INSERT INGNORE INTO `hist_data`(`stock_id`, `hist_date`, `open_price`, `close_price`, `min_price`, `max_price`, `adj_close`, `volume`) "
							+ "VALUES ("+stockid+",'" + tokens[0] + "'," + tokens[1] + "," + tokens[4] + "," + tokens[3] + ","
							+ tokens[2] + "," + tokens[6] + "," + tokens[5] + ")";
					System.out.println(query); 
					statement.executeUpdate(query);
				}
				input.close();
			}
		    rs.close();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
