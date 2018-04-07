import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class Searcher {
	
	public static int numberOfWeights = 7;
	public static int numberOfFeatures = 7;
	public static int numOfCols = 10;
	public static int numOfRows = 20;
	
	public static double[] weights = new double[numberOfWeights];
		
	public int search(State s, int[][] legalMoves) {
		int searchSpace = legalMoves.length;
		double best = -99999;// initialize to min; find max
		int index = 0;
		double[] heuristicsArray = new double[searchSpace];
		for (int i = 0; i < searchSpace; i++) {
			double heuristic = pickMoveExp(s, i);
			heuristic += getClearedLines(s);
			heuristicsArray[i] = heuristic;
			//System.out.println(" heuristic " + heuristic);
			if(best < heuristic) {
				best = heuristic;
				//index = i;
				//System.out.println(" best " + heuristic);
				//System.out.println(" index " + index);
			} 
		}
		List<Integer> list = new ArrayList<Integer>();
		for(int i = 0; i < searchSpace; i++) {
			if(heuristicsArray[i] == best) {
				list.add(i);
			}
		}
		
		Random rand = new Random();// randomly choose the index if they all have the same heuristics value

		int  n = rand.nextInt(list.size());
		index = list.get(n);
		
		//System.out.println("search complete, index " + index);
		return index;//return the index of legalMoves with lowest heuristics
	}
	
	public double pickMoveExp(State s, int i) {
		State sCopy = new State();
		sCopy.lost = s.lost;
		sCopy.label = s.label;
		sCopy.setTurnNumber(s.getTurnNumber());
		sCopy.cleared = s.cleared;
		sCopy.setField(s.getFieldClone());
		sCopy.top = s.getTopClone();
		sCopy.nextPiece = s.getNextPiece();
		sCopy.makeMove(i);
		double h = calculateHeuristics(sCopy);
		return h;
				
	}
	
	public int getClearedLines(State s) {
		//System.out.println(" clear " + s.getRowsCleared());

		return s.getRowsCleared();
	}
	
	public double calculateHeuristics(State s) {
		
		int colDiffSum = colDiffSum(s);
		int topHeight = getTopHeight(s);
		int hasLost = getHasLost(s);
		int numberOfRowsCleared = getClearedLines(s);
		int numberOfHoles = getNumberOfHoles(s);
		double meanHeightDiff = getMeanHeightDiff(s);
		int sumOfPitDepth = getAllPitDepth(s);

		double[] features = getAllFeatures(colDiffSum, topHeight, numberOfRowsCleared,hasLost, numberOfHoles, meanHeightDiff, sumOfPitDepth);
		
		double heurisitics = MultiplyFeaturesToWeights(features, weights);
		
		return heurisitics;
		
	}
	
	
	public void initializeWeights(double[] weights2) {
		weights = weights2;
	}
	
	/**
	 * a helper function that combine two arrays
	 * @param a
	 * @param b
	 * @return
	 */
    public int[] combine(int[] a, int[] b){
        int length = a.length + b.length;
        int[] result = new int[length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
    
    /**
     * Multiply each feature to the corresponding weight, and sum them
     * @param features
     * @param weights
     * @return the heuristic
     */
    public double MultiplyFeaturesToWeights(double[] features, double[] weights) {
    	double heuristic = 0;
    	int length = features.length;
    	for(int i = 0; i < length; i++) {
    		heuristic += features[i] * weights[i];
    	}
    	
    	return heuristic;
    }
	
    /**
     * Combine all features, ie phi 1 to phi 21 in the project guide
     * @param colHeights
     * @param colDifferences
     * @param topHeight
     * @return an array of length 21 of all features as int
     */
	public double[] getAllFeatures (int colDiffSum, int topHeight, int numberOfRowsCleared, int hasLost, int numberOfHoles, double meanHeightDiff, int sumOfPitDepth) {
		double[] features = {colDiffSum, topHeight, numberOfRowsCleared, hasLost, numberOfHoles, meanHeightDiff, sumOfPitDepth};
		return features;
	}
	
	/**
	 * Get the number of holes
	 * Holes are those empty locations with their block above occupied
	 * there maybe more than 1 block above, as long as one of them is occupied, it counts
	 * @param s
	 * @return
	 */
	public int getNumberOfHoles(State s) {
		int[] colHeights = getColHeights(s);
		int maxColHeight = getTopHeight(s);
		int numberOfHoles = 0;
		for(int i = 0; i < maxColHeight; i++) {
			// for every row
			// need to access at least row i + 1
			for(int j = 0; j < s.getField()[i].length; j++) {
				//for this block[i][j] at row i, col j
				if(s.getField()[i][j] == 0) {
					//it is empty
					//need to know if any block above is occupied
					boolean isHole = false;
					for (int k = i + 1; k < colHeights[j]; k++ ) {
						//the highest covering block is the one at row colHeights[j] - 1
						//height at the 0th row is 1
						// it is in the same column j
						if(s.getField()[k][j] != 0) {
							isHole = true;
							break;
						}
					}
					if(isHole) {
						//it is covered somewhere above
						numberOfHoles += 1;
					}
				}
			}
		}
		//int[] numberOfHolesArray = new int[1];
		//numberOfHolesArray[0] = numberOfHoles;
	//	System.out.println(" Holes " + numberOfHoles);

		return numberOfHoles;
	}
	
	/**
	 * Get the heights of all columns, ie phi 1 to phi 10 in the project guide
	 * There are 10 columns, therefore 10 heights
	 * @param s the current State
	 * @return an array of 10 ints
	 */
	public int[] getColHeights(State s) {
		int[] colHeights = new int[numOfCols];

		for(int i = 0; i < numOfCols; i++) {
			colHeights[0] = s.getTop()[i];
		}
		return colHeights;
	}
	 
	
	/**
	 * Get the height of the hightest column, ie. phi 20 in the project guide
	 * @param s the current State
	 * @return the highest height, an int
	 */
	public int getTopHeight(State s) {
		int max = 0;
		for(int i = 0; i < numOfCols; i++) {
			if(max < s.getTop()[i]) {
				max = s.getTop()[i];
			}
		}
		
		//int[] maxArray = new int[1];
		//maxArray[0] = max;
		return max;

	}
	
	/**
	 * Get the difference between columns, ie phi 11 to phi 19 in the project guide
	 * There are 10 columns, therefore 9 absolute differences between them
	 * @param s the current State
	 * @return an array of 9 ints
	 */
	public int[] getColDifferences(State s) {
		int[] colDiff = new int[numOfCols - 1];
		for(int i = 1; i < numOfCols; i++) {
			colDiff[i-1] = Math.abs(s.getTop()[i-1] - s.getTop()[i]);
		}
		return colDiff;
		
	}
	
	/**
	 * Calculate the sum of difference of column heights.
	 * The smaller, the better
	 * @param s
	 * @return
	 */
	public int colDiffSum(State s) {
		int[] colDiff = getColDifferences(s);
		int sum = IntStream.of(colDiff).sum();
		return sum;
	}
	
	/**
	 * Get whether the state has lost
	 * The smaller, the better
	 * @param s
	 * @return 1 if lost, 0 if not
	 */
	public int getHasLost(State s) {
		if (s.lost) {
			return 1;
		} else {
			return 0;
		}
	}
	
	/**
	 * get the difference of the 
	 * @param s
	 * @return
	 */
	public float getMeanHeightDiff(State s) {
		int[] heights =  getColHeights(s);
		int sum = IntStream.of(heights).sum();
		float averageHeight = sum/heights.length;
		int average = 0;
		for(int i = 0; i < heights.length; i++) {
			average += Math.abs(heights[i] - averageHeight);
		}
		average = average/heights.length;
		return average;
		
	}
	
	/**
	 * Further penalize actions that create deep pits
	 * @param s
	 * @return
	 */
	public int getAllPitDepth(State s) {
		int [] colDiff = getColDifferences(s);
		int sum = 0;
		for(int i = 0; i < colDiff.length; i++) {
			if (colDiff[i] > 1) {
				sum += colDiff[i];
			}
		}
		return sum;
	}
	

}
