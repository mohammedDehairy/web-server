package com.meldoheiri.webserver.servers.singlethreadednonblocking;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.meldoheiri.webserver.serverconfig.ServerConfig;
import com.meldoheiri.webserver.servers.WebServer;
import com.meldoheiri.webserver.servers.exceptions.WebServerException;
import com.meldoheiri.webserver.servers.httpsocketdatahandler.HTTPSocketDataHandler;

public class SingleThreadedNonBlockingIOServer implements WebServer {
    private final ServerConfig config;
    private final static int MAX_CONNECTIONS = 10000;
    private Map<SocketChannel, ConnectionState> connections = new HashMap<>();

    public SingleThreadedNonBlockingIOServer(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void start() throws WebServerException {

        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(serverAddress);

            // event loop
            Selector selector = Selector.open();

            // register server channel with selector to accept connections
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                selector.select(); // block until at least one channel is ready

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid())
                        continue;

                    if (key.isAcceptable()) {
                        handleAccept(key, selector);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handlWrite(key);
                    }

                    cleanUpIdleConnections();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new WebServerException("Server Failure", e);
        }
    }

    private void cleanUpIdleConnections() {
        Iterator<Map.Entry<SocketChannel, ConnectionState>> iterator = connections.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<SocketChannel, ConnectionState> entry = iterator.next();
            SocketChannel clientChannel = entry.getKey();
            ConnectionState connectionState = entry.getValue();

            if (connectionState.isIdle()) {
                try {
                    clientChannel.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    System.out.println("Connection idle for too long");
                    iterator.remove();
                }
            }
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept(); // Non-blocking accept new socket connection
        if (connections.entrySet().size() >= MAX_CONNECTIONS) {
            System.out.println("Connection refused");
            clientChannel.close();
            return;
        }

        clientChannel.configureBlocking(false);
        // Register the new channel with selector for read operations
        clientChannel.register(selector, SelectionKey.OP_READ);
        connections.put(clientChannel, new ConnectionState(new ByteArrayOutputStream()));
        System.out.println(connections.keySet().size());
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ConnectionState requestState = connections.get(clientChannel);

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead;
        try {
            bytesRead = clientChannel.read(buffer);
        } catch (IOException e) {
            closeConnection(clientChannel);
            e.printStackTrace();
            return;
        }
        boolean isReadyToWriteResponse = false;
        if (bytesRead == -1) {
            isReadyToWriteResponse = true;
        } else {
            buffer.flip();
            try {
                isReadyToWriteResponse = requestState.getRequestHandler().read(buffer.array());
            } catch (WebServerException e) {
                e.printStackTrace();
                return;
            }
        }
        buffer.clear();
        if (!isReadyToWriteResponse) {
            return;
        }
        requestState.fillWriteBuffer();
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void handlWrite(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ConnectionState requestState = connections.get(clientChannel);

        if (!requestState.writeBuffer.hasRemaining() && requestState.getRequestHandler().shouldCloseConnection()) {
            closeConnection(clientChannel);
            return;
        }

        if (!clientChannel.isOpen()) {
            return;
        }

        ByteBuffer writeBuffer = requestState.getWriteBuffer();
        try {
            clientChannel.write(writeBuffer);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("No buffer space available")) {
                throw e;
            }
            closeConnection(clientChannel);
            e.printStackTrace();
            return;
        }

        if (!requestState.writeBuffer.hasRemaining() && requestState.getRequestHandler().shouldCloseConnection()) {
            closeConnection(clientChannel);
            return;
        }

        if (requestState.writeBuffer.hasRemaining()) {
            System.out.println("Keep writing");
            key.interestOps(SelectionKey.OP_WRITE);
        } else {
            // Register the new channel with selector for read operations
            // System.out.println("Switch to reading");
            // key.interestOps(SelectionKey.OP_READ);
            // requestState.setWriteBuffer(null);
            closeConnection(clientChannel);
        }
    }

    private void closeConnection(SocketChannel clientChannel) {
        try {
            clientChannel.close();
            System.out.println("Connection closed");
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            connections.remove(clientChannel);
        }
    }

    static class ConnectionState {
        private ByteArrayOutputStream responseStream;
        private ByteBuffer writeBuffer;
        private final HTTPSocketDataHandler requestHandler;
        private long lastActiveTimeInMilliSeconds;

        ConnectionState(ByteArrayOutputStream responseStream) {
            this.responseStream = responseStream;
            this.requestHandler = new HTTPSocketDataHandler(responseStream, true);
            this.lastActiveTimeInMilliSeconds = now();
        }

        public ByteArrayOutputStream getResponseStream() {
            return responseStream;
        }

        public HTTPSocketDataHandler getRequestHandler() {
            return requestHandler;
        }

        public void fillWriteBuffer() {
            byte[] array = responseStream.toByteArray();
            writeBuffer = ByteBuffer.wrap(array);
            responseStream = new ByteArrayOutputStream();
            lastActiveTimeInMilliSeconds = now();
        }

        public ByteBuffer getWriteBuffer() {
            lastActiveTimeInMilliSeconds = now();
            return writeBuffer;
        }

        public void setWriteBuffer(ByteBuffer writeBuffer) {
            this.writeBuffer = writeBuffer;
        }

        public boolean isIdle() {
            return now() - lastActiveTimeInMilliSeconds > 30_000;
        }

        private long now() {
            return System.nanoTime() / 1000_000;
        }
    }
}
