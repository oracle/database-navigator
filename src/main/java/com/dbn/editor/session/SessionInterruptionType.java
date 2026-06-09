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

package com.dbn.editor.session;

import static com.dbn.nls.NlsResources.txt;

public enum SessionInterruptionType {
    DISCONNECT,
    TERMINATE;

    public String disconnectedAction() {
        return txt(this == TERMINATE ?
                "msg.sessions.const.DisconnectedAction_TERMINATE" :
                "msg.sessions.const.DisconnectedAction_DISCONNECT");
    }

    public String disconnectingAction() {
        return txt(this == TERMINATE ?
                "msg.sessions.const.DisconnectingAction_TERMINATE" :
                "msg.sessions.const.DisconnectingAction_DISCONNECT");
    }

    public String taskAction(int sessionCount) {
        if (this == TERMINATE) {
            return txt(sessionCount == 1 ?
                    "prc.sessions.text.KillingSessionTask" :
                    "prc.sessions.text.KillingSessionsTask");
        }
        return txt(sessionCount == 1 ?
                "prc.sessions.text.DisconnectingSessionTask" :
                "prc.sessions.text.DisconnectingSessionsTask");
    }
}
