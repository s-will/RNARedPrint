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
package jdrasil.algorithms;

import java.math.BigInteger;
import java.util.logging.Logger;


import jdrasil.algorithms.exact.CopsAndRobber;
import jdrasil.algorithms.exact.SATDecomposer;
import jdrasil.algorithms.exact.SATDecomposer.Encoding;
import jdrasil.algorithms.lowerbounds.MinorMinWidthLowerbound;
import jdrasil.algorithms.preprocessing.GraphReducer;
import jdrasil.algorithms.preprocessing.GraphSplitter;
import jdrasil.algorithms.upperbounds.StochasticGreedyPermutationDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import jdrasil.sat.Formula;
import jdrasil.utilities.logging.JdrasilLogger;

/**
 * This class implements a hand-crafted algorithm that uses various of the other algorithms to compute an optimal
 * tree-decomposition. 
 * 
 * Using call() will invoke the following:
 *   a) the graph will be partitioned into its connected components, which are treated separately in the following
 *   b) the graph will be reduced by preprocessing
 *   c) and upper bound will be computed by stochastic-min fill
 *   d) a lower bound will be computed by minor-min-width
 *   e) if ub == lb the computation is done
 *   f) if n^ub is smaller then some threshold the optimal solution will be computed by the dynamic cops-and-robber game
 *   g) otherwise the optimal solution will be computed by a SAT-Encoding, either serial or parallel
 *   h) finally, all computed decompositions are merged
 *   
 * @param <T> vertex type
 * @author Max Bannach
 */
public class ExactDecomposer<T extends Comparable<T>> implements TreeDecomposer<T> {

	/** Jdrasils Logger */
	private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

	/** The graph that we wish to decompose. */
	private final Graph<T> graph;
		
	/** If the graph has less then <value> vertices, then the instance may be solved by a game of Cops and Robber. */
	private final int COPS_VERTICES_THRESHOLD = 25;
	
	/** If the graph has a treewidth of less then <value>, then the instance may be solved by a game of Cops and Robber. */
	private final int COPS_TW_THRESHOLD = 8;
	
	/**
	 * Default constructor to initialize data structures. 
	 * @param graph – the graph that should be decomposed
	 */
	public ExactDecomposer(Graph<T> graph) {
		this.graph = graph;
	}
	
	/**
	 * Compute a tree-decomposition of a connected component of the graph.
	 * This method is the core of the algorithm invoked by @see call(), it will
	 * a) use preprocessing to reduce the size of the component
	 * b) compute lower and upper bounds of the component
	 * c) compute an optimal tree-decomposition via the cops-and-robber game if the component is small enough
	 * d) compute an optimal tree-decomposition via a SAT-encoding
	 * @param component
	 * @return
	 * @throws Exception
	 */
	private TreeDecomposition<T> computeTreeDecompositionOfComponent(Graph<T> component) throws Exception {
		
		GraphReducer<T> reducer = new GraphReducer<>(component);
		for (Graph<T> reduced : reducer) {
			int n = reduced.getVertices().size();
			LOG.info("Reduced the graph from " + component.getVertices().size() + " to " + n + " vertices");
			
			// first compute lower and upper bounds on the tree-width
			int lb = new MinorMinWidthLowerbound<>(reduced).call();
			LOG.info("Computed lower bound: " + lb);
			TreeDecomposition<T> ubDecomposition = new StochasticGreedyPermutationDecomposer<T>(reduced).call();
			int ub = ubDecomposition.getWidth();		
			LOG.info("Computed upper bound: " + ub);
			
			// if they match, we are done as well
			if (lb == ub) {
				LOG.info("The bounds match, extract decomposition");
				reducer.addbackTreeDecomposition(ubDecomposition);
				continue;
			}

			BigInteger freeMemory = new BigInteger(""+ Runtime.getRuntime().freeMemory());
			BigInteger expectedMemory = binom(new BigInteger(""+n), new BigInteger(""+ub)).multiply(new BigInteger(""+(n+32)/8));
			LOG.info("Free Memory: " + freeMemory);
			LOG.info("Expected Memory: " + expectedMemory);
			
			// otherwise check if the instance is small enough for the dynamic cops-and-robber game
			// the algorithm has running time O(n choose k), so we check the size of n choose k
			// This is also used if no SAT solver is available
			if (!Formula.canRegisterSATSolver() || (n <= COPS_VERTICES_THRESHOLD && ub <= COPS_TW_THRESHOLD && expectedMemory.compareTo(freeMemory) < 0)) {
				LOG.info("Solve with a game of Cops and Robbers");
				TreeDecomposition<T> decomposition = new CopsAndRobber<>(reduced).call();
				reducer.addbackTreeDecomposition(decomposition);
				continue;
			}

			/* If everything above does not work, we solve the problem using a SAT-encoding */
			LOG.info("Solve with a SAT solver");
			TreeDecomposition<T> decomposition = new SATDecomposer<>(reduced, Encoding.IMPROVED, lb, ub).call();

			reducer.addbackTreeDecomposition(decomposition);
		}
		return reducer.getTreeDecomposition();	
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		LOG.info("");
		
		GraphSplitter<T> splitter = new GraphSplitter<>(this.graph);
		for (Graph<T> component : splitter) {
			TreeDecomposition<T> decomposition = null;
			try {
				decomposition = computeTreeDecompositionOfComponent(component);
			} catch (Exception e) {
				LOG.warning("failed to compute decomposition of a component");
			}
			splitter.addbackTreeDecomposition(decomposition);
		}
		return splitter.getTreeDecomposition();
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return null;
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Exact;
	}

	/**
	 * Auxiliary method to compute n choose k with BigIntegers.
	 * @param n of n over k
	 * @param k of n over k
	 * @return an BigInteger representing \(\binom{n}{k}\)
	 */
	private BigInteger binom(BigInteger n, BigInteger k) {
		if (k.compareTo(n) > 0) return BigInteger.ZERO;
		if (k.compareTo(BigInteger.ZERO) == 0) {
			return BigInteger.ONE;
		} else if (k.multiply(new BigInteger(""+2)).compareTo(n) > 0) {
			return binom(n, n.subtract(k));
		}

		BigInteger result = n.subtract(k).add(BigInteger.ONE);
		for (BigInteger i = new BigInteger(""+2); i.compareTo(k) <= 0.; i = i.add(BigInteger.ONE)) {
			result = result.multiply(n.subtract(k).add(i));
			result = result.divide(i);
		}
		return result;
	}

}
