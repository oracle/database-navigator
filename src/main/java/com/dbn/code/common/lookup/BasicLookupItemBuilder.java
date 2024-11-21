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

package com.dbn.code.common.lookup;

import com.dbn.code.common.completion.CodeCompletionContext;

import javax.swing.*;

public class BasicLookupItemBuilder extends LookupItemBuilder {
    private CharSequence text;
    String hint;
    Icon icon;

    public BasicLookupItemBuilder(CharSequence text, String hint, Icon icon) {
        this.text = text;
        this.hint = hint;
        this.icon = icon;
    }

    @Override
    public String getTextHint() {
        return hint;
    }

    @Override
    public boolean isBold() {
        return false;
    }

    @Override
    public CharSequence getText(CodeCompletionContext completionContext) {
        return text;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }
}