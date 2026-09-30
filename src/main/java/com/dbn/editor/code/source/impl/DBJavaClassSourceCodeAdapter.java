package com.dbn.editor.code.source.impl;

import com.dbn.common.exception.Exceptions;
import com.dbn.common.file.FileTypes;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBJavaClass;
import com.intellij.openapi.fileTypes.BinaryFileDecompiler;
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.common.util.Unsafe.warned;
import static com.dbn.database.common.DatabaseContentLimits.checkJavaBinaryLength;
import static com.dbn.object.type.DBObjectType.JAVA_CLASS;

public class DBJavaClassSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBJavaClass> {
    public DBJavaClassSourceCodeAdapter() { super(JAVA_CLASS); }

    @Override
    public ResultSet loadSourceCode(DBJavaClass javaClass, DBContentType contentType, DBNConnection connection) throws SQLException {
        return javaClass.getMetadataInterface().loadObjectSourceCode(
                javaClass.getSchemaName(),
                javaClass.getName(),
                "JAVA SOURCE",
                connection);
    }

    @Override
    public ResultSet loadReadonlySourceCode(DBJavaClass javaClass, DBContentType contentType, DBNConnection connection) throws SQLException {
        CharSequence sourceCode = loadDecompiledCode(javaClass, connection);
        return createSourceCodeResultSet(sourceCode.isEmpty() ? null : sourceCode.toString());
    }

    private static CharSequence loadDecompiledCode(DBJavaClass javaClass, DBNConnection connection) throws SQLException {
        File tempFile = null;
        try {
            byte[] bytes = javaClass.getMetadataInterface().loadJavaBinaryCode(
                    javaClass.getSchemaName(),
                    javaClass.getName(),
                    connection);
            if (bytes == null) return "";
            checkJavaBinaryLength(bytes.length);

            tempFile = FileUtil.createTempFile(javaClass.getName(), ".class");
            Files.write(tempFile.toPath(), bytes);

            BinaryFileTypeDecompilers decompilers = BinaryFileTypeDecompilers.getInstance();

            FileType classFileType = FileTypes.getClassFileType();
            BinaryFileDecompiler decompiler = decompilers.forFileType(classFileType);
            if (decompiler == null) return "";

            LocalFileSystem fileSystem = LocalFileSystem.getInstance();
            VirtualFile virtualFile = fileSystem.refreshAndFindFileByIoFile(tempFile);
            if (virtualFile == null) return "";

            return decompiler.decompile(virtualFile);
        } catch (Exception e) {
            throw Exceptions.toSqlException(e);
        } finally {
            if (tempFile != null) {
                Path tempFilePath = tempFile.toPath();
                warned(() -> Files.delete(tempFilePath));
            }
        }
    }

    @Override
    public void saveSourceCode(DBJavaClass javaClass, DBContentType contentType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        var javaInterface = javaClass.getJavaInterface();
        javaInterface.updateJavaSource(
                javaClass.getSchemaName(true),
                javaClass.getName(true),
                newCode.getBytes(),
                connection);
        javaInterface.compileJavaClass(
                javaClass.getSchemaName(true),
                javaClass.getName(true),
                connection);
    }
}
