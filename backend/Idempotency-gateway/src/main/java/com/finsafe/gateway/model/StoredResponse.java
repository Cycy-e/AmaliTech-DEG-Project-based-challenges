package com.finsafe.gateway.model;

import java.util.Map;

public record StoredResponse(int status, Map<String, Object> body) {
}
