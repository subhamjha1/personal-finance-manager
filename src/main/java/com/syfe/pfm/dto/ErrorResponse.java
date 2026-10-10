package com.syfe.pfm.dto;

import java.util.List;

/**
 * Uniform error body. Never contains stack traces or internal details.
 *
 * @param status    HTTP status code
 * @param error     HTTP reason phrase
 * @param message   human-readable description
 * @param details   optional per-field problems
 * @param timestamp ISO-8601 instant
 */
public record ErrorResponse(int status, String error, String message, List<String> details, String timestamp) {
}
