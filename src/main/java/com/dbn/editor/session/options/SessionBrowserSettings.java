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

package com.dbn.editor.session.options;

import com.dbn.common.option.InteractiveOptionBroker;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.options.setting.Settings;
import com.dbn.connection.operation.options.OperationSettings;
import com.dbn.editor.session.options.ui.SessionBrowserSettingsForm;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class SessionBrowserSettings extends BasicConfiguration<OperationSettings, SessionBrowserSettingsForm> {
    public static final String REMEMBER_OPTION_HINT = ""; //"\n\n(you can remember your option and change it at any time in Settings > Operations > Session Manager)";

    private boolean reloadOnFilterChange = false;
    private final InteractiveOptionBroker<SessionInterruptionOption> disconnectSession =
            new InteractiveOptionBroker<>(
                    "disconnect-session",
                    "app.sessions.title.DisconnectSessions",
                    "app.sessions.message.DisconnectSessions" /*+ REMEMBER_OPTION_HINT*/,
                    SessionInterruptionOption.ASK,
                    SessionInterruptionOption.IMMEDIATE,
                    SessionInterruptionOption.POST_TRANSACTION,
                    SessionInterruptionOption.CANCEL);

    private final InteractiveOptionBroker<SessionInterruptionOption> killSession =
            new InteractiveOptionBroker<>(
                    "kill-session",
                    "app.sessions.title.KillSessions",
                    "app.sessions.message.KillSessions"/* + REMEMBER_OPTION_HINT*/,
                    SessionInterruptionOption.ASK,
                    SessionInterruptionOption.NORMAL,
                    SessionInterruptionOption.IMMEDIATE,
                    SessionInterruptionOption.CANCEL);

    public SessionBrowserSettings(OperationSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.sessions.title.SessionBrowser");
    }

    @Override
    public String getHelpTopic() {
        return "sessionBrowser";
    }


    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    @NotNull
    public SessionBrowserSettingsForm createConfigurationEditor() {
        return new SessionBrowserSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "session-browser";
    }

    @Override
    public void readConfiguration(Element element) {
        disconnectSession.readConfiguration(element);
        killSession.readConfiguration(element);
        reloadOnFilterChange = Settings.getBoolean(element, "reload-on-filter-change", reloadOnFilterChange);
    }

    @Override
    public void writeConfiguration(Element element) {
        disconnectSession.writeConfiguration(element);
        killSession.writeConfiguration(element);
        Settings.setBoolean(element, "reload-on-filter-change", reloadOnFilterChange);
    }
}
