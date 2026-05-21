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

package com.dbn.common.checksum;

import com.intellij.util.io.DigestUtil;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

import java.security.MessageDigest;
import java.util.function.Supplier;

@Getter
public enum ChecksumType {
    MD_5("MD5", "md5", () -> DigestUtil.md5()),
    SHA_1("SHA-1", "sha1", () -> DigestUtil.sha1()),
    SHA_256("SHA-256", "sha256", () -> DigestUtil.sha256()),
    SHA_512("SHA-512", "sha512", () -> DigestUtil.sha512()),
    //...
    ;

    private final String name;
    private final String extension;
    private final Supplier<MessageDigest> messageDigest;

    ChecksumType(@NonNls String name, @NonNls String extension, Supplier<MessageDigest> messageDigest) {
        this.name = name;
        this.extension = extension;
        this.messageDigest = messageDigest;
    }

    public int getHexLength() {
        return getMessageDigest().getDigestLength() * 2;
    }

    public boolean isStrong() {
        return this == SHA_256 || this == SHA_512;
    }

    public MessageDigest getMessageDigest() {
        return messageDigest.get();
    }
}
