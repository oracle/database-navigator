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

package com.dbn.execution.script.options.ui;

import com.dbn.common.checksum.file.FileFingerprint;
import com.dbn.common.checksum.file.FileFingerprintCache;
import com.dbn.common.checksum.file.FileFingerprintCache.Snapshot;
import com.dbn.common.checksum.file.FileFingerprintListener;
import com.dbn.common.checksum.file.FileFingerprintStatus;
import com.dbn.common.event.ApplicationEvents;
import com.dbn.common.ui.table.DBNEditableTableModel;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.connection.DatabaseType;
import com.dbn.execution.script.CmdLineExecutableSignature;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.execution.script.CmdLineInterfaceBundle;
import com.intellij.openapi.options.ConfigurationException;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.approval.UserApprovalAction.COMMAND_LINE_EXECUTION;
import static com.dbn.common.approval.UserApprovalAdapters.getApprovalKey;
import static com.dbn.common.checksum.file.FileFingerprintStatus.*;
import static com.dbn.execution.script.CmdLineExecutableSignature.createApprovalSignature;
import static com.dbn.execution.script.CmdLineExecutableSignature.getFingerprintCacheIdentity;
import static com.dbn.nls.NlsResources.txt;

public class CmdLineInterfacesTableModel extends DBNEditableTableModel implements FileFingerprintListener {
    @Getter
    private CmdLineInterfaceBundle bundle;

    public CmdLineInterfacesTableModel(CmdLineInterfaceBundle bundle) {
        this.bundle = bundle.clone();
        ApplicationEvents.subscribe(this, FileFingerprintListener.TOPIC, this);
        refreshFingerprints();
    }

    public void setBundle(CmdLineInterfaceBundle bundle) {
        this.bundle = bundle.clone();
        refreshFingerprints();
        notifyListeners(0, bundle.size(), -1);
    }

    @Override
    public int getRowCount() {
        return bundle.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnIndex == 0 ? txt("cfg.execution.column.DatabaseType") :
               columnIndex == 1 ? txt("app.shared.column.Name") :
               columnIndex == 2 ? txt("cfg.execution.column.ExecutablePath") :
               columnIndex == 3 ? txt("cfg.execution.column.ExecutableFingerprint") : null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 3 ? FileFingerprintStatus.class : String.class;

    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        CmdLineInterface environmentType = getInterface(rowIndex);
        return
           columnIndex == 0 ? environmentType.getDatabaseType() :
           columnIndex == 1 ? environmentType.getName() :
           columnIndex == 2 ? environmentType.getExecutablePath() :
           columnIndex == 3 ? getFingerprintStatus(environmentType) : null;
    }

    @Override
    public void setValueAt(Object o, int rowIndex, int columnIndex) {
        Object actualValue = getValueAt(rowIndex, columnIndex);
        if (!Commons.match(actualValue, o)) {
            CmdLineInterface cmdLineInterface = bundle.get(rowIndex);
            if (columnIndex == 0) {
                DatabaseType databaseType = (DatabaseType) o;
                cmdLineInterface.setDatabaseType(databaseType);
            } else if (columnIndex == 1) {
                cmdLineInterface.setName((String) o);
            } else if (columnIndex == 2) {
                cmdLineInterface.setExecutablePath((String) o);
            }

            notifyListeners(rowIndex, rowIndex, columnIndex);
            if (columnIndex == 0 || columnIndex == 2) {
                updateFingerprint(cmdLineInterface, columnIndex == 2);
            }
        }
    }

    public Map<String, String> getApprovalSignatures() {
        Map<String, String> signatures = new HashMap<>();
        for (CmdLineInterface cmdLineInterface : bundle.getInterfaces()) {
            String approvalKey = getApprovalKey(COMMAND_LINE_EXECUTION, cmdLineInterface);
            Snapshot fingerprint = getFingerprint(cmdLineInterface);
            if (fingerprint == null || fingerprint.status() != READY) continue;

            FileFingerprint fileFingerprint = fingerprint.fingerprint();
            if (fileFingerprint == null) continue;

            signatures.put(approvalKey, createApprovalSignature(cmdLineInterface, fileFingerprint));
        }
        return signatures;
    }

    public Set<String> getInterfaceNames() {
        return bundle.getInterfaceNames();
    }


    private CmdLineInterface getInterface(int rowIndex) {
        while (bundle.size() <= rowIndex) {
            bundle.add(new CmdLineInterface());
        }
        return bundle.get(rowIndex);
    }

    public void addInterface(CmdLineInterface cmdLineInterface) {
        bundle.add(cmdLineInterface);
        int rowIndex = bundle.size() - 1;
        notifyListeners(rowIndex, rowIndex, -1);
        updateFingerprint(cmdLineInterface, true);
    }

    @Override
    public void insertRow(int rowIndex) {
        CmdLineInterface cmdLineInterface = new CmdLineInterface();
        bundle.add(rowIndex, cmdLineInterface);
        notifyListeners(rowIndex, bundle.size()-1, -1);
        updateFingerprint(cmdLineInterface, false);
    }

    @Override
    public void removeRow(int rowIndex) {
        if (bundle.size() > rowIndex) {
            bundle.remove(rowIndex);
            notifyListeners(rowIndex, bundle.size()-1, -1);
        }
    }

    private void refreshFingerprints() {
        for (CmdLineInterface cmdLineInterface : bundle.getInterfaces()) {
            updateFingerprint(cmdLineInterface, false);
        }
    }

    private FileFingerprintStatus getFingerprintStatus(CmdLineInterface cmdLineInterface) {
        if (Strings.isEmpty(cmdLineInterface.getExecutablePath())) return EMPTY;

        Snapshot fingerprint = getFingerprint(cmdLineInterface);
        return fingerprint == null ? CALCULATING : fingerprint.status();
    }

    private void updateFingerprint(CmdLineInterface cmdLineInterface, boolean refresh) {
        String executablePath = cmdLineInterface.getExecutablePath();
        if (Strings.isEmpty(executablePath)) {
            notifyFingerprintStatuses();
            return;
        }

        CmdLineInterface signatureTarget = cmdLineInterface.clone();
        String identity = getFingerprintCacheIdentity(signatureTarget);
        FileFingerprintCache.getInstance().capture(
                identity,
                () -> CmdLineExecutableSignature.getFingerprint(signatureTarget),
                refresh);
        notifyFingerprintStatuses();
    }

    private Snapshot getFingerprint(CmdLineInterface cmdLineInterface) {
        String identity = getFingerprintCacheIdentity(cmdLineInterface);
        FileFingerprintCache cache = FileFingerprintCache.getInstance();
        Snapshot fingerprint = cache.get(identity);
        if (fingerprint != null || Strings.isEmpty(cmdLineInterface.getExecutablePath())) return fingerprint;

        CmdLineInterface signatureTarget = cmdLineInterface.clone();
        return cache.capture(
                identity,
                () -> CmdLineExecutableSignature.getFingerprint(signatureTarget),
                false);
    }

    @Override
    public void fingerprintChanged(@NotNull String fileIdentity) {
        if (isDisposed()) return;

        for (int rowIndex = 0; rowIndex < bundle.size(); rowIndex++) {
            CmdLineInterface cmdLineInterface = bundle.get(rowIndex);
            if (fileIdentity.equals(getFingerprintCacheIdentity(cmdLineInterface))) {
                notifyListeners(rowIndex, rowIndex, 3);
            }
        }
    }

    private void notifyFingerprintStatuses() {
        if (isDisposed()) return;

        for (int rowIndex = 0; rowIndex < bundle.size(); rowIndex++) {
            notifyListeners(rowIndex, rowIndex, 3);
        }
    }

    public void validate() throws ConfigurationException {
        Set<String> names = new HashSet<>();
        for (CmdLineInterface cmdLineInterface : bundle.getInterfaces()) {
            String name = cmdLineInterface.getName();
            if (Strings.isEmpty(name)) {
                throw new ConfigurationException(txt("cfg.execution.error.CommandLineInterfaceNamesRequired"));
            } else if (names.contains(name)) {
                throw new ConfigurationException(txt("cfg.execution.error.CommandLineInterfaceNamesUnique"));
            } else {
                names.add(name);
            }
        }

        for (CmdLineInterface cmdLineInterface : bundle.getInterfaces()) {
            if (Strings.isEmpty(cmdLineInterface.getExecutablePath())) {
                throw new ConfigurationException(txt("cfg.execution.error.CommandLineInterfaceExecutablePathsRequired"));
            }
        }
    }
}
