package com.dbn.execution.java.wrapper.naming;

import com.dbn.common.util.Naming;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
@Setter
public class CustomNamingProvider implements WrapperNamingProvider {
    private String javaWrapperName;
    private String sqlWrapperName;
    private Map<String, String> sqlPackageMethodMap;
    private Map<String, String> sqlTypesMap;

    @Override
    public String getJavaWrapperName(DBJavaClass javaClass) {
        return javaWrapperName;
    }

    @Override
    public String getJavaWrapperName(DBJavaMethod javaMethod) {
        return javaWrapperName;
    }

    @Override
    public String getSqlWrapperName(DBJavaClass javaClass) {
        return sqlWrapperName;
    }

    @Override
    public String getSqlWrapperName(DBJavaMethod javaMethod) {
        return sqlWrapperName;
    }

    @Override
    public String getSqlTypeName(DBJavaClass javaClass, int arrayDepth) {
        return "";
    }

    @Override
    public String getSqlTypeName(String javaClassName, int arrayDepth) {
        if(sqlTypesMap == null) return "" ;
        String typeName = toSqlTypeName(javaClassName, "TYPE");
        if (arrayDepth > 0) typeName += "_" + arrayDepth;
        return sqlTypesMap.get(typeName);
    }

    @Override
    public String getSqlMethodName(DBJavaMethod javaMethod) {
        if(sqlPackageMethodMap == null) return "" ;
        return sqlPackageMethodMap.get(Naming.toUpperSnakeCase(javaMethod.getSimpleName()));
    }

    private @NotNull String toSqlTypeName(String className, String qualifier) {
        return "OJVM_" + qualifier + "_" + className.replace(".", "_").replace("$", "_").toUpperCase();
    }
}
