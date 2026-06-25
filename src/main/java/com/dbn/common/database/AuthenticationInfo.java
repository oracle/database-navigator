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
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.TimeAware;
import com.dbn.connection.AuthenticationTokenType;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.credentials.Secret;
import com.dbn.credentials.Secrets;
import com.dbn.credentials.SecretsOwner;
import com.dbn.credentials.TransientSecretStore;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

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
import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setSensitiveString;
import static com.dbn.common.options.setting.Settings.setString;
import static com.dbn.common.util.Commons.match;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.Strings.nvle;
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
    private boolean temporary;

    private AuthenticationType type = USER_PASSWORD;
    private String user;
    private final Secret password = new Secret(CONNECTION_PASSWORD, () -> getSecretOwnerId(), () -> user);

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
    private final Secret azureClientCertificatePassword = new Secret(CONNECTION_AZURE_TOKEN_CERTIFICATE_PASSWORD, () -> getSecretOwnerId(), () -> user);
    private final Secret azureClientSecret = new Secret(CONNECTION_AZURE_TOKEN_CLIENT_SECRET, () -> getSecretOwnerId(), () -> user);

    public AuthenticationInfo(ConnectionDatabaseSettings parent, boolean temporary) {
        super(parent);
        this.temporary = temporary;
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
                return
                    isNotEmpty(user) &&
                    password.isProvided();
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
                            azureClientSecret.isProvided() &&
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
        if (type.isOneOf(USER, USER_PASSWORD)) return isNotEmpty(user);
        if (type == AuthenticationType.TOKEN) return azureClientSecret.isProvided();
        return false;
    }

    /**
     * Checks if instance 'that' "matches" this instance.  This is similar to a deepEquals between
     * the objects, however it covers additional equivalences.
     *
     * @see com.dbn.common.util.Commons#match(Object, Object)
     * @see Secrets#match(Secret, Secret)
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
    		case OS_CREDENTIALS:
    			return match(this.user, that.user) &&
                       Secrets.match(this.password, that.password);
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
                               Secrets.match(this.azureClientCertificatePassword, that.azureClientCertificatePassword) &&
                               match(this.azureDatabaseApplicationIdUri, that.azureDatabaseApplicationIdUri);

                    case AZURE_SERVICE_PRINCIPAL_TOKEN:
                        return match(this.azureClientId, that.azureClientId) &&
                                match(this.azureTenantId, that.azureTenantId) &&
                                Secrets.match(this.azureClientSecret, that.azureClientSecret) &&
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
            // transfer secrets outside transient config xml
            TransientSecretStore.consume(password, getSecretOwnerId(), CONNECTION_PASSWORD, user);
            TransientSecretStore.consume(azureClientSecret, getSecretOwnerId(), CONNECTION_AZURE_TOKEN_CLIENT_SECRET, user);
            TransientSecretStore.consume(azureClientCertificatePassword, getSecretOwnerId(), CONNECTION_AZURE_TOKEN_CERTIFICATE_PASSWORD, user);
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
        if (!Constants.isOneOf(type, supportedAuthTypes)) {
            type = supportedAuthTypes[0];
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        setEnum(element, "type", type);
        setString(element, "user", nvle(user));

        if (isTransientContext()) {
            // transfer secrets outside transient config xml
            TransientSecretStore.store(password, getSecretOwnerId(), CONNECTION_PASSWORD, user);
            TransientSecretStore.store(azureClientSecret, getSecretOwnerId(), CONNECTION_AZURE_TOKEN_CLIENT_SECRET, user);
            TransientSecretStore.store(azureClientCertificatePassword, getSecretOwnerId(), CONNECTION_AZURE_TOKEN_CERTIFICATE_PASSWORD, user);
        }

        setEnum(element, TOKEN_TYPE, tokenType);
        setSensitiveString(element, TOKEN_CONFIG_FILE, tokenConfigFile);
        setString(element, TOKEN_PROFILE, tokenProfile);
        setString(element, ADB_COMPARTMENT_OCID, compartmentOcid);
        setString(element, ADB_DATABASE_OCID, databaseOcid);

        setString(element, AZURE_TOKEN_DATABASE_ID_URI, azureDatabaseApplicationIdUri);
        setString(element, AZURE_TOKEN_TENANT_ID, azureTenantId);
        setString(element, AZURE_TOKEN_CLIENT_ID, azureClientId);
        setSensitiveString(element, AZURE_TOKEN_CLIENT_CERTIFICATE_FILE, azureClientCertificateFile);
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
        this.password.setToken(that.password);

        this.tokenType = that.tokenType;
        this.tokenConfigFile = that.tokenConfigFile;
        this.tokenProfile = that.tokenProfile;
        this.databaseOcid = that.databaseOcid;
        this.compartmentOcid = that.compartmentOcid;

        this.azureDatabaseApplicationIdUri = that.azureDatabaseApplicationIdUri;
        this.azureClientId = that.azureClientId;
        this.azureTenantId = that.azureTenantId;
        this.azureClientSecret.setToken(that.azureClientSecret);
        this.azureClientCertificateFile = that.azureClientCertificateFile;
        this.azureClientCertificatePassword.setToken(that.azureClientCertificatePassword);
    }

    @Override
    public boolean equals(Object o) {
        // lombok override (avoid using accessors / exclude irrelevant timestamp and temporary flag)
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationInfo that = (AuthenticationInfo) o;
        return type == that.type &&
                tokenType == that.tokenType &&
                Objects.equals(user, that.user) &&
                Secrets.match(password, that.password) &&
                Objects.equals(tokenConfigFile, that.tokenConfigFile) &&
                Objects.equals(tokenProfile, that.tokenProfile) &&
                Objects.equals(compartmentOcid, that.compartmentOcid) &&
                Objects.equals(databaseOcid, that.databaseOcid) &&
                Objects.equals(azureClientId, that.azureClientId) &&
                Objects.equals(azureTenantId, that.azureTenantId) &&
                Objects.equals(azureClientCertificateFile, that.azureClientCertificateFile) &&
                Secrets.match(azureClientCertificatePassword, that.azureClientCertificatePassword) &&
                Objects.equals(azureDatabaseApplicationIdUri, that.azureDatabaseApplicationIdUri) &&
                Secrets.match(azureClientSecret, that.azureClientSecret);
    }

    @Override
    public int hashCode() {
        // lombok override (avoid using accessors / exclude irrelevant timestamp and temporary flag)
        return Objects.hash(
                type,
                user,
                Secrets.hash(password),
                tokenType,
                tokenConfigFile,
                tokenProfile,
                compartmentOcid,
                databaseOcid,
                azureClientId,
                azureTenantId,
                azureClientCertificateFile,
                azureDatabaseApplicationIdUri,
                Secrets.hash(azureClientCertificatePassword),
                Secrets.hash(azureClientSecret));
    }

    /*********************************************************
     *                     SecretHolder                      *
     *********************************************************/

    @NotNull
    @Override
    public Object getSecretOwnerId() {
        return getConnectionSettings().getSecretOwnerId();
    }

    @Override
    public String getSecretOwnerName() {
        return getConnectionSettings().getSecretOwnerName();
    }

    private @NotNull ConnectionSettings getConnectionSettings() {
        return ensureParent().ensureParent();
    }

    @Override
    public Secret[] getSecrets() {
        return new Secret[] {
                password,
                azureClientSecret,
                azureClientCertificatePassword};
    }

    public char[] getPassword() {
        return password.getToken();
    }

    public void setPassword(char[] password) {
        this.password.setToken(password);
    }

    public char[] getAzureClientSecret() {
        return azureClientSecret.getToken();
    }

    public void setAzureClientSecret(char[] azureClientSecret) {
        this.azureClientSecret.setToken(azureClientSecret);
    }

    public char[] getAzureClientCertificatePassword() {
        return azureClientCertificatePassword.getToken();
    }

    public void setAzureClientCertificatePassword(char[] azureClientCertificatePassword) {
        this.azureClientCertificatePassword.setToken(azureClientCertificatePassword);
    }

}
