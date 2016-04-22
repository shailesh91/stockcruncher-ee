package edu.rutgers.se.ma;

import edu.rutgers.se.beans.*;
import java.util.*;
	
public class MovingAverage{	

	public static double[] myAverage(List<HistStock> data, int window) //simple moving average
	{
		double[] dataA = new double[data.size()];
		int i=0;
		for(HistStock hs : data) {
			dataA[i++] = hs.close;
		}
		return myAverage(dataA,window);
	}
	
	public static double[] myEMAverage(List<HistStock> data, int window) //simple moving average
	{
		double[] dataA = new double[data.size()];
		int i=0;
		for(HistStock hs : data) {
			dataA[i++] = hs.close;
		}
		return myEMAverage(dataA,window);
	}

	public static double[] myAverage(double[] data, int window) //simple moving average
	{
		int range = data.length;
		double[] MA = new double[range-window+1];
		for (int j=window-1 ; j<range ; j++)
			{
				double tmp = 0;
				for (int i=j-window+1; i<j+1 ; i++)
					{
						tmp = tmp+data[i];        
					}
				MA[j-window+1] = tmp/window;
        
			}
		return MA;
    
	}

	public static double[] myEMAverage(double[] data, int window) //exponential moving average
	{
		double[] EMA = new double[data.length-window+1];
		double sum = 0;
		double multiplier = 2/(double)(window+1);
		for(int i=0 ; i< window ; i++)
			{
				sum = sum+data[i];        
			}
		EMA[0] = sum/window; //first term of EMA is basically the moving average for the first N terms
		for(int j=window ; j<data.length ; j++)
			{
				int index = j-window+1;
				EMA[index] = data[j] * multiplier + EMA[index-1] *(1-multiplier); 
			}
        
        
		return EMA;
	}

}
