import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.stream.IntStream;


public class PlayerSkeleton {
	
	public static int numberOfWeights = 4;

	//features = aggregateHeights, numberOfRowsCleared, numberOfHoles,colDiffSum)
	public static int numOfParents = 1000;
	public static int numOfRandomParents = 100;
	public static int numberOfReplacement = 300;
	public static int numberOfGenarations = 100;
	public static double mutateRate = 0.05;
	public static double mutateRange = 0.2;
	public static double weight0 = -0.5;
	
	public static double[][] weightsSet = new double[numOfParents][numberOfWeights];
	public static int[] fitness = new int[numOfParents];
	public static int fitnessForOne = 0;
	public static int[] randomParentsIndex = new int[numOfRandomParents];
	public static double[] offSpring = new double[numberOfWeights];
	public static int[] bestParents = new int[2];//use 2 parents to produce offspring
	public static int numberOfRowsCleared = 0;
	public static int goal = 10000000;// stop if 10 rounds of games clear 10 million lines

	public static int simulationRound = 10;	
	public static int sleepTime = 0;

	//implement this function to have a working system
	/**
	 * 
	 * @param s the current state
	 * @param legalMoves legal moves in the current state
	 * @param weights the weights to use
	 * @return in index of the move which has highest heuristics (higher the better)
	 */
	public int pickMove(State s, int[][] legalMoves, double[] weights) {
		int [][] legalMs = s.legalMoves();
		Searcher searcher = new Searcher();
		searcher.initializeWeights(weights);
		int best = searcher.search(s, legalMs);
		return best;
	}
	
	/**
	 * Initialize weights to randomized value between 0(inc) and 1 (exc)
	 */
	public void initializeWeightsSet() {
		Random rand = new Random();
		for(int j = 0; j < numOfParents; j++) {
			for (int i = 0; i < numberOfWeights; i++) {
				if(i != 1) {
					weightsSet[j][i] = (-1) * rand.nextDouble();
					//System.out.println(weightsSet[j][i]);
				} else {
					weightsSet[j][i] = rand.nextDouble();
					//System.out.println(weightsSet[j][i]);
				}
			}
		}
		System.out.println("Initialization completed.");

	}
	
	/**
	 * 
	 * @param parentIndex the index of a set of weights
	 * @return the fitness of the set of weights with the index of parentIndex
	 * fitness is defined as the sum of number of rows cleared in a number of round of games.
	 * In this case, 10 games
	 */
	public int calculateFitnessForOneParent(int parentIndex) {
		double[] parentWeights = weightsSet[parentIndex];
		fitnessForOne = 0;
		IntStream.range(0,simulationRound).parallel().forEach(i->{
		//for(int i = 0; i < simulationRound; i++) {
			State s = new State();
			//play until end to see how good the new weights are
			//s = new State();
			//new TFrame(s);
			while(!s.hasLost()) {
				s.makeMove(pickMove(s, s.legalMoves(), parentWeights));
				//s.draw();
				//s.drawNext(0,0);
			}
			fitnessForOne += s.getRowsCleared();
		//}
		 });
		
		//System.out.println("fitness for " + parentIndex + " is " + fitnessForOne);

		
		return fitnessForOne;
	}
	
	/**
	 * calculate the fitness values for all parents and save them
	 */
	public void calculateFitnessAllParents() {
		for(int i = 0; i < numOfParents; i++) {
			fitness[i] = calculateFitnessForOneParent(i);
		}
	}
	
	/**
	 * Randomly select parents before selecting the best parents to produce offsprings
	 */
	public void selectParentsRandom() {
		Random rand = new Random();
		for(int i = 0; i < numOfRandomParents; i ++) {
			randomParentsIndex[i] = rand.nextInt(numOfParents);
		}
	}
	
	/**
	 * Get the best parents from selected random parents and save them
	 */
	public void getBestParents() {
		double[] currentBest = {-9999, -9999};// initialize best to min values
		int[] currentParentIndex = {-1, -1};
		for(int i = 0; i < numOfRandomParents; i ++) {
			if(fitness[randomParentsIndex[i]] > currentBest[0]) {
				//this one is better than the best
				currentParentIndex[0] = randomParentsIndex[i];
				currentBest[0] = fitness[randomParentsIndex[i]];
			} else if (fitness[randomParentsIndex[i]] > currentBest[1]) {
				//this one is not better than the best, but better than the second best
				currentParentIndex[1] = randomParentsIndex[i];
				currentBest[1] = fitness[randomParentsIndex[i]];
			}
		}
		bestParents[0] = currentParentIndex[0];
		bestParents[1] = currentParentIndex[1];
		
	}
	
	/**
	 * Get the offspring of the best parents in randomly selected parents
	 * Possibly mutate the offspring
	 */
	public void calculateOffSpring() {
		double totalFitness = fitness[bestParents[0]] + fitness[bestParents[1]];
		double fitnessParentOne = fitness[bestParents[0]];
		double fitnessParentTwo = fitness[bestParents[1]];
		double weightMultiplierOne = fitnessParentOne/totalFitness;
		double weightMultiplierTwo = fitnessParentTwo/totalFitness;
		//offSpring[0] = weight0;
		for(int i = 0; i < numberOfWeights; i++) {
			offSpring[i] = weightsSet[bestParents[0]][i] * weightMultiplierOne + weightsSet[bestParents[1]][i] * weightMultiplierTwo;
			//System.out.println("offSpring "+ i + " is " + offSpring[i]);

		}
		mutate();
	}
	
	/**
	 * Change w by multiplying (1 + delta) to the vector.
	 * delta ranges from -0.2 to 0.2.
	 * The +/- sign is preserved.
	 */
	public void mutate() {
		Random rand = new Random();
		double flag = rand.nextDouble();
		if(flag < mutateRate) {
			System.out.println("mutate");

			boolean weightChangePositive;
			double delta = 0;
			for(int i = 0; i < numberOfWeights; i++ ) {
				weightChangePositive = rand.nextBoolean();
				delta = rand.nextDouble() * mutateRange;
				if(weightChangePositive) {
					//delta is positive, the new value must be valid
					offSpring[i] = offSpring[i] * (1 + delta);
					} else {
						offSpring[i] = offSpring[i] * (1 - delta);
					}	
			}
		}
	}
	
	/**
	 * Get the index of the parent with lowest fitness value
	 * @return the index
	 */
	public int getWeakestParentIndex() {
		int index = -1;
		int lowestFitness = 9999;//initialize lowest fitness to max
		for(int i = 0; i < numOfParents; i++) {
			if(fitness[i] < lowestFitness) {
				//a weaker one found
				lowestFitness = fitness[i];
				index = i;
			}
		}
		return index;
	}
	
	/**
	 *  Get the index of the parent with highest fitness value
	 * @return
	 */
	public int getBestParentIndex() {
		int index = -1;
		int highestFitness = -9999;//initialize highest fitness to min
		for(int i = 0; i < numOfParents; i++) {
			if(fitness[i] > highestFitness) {
				//a weaker one found
				highestFitness = fitness[i];
				index = i;
			}
		}
		return index;
	}
	
	/**
	 * Replace the parent with the current offSpring
	 * @param index
	 */
	public void replaceOneParent(int index) {
		for(int i = 0; i < numberOfWeights; i ++) {
			weightsSet[index][i] = offSpring[i];
		}
		fitness[index] = calculateFitnessForOneParent(index);
	}
	
	/**
	 * Generate offsprings and replace weak parents.
	 * Repeat for a number of times, in this case 300,
	 * which comprises 30% of the population.
	 */
	public void produceNextgeneration() {
		int weakestParentIndex = -1;
		for(int i = 0; i < numberOfReplacement; i++) {
			weakestParentIndex = getWeakestParentIndex();
			//System.out.println("weakestParentIndex is " + weakestParentIndex);

			selectParentsRandom();
			getBestParents();
			calculateOffSpring();
			replaceOneParent(weakestParentIndex);
		}	
	}
	
	/**
	 * Display the best weights after 1 generation
	 * @return the weights
	 */
	public double[] showBest() {
		NumberFormat formatterNum = new DecimalFormat("#0.0000");  
		int bestIndex = getBestParentIndex();
		double[] bestWeight = weightsSet[bestIndex];
		numberOfRowsCleared = fitness[bestIndex];
		for(int i = 0; i < numberOfWeights; i++) {
			System.out.println("W" + i + ": " + formatterNum.format(bestWeight[i]));
		}
		System.out.println("Number of rows cleared: "+ numberOfRowsCleared);
		return bestWeight;
	}
		
	public static void main(String[] args) {
		
		PlayerSkeleton p = new PlayerSkeleton();
		
		//initialize weights and fitness values
		p.initializeWeightsSet();
		p.calculateFitnessAllParents();
		
		try {
			
			//logger
	        LocalDateTime now = LocalDateTime.now();
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
	        String formatDateTime = now.format(formatter);
			PrintWriter writer;

			writer = new PrintWriter("Log-" + formatDateTime + ".txt", "UTF-8");
			writer.println("training log starts\n");
			
			
			for(int i = 0; i < numberOfGenarations; i++) {
				System.out.println("Generation: "+ i);
				writer.println("Generation: "+ i);

				p.produceNextgeneration();
				
				double[] bestWeight = p.showBest();
				
				//logger
				writer.println("--------------------");

				for(int j = 0; j < numberOfWeights; j++) {
					writer.println("W" + i + ": " + bestWeight[j]);

				}
				writer.println("Number of rows cleared: "+ numberOfRowsCleared);
				writer.println("--------------------");
				if(numberOfRowsCleared > goal) {
					break;
				}

			}
		
		System.out.println("All completed.");
		writer.println("\nLog ends.\n");
		writer.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
	}
		
	
}
