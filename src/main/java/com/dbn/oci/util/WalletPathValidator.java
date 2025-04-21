/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.oci.util;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class WalletPathValidator  {
  public enum WalletValidationResult {
    VALID_EMPTY_LOCATION,      // Folder is empty and can be used for a new wallet
    VALID_EXISTING_WALLET,     // Folder contains a valid wallet
    INVALID_LOCATION           // Folder is invalid for wallet usage
  }


  // Required files for a valid wallet
  private static final Set<String> REQUIRED_FILES = Set.of(
          "cwallet.sso",
          "ewallet.p12",
          "ewallet.pem",
          "keystore.jks",
          "ojdbc.properties",
          "sqlnet.ora",
          "tnsnames.ora",
          "truststore.jks",
          "README"
  );

  public static WalletValidationResult validateWalletLocation(String path,String defaultPath) {
    File folder = new File(path);
    if (!folder.exists()) {
      // new wallet location
      return WalletValidationResult.VALID_EMPTY_LOCATION;
    }
    // Check if the path is not a directory
    if (!folder.isDirectory()) {
      return WalletValidationResult.INVALID_LOCATION;
    }

    // List all files and directories in the folder
    File[] files = folder.listFiles();
    if (files == null || files.length == 0) {
      // Folder is empty
      return WalletValidationResult.VALID_EMPTY_LOCATION;
    }

    // Check if the folder contains any subdirectories
    for (File file : files) {
      if (file.isDirectory()) {
        // If there is any subdirectory, the location is invalid
        return WalletValidationResult.INVALID_LOCATION;
      }
    }

    // Check if the folder contains a valid wallet
    if (containsValidWallet(files)) {
      return WalletValidationResult.VALID_EXISTING_WALLET;
    }

    // Folder is neither empty nor contains a valid wallet
    return WalletValidationResult.INVALID_LOCATION;
  }

  private static boolean containsValidWallet(File[] files) {
    // Collect the names of all files in the folder
    Set<String> fileNames = new HashSet<>();
    for (File file : files) {
      if (file.isFile()) {
        fileNames.add(file.getName());
      }
    }

    // Check if all required wallet files are present
    return fileNames.containsAll(REQUIRED_FILES);
  }
}
