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

package com.dbn.common.util;

import com.dbn.common.constant.PseudoConstant;

import java.util.UUID;

public final class TransientId extends PseudoConstant<TransientId> {

    private TransientId(String id) {
        super(id);
    }

    public static TransientId get(String id) {
        return PseudoConstant.get(TransientId.class, id);
    }

    public static TransientId create() {
        return new TransientId(UUID.randomUUID().toString());
    }
}
