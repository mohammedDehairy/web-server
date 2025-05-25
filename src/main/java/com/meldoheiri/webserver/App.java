package com.meldoheiri.webserver;

import java.io.IOException;
import com.meldoheiri.webserver.serverconfig.ServerConfig;
import com.meldoheiri.webserver.servers.WebServer;
import com.meldoheiri.webserver.servers.blockingmultithreaded.MultiThreadedBlockingIOServer;
import com.meldoheiri.webserver.servers.exceptions.WebServerException;
import com.meldoheiri.webserver.servers.singlethreadednonblocking.SingleThreadedNonBlockingIOServer;

public class App {

    public static void main(String[] args) throws IOException, WebServerException {
        ServerConfig config = new ServerConfig();
        WebServer server = new MultiThreadedBlockingIOServer(config);
        server.start();
    }
    
}