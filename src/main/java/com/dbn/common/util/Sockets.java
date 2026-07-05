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

package com.dbn.common.util;

import com.dbn.common.thread.Synchronized;
import com.dbn.diagnostics.Diagnostics;
import com.intellij.util.Range;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.net.UnknownHostException;

import static com.dbn.common.util.Strings.isEmptyOrSpaces;

/**
 * Utility methods for TCP sockets and http
 */
@UtilityClass
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

    public static boolean isLoopbackHost(@NonNls String hostName) throws UnknownHostException {
        if (isEmptyOrSpaces(hostName)) return false;

        InetAddress[] addresses = InetAddress.getAllByName(hostName);
        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress()) return true;
        }
        return false;
    }

    public static boolean isLocalNetworkHost(@NonNls String hostName) throws UnknownHostException {
        if (isEmptyOrSpaces(hostName)) return false;

        InetAddress[] addresses = InetAddress.getAllByName(hostName);
        for (InetAddress address : addresses) {
            if (!isLocalNetworkAddress(address)) return false;
        }
        return addresses.length > 0;
    }

    public static boolean isLocalNetworkAddress(InetAddress address) {
        return address.isAnyLocalAddress() ||
               address.isLoopbackAddress() ||
               address.isLinkLocalAddress() ||
               address.isSiteLocalAddress() ||
               isUniqueLocalAddress(address);
    }

    private static boolean isUniqueLocalAddress(InetAddress address) {
        if (!(address instanceof Inet6Address)) return false;

        byte[] bytes = address.getAddress();
        return (bytes[0] & 0xfe) == 0xfc;
    }

    /**
     * Tries to bind the server socket port for port.
     * @param port the port to be verified
     * @return true if the port was available to bind, false if we were unable to bind the port
     * for any reason. Logs and swallows any IOExceptions from ServerSocket rather than throwing.
     */
    public static boolean tryToBindPort(int port) {
        // synchronize evaluation to prevent false positives on concurrent invocations
        // (i.e., when the verification itself briefly keeps the port busy)
        return Synchronized.on("PORT_CHECK:" + port, k -> {
            return tryToBindPort("localhost", port);
        });
    }

    private static boolean tryToBindPort(@NonNls String hostName, int port) {
        ServerSocket socket = null;
        try  {
            InetAddress address = InetAddress.getByName(hostName);
            socket = new ServerSocket(port, 50, address);
            return true;
        } catch (IOException e) {
            Diagnostics.conditionallyLog(e);
            return false;
        } finally {
            if (socket != null && socket.isBound() && !socket.isClosed()) {
                Unsafe.warned(socket, s -> s.close());
            }
        }
    }
}
