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

import com.dbn.common.util.Files;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Formatter;

/**
 * File or folder checksum utilities
 * Main focus for these utilities is to allow verifying if the contents of given files or folders have changed
 * (practical usage: reload driver libraries if the files within the library folder have changed)
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class Checksum {

    /**
     * Single file content checksum producer
     * @param file the file to produce checksum for
     * @param type the {@link ChecksumType} to use
     * @return the calculated checksum
     */
    public static String fromFileContent(File file, ChecksumType type) {
        MessageDigest digest = type.getMessageDigest();
        updateDigest(digest, file);
        return concludeDigest(digest);
    }

    /**
     * Soft recursive file checksum producer
     * Visits all the files within the given path and produces a unique checksum for the entire package
     * The checksum computation is based on the file names and sizes
     * <br>
     *
     * @param path the path to be scanned (can be one single file or a directory)
     * @param type the {@link ChecksumType} to use
     * @return the calculated checksum
     */
    public static String fromFileAttributes(File path, ChecksumType type) {
        ChecksumVisitor visitor = new AttributeBasedVisitor(type);
        Files.visitRecursively(path, visitor);
        return visitor.produce();
    }

    /**
     * Strong but slower recursive file checksum producer
     * Visits all the files within the given path and produces a unique checksum for the entire package
     * The checksum computation is based on the file contents
     * <br>
     *
     * @param path the path to be scanned (can be one single file or a directory)
     * @param type the {@link ChecksumType} to use
     * @return the calculated checksum
     */
    public static String fromFileContents(File path, ChecksumType type) {
        ChecksumVisitor visitor = new ContentBasedVisitor(type);
        Files.visitRecursively(path, visitor);
        return visitor.produce();
    }


    private static class AttributeBasedVisitor extends ChecksumVisitor {
        AttributeBasedVisitor(ChecksumType checksumType) {
            super(checksumType);
        }

        @Override
        void visit(File file, MessageDigest digest) {
            String signature = file.isDirectory() ?
                    file.getName() :
                    file.getName() + file.length();

            digest.update(signature.getBytes());
        }
    }

    private static class ContentBasedVisitor extends AttributeBasedVisitor {
        ContentBasedVisitor(ChecksumType checksumType) {
            super(checksumType);
        }

        @Override
        void visit(File file, MessageDigest digest) {
            // digest soft attributes
            super.visit(file, digest);
            if (file.isDirectory()) return;

            // digest content
            updateDigest(digest, file);
        }
    }

    @SneakyThrows
    static void updateDigest(MessageDigest digest, File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[1024];
            int length;
            while ((length = inputStream.read(bytes)) != -1) {
                digest.update(bytes, 0, length);
            }
        }
    }

    static String concludeDigest(MessageDigest digest) {
        byte[] bytes = digest.digest();
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}
