package com.meldoheiri.webserver.servers.httpsocketdatahandler;

import com.meldoheiri.webserver.servers.exceptions.WebServerException;

public interface SocketDataHandler {
    boolean read(byte[] data) throws WebServerException;
}
