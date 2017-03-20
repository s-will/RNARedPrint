/*
 * Copyright (c) 2016-present, Max Bannach, Sebastian Berndt, Thorsten Ehlers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package jdrasil.algorithms.upperbounds;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
/**
 * StochasticGreedyPermutationDecomposer.java
 * @author bannach
 */
import java.util.logging.Logger;


import jdrasil.algorithms.upperbounds.GreedyPermutationDecomposer.Algorithm;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import jdrasil.utilities.RandomNumberGenerator;
import jdrasil.utilities.logging.JdrasilLogger;
import jdrasil.utilities.JdrasilProperties;

/**
 * The Greedy-Permutation heuristic performs very well and can be seen as randomized algorithm as it breaks ties randomly.
 * Therefore, multiple runs of the algorithm produce different results and, hence, we can perform a stochastic search
 * by using the heuristic multiple times and reporting the best result. As the Greedy-Permutation heuristic implements
 * different algorithms, we can pick different algorithms in different runs. As the performance of these algortihms differ,
 * we choose them with different probabilities.
 * 
 * @param <T> the vertex type
 * @author Max Bannach
 * @author Thorsten Ehlers
 */
public class StochasticGreedyPermutationDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = -8256243005350278791L;

	/** Jdrasils Logger */
	private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

	/** The graph to be decomposed. */
	private final Graph<T> graph;
	
	/** The decomposition we try to compute */
	private TreeDecomposition<T> decomposition;

	/** The best permutation that is computed. */
	public List<T> permutation;
	
	/**
	 * The algorithm is initialized with a graph that should be decomposed.
	 * @param graph to be decomposed
	 */
	public StochasticGreedyPermutationDecomposer(Graph<T> graph) {
		this.graph = graph;
		this.decomposition = new TreeDecomposition<T>(graph);
		this.decomposition.createBag(graph.getVertices());
	}

	@Override
	public TreeDecomposition<T> call() throws Exception {

		// an upper bound that will be improved during the run of the algortihm
		int ub = graph.getVertices().size();

		// iterating sqrt(n) times, at least 100
		int itr = (int) Math.max(Math.sqrt(ub), 100);

		// each run will call the Greed-Permutation heuristic
		while (itr --> 0) {
			if (Thread.currentThread().isInterrupted()) throw new Exception();
			GreedyPermutationDecomposer<T> greedyPermutation = new GreedyPermutationDecomposer<T>(graph);

			// choose an algorithm at random
			// with probability 0.5 we choose fill-in, as this algorithm performs very well,
			// the other algorithms have probability 0.1
			double p = RandomNumberGenerator.nextDouble();
			if (p > 0.9) {
				greedyPermutation.setToRun(Algorithm.Degree);
			} else if (p > 0.8) {
				greedyPermutation.setToRun(Algorithm.DegreePlusFillIn);
			} else if (p > 0.7) {
				greedyPermutation.setToRun(Algorithm.SparsestSubgraph);
			} else if (p > 0.6) {
				greedyPermutation.setToRun(Algorithm.FillInDegree);
			} else if (p > 0.5) {
				greedyPermutation.setToRun(Algorithm.DegreeFillIn);
			} else {
				greedyPermutation.setToRun(Algorithm.FillIn);
			}

			// compute the decomposition
			TreeDecomposition<T> newDec = greedyPermutation.call(ub);

			// we we get one, and if this decomposition improves the currently best -> update bound
			if (newDec != null && newDec.getWidth() < ub) {
				ub = newDec.getWidth();
				LOG.info("new upper bound: " + ub);
				decomposition = newDec;
				permutation = greedyPermutation.getPermutation();
			}
			if(JdrasilProperties.timeout())
                          break;
		}

		// Create a copy of the current decomposition, and try to improve it. 
		// Prevents race condition if signal handler is triggered while improve is running
		LOG.info("trying to improve the decomposition");
		TreeDecomposition<T> tmp = decomposition.copy();
		tmp.improveDecomposition();
		if(tmp.getWidth() < decomposition.getWidth())
			decomposition = tmp;

		// done
		return decomposition;
	}

	/**
	 * Returns the elimination order computed by call().
	 * @return permutation as List
	 */
	public List<T> getPermutation() {
		return permutation;
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Heuristic;
	}
	
	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return decomposition;
	}
	
}
