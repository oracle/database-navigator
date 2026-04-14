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

package com.dbn.connection.config.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.common.util.Timers;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.driver.DatabaseDriverManager;
import com.dbn.driver.DriverBundle;
import com.dbn.driver.DriverSource;
import com.dbn.driver.download.DriverDownloadManager;
import com.dbn.driver.download.metadata.DriverPackage;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.io.File;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.ui.util.ComboBoxes.getElements;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.Popups.popupBuilder;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.util.FileChoosers.addFileChooser;
import static com.dbn.common.util.FileChoosers.singleFolderOrJar;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.connection.DatabaseType.GENERIC;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static java.util.concurrent.TimeUnit.SECONDS;


public class ConnectionDriverSettingsForm extends DBNFormBase {
    private @Getter TextFieldWithBrowseButton driverLibraryTextField;
    private JPanel mainPanel;
    private JComboBox<DriverSource> driverSourceComboBox;
    private @Getter JComboBox<DriverOption> driverComboBox;
    private JLabel driverLabel;
    private JLabel driverLibraryLabel;
    private JLabel driverSourceLabel;
    private HyperlinkLabel reloadDriversLink;
    private JLabel reloadDriversCheckLabel;
    private JButton downloadButton;
    private JLabel driverErrorLabel;

    ConnectionDriverSettingsForm(@NotNull ConnectionDatabaseSettingsForm parent) {
        super(parent);

        initComboBox(driverSourceComboBox, DriverSource.BUNDLED, DriverSource.EXTERNAL);
        driverSourceComboBox.addActionListener(e -> {
            DriverSource selection = getSelection(driverSourceComboBox);

            driverLibraryTextField.setEnabled(selection == DriverSource.EXTERNAL);
            driverComboBox.setEnabled(selection == DriverSource.EXTERNAL);


            updateDriverFields();
            //driverSetupPanel.setVisible(isExternalLibrary);
        });

        addFileChooser(
                getProject(),
                driverLibraryTextField,
                singleFolderOrJar(),
                txt("cfg.connection.title.SelectDriverLibrary"),
                txt("cfg.connection.text.LibraryDriverClasses"));

        driverErrorLabel.setText("");
        driverErrorLabel.setVisible(false);

        reloadDriversCheckLabel.setText("");
        reloadDriversCheckLabel.setIcon(Icons.COMMON_CHECK);
        reloadDriversCheckLabel.setVisible(false);
        reloadDriversLink.setHyperlinkText(txt("cfg.connection.link.ReloadDrivers"));
        reloadDriversLink.addHyperlinkListener(e -> {
            reloadDriversLink.setVisible(false);
            DatabaseDriverManager driverManager = DatabaseDriverManager.getInstance();
            File driverLibrary = new File(driverLibraryTextField.getText());
            DriverBundle drivers;
            try {
                drivers = driverManager.loadDrivers(driverLibrary, true);
                if (drivers == null || drivers.isEmpty()) {
                    reloadDriversCheckLabel.setIcon(Icons.COMMON_WARNING);
                    reloadDriversCheckLabel.setText(txt("cfg.connection.text.NoDriversFound"));
                } else {
                    reloadDriversCheckLabel.setIcon(Icons.COMMON_CHECK);
                    reloadDriversCheckLabel.setText(txt("cfg.connection.text.DriversReloaded"));
                }
            } catch (Exception ex) {
                conditionallyLog(ex);
                reloadDriversCheckLabel.setIcon(Icons.COMMON_WARNING);
                reloadDriversCheckLabel.setText(ex.getMessage());
            }
            reloadDriversCheckLabel.setVisible(true);

            Timers.executeLater("TemporaryLabelTimeout", 3, SECONDS, () -> {
                updateReloadLink();
                reloadDriversCheckLabel.setVisible(false);
            });
        });
        downloadButton.addActionListener(e -> {
            Progress.modal(ensureProject(),
                    null, true,
                    "Loading Drivers",
                    "Loading driver package metadata...",
                    indicator -> showDownloadPopup()
            );
        });
    }

    private void showDownloadPopup() {
        try {
            DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
            List<DriverPackage> driverPackages = downloadManager.getDownloadedDriverPackages(getDatabaseType());
            dispatch(() -> showDownloadPopup(downloadButton, driverPackages));
        } catch (Exception e) {
            conditionallyLog(e);
            Messages.showErrorDialog(ensureProject(), "Failed to download driver libraries metadata", e);
        }
    }

    public ConnectionDatabaseSettingsForm getParentForm() {
        return ensureParentComponent();
    }

    void updateDriverFields() {
        DatabaseType databaseType = getDatabaseType();
        boolean allowBuiltInLibrary = isBuiltInLibrarySupported(databaseType);

        driverSourceComboBox.setEnabled(allowBuiltInLibrary);
        if (!allowBuiltInLibrary) {
            setSelection(driverSourceComboBox, DriverSource.EXTERNAL);
        }

        DriverSource selectedDriver = getDriverSource();
        boolean externalDriver = selectedDriver == DriverSource.EXTERNAL;

        driverLibraryLabel.setVisible(externalDriver);
        driverLibraryTextField.setVisible(externalDriver);
        driverLabel.setVisible(externalDriver);
        driverComboBox.setVisible(externalDriver);
        downloadButton.setVisible(externalDriver);

        updateErrorLabel(null);
        updateReloadLink();

        if (!externalDriver) return;

        String driverLibrary = getDriverLibrary();
        JTextField libraryTextField = driverLibraryTextField.getTextField();
        libraryTextField.setForeground(Colors.getTextFieldForeground());


        // 1. check library availability
        boolean fileExists = Strings.isNotEmpty(driverLibrary) && fileExists(driverLibrary);
        if (!fileExists) {
            libraryTextField.setForeground(JBColor.RED);
            String error = isEmpty(driverLibrary) ?
                    txt("cfg.connection.error.DriverLibraryNotSpecified") :
                    txt("cfg.connection.error.CannotLocateDriverFile");
            updateDriversSelector(null);
            updateErrorLabel(error);
            return;
        }


        // 2. verify database type compatibility
        DatabaseType libraryDatabaseType = DatabaseType.resolve(driverLibrary);
        if (isBuiltInLibrarySupported(databaseType) && libraryDatabaseType != getDatabaseType() && libraryDatabaseType != GENERIC) {
            String error = txt("cfg.connection.error.DriverLibraryMismatch");
            updateDriversSelector(null);
            updateErrorLabel(error);
            return;
        }

        // 3. load the drivers
        Progress.modal(getProject(), null, true,
                "Loading Drivers",
                "Loading driver classes...",
                indicator -> loadDrivers(driverLibrary));

        ;
    }

    private void loadDrivers(String driverLibrary) {
        try {
            DatabaseDriverManager driverManager = DatabaseDriverManager.getInstance();
            DriverBundle drivers = driverManager.loadDrivers(new File(driverLibrary), false);
            updateDriversSelector(drivers);

            if (drivers == null || drivers.isEmpty()) {
                String error = txt("cfg.connection.error.InvalidDriverLibrary");
                updateErrorLabel(error);
            }
        } catch (Exception e) {
            conditionallyLog(e);

            updateDriversSelector(null);
            String error = e.getMessage();
            updateErrorLabel(error);
        }
    }

    private void updateDriversSelector(@Nullable DriverBundle drivers) {
        if (drivers == null) {
            initComboBox(driverComboBox);
            setSelection(driverComboBox, null);
            return;
        }

        DriverOption selectedOption = getSelection(driverComboBox);
        initComboBox(driverComboBox);

        List<DriverOption> driverOptions = new ArrayList<>();
        for (Class<Driver> driver : drivers.getDriverClasses()) {
            DriverOption driverOption = new DriverOption(driver);
            driverOptions.add(driverOption);
            if (selectedOption != null && selectedOption.getDriver().equals(driver)) {
                selectedOption = driverOption;
            }
        }
        initComboBox(driverComboBox, driverOptions);

        if (selectedOption == null && !driverOptions.isEmpty()) {
            selectedOption = driverOptions.get(0);
        }
        setSelection(driverComboBox, selectedOption);
    }

    private void updateErrorLabel(String error) {
        if (error != null) {
            driverErrorLabel.setIcon(Icons.COMMON_ERROR);
            driverErrorLabel.setText(error);
            driverErrorLabel.setVisible(true);
        } else {
            driverErrorLabel.setText("");
            driverErrorLabel.setVisible(false);
        }
    }

    private void updateReloadLink() {
        reloadDriversLink.setVisible(
                getDriverSource() == DriverSource.EXTERNAL &&
                        isDriverLibraryAccessible());
    }

    public DriverSource getDriverSource() {
        JComboBox<DriverSource> driverSourceComboBox = this.driverSourceComboBox;
        return driverSourceComboBox == null ? DriverSource.EXTERNAL : getSelection(driverSourceComboBox);
    }

    private boolean isBuiltInLibrarySupported(DatabaseType databaseType) {
        return databaseType != GENERIC;
    }

    private boolean isDriverLibraryAccessible() {
        String driverLibrary = getDriverLibrary();
        return Strings.isNotEmpty(driverLibrary) && new File(driverLibrary).exists();
    }

    public String getDriverLibrary() {
        return getText(driverLibraryTextField);
    }

    public DatabaseType getDatabaseType() {
        return getParentForm().getSelectedDatabaseType();
    }

    public DatabaseType getDriverDatabaseType() {
        DriverOption selectedDriver = getSelection(driverComboBox);
        return selectedDriver == null ? null : DatabaseType.resolve(selectedDriver.getName());
    }

    private static boolean fileExists(String driverLibrary) {
        return driverLibrary != null && new File(driverLibrary).exists();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public DriverOption getDriverOption() {
        return getSelection(driverComboBox);
    }


    public void resetFormChanges() {
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        ConnectionDatabaseSettings configuration = parent.getConfiguration();

        setSelection(driverSourceComboBox, configuration.getDriverSource());
        driverLibraryTextField.setText(configuration.getDriverLibrary());
        updateDriverFields();

        List<DriverOption> driverOptions = getElements(driverComboBox);
        setSelection(driverComboBox, DriverOption.get(driverOptions, configuration.getDriver()));
    }

    private void showDownloadPopup(JButton button, List<DriverPackage> driverPackages) {
        List<AnAction> actions = new ArrayList<>();
        for (DriverPackage driverPackage : driverPackages) {
                String title = Actions.adjustActionName(driverPackage.getName());
                actions.add(new DumbAwareAction(title, null, null) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        String downloadPath = getDownloadPath(driverPackage);
                        driverLibraryTextField.setText(downloadPath);
                    }
                });
        }
        actions.add(Separator.create());
        actions.add(new DumbAwareAction("Download Libraries...", null, AllIcons.Actions.Download) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                Project project = getProject();
                Progress.modal(project, null, true,
                        "Loading Drivers",
                        "Loading driver package metadata...",
                        indicator -> initDownloadManagerDialog(indicator));
            }
        });
        popupBuilder(actions, button).
                withTitle("Driver Libraries").
                withTitleVisible(false).
                withSpeedSearch().
                buildAndShow();
    }

    @Nullable
    private static String getDownloadPath(DriverPackage driverPackage) {
        if (driverPackage == null) return null;

        DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
        return downloadManager.getDownloadPath(driverPackage.getId());
    }

    private void initDownloadManagerDialog(ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        indicator.setFraction(0.0);

        Project project = ensureProject();
        DatabaseType databaseType = getDatabaseType();

        DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
        downloadManager.openDownloadDialog(project, databaseType, path -> {
            String currentPath = driverLibraryTextField.getText();
            if (Objects.equals(currentPath, path)) {
                // when download targets the already specified location (initially empty)
                updateDriverFields();
            } else {
                driverLibraryTextField.setText(path);
            }
        });
    }

}



