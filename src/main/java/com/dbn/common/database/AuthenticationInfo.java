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

package com.dbn.common.database;

import com.dbn.common.constant.Constants;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.TimeAware;
import com.dbn.connection.AuthenticationTokenType;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.credentials.DatabaseCredentialManager;
import com.dbn.credentials.Secret;
import com.dbn.credentials.SecretsOwner;
import com.dbn.credentials.SecretsOwnerRegistry;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

import static com.dbn.common.database.AuthenticationInfo.Attributes.ADB_COMPARTMENT_OCID;
import static com.dbn.common.database.AuthenticationInfo.Attributes.ADB_DATABASE_OCID;
import static com.dbn.common.database.AuthenticationInfo.Attributes.AZURE_TOKEN_CLIENT_CERTIFICATE_FILE;
import static com.dbn.common.database.AuthenticationInfo.Attributes.AZURE_TOKEN_CLIENT_ID;
import static com.dbn.common.database.AuthenticationInfo.Attributes.AZURE_TOKEN_DATABASE_ID_URI;
import static com.dbn.common.database.AuthenticationInfo.Attributes.AZURE_TOKEN_TENANT_ID;
import static com.dbn.common.database.AuthenticationInfo.Attributes.TOKEN_CONFIG_FILE;
import static com.dbn.common.database.AuthenticationInfo.Attributes.TOKEN_PROFILE;
import static com.dbn.common.database.AuthenticationInfo.Attributes.TOKEN_TYPE;
import static com.dbn.common.options.setting.Settings.getChars;
import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.setChars;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setString;
import static com.dbn.common.util.Base64.decode;
import static com.dbn.common.util.Base64.encode;
import static com.dbn.common.util.Commons.match;
import static com.dbn.common.util.Commons.matchArrays;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.connection.AuthenticationTokenType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
import static com.dbn.connection.AuthenticationTokenType.AZURE_SERVICE_PRINCIPAL_TOKEN;
import static com.dbn.connection.AuthenticationType.BASIC_AUTH;
import static com.dbn.connection.AuthenticationType.OS_CREDENTIALS;
import static com.dbn.connection.AuthenticationType.USER;
import static com.dbn.connection.AuthenticationType.USER_PASSWORD;
import static com.dbn.credentials.SecretType.CONNECTION_AZURE_TOKEN_CERTIFICATE_PASSWORD;
import static com.dbn.credentials.SecretType.CONNECTION_AZURE_TOKEN_CLIENT_SECRET;
import static com.dbn.credentials.SecretType.CONNECTION_PASSWORD;

@Getter
@Setter
public class AuthenticationInfo extends BasicConfiguration<ConnectionDatabaseSettings, ConfigurationEditorForm> implements Cloneable<AuthenticationInfo>, TimeAware, SecretsOwner {

    interface Attributes {
        @NonNls String TOKEN_TYPE = "token-type";
        @NonNls String TOKEN_CONFIG_FILE = "token-config-file";
        @NonNls String TOKEN_PROFILE = "token-profile";
        @NonNls String ADB_COMPARTMENT_OCID = "adb-compartment-ocid";
        @NonNls String ADB_DATABASE_OCID = "adb-database-ocid";
        @NonNls String AZURE_TOKEN_CLIENT_ID = "azure-token-client-id";
        @NonNls String AZURE_TOKEN_TENANT_ID = "azure-token-tenant-id";
        @NonNls String AZURE_TOKEN_DATABASE_ID_URI = "azure-token-database-id-uri";
        @NonNls String AZURE_TOKEN_CLIENT_CERTIFICATE_FILE = "azure-token-client-certificate-file";
    }

    private final long timestamp = System.currentTimeMillis();

    private AuthenticationType type = USER_PASSWORD;
    private String user;
    private char[] password;
    private boolean temporary;
    
    // token auth
    private AuthenticationTokenType tokenType;

    // OCI token auth
    private String tokenConfigFile;
    private String tokenProfile;
    private String databaseOcid;
    private String compartmentOcid;

    // Azure token auth
    private String azureDatabaseApplicationIdUri;
    private String azureClientId;
    private String azureTenantId;
    private String azureClientCertificateFile;
    private char[] azureClientCertificatePassword;
    private char[] azureClientSecret;

    public AuthenticationInfo(ConnectionDatabaseSettings parent, boolean temporary) {
        super(parent);
        this.temporary = temporary;
        SecretsOwnerRegistry.register(this);
    }

    public ConnectionId getConnectionId() {
        return ensureParent().getConnectionId();
    }

    /**
     * The connection settings often uses "temporary" authentication objects that expire after
     * a certain time.  When that happens, we need to re-prompt the user for secret data like
     * passwords.  This method returns "true" if the temporary authentication data is still cached in memory
     * or "false" if the user needs to be prompted to re-enter them for this form.
     *
     * @return true if necessary info is still present in this info; false if they have expired.
     */
	public boolean isProvided() {
        // TODO C.B: Do we need a null check here?
        switch (type) {
            case USER: return isNotEmpty(user);
            case USER_PASSWORD:
            case BASIC_AUTH:
                return
                    isNotEmpty(user) &&
                    Chars.isNotEmpty(password);
            case TOKEN:
                // it's possible for token type to be null and that's not germaine here.
                if (tokenType == null) {
                    return false;
                }
                switch (tokenType) {
                    case OCI_INTERACTIVE: return true;
                    case OCI_API_KEY:
                        return
                            isNotEmpty(tokenConfigFile) &&
                            isNotEmpty(tokenProfile);

                    case AZURE_INTERACTIVE:
                        return
                            isNotEmpty(azureDatabaseApplicationIdUri);

                    case AZURE_SERVICE_PRINCIPAL_CERTIFICATE:
                        // Note: azureClientCertificatePassword can be empty as non-password protected
                        // certificate files are allowed.
                        return
                            isNotEmpty(azureClientId) &&
                            isNotEmpty(azureTenantId) &&
                            isNotEmpty(azureClientCertificateFile) &&
                            isNotEmpty(azureDatabaseApplicationIdUri);

                    case AZURE_SERVICE_PRINCIPAL_TOKEN:
                        return
                            isNotEmpty(azureClientId) &&
                            isNotEmpty(azureTenantId) &&
                            Chars.isNotEmpty(azureClientSecret) &&
                            isNotEmpty(azureDatabaseApplicationIdUri);
                }
            case NONE:
            case OS_CREDENTIALS:
            default:
                return true;
        }
    }

    /**
     * Utility returning the availability of "user" information in the authentication data
     * (user availability may be relevant when attempting to re-connect after receiving authentication errors
     *    e.g. for preventing user from being locked out after too many failed authentication attempts)
     * @return true if the AuthenticationInfo has user information, false otherwise
     */
    public boolean hasUserInformation() {
        if (type == OS_CREDENTIALS) return true;
        if (type.isOneOf(USER, USER_PASSWORD, BASIC_AUTH)) return isNotEmpty(user);
        if (type == AuthenticationType.TOKEN) return Chars.isNotEmpty(azureClientSecret);
        return false;
    }

    /**
     * Checks if instance 'that' "matches" this instance.  This is similar to a deepEquals between
     * the objects, however it covers additional equivalences.
     *
     * @see com.dbn.common.util.Commons#match(Object, Object)
     * @see com.dbn.common.util.Commons#matchArrays(Object, Object)
     *
     * @param that the other object to check
     * @return true if this "matches" that, false otherwise.
     */
    public boolean isSame(AuthenticationInfo that) {
        // TODO C.B.: Check type and that.type for null?
    	if (this.type != that.type) return false;
    	switch (this.type) {
    		case NONE:
    		case USER:
    		case USER_PASSWORD:
            case BASIC_AUTH:
    		case OS_CREDENTIALS:
    			return match(this.user, that.user) &&
    		           matchArrays(this.password, that.password);
    		case TOKEN:
                if (!match(this.tokenType, that.tokenType)) return false;

                switch (tokenType) {
                    case OCI_INTERACTIVE:
                        return  match(this.compartmentOcid, that.compartmentOcid) &&
                                match(this.databaseOcid, that.databaseOcid);

                    case OCI_API_KEY:
                        return match(this.tokenConfigFile, that.tokenConfigFile) &&
                               match(this.tokenProfile, that.tokenProfile) &&
                               match(this.compartmentOcid, that.compartmentOcid) &&
                               match(this.databaseOcid, that.databaseOcid);

                    case AZURE_INTERACTIVE:
                        return match(this.azureDatabaseApplicationIdUri, that.azureDatabaseApplicationIdUri);

                    case AZURE_SERVICE_PRINCIPAL_CERTIFICATE:
                        return match(this.azureClientId, that.azureClientId) &&
                               match(this.azureTenantId, that.azureTenantId) &&
                               match(this.azureClientCertificateFile, that.azureClientCertificateFile) &&
                               matchArrays(this.azureClientCertificatePassword, that.azureClientCertificatePassword) &&
                               match(this.azureDatabaseApplicationIdUri, that.azureDatabaseApplicationIdUri);

                    case AZURE_SERVICE_PRINCIPAL_TOKEN:
                        return match(this.azureClientId, that.azureClientId) &&
                                match(this.azureTenantId, that.azureTenantId) &&
                                matchArrays(this.azureClientSecret, that.azureClientSecret) &&
                                match(this.azureDatabaseApplicationIdUri, that.azureDatabaseApplicationIdUri);

                }

    		default:
    			return false;
    	}
    }

    @Override
    public void readConfiguration(Element element) {
        type = getEnum(element, "type", type);
        user = getString(element, "user", user);
        adjustAuthenticationType();

        if (isTransientContext()) {
            // only propagate password when config context is transient
            // (avoid storing it in config xml)
            password = decode(getChars(element, "transient-password", encode(password)));
            azureClientSecret = decode(getChars(element, "transient-azure-client-secret", encode(azureClientSecret)));
            azureClientCertificatePassword = decode(getChars(element, "transient-azure-cert-password", encode(azureClientCertificatePassword)));
        }

        // token auth attributes
        tokenType = getEnum(element, TOKEN_TYPE, AuthenticationTokenType.class);
        tokenConfigFile = getString(element, TOKEN_CONFIG_FILE, tokenConfigFile);
        tokenProfile = getString(element, TOKEN_PROFILE, tokenProfile);
        compartmentOcid = getString(element, ADB_COMPARTMENT_OCID, compartmentOcid);
        databaseOcid = getString(element, ADB_DATABASE_OCID, databaseOcid);

        // azure auth attributes
        azureClientId = getString(element, AZURE_TOKEN_CLIENT_ID, azureClientId);
        azureTenantId = getString(element, AZURE_TOKEN_TENANT_ID, azureTenantId);
        azureClientCertificateFile = getString(element, AZURE_TOKEN_CLIENT_CERTIFICATE_FILE, azureClientCertificateFile);
        azureDatabaseApplicationIdUri = getString(element, AZURE_TOKEN_DATABASE_ID_URI, azureDatabaseApplicationIdUri);
    }

    /**
     * Make sure the authentication type matches one of the supported types
     */
    private void adjustAuthenticationType() {
        AuthenticationType[] supportedAuthTypes = ensureParent().getDatabaseType().getAuthTypes();
        if (type == BASIC_AUTH && isHttpsConfigFile()) return;

        if (!Constants.isOneOf(type, supportedAuthTypes)) {
            type = supportedAuthTypes[0];
        }
    }

    private boolean isHttpsConfigFile() {
        DatabaseInfo databaseInfo = ensureParent().getDatabaseInfo();
        return databaseInfo.getUrlType() == DatabaseUrlType.CONFIG_FILE &&
                databaseInfo.getConfigProviderInfo().isConfigHttps();
    }

    @Override
    public void writeConfiguration(Element element) {
        setEnum(element, "type", type);
        setString(element, "user", nvl(user));

        if (isTransientContext()) {
            // only propagate password when config context is transient
            // (avoid storing it in config xml)
            setChars(element, "transient-password", encode(password));
            setChars(element, "transient-azure-client-secret", encode(azureClientSecret));
            setChars(element, "transient-azure-cert-password", encode(azureClientCertificatePassword));
        }

        setEnum(element, TOKEN_TYPE, tokenType);
        setString(element, TOKEN_CONFIG_FILE, tokenConfigFile);
        setString(element, TOKEN_PROFILE, tokenProfile);
        setString(element, ADB_COMPARTMENT_OCID, compartmentOcid);
        setString(element, ADB_DATABASE_OCID, databaseOcid);

        setString(element, AZURE_TOKEN_DATABASE_ID_URI, azureDatabaseApplicationIdUri);
        setString(element, AZURE_TOKEN_TENANT_ID, azureTenantId);
        setString(element, AZURE_TOKEN_CLIENT_ID, azureClientId);
        setString(element, AZURE_TOKEN_CLIENT_CERTIFICATE_FILE, azureClientCertificateFile);
    }

    @Override
    public AuthenticationInfo clone() {
        AuthenticationInfo authenticationInfo = new AuthenticationInfo(getParent(), temporary);
        authenticationInfo.updateWith(this);
        return authenticationInfo;
    }

    public void updateWith(AuthenticationInfo that) {
        this.type = that.type;
        this.user = that.user;
        this.password = that.password;

        this.tokenType = that.tokenType;
        this.tokenConfigFile = that.tokenConfigFile;
        this.tokenProfile = that.tokenProfile;
        this.databaseOcid = that.databaseOcid;
        this.compartmentOcid = that.compartmentOcid;

        this.azureDatabaseApplicationIdUri = that.azureDatabaseApplicationIdUri;
        this.azureClientId = that.azureClientId;
        this.azureTenantId = that.azureTenantId;
        this.azureClientSecret = that.azureClientSecret;
        this.azureClientCertificateFile = that.azureClientCertificateFile;
        this.azureClientCertificatePassword = that.azureClientCertificatePassword;
    }

    @Override
    public boolean equals(Object o) {
        // lombok override (avoid using accessors / exclude irrelevant timestamp and temporary flag)
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationInfo that = (AuthenticationInfo) o;
        return type == that.type &&
                tokenType == that.tokenType &&
                Objects.equals(user, that.user) &&
                Objects.deepEquals(password, that.password) &&
                Objects.equals(tokenConfigFile, that.tokenConfigFile) &&
                Objects.equals(tokenProfile, that.tokenProfile) &&
                Objects.equals(compartmentOcid, that.compartmentOcid) &&
                Objects.equals(databaseOcid, that.databaseOcid) &&
                Objects.equals(azureClientId, that.azureClientId) &&
                Objects.equals(azureTenantId, that.azureTenantId) &&
                Objects.equals(azureClientCertificateFile, that.azureClientCertificateFile) &&
                Objects.deepEquals(azureClientCertificatePassword, that.azureClientCertificatePassword) &&
                Objects.equals(azureDatabaseApplicationIdUri, that.azureDatabaseApplicationIdUri) &&
                Objects.deepEquals(azureClientSecret, that.azureClientSecret);
    }

    @Override
    public int hashCode() {
        // lombok override (avoid using accessors / exclude irrelevant timestamp and temporary flag)
        return Objects.hash(
                type,
                user,
                Arrays.hashCode(password),
                tokenType,
                tokenConfigFile,
                tokenProfile,
                compartmentOcid,
                databaseOcid,
                azureClientId,
                azureTenantId,
                azureClientCertificateFile,
                azureDatabaseApplicationIdUri,
                Arrays.hashCode(azureClientCertificatePassword),
                Arrays.hashCode(azureClientSecret));
    }

    /*********************************************************
     *                     SecretHolder                      *
     *********************************************************/

    @NotNull
    @Override
    public Object getSecretOwnerId() {
        return getConnectionId();
    }

    @Override
    public String getSecretOwnerName() {
        return ensureParent().getName();
    }

    @Override
    public Secret[] getSecrets() {
        return new Secret[] {
                getPasswordSecret(),
                getAzureTokenClientSecret(),
                getAzureTokenCertificatePassword()};
    }

    private Secret getPasswordSecret() {
        return new Secret(CONNECTION_PASSWORD, user, password);
    }

    private Secret getAzureTokenClientSecret() {
        return new Secret(CONNECTION_AZURE_TOKEN_CLIENT_SECRET, null, azureClientSecret);
    }

    private Secret getAzureTokenCertificatePassword() {
        return new Secret(CONNECTION_AZURE_TOKEN_CERTIFICATE_PASSWORD, null, azureClientCertificatePassword);
    }

    @Override
    public void initSecrets() {
        DatabaseCredentialManager credentialManager = DatabaseCredentialManager.getInstance();

        if (type == AuthenticationType.USER_PASSWORD || type == BASIC_AUTH) {
            Secret secret = credentialManager.loadSecret(CONNECTION_PASSWORD, getConnectionId(), user);
            password = secret.getToken();
        }
        else if (type == AuthenticationType.TOKEN) {
            if (tokenType == AZURE_SERVICE_PRINCIPAL_TOKEN) {
                Secret secret = credentialManager.loadSecret(CONNECTION_AZURE_TOKEN_CLIENT_SECRET, getConnectionId(), user);
                azureClientSecret = secret.getToken();

            } else if (tokenType == AZURE_SERVICE_PRINCIPAL_CERTIFICATE) {
                Secret secret = credentialManager.loadSecret(CONNECTION_AZURE_TOKEN_CERTIFICATE_PASSWORD, getConnectionId(), user);
                azureClientCertificatePassword = secret.getToken();
            }
        }
    }
}
