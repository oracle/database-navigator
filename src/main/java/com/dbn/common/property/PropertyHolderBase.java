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

package com.dbn.common.property;

import org.jetbrains.annotations.Nullable;

public abstract class PropertyHolderBase<T extends Property> implements PropertyHolder<T> {
    protected abstract T[] properties();
    protected abstract void change(T property, boolean value);

    protected void changed(T property, boolean value) {
        // no action by default
    }

    @SafeVarargs
    protected PropertyHolderBase(T ... properties) {
        for (T property : properties()) {
            if (property.implicit()) {
                set(property);
            }
        }
        if (properties != null) {
            for (T property : properties) {
                set(property);
            }
        }
    }

    @Override
    public boolean set(T property, boolean value) {
        return value ?
                set(property) :
                unset(property);
    }

    protected final boolean set(T property) {
        if (isNot(property)) {
            PropertyGroup group = property.group();
            if (group != null) {
                for (T prop : properties()) {
                    if (is(prop)) {
                        change(prop, false);
                        break;
                    }
                }
            }

            change(property, true);
            return true;
        }
        return false;
    }

    protected boolean unset(T property) {
        if (is(property)) {
            change(property, false);

            PropertyGroup group = property.group();
            if (group != null) {
                // set implicit property
                for (T prop : properties()) {
                    if (prop.group() == group && prop.implicit() && prop != property && !is(prop)) {
                        change(prop, true);
                        break;
                    }
                }
            }

            return true;
        }
        return false;
    }

    public void reset() {
        for (T property : properties()) {
            if (property.implicit()) {
                set(property);
            }
        }
    }

    public void merge(@Nullable PropertyHolder<T> source) {
        if (source != null) {
            for (T property : properties()) {
                if (source.is(property)) {
                    set(property, true);
                }
            }
        }
    }

    public void unmerge(@Nullable PropertyHolder<T> source) {
        if (source != null) {
            for (T property : properties()) {
                if (source.is(property)) {
                    set(property, false);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (T property : properties()) {
            if (is(property)) {
                if (builder.length() > 0) {
                    builder.append(" / ");
                }
                builder.append(property);
            }
        }
        return builder.toString();
    }

    public abstract static class IntStore<T extends Property.IntBase> extends PropertyHolderBase<T> {
        private volatile int computed;

        @SafeVarargs
        public IntStore(T ... properties) {
            super(properties);
        }

        public void inherit(IntStore<T> source) {
            this.computed = source.computed;
        }

        public final synchronized boolean is(T property) {
            return (computed & property.maskOn()) != 0;
        }

        protected synchronized void change(T property, boolean value) {
            this.computed = value ?
                    this.computed | property.maskOn() :
                    this.computed & property.maskOff();
            changed(property, value);
        }

        public void reset() {
            computed = 0;
            super.reset();
        }
    }

    public abstract static class LongStore<T extends Property.LongBase> extends PropertyHolderBase<T> {
        private volatile long computed;

        @SafeVarargs
        public LongStore(T ... properties) {
            super(properties);
        }

        protected void replace(LongStore<T> source) {
            this.computed = source.computed;
        }

        public final synchronized boolean is(T property) {
            return (computed & property.maskOn()) != 0;
        }

        protected synchronized void change(T property, boolean value) {
            this.computed = value ?
                    this.computed | property.maskOn() :
                    this.computed & property.maskOff();
            changed(property, value);
        }

        public void reset() {
            computed = 0;
            super.reset();
        }
    }
}
