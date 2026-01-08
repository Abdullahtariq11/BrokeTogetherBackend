package com.broketogether.api.dto;

public record ErrorResponse(int status, String message, long timestamp) {

}
