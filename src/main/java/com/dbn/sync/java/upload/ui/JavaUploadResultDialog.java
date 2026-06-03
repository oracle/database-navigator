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

package com.dbn.sync.java.upload.ui;

import com.dbn.batch.DatabaseBatchManager;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.sync.java.upload.JavaUploadBatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

public class JavaUploadResultDialog extends DBNDialog<JavaUploadResultForm> {
	private final JavaUploadBatch batch;

	public JavaUploadResultDialog(JavaUploadBatch batch) {
		super(batch.getProject(), txt("msg.java.title.JavaUploadResult"), false);
		//this.setDefaultSize(380, 420);
		this.setModal(true);
		this.setAutoSize(true);
		this.batch = batch;
		init();
	}

	@Override
	protected Action[] initializeActions() {
		renameAction(getCancelAction(), txt("msg.shared.button.Close"));
		return actions(
				getCancelAction(),
				createErrorAction());
	}

	@Nullable
	private Action createErrorAction() {
		if (!batch.getMessages().hasErrors()) return null;

		return createAction("Show Errors", () -> {
			DatabaseBatchManager batchManager = DatabaseBatchManager.getInstance(getProject());
			batchManager.showErrorDialog(batch);

		});
	}

	@Override
	protected @NotNull JavaUploadResultForm createForm() {
		return new JavaUploadResultForm(this, batch);
	}
}
