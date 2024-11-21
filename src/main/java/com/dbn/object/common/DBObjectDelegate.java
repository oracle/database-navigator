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

package com.dbn.object.common;

import com.dbn.common.dispose.StatefulDisposable;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public final class DBObjectDelegate extends DBObjectDelegateBase implements StatefulDisposable {

    private boolean disposed;

    public DBObjectDelegate(DBObject object) {
        super(object);
    }

    @Override
    public void dispose() {
        // do not dispose the delegated object
        disposed = true;
    }

    @Override
    public void disposeInner() {
        // do not dispose the delegated object
        disposed = true;
    }

    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DBObjectDelegate) {
            DBObjectDelegate that = (DBObjectDelegate) o;

            if (this.isDisposed()) return false;
            if (that.isDisposed()) return false;

            return Objects.equals(this.delegate(), that.delegate());
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.isDisposed()) return -1;
        return delegate().hashCode();
    }
}
