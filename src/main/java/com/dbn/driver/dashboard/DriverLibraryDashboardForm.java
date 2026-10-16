/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.driver.dashboard;

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.text.TextContent;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.ProgressInfo;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.panel.DBNBanner;
import com.dbn.common.ui.panel.DBNLoadingPanel;
import com.dbn.common.ui.select.DBNComboBoxRenderer;
import com.dbn.common.ui.table.DBNTableRowAction;
import com.dbn.common.ui.table.TableActions;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.driver.dashboard.DriverLibraryDashboardTableModel.DashboardDriverPackage;
import com.dbn.driver.download.DriverDownloadManager;
import com.dbn.driver.download.DriverPackageDownloader;
import com.dbn.driver.download.DriverPackageMetadataRefresh;
import com.dbn.driver.download.metadata.DriverPackage;
import com.dbn.driver.download.ui.DriverPackageInfoDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsContexts.Tooltip;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.dbn.common.message.MessageType.INFO;
import static com.dbn.common.message.MessageType.WARNING;
import static com.dbn.common.ui.panel.DBNLoadingPanel.newLoadingPanel;
import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;
import static com.dbn.common.ui.util.Mouse.onMouseDoubleClick;
import static com.dbn.common.util.Messages.OPTIONS_YES_NO;
import static com.dbn.common.util.Messages.showConfirmationDialog;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.driver.dashboard.DriverLibraryDashboardStatus.RESOLVING;
import static com.dbn.driver.dashboard.DriverLibraryDashboardStatus.resolve;
import static com.dbn.driver.dashboard.DriverLibraryDashboardStatus.typesToRefresh;
import static com.dbn.nls.NlsResources.txt;

/** Driver package inventory and management UI. */
public class DriverLibraryDashboardForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JPanel progressContainer;
    private JPanel messageContainer;
    private JPanel tableContainer;
    private JComboBox<DatabaseTypeOption> databaseTypeSelector;
    private DBNHintForm dashboardHintForm;
    private DBNBanner messageBanner;
    private DBNLoadingPanel refreshProgressPanel;
    private DriverLibraryDashboardTable table;
    private final Predicate<Point> tableActionColumnAt;
    private final Map<String, String> downloadFailures = new HashMap<>();
    private final Map<DriverPackageMetadataRefresh, Consumer<ProgressInfo>> refreshListeners = new HashMap<>();
    private final AtomicLong inventoryGeneration = new AtomicLong();
    private final Set<DatabaseType> displayedRefreshTypes = new HashSet<>();
    private List<DriverPackage> currentPackages = List.of();
    private List<DriverPackageMetadataRefresh> activeRefreshes = List.of();
    private boolean loadingInventory;
    private boolean deletingPackage;

    private final DriverLibraryDashboardOptions options;
    private final DBNTableRowAction<DashboardDriverPackage> tableDownloadAction = createDownloadAction();
    private final DBNTableRowAction<DashboardDriverPackage> tableDeleteAction = createDeleteAction();
    private final DriverLibraryDashboardTableModel tableModel = new DriverLibraryDashboardTableModel(p -> getActionTooltip(p));

    public DriverLibraryDashboardForm(DBNDialog<?> parent, DriverLibraryDashboardOptions options) {
        super(parent);
        this.options = options;
        this.table = new DriverLibraryDashboardTable(this, tableModel);
        this.tableActionColumnAt = TableActions.installTableAction(
                table,
                tableModel.getColumnCount() - 1,
                DashboardDriverPackage.class,
                p -> resolveTableAction(p));

        initHintPanel();
        initDatabaseTypeSelector();
        initLayoutComponents();
        installListeners();
        updateActions();
        whenFirstShown(() -> reloadInventory(false));
    }

    private void initDatabaseTypeSelector() {
        if (!options.isDatabaseTypeFixed()) databaseTypeSelector.addItem(new DatabaseTypeOption(null));
        for (DatabaseType type : DatabaseType.NATIVELY_SUPPORTED) {
            databaseTypeSelector.addItem(new DatabaseTypeOption(type));
        }
        DatabaseType contextualType = options.databaseType();
        if (options.isDatabaseTypeFixed() && contextualType != null &&
                Arrays.stream(DatabaseType.NATIVELY_SUPPORTED).noneMatch(type -> type == contextualType)) {
            databaseTypeSelector.addItem(new DatabaseTypeOption(contextualType));
        }
        databaseTypeSelector.setRenderer(new DBNComboBoxRenderer<>(databaseTypeSelector));
        setSelectedDatabaseType(options.databaseType());
        databaseTypeSelector.setEnabled(!options.isDatabaseTypeFixed());
    }

    private void initLayoutComponents() {
        progressContainer.setLayout(new BorderLayout(JBUI.scale(6), 0));
        refreshProgressPanel = newLoadingPanel(this, txt("app.driver.text.RefreshingMetadata"));
        refreshProgressPanel.installOn(progressContainer, false);
        progressContainer.setVisible(false);
        messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));
        messageBanner = new DBNBanner(INFO);
        messageContainer.add(messageBanner);
        clearMessage();
        tableContainer.setLayout(new BorderLayout());
        tableContainer.add(initTableComponent());
        mainPanel.setPreferredSize(JBUI.size(1050, 560));
    }

    private void initHintPanel() {
        dashboardHintForm = new DBNHintForm(this, createDashboardHint(List.of()), null);
        hintPanel.add(dashboardHintForm.getComponent());
    }

    private TextContent createDashboardHint(List<DashboardDriverPackage> driverPackages) {
        String hintKey = options.canSelectLibrary() ?
                "app.driver.hint.LibraryDashboardSelect" :
                "app.driver.hint.LibraryDashboardOverview";
        String hintText = txt(hintKey);
        CloudConfigProviderFamily providerFamily = options.providerFamily();
        boolean hasUnsupportedPackages = options.canSelectLibrary() && providerFamily != null &&
                driverPackages.stream().anyMatch(driverPackage -> !driverPackage.supported());
        if (hasUnsupportedPackages) {
            String unsupportedPackagesHint = txt("app.driver.text.UnsupportedPackages");
            hintText += "<br><br>" + unsupportedPackagesHint.replace("\n", "<br>");
        }
        return TextContent.htmlMessage(hintText);
    }

    private JPanel initTableComponent() {
        ToolbarDecorator decorator = createToolbarDecorator(table);
        decorator.setAddAction(null);
        decorator.setRemoveAction(null);
        decorator.setEditAction(null);
        decorator.setMoveUpAction(null);
        decorator.setMoveDownAction(null);
        decorator.addExtraAction(reloadAction);
        decorator.addExtraAction(Separator.getInstance());
        decorator.addExtraAction(infoAction);
        decorator.addExtraAction(downloadAction);
        decorator.addExtraAction(deleteAction);
        return createToolbarDecoratorComponent(decorator, table);
    }

    private void installListeners() {
        databaseTypeSelector.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) reloadInventory(false);
        });
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActions();
            }
        });
        onMouseDoubleClick(table, event -> {
            if (tableActionColumnAt.test(event.getPoint())) return;
            if (options.canSelectLibrary()) {
                if (canSelectSelectedPackage()) selectSelectedPackage();
            } else {
                showSelectedPackageInfo();
            }
        });
    }

    private void reloadInventory(boolean forceReload) {
        if (getProject() == null || getProject().isDisposed()) return;
        detachRefreshListeners();
        long generation = inventoryGeneration.incrementAndGet();
        loadingInventory = true;
        displayedRefreshTypes.clear();
        clearMessage();
        DatabaseType selectedType = getSelectedDatabaseType();
        List<DatabaseType> types = typesToRefresh(selectedType);
        List<CloudConfigProviderFamily> providerFamilies = getProviderFamilies();
        DriverDownloadManager manager = DriverDownloadManager.getInstance();
        List<DriverPackage> cachedPackages = readCachedPackages(manager, selectedType, types, providerFamilies);
        currentPackages = cachedPackages;
        applyRows(createRows(manager, cachedPackages, Map.copyOf(downloadFailures), Map.of()));
        table.setLoading(true);
        refreshProgressPanel.withLoadingText(txt("app.driver.text.RefreshingMetadata"));
        progressContainer.setVisible(true);
        updateActions();

        activeRefreshes = types.stream()
                .map(type -> manager.refreshDriverPackageInventory(getProject(), type, forceReload))
                .toList();

        for (DriverPackageMetadataRefresh refresh : activeRefreshes) {
            Consumer<ProgressInfo> listener = progress -> Dispatch.run(mainPanel, () ->
                    updateRefreshProgress(generation, selectedType, types, providerFamilies));
            refreshListeners.put(refresh, listener);
            refresh.addListener(listener);
        }
    }

    private void updateRefreshProgress(
            long generation,
            @Nullable DatabaseType selectedType,
            List<DatabaseType> types,
            List<CloudConfigProviderFamily> providerFamilies) {
        if (inventoryGeneration.get() != generation || !loadingInventory) return;

        List<ProgressInfo> progresses = activeRefreshes.stream()
                .map(DriverPackageMetadataRefresh::getProgress)
                .toList();
        DriverDownloadManager manager = DriverDownloadManager.getInstance();
        Map<String, String> resolutionErrors = new HashMap<>();
        progresses.forEach(progress -> resolutionErrors.putAll(progress.groupedErrors()));
        for (int index = 0; index < progresses.size(); index++) {
            ProgressInfo progress = progresses.get(index);
            DatabaseType databaseType = types.get(index);
            boolean refreshComplete = !progress.running() && displayedRefreshTypes.add(databaseType);
            if (progress.running() || refreshComplete) {
                updateRowsForType(manager, selectedType, databaseType, providerFamilies, resolutionErrors);
            }
        }

        long completed = progresses.stream().filter(progress -> !progress.running()).count();
        int runningIndex = -1;
        for (int index = 0; index < progresses.size(); index++) {
            if (progresses.get(index).running()) {
                runningIndex = index;
                break;
            }
        }
        if (runningIndex >= 0) {
            ProgressInfo running = progresses.get(runningIndex);
            DriverPackageMetadataRefresh runningRefresh = activeRefreshes.get(runningIndex);
            String details = runningRefresh.isResolvingPackageDetails() ?
                    running.detail() :
                    txt("app.driver.text.RefreshProgressDetails", running.detail(), completed, progresses.size());
            String progressText = txt(
                    "app.driver.text.RefreshProgressLabel",
                    running.text(),
                    details);
            refreshProgressPanel.withLoadingText(progressText);
            updateActions();
            return;
        }

        detachRefreshListeners();
        List<String> metadataErrors = progresses.stream()
                .flatMap(progress -> progress.errors().stream())
                .toList();
        boolean canceled = progresses.stream().anyMatch(ProgressInfo::canceled);
        loadingInventory = false;
        table.setLoading(false);
        progressContainer.setVisible(false);
        if (!metadataErrors.isEmpty()) {
            showMessage(WARNING,
                    txt("app.driver.text.MetadataRefreshFailed", metadataErrors.size()),
                    String.join("\n", metadataErrors));
        } else if (canceled) {
            showMessage(WARNING, txt("app.driver.text.RefreshCanceled"), null);
        } else if (currentPackages.isEmpty()) {
            showMessage(INFO, txt("app.driver.text.NoPackagesFound"), null);
        } else {
            clearMessage();
        }
        updateActions();
    }

    private void updateRowsForType(
            DriverDownloadManager manager,
            @Nullable DatabaseType selectedType,
            DatabaseType databaseType,
            List<CloudConfigProviderFamily> providerFamilies,
            Map<String, String> resolutionErrors) {
        List<DriverPackage> refreshedPackages = readCachedPackages(
                        manager, selectedType, List.of(databaseType), providerFamilies)
                .stream()
                .filter(driverPackage -> driverPackage.getDatabaseType() == databaseType)
                .toList();
        Map<String, DriverPackage> mergedPackages = new LinkedHashMap<>();
        currentPackages.stream()
                .filter(driverPackage -> driverPackage.getDatabaseType() != databaseType)
                .forEach(driverPackage -> mergedPackages.put(driverPackage.getId(), driverPackage));
        refreshedPackages.forEach(driverPackage -> mergedPackages.put(driverPackage.getId(), driverPackage));
        currentPackages = sortPackages(mergedPackages);

        List<DashboardDriverPackage> driverPackages = new ArrayList<>();
        for (int index = 0; index < tableModel.getRowCount(); index++) {
            DashboardDriverPackage driverPackage = tableModel.getRow(index);
            if (driverPackage != null && driverPackage.getDatabaseType() != databaseType) driverPackages.add(driverPackage);
        }
        driverPackages.addAll(createRows(manager, refreshedPackages, Map.copyOf(downloadFailures), resolutionErrors));
        driverPackages.sort(Comparator.comparing((DashboardDriverPackage driverPackage) -> {
                    DatabaseType type = driverPackage.getDatabaseType();
                    return type == null ? "" : type.getName();
                })
                .thenComparing(driverPackage -> driverPackage.getName() == null ? driverPackage.getId() : driverPackage.getName()));
        applyRows(driverPackages);
    }

    private static List<DriverPackage> sortPackages(Map<String, DriverPackage> packages) {
        return packages.values().stream()
                .sorted(Comparator.comparing((DriverPackage p) -> p.getDatabaseType() == null ? "" : p.getDatabaseType().getName())
                        .thenComparing(p -> p.getName() == null ? p.getId() : p.getName()))
                .toList();
    }

    private void showMessage(MessageType messageType, String message, @Nullable String details) {
        messageBanner
                .setMessageType(messageType)
                .setMessage(message, details);
        messageBanner.setVisible(true);
        updateFeedbackVisibility();
    }

    private void clearMessage() {
        messageBanner.setMessage(null);
        messageBanner.setVisible(false);
        updateFeedbackVisibility();
    }

    private void updateFeedbackVisibility() {
        messageContainer.setVisible(messageBanner.isVisible());
    }

    private void detachRefreshListeners() {
        refreshListeners.forEach(DriverPackageMetadataRefresh::removeListener);
        refreshListeners.clear();
    }

    private List<CloudConfigProviderFamily> getProviderFamilies() {
        return List.of(CloudConfigProviderFamily.values());
    }

    private List<DriverPackage> readCachedPackages(
            DriverDownloadManager manager,
            @Nullable DatabaseType selectedType,
            List<DatabaseType> types,
            List<CloudConfigProviderFamily> providerFamilies) {
        Map<String, DriverPackage> packages = new LinkedHashMap<>();
        for (DatabaseType type : types) {
            for (CloudConfigProviderFamily family : providerFamilies) {
                for (DriverPackage driverPackage : manager.getCachedDriverPackages(type, family)) {
                    if (selectedType != null && !driverPackage.matches(selectedType)) continue;
                    packages.put(driverPackage.getId(), driverPackage);
                }
            }
        }
        return packages.values().stream()
                .sorted(Comparator.comparing((DriverPackage p) -> p.getDatabaseType() == null ? "" : p.getDatabaseType().getName())
                        .thenComparing(p -> p.getName() == null ? p.getId() : p.getName()))
                .toList();
    }

    private List<DashboardDriverPackage> createRows(
            DriverDownloadManager manager,
            List<DriverPackage> packages,
            Map<String, String> downloadErrors,
            Map<String, String> resolutionErrors) {
        List<DashboardDriverPackage> driverPackages = new ArrayList<>();
        for (DriverPackage driverPackage : packages) {
            String packageId = driverPackage.getId();
            String failure = downloadErrors.get(packageId);
            if (failure == null) failure = resolutionErrors.get(packageId);
            boolean downloaded = manager.isPackageDownloaded(driverPackage);
            boolean downloading = manager.isPackageDownloading(packageId);
            boolean resolving = !driverPackage.isDetailsAvailable() ||
                    driverPackage.isDetailsResolving() ||
                    isPackageResolving(driverPackage);
            DriverLibraryDashboardStatus status = resolve(
                    failure != null,
                    resolving,
                    downloading,
                    downloaded,
                    driverPackage.isObsolete());
            String location = manager.getDownloadPath(packageId);
            if (location == null) location = DriverDownloadManager.getDriverPackageLocation(packageId);
            driverPackages.add(new DashboardDriverPackage(
                    driverPackage, status, isSupportedPackage(driverPackage), downloaded, location, failure));
        }
        return driverPackages;
    }

    private boolean isSupportedPackage(DriverPackage driverPackage) {
        CloudConfigProviderFamily providerFamily = options.providerFamily();
        return providerFamily == null || driverPackage.matches(providerFamily);
    }

    private boolean isPackageResolving(DriverPackage driverPackage) {
        return activeRefreshes.stream().anyMatch(refresh ->
                refresh.getDatabaseType() == driverPackage.getDatabaseType() &&
                        refresh.isPackageResolving(driverPackage.getId()));
    }

    private @NotNull DBNTableRowAction<DashboardDriverPackage> createDownloadAction() {
        return new DBNTableRowAction<>(
                Icons.ACTION_DOWNLOAD,
                txt("app.driver.action.Download"),
                txt("app.driver.action.Download"),
                this::canDownloadPackage,
                this::downloadPackage);
    }

    private @Nullable @Tooltip String getActionTooltip(DashboardDriverPackage p) {
        return resolveTableAction(p).getTooltip(p);
    }

    private @NotNull DBNTableRowAction<DashboardDriverPackage> createDeleteAction() {
        return new DBNTableRowAction<>(
                Icons.ACTION_DELETE,
                txt("app.driver.action.Delete"),
                txt("app.driver.action.Delete"),
                this::canDeletePackage,
                this::deletePackage);
    }

    private DBNTableRowAction<DashboardDriverPackage> resolveTableAction(DashboardDriverPackage driverPackage) {
        return driverPackage.downloaded() ? tableDeleteAction : tableDownloadAction;
    }

    private void downloadPackage(DashboardDriverPackage row) {
        if (!canDownloadPackage(row)) return;
        DriverPackage driverPackage = row.driverPackage();
        Dialogs.show(
                () -> new DriverPackageDownloadDialog(getProject(), driverPackage),
                (dialog, exitCode) -> {
                    if (exitCode != DialogWrapper.OK_EXIT_CODE) return;

                    DriverDownloadManager manager = DriverDownloadManager.getInstance();
                    manager.setDownloadPath(driverPackage.getId(), dialog.getDownloadPath());
                    downloadFailures.remove(driverPackage.getId());

                    DriverPackageDownloader downloader = new DriverPackageDownloader();
                    boolean started = downloader.downloadDriverPackageInBackground(getProject(), driverPackage, result ->
                            Dispatch.run(mainPanel, () -> {
                                if (result != null && !result.isBlank()) {
                                    downloadFailures.put(driverPackage.getId(), result);
                                } else {
                                    downloadFailures.remove(driverPackage.getId());
                                }
                                rebuildRowsAsync();
                            }));
                    if (!started) {
                        downloadFailures.put(driverPackage.getId(), txt("app.driver.error.DownloadAlreadyRunning"));
                    }
                    rebuildRowsAsync();
                });
    }

    void selectSelectedPackage() {
        DashboardDriverPackage driverPackage = getSelectedRow();
        if (driverPackage == null || !options.canSelectLibrary() || !driverPackage.supported()) return;

        DriverDownloadManager manager = DriverDownloadManager.getInstance();
        if (!manager.isPackageDownloaded(driverPackage.driverPackage())) return;
        String path = manager.getDownloadPath(driverPackage.getId());
        if (path == null) return;

        options.selectLibrary(path);
        ensureParentDialog().close(com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE);
    }

    private void deleteSelectedPackage() {
        deletePackage(getSelectedRow());
    }

    private void deletePackage(DashboardDriverPackage row) {
        if (row == null) return;
        DriverPackage driverPackage = row.driverPackage();
        DriverDownloadManager manager = DriverDownloadManager.getInstance();
        if (manager.isPackageDownloading(driverPackage.getId())) {
            showErrorDialog(getProject(), txt("app.driver.error.PackageIsDownloading"));
            return;
        }
        if (!canDeletePackage(row)) return;
        if (manager.isPackageReferenced(getProject(), driverPackage)) {
            showErrorDialog(getProject(), txt("app.driver.error.PackageInUse"));
            return;
        }

        String packageName = driverPackage.getName() == null ? driverPackage.getId() : driverPackage.getName();
        int result = showConfirmationDialog(
                getProject(),
                txt("msg.driver.title.DeleteDriverPackage"),
                txt("msg.driver.question.DeleteDriverPackage", packageName, row.location()),
                OPTIONS_YES_NO,
                1);
        if (result != 0) return;

        deletingPackage = true;
        updateActions();
        Progress.background(getProject(), null, false,
                txt("prc.driver.title.DeleteLibrary"),
                txt("prc.driver.text.DeleteLibrary", packageName),
                indicator -> {
                    DriverDownloadManager.PackageDeletionResult deletion = manager.deleteDownloadedPackage(getProject(), driverPackage);
                    Dispatch.run(mainPanel, () -> {
                        deletingPackage = false;
                        if (!deletion.deleted()) {
                            showDeletionError(deletion);
                            if (deletion.block() == DriverDownloadManager.DeletionBlock.DELETE_FAILED) {
                                downloadFailures.put(driverPackage.getId(), String.join("\n", deletion.failures()));
                            }
                        } else {
                            downloadFailures.remove(driverPackage.getId());
                        }
                        rebuildRowsAsync();
                    });
                });
    }

    private void showDeletionError(DriverDownloadManager.PackageDeletionResult result) {
        String message = switch (result.block()) {
            case DOWNLOADING -> txt("app.driver.error.PackageIsDownloading");
            case IN_USE -> txt("app.driver.error.PackageInUse");
            case NOT_DOWNLOADED -> txt("app.driver.error.PackageNotDownloaded");
            case DELETE_FAILED -> txt("app.driver.error.DeleteFailed", String.join("\n", result.failures()));
            case NONE -> "";
        };
        if (!message.isBlank()) showErrorDialog(getProject(), txt("app.driver.title.LibraryDashboard"), message);
    }

    private void rebuildRowsAsync() {
        List<DriverPackage> packages = currentPackages;
        Map<String, String> failures = Map.copyOf(downloadFailures);
        Background.run(() -> {
            List<DashboardDriverPackage> driverPackages = createRows(
                    DriverDownloadManager.getInstance(), packages, failures, Map.of());
            Dispatch.run(mainPanel, () -> {
                applyRows(driverPackages);
                updateActions();
            });
        });
    }

    private void applyRows(List<DashboardDriverPackage> driverPackages) {
        DashboardDriverPackage selectedDriverPackage = getSelectedRow();
        String selectedPackageId = selectedDriverPackage == null ? null : selectedDriverPackage.getId();
        tableModel.setRows(driverPackages);
        dashboardHintForm.setHintContent(createDashboardHint(driverPackages));
        if (selectedPackageId != null) {
            for (int modelIndex = 0; modelIndex < tableModel.getRowCount(); modelIndex++) {
                DashboardDriverPackage candidate = tableModel.getRow(modelIndex);
                if (candidate == null || !selectedPackageId.equals(candidate.getId())) continue;
                int viewIndex = table.convertRowIndexToView(modelIndex);
                if (viewIndex >= 0) table.setRowSelectionInterval(viewIndex, viewIndex);
                break;
            }
        }
    }

    private void showSelectedPackageInfo() {
        DashboardDriverPackage driverPackage = getSelectedRow();
        if (driverPackage == null || !driverPackage.isDetailsAvailable()) return;
        Dialogs.show(() -> new DriverPackageInfoDialog(
                getProject(), txt("app.driver.title.PackageInfo"), true, driverPackage.driverPackage()));
    }

    private DashboardDriverPackage getSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) return null;
        return tableModel.getRow(table.convertRowIndexToModel(selectedRow));
    }

    private void updateActions() {
        databaseTypeSelector.setEnabled(!options.isDatabaseTypeFixed() && !loadingInventory);
        if (options.canSelectLibrary()) {
            ensureParentDialog().setOKActionEnabled(canSelectSelectedPackage());
        }
    }

    private boolean canDownloadPackage(DashboardDriverPackage driverPackage) {
        if (driverPackage == null) return false;
        if (!driverPackage.isDetailsAvailable()) return false;
        if (driverPackage.status() == RESOLVING) return false;
        if (deletingPackage) return false;
        DriverDownloadManager manager = DriverDownloadManager.getInstance();
        return !isDownloaded(driverPackage) && !manager.isPackageDownloading(driverPackage.getId());
    }

    private boolean canDeleteSelectedPackage() {
        return canDeletePackage(getSelectedRow());
    }

    private boolean canDeletePackage(DashboardDriverPackage driverPackage) {
        if (driverPackage == null) return false;
        if (deletingPackage) return false;

        boolean downloaded = isDownloaded(driverPackage);
        if (loadingInventory && !downloaded) return false;

        DriverDownloadManager manager = DriverDownloadManager.getInstance();
        if (manager.isPackageDownloading(driverPackage.getId())) return false;
        if (downloaded) return true;
        return manager.isPackageTrackedForDeletion(driverPackage.driverPackage());
    }

    private boolean canSelectSelectedPackage() {
        DashboardDriverPackage driverPackage = getSelectedRow();
        if (driverPackage == null) return false;
        if (!options.canSelectLibrary()) return false;
        if (!driverPackage.supported()) return false;
        return isDownloaded(driverPackage);
    }

    private static boolean isDownloaded(DashboardDriverPackage driverPackage) {
        return driverPackage.downloaded();
    }

    private final BasicAction reloadAction = new BasicAction(txt("app.driver.action.ReloadMetadata"), null, Icons.ACTION_REFRESH) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            reloadInventory(true);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(!loadingInventory);
        }
    };

    private final BasicAction infoAction = new BasicAction(txt("app.driver.action.PackageInfo"), null, Icons.ACTION_INFO) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            showSelectedPackageInfo();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            DashboardDriverPackage driverPackage = getSelectedRow();
            e.getPresentation().setEnabled(driverPackage != null && driverPackage.isDetailsAvailable());
        }
    };

    private final BasicAction downloadAction = new BasicAction(txt("app.driver.action.Download"), null, Icons.ACTION_DOWNLOAD) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            downloadPackage(getSelectedRow());
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(canDownloadPackage(getSelectedRow()));
        }
    };

    private final BasicAction deleteAction = new BasicAction(txt("app.driver.action.Delete"), null, Icons.ACTION_DELETE) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            deleteSelectedPackage();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(canDeleteSelectedPackage());
        }
    };

    private void setSelectedDatabaseType(@Nullable DatabaseType type) {
        for (int i = 0; i < databaseTypeSelector.getItemCount(); i++) {
            if (databaseTypeSelector.getItemAt(i).databaseType() == type) {
                databaseTypeSelector.setSelectedIndex(i);
                return;
            }
        }
    }

    @Nullable
    private DatabaseType getSelectedDatabaseType() {
        DatabaseTypeOption option = (DatabaseTypeOption) databaseTypeSelector.getSelectedItem();
        return option == null ? null : option.databaseType();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        detachRefreshListeners();
        super.disposeInner();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return databaseTypeSelector;
    }

    private record DatabaseTypeOption(@Nullable DatabaseType databaseType) implements Presentable {
        @NotNull
        @Override
        public String getName() {
            return databaseType == null ? txt("app.driver.text.AllDatabaseTypes") : databaseType.getName();
        }

        @Override
        public Icon getIcon() {
            return databaseType == null ? null : databaseType.getIcon();
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
