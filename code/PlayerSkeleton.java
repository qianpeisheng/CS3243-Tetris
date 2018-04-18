import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;


public class PlayerSkeleton {
	
	public static int numberOfWeights = 7;
	//int[] features = {colDiffSum, topHeight, numberOfRowsCleared, hasLost, numberOfHoles,meanHeightDiff,sumOfPitDepth};

	public static double[] weights = {-0.4, -0.4, 0.4,-0.4,-0.4, -0.4,-0.4};// = new float[numberOfWeights];

	public static double[] lastWeights = {-0.4, -0.4, 0.4,-0.4,-0.4, -0.4,-0.4};// = new float[numberOfWeights];

	//public static double[] weights = {-0.1242, -0.0307, 0.298,-0.4959,-1, -0.3232,-0.3178};// = new float[numberOfWeights];
	//public static double[] lastWeights = {-0.1242, -0.0307, 0.298,-0.4959,-1, -0.3232,-0.3178};// = new float[numberOfWeights];

	//public static double[] weights = {-0.12, -0.03, 0.03,-0.5,-1, -0.3,-0.3};// = new float[numberOfWeights];
	//public static double[] lastWeights = {-0.12, -0.03, 0.03,-0.5,-1, -0.3,-0.3};// = new float[numberOfWeights];

	public static int currentRowClearedSum = 0;
	public static double currentScore = 0;
	
	public static int simulationRound = 20;
	public static int maximumRandomStarts = 100;
	public static double learning_rate = 0.05;
	public static double learning_rate_multiplier = 0.2;
	public static double terminate_learning_rate = 0.002;
	public static double percentageOfNeiboursToVisit = 0.33;//0 to 1
	
	
	
	public static int currentRowsCleared = 0;
	public static int lastRowsCleared = 0;
	
	public static int maximumNeighbours = 2187;//3^7
	public static int[] update = new int[numberOfWeights];
	public static int sleepTime = 0;

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		int [][] legalMs = s.legalMoves();
		Searcher searcher = new Searcher();
		searcher.initializeWeights(weights);
		int best = searcher.search(s, legalMs);
		return best;
	}
	
	public void initializeWeights() {
		for (int i = 0; i < numberOfWeights; i ++) {
			weights[i] = -1;
		}
	}
	
	public int[] intToArray(int indexOfNeighbour) {
		int[] updateInfo = new int[numberOfWeights];
		for(int i = 0; i <numberOfWeights; i++ ) {
			updateInfo[i] = indexOfNeighbour % 3;
			indexOfNeighbour = indexOfNeighbour / 3;
			update[i] = updateInfo[i];
		}
		return updateInfo;
	}
	
	public boolean checkNextNew(int [] nextArray) {
		boolean isNew = false;
		for(int i = 0; i < numberOfWeights; i++ ) {
			if(nextArray[i] == update[i] && update[i] != 0) {
				isNew = true;
			}
			break;
		}
		return isNew;
	}
	
	/**
	 * Hill climbing search
	 * div 3 -> index 
	 * 0 no change, 1 -ve 2 +ve
	 * @param s
	 */
	public void updateWeights(int[] updateInfo) {

		Random rand = new Random();
		
		//at least 1 weight must change, so start from 1
		for(int i = 0; i < numberOfWeights; i++) {
			if(updateInfo[i] == 0) {
				//does not change
				weights[i] = weights[i] ;
			} else if(updateInfo[i] == 1){
				//make a -ve move
				weights[i] = weights[i] - learning_rate;
				
				if (weights[i] < 0 && i == 2) {
					// weights for number of rows cleared must be positive
					//if it goes to -ve, give it a random +ve value between 0 and 1

					weights[i] = rand.nextDouble();
				}
			} else {
				//make a +ve move	
				weights[i] = weights[i] + learning_rate;
				
				if (weights[i] > 0 && i != 2) {
					// weights except the one for number of rows cleared must be negative
					//if it goes to +ve, give it a random negative value between -1 and 0

					weights[i] = (-1) * rand.nextDouble();
				}
			}
		}

	}
	
	/**
	 * restore weights
	 */
	public void getLastweights() {
		for(int i = 0; i < weights.length; i++) {
			weights[i] = lastWeights[i];
		}
	}
	
	public void updateLastweights() {
		for(int i = 0; i < weights.length; i++) {
			lastWeights[i] = weights[i];
		}
	}
	
	public void printWeights() {
		for(int i = 0; i < numberOfWeights; i ++) {
			System.out.println(new DecimalFormat("##.#####").format(weights[i]));
		}
	}
	
	/**
	 * Randomly give weights.
	 * This is for random restart in hill climbing
	 */
	public void randomRestart() {
		for (int i = 0; i < numberOfWeights; i++) {
			Random rand = new Random();
			if(i != 2) {
				weights[i] = (-1) * rand.nextDouble();
			} else {
				weights[i] = rand.nextDouble();
			}

		}
		updateLastweights();
	}

		
	public static void main(String[] args) {
		
		NumberFormat formatterNum = new DecimalFormat("#0.0000");  
		
		int[] maximumRowsCleared = new int[maximumRandomStarts];
		double[][] weightsLearnt = new double[maximumRandomStarts][numberOfWeights];
		State s = new State();
		//new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		
		try {
	        LocalDateTime now = LocalDateTime.now();
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
	        String formatDateTime = now.format(formatter);
			PrintWriter writer;

			writer = new PrintWriter("Log-" + formatDateTime + ".txt", "UTF-8");
			writer.println("round: " + simulationRound);
			writer.println("training log starts\n");
			//writer.close();
		
		int randomStartCount = 0;
		//do until local maximum is found
		while(randomStartCount < maximumRandomStarts) {
			/*
			//reset it to zero
			currentRowClearedSum = 0;
			currentScore = 0;
			
			for(int i = 0; i < round; i++) {
				s = new State();
				//new TFrame(s);
				while(!s.hasLost()) {

					s.makeMove(p.pickMove(s, s.legalMoves()));
					//s.draw();
					//s.drawNext(0,0);

					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				currentRowClearedSum += s.getRowsCleared();
				Searcher calculator = new Searcher();
				currentScore += calculator.calculateHeuristics(s);
			}*/
			System.out.println("current completed "+currentRowClearedSum+" rows.");
			
			writer.println("current completed "+currentRowClearedSum+" rows.");
			//p.printWeights();
			
			for(int i = 0; i < numberOfWeights; i++) {
				writer.println("w" + i +": " + formatterNum.format(weights[i]));
			}

			//for every neighbour
			//there are 3^numberOfWeights number of neighbours
			//i.e. every weights can increase or decrease, or dont change
			//except that all weights do not change
			
			//randomly choose neighbours, and move to the first neighbour 
			//that is better
			//otherwise, state that it is local maximum and decrease learning rate
			
			boolean isFound = false;
			int neighbourIndex = 0;
			int rowsCleared = 0;
			//0 means no change. There must be some change			
			ArrayList<Integer>  mylist = new ArrayList<Integer>();
			for (int i = 0; i < maximumNeighbours; i ++) {
				mylist.add(i);
			}
			
			//randomize the order
			Collections.shuffle(mylist);
			
			//search only 20% of the neighbours to speed up search
			for(neighbourIndex = 0; neighbourIndex < maximumNeighbours * percentageOfNeiboursToVisit; neighbourIndex ++) {
				
				//restore weights to the current weights
				//then perturb one or more of them as a neighbour	
				p.getLastweights();
				int nextNeighbourIndex = mylist.get(neighbourIndex);
				int[] nextArray = p.intToArray(nextNeighbourIndex);
				boolean isNextNew = p.checkNextNew(nextArray);
				if(!isNextNew) {
					//next is now new, no need to check it
					continue;
				}
				p.updateWeights(nextArray);
				
				
				rowsCleared = 0;
				//repeat a few times to and sum the result
				for(int i = 0; i < simulationRound; i++) {
	
					//play until end to see how good the new weights are
					s = new State();
					//new TFrame(s);
					while(!s.hasLost()) {

						s.makeMove(p.pickMove(s, s.legalMoves()));
						//s.draw();
						//s.drawNext(0,0);

						try {
							Thread.sleep(sleepTime);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					rowsCleared += s.getRowsCleared();
					// find all neighbours of current
				}
				
				if(rowsCleared >= currentRowClearedSum) {
					isFound = true;
					break;
				}
				
			}
			
			p.getLastweights();//set back to the current node, for now			
			int bestNeighbour = -1;
			int currentBestValue = currentRowClearedSum;
			if (isFound) {
				bestNeighbour = mylist.get(neighbourIndex);
				currentBestValue = rowsCleared;
			}

			currentRowClearedSum = currentBestValue;
			
			if (!isFound) {
				System.out.println("The current node is better than all neighbours");
				System.out.println("in random hill climbing");
				writer.println("The current node is better than all neighbours");
				
				// if learning rate can decrease
				if(learning_rate > terminate_learning_rate) {
					learning_rate = learning_rate*learning_rate_multiplier;
					System.out.println("learning rate decreases to " + learning_rate);
					writer.println("learning rate decreases to " + learning_rate);

				//otherwise, restart if the restart limit is not met
				} else if(randomStartCount < maximumRandomStarts) {
					
					//record the current result before restart
					maximumRowsCleared[randomStartCount] = currentRowClearedSum;
					for(int i = 0; i < numberOfWeights; i ++) {
						weightsLearnt[randomStartCount][i] = lastWeights[i];
					}
					writer.println("-----------------------");
					writer.println("round "+ randomStartCount+ " rows cleared: " + currentRowClearedSum);
					for(int i = 0; i < numberOfWeights; i ++) {
						writer.println(formatterNum.format(weightsLearnt[randomStartCount][i]));
		
					}
					writer.println("-----------------------");
					
					
					
					randomStartCount ++;
					p.randomRestart();
					learning_rate = 0.01;//reset learning rate
					currentRowClearedSum = 0;
					System.out.println("random restart round " + randomStartCount);
					writer.println("random restart round " + randomStartCount);

				} else {
					break;
				}

			} else {
				//there is a better neighbour
				//set current to this neighbour, and continue the loop
				System.out.println(bestNeighbour +" is better ");
				p.printWeights();
				
				writer.println(bestNeighbour +" is better ");
				
				int[] nextArray = p.intToArray(bestNeighbour);
				
				p.updateWeights(nextArray);
				p.updateLastweights();// new one is better than last, so replace it

				
			}
		}
		
		int largestRowCleared = 0;
		int index = -1;
		for(int i = 0; i < randomStartCount; i++) {
			if (maximumRowsCleared[i] > largestRowCleared) {
				largestRowCleared = maximumRowsCleared[i];
				index = i;
			}
		}
		
		writer.println("Best result: rows cleared in " + simulationRound + " rounds is " + maximumRowsCleared[index]);
		for(int i = 0; i < numberOfWeights; i++) {
			writer.println("w" + i + " = " + formatterNum.format(weightsLearnt[index][i]));
		}
		
		
		
		System.out.println("All completed.");
		writer.println("\nLog ends.\n");
		writer.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
		
	
}
