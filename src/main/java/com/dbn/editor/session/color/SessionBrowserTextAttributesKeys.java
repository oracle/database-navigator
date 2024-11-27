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

package com.dbn.editor.session.color;

import com.intellij.openapi.editor.colors.TextAttributesKey;

public interface SessionBrowserTextAttributesKeys {
    TextAttributesKey DEFAULT_ACTIVE_SESSION    = TextAttributesKey.createTextAttributesKey("DBNavigator.DefaultTextAttributes.SessionBrowser.ActiveSession");
    TextAttributesKey DEFAULT_INACTIVE_SESSION  = TextAttributesKey.createTextAttributesKey("DBNavigator.DefaultTextAttributes.SessionBrowser.InactiveSession");
    TextAttributesKey DEFAULT_CACHED_SESSION    = TextAttributesKey.createTextAttributesKey("DBNavigator.DefaultTextAttributes.SessionBrowser.CachedSession");
    TextAttributesKey DEFAULT_SNIPED_SESSION    = TextAttributesKey.createTextAttributesKey("DBNavigator.DefaultTextAttributes.SessionBrowser.SnipedSession");
    TextAttributesKey DEFAULT_KILLED_SESSION    = TextAttributesKey.createTextAttributesKey("DBNavigator.DefaultTextAttributes.SessionBrowser.KilledSession");

    TextAttributesKey ACTIVE_SESSION   = TextAttributesKey.createTextAttributesKey("DBNavigator.TextAttributes.SessionBrowser.ActiveSession",   DEFAULT_ACTIVE_SESSION);
    TextAttributesKey INACTIVE_SESSION = TextAttributesKey.createTextAttributesKey("DBNavigator.TextAttributes.SessionBrowser.InactiveSession", DEFAULT_INACTIVE_SESSION);
    TextAttributesKey CACHED_SESSION   = TextAttributesKey.createTextAttributesKey("DBNavigator.TextAttributes.SessionBrowser.CachedSession",   DEFAULT_CACHED_SESSION);
    TextAttributesKey SNIPED_SESSION   = TextAttributesKey.createTextAttributesKey("DBNavigator.TextAttributes.SessionBrowser.SnipedSession",   DEFAULT_SNIPED_SESSION);
    TextAttributesKey KILLED_SESSION   = TextAttributesKey.createTextAttributesKey("DBNavigator.TextAttributes.SessionBrowser.KilledSession",   DEFAULT_KILLED_SESSION);
}

