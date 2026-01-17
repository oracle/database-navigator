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

package com.dbn.vector.model.result;

import com.dbn.common.checksum.Checksum;
import com.dbn.common.checksum.ChecksumType;
import com.dbn.common.file.util.VirtualFiles;
import com.dbn.vector.model.request.EmbeddingFileSource;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@Getter
@Setter
public class EmbeddingFileResult extends EmbeddingResult<EmbeddingFileSource> {
    private String presentableSize;
    private String fileStoreId;
    private String fileHash;
    private long fileSize;
    private Map<String, Object> metadata;

    private boolean skipped = false;

    public EmbeddingFileResult(EmbeddingFileSource source) {
        super(source);
        initSteps();
    }

    public void initSource() {
        VirtualFile file = getFile();
        this.fileSize = file.getLength();
        this.presentableSize = VirtualFiles.getPresentableFileSize(file);
        this.fileHash = Checksum.fromFileContent(VfsUtilCore.virtualToIoFile(file), ChecksumType.SHA_256);
    }

    public VirtualFile getFile() {
        return getSource().getFile();
    }

    private void initSteps() {
        steps = new ArrayList<>(Arrays.asList(
                new StepResult(PipelineStep.CHECK_CRC),
                new StepResult(PipelineStep.UPLOADING_FILE),
                new StepResult(PipelineStep.EMBED)
        ));
    }

    @NotNull
    @Override
    public String getName() {
        return getFile().getName();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return getFile().getFileType().getIcon();
    }

    @Override
    public String getIdentifier() {
        return getFile().getPath();
    }
}
