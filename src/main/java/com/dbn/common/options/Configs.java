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

package com.dbn.common.options;

import com.intellij.openapi.options.ConfigurationException;
import lombok.experimental.UtilityClass;

/**
 * Utility class for handling configuration issues and related exceptions.
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class Configs {

    /**
     * Throws a {@link ConfigurationException} with the specified message.
     *
     * @param message the detail message expressing the problem with the configuration data
     * @throws ConfigurationException if a configuration error occurs
     */
    public static void fail(String message) throws ConfigurationException {
        throw new ConfigurationException(message);
    }
}
