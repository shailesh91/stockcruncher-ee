package edu.rutgers.se.ann;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.SupervisedTrainingElement;
import org.neuroph.core.learning.TrainingElement;
import org.neuroph.core.learning.TrainingSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;

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

	public void predictNext() {
		System.out.println("---------- Start predicting next day's prices ----------");
		double[] inputSet = new double[inputNum];
		double[] priceAll;
		double priceMax;

		
		historicalData = new ArrayList();
		historicalDate = new ArrayList();
		loadData(11);
		priceAll = new double[historicalData.size()];
		for (int i = 0; i < historicalData.size(); ++i) {
			priceAll[i] = historicalData.get(i);
		}
		priceMax = max(priceAll);

		File netFile = new File(
				"11-" + historicalDate.get(historicalDate.size() - 1) + ".nnet");
		if (netFile.exists()) {
			neuralNet = NeuralNetwork.load("11-"
					+ historicalDate.get(historicalDate.size() - 1) + ".nnet");
			System.out.println("Successfully load the neural net for 11-"
					+ historicalDate.get(historicalDate.size() - 1) + ".");

			TrainingSet testSet = new TrainingSet();
			for (int j = historicalData.size() - inputNum, i = 0; j < historicalData.size(); ++j, ++i) {
				inputSet[i] = norm(historicalData.get(j), priceMax);
			}

			testSet.addElement(new TrainingElement(inputSet));

			for (TrainingElement testElement : testSet.trainingElements()) {
				neuralNet.setInput(testElement.getInput());
				neuralNet.calculate();
				Vector<Double> networkOutput = neuralNet.getOutput();
				for (double output : networkOutput) {
					output = Math.round(deNorm(output, priceMax) * 100.0) / 100.0;
					resultNext = output;
					System.out.println("11: " + output);
				}
			}

		} else {
			System.out.println("ERROR: Net not exists.");
		}
		
		System.out.println("---------- End prediction ----------");
	}

	public void predictNext2(int symbolNum, double priceMax) {
		double[] priceTemp;
		double[] inputSet = new double[inputNum];

		System.out.println("---------- Start training ----------");
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
		System.out.println("---------- Finish training ----------");

		System.out.println("---------- Start testing ----------");
		TrainingSet testSet = new TrainingSet();

		for (int j = trainingLength - inputNum, i = 0; j < trainingLength; ++j, ++i) {
			inputSet[i] = priceTemp[j];
		}
		testSet.addElement(new TrainingElement(inputSet));

		for (TrainingElement testElement : testSet.trainingElements()) {
			neuralNet.setInput(testElement.getInput());
			neuralNet.calculate();
			Vector<Double> networkOutput = neuralNet.getOutput();
			System.out.println("Input is :");
			for (double input : testElement.getInput()) {
				input = deNorm(input, priceMax);
				System.out.println(input);
			}
			System.out.println("Output is :");
			for (double output : networkOutput) {
				output = Math.round(deNorm(output, priceMax) * 100.0) / 100.0;
				nextData = output;
				System.out.println("predict day 2: " + output);
			}
		}
		System.out.println("---------- Finish testing ----------");

		storeNext(symbolNum);
	}

	public void predictHistory() {

		double[] priceTemp;
		double priceMax;
		double[] inputSet = new double[inputNum];
		double[] priceAll;

			historicalData = new ArrayList();
			historicalDate = new ArrayList();
			resultData = new ArrayList();
			resultDate = new ArrayList();

			// load the stock data corresponding to the symbol
			loadData(11);

			// calculate the max closing price in history
			priceAll = new double[historicalData.size()];
			for (int i = 0; i < historicalData.size(); ++i) {
				priceAll[i] = historicalData.get(i);
			}
			priceMax = max(priceAll);

			for (int dateOffset = 0; dateOffset < historicalData.size() - trainingLength + 1; ++dateOffset) {
				File netFile = new File("11-"+ historicalDate.get(dateOffset + trainingLength - 1) + ".nnet");
				if (!netFile.exists()) {
					System.out.println("Net not exists.");
					System.out.println("---------- Start training ----------");
					TrainingSet trainingSet = new TrainingSet();
					System.out.println("The start date is " + historicalDate.get(dateOffset));
					System.out.println("The end date is " + historicalDate.get(dateOffset + trainingLength - 1));

					priceTemp = new double[trainingLength];
					for (int i = 0; i < trainingLength; ++i) {
						priceTemp[i] = historicalData.get(i + dateOffset);
					}
					// priceMax = max(priceTemp);
					// System.out.println("max: " + priceMax);
					for (int i = 0; i < priceTemp.length; ++i) {
						priceTemp[i] = norm(priceTemp[i], priceMax);
					}
					for (int i = 0; i < trainingLength - inputNum; ++i) {
						for (int j = 0; j < inputNum; ++j) {
							inputSet[j] = priceTemp[i + j];
						}
						trainingSet.addElement(
								new SupervisedTrainingElement(inputSet, new double[] { priceTemp[i + inputNum] }));
					}

					neuralNet.learnInSameThread(trainingSet);
					System.out.println("---------- Finish training ----------");
					neuralNet.save("11-"+ historicalDate.get(dateOffset + trainingLength - 1) + ".nnet");
					System.out.println("Successfully save the neural net for 11-"
							+ historicalDate.get(dateOffset + trainingLength - 1) + ".");
				} else {
					System.out.println("Net exists.");
					neuralNet = NeuralNetwork.load("11-"
							+ historicalDate.get(dateOffset + trainingLength - 1) + ".nnet");
					System.out.println("Successfully load the neural net for 11-"
							+ historicalDate.get(dateOffset + trainingLength - 1) + ".");
				}

				// Start testing
				System.out.println("---------- Start testing ----------");
				TrainingSet testSet = new TrainingSet();

				// // tried to use the same nerual net to predict several days,
				// but the result was not good enough
				// for (int i = 0; i < 5; ++i) {
				// for (int j = trainingLength - inputNum + i, k = 0; j <
				// trainingLength + i; ++j, ++k) {
				// inputSet[k] = norm(historicalData.get(j), priceMax);
				// }
				// testSet.addElement(new TrainingElement(inputSet));
				// }

				// did not predict tomorrow's price before, but now do
				// if (dateOffset < historicalData.size() - trainingLength) {
				if (dateOffset < historicalData.size() - trainingLength + 1) {
					for (int j = trainingLength - inputNum, i = 0; j < trainingLength; ++j, ++i) {
						inputSet[i] = norm(historicalData.get(j + dateOffset), priceMax);
					}
					testSet.addElement(new TrainingElement(inputSet));

					for (TrainingElement testElement : testSet.trainingElements()) {
						neuralNet.setInput(testElement.getInput());
						neuralNet.calculate();
						Vector<Double> networkOutput = neuralNet.getOutput();
						// System.out.println("Input is :");
						// for (double input : testElement.getInput()) {
						// input = deNorm(input, priceMax);
						// System.out.println(input);
						// }
						System.out.println("Output is :");
						for (double output : networkOutput) {
							output = Math.round(deNorm(output, priceMax) * 100.0) / 100.0;
							resultData.add(output);
							if (dateOffset == historicalData.size() - trainingLength) {
								resultDate.add("predict day 1");
								System.out.println("predict day 1: " + output);
							} else {
								resultDate.add(historicalDate.get(trainingLength + dateOffset));
								System.out.println(historicalDate.get(trainingLength + dateOffset) + ": " + output);
							}
						}
					}

				}

				System.out.println("---------- Finish testing ----------");

			}

			// store the predicted result into database
			storeResult(11);

			// predict the closing price of the day after tomorrow and store it
			// into database
			predictNext2(11, priceMax);


		// Experiments:
		// calculated
		// 31;3;2009;4084,76 -> 4121 Error=0.01 Rate=0.7 Iterat=100
		// 31;3;2009;4084,76 -> 4096 Error=0.01 Rate=0.7 Iterat=1000
		// 31;3;2009;4084,76 -> 4093 Error=0.01 Rate=0.7 Iterat=10000
		// 31;3;2009;4084,76 -> 4108 Error=0.01 Rate=0.7 Iterat=100000
		// 31;3;2009;4084,76 -> 4084 Error=0.001 Rate=0.7 Iterat=10000
	}

	private void loadData(int stock_id) {

		historicalData.clear();
		historicalDate.clear();

		try {

			Connection connection;

			connection = DriverManager.getConnection(DatabaseManager.URL + DatabaseManager.DATABASE_NAME,
					DatabaseManager.USER_NAME, DatabaseManager.PASSWORD);

			Statement statement = connection.createStatement();

			ResultSet res = statement.executeQuery("SELECT * FROM hist_data WHERE stock_id = '" + stock_id + "'");

			while (res.next()) {
				historicalData.add(res.getDouble("close_price"));
				historicalDate.add(res.getString("hist_date"));
			}

			connection.close();

		} catch (Exception e) {
			System.out.println("database operation error (loading).");
		}

	}

	public void storeResult(int symbolNum) {
		String action;
		for (int i = 0; i < resultData.size(); ++i) {
			if (i == resultData.size() - 1) {
				action = "UNKNOWN";
				String q = "INSERT INTO PredictionANN VALUES (11, '"
						+ resultDate.get(i) + "', " + resultData.get(i) + "," + 0 + ", '" + action + "')";
				System.out.println(q);
				//statement.executeUpdate();
			} else {
				if (resultData.get(i) < resultData.get(i + 1)) {
					action = "BUY";
				} else if (resultData.get(i) > resultData.get(i + 1)) {
					action = "SELL";
				} else {
					action = "HOLD";
				}
				String q = "INSERT INTO PredictionANN VALUES (11, '"
						+ resultDate.get(i) + "', " + resultData.get(i) + ","
						+ Math.round((resultData.get(i) - historicalData.get(i + trainingLength))
								/ historicalData.get(i + trainingLength) * 10000.0) / 100.0
						+ ", '" + action + "')";
				System.out.println(q);
				//statement.executeUpdate(q);
			}
		}
	}

	public void storeNext(int symbolNum) {
		String action;

		action = "UNKNOWN";
		String q = "INSERT INTO PredictionANN VALUES (11, '" + nextDate
				+ "', " + nextData + "," + 0 + ", '" + action + "')";
		System.out.println(q);

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
