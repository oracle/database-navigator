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

package com.dbn.language.common;

/**
 * Compact fixed-size token sequence key. It avoids the object and backing-array overhead of
 * short ArrayList keys in hot parser caches.
 */
public final class TokenChain {
    public int size;
    private int hashCode;
    private TokenType token0;
    private TokenType token1;
    private TokenType token2;
    private TokenType token3;

    public int size() {
        return size;
    }

    public TokenType get(int index) {
        return switch (index) {
            case 0 -> token0;
            case 1 -> token1;
            case 2 -> token2;
            case 3 -> token3;
            default -> throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        };
    }

    private int calculateHashCode() {
        int result = 1;
        for (int i = 0; i < size; i++) {
            TokenType token = get(i);
            result = 31 * result + (token == null ? 0 : token.hashCode());
        }
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) return true;
        if (!(object instanceof TokenChain tokenChain)) return false;
        if (size != tokenChain.size) return false;
        for (int i = 0; i < size; i++) {
            TokenType token = get(i);
            if (token == null ? tokenChain.get(i) != null : !token.equals(tokenChain.get(i))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public void add(TokenType tokenType) {
        switch (size) {
            case 0 -> token0 = tokenType;
            case 1 -> token1 = tokenType;
            case 2 -> token2 = tokenType;
            case 3 -> token3 = tokenType;
            default -> throw new IndexOutOfBoundsException("Token chain cannot exceed 4 entries");
        }
        size++;
    }

    public TokenChain finish() {
        hashCode = calculateHashCode();
        return this;
    }
}
