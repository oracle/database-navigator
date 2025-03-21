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

import java.security.MessageDigest;
import java.util.function.Supplier;

public enum ChecksumType {
    MD_5(() -> DigestUtil.md5()),
    SHA_1(() -> DigestUtil.sha1()),
    SHA_256(() -> DigestUtil.sha256()),
    //...
    ;

    private final Supplier<MessageDigest> messageDigest;

    ChecksumType(Supplier<MessageDigest> messageDigest) {
        this.messageDigest = messageDigest;
    }

    public MessageDigest getMessageDigest() {
        return messageDigest.get();
    }
}
