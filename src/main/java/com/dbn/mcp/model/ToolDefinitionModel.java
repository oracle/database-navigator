package com.dbn.mcp.model;

import com.dbn.common.util.Json;
import com.dbn.common.util.Strings;
import com.dbn.mcp.ui.ParamTableModel;
import com.dbn.mcp.util.SqlParameterParser;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ToolDefinitionModel {
    private String name;
    private String description;
    private String statement;
    private ParamTableModel paramsModel;

    public ToolDefinitionModel(ParamTableModel paramsModel) {
        this.paramsModel = paramsModel;
    }

    public String getRewrittenStatement() {
        if (statement == null) return "";
        return SqlParameterParser.rewriteToJdbc(statement).getSql();
    }

    public String getParamOrderCsv() {
        if (statement == null) return "";
        return String.join(",", SqlParameterParser.rewriteToJdbc(statement).getParamOrder());
    }

    public String getJsonSchema() {
        if (statement == null) return "{\"type\":\"object\",\"properties\":{}}";
        List<String> params = SqlParameterParser.uniqueInOrder(SqlParameterParser.parseOccurrences(statement));
        Map<String, ParamRow> paramMap = buildParamMap();

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> requiredParams = new ArrayList<>();
        for (String p : params) {
            ParamRow row = paramMap.get(p);
            properties.put(p, buildParamSchema(row));
            if (row != null && row.isRequired()) {
                requiredParams.add(p);
            }
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!requiredParams.isEmpty()) {
            schema.put("required", requiredParams);
        }
        return Json.writeAsString(schema);
    }

    private Map<String, ParamRow> buildParamMap() {
        Map<String, ParamRow> map = new LinkedHashMap<>();
        for (ParamRow row : paramsModel.getRows()) {
            map.put(SqlParameterParser.stripColon(row.getName()), row);
        }
        return map;
    }

    private Map<String, Object> buildParamSchema(ParamRow row) {
        Map<String, Object> schema = new LinkedHashMap<>();
        ParamType type = row != null ? row.getType() : ParamType.STRING;

        schema.put("type", type.getYamlType());
        if (row != null && Strings.isNotEmpty(row.getDescription())) schema.put("description", row.getDescription());
        if (row != null && Strings.isNotEmpty(row.getDefaultValue())) addDefault(schema, row.getDefaultValue(), type);

        return schema;
    }

    private void addDefault(Map<String, Object> schema, String value, ParamType type) {
        try {
            switch (type) {
                case BOOLEAN: schema.put("default", "true".equalsIgnoreCase(value)); break;
                case INTEGER: schema.put("default", Long.parseLong(value)); break;
                case NUMBER:  schema.put("default", Double.parseDouble(value)); break;
                default:      schema.put("default", value);
            }
        } catch (NumberFormatException ignored) {}
    }
}
