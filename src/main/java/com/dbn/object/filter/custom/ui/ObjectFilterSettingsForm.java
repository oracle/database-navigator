package com.dbn.object.filter.custom.ui;

import com.dbn.browser.options.ObjectFilterChangeListener;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.ValueSelector;
import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Dialogs;
import com.dbn.object.filter.custom.ObjectFilter;
import com.dbn.object.filter.custom.ObjectFilterSettings;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.ui.util.ComponentAligner.alignFormComponents;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.common.util.Strings.toUpperCase;

public class ObjectFilterSettingsForm extends ConfigurationEditorForm<ObjectFilterSettings> implements ComponentAligner.Container {
    private JPanel mainPanel;
    private JPanel filtersPanel;
    private JPanel actionsPanel;

    private final List<ObjectFilterExpressionForm> filterForms = DisposableContainers.list(this);
    private final Set<DBObjectType> modifiedFilters = new HashSet<>();

    public ObjectFilterSettingsForm(ObjectFilterSettings configuration) {
        super(configuration);

        filtersPanel.setLayout(new BoxLayout(filtersPanel, BoxLayout.Y_AXIS));
        //filtersPanel.setBorder(new CompoundBorder(Borders.BOTTOM_LINE_BORDER, JBUI.Borders.emptyBottom(4)));

        actionsPanel.add(new ObjectTypeSelector(), BorderLayout.WEST);

        configuration.getFilters().forEach(f -> createFilterPanel(f));
        alignFormComponents(this);
    }

    @Override
    public List<ObjectFilterExpressionForm> getAlignableForms() {
        return filterForms;
    }

    public boolean markModified(ObjectFilter<?> filter) {
        getConfiguration().setModified(true);
        return modifiedFilters.add(filter.getObjectType());
    }

    private void addFilterPanel(ObjectFilter<?> filter) {
        markModified(filter);
        createFilterPanel(filter);
    }
    private void createFilterPanel(ObjectFilter<?> filter) {
        ObjectFilterExpressionForm expressionForm = new ObjectFilterExpressionForm(this, filter);
        filtersPanel.add(expressionForm.getComponent());
        filterForms.add(expressionForm);
        alignFormComponents(this);
    }

    public void removeFilterPanel(ObjectFilter<?> filter) {
        markModified(filter);
        for (ObjectFilterExpressionForm conditionForm : filterForms) {
            if (conditionForm.getFilter() == filter) {
                filterForms.remove(conditionForm);
                filtersPanel.remove(conditionForm.getComponent());
                break;
            }
        }

        alignFormComponents(this);
        UserInterface.repaint(mainPanel);
    }

    public void showFilterDetailsDialog(ObjectFilter<?> filter, boolean create, Runnable callback) {
        Dialogs.show(() -> new ObjectFilterDetailsDialog(filter, create),
                (dialog, exitCode) -> when(exitCode == 0, callback));
    }

    public void showFilterPreview(ObjectFilter<?> filter, Component source) {
        ObjectFilterPreviewPopup previewPopup = new ObjectFilterPreviewPopup(filter);
        previewPopup.show(source);
    }

    private class ObjectTypeSelector extends ValueSelector<DBObjectType> {
        ObjectTypeSelector() {
            super(PlatformIcons.ADD_ICON, "Add Filter", null, ValueSelectorOption.HIDE_DESCRIPTION);
            addListener((oldValue, newValue) -> {
                createFilter(newValue);
                resetValues();
            });
        }

        @Override
        public List<DBObjectType> loadValues() {
            List<DBObjectType> objectTypes = new ArrayList<>(Arrays.asList(
                    DBObjectType.SCHEMA,
                    DBObjectType.TABLE,
                    DBObjectType.VIEW,
                    DBObjectType.COLUMN,
                    DBObjectType.CONSTRAINT,
                    DBObjectType.INDEX,
                    DBObjectType.TRIGGER,
                    DBObjectType.FUNCTION,
                    DBObjectType.PROCEDURE,
                    DBObjectType.PACKAGE,
                    DBObjectType.TYPE,
                    DBObjectType.SYNONYM,
                    DBObjectType.JAVA_OBJECT,
                    DBObjectType.DBLINK));

            List<DBObjectType> configuredObjectTypes = convert(filterForms, f -> f.getFilter().getObjectType());
            objectTypes.removeAll(configuredObjectTypes);
            return objectTypes;
        }

        @Override
        public String getOptionDisplayName(DBObjectType value) {
            return toUpperCase(value.getName());
        }
    }

    private void createFilter(DBObjectType objectType) {
        ObjectFilterSettings filterSettings = getConfiguration();
        ObjectFilter<?> filter = new ObjectFilter<>(filterSettings);
        filter.setObjectType(objectType);

        showFilterDetailsDialog(filter, true, () -> addFilterPanel(filter));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ObjectFilterSettings configuration = getConfiguration();
        boolean notifyFilterListeners = configuration.isModified();

        List<ObjectFilter<?>> filters = convert(filterForms, f -> f.getFilter());
        getConfiguration().setFilters(filters);

        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
            if (!notifyFilterListeners) return;

            DBObjectType[] refreshObjectTypes = modifiedFilters.toArray(new DBObjectType[0]);
            ProjectEvents.notify(project, ObjectFilterChangeListener.TOPIC,
                    (listener) -> listener.nameFiltersChanged(configuration.getConnectionId(), refreshObjectTypes));
        });
    }

    @Override
    public void resetFormChanges() {
    }
}
