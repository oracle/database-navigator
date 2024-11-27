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

package com.dbn.object.common.status;

import com.dbn.common.property.PropertyHolderBase;
import com.dbn.editor.DBContentType;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class DBObjectStatusHolder {
    private final DBContentType mainContentType;
    private Entry[] statusEntries;

    public DBObjectStatusHolder(DBContentType mainContentType) {
        this.mainContentType = mainContentType;
    }

    private Entry ensure(DBContentType contentType) {
        Entry statusEntry = get(contentType);
        if (statusEntry == null) {
            synchronized (this) {
                statusEntry = get(contentType);
                if (statusEntry == null) {
                    if (statusEntries == null) {
                        statusEntries = new Entry[1];
                        statusEntry = new Entry(contentType);
                        statusEntries[0] = statusEntry;
                    } else {
                        statusEntry = get(contentType);
                        if (statusEntry == null) {
                            int currentSize = this.statusEntries.length;
                            Entry[] statusEntries = new Entry[currentSize + 1];
                            System.arraycopy(this.statusEntries, 0, statusEntries, 0, currentSize);
                            statusEntry = new Entry(contentType);
                            statusEntries[currentSize] = statusEntry;
                            this.statusEntries = statusEntries;
                        }
                    }
                }
            }
        }
        return statusEntry;
    }

    @Nullable
    private Entry get(DBContentType contentType) {
        if (statusEntries != null) {
            for (Entry statusEntry : statusEntries) {
                if (statusEntry.getContentType() == contentType) {
                    return statusEntry;
                }
            }
        }
        return null;
    }


    public boolean set(DBContentType contentType, DBObjectStatus status, boolean value) { Entry statusEntry = ensure(contentType);
        return statusEntry.set(status, value);
    }

    public boolean set(DBObjectStatus status, boolean value) {
        DBContentType[] subContentTypes = mainContentType.getSubContentTypes();
        if (subContentTypes.length > 0) {
            boolean hasChanged = false;
            for (DBContentType contentType : subContentTypes) {
                if (set(contentType, status, value)) {
                    hasChanged = true;
                }
            }
            return hasChanged;
        } else {
            return set(mainContentType, status, value);
        }
    }

    public boolean is(DBObjectStatus status) {
        DBContentType[] subContentTypes = mainContentType.getSubContentTypes();
        if (subContentTypes.length > 0) {
            for (DBContentType contentType : subContentTypes) {
                if (status.isPropagable()) {
                    if (!is(contentType, status)) return false;
                } else {
                    if (is(contentType, status)) return true;
                }
            }
            return status.isPropagable();
        } else {
            return is(mainContentType, status);
        }
    }

    public boolean isNot(DBObjectStatus status) {
        return !is(status);
    }

    public boolean is(DBContentType contentType, DBObjectStatus status) {
        Entry statusEntry = get(contentType);
        return statusEntry == null ?
                status.getDefaultValue() :
                statusEntry.is(status);
    }

    public boolean isNot(DBContentType contentType, DBObjectStatus status) {
        return !is(contentType, status);
    }

    @Getter
    private static class Entry extends PropertyHolderBase.IntStore<DBObjectStatus> {
        private final DBContentType contentType;

        @Override
        protected DBObjectStatus[] properties() {
            return DBObjectStatus.VALUES;
        }

        Entry(DBContentType contentType) {
            this.contentType = contentType;
        }
    }
}
