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
package jdrasil.graph.invariants;

import java.util.Map;

import jdrasil.graph.Graph;

/**
 * A graph property is a class of graphs that is closed under isomorphism. A graph invariant is a function that
 * maps isomorphic graphs to the same value. Examples for graph invariants are "number of vertices", "size of the minimum vertex-cover", etc..
 * An example of a graph property could be "contains a triangle". In this sense, we can model graph properties as invariants that map to boolean values.
 * 
 * This class models a graph-invariant. In order to do so, it essentially provides the method @see getValue(), which returns the computed invariant.
 * Since many invariants can be represented by an additional model (for instance, a model for the vertex-cover model is the actual vertex-cover), 
 * the class also provides the method @see getModel(), which returns a map from vertices to some values.
 * 
 * @param <T> the vertex type
 * @param <Value> the type of the invariant (@see getValue())
 * @param <Image> the type to which vertices are mapped (@see getModel())
 * 
 * @author Max Bannach
 */
public abstract class Invariant<T extends Comparable<T>, Value, Image> {

	/** The graph for which the invariant is computed. */
	protected Graph<T> graph;
	
	/** The value of the invariant (or  "the invariant"). */
	private Value value;
	
	/** A corresponding model, i.e., a function that maps vertices to values of type @see Image. */
	private Map<T, Image> model;
	
	/**
	 * The constructor of the invariant for a fixed graph G.
	 * This method will invoke the computation of the invariant. In order to do so,
	 * it will call (in this order) the methods @see computeModel() and @see computeValue().
	 * 
	 * @param graph
	 */
	public Invariant(Graph<T> graph) {
		this.graph = graph;
		this.model = computeModel();
		this.value = computeValue();
	}
	
	/**
	 * Compute a model of the invariant, i.e., a function that maps from vertices to the Image.
	 * This method will be invoked by the constructor and should not be called manually.
	 * 
	 * As the name states, this method should do the computational task (i.e., actually computing the invariant / a model of it).
	 * This method will be called _before_ @see computeValue().
	 * 
	 * @return
	 */
	protected abstract Map<T, Image> computeModel();
	
	/**
	 * Compute the value of the invariant, i.e., "the invariant".
	 * This method will be invoked by the constructor and should not be called manually.
	 * 
	 * As the name states, this method should do the computational task (i.e., actually computing the value).
	 * However, this method is called _after_ @see computeModel() and, therefore, the method @see getModel() is available 
	 * during the evaluation of this method.
	 * 
	 * @return
	 */
	protected abstract Value computeValue();
	
	/**
	 * Returns the value of the invariant (computed by @see computeValue()).
	 * This method is just a getter and works in O(1).
	 * @return
	 */
	public Value getValue() {
		return this.value;
	}
	
	/**
	 * Returns a model of the invariant (computed by @see computeModel()).
	 * This method is just a getter and works in O(1).
	 * @return
	 */
	public Map<T, Image> getModel() {
		return this.model;
	}
	
	/**
	 * Many graph invariants are NP-hard or worse, therefore it is often not possible to compute them exact.
	 * A graph invariant can communicate with this method whether or not it is exact.
	 * @return
	 */
	public abstract boolean isExact();
	
}
