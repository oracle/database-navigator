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

import com.dbn.editor.DBContentType;
import com.dbn.object.common.status.DBObjectStatus.Propagation;

public class DBObjectStatusHolder {
    private static final int STATUS_MASK = 0xFFFF;
    private static final int DEFAULT_STATUS_MASK = defaultStatusMask();

    private final DBContentType mainContentType;
    private int statusBits;

    public DBObjectStatusHolder(DBContentType mainContentType) {
        DBContentType[] subContentTypes = mainContentType.getSubContentTypes();
        if (subContentTypes.length > 2) {
            throw new IllegalArgumentException("Status holder supports at most two sub-content types");
        }

        this.mainContentType = mainContentType;
        this.statusBits = DEFAULT_STATUS_MASK;
        if (subContentTypes.length == 2) {
            this.statusBits |= DEFAULT_STATUS_MASK << Short.SIZE;
        }
    }

    public synchronized boolean set(DBContentType contentType, DBObjectStatus status, boolean value) {
        if (contentType == mainContentType && contentType.isBundle()) {
            return set(status, value);
        }

        int slot = requireSlot(contentType);
        int mask = status.maskOn() & STATUS_MASK;
        int shiftedMask = mask << (slot * Short.SIZE);
        boolean currentValue = (statusBits & shiftedMask) != 0;
        if (currentValue == value) return false;

        statusBits = value ?
                statusBits | shiftedMask :
                statusBits & ~shiftedMask;
        return true;
    }

    public synchronized boolean set(DBObjectStatus status, boolean value) {
        DBContentType[] subContentTypes = mainContentType.getSubContentTypes();
        if (subContentTypes.length > 0) {
            boolean hasChanged = false;
            for (DBContentType contentType : subContentTypes) {
                if (set(contentType, status, value)) {
                    hasChanged = true;
                }
            }
            return hasChanged;
        }
        return set(mainContentType, status, value);
    }

    public synchronized boolean is(DBObjectStatus status) {
        DBContentType[] subContentTypes = mainContentType.getSubContentTypes();
        Propagation propagation = status.getPropagation();

        if (subContentTypes.length > 0) {
            if (propagation != Propagation.NONE) {
                for (DBContentType contentType : subContentTypes) {
                    boolean statusMatch = is(contentType, status);
                    if (propagation == Propagation.ANY) {
                        // if any of the subcontents matches the status -> true
                        if (statusMatch) return true;

                    } else if (propagation == Propagation.ALL) {
                        // if at least one of the subcontents does not match the status -> false
                        if (!statusMatch) return false;
                    }
                }
                return status.getDefaultValue();
            }

            return status.getDefaultValue();
        }

        return is(mainContentType, status);
    }

    public boolean isNot(DBObjectStatus status) {
        return !is(status);
    }

    public synchronized boolean is(DBContentType contentType, DBObjectStatus status) {
        if (contentType == mainContentType && contentType.isBundle()) {
            return is(status);
        }

        int slot = findSlot(contentType);
        if (slot < 0) return status.getDefaultValue();

        int mask = status.maskOn() & STATUS_MASK;
        return (statusBits & (mask << (slot * Short.SIZE))) != 0;
    }

    public boolean isNot(DBContentType contentType, DBObjectStatus status) {
        return !is(contentType, status);
    }

    private int requireSlot(DBContentType contentType) {
        int slot = findSlot(contentType);
        if (slot < 0) {
            throw new IllegalArgumentException(
                    "Content type " + contentType + " is not supported by " + mainContentType);
        }
        return slot;
    }

    private int findSlot(DBContentType contentType) {
        DBContentType[] subContentTypes = mainContentType.getSubContentTypes();
        if (subContentTypes.length == 0) {
            return mainContentType == contentType ? 0 : -1;
        }

        for (int i = 0; i < subContentTypes.length; i++) {
            if (subContentTypes[i] == contentType) return i;
        }
        return -1;
    }

    private static int defaultStatusMask() {
        int mask = 0;
        for (DBObjectStatus status : DBObjectStatus.VALUES) {
            if (status.getDefaultValue()) {
                mask |= status.maskOn() & STATUS_MASK;
            }
        }
        return mask;
    }
}
