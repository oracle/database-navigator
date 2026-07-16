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
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class JdwpPacketReaderTest {
    @Test
    public void rejectsNegativePacketLength() {
        FakeTunnelConnection connection = new FakeTunnelConnection(lengthHeader(-1));
        JdwpPacketReader reader = reader(connection);

        IOException exception = Assert.assertThrows(IOException.class, reader::readPacket);

        Assert.assertTrue(exception.getMessage().contains("Malformed JDWP packet length: -1"));
        Assert.assertTrue(connection.closed);
    }

    @Test
    public void rejectsPacketLengthSmallerThanHeader() {
        FakeTunnelConnection connection = new FakeTunnelConnection(lengthHeader(JdwpPacketReader.HEADER_LENGTH - 1));
        JdwpPacketReader reader = reader(connection);

        IOException exception = Assert.assertThrows(IOException.class, reader::readPacket);

        Assert.assertTrue(exception.getMessage().contains("Malformed JDWP packet length: 10"));
        Assert.assertTrue(connection.closed);
    }

    @Test
    public void rejectsPacketLengthLargerThanBuffer() {
        int packetLength = JdwpPacketReader.BUFFER_SIZE + 1;
        FakeTunnelConnection connection = new FakeTunnelConnection(lengthHeader(packetLength));
        JdwpPacketReader reader = reader(connection);

        IOException exception = Assert.assertThrows(IOException.class, reader::readPacket);

        Assert.assertTrue(exception.getMessage().contains("Malformed JDWP packet length: " + packetLength));
        Assert.assertTrue(connection.closed);
    }

    @Test
    public void rejectsIntegerMaxPacketLengthWithoutAllocatingPacket() {
        FakeTunnelConnection connection = new FakeTunnelConnection(lengthHeader(Integer.MAX_VALUE));
        JdwpPacketReader reader = reader(connection);

        IOException exception = Assert.assertThrows(IOException.class, reader::readPacket);

        Assert.assertTrue(exception.getMessage().contains("Malformed JDWP packet length: " + Integer.MAX_VALUE));
        Assert.assertTrue(connection.closed);
    }

    @Test
    public void rejectsZeroLengthReads() {
        FakeTunnelConnection connection = FakeTunnelConnection.zeroRead();
        JdwpPacketReader reader = reader(connection);

        IOException exception = Assert.assertThrows(IOException.class, reader::readPacket);

        Assert.assertTrue(exception.getMessage().contains("JDWP tunnel closed while reading packet"));
        Assert.assertTrue(connection.closed);
    }

    @Test
    public void readsPacketSplitAcrossTunnelReads() throws IOException {
        byte[] packet = packet(25, 1);
        FakeTunnelConnection connection = new FakeTunnelConnection(
                Arrays.copyOfRange(packet, 0, 4),
                Arrays.copyOfRange(packet, 4, packet.length));
        JdwpPacketReader reader = reader(connection);

        Assert.assertArrayEquals(packet, reader.readPacket());
        Assert.assertFalse(connection.closed);
    }

    @Test
    public void keepsExtraBytesForNextPacket() throws IOException {
        byte[] firstPacket = packet(25, 1);
        byte[] secondPacket = packet(11, 2);
        byte[] combinedPackets = concatenate(firstPacket, secondPacket);
        FakeTunnelConnection connection = new FakeTunnelConnection(combinedPackets);
        JdwpPacketReader reader = reader(connection);

        Assert.assertArrayEquals(firstPacket, reader.readPacket());
        Assert.assertArrayEquals(secondPacket, reader.readPacket());
        Assert.assertFalse(connection.closed);
    }

    private static JdwpPacketReader reader(FakeTunnelConnection connection) {
        return new JdwpPacketReader(
                ByteBuffer.allocate(JdwpPacketReader.BUFFER_SIZE),
                () -> connection,
                connection::closeQuietly);
    }

    private static byte[] lengthHeader(int packetLength) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(packetLength).array();
    }

    private static byte[] packet(int packetLength, int id) {
        ByteBuffer buffer = ByteBuffer.allocate(packetLength);
        buffer.putInt(packetLength);
        buffer.putInt(id);
        buffer.put((byte) 0);
        buffer.put((byte) 64);
        buffer.put((byte) 100);
        while (buffer.hasRemaining()) {
            buffer.put((byte) buffer.position());
        }
        return buffer.array();
    }

    private static byte[] concatenate(byte[] first, byte[] second) {
        byte[] bytes = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, bytes, first.length, second.length);
        return bytes;
    }

    private static class FakeTunnelConnection implements NSTunnelConnectionProxy {
        private final byte[][] chunks;
        private final boolean zeroRead;
        private int chunkIndex;
        private int chunkOffset;
        private boolean closed;

        private FakeTunnelConnection(byte[]... chunks) {
            this(false, chunks);
        }

        private FakeTunnelConnection(boolean zeroRead, byte[]... chunks) {
            this.zeroRead = zeroRead;
            this.chunks = chunks;
        }

        private static FakeTunnelConnection zeroRead() {
            return new FakeTunnelConnection(true);
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        public String tunnelAddress() {
            return "host=localhost;port=5005";
        }

        @Override
        public int read(ByteBuffer buffer) {
            if (closed) return -1;
            if (zeroRead) return 0;
            if (chunkIndex >= chunks.length) return -1;

            byte[] chunk = chunks[chunkIndex];
            int length = Math.min(buffer.remaining(), chunk.length - chunkOffset);
            buffer.put(chunk, chunkOffset, length);
            chunkOffset += length;
            if (chunkOffset == chunk.length) {
                chunkIndex++;
                chunkOffset = 0;
            }
            return length;
        }

        @Override
        public void write(ByteBuffer buffer) {
        }

        private void closeQuietly() {
            close();
        }
    }
}
