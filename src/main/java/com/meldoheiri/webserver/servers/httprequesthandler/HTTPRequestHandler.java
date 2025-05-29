package com.meldoheiri.webserver.servers.httprequesthandler;

public interface HTTPRequestHandler {
    HTTPResponse handle(HTTPRequest request);
}
