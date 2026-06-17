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

package com.dbn.common.ui.util;

import com.dbn.common.util.Chars;
import com.dbn.common.util.Commons;
import lombok.experimental.UtilityClass;

import javax.swing.JPasswordField;

import static com.dbn.common.ui.util.ClientProperty.PASSWORD_FIELD_STATE;
import static com.dbn.common.ui.util.TextFields.onTextChange;

/**
 * Utilities for binding persisted password values to {@link JPasswordField} controls without
 * copying the real secret into an immutable {@link String}.
 * <p>
 * {@link #setPassword(JPasswordField, char[])} stores the configured password as client state
 * and displays a placeholder rounded up to a greater multiple of four characters when a password
 * exists. If the user does not edit the field, {@link #getPassword(JPasswordField, char[])} returns
 * the configured password instead of the placeholder text. Once the user edits the field, the field
 * value becomes authoritative.
 */
@UtilityClass
public class PasswordFields {
    private static final char PASSWORD_PLACEHOLDER_CHAR = '*';
    private static final int PLACEHOLDER_LENGTH_BUCKET = 4;
    private static final int MAX_PLACEHOLDER_LENGTH = 60;

    /**
     * Binds the configured password to the field and displays a placeholder rounded up to a greater
     * multiple of four characters for non-empty passwords. Use this from form reset/load code
     * instead of {@link JPasswordField#setText(String)}.
     *
     * @param textComponent the password field to bind
     * @param password the configured password, or {@code null} / empty when no password is set
     */
    public static void setPassword(JPasswordField textComponent, char[] password) {
        if (textComponent == null) return;

        PasswordFieldState state = getPasswordFieldState(textComponent);
        state.defaultPassword = password;
        state.modified = false;
        state.updating = true;
        try {
            textComponent.setText(Chars.isEmpty(password) ? "" : getPasswordPlaceholder(password));
        } finally {
            state.updating = false;
        }
    }

    /**
     * Returns the user-entered password, or the password previously bound with
     * {@link #setPassword(JPasswordField, char[])} if the field was not edited.
     *
     * @param textComponent the password field to read
     * @return the effective password value, possibly {@code null}
     */
    public static char[] getPassword(JPasswordField textComponent) {
        return getPassword(textComponent, null);
    }

    /**
     * Returns the user-entered password, or the password previously bound with
     * {@link #setPassword(JPasswordField, char[])} if the field was not edited. The supplied default
     * is used when the field has no bound state, which is useful while applying form changes back to
     * the same configuration object.
     *
     * @param textComponent the password field to read
     * @param defaultPassword fallback password when no bound value exists
     * @return the effective password value, possibly {@code null}
     */
    public static char[] getPassword(JPasswordField textComponent, char[] defaultPassword) {
        if (textComponent == null) return defaultPassword;

        PasswordFieldState state = PASSWORD_FIELD_STATE.get(textComponent);
        if (state != null && !state.modified) {
            return state.defaultPassword == null ? defaultPassword : state.defaultPassword;
        }
        return textComponent.getPassword();
    }

    /**
     * Checks whether the user explicitly changed the field value compared with the configured
     * password. A field that only contains the placeholder installed by
     * {@link #setPassword(JPasswordField, char[])} is treated as unchanged.
     *
     * @param textComponent the password field to inspect
     * @param defaultPassword the configured password to compare with edited field contents
     * @return {@code true} when the field was edited and differs from the configured password
     */
    public static boolean isPasswordChanged(JPasswordField textComponent, char[] defaultPassword) {
        if (textComponent == null) return false;

        PasswordFieldState state = PASSWORD_FIELD_STATE.get(textComponent);
        if (state != null && !state.modified) return false;

        return !Commons.matchArrays(defaultPassword, textComponent.getPassword());
    }

    /**
     * Creates a display-only password placeholder whose length is close to, but not exactly,
     * the configured password length.
     *
     * @param password the password to represent
     * @return an empty string for empty passwords, otherwise a placeholder rounded up to a greater
     * multiple of four characters and capped at sixty characters
     */
    public static String getPasswordPlaceholder(char[] password) {
        return getPasswordPlaceholder(Chars.isEmpty(password) ? 0 : password.length);
    }

    /**
     * Creates a display-only password placeholder whose length is close to, but not exactly,
     * the configured password length.
     *
     * @param length the password length to represent
     * @return an empty string for empty passwords, otherwise a placeholder rounded up to a greater
     * multiple of four characters and capped at sixty characters
     */
    public static String getPasswordPlaceholder(int length) {
        if (length <= 0) return "";

        int placeholderLength = ((length + PLACEHOLDER_LENGTH_BUCKET - 1) / PLACEHOLDER_LENGTH_BUCKET) * PLACEHOLDER_LENGTH_BUCKET;
        if (placeholderLength == length) {
            placeholderLength += PLACEHOLDER_LENGTH_BUCKET;
        }
        placeholderLength = Math.min(placeholderLength, MAX_PLACEHOLDER_LENGTH);
        return String.valueOf(PASSWORD_PLACEHOLDER_CHAR).repeat(placeholderLength);
    }

    private static PasswordFieldState getPasswordFieldState(JPasswordField textComponent) {
        PasswordFieldState state = PASSWORD_FIELD_STATE.get(textComponent);
        if (state != null) return state;

        state = new PasswordFieldState();
        PasswordFieldState passwordFieldState = state;
        PASSWORD_FIELD_STATE.set(textComponent, passwordFieldState);
        onTextChange(textComponent, e -> {
            if (!passwordFieldState.updating) {
                passwordFieldState.modified = true;
            }
        });
        return state;
    }

    private static class PasswordFieldState {
        private char[] defaultPassword;
        private boolean modified;
        private boolean updating;
    }
}
