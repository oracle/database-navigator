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

import com.dbn.common.approval.UserApprovable;
import com.dbn.common.approval.UserApprovalAction;
import com.dbn.common.checksum.Checksum;
import com.dbn.common.util.Measured;
import com.dbn.driver.DriverLibraryInfo;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.util.List;
import java.util.Set;

import static com.dbn.common.approval.UserApprovalAction.DRIVER_LIBRARY_LOAD;
import static com.dbn.common.checksum.ChecksumType.SHA_256;

@Getter
public class DriverLibraryApproval implements UserApprovable {
    private final File library;

    private final DriverLibraryInfo libraryInfo;
    private final String fingerprint;

    @SneakyThrows
    public DriverLibraryApproval(File library) {
        this.library = library.getCanonicalFile();
        this.libraryInfo = new DriverLibraryInfo(library);

        this.fingerprint = Measured.call("calculating checksum for library " + library, () -> sha256(libraryInfo.getJars()));
    }

    @Override
    public Set<UserApprovalAction> getApprovalActions() {
        return Set.of(DRIVER_LIBRARY_LOAD);
    }

    private static String sha256(List<File> files) {
        StringBuilder buffer = new StringBuilder();
        for (File file : files) {
            buffer.append(file.getName())
                    .append(':')
                    .append(Checksum.fromFileContent(file, SHA_256))
                    .append('\n');
        }
        return Checksum.fromStringContent(buffer.toString(), SHA_256);
    }
}
