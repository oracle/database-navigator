/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.debugger.jdwp.process;

import com.dbn.debugger.jdwp.process.tunnel.NSTunnelConnectionProxy;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

final class JdwpPacketReader {
    static final int HEADER_LENGTH = 11;
    static final int BUFFER_SIZE = 320000;
    static final int MAX_PACKET_LENGTH = BUFFER_SIZE;

    private final ByteBuffer readBuffer;
    private final Supplier<NSTunnelConnectionProxy> connectionSupplier;
    private final Runnable closeConnection;

    JdwpPacketReader(
            @NotNull ByteBuffer readBuffer,
            @NotNull Supplier<NSTunnelConnectionProxy> connectionSupplier,
            @NotNull Runnable closeConnection) {
        this.readBuffer = readBuffer;
        this.connectionSupplier = connectionSupplier;
        this.closeConnection = closeConnection;
    }

    byte[] readPacket() throws IOException {
        readAtLeast(Integer.BYTES);

        int packetLength = readBuffer.getInt(0);
        validatePacketLength(packetLength);

        readAtLeast(packetLength);

        readBuffer.flip();
        byte[] packet = new byte[packetLength];
        readBuffer.get(packet);
        readBuffer.compact();

        return packet;
    }

    private void readAtLeast(int length) throws IOException {
        while (readBuffer.position() < length) {
            readFromTunnel();
        }
    }

    private void readFromTunnel() throws IOException {
        NSTunnelConnectionProxy debugConnection = getConnection();
        int position = readBuffer.position();
        int read = read(debugConnection);
        if (read <= 0 || readBuffer.position() <= position) {
            closeConnection.run();
            throw new IOException("JDWP tunnel closed while reading packet");
        }
    }

    private int read(NSTunnelConnectionProxy debugConnection) throws IOException {
        try {
            return debugConnection.read(readBuffer);
        } catch (RuntimeException e) {
            closeConnection.run();
            throw new IOException("JDWP tunnel failed while reading packet", e);
        }
    }

    @NotNull
    private NSTunnelConnectionProxy getConnection() throws IOException {
        NSTunnelConnectionProxy debugConnection = connectionSupplier.get();
        if (debugConnection == null) {
            throw new IOException("JDWP tunnel is not open");
        }
        return debugConnection;
    }

    private void validatePacketLength(int packetLength) throws IOException {
        if (packetLength >= HEADER_LENGTH && packetLength <= MAX_PACKET_LENGTH) return;

        closeConnection.run();
        throw new IOException("Malformed JDWP packet length: " + packetLength);
    }
}
