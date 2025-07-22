package com.dbn.common.util;

/**
 * A variation of the GOF Visitor pattern (https://en.wikipedia.org/wiki/Visitor_pattern)
 * This version provides a short-circuit option that allows the visitor to decide
 * whether to continue based on a the results of specific visit node.
 *
 * @param <T> the type of the node type to visit
 * @param <R> the result type for each visit.
 */
public interface Visitor<T,R> {
    /**
     *
     * @param node the node to visit
     * @return a result type R that allows the accepting entity to modify the visition
     */
    public R visit(T node);

    /**
     *
     * @param result
     * @return true if the visitation should continue; false if it should stop before visiting
     * the next node.
     */
    public default boolean shouldContinue(R result) {
        return true;
    }
}
