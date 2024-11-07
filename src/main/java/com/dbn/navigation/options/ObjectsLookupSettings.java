package com.dbn.navigation.options;

import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.options.setting.BooleanSetting;
import com.dbn.common.ui.list.Selectable;
import com.dbn.navigation.options.ui.ObjectsLookupSettingsForm;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.options.ConfigurationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.List;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.cachedUpperCase;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ObjectsLookupSettings extends BasicProjectConfiguration<NavigationSettings, ObjectsLookupSettingsForm> {
    private final List<ObjectTypeEntry> lookupObjectTypes = Arrays.asList(
        new ObjectTypeEntry(DBObjectType.SCHEMA, true),
        new ObjectTypeEntry(DBObjectType.USER, false),
        new ObjectTypeEntry(DBObjectType.ROLE, false),
        new ObjectTypeEntry(DBObjectType.PRIVILEGE, false),
        new ObjectTypeEntry(DBObjectType.CHARSET, false),
        new ObjectTypeEntry(DBObjectType.TABLE, true),
        new ObjectTypeEntry(DBObjectType.VIEW, true),
        new ObjectTypeEntry(DBObjectType.MATERIALIZED_VIEW, true),
        //new ObjectTypeEntry(DBObjectType.NESTED_TABLE, false),
        //new ObjectTypeEntry(DBObjectType.COLUMN, false),
        new ObjectTypeEntry(DBObjectType.INDEX, true),
        new ObjectTypeEntry(DBObjectType.CONSTRAINT, true),
        new ObjectTypeEntry(DBObjectType.DATASET_TRIGGER, true),
        new ObjectTypeEntry(DBObjectType.DATABASE_TRIGGER, true),
        new ObjectTypeEntry(DBObjectType.SYNONYM, false),
        new ObjectTypeEntry(DBObjectType.SEQUENCE, true),
        new ObjectTypeEntry(DBObjectType.PROCEDURE, true),
        new ObjectTypeEntry(DBObjectType.FUNCTION, true),
        new ObjectTypeEntry(DBObjectType.PACKAGE, true),
        new ObjectTypeEntry(DBObjectType.TYPE, true),
        //new ObjectTypeEntry(DBObjectType.TYPE_ATTRIBUTE, false),
        //new ObjectTypeEntry(DBObjectType.ARGUMENT, false),
        new ObjectTypeEntry(DBObjectType.JAVA_OBJECT, true),
        new ObjectTypeEntry(DBObjectType.DIMENSION, false),
        new ObjectTypeEntry(DBObjectType.CLUSTER, false),
        new ObjectTypeEntry(DBObjectType.DBLINK, false),
        new ObjectTypeEntry(DBObjectType.CREDENTIAL, false)
    );

    private final BooleanSetting forceDatabaseLoad = new BooleanSetting("force-database-load", false);
    private final BooleanSetting promptConnectionSelection = new BooleanSetting("prompt-connection-selection", true);
    private final BooleanSetting promptSchemaSelection = new BooleanSetting("prompt-schema-selection", true);

    public ObjectsLookupSettings(NavigationSettings parent) {
        super(parent);
    }

    @NotNull
    @Override
    public ObjectsLookupSettingsForm createConfigurationEditor() {
        return new ObjectsLookupSettingsForm(this);
    }

    @Override
    public void apply() throws ConfigurationException {
        super.apply();
    }

    public boolean isEnabled(DBObjectType objectType) {
        for (ObjectTypeEntry objectTypeEntry : lookupObjectTypes) {
            if (objectTypeEntry.getObjectType() == objectType) {
                return objectTypeEntry.isSelected();
            }
        }
        return false;
    }

    private ObjectTypeEntry getObjectTypeEntry(DBObjectType objectType) {
        for (ObjectTypeEntry objectTypeEntry : getLookupObjectTypes()) {
            DBObjectType visibleObjectType = objectTypeEntry.getObjectType();
            if (visibleObjectType == objectType || objectType.isInheriting(visibleObjectType)) {
                return objectTypeEntry;
            }
        }
        return null;
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    public String getConfigElementName() {
        return "lookup-filters";
    }

    @Override
    public void readConfiguration(Element element) {
        Element visibleObjectsElement = element.getChild("lookup-objects");
        for (Element child : visibleObjectsElement.getChildren()) {
            String typeName = stringAttribute(child, "name");
            DBObjectType objectType = DBObjectType.get(typeName);
            if (objectType != null) {
                boolean enabled = booleanAttribute(child, "enabled", false);
                ObjectTypeEntry objectTypeEntry = getObjectTypeEntry(objectType);
                if (objectTypeEntry != null) {
                    objectTypeEntry.setSelected(enabled);
                }
            }
        }
        forceDatabaseLoad.readConfiguration(element);
        promptConnectionSelection.readConfiguration(element);
        promptSchemaSelection.readConfiguration(element);
    }

    @Override
    public void writeConfiguration(Element element) {
        Element visibleObjectsElement = newElement(element, "lookup-objects");

        for (ObjectTypeEntry objectTypeEntry : getLookupObjectTypes()) {
            Element child = newElement(visibleObjectsElement, "object-type");
            child.setAttribute("name", objectTypeEntry.getName());
            child.setAttribute("enabled", Boolean.toString(objectTypeEntry.isSelected()));
        }
        forceDatabaseLoad.writeConfiguration(element);
        promptConnectionSelection.writeConfiguration(element);
        promptSchemaSelection.writeConfiguration(element);
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    private static class ObjectTypeEntry implements Selectable<ObjectTypeEntry> {
        private final DBObjectType objectType;
        private boolean selected;

        private ObjectTypeEntry(DBObjectType objectType, boolean selected) {
            this.objectType = objectType;
            this.selected = selected;
        }

        @Override
        public Icon getIcon() {
            return objectType.getIcon();
        }

        @Override
        @NotNull
        public String getName() {
            return cachedUpperCase(objectType.getName());
        }

        @Override
        public String getError() {
            return null;
        }

        @Override
        public boolean isMasterSelected() {
            return true;
        }

        @Override
        public int compareTo(ObjectTypeEntry remote) {
            return objectType.compareTo(remote.objectType);
        }
    }
}
