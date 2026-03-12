/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.execution.java.wrapper.support;

import com.dbn.execution.java.wrapper.TypeMappings;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.util.Strings.capitalize;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;
import static com.dbn.object.type.DBJavaScalarType.isScalar;

@UtilityClass
public class WrapperSupportEvaluator {

    private static final short MAX_SCALAR_ARRAY_DEPTH = 1;
    private static final short MAX_NONSCALAR_ARRAY_DEPTH = 0;

    // --- Public API

    public static WrapperSupportInfo evaluateArgumentSupport(
            DBJavaParameter javaParameter,
            WrapperSupportData supportData) {
        return evaluateArgumentSupport(
                javaParameter.getJavaClassRef(),
                javaParameter.getArrayDepth(),
                supportData);
    }

    private static WrapperSupportInfo evaluateArgumentSupport(
            DBObjectRef<DBJavaClass> javaClass,
            short arrayDepth,
            WrapperSupportData supportData) {

        if (supportData == null) {
            supportData = new WrapperSupportData();
        }

        WrapperSupportEntity supportEntity = new WrapperSupportEntity(javaClass, arrayDepth, true);
        return evaluateWrapperSupport(
                supportData, supportEntity,
                new HashSet<>());
    }

    public static WrapperSupportInfo evaluateReturnArgumentSupport(DBObjectRef<DBJavaClass> javaClass, short arrayDepth, WrapperSupportData supportData) {
        if (supportData == null) {
            supportData = new WrapperSupportData();
        }

        WrapperSupportEntity supportEntity = new WrapperSupportEntity(javaClass, arrayDepth, false);
        return evaluateWrapperSupport(
                supportData, supportEntity,
                new HashSet<>());
    }

    // --- Core recursion with config
    /**
     * Shared recursive engine for both argument and return checks.
     */
    private static WrapperSupportInfo evaluateWrapperSupport(
            WrapperSupportData data,
            WrapperSupportEntity entity,
            Set<String> classesSeenTillNow) {

        String javaClassName = entity.getJavaClassName();

        WrapperSupportInfo earlyExit = verifyWrapperSupport(
                data, entity, classesSeenTillNow);
        if(earlyExit != null) return earlyExit;

        classesSeenTillNow.add(javaClassName);

        int displayRowCount = 1;
        try {
            DBObjectRef<DBJavaClass> javaClass = entity.getJavaClass();
            boolean input = entity.isInput();

            for (DBJavaField field : javaClass.ensure().getFields()) {
                if (!field.isPublic()) {
                    boolean hasMethod = input
                            ? field.findSetterMethod() != null
                            : field.findGetterMethod() != null;
                    if (!hasMethod) {
                        WrapperSupportInfo supportInfo = new WrapperSupportInfo();
                        supportInfo.setSupported(false);
                        supportInfo.setNestingLevel(-1);
                        String accessor = input ? "set" : "get";
                        supportInfo.setUnsupportedReason(
                                accessor + capitalize(field.getName()) + " " + (input ? "setter" : "getter")
                                        + " method not found for "
                                        + javaClassName + "." + capitalize(field.getName()));

                        data.addSupportInfo(javaClassName, supportInfo, input);
                        return supportInfo;
                    }
                }
                WrapperSupportEntity fieldEntity = new WrapperSupportEntity(field.getJavaClassRef(), field.getArrayDepth(), entity.isInput());
                WrapperSupportInfo fieldCompliance = evaluateWrapperSupport(data, fieldEntity, classesSeenTillNow);

                if (!fieldCompliance.isSupported()) return fieldCompliance;
                displayRowCount += fieldCompliance.getNestingLevel();
            }
            WrapperSupportInfo supportInfo = new WrapperSupportInfo();
            supportInfo.setSupported(true);
            supportInfo.setNestingLevel(displayRowCount);
            data.addSupportInfo(javaClassName, supportInfo, input);
            return supportInfo;
        } finally {
            classesSeenTillNow.remove(javaClassName);
        }
    }

    private static WrapperSupportInfo verifyWrapperSupport(
            WrapperSupportData data,
            WrapperSupportEntity entity,
            Set<String> classesSeenTillNow) {

        DBObjectRef<DBJavaClass> javaClass = entity.getJavaClass();
        short arrayDepth = entity.getArrayDepth();
        boolean input = entity.isInput();
        String javaClassName = entity.getJavaClassName();

        if (!isArraySupported(javaClass, arrayDepth)){
            return getArrayNotSupportedData(javaClass, input);
        }

        if (isScalar(javaClass)) return getScalarComplianceData();

        WrapperSupportInfo supportInfo = data.getSupportInfo(javaClassName, input);
        if (supportInfo != null) return supportInfo;

        if (classesSeenTillNow.contains(javaClassName)) {
            WrapperSupportInfo info = getCyclicComplianceData(javaClass);
            data.addSupportInfo(javaClassName, supportInfo, input);
            return info;
        }

        if (input && !javaClass.ensure().hasPublicDefaultConstructor()) {
            WrapperSupportInfo info = new WrapperSupportInfo();
            info.setSupported(false);
            info.setNestingLevel(-1);
            info.setUnsupportedReason("No default constructor found for class " + javaClassName);
            data.addSupportInfo(javaClassName, info, input);
            return info;
        }

        if (TypeMappings.getUNSUPPORTED_TYPES().contains(javaClassName)) {
            WrapperSupportInfo info = new WrapperSupportInfo();
            info.setSupported(false);
            info.setNestingLevel(-1);
            info.setUnsupportedReason(javaClassName + " is not supported.");
            data.addSupportInfo(javaClassName, info, input);
            return info;
        }

        return null; // If no early exit, continue in main logic
    }

    // --- Utility/shared logic

    private static boolean isArraySupported(DBObjectRef<DBJavaClass> javaClass, short arrayDepth) {
        return arrayDepth <= getMaxArrayDepth(javaClass);
    }

    private static WrapperSupportInfo getArrayNotSupportedData(DBObjectRef<DBJavaClass> javaClass, boolean isArgument) {
        WrapperSupportInfo data = new WrapperSupportInfo();
        data.setSupported(false);
        data.setNestingLevel(-1);
        int maxDepthSupported = getMaxArrayDepth(javaClass);

        data.setUnsupportedReason(
                "Array of type " + getCanonicalName(javaClass)
                        + " with depth greater than " + maxDepthSupported + " is not supported as "
                        + (isArgument ? "argument" : "return") + ".");
        return data;
    }

    private static short getMaxArrayDepth(DBObjectRef<DBJavaClass> javaClass) {
        return isScalar(javaClass) ?
                MAX_SCALAR_ARRAY_DEPTH :
                MAX_NONSCALAR_ARRAY_DEPTH;
    }

    private static WrapperSupportInfo getCyclicComplianceData(DBObjectRef<DBJavaClass> javaClass) {
        WrapperSupportInfo data = new WrapperSupportInfo();
        data.setSupported(false);
        data.setNestingLevel(-1);
        data.setUnsupportedReason(
                "Class " + getCanonicalName(javaClass) +
                        " contains a cyclic self-reference, which is not supported for arguments or return values."
        );
        return data;
    }

    private static WrapperSupportInfo getScalarComplianceData() {
        WrapperSupportInfo data = new WrapperSupportInfo();
        data.setSupported(true);
        data.setNestingLevel(1);
        return data;
    }


    // --- Cached data builder (unchanged except using shared methods)
    public static WrapperSupportData evaluateWrapperSupport(List<DBJavaMethod> javaMethods) {
        WrapperSupportData supportData = new WrapperSupportData();
        if (javaMethods == null) return supportData;

        for (DBJavaMethod javaMethod : javaMethods) {
            evaluateWrapperSupport(javaMethod, supportData);
        }
        return supportData;
    }

    public static WrapperSupportData evaluateWrapperSupport(DBJavaMethod javaMethod){
        return evaluateWrapperSupport(javaMethod, new WrapperSupportData());
    }

    private static WrapperSupportData evaluateWrapperSupport(DBJavaMethod javaMethod, WrapperSupportData data) {
        if (data == null) data = new WrapperSupportData();
        if (javaMethod == null) return data;

        // Arguments
        for (DBJavaParameter javaParameter : javaMethod.getParameters()) {
            DBObjectRef<DBJavaClass> parameterClass = javaParameter.getJavaClassRef();
            String parameterClassName = getCanonicalName(parameterClass);
            if (javaParameter.isScalar()
                    || javaParameter.getArrayDepth() > MAX_NONSCALAR_ARRAY_DEPTH
                    || data.getArgumentData().containsKey(parameterClassName)) continue;
            evaluateArgumentSupport(parameterClass, (short) 0, data);
        }
        // Return
        boolean returnIsVoid = javaMethod.isReturningVoid();
        if (!returnIsVoid) {
            DBObjectRef<DBJavaClass> returnClass = javaMethod.getReturnClassRef();
            String returnClassName = getCanonicalName(returnClass);
            short returnArrayDepth = javaMethod.getReturnArrayDepth();
            if (!isScalar(returnClass)
                    && returnArrayDepth <= MAX_NONSCALAR_ARRAY_DEPTH
                    && !data.getReturnData().containsKey(returnClassName)) {
                evaluateReturnArgumentSupport(returnClass, returnArrayDepth, data);
            }
        }
        return data;
    }

}