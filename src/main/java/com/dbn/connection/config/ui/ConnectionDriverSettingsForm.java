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

import com.dbn.common.Result;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.Threads;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.connection.config.provider.CloudConfigProviderType;
import com.dbn.driver.DatabaseDriverManager;
import com.dbn.driver.DriverBundle;
import com.dbn.driver.DriverSource;
import com.dbn.driver.download.DriverDownloadManager;
import com.dbn.driver.download.metadata.DriverPackage;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.ui.AsyncProcessIcon;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.File;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.thread.Dispatch.async;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;
import static com.dbn.common.ui.util.ComboBoxes.getElements;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.Popups.popupBuilder;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.installErrorHighlighting;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.ui.util.TextFields.setTextSilently;
import static com.dbn.common.util.FileChoosers.addFileChooser;
import static com.dbn.common.util.FileChoosers.singleFolderOrJar;
import static com.dbn.common.util.Lists.firstElement;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.connection.DatabaseType.GENERIC;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;


public class ConnectionDriverSettingsForm extends DBNFormBase {
    private @Getter TextFieldWithBrowseButton driverLibraryTextField;
    private JPanel mainPanel;
    private JComboBox<DriverSource> driverSourceComboBox;
    private @Getter JComboBox<DriverOption> driverComboBox;
    private JLabel driverLabel;
    private JLabel driverLibraryLabel;
    private JLabel driverSourceLabel;
    private HyperlinkLabel reloadDriversLink;
    private HyperlinkLabel useExternalLibraryLink;
    private JButton downloadButton;
    private JLabel driverErrorLabel;
    private JPanel loadingDriversPanel;

    private Throwable driverError;
    private boolean loadingDrivers;

    ConnectionDriverSettingsForm(@NotNull ConnectionDatabaseSettingsForm parent) {
        super(parent);

        initDriverSourceFields();
        initDriverLibraryFields();
        initDriverStatusFields();
        initDriverDownloadFields();

        whenFirstShown(() -> loadDrivers());
    }

    private void initDriverSourceFields() {
        initComboBox(driverSourceComboBox, DriverSource.BUNDLED, DriverSource.EXTERNAL);
        onSelectionChange(driverSourceComboBox,e -> {
            DriverSource selection = getSelection(driverSourceComboBox);

            driverLibraryTextField.setEnabled(selection == DriverSource.EXTERNAL);
            driverComboBox.setEnabled(selection == DriverSource.EXTERNAL);

            updateDriverFields();
        });
    }

    private void initDriverLibraryFields() {
        addFileChooser(
                getProject(),
                driverLibraryTextField,
                singleFolderOrJar(),
                txt("cfg.connection.title.SelectDriverLibrary"),
                txt("cfg.connection.text.LibraryDriverClasses"));

        onTextChange(driverLibraryTextField, e -> reloadDrivers());
        installErrorHighlighting(driverLibraryTextField, s -> isNotEmpty(s) && !fileExists(s) ? txt("cfg.connection.error.DriverLibraryNotFileOrDirectory") : null);
    }

    private void initDriverStatusFields() {
        loadingDriversPanel.add(new AsyncProcessIcon("Loading drivers..."), BorderLayout.WEST);
        reloadDriversLink.setHyperlinkText(txt("cfg.connection.link.ReloadDrivers"));
        onHyperlinkAccess(reloadDriversLink, e -> reloadDrivers());

        useExternalLibraryLink.setHyperlinkText(txt("cfg.connection.link.UseExternalLibrary"));
        onHyperlinkAccess(useExternalLibraryLink, e -> useExternalLibrary());

        driverErrorLabel.setText("");
        driverErrorLabel.setIcon(Icons.COMMON_ERROR);
    }

    private void reloadDrivers() {
        loadDrivers();
    }

    private void loadDrivers() {
        if (!isExternalDriver()) return;

        if (loadingDrivers) return;
        loadingDrivers = true;
        driverError = null;
        updateFieldAvailability();

        async(mainPanel,
                () -> loadDriverBundle(),
                r -> applyDriverBundle(r));
    }

    private Result<DriverBundle> loadDriverBundle() {
        try {
            Threads.sleep(500);
            String error = verifyDriverLibrary();
            if (error != null) {
                throw new IllegalArgumentException(error);
            }

            File driverLibrary = getDriverLibraryFile();
            if (driverLibrary == null) return new Result<>(null);

            DatabaseDriverManager driverManager = DatabaseDriverManager.getInstance();
            DriverBundle driverBundle = driverManager.loadDrivers(driverLibrary, true);
            return new Result<>(driverBundle);
        } catch (Exception e) {
            conditionallyLog(e);
            return new Result<>(e);
        } finally {
            loadingDrivers = false;
            updateFieldAvailability();
        }
    }

    private void applyDriverBundle(Result<DriverBundle> result) {
        if (result.isSuccess()) {
            updateDriversSelector(result.getValue());
            driverError = null;
        } else {
            updateDriversSelector(null);
            driverError = result.getError();

            String message = Exceptions.rootCauseOf(driverError).getMessage();
            driverErrorLabel.setText(message);
        }
        updateFieldAvailability();
    }

    private void initDriverDownloadFields() {
        downloadButton.addActionListener(e -> {
            DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
            CloudConfigProviderFamily providerFamily = getCloudConfigProviderFamily();
            if (downloadManager.isDriverPackageMetadataOutdated(getDatabaseType(), providerFamily)) {
                Progress.modal(ensureProject(),
                        null, true,
                        txt("prc.connection.title.LoadingDrivers"),
                        txt("prc.connection.text.LoadingDriverPackageMetadata"),
                        indicator -> showDownloadPopup(providerFamily)
                );
            } else {
                Background.run(() -> showDownloadPopup(providerFamily));
            }
        });
    }

    private void useExternalLibrary() {
        setSelection(driverSourceComboBox, DriverSource.EXTERNAL);
        driverLibraryTextField.getTextField().requestFocusInWindow();
        downloadButton.doClick();
    }

    private void showDownloadPopup(@Nullable CloudConfigProviderFamily providerFamily) {
        try {
            DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
            List<DriverPackage> driverPackages = downloadManager.getDownloadedDriverPackages(getDatabaseType(), providerFamily);
            dispatch(() -> showDownloadPopup(downloadButton, driverPackages, providerFamily));
        } catch (Exception e) {
            conditionallyLog(e);
            showErrorDialog(ensureProject(), txt("msg.driver.error.DriverLibrariesMetadataDownloadFailed"), e);
        }
    }

    public ConnectionDatabaseSettingsForm getParentForm() {
        return ensureParentComponent();
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(() -> isExternalDriver(), array(
                driverLibraryLabel,
                driverLibraryTextField,
                driverLabel,
                driverComboBox,
                downloadButton));

        fieldAdapter.initFieldsVisibility(() -> loadingDrivers && isExternalDriver(), array(loadingDriversPanel));
        fieldAdapter.initFieldsVisibility(() -> !loadingDrivers && isExternalDriver(), array(reloadDriversLink));
        fieldAdapter.initFieldsVisibility(() -> isDriverErrorVisible(), array(driverErrorLabel));
        fieldAdapter.initFieldsVisibility(() -> isCloudProviderSupportRequired(), array(useExternalLibraryLink));
        fieldAdapter.initFieldsAvailability(() -> !loadingDrivers, array(driverComboBox));
    }

    void updateDriverFields() {
        DatabaseType databaseType = getDatabaseType();
        boolean allowBuiltInLibrary = isBuiltInLibrarySupported(databaseType);

        driverSourceComboBox.setEnabled(allowBuiltInLibrary);
        if (!allowBuiltInLibrary) {
            setSelection(driverSourceComboBox, DriverSource.EXTERNAL);
        }

        updateDriverStatusMessage();
        updateFieldAvailability();
    }

    private void updateDriverStatusMessage() {
        if (driverError != null && isExternalDriver()) return;

        CloudConfigProviderType provider = getParentForm().getExternalLibraryCloudProvider();
        driverErrorLabel.setText(isCloudProviderSupportRequired() ?
                txt("cfg.connection.error.CloudProviderSupportRequired", getCloudProviderName(provider)) :
                "");
    }

    private boolean isDriverErrorVisible() {
        return driverError != null && isExternalDriver() || isCloudProviderSupportRequired();
    }

    private boolean isCloudProviderSupportRequired() {
        return getParentForm().getExternalLibraryCloudProvider() != null && !isExternalDriver();
    }

    private static String getCloudProviderName(CloudConfigProviderType provider) {
        if (provider == null) return "";
        if (provider.isAzure()) return "Azure";
        if (provider.isAws()) return "AWS";
        if (provider.isGcp()) return "GCP";
        if (provider.isHashicorp()) return "HashiCorp";

        return provider.getName();
    }

    private String verifyDriverLibrary() {
        if (!isExternalDriver()) return null;

        String driverLibrary = getDriverLibrary();

        // 1. check library availability
        boolean fileExists = isNotEmpty(driverLibrary) && fileExists(driverLibrary);
        if (!fileExists) {
            return isEmpty(driverLibrary) ?
                    txt("cfg.connection.error.DriverLibraryNotSpecified") :
                    txt("cfg.connection.error.DriverLibraryInvalid");
        }


        // 2. verify database type compatibility
        DatabaseType databaseType = getDatabaseType();
        DatabaseType libraryDatabaseType = DatabaseType.resolve(driverLibrary);
        if (isBuiltInLibrarySupported(databaseType) && libraryDatabaseType != databaseType && libraryDatabaseType != GENERIC) {
            return txt("cfg.connection.error.DriverLibraryMismatch");
        }

        return null;
    }

    private boolean isExternalDriver() {
        return getDriverSource() == DriverSource.EXTERNAL;
    }

    private void updateDriversSelector(@Nullable DriverBundle drivers) {
        if (drivers == null) {
            initComboBox(driverComboBox);
            setSelection(driverComboBox, null);
            return;
        }

        DriverOption selectedOption = getSelection(driverComboBox);
        initComboBox(driverComboBox);

        Set<Class<Driver>> driverClasses = drivers.getDriverClasses();
        List<DriverOption> driverOptions = Lists.convert(driverClasses, d -> new DriverOption(d));
        initComboBox(driverComboBox, driverOptions);

        if (selectedOption == null) {
            selectedOption = firstElement(driverOptions);
        }
        setSelection(driverComboBox, selectedOption);
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
        return isNotEmpty(driverLibrary) && new File(driverLibrary).exists();
    }

    public String getDriverLibrary() {
        return getText(driverLibraryTextField);
    }

    @Nullable
    public File getDriverLibraryFile() {
        String driverLibrary = getDriverLibrary();
        if (Strings.isEmpty(driverLibrary)) return null;

        return new File(driverLibrary);
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
        setTextSilently(driverLibraryTextField, configuration.getDriverLibrary());
        updateDriverFields();

        List<DriverOption> driverOptions = getElements(driverComboBox);
        setSelection(driverComboBox, DriverOption.get(driverOptions, configuration.getDriver()));
    }

    private void showDownloadPopup(
            JButton button,
            List<DriverPackage> driverPackages,
            @Nullable CloudConfigProviderFamily providerFamily) {
        List<AnAction> actions = new ArrayList<>();
        for (DriverPackage driverPackage : driverPackages) {
                String title = Actions.adjustActionName(driverPackage.getName());
                actions.add(new DumbAwareAction(title, null, null) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        String downloadPath = getDownloadPath(driverPackage);
                        setText(driverLibraryTextField, downloadPath);
                    }
                });
        }
        actions.add(Separator.getInstance());
        actions.add(new DumbAwareAction(txt("cfg.connection.action.DownloadLibraries"), null, AllIcons.Actions.Download) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
                if (downloadManager.isDriverPackageMetadataOutdated(getDatabaseType(), providerFamily)) {
                    Project project = getProject();
                    Progress.modal(project, null, true,
                            txt("prc.connection.title.LoadingDrivers"),
                            txt("prc.connection.text.LoadingDriverPackageMetadata"),
                            indicator -> initDownloadManagerDialog(providerFamily));
                } else {
                    Background.run(() -> initDownloadManagerDialog(providerFamily));
                }
            }
        });
        popupBuilder(actions, button).
                withTitle(txt("cfg.connection.title.DriverLibraries")).
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

    private void initDownloadManagerDialog(@Nullable CloudConfigProviderFamily providerFamily) {
        Project project = ensureProject();
        DatabaseType databaseType = getDatabaseType();

        DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
        downloadManager.openDownloadDialog(project, databaseType, providerFamily, path -> {
            String currentPath = driverLibraryTextField.getText();
            if (Objects.equals(currentPath, path)) {
                // when download targets the already specified location (initially empty)
                updateDriverFields();
            } else {
                setText(driverLibraryTextField, path);
            }
        });
    }

    @Nullable
    private CloudConfigProviderFamily getCloudConfigProviderFamily() {
        CloudConfigProviderType cloudProviderType = getParentForm().getCloudConfigProviderType();
        return cloudProviderType == null ? null : cloudProviderType.getFamily();
    }

}
