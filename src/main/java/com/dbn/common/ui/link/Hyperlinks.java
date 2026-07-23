/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.ui.link;

import com.intellij.openapi.util.NlsContexts.LinkLabel;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.HyperlinkLabel;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.util.function.Consumer;

import static com.dbn.common.ui.util.ClientProperty.NON_DISABLEABLE;

@UtilityClass
public class Hyperlinks {
    public static void initHyperlink(
            @NotNull HyperlinkLabel hyperlink,
            @LinkLabel String text,
            @Nullable @NonNls String target) {
        NON_DISABLEABLE.set(hyperlink, true);
        hyperlink.setHyperlinkText(text);
        hyperlink.setHyperlinkTarget(target);
        hyperlink.setToolTipText(target);
    }

    public static void initHyperlink(
            @NotNull HyperlinkLabel hyperlink,
            @LinkLabel String text,
            @NotNull Runnable action) {
        initHyperlink(hyperlink, text, (String) null);
        onHyperlinkAccess(hyperlink, e -> action.run());
    }

    public static void onHyperlinkAccess(HyperlinkLabel hyperlinkLabel, Consumer<HyperlinkEvent> action) {
        hyperlinkLabel.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(@NotNull HyperlinkEvent e) {
                action.accept(e);
            }
        });

    }
}
