package com.test.platform.api.registry.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class Sha256 {
	private Sha256() {
	}

	static String hex(byte[] bytes) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] out = digest.digest(bytes);
			StringBuilder sb = new StringBuilder(out.length * 2);
			for (byte b : out) {
				sb.append(Character.forDigit((b >> 4) & 0xF, 16));
				sb.append(Character.forDigit(b & 0xF, 16));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
