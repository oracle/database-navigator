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

package com.dbn.connection;

import com.dbn.common.ui.Presentable;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enumeration for all possible token authentication types<br>
 * See: <a href="https://docs.oracle.com/en/database/oracle/oracle-database/23/odpnt/ConnectionTokenAuthentication.html">Oracle token auth types</a>
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
@AllArgsConstructor
public enum AuthenticationTokenType implements Presentable {
    OCI_API_KEY("OCI API Key"),
    OCI_INSTANCE_PRINCIPAL("OCI Instance Principal"),
    OCI_RESOURCE_PRINCIPAL("OCI Resource Principal"),
    OCI_DELEGATION_TOKEN("OCI Delegation Token"),
    OCI_INTERACTIVE("OCI Interactive"),
    ;
    private final String name;
}
