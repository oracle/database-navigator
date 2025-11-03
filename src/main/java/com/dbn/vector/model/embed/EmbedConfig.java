package com.dbn.vector.model.embed;

import com.dbn.vector.model.VectorEmbeddingConfig;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.Map;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;

@Getter
@Setter
public class EmbedConfig extends VectorEmbeddingConfig {
    private ModelLocation modelLocation = ModelLocation.IN_DATABASE_MODEL;

    private DatabaseModelConfig databaseModelConfig = new DatabaseModelConfig();
    private ThirdPartyModelConfig thirdPartyModelConfig = new ThirdPartyModelConfig();

    public String getConfigJson() {
        switch (modelLocation) {
            case IN_DATABASE_MODEL: return databaseModelConfig.getConfigJson();
            case THIRD_PARTY_MODEL: return thirdPartyModelConfig.getConfigJson();
            default: throw new IllegalArgumentException("Unexpected value: " + modelLocation);
        }
    }

    public Map<String, ?> getConfigMap() {
        switch (modelLocation) {
            case IN_DATABASE_MODEL: return databaseModelConfig.getConfigMap();
            case THIRD_PARTY_MODEL: return thirdPartyModelConfig.getConfigMap();
            default: throw new IllegalArgumentException("Unexpected value: " + modelLocation);
        }
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        super.readState(element);
        modelLocation = enumAttribute(element, "model-location", modelLocation);

        Element databaseModelElement = element.getChild("database-model");
        databaseModelConfig.readState(databaseModelElement);

        Element thirdPartyModelElement = element.getChild("third-party-model");
        thirdPartyModelConfig.readState(thirdPartyModelElement);
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);
        setEnumAttribute(element, "model-location", modelLocation);

        Element databaseModelElement = newElement(element, "database-model");
        databaseModelConfig.writeState(databaseModelElement);

        Element thirdPartyModelElement = newElement(element, "third-party-model");
        thirdPartyModelConfig.writeState(thirdPartyModelElement);
    }
}
