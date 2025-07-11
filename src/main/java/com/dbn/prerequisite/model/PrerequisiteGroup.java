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
import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.operation.DatabaseOperation;
import com.dbn.common.option.InteractiveOptionBroker;
import com.dbn.common.option.OptionBroker;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
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
import java.util.stream.Stream;

import static com.dbn.common.load.ProgressMonitor.setProgressDetail;
import static com.dbn.prerequisite.event.PrerequisiteEventType.EVALUATION_FAILED;
import static com.dbn.prerequisite.event.PrerequisiteEventType.EVALUATION_FINISHED;
import static com.dbn.prerequisite.event.PrerequisiteEventType.EVALUATION_STARTED;
import static com.dbn.prerequisite.model.PrerequisiteStatus.AVAILABLE;
import static com.dbn.prerequisite.model.PrerequisiteStatus.EVALUATING;
import static com.dbn.prerequisite.model.PrerequisiteStatus.UNAVAILABLE;
import static com.dbn.prerequisite.model.PrerequisiteStatus.UNKNOWN;

/**
 * Group of prerequisites associated to a given {@link DatabaseOperation}
 * All prerequisites are expected to be met for performing the given operation
 */
@Getter
public class PrerequisiteGroup extends StatefulDisposableBase implements DatabaseContextBase {
    private final WeakRef<PrerequisiteData> data;
    private final DatabaseOperation operation;
    private final List<PrerequisiteType> prerequisiteTypes;

    private final Listeners<PrerequisiteEventListener> listeners = Listeners.create(this);
    private final OptionBroker<PrerequisiteOption> optionBroker;

    private final AtomicInteger evaluationCount = new AtomicInteger();
    private boolean evaluating = false;
    private long evaluationTimestamp;

    public PrerequisiteGroup(PrerequisiteData prerequisiteData, DatabaseOperation operation, List<PrerequisiteType> prerequisiteTypes) {
        this.data = WeakRef.of(prerequisiteData);
        this.operation = operation;
        this.prerequisiteTypes = Collections.unmodifiableList(prerequisiteTypes);
        this.optionBroker = createOptionBroker(operation);
    }

    @NotNull
    public PrerequisiteData getData() {
        return data.ensure();
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return getData().getConnection();
    }

    public List<Prerequisite> getPrerequisites() {
        return Lists.convert(prerequisiteTypes, t -> getData().getPrerequisite(t));
    }

    private Stream<Prerequisite> prerequisites() {
        return prerequisiteTypes.stream().map(t -> getData().getPrerequisite(t));
    }

    @NotNull
    @Override
    public Project getProject() {
        return getConnection().getProject();
    }

    public String createAdviceContent() {
        StringBuilder builder = new StringBuilder();
        ConnectionHandler connection = getConnection();
        for (Prerequisite prerequisite : getPrerequisites()) {
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
        return prerequisiteTypes.size();
    }

    public boolean isEvaluated() {
        return evaluationCount.get() == size();
    }

    public boolean arePrerequisitesMet() {
        return prerequisites().allMatch(p -> p.getStatus() == AVAILABLE);
    }

    public boolean has(PrerequisiteStatus status) {
        return prerequisites().anyMatch(p -> p.getStatus() == status);
    }

    public int count(PrerequisiteStatus status) {
        return (int) prerequisites().filter(p -> p.getStatus() == status).count();
    }

    public synchronized void evaluateAll(boolean background) {
        if (evaluating) return;
        evaluating = true;

        notifyEvaluationStarted(null);
        evaluationCount.set(0);
        prerequisites().forEach(p -> evaluatePrerequisite(p, background));
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

            prerequisite.setStatus(conditionsMet ? AVAILABLE : UNAVAILABLE);
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
        return new InteractiveOptionBroker<>(
                "missing-prerequisites",
                "Missing Prerequisites",
                "Not all requirements for performing \"" + operation + "\" are met. " +
                        "You may not be able to perform this operation.\n" +
                        "Do you want to continue?",
                PrerequisiteOption.CONTINUE,
                PrerequisiteOption.RESOLVE,
                PrerequisiteOption.CONTINUE,
                PrerequisiteOption.CANCEL).
                            withIcon(Icons.DIALOG_WARNING).
                            withDoNotShowMessage("Skip verification for this connection");

    }

    public TitledMessage createStatusMessage() {
        String description = operation.getName();
        if (isEvaluated()) {
            int total = size();

            int unknown = count(UNKNOWN);
            int available = count(AVAILABLE);
            int unavailable = count(UNAVAILABLE);

            if (available == total) {
                return new TitledMessage(MessageType.SUCCESS,
                        description + " - Requirements met",
                        "All requirements for performing the operation \"" + description + "\" are met\n");
            }

            if (unavailable == total) {
                return new TitledMessage(MessageType.ERROR,
                        description + " - Requirements not met",
                        "None of the requirements for performing the operation \"" + description + "\" are met.\n" +
                                "Please request the missing privileges from your database administrator.");
            }

            if (unknown == total) {
                return new TitledMessage(MessageType.ERROR,
                        description + " - Failed to verify requirements",
                        "Could not verify any of the requirements for performing the operation \"" + description + "\".\n  " +
                                "Please check the connectivity or database access rights.");

            }

            if (available > 0) {
                return new TitledMessage(MessageType.WARNING,
                        description + " - Requirements partially met",
                        "Some of the requirements for performing the operation \"" + description + "\" are not met.\n" +
                                "Please request the missing privileges from your database administrator.");
            }

        }
        return new TitledMessage(MessageType.INFO,
                description + " - Verifying requirements...",
                "Verifying requirements for performing the operation \"" + description + "\"\n");
    }

    @Override
    public void disposeInner() {

    }
}
