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

package com.dbn.common.dispose;

import com.dbn.common.latent.Latent;
import com.dbn.common.latent.Loader;
import com.dbn.common.options.Configuration;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.util.Unsafe;
import com.dbn.connection.DatabaseEntity;
import com.dbn.data.model.DataModel;
import com.intellij.openapi.components.NamedComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.ReflectionUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.script.ScriptEngine;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public final class Nullifier {

    private static final Map<Class<?>, List<Field>> NULLIFIABLE_FIELDS = new ConcurrentHashMap<>(100);
    private static final Class[] NULLIFIABLE_CLASSES = new Class[] {
            Map.class,
            Collection.class,
            Latent.class,
            Loader.class,
            Editor.class,
            Document.class,
            PsiElement.class,
            VirtualFile.class,
            Configuration.class,
            DatabaseEntity.class,
            AutoCloseable.class,
            NamedComponent.class,
            EventListener.class,
            DataModel.class,
            TreePath.class,
            EventListener.class,
            DBNComponent.class,
            ScriptEngine.class
    };

    public static void clearCollection(Collection<?> collection) {
        if (collection == null) return;
        if (collection.isEmpty()) return;

        boolean cleared = Unsafe.silent(collection, c -> c.clear());
        if (!cleared && collection instanceof List) {
            Unsafe.silent(collection, c -> nullify((List<?>) c));
        }
    }

    public static void clearMap(Map<?, ?> map) {
        Unsafe.silent(map, m -> m.clear());
    }

    private static void nullify(List<?> collection) {
        for (int i = 0; i<collection.size(); i++) {
            collection.set(i, null);
        }
    }

    public static void nullify(Object object) {
        if (object instanceof Component) return;

        BackgroundDisposer.queue(() -> nullifyFields(object));
    }

    private static void nullifyFields(Object object) {
        List<Field> fields = nullifiableFields(object.getClass());
        for (Field field : fields) {
            try {
                nullifyField(object, field);
            } catch (UnsupportedOperationException ignore) {
            } catch (Throwable e) {
                log.error("Failed to nullify field", e);
            }
        }
    }

    private static void nullifyField(Object object, Field field) throws IllegalAccessException {
        field.setAccessible(true);
        Object fieldValue = field.get(object);
        if (fieldValue == null) return;

        if (fieldValue instanceof Collection<?>) {
            Collection collection = (Collection) fieldValue;
            clearCollection(collection);
        } else if (fieldValue instanceof Map) {
            Map map = (Map) fieldValue;
            clearMap(map);
        } else if (fieldValue instanceof Latent){
            Latent latent = (Latent) fieldValue;
            latent.reset();
            nullify(latent);
        } else {
            field.set(object, null);
        }
    }

    private static List<Field> nullifiableFields(Class clazz) {
        return NULLIFIABLE_FIELDS.computeIfAbsent(clazz, c ->
                ReflectionUtil.
                        collectFields(c).
                        stream().
                        filter(field -> isNullifiable(field)).
                        collect(Collectors.toList()));
    }

    private static boolean isNullifiable(Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) ||
                //Modifier.isFinal(modifiers) ||
                Modifier.isNative(modifiers)) {
            return false;
        }

        Sticky sticky = field.getAnnotation(Sticky.class);
        if (sticky != null) return false;

        for (Class<?> nullifiableClass : NULLIFIABLE_CLASSES) {
            Class<?> fieldType = field.getType();
            if (nullifiableClass.isAssignableFrom(fieldType)) return true;
        }
        return false;
    }
}
