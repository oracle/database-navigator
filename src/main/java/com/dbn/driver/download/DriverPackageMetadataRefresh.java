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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.dbn.nls.NlsResources.txt;

@Slf4j
public final class DriverPackageMetadataRefresh {
    private final DatabaseType databaseType;
    private final List<CloudConfigProviderFamily> providerFamilies;
    private final boolean forceReload;
    private final List<java.util.function.Consumer<ProgressInfo>> listeners = new CopyOnWriteArrayList<>();
    private volatile ProgressInfo progress;

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
        Map<String, String> resolutionErrors = new HashMap<>();
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
                    packages.put(driverPackage.getId(), driverPackage);
                }
            }

            for (DriverPackage driverPackage : packages.values()) {
                indicator.checkCanceled();
                if (driverPackage.isDetailsAvailable()) continue;
                String detail = driverPackage.getName() == null ? driverPackage.getId() : driverPackage.getName();
                publish(
                        indicator,
                        txt("app.driver.text.ResolvingDriverPackageDetails"),
                        detail,
                        metadataErrors,
                        resolutionErrors,
                        true,
                        false);
                try {
                    driverDownloadManager.resolveDriverPackageDetails(driverPackage, new DownloadSession(indicator).withDownloadSize(1));
                } catch (ProcessCanceledException e) {
                    throw e;
                } catch (Exception e) {
                    resolutionErrors.put(driverPackage.getId(), errorText(e));
                }
            }
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

    public ProgressInfo getProgress() {
        return progress;
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

}
