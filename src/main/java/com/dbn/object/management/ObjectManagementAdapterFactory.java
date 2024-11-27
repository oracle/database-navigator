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

package com.dbn.object.management;

import com.dbn.object.common.DBObject;
import com.dbn.object.event.ObjectChangeAction;

/**
 * Object management adapter factory
 * Implementations are expected to create {@link ObjectManagementAdapter} instances for all supported {@link ObjectChangeAction} types
 * @param <T> type of the object the adapter factory is responsible for
 *
 * @author Dan Cioca (Oracle)
 */
public interface ObjectManagementAdapterFactory<T extends DBObject> {
    ObjectManagementAdapter<T> createAdapter(T object, ObjectChangeAction action);

}
