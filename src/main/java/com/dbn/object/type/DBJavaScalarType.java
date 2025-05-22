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

package com.dbn.object.type;

import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.unmodifiableMap;

/**
 * Collection of java classes which can be considered "scalar" as in holding one single value in mathematical terms.
 * Scalar classes have special treatment in the java execution engine (e.g. kept unpacked when introspecting java results aso...)
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public final class DBJavaScalarType {
    private static final Class[] REGISTRY = new Class[]{
            boolean.class,
            byte.class,
            char.class,
            double.class,
            float.class,
            int.class,
            long.class,
            short.class,

            Boolean.class,
            Byte.class,
            Character.class,
            Double.class,
            Float.class,
            Integer.class,
            Long.class,
            Short.class,
            String.class,
            Number.class,
            BigDecimal.class,
            BigInteger.class,

            AtomicBoolean.class,
            AtomicInteger.class,
            AtomicLong.class,

            //...
    };

    private static final Map<String, DBJavaScalarType> canonicalNameMappings;   // e.g. com.dbn.SampleClass (canonical representation)
    private static final Map<String, DBJavaScalarType> objectNameMappings;      // e.g. com/dbn/SampleClass (database object representation)

    static {
        Map<String, DBJavaScalarType> nameMap = new HashMap<>();
        Map<String, DBJavaScalarType> pathMap = new HashMap<>();
        for (Class<?> type : REGISTRY) {
            nameMap.put(type.getCanonicalName(), new DBJavaScalarType(type));
            pathMap.put(type.getCanonicalName().replace(".", "/"), new DBJavaScalarType(type));
        }
        canonicalNameMappings = unmodifiableMap(nameMap);
        objectNameMappings = unmodifiableMap(pathMap);
    }

    private final Class<?> type;
    private final String name;
    private final String path;
    private final String canonicalName;

    DBJavaScalarType(Class<?> type) {
        this.type = type;
        this.name = type.getSimpleName();
        this.path = type.getCanonicalName().replace(".", "/");
        this.canonicalName = type.getCanonicalName();
    }

    public static DBJavaScalarType forName(String name) {
        return canonicalNameMappings.get(name);
    }

    public static DBJavaScalarType forObjectName(String objectName) {
        return objectNameMappings.get(objectName);
    }

    /**
     * Determines whether the given class name represents a scalar (single value) type.
     * A scalar type is a value type that is either a standard Java primitive,
     * its corresponding wrapper class, or commonly used value types such as {@code String},
     * {@code Number}, {@code BigDecimal}, or atomic value types.
     *
     * @return {@code true} if the className corresponds to a scalar type;
     *         {@code false} otherwise.
     */
    public static boolean isScalar(String className) {
        return forObjectName(className) != null;
    }

    public static boolean isScalar(@Nullable DBObjectRef<?> object) {
        return object != null && object.getObjectType().matches(DBObjectType.JAVA_CLASS) && isScalar(object.getObjectName());

    }
}

