/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.common.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

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
     * Soft recursive file checksum producer
     * Visits all the files within the given path and produces a unique checksum for the entire package
     * The checksum computation is based on the file names and sizes
     * <br>
     * @param path the path to be scanned (can be one single file or a directory)
     * @return the calculated checksum
     */
    public static String soft(File path) {
        ChecksumVisitor visitor = new Soft();
        Files.visitRecursively(path, visitor);
        return visitor.produce();
    }

    /**
     * Strong but slower recursive file checksum producer
     * Visits all the files within the given path and produces a unique checksum for the entire package
     * The checksum computation is based on the file contents
     * <br>
     * @param path the path to be scanned (can be one single file or a directory)
     * @return the calculated checksum
     */
    public static String strong(File path) {
        ChecksumVisitor visitor = new Strong();
        Files.visitRecursively(path, visitor);
        return visitor.produce();
    }


    private static class Soft extends ChecksumVisitor {
        @Override
        void visit(File file, MessageDigest digest) {
            String signature = file.isDirectory() ?
                    file.getName() :
                    file.getName() + file.length();

            digest.update(signature.getBytes());
        }
    }

    private static class Strong extends Soft {
        @Override
        @SneakyThrows
        void visit(File file, MessageDigest digest) {
            // digest soft attributes
            super.visit(file, digest);
            if (file.isDirectory()) return;

            // digest content
            try (FileInputStream inputStream = new FileInputStream(file)) {
                byte[] bytes = new byte[1024];
                int length;
                while ((length = inputStream.read(bytes)) != -1) {
                    digest.update(bytes, 0, length);
                }
            }
        }
    }
}
