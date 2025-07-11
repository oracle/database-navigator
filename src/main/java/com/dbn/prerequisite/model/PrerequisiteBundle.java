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
import com.dbn.common.icon.Icons;
import com.dbn.common.operation.DatabaseOperation;
import com.dbn.common.option.InteractiveOptionBroker;
import com.dbn.common.option.OptionBroker;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.prerequisite.evaluation.PrerequisiteEvaluator;
import com.dbn.prerequisite.event.PrerequisiteEvent;
import com.dbn.prerequisite.event.PrerequisiteEventListener;
import com.dbn.prerequisite.event.PrerequisiteEventType;
import com.dbn.prerequisite.resolution.PrerequisiteAdvice;
import com.dbn.prerequisite.resolution.PrerequisiteAdvisor;
import com.dbn.prerequisite.resolution.PrerequisiteOption;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.common.load.ProgressMonitor.setProgressDetail;
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
    private final OptionBroker<PrerequisiteOption> optionBroker;

    private final AtomicInteger evaluationCount = new AtomicInteger();
    private boolean evaluating = false;
    private long evaluationTimestamp;

    public PrerequisiteBundle(ConnectionHandler connection, DatabaseOperation operation, List<Prerequisite> prerequisites) {
        this.connection = ConnectionRef.of(connection);
        this.operation = operation;
        this.prerequisites = Collections.unmodifiableList(prerequisites);
        this.optionBroker = createOptionBroker(operation);
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

    public int size() {
        return prerequisites.size();
    }

    public boolean isEvaluated() {
        return evaluationCount.get() == size();
    }

    public boolean isCompletely(PrerequisiteStatus status) {
        return Lists.allMatch(prerequisites, p -> p.getStatus() == status);
    }

    public boolean has(PrerequisiteStatus status) {
        return Lists.anyMatch(prerequisites, p -> p.getStatus() == status);
    }

    public int count(PrerequisiteStatus status) {
        return Lists.count(prerequisites, p -> p.getStatus() == status);
    }

    public synchronized void evaluateAll(boolean background) {
        if (evaluating) return;
        evaluating = true;

        notifyEvaluationStarted(null);
        evaluationCount.set(0);
        prerequisites.forEach(p -> evaluatePrerequisite(p, background));
    }

    private void evaluatePrerequisite(Prerequisite prerequisite, boolean background) {
        if (background) {
            Background.run(() -> evaluatePrerequisite(prerequisite));
        } else {
            evaluatePrerequisite(prerequisite);
        }
    }

    private void evaluatePrerequisite(Prerequisite prerequisite) {
        try {
            setProgressDetail("Evaluating prerequisite \"" + prerequisite.getName() + "\"");

            // reset the prerequisite
            prerequisite.setStatusMessage(null);
            prerequisite.setStatusException(null);
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
        } finally {
            evaluationCount.incrementAndGet();
            if (evaluationCount.get() == size()) {
                notifyEvaluationFinished(null);
                evaluating = false;
                evaluationTimestamp = System.currentTimeMillis();
            }
        }
    }

    private double getEvaluationProgress() {
        return (double) evaluationCount.get() / size();
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

    public void removeEventListener(PrerequisiteEventListener listener) {
        listeners.remove(listener);
    }


    private static OptionBroker<PrerequisiteOption> createOptionBroker(DatabaseOperation operation) {
        String description = operation.getType().getDescription();
        return new InteractiveOptionBroker<>(
                "missing-prerequisites",
                "Missing Prerequisites",
                "Not all requirements for performing \"" + description + "\" are met. Do you want to continue?",
                PrerequisiteOption.CONTINUE,
                PrerequisiteOption.RESOLVE,
                PrerequisiteOption.CONTINUE,
                PrerequisiteOption.CANCEL).
                            withIcon(Icons.DIALOG_WARNING).
                            withDoNotShowMessage("Skip verification for this connection");

    }

    @Override
    public void disposeInner() {

    }
}
