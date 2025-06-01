package com.meldoheiri.webserver.serverconfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerConfig {
    private String host;
    private int port;
    private int noThreads;
    private SocketScheduler socketScheduler;

    public ServerConfig() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                throw new IOException("application.properties is not found in class path");
            }
            properties.load(inputStream);
            host = properties.getProperty("server.host");
            port = Integer.parseInt(properties.getProperty("server.port"));
            noThreads = Integer.parseInt(properties.getProperty("server.noThreads"));
            String schedulerString = properties.getProperty("server.sockerscheduler");
            if (schedulerString == null) {
                schedulerString = "SingleThreadedNonBlockingIOServer";
            }
            this.socketScheduler = SocketScheduler.valueOf(schedulerString);
            if (socketScheduler == null) {
                this.socketScheduler = SocketScheduler.SingleThreadedNonBlockingIOServer;
            }
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getNoThreads() {
        return noThreads;
    }

    public SocketScheduler getSocketScheduler() {
        return socketScheduler;
    }

    public enum SocketScheduler {
        MultiThreadedBlockingIOServer,
        SingleThreadedNonBlockingIOServer;
    }
}
