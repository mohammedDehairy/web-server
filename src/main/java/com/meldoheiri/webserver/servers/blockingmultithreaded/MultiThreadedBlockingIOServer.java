package com.meldoheiri.webserver.servers.blockingmultithreaded;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.meldoheiri.webserver.serverconfig.ServerConfig;
import com.meldoheiri.webserver.servers.WebServer;
import com.meldoheiri.webserver.servers.exceptions.WebServerException;
import com.meldoheiri.webserver.servers.httpsocketdatahandler.HTTPSocketDataHandler;

public class MultiThreadedBlockingIOServer implements WebServer {
    private final ServerConfig config;
    private AtomicInteger connectionCount = new AtomicInteger(0);

    public MultiThreadedBlockingIOServer(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void start() throws WebServerException {
        try {
            InetAddress serverAddress = InetAddress.getByName(config.getHost());
            int port = config.getPort();
            ExecutorService threadPool = Executors.newFixedThreadPool(config.getNoThreads());
            try (ServerSocket serverSocket = new ServerSocket(port, 50, serverAddress)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleClient(clientSocket));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new WebServerException("Server Failure", e);
        }
    }

    private void handleClient(Socket socket) {
        try {
            System.out.println("Accept Socket connection: " + connectionCount.incrementAndGet());
            HTTPSocketDataHandler requestHandler = new HTTPSocketDataHandler(socket.getOutputStream(), false);
            byte[] buffer = new byte[1024];
            InputStream in = socket.getInputStream();
            int readBytes;
            while ((readBytes = in.read(buffer, 0, buffer.length)) != -1) {
                requestHandler.read(buffer);
                if (requestHandler.shouldCloseConnection()) {
                    closeConnection(socket);
                    return;
                }
            }
        } catch (IOException | WebServerException e) {
            System.err.println("Failed to handle client connection");
            e.printStackTrace();
        } finally {
            closeConnection(socket);
        }
    }

    private void closeConnection(Socket socket) {
        try {
            socket.close();
            System.out.println("Socket closed");
            if (connectionCount.get() >= 0) {
                connectionCount.decrementAndGet();
            }
        } catch (IOException e) {
            System.err.println("Failed to close client socket");
            e.printStackTrace();
        }
    }
}
