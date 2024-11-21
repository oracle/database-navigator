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

import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Commons;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.HashMap;
import java.util.Map;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
class IconLoader {
    static final Map<String, Icon> REGISTRY = new HashMap<>();

    static Icon load(@NonNls String path) {
        return new LatentIcon(path) {
            @Override
            protected Icon load() {
                String path = getPath();
                log.info("Loading icon {}", path);

                return Commons.coalesce(
                        () -> findNewIcon(path),
                        () -> findSvgIcon(path),
                        () -> findIcon(path));

            }
        };
    }

    static Icon init(@NonNls String path) {
        path = adjustPath(path, true, UserInterface.isNewUI());
        return findIcon(path);
    }

    private static String adjustPath(@NonNls String path, boolean extension, boolean location) {
        if (extension) path = path.replace(".png", ".svg");
        if (location) path = path.replace("/img/", "/img/expui/");
        return path;
    }

    private static Icon findNewIcon(String path) {
        if (!UserInterface.isNewUI()) return null;
        path = adjustPath(path, true, true);
        return find(path);
    }

    private static @Nullable Icon findSvgIcon(String path) {
        String svgPath = adjustPath(path, true, false);
        return find(svgPath);
    }

    private static @Nullable Icon find(@NonNls String path) {
        try {
            Icon icon = findIcon(path);
            if (icon != null && icon.getIconWidth() > 1) return icon;
        } catch (ProcessCanceledException e) {
            conditionallyLog(e);
            throw e;
        } catch (Throwable t) {
            log.error("Failed to load icon {}", path, t);
        }
        return null;
    }

    private static @Nullable Icon findIcon(String path) {
        return com.intellij.openapi.util.IconLoader.findIcon(path, Icons.class.getClassLoader());
    }

    static Icon load(@NonNls String path, @NonNls String key) {
        Icon icon = load(path);
        REGISTRY.put(key, icon);
        return icon;
    }

}
