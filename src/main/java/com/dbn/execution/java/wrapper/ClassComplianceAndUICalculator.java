package com.dbn.execution.java.wrapper;

import com.dbn.object.DBJavaClass;
import com.dbn.execution.java.wrapper.ClassComplianceAndUI.ComplianceData;
import com.dbn.execution.java.wrapper.ClassComplianceAndUI.CachedData;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.type.DBJavaAccessibility;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

import static com.dbn.common.util.Strings.capitalize;

public class ClassComplianceAndUICalculator {

    private static final ClassComplianceAndUICalculator INSTANCE = new ClassComplianceAndUICalculator();

    // Argument constants
    private static final short ARGUMENT_MAX_COMPLIANT_SCALAR_ARRAY_DEPTH = 1;
    private static final short ARGUMENT_MAX_COMPLIANT_NONSCALAR_ARRAY_DEPTH = 0;
    // Return constants
    private static final short RETURN_MAX_COMPLIANT_SCALAR_ARRAY_DEPTH = 1;
    private static final short RETURN_MAX_COMPLIANT_NONSCALAR_ARRAY_DEPTH = 0;

    private ClassComplianceAndUICalculator() { }

    public static ClassComplianceAndUICalculator getInstance() { return INSTANCE; }

    // --- Public API

    public ComplianceData getArgumentComplianceData(DBJavaParameter dbJavaParameter) {
        return getArgumentComplianceData(dbJavaParameter, new CachedData());
    }
    public ComplianceData getArgumentComplianceData(DBJavaParameter dbJavaParameter, CachedData cachedData) {
        return getArgumentComplianceData(dbJavaParameter.getJavaClass(), dbJavaParameter.getArrayDepth(), cachedData);
    }
    public ComplianceData getArgumentComplianceData(DBJavaClass dbJavaClass, short arrayDepth) {
        return getArgumentComplianceData(dbJavaClass, arrayDepth, new CachedData());
    }
    public ComplianceData getArgumentComplianceData(DBJavaClass dbJavaClass, short arrayDepth, CachedData cachedData) {
        if (cachedData == null) {cachedData = new CachedData();}
        return getComplianceData(
                dbJavaClass, arrayDepth, new HashSet<>(), cachedData, // recursion set init
                true, // isArgument
                ARGUMENT_MAX_COMPLIANT_SCALAR_ARRAY_DEPTH, ARGUMENT_MAX_COMPLIANT_NONSCALAR_ARRAY_DEPTH
        );
    }

    public ComplianceData getReturnComplianceData(DBJavaClass dbJavaClass, short arrayDepth) {
        return getReturnComplianceData(dbJavaClass, arrayDepth, new CachedData());
    }
    public ComplianceData getReturnComplianceData(DBJavaClass dbJavaClass, short arrayDepth, CachedData cachedData) {
        if (cachedData == null) {cachedData = new CachedData();}
        return getComplianceData(
                dbJavaClass, arrayDepth, new HashSet<>(), cachedData,
                false, // isArgument
                RETURN_MAX_COMPLIANT_SCALAR_ARRAY_DEPTH, RETURN_MAX_COMPLIANT_NONSCALAR_ARRAY_DEPTH
        );
    }

    // --- Core recursion with config
    /**
     * Shared recursive engine for both argument and return checks.
     */
    private ComplianceData getComplianceData(
            DBJavaClass dbJavaClass,
            short arrayDepth,
            Set<DBJavaClass> classesSeenTillNow,
            CachedData cachedData,
            boolean isArgument,
            short maxScalarArrayDepth,
            short maxNonScalarArrayDepth
    ) {
        Map<DBJavaClass, ComplianceData> cache = isArgument
                ? cachedData.getArgumentData()
                : cachedData.getReturnData();

        ComplianceData earlyExit = checkCommonComplianceIssues(
                dbJavaClass, arrayDepth, classesSeenTillNow, cache, isArgument, maxScalarArrayDepth, maxNonScalarArrayDepth);
        if(earlyExit != null) return earlyExit;

        classesSeenTillNow.add(dbJavaClass);

        int displayRowCount = 1;
        try {

            for (DBJavaField field : dbJavaClass.getFields()) {
                if (!(field.getAccessibility() == DBJavaAccessibility.PUBLIC)) {
                    boolean hasMethod = isArgument
                            ? field.findSetterMethod() != null
                            : field.findGetterMethod() != null;
                    if (!hasMethod) {
                        ComplianceData complianceData = new ComplianceData();
                        complianceData.setSupported(false);
                        complianceData.setDisplayRowCount(-1);
                        String accessor = isArgument ? "set" : "get";
                        complianceData.setUnsupportedReason(
                                accessor + capitalize(field.getName()) + " " + (isArgument ? "setter" : "getter")
                                        + " method not found for "
                                        + dbJavaClass.getCanonicalName() + "." + capitalize(field.getName())
                        );
                        cache.put(dbJavaClass, complianceData);
                        return complianceData;
                    }
                }
                ComplianceData fieldCompliance = getComplianceData(
                        field.getJavaClass(), field.getArrayDepth(), classesSeenTillNow, cachedData,
                        isArgument, maxScalarArrayDepth, maxNonScalarArrayDepth);
                if (!fieldCompliance.isSupported()) return fieldCompliance;
                displayRowCount += fieldCompliance.getDisplayRowCount();
            }
            ComplianceData classComplianceData = new ComplianceData();
            classComplianceData.setSupported(true);
            classComplianceData.setDisplayRowCount(displayRowCount);
            cache.put(dbJavaClass, classComplianceData);
            return classComplianceData;
        } finally {
            classesSeenTillNow.remove(dbJavaClass);
        }
    }

    private ComplianceData checkCommonComplianceIssues(
            DBJavaClass dbJavaClass,
            short arrayDepth,
            Set<DBJavaClass> classesSeenTillNow,
            Map<DBJavaClass, ComplianceData> cache,
            boolean isArgument,
            short maxScalarArrayDepth,
            short maxNonScalarArrayDepth
    ) {
        if (!isArraySupported(dbJavaClass, arrayDepth, maxScalarArrayDepth, maxNonScalarArrayDepth))
            return getArrayNotSupportedData(dbJavaClass, isArgument, maxScalarArrayDepth, maxNonScalarArrayDepth);

        if (dbJavaClass.isScalar())
            return getScalarComplianceData();

        if (cache.containsKey(dbJavaClass))
            return cache.get(dbJavaClass);

        if (classesSeenTillNow.contains(dbJavaClass)) {
            ComplianceData cyclicData = getCyclicComplianceData(dbJavaClass);
            cache.put(dbJavaClass, cyclicData);
            return cyclicData;
        }

        if (isArgument && !dbJavaClass.hasPublicDefaultConstructor()) {
            ComplianceData data = new ComplianceData();
            data.setSupported(false);
            data.setDisplayRowCount(-1);
            data.setUnsupportedReason(
                    "No default constructor found for class " + dbJavaClass.getCanonicalName()
            );
            cache.put(dbJavaClass, data);
            return data;
        }

        if (TypeMappings.getUNSUPPORTED_TYPES().contains(dbJavaClass.getCanonicalName())) {
            ComplianceData data = new ComplianceData();
            data.setSupported(false);
            data.setDisplayRowCount(-1);
            data.setUnsupportedReason(
                    dbJavaClass.getCanonicalName() + " is not supported."
            );
            cache.put(dbJavaClass, data);
            return data;
        }

        return null; // If no early exit, continue in main logic
    }

    // --- Utility/shared logic

    private boolean isArraySupported(DBJavaClass dbJavaClass, short arrayDepth,
                                     short maxScalarArrayDepth, short maxNonScalarArrayDepth) {
        return dbJavaClass.isScalar() ? arrayDepth <= maxScalarArrayDepth : arrayDepth <= maxNonScalarArrayDepth;
    }

    private ComplianceData getArrayNotSupportedData(DBJavaClass dbJavaClass, boolean isArgument,
                                                    short maxScalarArrayDepth, short maxNonScalarArrayDepth) {
        ComplianceData data = new ComplianceData();
        data.setSupported(false);
        data.setDisplayRowCount(-1);
        int maxDepthSupported = dbJavaClass.isScalar() ? maxScalarArrayDepth : maxNonScalarArrayDepth;
        data.setUnsupportedReason(
                "Array of type " + dbJavaClass.getCanonicalName()
                        + " with depth greater than " + maxDepthSupported + " is not supported as "
                        + (isArgument ? "argument" : "return") + "."
        );
        return data;
    }

    private ComplianceData getCyclicComplianceData(DBJavaClass dbJavaClass) {
        ComplianceData data = new ComplianceData();
        data.setSupported(false);
        data.setDisplayRowCount(-1);
        data.setUnsupportedReason(
                "Class " + dbJavaClass.getCanonicalName() +
                        " contains a cyclic self-reference, which is not supported for arguments or return values."
        );
        return data;
    }

    private ComplianceData getScalarComplianceData() {
        ComplianceData data = new ComplianceData();
        data.setSupported(true);
        data.setDisplayRowCount(1);
        return data;
    }


    // --- Cached data builder (unchanged except using shared methods)
    public CachedData buildCachedData(List<DBJavaMethod> dbJavaMethods) {
        CachedData cachedData = new CachedData();
        if (dbJavaMethods == null) return cachedData;
        for (DBJavaMethod dbJavaMethod : dbJavaMethods) {
            buildCachedData(dbJavaMethod, cachedData);
        }
        return cachedData;
    }

    public CachedData buildCachedData(DBJavaMethod dbJavaMethod){
        return buildCachedData(dbJavaMethod, new CachedData());
    }

    private CachedData buildCachedData(DBJavaMethod dbJavaMethod, CachedData cachedData) {
        if (cachedData == null) cachedData = new CachedData();
        if (dbJavaMethod == null) return cachedData;
        // Arguments
        for (DBJavaParameter dbJavaParameter : dbJavaMethod.getParameters()) {
            if (dbJavaParameter.isScalar()
                    || dbJavaParameter.getArrayDepth() > ARGUMENT_MAX_COMPLIANT_NONSCALAR_ARRAY_DEPTH
                    || cachedData.getArgumentData().containsKey(dbJavaParameter.getJavaClass())) continue;
            getArgumentComplianceData(dbJavaParameter.getJavaClass(), (short) 0, cachedData);
        }
        // Return
        boolean returnIsVoid = dbJavaMethod.getSignature().split(":")[1].trim().equals("void");
        if (!returnIsVoid) {
            DBJavaClass returnClass = dbJavaMethod.getReturnClass();
            short returnArrayDepth = dbJavaMethod.getReturnArrayDepth();
            if (!returnClass.isScalar()
                    && returnArrayDepth <= RETURN_MAX_COMPLIANT_NONSCALAR_ARRAY_DEPTH
                    && !cachedData.getReturnData().containsKey(returnClass)) {
                getReturnComplianceData(returnClass, returnArrayDepth, cachedData);
            }
        }
        return cachedData;
    }

}