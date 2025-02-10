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

package com.dbn.execution.statement;

import com.dbn.common.compatibility.Workaround;
import com.dbn.execution.statement.action.StatementGutterAction;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Objects;

import static com.dbn.common.util.Traces.isCalledThroughClass;

public class StatementGutterRenderer extends GutterIconRenderer {
    private final StatementGutterAction action;
    private final int hashCode;

    public StatementGutterRenderer(ExecutablePsiElement executablePsiElement) {
        this.action = new StatementGutterAction(executablePsiElement);
        hashCode = Objects.hashCode(executablePsiElement);
    }

    @Override
    @NotNull
    public Icon getIcon() {
        return action.getIcon();
    }

    @Override
    public boolean isNavigateAction() {
        return true;
    }

    @Override
    @Nullable
    @Workaround // TODO workaround for Idea 15 bug (showing gutter actions as intentions)
    public AnAction getClickAction() {
        return isCalledThroughClass(ShowIntentionsPass.class, 20) ? null : action;
    }

    @Override
    @Nullable
    public String getTooltipText() {
        return action.getTooltipText();
    }

    @NotNull
    @Override
    public Alignment getAlignment() {
        return Alignment.RIGHT;
    }

    @Override
    public boolean equals(Object o) {
        // prevent double gutter actions
        if (o == this) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatementGutterRenderer that = (StatementGutterRenderer) o;
        return Objects.equals(
                this.action.getPsiElement(),
                that.action.getPsiElement());
    }

    @Override
    public int hashCode() {
        // prevent double gutter actions
        return hashCode;
    }
}
