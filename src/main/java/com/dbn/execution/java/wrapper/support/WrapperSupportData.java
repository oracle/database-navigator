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

package com.dbn.execution.java.wrapper.support;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class WrapperSupportData {
    private final Map<String, WrapperSupportInfo> argumentData = new HashMap<>();
    private final Map<String, WrapperSupportInfo> returnData = new HashMap<>();

    public WrapperSupportInfo getSupportInfo(String className, boolean argument) {
        return argument ?
                argumentData.get(className) :
                returnData.get(className);
    }

    public void addSupportInfo(String className, WrapperSupportInfo info, boolean argument) {
        Map<String, WrapperSupportInfo> container = argument ? argumentData : returnData;
        container.put(className, info);
    }

}
