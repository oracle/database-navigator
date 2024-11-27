/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.connection.config;

import com.dbn.common.filter.CompositeFilter;
import com.dbn.common.filter.Filter;
import com.dbn.common.latent.Latent;
import com.dbn.common.options.CompositeProjectConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ui.ConnectionFilterSettingsForm;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.filter.custom.ObjectFilter;
import com.dbn.object.filter.custom.ObjectFilterSettings;
import com.dbn.object.filter.generic.FeaturedColumnsFilter;
import com.dbn.object.filter.generic.NonEmptySchemaFilter;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ConnectionFilterSettings extends CompositeProjectConfiguration<ConnectionSettings, ConnectionFilterSettingsForm> {
    
    private final @Getter(lazy = true) ObjectFilterSettings objectFilterSettings = new ObjectFilterSettings(this, getConnectionId());
    private final @Getter(lazy = true) ObjectTypeFilterSettings objectTypeFilterSettings = new ObjectTypeFilterSettings(this, getConnectionId());

    private boolean hideEmptySchemas = false;
    private boolean hidePseudoColumns = false;
    private boolean hideAuditColumns = false;

    private transient final Latent<Filter<DBSchema>> schemaFilter = Latent.basic(() -> loadSchemaFilter());
    private transient final Latent<Filter<DBColumn>> columnFilter = Latent.basic(() -> loadColumnFilter());

    @Nullable
    private Filter<DBSchema> loadSchemaFilter() {
        ObjectFilterSettings objectFilterSettings = getObjectFilterSettings();
        ObjectFilter<DBSchema> filter = objectFilterSettings.getFilter(DBObjectType.SCHEMA);
        if (filter == null) {
            return hideEmptySchemas ? NonEmptySchemaFilter.INSTANCE : null;
        } else {
            if (hideEmptySchemas) {
                return CompositeFilter.from(NonEmptySchemaFilter.INSTANCE, filter);
            } else {
                return filter;
            }
        }
    }

    @Nullable
    private Filter<DBColumn> loadColumnFilter() {
        ObjectFilterSettings objectFilterSettings = getObjectFilterSettings();
        ObjectFilter<DBColumn> filter = objectFilterSettings.getFilter(DBObjectType.COLUMN);
        Filter<DBColumn> featuredFilter = FeaturedColumnsFilter.get(hidePseudoColumns, hideAuditColumns);
        if (filter == null) {
            return featuredFilter;
        } else {
            if (featuredFilter != null) {
                return CompositeFilter.from(featuredFilter, filter);
            } else {
                return filter;
            }
        }
    }

    public void setHideEmptySchemas(boolean hideEmptySchemas) {
        if (this.hideEmptySchemas == hideEmptySchemas) return;

        this.hideEmptySchemas = hideEmptySchemas;
        this.schemaFilter.reset();
    }

    public void setHidePseudoColumns(boolean hidePseudoColumns) {
        if (this.hidePseudoColumns == hidePseudoColumns) return;

        this.hidePseudoColumns = hidePseudoColumns;
        this.columnFilter.reset();
    }

    public void setHideAuditColumns(boolean hideAuditColumns) {
        if (this.hideAuditColumns == hideAuditColumns) return;

        this.hideAuditColumns = hideAuditColumns;
        this.columnFilter.reset();

    }

    ConnectionFilterSettings(ConnectionSettings connectionSettings) {
        super(connectionSettings);
    }

    public ConnectionId getConnectionId() {
        return ensureParent().getConnectionId();
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.connection.title.FilterSettings");
    }

    @Override
    public String getHelpTopic() {
        return "connectionFilterSettings";
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @NotNull
    @Override
    public ConnectionFilterSettingsForm createConfigurationEditor() {
        return new ConnectionFilterSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "object-filters";
    }

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
                getObjectFilterSettings(),
                getObjectTypeFilterSettings()/*,
                getObjectNameFilterSettings()*/};
    }

    @Override
    public void readConfiguration(Element element) {
        hideEmptySchemas = booleanAttribute(element, "hide-empty-schemas", hideEmptySchemas);
        hidePseudoColumns = booleanAttribute(element, "hide-pseudo-columns", hidePseudoColumns);
        hideAuditColumns = booleanAttribute(element, "hide-audit-columns", hideAuditColumns);
        super.readConfiguration(element);

        schemaFilter.reset();
        columnFilter.reset();
    }

    @Override
    public void writeConfiguration(Element element) {
        setBooleanAttribute(element, "hide-empty-schemas", hideEmptySchemas);
        setBooleanAttribute(element, "hide-pseudo-columns", hidePseudoColumns);
        setBooleanAttribute(element, "hide-audit-columns", hideAuditColumns);
        super.writeConfiguration(element);
    }

    @Nullable
    public <T extends DBObject> Filter<T> getNameFilter(DBObjectType objectType) {
        return
            objectType == DBObjectType.SCHEMA ? cast(schemaFilter.get()) :
            objectType == DBObjectType.COLUMN ? cast(columnFilter.get()):
                cast(getObjectFilterSettings().getFilter(objectType));
    }

    public ConnectionFilterSettings clone() {
        Element element = new Element(getConfigElementName());
        writeConfiguration(element);
        ConnectionFilterSettings settings = new ConnectionFilterSettings(getParent());
        settings.readConfiguration(element);
        return settings;
    }
}
