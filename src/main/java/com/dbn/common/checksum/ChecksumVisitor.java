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

import com.dbn.common.lookup.Visitor;

import java.io.File;
import java.security.MessageDigest;

/**
 * Abstract implementation of a file visitor ({@link Visitor<File>}) holding an {@link MessageDigest}
 * It can be used to visit files recursively, each contributing to the update of the MessageDigest
 * The digest logic is delegated to the implementers to allow different checksum algorithms
 * (e.g. based on file content / based on file name and size)
 *
 * @author Dan Cioca (Oracle)
 */
abstract class ChecksumVisitor implements Visitor<File> {
    protected final MessageDigest digest;

    ChecksumVisitor(ChecksumType checksumType) {
        digest = checksumType.getMessageDigest();
    }

    @Override
    public void visit(File file) {
        visit(file, digest);
    }

    abstract void visit(File file, MessageDigest digest);

    String produce() {
        return Checksum.concludeDigest(digest);
    }
}
