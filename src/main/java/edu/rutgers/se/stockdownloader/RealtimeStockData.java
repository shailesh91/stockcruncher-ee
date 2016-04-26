package edu.rutgers.se.stockdownloader;

import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import edu.rutgers.se.beans.InstStock;
import edu.rutgers.se.beans.Stock;
import edu.rutgers.se.config.DatabaseManager;

public class RealtimeStockData {
	
	public static void collectData(){
		try {
			System.out.println("Realtime Stock Update");
			Class.forName("com.mysql.jdbc.Driver");
			Connection connection;
			connection = DriverManager.getConnection(DatabaseManager.URL + DatabaseManager.DATABASE_NAME,
					DatabaseManager.USER_NAME, DatabaseManager.PASSWORD);
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT id, stock_symbol FROM stocks WHERE init = 1 AND id IN (SELECT DISTINCT stock_id FROM portfolio_items)");
			List<Stock> allStocks = new ArrayList<Stock>();
			while(rs.next()){
		    	Stock st = new Stock();
				st.setId(rs.getInt("id"));
				st.setSymbol(rs.getString("stock_symbol"));
				allStocks.add(st);
		    }
			for(Stock s:allStocks){
				String symbol = s.getSymbol();
				int stockid = s.getId();
				String url = "http://finance.yahoo.com/d/quotes.csv?s=" + symbol + "&f=sd1t1l1v&e=.csv";
				URL yahoolive = new URL(url);
				URLConnection datalive = yahoolive.openConnection();
				Scanner input = new Scanner(datalive.getInputStream());
				System.out.println("Updating Live Stock Data for "+symbol);
				while (input.hasNext()) {
					String line = input.nextLine();
					String[] tokenslive = line.split(",");
					
					SimpleDateFormat from = new SimpleDateFormat("MM/dd/yyyy");
					SimpleDateFormat to = new SimpleDateFormat("yyyy-MM-dd");
					Date date = from.parse(tokenslive[1].substring(1, tokenslive[1].length() - 1));       // 01/02/2014
					String datefinal = to.format(date);     // 2014-02-01
					
					from = new SimpleDateFormat("h:mma");
					to = new SimpleDateFormat("HH:mm:ss");
					date = from.parse(tokenslive[2].substring(1, tokenslive[2].length() - 1));       
					String timefinal = to.format(date);     
					
					String query = "INSERT IGNORE INTO `inst_data`(`stock_id`, `inst_datetime`, `inst_price`, `volume`) VALUES ("+stockid+",'"+datefinal+" "+timefinal+"'," + tokenslive[3] + "," + tokenslive[4] + ")";
					statement.executeUpdate(query);
				}
				input.close();
			}
		    connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private String convertDate(String date) {
		String s1 = null, s2 = null, s3 = null, s4 = null;
		String datetoken[];
		String date1 = date.replace("\"", "");

		datetoken = date1.split("/");
		if (datetoken[0].length() == 1) {
			s1 = datetoken[2].concat("0");
			s2 = s1.concat(datetoken[0]);
		} else {
			s2 = datetoken[2].concat(datetoken[0]);
		}

		if (datetoken[1].length() == 1) {
			s3 = s2.concat("0");
			s4 = s3.concat(datetoken[1]);
		} else {
			s4 = s2.concat(datetoken[1]);
		}

		return s4;
	}
}
