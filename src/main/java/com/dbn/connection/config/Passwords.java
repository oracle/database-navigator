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

import com.dbn.common.util.Strings;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
@Deprecated // TODO remove after subsequent release (passwords moved to IDE keychain, no longer stored to xml configuration)
public final class Passwords {

    public static String decodePassword(String password) {
        try {
            password = Strings.isEmpty(password) ? "" : new String(Base64.getDecoder().decode(nvl(password).getBytes()));
        } catch (Exception e) {
            conditionallyLog(e);
            // password may not be encoded yet
        }

        return password;
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }

}
