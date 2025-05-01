package com.dbn.events;

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
