package com.dbn.mcp.model;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Unsafe.cast;

@Data
@NoArgsConstructor
public class McpToolParam implements PersistentStateElement, Cloneable<McpToolParam> {
    private McpToolParamType type;
    private String name;
    private String description;
    private transient String testValue;
    private boolean required;

    public McpToolParam(String name, McpToolParamType type, String testValue, String description, boolean required) {
        this.name = name;
        this.type = nvl(type, McpToolParamType.STRING);
        this.testValue = testValue;
        this.description = description;
        this.required = required;
    }

    @Override
    public void readState(Element element) {
        type = enumAttribute(element, "type", McpToolParamType.STRING);
        name = stringAttribute(element, "name", name);
        description = readCdata(element.getChild("description"));
        //testValue = stringAttribute(element, "test-value", testValue);
        required = booleanAttribute(element, "required", required);
    }

    @Override
    public void writeState(Element element) {
        setEnumAttribute(element, "type", type);
        setStringAttribute(element, "name", name);
        writeCdata(newElement(element, "description"), description);
        //setStringAttribute(element, "test-value", testValue);
        setBooleanAttribute(element, "required", required);
    }

    @Override
    @SneakyThrows
    public McpToolParam clone() {
        return cast(super.clone());
    }
}
