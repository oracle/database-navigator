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

package com.dbn.common.message;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Color;

public enum MessageType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    PAUSE,
    SYSTEM,
    QUESTION,
    PROCESSING,
    NEUTRAL;

    @Nullable
    public Icon getDialogIcon() {
        return switch (this) {
            case INFO -> Icons.DIALOG_INFORMATION;
            case SUCCESS -> Icons.DIALOG_SUCCESS;
            case WARNING -> Icons.DIALOG_WARNING;
            case ERROR -> Icons.DIALOG_ERROR;
            case PAUSE -> Icons.DIALOG_PAUSE;
            case QUESTION -> Icons.DIALOG_QUESTION;
            default -> null;
        };
    }

    @Nullable
    public Icon getTitleIcon() {
        return switch (this) {
            case INFO -> Icons.COMMON_INFO;
            case SUCCESS -> Icons.COMMON_STATUS_SUCCESS; // TODO
            case WARNING -> Icons.COMMON_WARNING;
            case ERROR -> Icons.COMMON_ERROR;
            case PAUSE -> Icons.COMMON_PAUSED;
            case QUESTION -> Icons.DIALOG_QUESTION; // TODO
            default -> null;
        };
    }

    public Color getBannerBackgroundColor() {
        return switch (this) {
            case INFO -> Colors.Banner.INFO_BACKGROUND_COLOR;
            case SUCCESS -> Colors.Banner.SUCCESS_BACKGROUND_COLOR;
            case WARNING -> Colors.Banner.WARNING_BACKGROUND_COLOR;
            case ERROR -> Colors.Banner.ERROR_BACKGROUND_COLOR;
            default -> Colors.getLightPanelBackground();
        };
    }

    public Color getBannerBorderColor() {
        return switch (this) {
            case INFO -> Colors.Banner.INFO_BORDER_COLOR;
            case SUCCESS -> Colors.Banner.SUCCESS_BORDER_COLOR;
            case WARNING -> Colors.Banner.WARNING_BORDER_COLOR;
            case ERROR -> Colors.Banner.ERROR_BORDER_COLOR;
            default -> Colors.getLightPanelBackground();
        };
    }



}
