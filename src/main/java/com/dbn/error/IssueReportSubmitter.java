package com.dbn.error;

import com.dbn.DatabaseNavigator;
import com.dbn.common.action.Lookups;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Strings;
import com.dbn.nls.NlsSupport;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

import static com.dbn.common.notification.NotificationGroup.REPORTING;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus.FAILED;
import static com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus.NEW_ISSUE;

@Slf4j
public abstract class IssueReportSubmitter extends ErrorReportSubmitter implements NlsSupport {

    @Override
    public IdeaPluginDescriptor getPluginDescriptor() {
        IdeaPluginDescriptor pluginDescriptor = (IdeaPluginDescriptor) super.getPluginDescriptor();
        if (pluginDescriptor == null) {
            pluginDescriptor = DatabaseNavigator.getPluginDescriptor();
            setPluginDescriptor(pluginDescriptor);
        }
        return pluginDescriptor;
    }
    @NotNull
    @Override
    public String getReportActionText() {
        return txt("msg.reporting.title.SubmitIssueReport");
    }

    public abstract String getTicketUrl(String ticketId);

    protected abstract IssueReportBuilder getBuilder();

    @Override
    public boolean submit(
            @NotNull IdeaLoggingEvent[] events,
            @Nullable String additionalInfo,
            @NotNull Component parentComponent,
            @NotNull Consumer consumer) {
        Project project = Lookups.getProject(parentComponent);
        IdeaPluginDescriptor plugin = getPluginDescriptor();
        IssueReportBuilder builder = getBuilder();

        IssueReport report = builder.buildReport(project, plugin, events, additionalInfo, cast(consumer));
        submitReport(report);
        return true;
    }

    private void submitReport(@Nullable IssueReport report) {
        if (report == null) return;

        Project project = report.getProject();

        Progress.prompt(project, null, true, txt("prc.reporting.title.SubmittingIssueReport"), null, progress -> {
            TicketResponse response;
            Consumer<SubmittedReportInfo> consumer = report.getConsumer();
            try {
                response = submit(report);
            } catch (Exception e) {
                conditionallyLog(e);

                NotificationSupport.sendErrorNotification(project, REPORTING,
                        txt("ntf.reporting.error.FailedToSendIssueReport", e));

                consumer.consume(new SubmittedReportInfo(null, null, FAILED));
                return;
            }

            String errorMessage = response.getErrorMessage();
            if (Strings.isEmpty(errorMessage)) {
                log.info("Error report submitted, response: {}", response);

                String ticketId = response.getTicketId();
                String ticketUrl = getTicketUrl(ticketId);
                NotificationSupport.sendInfoNotification(project, REPORTING,
                        txt("ntf.reporting.info.IssueReportSuccessfullySent", ticketUrl, ticketId));

                consumer.consume(new SubmittedReportInfo(ticketUrl, ticketId, NEW_ISSUE));
            } else {
                NotificationSupport.sendErrorNotification(project, REPORTING,
                        txt("ntf.reporting.error.ErrorSendingIssueReport", errorMessage));
                consumer.consume(new SubmittedReportInfo(null, null, FAILED));
            }
        });
    }

    @NotNull
    public abstract TicketResponse submit(IssueReport report) throws Exception;

}