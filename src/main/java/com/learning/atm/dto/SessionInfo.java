package com.learning.atm.dto;

import java.time.Instant;

/**
 * One signed-in device, shown on the "active sessions" screen.
 *
 * @param current marks the session making the request, so the UI can badge it "this device"
 */
public record SessionInfo(
		Instant signedInAt,
		Instant lastUsedAt,
		String userAgent,
		boolean current) {
}
