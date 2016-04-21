package edu.rutgers.se.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Utilities {
	/**
	 * Returns sql format timestamp YYYYY-MM-DD HH:MM:SS
	 * @return time string with hour
	 */
	public static String getCurrentSQLDateString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		return sdf.format(new Date());
	}
	
	/**
	 * Returns current date string
	 * @return
	 */
	public static String getDataStampString() {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:MM:SS");
		return sdf.format(new Date());
	}
	
	/**
	 * Returns a string representation of formated time:
	 * 	SQL timestamp: 	YYYY-MM-DD HH:MM:SS
	 * 	SQL date		YYYY-MM-DD
	 * @param pMillis
	 * @param pFormat
	 * @return formatted time
	 */
	public static String getSQLTimeStamp(Long pMillis, String pFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat(pFormat);
		return sdf.format(new Date(pMillis));
	}
	
	public static String getSQLTimeStamp(Date pDate, String pFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat(pFormat);
		return sdf.format(pDate);
	}
	
	
	public static String getSQLDateTimeString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:MM:SS");
		return sdf.format(new Date());
	}
	/**
	 * Returns a legal Calendar instance of the specified date.
	 * @param pDateString
	 * @return
	 */
	public static Calendar getDateFromString(String pDateString) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
		try {
			cal.setTime(sdf.parse(pDateString));
		} catch (Exception e) {
			return null;
		}
		return cal;
	}
}
