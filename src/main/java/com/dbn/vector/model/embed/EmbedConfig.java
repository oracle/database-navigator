package com.dbn.vector.model.embed;

import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;

@Getter
@Setter
public class EmbedConfig implements PersistentStateElement {
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

    @Override
    public void readState(Element element) {
        if (element == null) return;

        modelLocation = enumAttribute(element, "model-location", modelLocation);

        Element databaseModelElement = element.getChild("database-model");
        databaseModelConfig.readState(databaseModelElement);

        Element thirdPartyModelElement = element.getChild("third-party-model");
        thirdPartyModelConfig.readState(thirdPartyModelElement);
    }

    @Override
    public void writeState(Element element) {
        setEnumAttribute(element, "model-location", modelLocation);

        Element databaseModelElement = newElement(element, "database-model");
        databaseModelConfig.writeState(databaseModelElement);

        Element thirdPartyModelElement = newElement(element, "third-party-model");
        thirdPartyModelConfig.writeState(thirdPartyModelElement);
    }
}
