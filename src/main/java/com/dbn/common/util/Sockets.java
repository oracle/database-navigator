package com.dbn.common.util;

import com.dbn.diagnostics.Diagnostics;
import com.intellij.util.Range;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

/**
 * Utility methods for TCP sockets and http
 */
public class Sockets {
    /**
     * TODO: could move to Http specific class but we don't really have much to put there right now
     * Or is there an apache commons class with similar constants? Could not find one.
     *
     * @param urlStr
     */
    public static Range<Integer> INFO_RESPONSE = new Range<>(100, 199);
    public static Range<Integer> SUCCESS_RESPONSE = new Range<>(200, 299);
    public static Range<Integer> REDIRECT_RESPONSE = new Range<>(300, 399);
    public static Range<Integer> CLIENT_ERROR_RESPONSE = new Range<>(400,499);
    public static Range<Integer> SERVER_ERROR_RESPONSE = new Range<>(500,599);
    public static Range<Integer> ALL_ERROR_RESPONSE = new Range<>(400, 599);
    public static Range<Integer> ALL_SUCCESS_RESPONSE = new Range<>(100,299);

    /**
     * Tries to connect the urlStr assuming it is an HTTP server.
     * Sends a GET request. In DeveloperMode, prints the HTTP response if
     * successful.
     *
     * @param urlStr
     * @return true if the HTTP request returns a success code.
     * // TODO: should we narrow the success code range?
     */
    public static boolean pokeWebServer(String urlStr) {
        HttpURLConnection httpConnection = null;
        try {
            httpConnection = (HttpURLConnection) new URL(urlStr).openConnection();
            httpConnection.setRequestMethod("GET");
            // Get the response code
            int responseCode = httpConnection.getResponseCode();
            if (!ALL_SUCCESS_RESPONSE.isWithin(responseCode)) {
                throw new IOException(
                        String.format("Non-success http response code poking port: code=%d", responseCode));
            }
            
            if (Diagnostics.isDeveloperMode()) {
                // Read the response
                try (BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()))) {
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                } catch (IOException ioe) {
                    Diagnostics.conditionallyLog(ioe);
                }
            }

            // Disconnect
            httpConnection.disconnect();
            return true;
        } catch (IOException e) {
            Diagnostics.conditionallyLog(e);
            return false;
        }
        finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }

    /**
     *
     * @return the InetAddress for localhost
     * @throws UnknownHostException
     */
    public static InetAddress getLocalHost() throws UnknownHostException {
        return InetAddress.getByName("localhost");
    }

    /**
     * Tries to bind the server socket port for port.
     * @param port
     * @return true if port was available to bind, false if it's already bound.
     * @throws IOException
     */
    public static boolean tryToBindPort(int port) throws IOException {
        ServerSocket socket = null;
        try  {
            socket = new ServerSocket(8181, 50, getLocalHost());
            return true;
        } catch (IOException e) {
            if (e instanceof BindException) {
                return false;
            }
            throw e;
        } finally {
            if (socket != null && socket.isBound() && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}
