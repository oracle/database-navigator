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

package com.dbn.driver.approval;

import com.dbn.common.approval.UserApprovalAction;
import com.dbn.common.approval.UserApprovalAdapter;
import com.dbn.common.approval.UserApprovalOption;
import com.dbn.driver.DriverLibraryInfo;

import static com.dbn.common.approval.UserApprovalAction.DRIVER_LIBRARY_LOAD;
import static com.dbn.nls.NlsResources.txt;

/**
 * Prepares user approval information for loading external JDBC driver libraries,
 * which may execute driver code from user-selected files or directories.
 */
public class DriverLibraryApprovalAdapter implements UserApprovalAdapter<DriverLibraryApproval> {
    private static final UserApprovalOption[] APPROVAL_OPTIONS = {
            UserApprovalOption.one(txt("msg.driver.button.TrustAndLoadDriver")),
            UserApprovalOption.none(txt("msg.shared.button.Cancel"))};

    @Override
    public Class<DriverLibraryApproval> getApprovalClass() {
        return DriverLibraryApproval.class;
    }

    @Override
    public UserApprovalAction getApprovalAction() {
        return DRIVER_LIBRARY_LOAD;
    }

    @Override
    public String getApprovalTitle(DriverLibraryApproval approval) {
        return txt("msg.driver.title.TrustExternalJdbcDriverLibrary");
    }

    @Override
    public String getApprovalMessage(DriverLibraryApproval approval) {
        DriverLibraryInfo info = approval.getLibraryInfo();

        String directoryInfo = "";
        if (info.isDirectory()) {
            String jarLabel = info.getJarCount() == 1 ?
                    txt("msg.driver.text.JarFile") :
                    txt("msg.driver.text.JarFiles");
            directoryInfo = txt("msg.driver.message.TrustExternalJdbcDriverLibraryDirectory", info.getJarCount(), jarLabel) + "\n";
        }
        return txt("msg.driver.message.TrustExternalJdbcDriverLibrary", info.getPath(), directoryInfo, approval.getFingerprint());
    }

    @Override
    public String getApprovalKey(DriverLibraryApproval approval) {
        DriverLibraryInfo info = approval.getLibraryInfo();
        return "jdbc-driver:" + info.getPath() + ":" + approval.getFingerprint();
    }

    @Override
    public UserApprovalOption[] getApprovalOptions(DriverLibraryApproval approvable) {
        return APPROVAL_OPTIONS;
    }
}
