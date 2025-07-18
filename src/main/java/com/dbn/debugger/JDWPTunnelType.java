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

package com.dbn.debugger;

import com.dbn.common.ui.Presentable;
import lombok.Getter;

@Getter
public enum JDWPTunnelType implements Presentable {
    TCP_DRIVER_TUNNEL ("TCP driver tunnel"),   // tunnel thru jdbc driver
    SSH_REVERSE_TUNNEL("SSH reverse tunnel"),  // reverse SSH tunnel from databsae server to local
    NONE("None");                              // no tunnel, database is expected to have dirrect access to developer workspace

    private final String name;

    JDWPTunnelType(String name) {
        this.name = name;
    }
}
