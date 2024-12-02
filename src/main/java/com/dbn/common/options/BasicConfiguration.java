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

package com.dbn.common.options;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ref.WeakRef;
import com.dbn.options.TopLevelConfig;
import com.intellij.openapi.options.ConfigurationException;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.options.ConfigActivity.RESETTING;

@Getter
public abstract class BasicConfiguration<P extends Configuration, E extends ConfigurationEditorForm>
        extends AbstractConfiguration<P, E> {

    private final transient WeakRef<P> parent;
    private transient WeakRef<E> settingsEditor;
    private transient boolean modified = false;

    public BasicConfiguration(P parent) {
        this.parent = WeakRef.of(parent);
    }

    public P getParent() {
        return WeakRef.get(this.parent);
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return null;
    }

    public Icon getIcon() {
        return null;
    }

    @Override
    @NotNull
    public String getId() {
        return getClass().getName();
    }

    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nullable
    public final E getSettingsEditor() {
        return WeakRef.get(settingsEditor);
    }

    @NotNull
    public final E ensureSettingsEditor() {
        return Failsafe.nd(getSettingsEditor());
    }


    @Override
    @NotNull
    public JComponent createComponent() {
        E editorForm = createConfigurationEditor();
        this.settingsEditor = WeakRef.of(editorForm);
        return editorForm.getComponent();
    }

    public void setModified(boolean modified) {
        if (ConfigMonitor.is(RESETTING)) return;

        this.modified = modified;
    }

    @Override
    public void apply() throws ConfigurationException {
        E editorForm = getSettingsEditor();
        if (isValid(editorForm)) {
            editorForm.applyFormChanges();
        }
        modified = false;

        if (this instanceof TopLevelConfig) {
            TopLevelConfig topLevelConfig = (TopLevelConfig) this;
            Configuration originalSettings = topLevelConfig.getOriginalSettings();
            if (originalSettings != this ) {
                Element settingsElement = new Element("settings");
                writeConfiguration(settingsElement);
                originalSettings.readConfiguration(settingsElement);
            }

            // Notify only when all changes are set
            ConfigMonitor.notifyChanges();
        }
    }

    @Override
    public void reset() {
        try {
            ConfigMonitor.set(RESETTING, true);
            E editorForm = getSettingsEditor();
            if (editorForm != null) {
                editorForm.resetFormChanges();
            }
        } finally {
            modified = false;
            ConfigMonitor.set(RESETTING, false);
        }
    }

    @Override
    public void disposeUIResources() {
        settingsEditor = Disposer.replace(settingsEditor, null);
    }

    @NonNls
    public String getConfigElementName() {
        //throw new UnsupportedOperationException("Element name not defined for this configuration type.");
        return null;
    }

    protected static String nvl(String value) {
        return value == null ? "" : value;
    }


}
