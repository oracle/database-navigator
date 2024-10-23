package com.dbn.error.jira;

import com.dbn.error.IssueReport;
import com.dbn.error.IssueReportBuilder;
import com.dbn.error.IssueReportSubmitter;
import com.dbn.error.TicketResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static java.nio.charset.StandardCharsets.UTF_8;

public class JiraIssueReportSubmitter extends IssueReportSubmitter {
    private static final GsonBuilder GSON_BUILDER = new GsonBuilder();
    private static final JiraIssueReportBuilder REPORT_BUILDER = new JiraIssueReportBuilder();
    private static final String URL = "https://database-navigator.atlassian.net/";

    @Override
    public String getTicketUrl(String ticketId) {
        return URL + "browse/" + ticketId;
    }

    @Override
    protected IssueReportBuilder getBuilder() {
        return REPORT_BUILDER;
    }

    @NotNull
    @Override
    public TicketResponse submit(IssueReport report) throws Exception {
        JiraTicketRequest ticketRequest = new JiraTicketRequest(report);
        try {
            Gson gson = GSON_BUILDER.create();
            String requestJson = gson.toJson(ticketRequest.getJsonObject());

            HttpURLConnection connection = openConnection();
            writeRequest(connection, requestJson);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return new JiraTicketResponse(null, "Received authorization error from server");
            }

            String jsonResponse = readResponse(connection);
            return new JiraTicketResponse(jsonResponse, null);
        } catch (Exception e) {
            conditionallyLog(e);
            return new JiraTicketResponse(null, e.getMessage());
        }
    }

    private static void writeRequest(HttpURLConnection connection, String requestString) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestString.getBytes(UTF_8));
        }
    }

    @NotNull
    private static HttpURLConnection openConnection() throws IOException {
        // https://developer.atlassian.com/cloud/jira/platform/jira-rest-api-basic-authentication/
        URL url = new URL(URL + "rest/api/latest/issue");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        //TODO connection.addRequestProperty("Authorization", "Basic ["email:apikey" -> base64"]);
        return connection;
    }

    @NotNull
    private static String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = newResponseReader(connection)) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private static BufferedReader newResponseReader(HttpURLConnection connection) throws IOException {
        InputStream is = connection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is, UTF_8);
        return new BufferedReader(isr);
    }
}
