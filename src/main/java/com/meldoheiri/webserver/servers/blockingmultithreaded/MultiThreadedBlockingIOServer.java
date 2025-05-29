package com.meldoheiri.webserver.servers.blockingmultithreaded;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.meldoheiri.webserver.serverconfig.ServerConfig;
import com.meldoheiri.webserver.servers.WebServer;
import com.meldoheiri.webserver.servers.exceptions.WebServerException;
import com.meldoheiri.webserver.servers.httpsocketdatahandler.HTTPSocketDataHandler;

public class MultiThreadedBlockingIOServer implements WebServer {
    private final ServerConfig config;

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
            HTTPSocketDataHandler requestHandler = new HTTPSocketDataHandler(socket.getOutputStream());
            byte[] buffer = new byte[1024];
            InputStream in = socket.getInputStream();
            int readBytes;
            while ((readBytes = in.read(buffer, 0, buffer.length)) != -1) {
                requestHandler.read(buffer);
            }
        } catch (IOException | WebServerException e) {
            System.err.println("Failed to handle client connection");
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Failed to close client socket");
                e.printStackTrace();
            }
        }
    }
}
