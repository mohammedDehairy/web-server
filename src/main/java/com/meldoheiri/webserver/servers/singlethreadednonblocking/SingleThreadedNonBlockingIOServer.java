package com.meldoheiri.webserver.servers.singlethreadednonblocking;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.meldoheiri.webserver.serverconfig.ServerConfig;
import com.meldoheiri.webserver.servers.WebServer;
import com.meldoheiri.webserver.servers.exceptions.WebServerException;
import com.meldoheiri.webserver.servers.httprequesthandler.HTTPSocketDataHandler;

public class SingleThreadedNonBlockingIOServer implements WebServer {
    private final ServerConfig config;

    public SingleThreadedNonBlockingIOServer(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void start() throws WebServerException {

        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.configureBlocking(false);
            serverChannel.bind(serverAddress);

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
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new WebServerException("Server Failure", e);
        }
    }

    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept(); // Non-blocking accept new socket connection
        clientChannel.configureBlocking(false);

        // Register the new channel with selector for read operations
        clientChannel.register(selector, SelectionKey.OP_READ, new RequestState(new ByteArrayOutputStream()));
    }

    private void handleRead(SelectionKey key) throws IOException, WebServerException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        try {
            RequestState requestState = (RequestState) key.attachment();

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = clientChannel.read(buffer);
            boolean isReadyToWriteResponse = false;
            if (bytesRead == -1) {
                isReadyToWriteResponse = true;
            } else {
                buffer.flip();
                isReadyToWriteResponse = requestState.getRequestHandler().read(buffer.array());
                buffer.clear();
            }
        
            if (isReadyToWriteResponse) {
                ByteBuffer responsBuffer = ByteBuffer.wrap(requestState.responseStream.toByteArray());
                clientChannel.write(responsBuffer);
                clientChannel.close();
            }
            
        } catch (IOException | WebServerException e) {
            try {
                clientChannel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            throw e;
        }
    }

    static class RequestState {
        private final ByteArrayOutputStream responseStream;
        private final HTTPSocketDataHandler requestHandler;

        RequestState(ByteArrayOutputStream responseStream) {
            this.responseStream = responseStream;
            this.requestHandler = new HTTPSocketDataHandler(responseStream);
        }

        public ByteArrayOutputStream getResponseStream() {
            return responseStream;
        }

        public HTTPSocketDataHandler getRequestHandler() {
            return requestHandler;
        }
    }
}
