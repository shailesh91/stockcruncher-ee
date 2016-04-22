package edu.rutgers.se.svm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import edu.rutgers.se.svm.libsvm.svm;
import edu.rutgers.se.svm.libsvm.svm_model;
import edu.rutgers.se.svm.libsvm.svm_node;
import edu.rutgers.se.svm.libsvm.svm_parameter;
import edu.rutgers.se.svm.libsvm.svm_problem;

public class SVMMain {
	private HashMap<svm_model, Integer> gModelDataPoints = new HashMap<svm_model, Integer>();

	public svm_parameter param;
	public BufferedReader reader = null;
	public double[] MyY;
	public double[][] MyX;
	private static final String filedir = "svm/";
	
	public static int countLines(String filename) throws IOException {
		int o = 0;
		try {
			Resource resource = new ClassPathResource(filedir+filename);
			
			BufferedReader input = new BufferedReader(
					new InputStreamReader (resource.getInputStream()));
			String nextLine;

			while ((nextLine = input.readLine()) != null) {
				o = o + 1;
			}
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return o;
	}

	public static double getMaxValue(double[] numbers) {
		double maxValue = numbers[0];
		for (int i = 1; i < numbers.length; i++) {
			if (numbers[i] > maxValue) {
				maxValue = numbers[i];
			}
		}
		return maxValue;
	}

	public static double getMinValue(double[] numbers) {
		double minValue = numbers[0];
		for (int i = 1; i < numbers.length; i++) {
			if (numbers[i] < minValue) {
				minValue = numbers[i];
			}
		}
		return minValue;
	}

	public static double[] DownSample(int down, double[] data) {
		double[] DownTest = new double[down];
		int interval = data.length / down;
		for (int q = 0; q < down; q++) {
			DownTest[q] = data[interval * q];

		}
		return DownTest;
	}

	public static double[] scale(double[] TrainFeat, double[] TestFeat) {
		double maxTrain = getMaxValue(TrainFeat);
		double minTrain = getMinValue(TrainFeat);
		double maxTest = getMaxValue(TestFeat);
		double minTest = getMinValue(TestFeat);
		double[] scaledata = new double[TestFeat.length];
		for (int r = 0; r < TestFeat.length; r++) {
			scaledata[r] = ((double) Math
					.round(100 * (minTrain + (maxTrain - minTrain) * (TestFeat[r] - minTest) / (maxTest - minTest))))
					/ 100;
		}
		return scaledata;

	}

	public svm_model svmTrain(String filename) throws IOException {

		int count = 150;
		int NumFeat = countLines(filename);
		MyX = new double[count][NumFeat];// the attributes
		int ColNum = 0;
		System.out.println(filename);
		// reading one instance of cup & handle
		try {
			Resource resource = new ClassPathResource(filedir+filename);
			BufferedReader input = new BufferedReader(
					new InputStreamReader(resource.getInputStream()));
			String Line = null;
			while ((Line = input.readLine()) != null) {
				MyX[0][ColNum] = Double.parseDouble(Line);
				ColNum = ColNum + 1;
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		NumFeat = ColNum;

		// make a training set with the first 50 rows being the cup&handle with
		// added noise,
		// and the last 100 rows y=x and y=-x with added noise
		// Also set Y equal to 1 for cup and handle, and -1 for y=x and y=-x
		Random randomno = new Random();
		MyY = new double[count]; // +-1
		MyY[0] = 1;
		for (int i = 1; i < 50; i++) {
			for (int j = 0; j < NumFeat; j++) {
				MyX[i][j] = MyX[0][j] + 0.01 * i * (randomno.nextGaussian());
				MyY[i] = 1;
			}
		}
		for (int i = 50; i < 100; i++) {

			for (int j = 0; j < NumFeat; j++) {
				MyX[i][j] = (i - 50) + 0.0001 * i * (randomno.nextGaussian());
				MyY[i] = -1;
			}
		}
		for (int i = 100; i < 150; i++) {

			for (int j = 0; j < NumFeat; j++) {
				MyX[i][j] = (100 - i) + 0.0001 * i * (randomno.nextGaussian());
				MyY[i] = -1;
			}
		}
		svm_problem myprob = new svm_problem();
		myprob.l = count;
		myprob.y = MyY;
		myprob.x = new svm_node[myprob.l][NumFeat];
		for (int k = 0; k < count; k++) {
			for (int y = 0; y < NumFeat; y++) {
				svm_node node = new svm_node();
				node.index = y + 1;
				node.value = MyX[k][y];
				myprob.x[k][y] = node;
			}
		}
		// changing SVM parameters:
		param = new svm_parameter();
		/*
		 * param.svm_type=svm_parameter.C_SVC;
		 * param.kernel_type=svm_parameter.RBF; param.gamma=0.5; param.nu=0.5;
		 * param.cache_size=20000; param.C=1; param.eps=0.001; param.p=0.1;
		 */

		param.probability = 1;
		param.gamma = 0.5;
		param.nu = 0.5;
		param.C = 1;
		param.svm_type = svm_parameter.C_SVC;
		param.kernel_type = svm_parameter.LINEAR;
		param.cache_size = 20000;
		param.eps = 0.001;
		// check if the parameters are feasible
		String check = svm.svm_check_parameter(myprob, param);
		double[] target = new double[MyY.length];

		svm.svm_cross_validation(myprob, param, 3, target);
		double correctCounter = 0;
		for (int i = 0; i < target.length; i++) {
			if (target[i] == MyY[i]) {
				correctCounter++;
			}
		}
		System.out.println("Cross Validation result is " + correctCounter);
		System.out.println("Check parameters is " + check);
		svm_model mymodel = svm.svm_train(myprob, param);
		return mymodel;
	}

	public double svmTest(double[] TestFeatures, svm_model svm_model, int NumDown, int range) throws IOException {
		// Reading the closing price from the csv file:
		/*
		 * int NumLines = countLines(filename); double [] TestFeatures = new
		 * double[NumLines]; int TestFileSize = 0; double v=0.0; try { //
		 * CSVReader reader = new CSVReader(new FileReader(filename)); String []
		 * nextLine; /* while ((nextLine = reader.readNext()) != null) {
		 * TestFeatures[TestFileSize] = Double.parseDouble(nextLine[3]);
		 * TestFileSize = TestFileSize+1; }
		 * 
		 * } catch(Exception e) { e.printStackTrace(); }
		 */
		// check the range of the stock the user is interested in
		double v = 0.0;
		if (range > TestFeatures.length || range < NumDown) {
			// have to check this with what is required
			System.out.println("error in range asked");
			System.exit(0);
		} else {
			// data1 will include the range asked by the user
			double[] data1 = new double[range];
			for (int z = 0; z < range; z++) {
				data1[z] = TestFeatures[TestFeatures.length - range + z];
			}
			// now the data at the range requested by user will be downsampled
			// to the number of data samples in the model
			double[] TestData = null;
			TestData = DownSample(NumDown, data1);
			// scaling the data:
			double[] scaleTestFeat = new double[TestData.length];
			scaleTestFeat = scale(MyX[0], TestData);
			for (int s = 0; s < scaleTestFeat.length; s++) {
				System.out.println(scaleTestFeat[s]);
			}
			v = evaluate(TestData, svm_model);
		}
		
		// System.out.println("The predicted class is " + v);
		return v;
	}

	public double evaluate(double[] features, svm_model model) {
		svm_node[] testnode = new svm_node[features.length];
		for (int i = 0; i < features.length; i++) {
			svm_node node = new svm_node();
			node.index = i;
			node.value = features[i];

			testnode[i] = node;
		}
		double v = svm.svm_predict(model, testnode);

		return v;
	}

	private SVMMain() throws IOException
	{	
		gModels = new HashMap<Integer,svm_model>();
		
		
		// this range and the stock selected are input by the user, specifying the number of days he/she's interested in
		
		int range = 40; // user input
		
		// String TestFileName = "/Users/parishad/term4/SoftwareEngineering/HW3/histdataApple.csv";
		//check to see if it's cup and handle:
		svm_model MyModel = new svm_model();
		
		// int NumLines = mymain.countLines("/Users/parishad/term4/SoftwareEngineering/MyProject/cup21.txt");
		MyModel = this.svmTrain("cup21.txt");
		gModels.put(1, MyModel);
		// double check_cup_handle = pari.mySvmTest(TestFileName,MyModel,NumLines,range);
		//check to see if it's ascending triangle
		svm_model MyModel1 = new svm_model();
		// int NumLines1 = mymain.countLines("/Users/parishad/term4/SoftwareEngineering/MyProject/AsTri21.txt");
		MyModel1 = this.svmTrain("AsTri21.txt");
		gModels.put(2, MyModel1);
		// double check_as_tri = pari.mySvmTest(TestFileName,MyModel1,NumLines1,range);
		// check to see if it's descending triangle
		svm_model MyModel2 = new svm_model();
		// int NumLines2 = mymain.countLines("/Users/parishad/term4/SoftwareEngineering/MyProject/DesTri21.txt");
		MyModel2 = this.svmTrain("DesTri21.txt");
		gModels.put(3, MyModel2);
		// double check_des_tri = pari.mySvmTest(TestFileName,MyModel2,NumLines2,range);
		// check to see if it's head and shoulder
		svm_model MyModel3 = new svm_model();
		MyModel3 = this.svmTrain("h&s21.txt");
		gModels.put(4, MyModel3);
		// double check_hs = pari.mySvmTest(TestFileName,MyModel3,NumLines3,range);
		//check to see if it's rounding bottom
		svm_model MyModel4 = new svm_model();
		// int NumLines4 = mymain.countLines("/Users/parishad/term4/SoftwareEngineering/MyProject/RoundBott.txt");
		MyModel4 = this.svmTrain("RoundBott.txt");
		gModels.put(5, MyModel4);
		// double check_rb = pari.mySvmTest(TestFileName,MyModel4,NumLines4,range);
		//check to see if it's double tops
		svm_model MyModel5 = new svm_model();
		//int NumLines5 = mymain.countLines("/Users/parishad/term4/SoftwareEngineering/MyProject/DoubleTops.txt");
		MyModel5 = this.svmTrain("DoubleTops.txt");
		gModels.put(6, MyModel5);
		// double check_doubtop = pari.mySvmTest(TestFileName,MyModel5,NumLines5,range);
		//check to see if it's double tops
		svm_model MyModel6 = new svm_model();
		// int NumLines6 = mymain.countLines("/Users/parishad/term4/SoftwareEngineering/MyProject/DoubleBotts.txt");
		MyModel6 = this.svmTrain("DoubleBotts.txt");
		gModels.put(7, MyModel6);
		
		//gLogger.log("TRAINING COMPLETED", LOG_TYPE.GRAL);
		
	}

	public String getModel(List<Double> pData) {
		return null;
	}

	private HashMap<Integer, svm_model> gModels;

	public HashMap<Integer, svm_model> getGmodels() {
		return this.gModels;
	}

	private static SVMMain instance;

	//private Logger gLogger;

	public static SVMMain GetInstance() {
		if (SVMMain.instance == null) {
			try {
				SVMMain.instance = new SVMMain();
			} catch (Exception e) {
				System.out.println("FAILED TO START THE SVM");
			}
		}
		return SVMMain.instance;
	}
}
