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

package com.dbn.driver.download;

import com.dbn.common.thread.Progress;
import com.dbn.common.thread.ProgressInfo;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.driver.download.metadata.DriverPackage;
import com.dbn.driver.download.metadata.DriverPackageMetadata.RefreshMode;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.nls.NlsResources.txt;

@Slf4j
@Getter
public final class DriverPackageMetadataRefresh {
    private static final DateTimeFormatter STDOUT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final AtomicInteger ACTIVE_PACKAGE_DETAIL_RESOLUTIONS = new AtomicInteger();

    private final DatabaseType databaseType;
    private final List<CloudConfigProviderFamily> providerFamilies;
    private final boolean forceReload;
    private final List<java.util.function.Consumer<ProgressInfo>> listeners = new CopyOnWriteArrayList<>();
    private final Set<String> resolvingPackageIds = ConcurrentHashMap.newKeySet();
    private volatile ProgressInfo progress;
    private volatile boolean resolvingPackageDetails;

    DriverPackageMetadataRefresh(
            DatabaseType databaseType,
            List<CloudConfigProviderFamily> providerFamilies,
            boolean forceReload) {
        this.databaseType = databaseType;
        this.providerFamilies = providerFamilies;
        this.forceReload = forceReload;
        this.progress = new ProgressInfo(
                true,
                txt("app.driver.text.RefreshingMetadata"),
                databaseType.getName(),
                List.of(),
                Map.of(),
                false);
    }

    public void start(Project project) {
        Progress.background(
                project,
                null,
                true,
                txt("app.driver.title.LibraryDashboard"),
                txt("app.driver.text.RefreshingMetadata"),
                this::run,
                this::finishNotStarted);
    }

    private void finishNotStarted() {
        publish(
                null,
                txt("app.driver.text.RefreshingMetadata"),
                databaseType.getName(),
                List.of(),
                Map.of(),
                false,
                true);
    }

    private void run(ProgressIndicator indicator) {
        DriverDownloadManager driverDownloadManager = DriverDownloadManager.getInstance();
        List<String> metadataErrors = new ArrayList<>();
        Map<String, String> resolutionErrors = new ConcurrentHashMap<>();
        boolean canceled = false;
        try {
            indicator.setIndeterminate(true);
            for (CloudConfigProviderFamily providerFamily : providerFamilies) {
                indicator.checkCanceled();
                String detail = txt("app.driver.text.DriverTypeProvider", databaseType.getName(), providerFamily.name());
                publish(indicator, txt("app.driver.text.RefreshingMetadata"), detail, metadataErrors, resolutionErrors, true, false);
                try {
                    driverDownloadManager.refreshDriverPackageMetadata(
                            databaseType,
                            providerFamily,
                            forceReload ? RefreshMode.FORCE : RefreshMode.NORMAL);
                } catch (ProcessCanceledException e) {
                    throw e;
                } catch (Exception e) {
                    metadataErrors.add(detail + ": " + errorText(e));
                }
            }

            Map<String, DriverPackage> packages = new LinkedHashMap<>();
            for (CloudConfigProviderFamily providerFamily : providerFamilies) {
                for (DriverPackage driverPackage : driverDownloadManager.getCachedDriverPackages(databaseType, providerFamily)) {
                    if (driverPackage.getDatabaseType() != databaseType) continue;
                    packages.put(driverPackage.getId(), driverPackage);
                }
            }

            String resolvingDetails = txt("app.driver.text.ResolvingDriverPackageDetails");
            resolvingPackageDetails = true;
            publish(indicator, resolvingDetails, databaseType.getName(), metadataErrors, resolutionErrors, true, false);
            AtomicBoolean resolutionCanceled = new AtomicBoolean();
            DownloadSession downloadSession = new DownloadSession(indicator).withDownloadSize(Math.max(packages.size(), 1));
            List<DriverPackage> unresolvedPackages = packages.values().stream()
                    .filter(driverPackage -> !driverPackage.isDetailsAvailable())
                    .toList();
            DriverPackageTaskExecutor.map(unresolvedPackages, driverPackage -> {
                String detail = driverPackage.getName() == null ? driverPackage.getId() : driverPackage.getName();
                resolvingPackageIds.add(driverPackage.getId());
                publish(indicator, resolvingDetails, detail, metadataErrors, resolutionErrors, true, false);
                long startedAt = System.nanoTime();
                int activeResolutions = ACTIVE_PACKAGE_DETAIL_RESOLUTIONS.incrementAndGet();
                String threadName = Thread.currentThread().getName();
                logInfoAndPrint("[DBN] Driver package detail resolution started: databaseType=" + databaseType +
                        ", package=" + detail + ", active=" + activeResolutions + ", thread=" + threadName);
                String outcome = "completed";
                try {
                    indicator.checkCanceled();
                    driverDownloadManager.resolveDriverPackageDetails(driverPackage, downloadSession);
                } catch (ProcessCanceledException e) {
                    resolutionCanceled.set(true);
                    outcome = "canceled";
                } catch (Exception e) {
                    resolutionErrors.put(driverPackage.getId(), errorText(e));
                    outcome = "failed";
                } finally {
                    resolvingPackageIds.remove(driverPackage.getId());
                    long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
                    int activeAfterResolution = ACTIVE_PACKAGE_DETAIL_RESOLUTIONS.decrementAndGet();
                    logInfoAndPrint("[DBN] Driver package detail resolution " + outcome +
                            ": databaseType=" + databaseType +
                            ", package=" + detail +
                            ", detailsAvailable=" + driverPackage.isDetailsAvailable() +
                            ", elapsed=" + elapsedMillis + "ms" +
                            ", active=" + activeAfterResolution +
                            ", thread=" + threadName);
                }
                return detail;
            }, detail -> {
                if (!indicator.isCanceled()) {
                    publish(indicator, resolvingDetails, detail, metadataErrors, resolutionErrors, true, false);
                }
            });
            if (resolutionCanceled.get()) throw new ProcessCanceledException();
            indicator.checkCanceled();
        } catch (ProcessCanceledException e) {
            canceled = true;
        } catch (Exception e) {
            metadataErrors.add(errorText(e));
        } finally {
            publish(
                    indicator,
                    txt("app.driver.text.RefreshingMetadata"),
                    databaseType.getName(),
                    metadataErrors,
                    resolutionErrors,
                    false,
                    canceled);
        }
    }

    private void publish(
            @org.jetbrains.annotations.Nullable ProgressIndicator indicator,
            String text,
            String detail,
            List<String> metadataErrors,
            Map<String, String> resolutionErrors,
            boolean running,
            boolean canceled) {
        if (indicator != null) {
            indicator.setText(text);
            indicator.setText2(detail);
        }
        ProgressInfo snapshot = new ProgressInfo(
                running,
                text,
                detail,
                List.copyOf(metadataErrors),
                Map.copyOf(resolutionErrors),
                canceled);
        this.progress = snapshot;
        for (java.util.function.Consumer<ProgressInfo> listener : listeners) {
            try {
                listener.accept(snapshot);
            } catch (RuntimeException e) {
                log.warn("Driver package refresh listener failed.", e);
            }
        }
    }

    public boolean isRunning() {
        return progress.running();
    }

    public boolean isPackageResolving(String packageId) {
        return resolvingPackageIds.contains(packageId);
    }

    public void addListener(java.util.function.Consumer<ProgressInfo> listener) {
        listeners.add(listener);
        listener.accept(progress);
    }

    public void removeListener(java.util.function.Consumer<ProgressInfo> listener) {
        listeners.remove(listener);
    }

    private static String errorText(Exception exception) {
        return exception.getLocalizedMessage() == null ? exception.toString() : exception.getLocalizedMessage();
    }

    // TEMP: mirror package-resolution diagnostics to stdout while comparing the IDE log and run consoles.
    private static void logInfoAndPrint(String message) {
        log.info(message);
        System.out.println(LocalDateTime.now().format(STDOUT_TIMESTAMP_FORMAT) + " " + message);
    }

}
