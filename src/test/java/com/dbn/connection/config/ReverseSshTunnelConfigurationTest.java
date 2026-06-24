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

package com.dbn.connection.config;

import com.intellij.openapi.options.ConfigurationException;
import org.junit.Assert;
import org.junit.Test;

public class ReverseSshTunnelConfigurationTest {
    @Test
    public void acceptsLoopbackBindHosts() throws ConfigurationException {
        ReverseSshTunnelConfiguration.validateBindHost("127.0.0.1");
        ReverseSshTunnelConfiguration.validateBindHost("localhost");
        ReverseSshTunnelConfiguration.validateBindHost("::1");
    }

    @Test
    public void rejectsWildcardBindHosts() {
        Assert.assertThrows(
                ConfigurationException.class,
                () -> ReverseSshTunnelConfiguration.validateBindHost("0.0.0.0"));
        Assert.assertThrows(
                ConfigurationException.class,
                () -> ReverseSshTunnelConfiguration.validateBindHost("::"));
    }

    @Test
    public void rejectsNonLoopbackBindHosts() {
        Assert.assertThrows(
                ConfigurationException.class,
                () -> ReverseSshTunnelConfiguration.validateBindHost("8.8.8.8"));
        Assert.assertThrows(
                ConfigurationException.class,
                () -> ReverseSshTunnelConfiguration.validateBindHost("192.0.2.1"));
    }

    @Test
    public void rejectsBlankBindHosts() {
        Assert.assertThrows(
                ConfigurationException.class,
                () -> ReverseSshTunnelConfiguration.validateBindHost(""));
        Assert.assertThrows(
                ConfigurationException.class,
                () -> ReverseSshTunnelConfiguration.validateBindHost(" "));
    }
}
