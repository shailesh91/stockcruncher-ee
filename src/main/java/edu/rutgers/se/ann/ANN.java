package edu.rutgers.se.ann;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.SupervisedTrainingElement;
import org.neuroph.core.learning.TrainingElement;
import org.neuroph.core.learning.TrainingSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;

import edu.rutgers.se.beans.Stock;
import edu.rutgers.se.config.DatabaseManager;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class ANN {

	private List<Double> historicalData;
	private List<String> historicalDate;
	private int trainingLength;
	private NeuralNetwork neuralNet;
	private int inputNum;
	private List<Double> resultData;
	private List<String> resultDate;
	private double nextData;
	private String nextDate = "predict day 2";
	private double resultNext;

	public ANN() {
		// Set up the basic parameters for MultiLayerPerceptron
		trainingLength = 10;
		int maxIterations = 10000;
		inputNum = 4;
		int outputNum = 1;
		int middleLayer = 9;
		neuralNet = new MultiLayerPerceptron(inputNum, middleLayer, outputNum);
		((BackPropagation) neuralNet.getLearningRule()).setMaxError(0.001);// 0-1
		((BackPropagation) neuralNet.getLearningRule()).setLearningRate(0.7);// 0-1
		((BackPropagation) neuralNet.getLearningRule()).setMaxIterations(maxIterations);// 0-1
	}

	public double predictNext2(double priceMax) {
		double[] priceTemp;
		double[] inputSet = new double[inputNum];
		double prediction = priceMax;

		//System.out.println("---------- Start training ----------");
		TrainingSet trainingSet = new TrainingSet();

		priceTemp = new double[trainingLength];
		for (int i = 0; i < trainingLength - 1; ++i) {
			priceTemp[i] = historicalData.get(historicalData.size() - trainingLength + 1 + i);
		}
		priceTemp[trainingLength - 1] = resultData.get(resultData.size() - 1);

		for (int i = 0; i < priceTemp.length; ++i) {
			priceTemp[i] = norm(priceTemp[i], priceMax);
		}
		for (int i = 0; i < trainingLength - inputNum; ++i) {
			for (int j = 0; j < inputNum; ++j) {
				inputSet[j] = priceTemp[i + j];
			}
			trainingSet.addElement(new SupervisedTrainingElement(inputSet, new double[] { priceTemp[i + inputNum] }));
		}

		neuralNet.learnInSameThread(trainingSet);
		//System.out.println("---------- Finish training ----------");

		//System.out.println("---------- Start testing ----------");
		TrainingSet testSet = new TrainingSet();

		for (int j = trainingLength - inputNum, i = 0; j < trainingLength; ++j, ++i) {
			inputSet[i] = priceTemp[j];
		}
		testSet.addElement(new TrainingElement(inputSet));

		for (TrainingElement testElement : testSet.trainingElements()) {
			neuralNet.setInput(testElement.getInput());
			neuralNet.calculate();
			Vector<Double> networkOutput = neuralNet.getOutput();
			for (double input : testElement.getInput()) {
				input = deNorm(input, priceMax);
				//System.out.println(input);
			}
			for (double output : networkOutput) {
				output = Math.round(deNorm(output, priceMax) * 100.0) / 100.0;
				nextData = output;
				prediction = output;
				//System.out.println("predict day 2: " + output);
			}
		}
		//System.out.println("---------- Finish testing ----------");
		return prediction;
	}

	public String predictHistory(Stock st) {

		double[] priceTemp;
		double priceMax;
		double[] inputSet = new double[inputNum];
		double[] priceAll;
		double prediction =0;

		historicalData = new ArrayList<Double>();
		historicalDate = new ArrayList<String>();
		
		resultData = new ArrayList<Double>();
		resultDate = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		// load the stock data corresponding to the symbol
		try {
			Connection connection;
			connection = DriverManager.getConnection(DatabaseManager.URL + DatabaseManager.DATABASE_NAME,
					DatabaseManager.USER_NAME, DatabaseManager.PASSWORD);

			Statement statement = connection.createStatement();
			ResultSet res = statement.executeQuery("SELECT * FROM hist_data WHERE stock_id = '" + st.getId() + "'");
			while (res.next()) {
				historicalData.add(res.getDouble("close_price"));
				historicalDate.add(res.getString("hist_date"));
			}
			connection.close();
		} catch (Exception e) {
			System.out.println("database operation error (loading).");
		}
		
		//historical data - close_price, hist_date

		// calculate the max closing price in history
		priceAll = new double[historicalData.size()];
		for (int i = 0; i < historicalData.size(); ++i) {
			priceAll[i] = historicalData.get(i);
		}
		priceMax = max(priceAll);

		for (int dateOffset = 0; dateOffset < historicalData.size() - trainingLength + 1; ++dateOffset) {
			File netFile = new File(st.getId()+"-"+ historicalDate.get(dateOffset + trainingLength - 1) + ".nnet");
			if (!netFile.exists()) {
				//Training Start
				TrainingSet trainingSet = new TrainingSet();
				
				priceTemp = new double[trainingLength];
				for (int i = 0; i < trainingLength; ++i) {
					priceTemp[i] = historicalData.get(i + dateOffset);
				}
				
				for (int i = 0; i < priceTemp.length; ++i) {
					priceTemp[i] = norm(priceTemp[i], priceMax);
				}
				for (int i = 0; i < trainingLength - inputNum; ++i) {
					for (int j = 0; j < inputNum; ++j) {
						inputSet[j] = priceTemp[i + j];
					}
					trainingSet.addElement(new SupervisedTrainingElement(inputSet, new double[] { priceTemp[i + inputNum] }));
				}
				neuralNet.learnInSameThread(trainingSet);
				neuralNet.save(st.getId()+"-"+ historicalDate.get(dateOffset + trainingLength - 1) + ".nnet");
			} else {
				neuralNet = NeuralNetwork.load(st.getId()+"-"+ historicalDate.get(dateOffset + trainingLength - 1) + ".nnet");
				//successful net load
			}

			// Start testing
			TrainingSet testSet = new TrainingSet();
			if (dateOffset < historicalData.size() - trainingLength + 1) {
				for (int j = trainingLength - inputNum, i = 0; j < trainingLength; ++j, ++i) {
					inputSet[i] = norm(historicalData.get(j + dateOffset), priceMax);
				}
				testSet.addElement(new TrainingElement(inputSet));

				for (TrainingElement testElement : testSet.trainingElements()) {
					neuralNet.setInput(testElement.getInput());
					neuralNet.calculate();
					Vector<Double> networkOutput = neuralNet.getOutput();
					for (double output : networkOutput) {
						output = Math.round(deNorm(output, priceMax) * 100.0) / 100.0;
						resultData.add(output);
						if (dateOffset == historicalData.size() - trainingLength) {
							resultDate.add("predict day 1");
							//System.out.println("predict day 1: " + output);
							sb.append("[{'lastten':["+historicalData.get(dateOffset)+",");
							for(int i = dateOffset + 1; i <  historicalData.size() ;i++){
								sb.append(","+historicalData.get(i));
							}
							sb.append("]}");
							
							prediction = resultData.get(resultData.size() - 1);
						} else {
							resultDate.add(historicalDate.get(trainingLength + dateOffset));
						}
					}
				}
			}
		}
		
		sb.append(",{'prediction':["+prediction+","+predictNext2(priceMax)+"]}]");
		return sb.toString();
	}
	
	// get the max input
	private double max(double[] nums) {
		double max = nums[0];
		for (int i = 1; i < nums.length; ++i) {
			if (nums[i] > max) {
				max = nums[i];
			}
		}
		return max;
	}

	// normalize the inputs
	private double norm(double num, double max) {
		return (num / max) * 0.8 + 0.1;
	}

	// denormalize the number
	private double deNorm(double num, double max) {
		return max * (num - 0.1) / 0.8;
	}

}
