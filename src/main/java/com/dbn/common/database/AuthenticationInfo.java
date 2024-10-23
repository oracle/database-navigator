package com.dbn.common.database;

import com.dbn.common.constant.Constants;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Strings;
import com.dbn.common.util.TimeAware;
import com.dbn.connection.AuthenticationTokenType;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.Passwords;
import com.dbn.credentials.DatabaseCredentialManager;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.database.AuthenticationInfo.Attributes.OLD_PWD_ATTRIBUTE;
import static com.dbn.common.database.AuthenticationInfo.Attributes.TEMP_PWD_ATTRIBUTE;
import static com.dbn.common.database.AuthenticationInfo.Attributes.TOKEN_CONFIG_FILE;
import static com.dbn.common.database.AuthenticationInfo.Attributes.TOKEN_PROFILE;
import static com.dbn.common.database.AuthenticationInfo.Attributes.TOKEN_TYPE;
import static com.dbn.common.options.setting.Settings.getBoolean;
import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setString;
import static com.dbn.common.util.Commons.match;
import static com.dbn.common.util.Strings.isNotEmpty;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class AuthenticationInfo extends BasicConfiguration<ConnectionDatabaseSettings, ConfigurationEditorForm> implements Cloneable<AuthenticationInfo>, TimeAware {
    interface Attributes {
        String TOKEN_TYPE = "token-type";
        String TOKEN_CONFIG_FILE = "token-config-file";
        String TOKEN_PROFILE = "token-profile";


        @Deprecated // TODO move to keychain
        String OLD_PWD_ATTRIBUTE = "password";
        @Deprecated // TODO move to keychain
        String TEMP_PWD_ATTRIBUTE = "deprecated-pwd";
    }

    private final long timestamp = System.currentTimeMillis();

    private AuthenticationType type = AuthenticationType.USER_PASSWORD;
    private String user;
    private String password;
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

    public void setPassword(String password) {
        this.password = Strings.isEmpty(password) ? null : password;
    }

	public boolean isProvided() {
        switch (type) {
            case NONE: return true;
            case USER: return isNotEmpty(user);
            case USER_PASSWORD: return isNotEmpty(user) && isNotEmpty(password);
            case OS_CREDENTIALS: return true;
            case TOKEN: return tokenType == AuthenticationTokenType.OCI_INTERACTIVE ||
                    (isNotEmpty(tokenConfigFile) && isNotEmpty(tokenProfile));
        }
        return true;
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
        user = getString(element, "user", user);
        DatabaseCredentialManager credentialManager = DatabaseCredentialManager.getInstance();

        if (DatabaseCredentialManager.USE) {
            password = credentialManager.getPassword(getConnectionId(), user);
        }

        tokenType = getEnum(element, TOKEN_TYPE, AuthenticationTokenType.class);
        tokenConfigFile = getString(element, TOKEN_CONFIG_FILE, tokenConfigFile);
        tokenProfile = getString(element, TOKEN_PROFILE, tokenProfile);

        // old storage fallback - TODO cleanup
        if (Strings.isEmpty(password)) {
            password = Passwords.decodePassword(getString(element, TEMP_PWD_ATTRIBUTE, password));
            if (Strings.isEmpty(password)) {
                password = Passwords.decodePassword(getString(element, OLD_PWD_ATTRIBUTE, password));
            }

            if (isNotEmpty(this.password) && DatabaseCredentialManager.USE) {
                credentialManager.setPassword(getConnectionId(), user, this.password);
            }
        }

        type = getEnum(element, "type", type);

        AuthenticationType[] supportedAuthTypes = ensureParent().getDatabaseType().getAuthTypes();
        if (!Constants.isOneOf(type, supportedAuthTypes)) {
            type = supportedAuthTypes[0];
        }

        // TODO backward compatibility
        if (getBoolean(element, "os-authentication", false)) {
            type = AuthenticationType.OS_CREDENTIALS;
        } else if (getBoolean(element, "empty-authentication", false)) {
            type = AuthenticationType.USER;
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        setEnum(element, "type", type);
        setString(element, "user", nvl(user));

        String encodedPassword = Passwords.encodePassword(password);
        if (!DatabaseCredentialManager.USE){
            setString(element, TEMP_PWD_ATTRIBUTE, encodedPassword);
        }

        setEnum(element, TOKEN_TYPE, tokenType);
        setString(element, TOKEN_CONFIG_FILE, tokenConfigFile);
        setString(element, TOKEN_PROFILE, tokenProfile);
    }

    @Override
    public AuthenticationInfo clone() {
        AuthenticationInfo authenticationInfo = new AuthenticationInfo(getParent(), temporary);
        authenticationInfo.type = type;
        authenticationInfo.user = user;
        authenticationInfo.password = password;
        
        // Token Auth
        authenticationInfo.tokenConfigFile = this.tokenConfigFile;
        authenticationInfo.tokenProfile = this.tokenProfile;
        authenticationInfo.tokenType = this.tokenType;
        
        return authenticationInfo;
    }

    public void updateKeyChain(String oldUserName, String oldPassword) {
        if (type == AuthenticationType.USER_PASSWORD && !temporary && DatabaseCredentialManager.USE) {
            oldUserName = nvl(oldUserName);
            oldPassword = nvl(oldPassword);

            String newUserName = nvl(user);
            String newPassword = nvl(password);

            boolean userNameChanged = !match(oldUserName, newUserName);
            boolean passwordChanged = !match(oldPassword, newPassword);
            if (userNameChanged || passwordChanged) {
                DatabaseCredentialManager credentialManager = DatabaseCredentialManager.getInstance();
                ConnectionId connectionId = getConnectionId();

                if (userNameChanged) {
                    credentialManager.removePassword(connectionId, oldUserName);
                }
                if (isNotEmpty(newUserName) && isNotEmpty(newPassword)) {
                    credentialManager.setPassword(connectionId, newUserName, newPassword);
                }
            }
        }
    }
}
