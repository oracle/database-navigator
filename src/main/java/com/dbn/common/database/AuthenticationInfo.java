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
import com.dbn.common.options.ConfigMonitor;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.TimeAware;
import com.dbn.connection.AuthenticationTokenType;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.credentials.DatabaseCredentialManager;
import com.dbn.credentials.Secret;
import com.dbn.credentials.SecretsOwner;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

import static com.dbn.common.database.AuthenticationInfo.Attributes.DEPRECATED_PWD_ATTRIBUTE;
import static com.dbn.common.database.AuthenticationInfo.Attributes.TOKEN_CONFIG_FILE;
import static com.dbn.common.database.AuthenticationInfo.Attributes.TOKEN_PROFILE;
import static com.dbn.common.database.AuthenticationInfo.Attributes.TOKEN_TYPE;
import static com.dbn.common.options.ConfigActivity.INITIALIZING;
import static com.dbn.common.options.setting.Settings.getChars;
import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.setChars;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setString;
import static com.dbn.common.util.Base64.decode;
import static com.dbn.common.util.Base64.encode;
import static com.dbn.common.util.Commons.match;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.connection.AuthenticationType.OS_CREDENTIALS;
import static com.dbn.connection.AuthenticationType.USER;
import static com.dbn.connection.AuthenticationType.USER_PASSWORD;
import static com.dbn.credentials.SecretType.CONNECTION_PASSWORD;

@Getter
@Setter
public class AuthenticationInfo extends BasicConfiguration<ConnectionDatabaseSettings, ConfigurationEditorForm> implements Cloneable<AuthenticationInfo>, TimeAware, SecretsOwner {

    interface Attributes {
        @NonNls String TOKEN_TYPE = "token-type";
        @NonNls String TOKEN_CONFIG_FILE = "token-config-file";
        @NonNls String TOKEN_PROFILE = "token-profile";


        @Deprecated // TODO moved to IDE keychain (cleanup after followup release)
        @NonNls String DEPRECATED_PWD_ATTRIBUTE = "deprecated-pwd";
    }

    private final long timestamp = System.currentTimeMillis();

    private AuthenticationType type = USER_PASSWORD;
    private String user;
    private char[] password;
    private boolean temporary;
    
    // token auth
    private AuthenticationTokenType tokenType;
    private String tokenConfigFile;
    private String tokenProfile;

    public AuthenticationInfo(ConnectionDatabaseSettings parent, boolean temporary) {
        super(parent);
        this.temporary = temporary;
    }

    public ConnectionId getConnectionId() {
        return ensureParent().getConnectionId();
    }

	public boolean isProvided() {
        switch (type) {
            case NONE: return true;
            case USER: return isNotEmpty(user);
            case USER_PASSWORD: return isNotEmpty(user) && Chars.isNotEmpty(password);
            case OS_CREDENTIALS: return true;
            case TOKEN: return tokenType == AuthenticationTokenType.OCI_INTERACTIVE ||
                    (isNotEmpty(tokenConfigFile) && isNotEmpty(tokenProfile));
        }
        return true;
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

        return false;
    }

    public boolean isSame(AuthenticationInfo authenticationInfo) {
    	if (authenticationInfo.type != type) return false;
    	switch (type) {
    		case NONE:
    		case USER:
    		case USER_PASSWORD:
    		case OS_CREDENTIALS:
    			return match(this.user, authenticationInfo.user) &&
    		           match(this.password, authenticationInfo.password);
    		case TOKEN:
                return match(this.tokenConfigFile, authenticationInfo.tokenConfigFile) &&
                       match(this.tokenProfile, authenticationInfo.tokenProfile) &&
                       match(this.tokenType, authenticationInfo.tokenType);
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
        }



        // token auth attributes
        tokenType = getEnum(element, TOKEN_TYPE, AuthenticationTokenType.class);
        tokenConfigFile = getString(element, TOKEN_CONFIG_FILE, tokenConfigFile);
        tokenProfile = getString(element, TOKEN_PROFILE, tokenProfile);

        restorePassword(element);
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
        setString(element, "user", nvl(user));

        if (isTransientContext()) {
            // only propagate password when config context is transient
            // (avoid storing it in config xml)
            setChars(element, "transient-password", encode(password));
        }

        setEnum(element, TOKEN_TYPE, tokenType);
        setString(element, TOKEN_CONFIG_FILE, tokenConfigFile);
        setString(element, TOKEN_PROFILE, tokenProfile);
    }

    @Deprecated // TODO cleanup in subsequent release (temporarily support old storage)
    private void restorePassword(Element element) {
        if (!ConfigMonitor.is(INITIALIZING)) return; // only during config initialization

        if (type != USER_PASSWORD) return;
        if (Chars.isNotEmpty(password)) return;

        password = decode(getChars(element, DEPRECATED_PWD_ATTRIBUTE, Chars.EMPTY_ARRAY));
        // password still in old config store
        if (isNotEmpty(user) && Chars.isNotEmpty(password)) {
            DatabaseCredentialManager credentialManager = DatabaseCredentialManager.getInstance();
            credentialManager.queueSecretsInsert(getConnectionId(), getPasswordSecret());
        }
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
                Objects.equals(tokenProfile, that.tokenProfile);
    }

    @Override
    public int hashCode() {
        // lombok override (avoid using accessors / exclude irrelevant timestamp and temporary flag)
        return Objects.hash(type, user, Arrays.hashCode(password), tokenType, tokenConfigFile, tokenProfile);
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
    public Secret[] getSecrets() {
        return new Secret[] {getPasswordSecret()};
    }

    private Secret getPasswordSecret() {
        return new Secret(CONNECTION_PASSWORD, user, password);
    }

    @Override
    public void initSecrets() {
        if (type == AuthenticationType.USER_PASSWORD) {
            DatabaseCredentialManager credentialManager = DatabaseCredentialManager.getInstance();
            Secret secret = credentialManager.loadSecret(CONNECTION_PASSWORD, getConnectionId(), user);
            password = secret.getToken();
        }
    }
}
