package com.meldoheiri.webserver.servers.httpsocketdatahandler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.meldoheiri.webserver.servers.exceptions.WebServerException;
import com.meldoheiri.webserver.servers.httprequesthandler.DefaultHTTPRequestHandler;
import com.meldoheiri.webserver.servers.httprequesthandler.HTTPRequest;
import com.meldoheiri.webserver.servers.httprequesthandler.HTTPRequestHandler;
import com.meldoheiri.webserver.servers.httprequesthandler.HTTPResponse;
import com.meldoheiri.webserver.validators.PathValidator;

public class HTTPSocketDataHandler implements SocketDataHandler {
    private static final PathValidator pathValidator = new PathValidator();
    private static final Set<String> ACCEPTED_METHODS = Set.of("GET", "POST", "DELETE", "PUT", "PATCH", "HEAD", "OPTIONS").stream().map(String::toLowerCase).collect(Collectors.toSet());

    private final BufferedWriter responseWriter;
    private final StringBuilder requestLines = new StringBuilder();
    private Map<String, String> headersMap = null;
    private int bytesCount = 0;
    private int bodyLength = 0;
    private int bodyStartIndex = -1;
    private String[] firstLine;
    private boolean closeConnections;

    private static final String ROOT_PATH = "/usr/local/MyWebServer";

    private final Map<String, HTTPRequestHandler> routingTable = Map.of("MyWebApp", new DefaultHTTPRequestHandler());

    public HTTPSocketDataHandler(OutputStream responseOutputStream, boolean closeConnections) {
        this.responseWriter = new BufferedWriter(new OutputStreamWriter(responseOutputStream));
        this.closeConnections = closeConnections;
    }

    @Override
    public boolean read(byte[] data) throws WebServerException {
        try {
            bytesCount += data.length;
            requestLines.append(new String(data, StandardCharsets.UTF_8.name()));
            parseHeadersIfComplete();
            if (isReadyToWriteResponse()) {
                writeResponse();
                return true;
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            throw new WebServerException("Failed to handle client request", e);
        }
    }

    private String buildBody() {
        String requestString = requestLines.toString();
        return requestString.substring(bodyStartIndex, bodyStartIndex + bodyLength);
    }

    private void parseHeadersIfComplete() throws WebServerException {
        if (headersMap != null) {
            return;
        }
        String headers = requestLines.toString();
        boolean headersComplete = headers.contains("\r\n\r\n");
        if (!headersComplete) {
            return;
        }
        String[] lines = headers.split("\r\n");
        firstLine = parseFirstLine(lines[0]);
        headersMap = new HashMap<>();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            String[] keyValue = line.split(": ");
            if (keyValue.length != 2) {
                continue;
            }
            headersMap.put(keyValue[0], keyValue[1]);
            if (keyValue[0].equalsIgnoreCase("content-length")) {
                try {
                    bodyLength = Integer.parseInt(keyValue[1]);
                } catch (NumberFormatException e) {
                    throw new WebServerException("Bad Request", e);
                }
            }
        }
    }

    private boolean isReadyToWriteResponse() throws UnsupportedEncodingException {
        String requestString = requestLines.toString();
        bodyStartIndex = requestString.indexOf("\r\n\r\n") + 4;
        int bodyBytes = bytesCount - bodyStartIndex;
        return bodyBytes >= bodyLength;
    }

    private void writeResponse() throws IOException, WebServerException {
        String fullPath = firstLine[1];
        if (!pathValidator.validate(fullPath)) {
            throw new WebServerException("Invalid path: " + firstLine);
        }
        String[] components = fullPath.split("/");
        String root;
        if (components.length < 2) {
            root = "";
        } else {
            root = components[1];
        }
        HTTPRequestHandler responseHandler = routingTable.get(root);
        HTTPResponse response;
        if (responseHandler == null) {
            response = new HTTPResponse("HTTP/1.1 404 Not Found\r\n", Map.of(), "");
        } else {
            response = responseHandler.handle(new HTTPRequest(ROOT_PATH + fullPath, headersMap, buildBody()));
        }
        String reponse = buildResponse(response);
        responseWriter.write(reponse);
        responseWriter.flush();
    }

    private String buildResponse(HTTPResponse response) throws WebServerException {
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append(response.firstLine());
        responseBuilder.append("Content-Type: text/plain\r\n");
        if (shouldCloseConnection() || closeConnections) {
            responseBuilder.append("Connection: close\r\n");
        } else {
            responseBuilder.append("Connection: keep-alive\r\n");
        }

        for (Map.Entry<String, String> header : response.headers().entrySet()) {
            responseBuilder.append(header.getKey() + ": " + header.getValue() + "\r\n");
        }
        responseBuilder.append("Content-Length: " + response.body().getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n");
        responseBuilder.append(response.body());

        return  responseBuilder.toString();
    }

    private String[] parseFirstLine(String firstLine) throws WebServerException {
        String[] components = firstLine.split(" ");
        if (components.length != 3) {
            throw new WebServerException("Unexpected first line");
        }

        if (!ACCEPTED_METHODS.contains(components[0].toLowerCase())) {
            throw new WebServerException("Only GET method is allowed");
        }

        if (!"HTTP/1.1".equalsIgnoreCase(components[2])) {
            throw new WebServerException("Only HTTP/1.1 is supported");
        }

        return components;
    }

    @Override
    public boolean shouldCloseConnection() {
        return headersMap.containsKey("Connection") && headersMap.get("Connection").equals("close");
    }
}
