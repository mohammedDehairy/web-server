package com.meldoheiri.webserver.validators;

import java.util.regex.Pattern;

public class PathValidator {
    private static final Pattern PATH_REGEX = Pattern.compile("^(\\/[a-zA-Z0-9._~!$&'()*+,;=:@%-]*)*$");

    public boolean validate(String path) {
        return path != null && PATH_REGEX.matcher(path).matches();
    }
}
