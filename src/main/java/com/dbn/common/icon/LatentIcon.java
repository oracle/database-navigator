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

package com.dbn.common.icon;

import com.dbn.common.latent.Latent;
import com.dbn.common.util.Safe;
import com.intellij.openapi.util.ScalableIcon;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

@Getter
abstract class LatentIcon implements ScalableIcon {
    private final String path;
    private final Latent<Icon> delegate = Latent.basic(() -> load());

    public LatentIcon(String path) {
        this.path = path;
    }

    @Override
    public float getScale() {
        return Safe.call(delegate(), ScalableIcon.class, i -> i.getScale(), 1F);
    }

    @Override
    public @NotNull Icon scale(float scaleFactor) {
        return Safe.call(delegate(), ScalableIcon.class, i -> i.scale(scaleFactor), delegate());
    }

    protected abstract Icon load();

    @Delegate
    private ScalableIcon delegate() {
        return (ScalableIcon) delegate.get();
    }
}
