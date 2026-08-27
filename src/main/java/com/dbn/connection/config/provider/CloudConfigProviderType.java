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

package com.dbn.connection.config.provider;

import com.dbn.common.ui.Presentable;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

import static com.dbn.connection.config.provider.CloudConfigProviderFamily.AWS;
import static com.dbn.connection.config.provider.CloudConfigProviderFamily.AZURE;
import static com.dbn.connection.config.provider.CloudConfigProviderFamily.GCP;
import static com.dbn.connection.config.provider.CloudConfigProviderFamily.HASHICORP;
import static com.dbn.connection.config.provider.CloudConfigProviderFamily.OCI;
import static com.dbn.nls.NlsResources.txt;

@Getter
public enum CloudConfigProviderType implements Presentable {
    OCI_OBJECT(OCI, "OCI Object Storage", "ociobject"),
    OCI_DB_TOOLS(OCI, "OCI Database Tools", "ocidbtools"),
    OCI_VAULT(OCI, "OCI Vault", "ocivault"),
    AZURE_APP_CONFIG(AZURE, "Azure App Configuration", "azure"),
    AZURE_VAULT(AZURE, "Azure Vault", "azurevault"),
    AWS_S3(AWS, "AWS S3", "awss3"),
    AWS_SECRETS(AWS, "AWS Secrets Manager", "awssecretsmanager"),
    GCP_STORAGE(GCP, "GCP Cloud Storage", "gcpstorage"),
    GCP_SECRET_MANAGER(GCP, "GCP Secret Manager", "gcpsecretmanager"),
    HASHICORP_VAULT(HASHICORP, "HashiCorp Vault", "hcpvaultdedicated");

    private final CloudConfigProviderFamily family;
    private final String name;
    private final String slug;

    CloudConfigProviderType(CloudConfigProviderFamily family, String name, @NonNls String slug) {
        this.family = family;
        this.name = name;
        this.slug = slug;
    }

    public String getLocationName() {
        return txt("cfg.connection.label.ProviderSourceLocation_" + name());
    }

    public String getDocUrl() {
        return txt("cfg.connection.url.ProviderDocumentation_" + name());
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
        return getFamily() == OCI;
    }

    public boolean isAws() {
        return getFamily() == AWS;
    }

    public boolean isGcp() {
        return getFamily() == GCP;
    }

    public boolean isAzure() {
        return getFamily() == AZURE;
    }

    public boolean isHashicorp() {
        return getFamily() == HASHICORP;
    }

    public String getAwsRegionParameterName() {
        return isAws() ? "AWS_REGION" : null;
    }
}
