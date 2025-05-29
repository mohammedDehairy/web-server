package com.meldoheiri.webserver.servers.httprequesthandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class DefaultHTTPRequestHandler implements HTTPRequestHandler {
    @Override
    public HTTPResponse handle(HTTPRequest request) {
        try {
            Path path = Paths.get(request.path());
            if (Files.isDirectory(path)) {
                path = Paths.get(request.path() + "/index.html");
            }
            String body = Files.readString(path);
            return new HTTPResponse("HTTP/1.1 200 OK\r\n", Map.of(), body);
        } catch (IOException | InvalidPathException e) {
            e.printStackTrace();
            return new HTTPResponse("HTTP/1.1 404 Not Found\r\n", Map.of(), "");
        }
    }
}
