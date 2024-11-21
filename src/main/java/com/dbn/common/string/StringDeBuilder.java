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

package com.dbn.common.string;

import com.dbn.common.util.Strings;

/**
 * Double-Ended StringBuilder optimised for prepends (insert at index 0)
 * Uses a thread local pre-sized empty {@link StringBuilder} starting appending and prepending in the middle
 */
public final class StringDeBuilder {
    private static final int playgroundSize = 6000;
    private static final ThreadLocal<StringBuilder> delegate = new ThreadLocal<>();

    private int left;
    private int right;

    public StringDeBuilder() {
        this.left = playgroundSize / 2;
        this.right = left;
    }

    private StringBuilder delegate() {
        StringBuilder delegate = StringDeBuilder.delegate.get();
        if (delegate == null) {
            delegate = new StringBuilder(Strings.repeat(" ", playgroundSize));
            StringDeBuilder.delegate.set(delegate);
        }
        return delegate;
    }

    public int length() {
        return right - left;
    }

    @Override
    public String toString() {
        return delegate().substring(left, right);
    }



    public void append(String string) {
        int length = string.length();
        delegate().replace(right, right + length, string);
        right += length;
    }

    public void prepend(String string) {
        int length = string.length();
        delegate().replace(left - length, left, string);
        left -= length;
    }

    public void append(char chr) {
        delegate().setCharAt(right, chr);
        right++;
    }

    public void prepend(char chr) {
        delegate().setCharAt(left - 1, chr);
        left--;
    }


    public void append(short shrt) {
        append(Short.toString(shrt));
    }

    public void prepend(short shrt) {
        prepend(Short.toString(shrt));
    }

    public void append(int integer) {
        append(Integer.toString(integer));
    }

    public void prepend(int integer) {
        prepend(Integer.toString(integer));
    }
}
