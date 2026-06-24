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

package com.dbn.connection.config;

import com.dbn.common.option.InteractiveOptionBroker;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.connection.config.ui.ConnectionDebuggerSettingsForm;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.JDWPTunnelType;
import com.dbn.debugger.options.DebuggerTypeOption;
import com.intellij.util.Range;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.getBoolean;
import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getInteger;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBoolean;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setInteger;
import static com.dbn.common.options.setting.Settings.setString;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
public class ConnectionDebuggerSettings extends BasicConfiguration<ConnectionSettings, ConnectionDebuggerSettingsForm> {
    private boolean compileDependencies = true;
    private JDWPTunnelType jdwpTunnelType = JDWPTunnelType.NONE;

    private String tcpHostAddress;
    private Range<Integer> tcpPortRange = new Range<>(4000, 4999);
    private transient String jdwpHostPort;

    //reverse ssh tunnel settings
    private ReverseSshTunnelConfiguration reverseSshTunnelConfig = new ReverseSshTunnelConfiguration(this);

    private final InteractiveOptionBroker<DebuggerTypeOption> debuggerType =
            new InteractiveOptionBroker<>(
                    "debugger-type",
                    txt("msg.debugger.title.DebuggerType"),
                    txt("msg.debugger.question.SelectDebuggerType"),
                    DBDebuggerType.JDWP.isSupported() ? DebuggerTypeOption.ASK : DebuggerTypeOption.JDBC,
                    DebuggerTypeOption.JDWP,
                    DebuggerTypeOption.JDBC,
                    DebuggerTypeOption.CANCEL);

    public ConnectionDebuggerSettings(ConnectionSettings parent) {
        super(parent);
    }

    @Override
    @NotNull
    public ConnectionDebuggerSettingsForm createConfigurationEditor() {
        return new ConnectionDebuggerSettingsForm(this);
    }

    public String getConfigElementName() {
        return "debugger";
    }

    public synchronized void setJdwpHostPort(String jdwpHostPort) {
        this.jdwpHostPort = jdwpHostPort;
    }

    public synchronized String consumeJdwpHostPort() {
        String jdwpHostPort = this.jdwpHostPort;
        this.jdwpHostPort = null;
        return jdwpHostPort;
    }

    @Override
    public void readConfiguration(Element element) {
        compileDependencies = getBoolean(element, "compile-dependencies", compileDependencies);
        jdwpTunnelType = getEnum(element, "jdwp-tunnel-type", jdwpTunnelType);
        tcpHostAddress = getString(element, "tcp-host-address", tcpHostAddress);
        int tcpPortFrom = getInteger(element, "tcp-port-from", tcpPortRange.getFrom());
        int tcpPortTo = getInteger(element, "tcp-port-to", tcpPortRange.getTo());
        tcpPortRange = new Range<>(tcpPortFrom, tcpPortTo);

        debuggerType.readConfiguration(element);
        Element sshTunnelElement = element.getChild("reverse-ssh-tunnel");
        reverseSshTunnelConfig.readConfiguration(sshTunnelElement);

        // TODO remove after few subsequent releases (backward compatibility)
        boolean driverTunneling = getBoolean(element, "tcp-driver-tunneling", false);
        if (driverTunneling) jdwpTunnelType = JDWPTunnelType.TCP_DRIVER_TUNNEL;
    }

    @Override
    public void writeConfiguration(Element element) {
        setBoolean(element, "compile-dependencies", compileDependencies);
        setString(element, "tcp-host-address", tcpHostAddress);
        setInteger(element, "tcp-port-from", tcpPortRange.getFrom());
        setInteger(element, "tcp-port-to", tcpPortRange.getTo());
        setEnum(element, "jdwp-tunnel-type", jdwpTunnelType);
        debuggerType.writeConfiguration(element);

        Element sshTunnelElement = newElement(element, "reverse-ssh-tunnel");
        reverseSshTunnelConfig.writeConfiguration(sshTunnelElement);

    }
}
