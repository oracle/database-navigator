/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.batch.impl;

import com.dbn.batch.BatchTask;
import com.dbn.common.message.Message;
import com.dbn.common.message.MessageType;
import com.dbn.common.util.Tagged;
import com.dbn.common.util.UUIDs;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BatchTaskBase implements BatchTask, Tagged<Object> {
    private final String identifier = UUIDs.compact();

    private boolean enabled = true;
    private boolean selected = true;
    private Message message;

    @Override
    public void markSuccessful(String message) {
        this.message = new Message(MessageType.SUCCESS, message);
    }

    @Override
    public void markErrored(String message) {
        this.message = new Message(MessageType.ERROR, message);
    }

    @Override
    public int compareTo(BatchTask o) {
        return getName().compareTo(o.getName());
    }
}
