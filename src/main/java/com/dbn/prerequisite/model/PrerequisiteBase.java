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

package com.dbn.prerequisite.model;

import com.dbn.prerequisite.definition.PrerequisiteDefinition;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
public class PrerequisiteBase implements Prerequisite {
    @Delegate
    private final PrerequisiteDefinition definition;
    private PrerequisiteStatus status = PrerequisiteStatus.NEW;
    private String statusMessage;
    private Exception statusException;

    public PrerequisiteBase(PrerequisiteDefinition definition) {
        this.definition = definition;
    }
}
