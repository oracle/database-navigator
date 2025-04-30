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

package com.dbn.framework.batch.impl;

import com.dbn.common.util.Tagged;
import com.dbn.framework.batch.BatchElement;
import lombok.Data;

@Data
public abstract class BatchElementBase implements BatchElement, Tagged<Object> {
    private transient boolean enabled = true;
    private transient boolean selected = true;

    @Override
    public int compareTo(BatchElement o) {
        return getName().compareTo(o.getName());
    }
}
