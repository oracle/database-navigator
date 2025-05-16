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

package com.dbn.common.presentation.provider;

import com.dbn.common.presentation.PresentationProvider;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Named;
import com.dbn.common.util.Naming;
import com.intellij.navigation.ItemPresentation;

import javax.swing.Icon;

public class DefaultPresentationProvider implements PresentationProvider<Object> {
    @Override
    public boolean supports(Class objectType) {
        return true;
    }

    @Override
    public String getName(Object object) {
        if (object instanceof Named) {
            Named presentable = (Named) object;
            return presentable.getName();
        }

        if (object instanceof ItemPresentation) {
            ItemPresentation itemPresentation = (ItemPresentation) object;
            return itemPresentation.getPresentableText();
        }

        return object.toString();
    }

    @Override
    public String getTypeName(Object object) {
        return Naming.lowerCaseWords(object.getClass().getSimpleName());
    }

    @Override
    public String getDescription(Object object) {
        if (object instanceof Presentable) {
            Presentable presentable = (Presentable) object;
            return presentable.getDescription();
        }
        return null;
    }

    @Override
    public Icon getIcon(Object object) {
        if (object instanceof Presentable) {
            Presentable presentable = (Presentable) object;
            return presentable.getIcon();
        }

        if (object instanceof ItemPresentation) {
            ItemPresentation itemPresentation = (ItemPresentation) object;
            return itemPresentation.getIcon(false);
        }

        return null;
    }
}
