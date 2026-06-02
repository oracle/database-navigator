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

import com.dbn.common.approval.UserApprovalManager;
import com.dbn.common.util.Files;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.io.File;

@UtilityClass
public class DriverLibraryApprovalUtil {

    @Nullable
    @SneakyThrows
    public static DriverLibraryApproval createApproval(@Nullable File libraryFile) {
        if (libraryFile == null) return null;
        if (!libraryFile.exists()) return null;
        if (isManagedDriverLibrary(libraryFile)) return null;

        return new DriverLibraryApproval(libraryFile);
    }

    @SneakyThrows
    public static void approveTemporarily(File libraryFile) {
        DriverLibraryApproval approval = createApproval(libraryFile);
        if (approval == null) return;

        UserApprovalManager approvalManager = UserApprovalManager.getInstance();
        approvalManager.approveTemporarily(approval);

    }

    @SneakyThrows
    public static void ensureApproved(File libraryFile) {
        DriverLibraryApproval approval = createApproval(libraryFile);
        if (approval == null) return;

        UserApprovalManager approvalManager = UserApprovalManager.getInstance();
        approvalManager.ensureApproved(approval);
    }

    private boolean isManagedDriverLibrary(File libraryFile) throws Exception {
        File library = libraryFile.getCanonicalFile();
        File deploymentRoot = Files.getPluginDeploymentRoot().getCanonicalFile();
        if (!isSameOrChild(deploymentRoot, library)) return false;

        if (isBundledDriverLibrary(deploymentRoot, library)) return true;

        File driverPackages = new File(deploymentRoot, "driver-packages").getCanonicalFile();
        return isSameOrChild(driverPackages, library);
    }

    private boolean isBundledDriverLibrary(File deploymentRoot, File library) throws Exception {
        File current = library;
        while (current != null && isSameOrChild(deploymentRoot, current)) {
            if (current.getName().startsWith("bundled-jdbc-")) return true;
            current = current.getParentFile();
        }
        return false;
    }

    private boolean isSameOrChild(File ancestor, File file) throws Exception {
        String ancestorPath = ancestor.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        return filePath.equals(ancestorPath) || filePath.startsWith(ancestorPath + File.separator);
    }
}
