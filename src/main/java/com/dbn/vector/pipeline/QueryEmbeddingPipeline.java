package com.dbn.vector.pipeline;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.ExecutableBundlePsiElement;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.vector.model.VectorEmbeddingContext;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.dbn.vector.model.request.EmbeddingSourceQueries;
import com.dbn.vector.model.request.EmbeddingSourceQuery;
import com.dbn.vector.model.result.EmbeddingQueryResult;
import com.dbn.vector.model.result.PipelineStep;
import com.dbn.vector.model.result.StepResult;
import com.dbn.vector.service.QueryProcessingService;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.connection.Resources.commit;
import static com.dbn.connection.Resources.rollbackSilently;


public class QueryEmbeddingPipeline implements EmbeddingPipeline {
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final String TEXT_COLUMN_ALIAS = "TEXT";

    private final QueryProcessingService queryProcessingService = new QueryProcessingService();

    @Override
    public void execute(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull VectorEmbeddingResult result) {

        EmbeddingSourceQueries sources = request.getSourceConfig().getSourceQueries();
        for (EmbeddingSourceQuery source : sources.getElements()) {
            EmbeddingQueryResult queryResult = result.getResult(source);

            String metadata = queryProcessingService.buildRowMetadata(request, source);
            queryResult.setMetadata(metadata);
            context.getProgressIndicator().setText2("Processing query " + queryResult.getName());

            // Execute the embedding with batching
            embedQueryDataInBatches(context, request, queryResult);
        }

    }

    /**
     * Embed data from the source table using batching for failure recovery.
     * Each batch is committed separately, so progress is preserved on failure.
     */
    private void embedQueryDataInBatches(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull EmbeddingQueryResult result) {

        StepResult embedStep = result.startStep(PipelineStep.EMBED);
        ProgressIndicator progressIndicator = context.getProgressIndicator();
        DBNConnection conn = context.getConnection();

        try {
            int totalRowsEmbedded = 0;
            int batchNumber = 0;

            conn.setAutoCommit(false);

            ConnectionHandler connection = request.getConnection();
            String selectStatement = result.getSelectStatement();
            selectStatement = adjustSelectStatement(connection, selectStatement);

            while (true) {
                if (progressIndicator.isCanceled()) break;

                batchNumber++;
                progressIndicator.setText2("Processing query " + result.getName() + " (batch " + batchNumber + " / rows embedded " + totalRowsEmbedded + ")");
                EmbeddingDestinationConfig destinationConfig = request.getDestinationConfig();

                // Process one batch
                DatabaseVectorInterface vectorInterface = connection.getVectorInterface();
                int rowsEmbedded = vectorInterface.embedQueryContent(
                        conn,
                        selectStatement,
                        request.getChunkConfigJson(),
                        request.getModelConfigJson(),
                        destinationConfig.getSchemaName(),
                        destinationConfig.getTableName(),
                        result.getMetadata(), DEFAULT_BATCH_SIZE);

                // Commit after each batch - this is the recovery point
                commit(conn);

                totalRowsEmbedded += rowsEmbedded;
                if (rowsEmbedded == 0) break;
            }

            if (progressIndicator.isCanceled()) {
                embedStep.markSuccess();
                result.finishSuccess(totalRowsEmbedded);
            } else {
                embedStep.markSuccess();
                result.finishSuccess(totalRowsEmbedded);
            }

        } catch (Exception e) {
            // Rollback only the current failed batch
            rollbackSilently(conn);
            embedStep.markFailed("EMBED_ERROR", e);
            result.finishFailed("EMBED_ERROR", e);
        }
    }

    /**
     * Prepare the select statement to be embedded as subquery in the embedding statement
     * (see "embed-query-content" statement definition)
     *  - trim the statement
     *  - remove tailing semicolons
     *  - identify first select-item and make sure it has an alias named "TEXT"
     */
    private String adjustSelectStatement(
            @NotNull ConnectionHandler connection,
            @NotNull String selectStatement) {
        DBLanguageDialect languageDialect = nd(connection.getLanguageDialect(SQLLanguage.INSTANCE));

        // cleanup tailing semicolons
        selectStatement = selectStatement.trim();
        while (selectStatement.endsWith(";")) selectStatement = selectStatement.substring(0, selectStatement.length() - 1).trim();

        DBLanguagePsiFile previewFile = DBLanguagePsiFile.createFromText(
                connection.getProject(),
                "query",
                languageDialect,
                selectStatement,
                connection,
                connection.getDefaultSchemaId());
        if (isNotValid(previewFile)) return selectStatement;

        PsiElement firstChild = previewFile.getFirstChild();
        if (firstChild instanceof ExecutableBundlePsiElement rootPsiElement) {
            List<ExecutablePsiElement> executablePsiElements = rootPsiElement.getExecutablePsiElements();
            if (executablePsiElements.isEmpty()) return selectStatement;

            ExecutablePsiElement psiElement = executablePsiElements.get(0);
            BasePsiElement<?> selectItemPsiElement = psiElement.findFirstPsiElement(e -> isSelectItem(e));
            BasePsiElement<?> aliasDefinitionPsiElement = selectItemPsiElement.findFirstPsiElement(e -> e.getElementId().equals("column_alias_definition"));

            if (aliasDefinitionPsiElement == null) {
                // insert alias definition
                int insertOffset = selectItemPsiElement.getTextOffset() + selectItemPsiElement.getTextLength();
                return selectStatement.substring(0, insertOffset) + " as " + TEXT_COLUMN_ALIAS +  selectStatement.substring(insertOffset);
            } else {
                BasePsiElement aliasDefinition = aliasDefinitionPsiElement.findFirstPsiElement(ElementTypeAttribute.ALIAS_DEFINITION);
                if (aliasDefinition == null) return selectStatement;

                if (!aliasDefinition.getText().equalsIgnoreCase(TEXT_COLUMN_ALIAS)) {
                    // rename alias
                    int replaceOffset = aliasDefinition.getTextOffset();
                    return selectStatement.substring(0, replaceOffset) + TEXT_COLUMN_ALIAS +  selectStatement.substring(replaceOffset + aliasDefinition.getTextLength());
                }

            }
            return psiElement.getText();
        }

        return selectStatement;
    }

    private boolean isSelectItem(BasePsiElement<?> element) {
        if (!element.getElementId().equals("select_item")) return false;

        BasePsiElement subqueryFactoringClause = element.findEnclosingElement(false, e -> e.getElementId().equals("subquery_factoring_clause"));
        return subqueryFactoringClause == null;
    }
}
