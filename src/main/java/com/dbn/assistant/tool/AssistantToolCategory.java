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

package com.dbn.assistant.tool;

import com.dbn.common.constant.Constant;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum AssistantToolCategory implements Constant<AssistantToolCategory> {
    EXTERNAL(
            txt("app.assistant.const.AssistantToolCategory_EXTERNAL"),
            txt("app.assistant.hint.AssistantToolCategory_EXTERNAL")),

    USER_INTERACTION(
            txt("app.assistant.const.AssistantToolCategory_USER_INTERACTION"),
            txt("app.assistant.hint.AssistantToolCategory_USER_INTERACTION")),

    CONFIG_INFO_PROVIDER(
            txt("app.assistant.const.AssistantToolCategory_CONFIG_INFO_PROVIDER"),
            txt("app.assistant.hint.AssistantToolCategory_CONFIG_INFO_PROVIDER")),

    METADATA_PROVIDER(
            txt("app.assistant.const.AssistantToolCategory_METADATA_PROVIDER"),
            txt("app.assistant.hint.AssistantToolCategory_METADATA_PROVIDER")),

    SOURCE_CODE_PROVIDER(
            txt("app.assistant.const.AssistantToolCategory_SOURCE_CODE_PROVIDER"),
            txt("app.assistant.hint.AssistantToolCategory_SOURCE_CODE_PROVIDER")),

    DATA_PROVIDER(
            txt("app.assistant.const.AssistantToolCategory_DATA_PROVIDER"),
            txt("app.assistant.hint.AssistantToolCategory_DATA_PROVIDER")),

    ACTION_INVOKER(
            txt("app.assistant.const.AssistantToolCategory_ACTION_INVOKER"),
            txt("app.assistant.hint.AssistantToolCategory_ACTION_INVOKER")),

    IDE_ACTION_INVOKER(
            txt("app.assistant.const.AssistantToolCategory_IDE_ACTION_INVOKER"),
            txt("app.assistant.hint.AssistantToolCategory_IDE_ACTION_INVOKER")),
    ;

    private final String description;
    private final String name;

    AssistantToolCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }

}
