package com.dbn.vector.model;

import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.common.ui.table.DBNTableGutterModel;
import com.dbn.common.ui.table.DBNTableWithGutterModel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.vector.DatabaseVectorManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.sql.SQLException;
import java.util.List;

@Getter
public class ChunkDataModel extends DBNMutableTableModel<ChunkData> implements DBNTableWithGutterModel<ChunkData> {
  private final ConnectionRef connection;
  private final ListModel gutterModel = new DBNTableGutterModel<>(this);

  private List<ChunkData> chunks ;

  // Column identifiers
//  public static final String CHUNK_ID         = "CHUNK_ID";
  public static final String CHUNK_OFFSET            = "CHUNK_OFFSET";
  public static final String CHUNK_LENGTH         = "CHUNK_LENGTH";
  public static final String CHUNK_DATA         = "CHUNK_DATA";

  private static final String[] COLUMN_NAMES = {
//          CHUNK_ID,
          CHUNK_OFFSET,
          CHUNK_LENGTH,
          CHUNK_DATA
  };

  public ChunkDataModel(ConnectionHandler connection) {
    this.connection = ConnectionRef.of(connection);
  }

  public ConnectionHandler getConnection() {
    return ConnectionRef.ensure(connection);
  }

  @Override
  public int getRowCount() {
    if(chunks == null) return 0;
    return chunks.size();
  }

  @Override
  public int getColumnCount() {
    return COLUMN_NAMES.length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    if(chunks == null) return null;
    ChunkData reg = chunks.get(rowIndex);
    switch (columnIndex) {
      case 0: return reg.getChunk_offset();
      case 1: return reg.getChunk_length();
      case 2: return reg.getChunk_data();
      default: return "";
    }
  }

  @Override
  public String getColumnName(int column) {
    return COLUMN_NAMES[column];
  }

  @Override
  public @NotNull Class<?> getColumnClass(int columnIndex) {
    // If any column is a number, you could return Integer.class, Long.class, etc.
    // Here we assume everything is best represented as String.
    return String.class;
  }

  @Override
  public void disposeInner() {
    // Clean up if needed (e.g. unregister listeners), but no-op here.
  }

  public void refresh(ChunkConfiguration chunkConfiguration, String text, DBNConnection conn) throws SQLException {
//    ConnectionHandler connection = getConnection();
    DatabaseVectorManager databaseVectorManager = DatabaseVectorManager.getInstance(connection.ensure().getProject());
    chunks = databaseVectorManager.chunk(chunkConfiguration,text, conn);
    notifyRowChanges();
  }


  @Override
  public ListModel getListModel() {
    return gutterModel;
  }
}


