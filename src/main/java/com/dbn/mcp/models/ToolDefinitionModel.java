package com.dbn.mcp.models;

import com.dbn.common.util.Json;
import com.dbn.mcp.McpServerInputForm.ParamRow;
import com.dbn.mcp.McpServerInputForm.ParamType;
import com.dbn.mcp.ui.ParamTableModel;
import com.dbn.mcp.util.SqlParameterParser;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ToolDefinitionModel {
    private String name;
    private String description;
    private String sql;
    private ParamTableModel paramsModel;

    public ToolDefinitionModel(ParamTableModel paramsModel) {
        this.paramsModel = paramsModel;
    }

    public String getRewrittenSql() {
        return SqlParameterParser.rewriteToJdbc(sql).getSql();
    }

    public String getParamOrderCsv() {
        return String.join(",", SqlParameterParser.rewriteToJdbc(sql).getParamOrder());
    }

    public String getJsonSchema() {
        List<String> params = SqlParameterParser.uniqueInOrder(SqlParameterParser.parseOccurrences(sql));
        Map<String, ParamRow> paramMap = buildParamMap();

        Map<String, Object> properties = new LinkedHashMap<>();
        for (String p : params) properties.put(p, buildParamSchema(paramMap.get(p)));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new ArrayList<>(params));
        return Json.writeAsString(schema);
    }

    private Map<String, ParamRow> buildParamMap() {
        Map<String, ParamRow> map = new LinkedHashMap<>();
        for (ParamRow row : paramsModel.getRows()) {
            map.put(SqlParameterParser.stripColon(row.name), row);
        }
        return map;
    }

    private Map<String, Object> buildParamSchema(ParamRow row) {
        Map<String, Object> schema = new LinkedHashMap<>();
        ParamType type = row != null ? row.type : ParamType.String;

        schema.put("type", toJsonType(type));
        if (type == ParamType.Date) schema.put("format", "date");
        if (row != null && notEmpty(row.description)) schema.put("description", row.description);
        if (row != null && notEmpty(row.defaultValue)) addDefault(schema, row.defaultValue, type);

        return schema;
    }

    private void addDefault(Map<String, Object> schema, String value, ParamType type) {
        try {
            switch (type) {
                case Boolean: schema.put("default", "true".equalsIgnoreCase(value)); break;
                case Integer: schema.put("default", Long.parseLong(value)); break;
                case Float:   schema.put("default", Double.parseDouble(value)); break;
                default:      schema.put("default", value);
            }
        } catch (NumberFormatException ignored) {}
    }

    private String toJsonType(ParamType type) {
        switch (type) {
            case Integer: return "integer";
            case Float:   return "number";
            case Boolean: return "boolean";
            default:      return "string";
        }
    }

    private boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
