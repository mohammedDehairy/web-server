package com.meldoheiri.webserver.servers;

import com.meldoheiri.webserver.servers.exceptions.WebServerException;

public interface WebServer {
    void start() throws WebServerException;
}
