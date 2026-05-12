package com.dbn.mcp.model;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Json;
import com.dbn.mcp.util.SqlParameterParser;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public class McpToolDefinition implements PersistentStateElement, Cloneable<McpToolDefinition> {
    private String name;
    private String description;
    private String statement;
    private List<McpToolParam> parameters = new ArrayList<>();

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
        Map<String, McpToolParam> paramMap = buildParamMap();

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> requiredParams = new ArrayList<>();
        for (String p : params) {
            McpToolParam row = paramMap.get(p);
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

    private Map<String, McpToolParam> buildParamMap() {
        Map<String, McpToolParam> map = new LinkedHashMap<>();
        for (McpToolParam row : parameters) {
            map.put(SqlParameterParser.stripColon(row.getName()), row);
        }
        return map;
    }

    private Map<String, Object> buildParamSchema(McpToolParam row) {
        Map<String, Object> schema = new LinkedHashMap<>();
        McpToolParamType type = row != null ? row.getType() : McpToolParamType.STRING;

        schema.put("type", type.getSchemaType());
        if (type.getSchemaFormat() != null) {
            schema.put("format", type.getSchemaFormat());
        }
        if (row != null && row.getDescription() != null && !row.getDescription().isEmpty()) {
            schema.put("description", row.getDescription());
        }

        return schema;
    }

    @Override
    public void readState(Element element) {
        name = stringAttribute(element, "name", name);
        description = readCdata(element.getChild("description"));
        statement = readCdata(element.getChild("statement"));

        Element parametersElement = element.getChild("parameters");
        for (Element parameterElement : childrenOf(parametersElement, "parameter")) {
            McpToolParam param = new McpToolParam();
            param.readState(parameterElement);
            parameters.add(param);
        }
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "name", name);
        writeCdata(newElement(element, "description"), description);
        writeCdata(newElement(element, "statement"), statement);

        Element parametersElement = newElement(element, "parameters");
        for (McpToolParam parameter : parameters) {
            Element parameterElement = newElement(parametersElement, "parameter");
            parameter.writeState(parameterElement);
        }
    }

    @Override
    @SneakyThrows
    public McpToolDefinition clone() {
        McpToolDefinition clone = cast(super.clone());
        clone.parameters = new ArrayList<>(Cloneable.cloneList(parameters));
        return clone;
    }
}
