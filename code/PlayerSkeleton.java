import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class PlayerSkeleton {
	
	public static int numberOfWeights = 7;
	public static double[] weights = {-0.5, -0.5, 0.5,-0.5,-0.5, -0.5,-0.5};// = new float[numberOfWeights];
	public static double[] lastWeights = {-0.5, -0.5, 0.5,-0.5,-0.5, -0.5,-0.5};// = new float[numberOfWeights];

	//public static double[] weights = {-0.12, -0.03, 0.03,-0.5,-1, -0.3,-0.3};// = new float[numberOfWeights];
	//public static double[] lastWeights = {-0.12, -0.03, 0.03,-0.5,-1, -0.3,-0.3};// = new float[numberOfWeights];

	public static int currentRowClearedSum = 0;
	public static double currentScore = 0;
	public static int round = 20;
	public static int sleepTime = 0;

	//int[] features = {colDiffSum, topHeight, numberOfRowsCleared, hasLost, numberOfHoles,meanHeightDiff,sumOfPitDepth};

	public static double learning_rate = 0.01;
	public static double learning_rate_multiplier = 0.8;
	public static double terminate_learning_rate = 0.00001;
	public static int currentRowsCleared = 0;
	public static int lastRowsCleared = 0;

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
	public void updateWeights(int index) {
		boolean positive;
		if(index%2 == 0) {
			positive = true;
		} else {
			positive = false;
		}
		if (positive) {
			weights[index/2] = weights[index/2] + learning_rate;
		} else {
			weights[index/2] = weights[index/2] - learning_rate;
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

		
	public static void main(String[] args) {
		State s = new State();
		//new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();

		boolean localMaximumReached = false;
		
		//do until local maximum is found
		while(!localMaximumReached && (learning_rate > terminate_learning_rate)) {
			
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
			}
			System.out.println("current completed "+currentRowClearedSum+" rows.");
			System.out.println("current score "+ new DecimalFormat("##.##").format(currentScore));


			
			int[] neighbours = new int[2*numberOfWeights];
			double[] neighboursScore = new double[2*numberOfWeights];
			
			//initialize neighbours
			for(int i = 0; i < neighbours.length; i++) {
				neighbours[i] = 0;
				neighboursScore[i] = 0;
			}
			

			//for every neighbour
			for(int j = 0; j < 2*numberOfWeights; j++) {
				//restore weights to the current weights
				//then perturb one of them as a neighbour	
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
					Searcher calculator = new Searcher();
					neighboursScore[j] += calculator.calculateHeuristics(s);
				}
				//System.out.println("neighbour "+ j + " completes "+neighbours[j]+" rows.");
			}
			
			p.getLastweights();//set back to the current node, for now

		
			// all neighbours are computed; find the maximum
			
			int bestNeighbour = -1;
			double currentBestValue = currentScore;
			//System.out.println("current node "+ currentRowClearedSum+" --- ");

			
			for(int k = 0; k < neighbours.length; k++) {
				if(neighboursScore[k] >= currentBestValue) {
					bestNeighbour = k;
					currentBestValue = neighboursScore[k];
					//System.out.println("k value " + k);

				}
			}
			if (bestNeighbour == -1) {
				System.out.println("The current node is better than all neighbours");
				if(learning_rate > terminate_learning_rate) {
					learning_rate = learning_rate*learning_rate_multiplier;
					System.out.println("learning rate decreases to" + learning_rate);

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
