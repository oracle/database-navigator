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

package com.dbn.object.lookup;

import com.dbn.common.collections.ConcurrentStringInternMap;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaEntity;
import com.dbn.object.type.DBObjectType;
import lombok.experimental.UtilityClass;

import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Unsafe.cast;

/**
 * Utility class for managing transformations and caching of database-style Java class names.
 * Provides methods to compute and cache simple names and canonical Java class names for efficient reuse.
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class DBJavaNameCache {
    private static final ConcurrentStringInternMap simpleNameCache = new ConcurrentStringInternMap();
    private static final ConcurrentStringInternMap canonicalNameCache = new ConcurrentStringInternMap();

    /**
     * Extracts and returns the simple name from the given database-style Java class name.
     * The simple name is the part of the class name after the last "/".
     * The result is cached for future invocations.
     *
     * @param objectName the database-style Java class name, where segments are separated by "/"
     * @return the simple name of the class, which is the part of the name after the last "/"
     * @deprecated assumes $ are always inner class separators
     */
    public static String getSimpleName(String objectName) {
        if (isEmpty(objectName)) return "";
        return simpleNameCache.computeIfAbsent(objectName, n -> n.substring(
                Math.max(
                        n.lastIndexOf("/"),
                        n.lastIndexOf("$")) + 1)); // TODO $ is not a reliable indicator for inner class (can be part of the name of a real class)
    }

    /**
     * Transforms a database representation of a Java class name into its canonical Java class name.
     * The method replaces all occurrences of "/" with "." in the input string.
     * The result is cached for future invocations.
     *
     * @param objectName the database representation of the Java class name, where parts of the name
     *                   are separated by "/" instead of "."
     * @return the canonical Java class name with parts of the name separated by "."
     * @deprecated not properly supporting inner classes
     */
    public static String getCanonicalName(String objectName) {
        if (isEmpty(objectName)) return "";
        return canonicalNameCache.computeIfAbsent(objectName, n -> n.replace("/", "."));
    }

    public static String getSimpleName(DBObjectRef<DBJavaEntity> javaEntity) {
        return simpleNameCache.computeIfAbsent(javaEntity.getObjectName(), n -> resolveSimpleName(javaEntity));
    }

    public static String getCanonicalName(DBObjectRef<DBJavaClass> javaClass) {
        String className = javaClass.getObjectName();
        return canonicalNameCache.computeIfAbsent(className, n -> resolveCanonicalName(javaClass));
    }

    private static String resolveCanonicalName(DBObjectRef<DBJavaClass> javaClass) {
        String className = javaClass.getObjectName();
        Object parent = javaClass.getParent();
        if (parent instanceof DBObjectRef) {
            DBObjectRef<?> parentRef = (DBObjectRef) parent;
            if (parentRef.getObjectType() == DBObjectType.JAVA_CLASS) {
                DBObjectRef<DBJavaClass> parentClassRef = cast(parentRef);
                String parenClassName = parentRef.getObjectName();
                return resolveCanonicalName(parentClassRef) + "." + className.substring(parenClassName.length() + 1);
            }
        }

        return className.replace("/", ".");
    }

    private static String resolveSimpleName(DBObjectRef<DBJavaEntity> javaEntity) {
        String entityName = javaEntity.getObjectName();
        DBObjectType entityType = javaEntity.getObjectType();

        if (entityType == DBObjectType.JAVA_CLASS) {
            Object parent = javaEntity.getParent();
            if (parent instanceof DBObjectRef) {
                DBObjectRef<?> parentRef = (DBObjectRef) parent;
                // inner class handling
                if (parentRef.getObjectType() == DBObjectType.JAVA_CLASS) {
                    String parenClassName = parentRef.getObjectName();
                    return entityName.substring(parenClassName.length() + 1);
                }
            }
        }

        return entityName.substring(entityName.lastIndexOf("/") + 1);
    }
}
