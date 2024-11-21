/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.common.constant;


import java.io.Serializable;
import java.util.Objects;

public interface Constant<T extends Constant<T>> extends Serializable, Comparable<T> {
    default String id() {
        if (this instanceof Enum) {
            Enum enumeration = (Enum) this;
            return enumeration.name();
        }
        throw new AbstractMethodError();
    }

    default boolean is(String id){
        return Objects.equals(id(), id);
    }

    default boolean isOneOf(T... constants){return Constants.isOneOf(this, constants);}

    static <T> T[] array(T ... constants) {
        return constants;
    }

    @Override
    default int compareTo(T o) {
        return ordinal() - o.ordinal();
    }

    int ordinal();
}
