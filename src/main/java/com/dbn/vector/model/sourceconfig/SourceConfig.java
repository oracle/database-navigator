package com.dbn.vector.model.sourceconfig;

import com.dbn.vector.model.VectorEmbeddingConfig;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;

@Getter
@Setter
public class SourceConfig extends VectorEmbeddingConfig {
    private SourceType sourceType = SourceType.DATABASE_TABLE;
    private final DBTableSourceConfig tableSourceConfig = new DBTableSourceConfig();
    private final FileSystemSourceConfig fileSourceConfig = new FileSystemSourceConfig();

    @Override
    public void readState(Element element) {
        if (element == null) return;

        super.readState(element);
        sourceType = enumAttribute(element, "source-type", sourceType);

        Element tableSourceElement = element.getChild("table-source");
        Element fileSourceElement = element.getChild("file-source");
        tableSourceConfig.readState(tableSourceElement);
        fileSourceConfig.readState(fileSourceElement);
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);
        setEnumAttribute(element, "source-type", sourceType);

        Element tableSourceElement = newElement(element, "table-source");
        Element fileSourceElement = newElement(element, "file-source");
        tableSourceConfig.writeState(tableSourceElement);
        fileSourceConfig.writeState(fileSourceElement);
    }
}
