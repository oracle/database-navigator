package com.dbn.object.filter.type;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.browser.options.DatabaseBrowserSettings;
import com.dbn.common.filter.Filter;
import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.options.ProjectConfiguration;
import com.dbn.common.options.setting.BooleanSetting;
import com.dbn.connection.ConnectionId;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.filter.type.ui.ObjectTypeFilterSettingsForm;
import com.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ObjectTypeFilterSettings extends BasicProjectConfiguration<ProjectConfiguration, ObjectTypeFilterSettingsForm> {
    private final List<ObjectTypeFilterSetting> settings = Arrays.asList(
            new ObjectTypeFilterSetting(this, DBObjectType.SCHEMA),
            new ObjectTypeFilterSetting(this, DBObjectType.USER),
            new ObjectTypeFilterSetting(this, DBObjectType.ROLE),
            new ObjectTypeFilterSetting(this, DBObjectType.PRIVILEGE),
            new ObjectTypeFilterSetting(this, DBObjectType.CHARSET),
            new ObjectTypeFilterSetting(this, DBObjectType.TABLE),
            new ObjectTypeFilterSetting(this, DBObjectType.VIEW),
            new ObjectTypeFilterSetting(this, DBObjectType.MATERIALIZED_VIEW),
            new ObjectTypeFilterSetting(this, DBObjectType.NESTED_TABLE),
            new ObjectTypeFilterSetting(this, DBObjectType.COLUMN),
            new ObjectTypeFilterSetting(this, DBObjectType.INDEX),
            new ObjectTypeFilterSetting(this, DBObjectType.CONSTRAINT),
            new ObjectTypeFilterSetting(this, DBObjectType.DATASET_TRIGGER),
            new ObjectTypeFilterSetting(this, DBObjectType.DATABASE_TRIGGER),
            new ObjectTypeFilterSetting(this, DBObjectType.SYNONYM),
            new ObjectTypeFilterSetting(this, DBObjectType.SEQUENCE),
            new ObjectTypeFilterSetting(this, DBObjectType.PROCEDURE),
            new ObjectTypeFilterSetting(this, DBObjectType.FUNCTION),
            new ObjectTypeFilterSetting(this, DBObjectType.PACKAGE),
            new ObjectTypeFilterSetting(this, DBObjectType.TYPE),
            new ObjectTypeFilterSetting(this, DBObjectType.TYPE_ATTRIBUTE),
            new ObjectTypeFilterSetting(this, DBObjectType.ARGUMENT),
            new ObjectTypeFilterSetting(this, DBObjectType.JAVA_OBJECT),
            new ObjectTypeFilterSetting(this, DBObjectType.DIMENSION),
            new ObjectTypeFilterSetting(this, DBObjectType.CLUSTER),
            new ObjectTypeFilterSetting(this, DBObjectType.DBLINK),
            new ObjectTypeFilterSetting(this, DBObjectType.CREDENTIAL),
            new ObjectTypeFilterSetting(this, DBObjectType.AI_PROFILE));

    private final BooleanSetting useMasterSettings = new BooleanSetting("use-master-settings", true);

    private transient final ConnectionId connectionId;

    public ObjectTypeFilterSettings(ProjectConfiguration parent, @Nullable ConnectionId connectionId) {
        super(parent);
        this.connectionId = connectionId;
    }

    public ObjectTypeFilterSettings getMasterSettings() {
        if (isProjectLevel()) { // is project level
            return null;
        } else {
            DatabaseBrowserSettings databaseBrowserSettings = DatabaseBrowserSettings.getInstance(getProject());
            return databaseBrowserSettings.getFilterSettings().getObjectTypeFilterSettings();
        }
    }

    private boolean isProjectLevel() {
        return connectionId == null;
    }

    @NotNull
    @Override
    public ObjectTypeFilterSettingsForm createConfigurationEditor() {
        return new ObjectTypeFilterSettingsForm(this);
    }

    private final Filter<BrowserTreeNode> elementFilter = treeNode -> {
        if (treeNode == null) {
            return false;
        }

        if (treeNode instanceof DBObject) {
            DBObject object = (DBObject) treeNode;
            DBObjectType objectType = object.getObjectType();
            return isVisible(objectType);
        }

        if (treeNode instanceof DBObjectList) {
            DBObjectList objectList = (DBObjectList) treeNode;
            return isVisible(objectList.getObjectType());
        }

        return true;
    };

    private final Filter<DBObjectType> typeFilter = objectType -> objectType != null && isVisible(objectType);

    public boolean isVisible(DBObjectType objectType) {
        if (isProjectLevel()) return isSelected(objectType);

        ObjectTypeFilterSettings masterSettings = getMasterSettings();
        return useMasterSettings.value() ?
                masterSettings.isSelected(objectType) :
                masterSettings.isSelected(objectType) && isSelected(objectType);
    }

    private boolean isSelected(DBObjectType objectType) {
        ObjectTypeFilterSetting objectTypeEntry = getObjectTypeEntry(objectType);
        return objectTypeEntry == null || objectTypeEntry.isSelected();
    }

    private void setVisible(DBObjectType objectType, boolean visible) {
        ObjectTypeFilterSetting objectTypeEntry = getObjectTypeEntry(objectType);
        if (objectTypeEntry != null) {
            objectTypeEntry.setSelected(visible);
        }
    }


    private ObjectTypeFilterSetting getObjectTypeEntry(DBObjectType objectType) {
        for (ObjectTypeFilterSetting objectTypeEntry : getSettings()) {
            DBObjectType visibleObjectType = objectTypeEntry.getObjectType();
            if (visibleObjectType == objectType || objectType.isInheriting(visibleObjectType)) {
                return objectTypeEntry;
            }
        }
        return null;
    }

    public boolean isSelected(ObjectTypeFilterSetting objectFilterEntry) {
        for (ObjectTypeFilterSetting entry : getSettings()) {
            if (entry.equals(objectFilterEntry)) {
                return entry.isSelected();
            }
        }
        return false;
    }


    @Override
    public String getConfigElementName() {
        return "object-type-filter";
    }

    @Override
    public void readConfiguration(Element element) {
        useMasterSettings.readConfigurationAttribute(element);
        for (Element child : element.getChildren()) {
            String typeName = stringAttribute(child, "name");
            DBObjectType objectType = DBObjectType.get(typeName);
            if (objectType != null) {
                boolean enabled = Boolean.parseBoolean(stringAttribute(child, "enabled"));
                setVisible(objectType, enabled);
            }
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        if (!isProjectLevel()) {
            useMasterSettings.writeConfigurationAttribute(element);
        }

        for (ObjectTypeFilterSetting objectTypeEntry : getSettings()) {
            Element child = newElement(element, "object-type");
            child.setAttribute("name", objectTypeEntry.getObjectType().name());
            child.setAttribute("enabled", Boolean.toString(objectTypeEntry.isSelected()));
        }
    }
}
