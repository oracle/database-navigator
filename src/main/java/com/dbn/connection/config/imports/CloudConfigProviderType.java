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

package com.dbn.connection.config.imports;

import com.dbn.common.ui.Presentable;
import lombok.Getter;

@Getter
public enum CloudConfigProviderType implements Presentable {
    OCI_OBJECT("OCI Object Storage"),
    OCI_DB_TOOLS("OCI Database Tools"),
    OCI_VAULT("OCI Vault"),
    AZURE_APP_CONFIG("Azure App Configuration"),
    AZURE_VAULT("Azure Vault"),
    AWS_S3("AWS S3"),
    AWS_SECRETS("AWS Secrets Manager"),
    AWS_PARAMETER_STORE("AWS Systems Manager Parameter Store"),
    AWS_APPCONFIG("AWS AppConfig"),
    GCP_STORAGE("GCP Cloud Storage"),
    GCP_SECRET_MANAGER("GCP Secret Manager"),
    HASHICORP_VAULT("HashiCorp Vault");

    private final String name;

    CloudConfigProviderType(String name) {
        this.name = name;
    }

    public String getSlug() {
        return switch (this) {
            case OCI_OBJECT -> "ociobject";
            case OCI_DB_TOOLS -> "ocidbtools";
            case OCI_VAULT -> "ocivault";
            case AZURE_APP_CONFIG -> "azure";
            case AZURE_VAULT -> "azurevault";
            case AWS_S3 -> "awss3";
            case AWS_SECRETS -> "awssecretsmanager";
            case AWS_PARAMETER_STORE -> "awsparameterstore";
            case AWS_APPCONFIG -> "awsappconfig";
            case GCP_STORAGE -> "gcpstorage";
            case GCP_SECRET_MANAGER -> "gcpsecretmanager";
            case HASHICORP_VAULT -> "hcpvaultdedicated";
        };
    }

    public static CloudConfigProviderType fromSlug(String slug) {
        if (slug == null) return null;

        for (CloudConfigProviderType type : values()) {
            if (type.getSlug().equalsIgnoreCase(slug)) {
                return type;
            }
        }
        return null;
    }

    public boolean isOci() {
        return this == OCI_OBJECT ||
                this == OCI_DB_TOOLS ||
                this == OCI_VAULT;
    }
}
