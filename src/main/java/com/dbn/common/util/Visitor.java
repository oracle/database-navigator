package com.dbn.common.util;

public interface Visitor<T,R> {
    public R visit(T me);

    public default boolean shouldContinue(R result) {
        return true;
    }
}
