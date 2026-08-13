package com.skala.qna.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.skala.qna.organization.UserRole;

@Service
public class JwtService {

	private static final String ALGORITHM = "HmacSHA256";
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
	private static final Pattern SUBJECT = Pattern.compile("\\\"sub\\\":(\\d+)");
	private static final Pattern ROLE = Pattern.compile("\\\"role\\\":\\\"([A-Z]+)\\\"");
	private static final Pattern EXPIRATION = Pattern.compile("\\\"exp\\\":(\\d+)");

	private final byte[] secret;
	private final long expirationSeconds;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds) {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes.");
		}
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
		this.expirationSeconds = expirationSeconds;
	}

	public String issue(Long userId, UserRole role) {
		long now = Instant.now().getEpochSecond();
		long expiresAt = now + expirationSeconds;
		String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
		String payload = encode("{\"sub\":" + userId + ",\"role\":\"" + role + "\",\"iat\":" + now
				+ ",\"exp\":" + expiresAt + "}");
		String content = header + "." + payload;
		return content + "." + ENCODER.encodeToString(sign(content));
	}

	public long expirationSeconds() {
		return expirationSeconds;
	}

	public Claims parse(String token) {
		try {
			String[] parts = token.split("\\.", -1);
			if (parts.length != 3) {
				throw new InvalidTokenException();
			}
			byte[] expected = sign(parts[0] + "." + parts[1]);
			byte[] actual = DECODER.decode(parts[2]);
			if (!MessageDigest.isEqual(expected, actual)) {
				throw new InvalidTokenException();
			}
			String payload = new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8);
			long userId = number(payload, SUBJECT);
			long expiresAt = number(payload, EXPIRATION);
			Matcher roleMatcher = ROLE.matcher(payload);
			if (!roleMatcher.find() || Instant.now().getEpochSecond() >= expiresAt) {
				throw new InvalidTokenException();
			}
			return new Claims(userId, UserRole.valueOf(roleMatcher.group(1)), expiresAt);
		} catch (InvalidTokenException | IllegalArgumentException exception) {
			throw new InvalidTokenException();
		}
	}

	private long number(String payload, Pattern pattern) {
		Matcher matcher = pattern.matcher(payload);
		if (!matcher.find()) {
			throw new InvalidTokenException();
		}
		return Long.parseLong(matcher.group(1));
	}

	private String encode(String value) {
		return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private byte[] sign(String value) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(secret, ALGORITHM));
			return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
		} catch (Exception exception) {
			throw new IllegalStateException("JWT signing failed.", exception);
		}
	}

	public record Claims(Long userId, UserRole role, long expiresAt) {
	}

	public static class InvalidTokenException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
