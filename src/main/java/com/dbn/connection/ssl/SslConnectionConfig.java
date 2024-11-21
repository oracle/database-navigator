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

package com.dbn.connection.ssl;

import lombok.Value;

import java.io.File;

@Value
public class SslConnectionConfig {
    private final File certificateAuthorityFile;
    private final File clientCertificateFile;
    private final File clientKeyFile;

    public SslConnectionConfig(File certificateAuthorityFile, File clientCertificateFile, File clientKeyFile) {
        this.certificateAuthorityFile = certificateAuthorityFile;
        this.clientCertificateFile = clientCertificateFile;
        this.clientKeyFile = clientKeyFile;
    }
}
