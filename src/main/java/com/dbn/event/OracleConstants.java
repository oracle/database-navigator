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

package com.dbn.event;

import org.jetbrains.annotations.NonNls;

@NonNls
public class OracleConstants {
  public static final String DCN_NOTIFY_ROWIDS = "DCN_NOTIFY_ROWIDS";
  public static final String DCN_CLIENT_INIT_CONNECTION = "DCN_CLIENT_INIT_CONNECTION";
  public static final String DCN_QOS_RELIABLE = "NTF_QOS_RELIABLE";

  public static final String DCN_IGNORE_INSERTOP = "DCN_IGNORE_INSERTOP";
  public static final String DCN_IGNORE_UPDATEOP = "DCN_IGNORE_UPDATEOP";
  public static final String DCN_IGNORE_DELETEOP = "DCN_IGNORE_DELETEOP";

  public static final int DCN_NOTIFY_INSERTOP = 2;
  public static final int DCN_NOTIFY_UPDATEOP = 4;
  public static final int DCN_NOTIFY_DELETEOP = 8;

}
