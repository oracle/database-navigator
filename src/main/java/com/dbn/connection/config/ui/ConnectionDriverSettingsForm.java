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
import com.dbn.common.routine.Consumer;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.PresentableFactory;
import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.common.util.Timers;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.driver.DatabaseDriverManager;
import com.dbn.driver.DriverBundle;
import com.dbn.driver.DriverSource;
import com.dbn.driver.packages.DriverDownloadManager;
import com.dbn.driver.packages.DriverPackage;
import com.dbn.driver.packages.DriverPackageBundle;
import com.dbn.driver.packages.DriverPackageStatus;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.io.File;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.ui.util.ComboBoxes.getElements;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.util.Conditional.when;
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
    private ComboBox<String> driverLibraryComboBox;
    private DBNComboBox<PresentableDriverPackage> downloadedLibrariesComboBox;
    DownloadManagerDialog downloadManagerDialog;

    private static final FileChooserDescriptor LIBRARY_FILE_DESCRIPTOR = new FileChooserDescriptor(false, true, true, true, false, false);

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

        // TODO NLS
        driverLibraryTextField.addBrowseFolderListener(
                txt("cfg.connection.title.SelectDriverLibrary"),
                txt("cfg.connection.text.LibraryDriverClasses"),
                null, LIBRARY_FILE_DESCRIPTOR);

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
                updateDriverReloadLink();
                reloadDriversCheckLabel.setVisible(false);
            });
        });

        downloadedLibrariesComboBox.addActionListener(e->{
            PresentableDriverPackage presentableDriverPackage = getSelection(downloadedLibrariesComboBox);
            if(presentableDriverPackage!=null) {
                DriverPackage driverPackage = presentableDriverPackage.getDriverPackage();
                driverLibraryTextField.setText(driverPackage.getPath());
            }
        });
        populateDriverLibraryAction();
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

        String error = null;
        DriverSource selectedDriver = getDriverSource();

        driverLibraryLabel.setVisible(selectedDriver == DriverSource.EXTERNAL);
        driverLibraryTextField.setVisible(selectedDriver == DriverSource.EXTERNAL);
        driverLabel.setVisible(selectedDriver == DriverSource.EXTERNAL);
        driverComboBox.setVisible(selectedDriver == DriverSource.EXTERNAL);
        downloadedLibrariesComboBox.setVisible(selectedDriver == DriverSource.EXTERNAL);

        updateDriverReloadLink();

        if (selectedDriver == DriverSource.EXTERNAL) {
            String driverLibrary = getDriverLibrary();

            boolean fileExists = Strings.isNotEmpty(driverLibrary) && fileExists(driverLibrary);
            JTextField libraryTextField = driverLibraryTextField.getTextField();
            if (fileExists) {
                libraryTextField.setForeground(Colors.getTextFieldForeground());
                DatabaseType libraryDatabaseType = DatabaseType.resolve(driverLibrary);
                if (isBuiltInLibrarySupported(databaseType) && libraryDatabaseType != getDatabaseType()) {
                    error = txt("cfg.connection.error.DriverLibraryMismatch");
                    initComboBox(driverComboBox);
                    setSelection(driverComboBox, null);
                } else {
                    DatabaseDriverManager driverManager = DatabaseDriverManager.getInstance();
                    DriverBundle drivers = null;
                    try {
                        drivers = driverManager.loadDrivers(new File(driverLibrary), false);
                    } catch (Exception e) {
                        conditionallyLog(e);
                        Messages.showErrorDialog(getProject(), e.getMessage());
                    }
                    DriverOption selectedOption = getSelection(driverComboBox);
                    initComboBox(driverComboBox);
                    //driverComboBox.addItem("");
                    if (drivers != null && !drivers.isEmpty()) {
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
                    } else {
                        error = txt("cfg.connection.error.InvalidDriverLibrary");
                    }
                    setSelection(driverComboBox, selectedOption);
                }
            } else {
                libraryTextField.setForeground(JBColor.RED);
                if (Strings.isEmpty(driverLibrary)) {
                    error = txt("cfg.connection.error.DriverLibraryNotSpecified");
                } else {
                    error = txt("cfg.connection.error.CannotLocateDriverFile");
                }
                initComboBox(driverComboBox);
                //driverComboBox.addItem("");
            }
        }
    }

    private void updateDriverReloadLink() {
        reloadDriversLink.setVisible(
                getDriverSource() == DriverSource.EXTERNAL &&
                        isDriverLibraryAccessible());
    }

    public DriverSource getDriverSource() {
        return getSelection(driverSourceComboBox);
    }

    private boolean isBuiltInLibrarySupported(DatabaseType databaseType) {
        return databaseType != DatabaseType.GENERIC;
    }

    private boolean isDriverLibraryAccessible() {
        String driverLibrary = getDriverLibrary();
        return Strings.isNotEmpty(driverLibrary) && new File(driverLibrary).exists();
    }

    public String getDriverLibrary() {
        return driverLibraryTextField.getTextField().getText();
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

    public void updateExternalOption(DriverPackage driverPackage) {
        populateDriverLibraryAction();
        driverLibraryTextField.setText(driverPackage.getPath());
    }
    public void populateDriverLibraryAction() {
        downloadedLibrariesComboBox.set(ValueSelectorOption.HIDE_ICON, true);
        downloadedLibrariesComboBox.setValueFactory(new PresentableFactory<>(txt("cfg.connection.link.DownloadLibraries")) {
            @Override
            public void create(Consumer<PresentableDriverPackage> consumer) {
                downloadManagerDialog = new DownloadManagerDialog(ensureProject(), getDatabaseType());
                Dialogs.show(() -> downloadManagerDialog, (dialog, exitCode) -> {
                    when(exitCode != DialogWrapper.CANCEL_EXIT_CODE, () -> updateExternalOption(dialog.selectedDriverPackage));
                });
            }

            @Override
            public Icon getIcon() {
                return AllIcons.Actions.Download;
            }
        });

        downloadedLibrariesComboBox.clearValues();
        List<PresentableDriverPackage> presentableList = DriverDownloadManager.getInstance().getPackagesStatus().stream().filter(DriverPackageStatus::isComplete).map(packageStatus -> new PresentableDriverPackage(DriverPackageBundle.getDriverPackage(packageStatus.getPackageId()))).collect(Collectors.toList());
        downloadedLibrariesComboBox.addValues(presentableList);
        downloadedLibrariesComboBox.setSelectedItem(!presentableList.isEmpty()?presentableList.get(0):null);
    }

    public static class PresentableDriverPackage implements Presentable {

        private final DriverPackage driverPackage;

        public PresentableDriverPackage(DriverPackage driverPackage) {
            this.driverPackage = driverPackage;
        }

        @Override
        public String getName() {
            return driverPackage.getName();
        }

        public DriverPackage getDriverPackage() { return driverPackage; }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public Icon getIcon() {
            return null;
        }
    }

}



