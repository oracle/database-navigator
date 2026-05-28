package com.dbn.common.util;

/**
 * Implemented by classes that can adapt themselves to other classes.
 */
public interface Adaptable {
    /**
     *
     * @param type the class type to adapt the object to
     * @return a version of the implemention of this as T or null if this object doesn't adapt to T
     * @param <T> the type
     */
    <T> T adaptTo(Class<T> type);
}
