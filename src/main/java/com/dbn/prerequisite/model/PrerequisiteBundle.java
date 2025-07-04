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

package com.dbn.prerequisite.model;

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.operation.DatabaseOperation;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.util.Listeners;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.prerequisite.evaluation.PrerequisiteEvaluator;
import com.dbn.prerequisite.event.PrerequisiteEvent;
import com.dbn.prerequisite.event.PrerequisiteEventListener;
import com.dbn.prerequisite.event.PrerequisiteEventType;
import com.dbn.prerequisite.resolution.PrerequisiteAdvice;
import com.dbn.prerequisite.resolution.PrerequisiteAdvisor;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static com.dbn.prerequisite.event.PrerequisiteEventType.EVALUATION_FAILED;
import static com.dbn.prerequisite.event.PrerequisiteEventType.EVALUATION_FINISHED;
import static com.dbn.prerequisite.event.PrerequisiteEventType.EVALUATION_STARTED;
import static com.dbn.prerequisite.model.PrerequisiteStatus.EVALUATING;
import static com.dbn.prerequisite.model.PrerequisiteStatus.SATISFIED;
import static com.dbn.prerequisite.model.PrerequisiteStatus.UNKNOWN;
import static com.dbn.prerequisite.model.PrerequisiteStatus.UNSATISFIED;

@Getter
public class PrerequisiteBundle extends StatefulDisposableBase implements DatabaseContextBase {
    private final ConnectionRef connection;
    private final DatabaseOperation operation;
    private final List<Prerequisite> prerequisites;
    private final Listeners<PrerequisiteEventListener> listeners = Listeners.create(this);


    public PrerequisiteBundle(ConnectionHandler connection, DatabaseOperation operation, List<Prerequisite> prerequisites) {
        this.connection = ConnectionRef.of(connection);
        this.operation = operation;
        this.prerequisites = Collections.unmodifiableList(prerequisites);
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    @NotNull
    @Override
    public Project getProject() {
        return getConnection().getProject();
    }

    public String createAdviceContent() {
        StringBuilder builder = new StringBuilder();
        ConnectionHandler connection = this.connection.ensure();
        for (Prerequisite prerequisite : prerequisites) {
            PrerequisiteAdvisor advisor = prerequisite.getDefinition().getAdvisor();
            PrerequisiteAdvice advice = advisor.advise(connection);
            builder.append("--");
            builder.append(advice.getDescription());
            builder.append("\n");
            builder.append(advice.getCode());
            builder.append("\n");
            builder.append("\n");
        }

        return builder.toString();
    }

    public void evaluateAll() {
        prerequisites.forEach(p -> evaluatePrerequisite(p));
    }

    private void evaluatePrerequisite(Prerequisite prerequisite) {
        Background.run(() -> {
            try {
                prerequisite.setStatus(EVALUATING);
                notifyEvaluationStarted(prerequisite);

                PrerequisiteEvaluator evaluator = prerequisite.getDefinition().getEvaluator();
                boolean conditionsMet = evaluator.evaluate(this);

                prerequisite.setStatus(conditionsMet ? SATISFIED : UNSATISFIED);
                notifyEvaluationFinished(prerequisite);

            } catch (Exception e) {
                prerequisite.setStatus(UNKNOWN);
                prerequisite.setStatusException(e);
                prerequisite.setStatusMessage("Could not verify prerequisite. " + e.getMessage());

                notifyEvaluationFailed(prerequisite);
            }
        });
    }

    private void notifyEvaluationStarted(Prerequisite prerequisite) {
        notifyEvent(EVALUATION_STARTED, prerequisite);
    }

    private void notifyEvaluationFinished(Prerequisite prerequisite) {
        notifyEvent(EVALUATION_FINISHED, prerequisite);
    }

    private void notifyEvaluationFailed(Prerequisite prerequisite) {
        notifyEvent(EVALUATION_FAILED, prerequisite);
    }

    private void notifyEvent(PrerequisiteEventType type, Prerequisite prerequisite) {
        PrerequisiteEvent event = new PrerequisiteEvent(type, prerequisite);
        listeners.notify(l -> l.eventOccurred(event));
    }

    public void addEventListener(PrerequisiteEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void disposeInner() {

    }
}
