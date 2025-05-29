package com.meldoheiri.webserver.servers.httprequesthandler;

import java.util.Map;

public record HTTPResponse(String firstLine, Map<String, String> headers, String body) {
    
}
