package edu.rutgers.se.stockdownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.rutgers.se.config.DatabaseManager;

public class InitializeStock {
	
	public static void collectHistoricData(String symbol,Integer stockid) {
		try {
			Date end = new Date();
			
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(end);
			int yearEnd = calendar.get(Calendar.YEAR);
			int monthEnd = calendar.get(Calendar.MONTH);
			int dayEnd = calendar.get(Calendar.DAY_OF_MONTH);
			
			int yearStart = 2015;
			int monthStart = 1;
			int dayStart = 1;
			
			Class.forName("com.mysql.jdbc.Driver");
			Connection connection;
			connection = DriverManager.getConnection(DatabaseManager.URL + DatabaseManager.DATABASE_NAME,
					DatabaseManager.USER_NAME, DatabaseManager.PASSWORD);
			Statement statement = connection.createStatement();
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
				String query = "INSERT IGNORE INTO `hist_data`(`stock_id`, `hist_date`, `open_price`, `close_price`, `min_price`, `max_price`, `adj_close`, `volume`) VALUES ("+stockid+",'" + tokens[0] + "'," + tokens[1] + "," + tokens[4] + "," + tokens[3] + "," + tokens[2] + "," + tokens[6] + "," + tokens[5] + ")";
				statement.execute(query);
			}
			input.close();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void collectRealtimeData(String symbol, int stockid) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connection;
			connection = DriverManager.getConnection(DatabaseManager.URL + DatabaseManager.DATABASE_NAME,
					DatabaseManager.USER_NAME, DatabaseManager.PASSWORD);
			
			long end = System.currentTimeMillis()/1000;
			long start = System.currentTimeMillis()/1000 - 604800;
			
			String url = "https://finance-yql.media.yahoo.com/v7/finance/chart/"+symbol+"?period1="+start+"&period2="+end+"&interval=1m&indicators=quote&includeTimestamps=true&includePrePost=true&events=div%7Csplit%7Cearn";
			System.out.println(url);
			JSONObject json = readJsonFromUrl(url).getJSONObject("chart").getJSONArray("result").getJSONObject(0);
		    //System.out.println(json.get("meta"));
			JSONArray timeStamps = json.getJSONArray("timestamp");
			JSONArray volumes = json.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("volume");
			JSONArray close_prices = json.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("close");
		
		    for (int x = 0 ; x < timeStamps.length(); x++) {
		        String timestamp = timeStamps.get(x).toString();
		        String volume = volumes.get(x).toString();
		        String price = close_prices.get(x).toString();
		        Date d = new Date(Long.parseLong(timestamp) * 1000);
		        
		        SimpleDateFormat to = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String datetime = to.format(d);    
				Statement statement = connection.createStatement();
				String query ="INSERT IGNORE INTO `inst_data`(`stock_id`, `inst_datetime`, `inst_price`, `volume`) VALUES ("+stockid+",'"+datetime+"'," + price + "," + volume + ")";
				statement.execute(query);
		    }
			
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	  }

	  public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
	    InputStream is = new URL(url).openStream();
	    try {
	      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	      String jsonText = readAll(rd);
	      JSONObject json = new JSONObject(jsonText);
	      return json;
	    } finally {
	      is.close();
	    }
	  }
}
