package com.meldoheiri.webserver;

import java.io.IOException;

import com.meldoheiri.webserver.serverconfig.ServerConfig;
import com.meldoheiri.webserver.serverconfig.ServerConfig.SocketScheduler;
import com.meldoheiri.webserver.servers.WebServer;
import com.meldoheiri.webserver.servers.blockingmultithreaded.MultiThreadedBlockingIOServer;
import com.meldoheiri.webserver.servers.exceptions.WebServerException;
import com.meldoheiri.webserver.servers.singlethreadednonblocking.SingleThreadedNonBlockingIOServer;

public class App {

    public static void main(String[] args) throws IOException, WebServerException {
        ServerConfig config = new ServerConfig();
        WebServer server = creatWebServer(config);
        server.start();
    }

    private static WebServer creatWebServer(ServerConfig config) {
        SocketScheduler socketScheduler = config.getSocketScheduler();
        switch (socketScheduler) {
            case SingleThreadedNonBlockingIOServer:
                return new SingleThreadedNonBlockingIOServer(config);
            case MultiThreadedBlockingIOServer:
                return new MultiThreadedBlockingIOServer(config);
            default:
                return new MultiThreadedBlockingIOServer(config);
        }
    }

}