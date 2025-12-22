package com.dbn.vector.model.staging;

import com.dbn.vector.model.VectorEmbeddingConfig;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
public class StagingConfig extends VectorEmbeddingConfig {
    private String schemaName;
    private String tableName;

    @Override
    public void readState(Element element) {
        if (element == null) return;

        super.readState(element);
        schemaName = stringAttribute(element, "schema");
        tableName = stringAttribute(element, "table");
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);
        setStringAttribute(element, "schema", schemaName);
        setStringAttribute(element, "table", tableName);
    }
}
