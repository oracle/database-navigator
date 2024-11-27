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

package com.dbn.navigation.object;

import com.dbn.common.consumer.SetCollector;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.latent.Latent;
import com.dbn.common.sign.Signed;
import com.dbn.object.common.DBObject;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class DBObjectLookupData extends StatefulDisposableBase implements Signed {
    private final SetCollector<DBObject> data = SetCollector.concurrent();
    private final Latent<String[]> names = Latent.mutable(() -> data.size(), () -> buildNames());

    @Getter
    @Setter
    private int signature = -1;

    private String[] buildNames() {
        checkDisposed();
        return data.elements().
                stream().
                sorted().
                map(object -> object.getName()).
                distinct().
                toArray(String[]::new);
    }

    public String[] names() {
        return names.get();
    }

    public Object[] elements(String name) {
        checkDisposed();
        return data.elements().
                stream().
                filter(object -> Objects.equals(object.getName(), name)).
                sorted().
                toArray();
    }

    public void accept(DBObject object) {
        checkDisposed();
        data.accept(object);
    }


    @Override
    public void disposeInner() {
        data.clear();
        names.reset();
    }
}
