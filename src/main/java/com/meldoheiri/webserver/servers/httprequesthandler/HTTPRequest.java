package com.meldoheiri.webserver.servers.httprequesthandler;

import java.util.Map;

public record HTTPRequest(String path, Map<String, String> headers, String body) {
	
}
