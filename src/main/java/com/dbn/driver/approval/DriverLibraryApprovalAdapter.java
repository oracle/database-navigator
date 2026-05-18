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

import com.dbn.common.approval.UserApprovalAdapter;
import com.dbn.driver.DriverLibraryInfo;

public class DriverLibraryApprovalAdapter implements UserApprovalAdapter<DriverLibraryApproval> {
    @Override
    public Class<DriverLibraryApproval> getApprovalClass() {
        return DriverLibraryApproval.class;
    }

    @Override
    public String getApprovalTitle(DriverLibraryApproval approval) {
        return "Approve External JDBC Driver";
    }

    @Override
    public String getApprovalMessage(DriverLibraryApproval approval) {
        DriverLibraryInfo info = approval.getLibraryInfo();

        StringBuilder message = new StringBuilder();
        message.append("Database Navigator wants to load the following external JDBC driver library:\n");
        message.append(info.getPath()).append("\n\n");
        message.append("JDBC drivers are Java code and will run inside the IDE process. ");
        message.append("Only approve this driver if you trust its source.\n\n");
        if (info.isDirectory()) {
            message.append("This location contains ")
                    .append(info.getJarCount())
                    .append(info.getJarCount() == 1 ? " JAR file." : " JAR files.")
                    .append("\n");
        }
        message.append("Database Navigator will ask again if the path or driver contents change.");
        //message.append("SHA-256: ").append(approval.getFingerprint());
        return message.toString();
    }

    @Override
    public String getApprovalKey(DriverLibraryApproval approval) {
        DriverLibraryInfo info = approval.getLibraryInfo();
        return "jdbc-driver:" + info.getPath() + ":" + approval.getFingerprint();
    }
}
