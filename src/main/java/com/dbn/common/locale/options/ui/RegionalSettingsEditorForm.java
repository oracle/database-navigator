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

package com.dbn.common.locale.options.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.locale.DBDateFormat;
import com.dbn.common.locale.DBNumberFormat;
import com.dbn.common.locale.Formatter;
import com.dbn.common.locale.options.RegionalSettings;
import com.dbn.common.locale.options.RegionalSettingsListener;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.ValueSelectorListener;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Commons;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Date;
import java.util.Locale;

import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class RegionalSettingsEditorForm extends ConfigurationEditorForm<RegionalSettings> {
    private JPanel mainPanel;
    private JPanel previewPanel;
    private JLabel numberPreviewLabel;
    private JLabel integerPreviewLabel;
    private JLabel datePreviewLabel;
    private JLabel timePreviewLabel;
    private JLabel errorLabel;
    private JTextField customNumberFormatTextField;
    private JTextField customDateFormatTextField;
    private JTextField customTimeFormatTextField;
    private JRadioButton presetPatternsRadioButton;
    private JRadioButton customPatternsRadioButton;
    private DBNComboBox<LocaleOption> localeComboBox;
    private DBNComboBox<DBNumberFormat> numberFormatComboBox;
    private DBNComboBox<DBDateFormat> dateFormatComboBox;

    boolean isUpdating = false;


    private final Date previewDate = new Date();
    private final double previewNumber = ((double)(System.currentTimeMillis()/1000))/1000;

    public RegionalSettingsEditorForm(RegionalSettings regionalSettings) {
        super(regionalSettings);
        previewPanel.setBorder(Borders.COMPONENT_OUTLINE_BORDER);
        previewPanel.setBackground(Colors.getLighterPanelBackground());
        errorLabel.setVisible(false);
        initComboBox(localeComboBox, LocaleOption.ALL);
        initComboBox(numberFormatComboBox,
                DBNumberFormat.UNGROUPED,
                DBNumberFormat.GROUPED);

        initComboBox(dateFormatComboBox,
                DBDateFormat.SHORT,
                DBDateFormat.MEDIUM,
                DBDateFormat.LONG,
                DBDateFormat.FULL);

        ValueSelectorListener previewListener = (oldValue, newValue) -> updatePreview();

        resetFormChanges();
        updatePreview();

        Font labelFont = Fonts.getEditorFont();
        Font previewFont = Fonts.deriveFont(labelFont, (float) (labelFont.getSize() * 1.2));
        numberPreviewLabel.setFont(previewFont);
        integerPreviewLabel.setFont(previewFont);
        datePreviewLabel.setFont(previewFont);
        timePreviewLabel.setFont(previewFont);

        numberFormatComboBox.addListener(previewListener);
        dateFormatComboBox.addListener(previewListener);
        localeComboBox.addListener(previewListener);
        registerComponent(mainPanel);
    }

    @Override
    protected ItemListener createItemListener() {
        return e -> {
            getConfiguration().setModified(true);
            updatePreview();
        };
    }

    @Override
    protected ActionListener createActionListener() {
        return e -> {
            getConfiguration().setModified(true);
            updatePreview();
        };
    }

    @Override
    protected DocumentListener createDocumentListener() {
        return new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                getConfiguration().setModified(true);
                updatePreview();
            }
        };
    }

    @NotNull
    public Locale getSelectedLocale() {
        LocaleOption localeOption = localeComboBox.getSelectedValue();
        Locale defaultLocale = Locale.getDefault();
        return localeOption == null ? defaultLocale : Commons.nvl(localeOption.getLocale(), defaultLocale);
    }

    public void setSelectedLocale(Locale locale) {
        LocaleOption localeOption = LocaleOption.get(locale);
        localeComboBox.setSelectedValue(localeOption);
    }

    private void updatePreview() {
        if (isUpdating) return;
        isUpdating = true;
        try {
            Locale locale = getSelectedLocale();
            DBDateFormat dateFormat = dateFormatComboBox.getSelectedValue();
            DBNumberFormat numberFormat = numberFormatComboBox.getSelectedValue();
            boolean customSettings = customPatternsRadioButton.isSelected();
            Formatter formatter = null;
            if (customSettings) {
                try {
                    formatter = new Formatter(
                            0,
                            locale,
                            customDateFormatTextField.getText(),
                            customTimeFormatTextField.getText(),
                            customNumberFormatTextField.getText());
                    errorLabel.setVisible(false);
                } catch (Exception e) {
                    conditionallyLog(e);
                    errorLabel.setText("Invalid pattern: " + e.getMessage());
                    errorLabel.setIcon(Icons.STMT_EXECUTION_ERROR);
                    errorLabel.setVisible(true);
                }
            } else {
                formatter = new Formatter(0, locale, dateFormat == null ? DBDateFormat.MEDIUM : dateFormat, numberFormat);
                customNumberFormatTextField.setText(formatter.getNumberFormatPattern());
                customDateFormatTextField.setText(formatter.getDateFormatPattern());
                customTimeFormatTextField.setText(formatter.getTimeFormatPattern());
            }

            if (formatter != null) {
                datePreviewLabel.setText(formatter.formatDate(previewDate));
                timePreviewLabel.setText(formatter.formatTime(previewDate));
                numberPreviewLabel.setText(formatter.formatNumber(previewNumber));
                integerPreviewLabel.setText(formatter.formatInteger(previewNumber));
            }

            numberFormatComboBox.setEnabled(!customSettings);
            dateFormatComboBox.setEnabled(!customSettings);

            customNumberFormatTextField.setEnabled(customSettings);
            customDateFormatTextField.setEnabled(customSettings);
            customTimeFormatTextField.setEnabled(customSettings);
        } finally {
            isUpdating = false;
        }
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        RegionalSettings configuration = getConfiguration();
        boolean modified = configuration.isModified();

        Locale locale = getSelectedLocale();
        configuration.setLocale(locale);

        DBDateFormat dateFormat = dateFormatComboBox.getSelectedValue();
        configuration.setDateFormatOption(dateFormat);

        DBNumberFormat numberFormat = numberFormatComboBox.getSelectedValue();
        configuration.setNumberFormatOption(numberFormat);
        
        configuration.getUseCustomFormats().to(customPatternsRadioButton);
        configuration.getCustomDateFormat().to(customDateFormatTextField);
        configuration.getCustomTimeFormat().to(customTimeFormatTextField);
        configuration.getCustomNumberFormat().to(customNumberFormatTextField);


        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
            if (modified) {
                ProjectEvents.notify(project,
                        RegionalSettingsListener.TOPIC,
                        (listener) -> listener.settingsChanged());
            }});
    }

    @Override
    public void resetFormChanges() {
        RegionalSettings regionalSettings = getConfiguration();
        setSelectedLocale(regionalSettings.getLocale());

        Boolean useCustomFormats = regionalSettings.getUseCustomFormats().value();
        customPatternsRadioButton.setSelected(useCustomFormats);
        presetPatternsRadioButton.setSelected(!useCustomFormats);
        if (customPatternsRadioButton.isSelected()) {
            regionalSettings.getCustomDateFormat().from(customDateFormatTextField);
            regionalSettings.getCustomTimeFormat().from(customTimeFormatTextField);
            regionalSettings.getCustomNumberFormat().from(customNumberFormatTextField);
        }

        DBDateFormat dateFormat = regionalSettings.getDateFormatOption();
        dateFormatComboBox.setSelectedValue(dateFormat);

        DBNumberFormat numberFormat = regionalSettings.getNumberFormatOption();
        numberFormatComboBox.setSelectedValue(numberFormat);
    }
}
