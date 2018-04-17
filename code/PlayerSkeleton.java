import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class PlayerSkeleton {
	
	public static int numberOfWeights = 7;
	public static double[] weights = {-0.5, -0.5, 0.5,-0.5,-0.5, -0.5,-0.5};// = new float[numberOfWeights];

	public static double[] lastWeights = {-0.5, -0.5, 0.5,-0.5,-0.5, -0.5,-0.5};// = new float[numberOfWeights];

	//public static double[] weights = {-0.1242, -0.0307, 0.298,-0.4959,-1, -0.3232,-0.3178};// = new float[numberOfWeights];
	//public static double[] lastWeights = {-0.1242, -0.0307, 0.298,-0.4959,-1, -0.3232,-0.3178};// = new float[numberOfWeights];

	//public static double[] weights = {-0.12, -0.03, 0.03,-0.5,-1, -0.3,-0.3};// = new float[numberOfWeights];
	//public static double[] lastWeights = {-0.12, -0.03, 0.03,-0.5,-1, -0.3,-0.3};// = new float[numberOfWeights];

	public static int currentRowClearedSum = 0;
	public static double currentScore = 0;
	public static int round = 100;
	public static int sleepTime = 0;

	//int[] features = {colDiffSum, topHeight, numberOfRowsCleared, hasLost, numberOfHoles,meanHeightDiff,sumOfPitDepth};

	public static double learning_rate = 0.01;
	public static double learning_rate_multiplier = 0.5;
	public static double terminate_learning_rate = 0.00001;
	public static int currentRowsCleared = 0;
	public static int lastRowsCleared = 0;
	
	public static int currentNodeValue;

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
	
	
	/**
	 * Hill climbing search
	 * div 2 -> index 
	 * %2 = 0 -> +ve; %2 = 1 -> -ve
	 * @param s
	 */
	public void updateWeights(int indexOfNeighbour) {
		int[] isUpdate = new int[numberOfWeights];
		for(int i = 0; i <numberOfWeights; i++ ) {
			isUpdate[i] = indexOfNeighbour % 3;
			indexOfNeighbour = indexOfNeighbour / 3;
		}
		
		//at least 1 weight must change, so start from 1
		for(int i = 1; i < numberOfWeights; i++) {
			if(isUpdate[i] == 0) {
				//make a negative change 
				weights[i] = weights[i] ;
			} else if(isUpdate[i] == 1){
				weights[i] = weights[i] - learning_rate;
			} else {
				weights[i] = weights[i] + learning_rate;
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
			System.out.println(new DecimalFormat("##.####").format(weights[i]));
		}
	}

		
	public static void main(String[] args) {
		State s = new State();
		//new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		p.currentNodeValue = 0;

		boolean localMaximumReached = false;
		
		//do until local maximum is found
		while(!localMaximumReached && (learning_rate > terminate_learning_rate)) {
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
			currentRowClearedSum = PlayerSkeleton.currentNodeValue;
			System.out.println("current completed "+currentRowClearedSum+" rows.");
			//System.out.println("current score "+ new DecimalFormat("##.##").format(currentScore));
			p.printWeights();


			
			int[] neighbours = new int[(int) Math.pow(3,numberOfWeights)];
			double[] neighboursScore = new double[(int) Math.pow(3,numberOfWeights)];
			
			//initialize neighbours
			for(int i = 0; i < neighbours.length; i++) {
				neighbours[i] = 0;
				neighboursScore[i] = 0;
			}
			

			//for every neighbour
			//there are 3^numberOfWeights number of neighbours
			//i.e. every weights can increase or decrease, or dont change
			//except that all weights do not change
			for(int j = 0; j < Math.pow(3, numberOfWeights); j++) {
				
				//restore weights to the current weights
				//then perturb one or more of them as a neighbour	
				p.getLastweights();
				p.updateWeights(j);
				//repeat a few times to and sum the result
				for(int i = 0; i < round; i++) {
	
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
					neighbours[j] += s.getRowsCleared();// find all neighbours of current
					//Searcher calculator = new Searcher();
					//neighboursScore[j] += calculator.calculateHeuristics(s);
				}
			}
			
			p.getLastweights();//set back to the current node, for now

		
			// all neighbours are computed; find the maximum
			
			int bestNeighbour = -1;
			int currentBestValue = currentRowClearedSum;
			//System.out.println("current node "+ currentRowClearedSum+" --- ");

			
			for(int k = 0; k < neighbours.length; k++) {
				if(neighbours[k] >= currentBestValue) {
					bestNeighbour = k;
					currentBestValue = neighbours[k];
					//System.out.println("k value " + k);

				}
			}
			PlayerSkeleton.currentNodeValue = currentBestValue;
			if (bestNeighbour == -1) {
				System.out.println("The current node is better than all neighbours");
				if(learning_rate > terminate_learning_rate) {
					learning_rate = learning_rate*learning_rate_multiplier;
					System.out.println("learning rate decreases to " + learning_rate);

				} else {
					localMaximumReached = true;
					break;
				}

			} else {
				//there is a better neighbour
				//set current to this neighbour, and continue the loop
				System.out.println("neighbour "+ bestNeighbour + " scores "+new DecimalFormat("##.##").format(neighboursScore[bestNeighbour]));
				System.out.println(bestNeighbour +" is better ");
				p.updateWeights(bestNeighbour);
				p.updateLastweights();// new one is better than last, so replace it

				
			}
		}
		System.out.println("All completed.");
	}
	
}
