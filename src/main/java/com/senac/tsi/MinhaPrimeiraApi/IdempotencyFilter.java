package com.senac.tsi.MinhaPrimeiraApi;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER = "Idempotency-Key";
    private static final int MAX_KEY_LENGTH = 255;
    private static final Duration TTL = Duration.ofHours(24);

    // One entry per (path, key). response == null means the first request is still running
    private record Entry(byte[] requestHash, Instant createdAt, StoredResponse response) {
        boolean expired() {
            return createdAt.plus(TTL).isBefore(Instant.now());
        }
    }

    private record StoredResponse(int status, String contentType, String location, byte[] body) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only POST is non-idempotent by definition; PUT/DELETE already are
        return !"POST".equalsIgnoreCase(request.getMethod()) || request.getHeader(HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(HEADER).trim();
        if (key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
            writeError(response, 400, "Idempotency-Key must have between 1 and " + MAX_KEY_LENGTH + " characters");
            return;
        }
        // The same key sent to another API version is a different request (different payload and response)
        String storeKey = request.getRequestURI() + "|" + apiVersion(request) + "|" + key;

        Entry existing = entries.get(storeKey);
        if (existing != null && existing.expired()) {
            entries.remove(storeKey, existing);
            existing = null;
        }

        if (existing != null) {
            // Repeated key: never run the controller again, just answer from the store
            byte[] hash = sha256(request.getInputStream().readAllBytes());
            if (!Arrays.equals(hash, existing.requestHash())) {
                writeError(response, 422, "Idempotency-Key was already used with a different request body");
            } else if (existing.response() == null) {
                writeError(response, 409, "A request with this Idempotency-Key is still being processed");
            } else {
                replay(response, existing.response());
            }
            return;
        }

        // Read the body up front so we can hash it before reserving the key
        byte[] body = request.getInputStream().readAllBytes();
        Entry inProgress = new Entry(sha256(body), Instant.now(), null);

        // putIfAbsent makes the reservation atomic when two requests with the same key arrive together
        Entry raced = entries.putIfAbsent(storeKey, inProgress);
        if (raced != null) {
            writeError(response, 409, "A request with this Idempotency-Key is still being processed");
            return;
        }

        evictExpired();

        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        boolean stored = false;
        try {
            chain.doFilter(new BodyReplayingRequest(request, body), cachedResponse);

            int status = cachedResponse.getStatus();
            // Only remember successes: a failed request created nothing, so the client may safely retry it
            if (status >= 200 && status < 300) {
                StoredResponse result = new StoredResponse(
                        status,
                        cachedResponse.getContentType(),
                        cachedResponse.getHeader(HttpHeaders.LOCATION),
                        cachedResponse.getContentAsByteArray());
                entries.replace(storeKey, inProgress, new Entry(inProgress.requestHash(), inProgress.createdAt(), result));
                stored = true;
            }
        } finally {
            if (!stored) {
                entries.remove(storeKey, inProgress);
            }
            cachedResponse.copyBodyToResponse();
        }
    }

    private void replay(HttpServletResponse response, StoredResponse stored) throws IOException {
        response.setStatus(stored.status());
        if (stored.contentType() != null) {
            response.setContentType(stored.contentType());
        }
        if (stored.location() != null) {
            response.setHeader(HttpHeaders.LOCATION, stored.location());
        }
        response.setHeader("Idempotent-Replayed", "true");
        response.getOutputStream().write(stored.body());
    }

    private static String apiVersion(HttpServletRequest request) {
        String version = request.getHeader(ApiVersionConfig.HEADER);
        if (version == null) {
            version = request.getParameter(ApiVersionConfig.QUERY_PARAM);
        }
        return version == null ? "default" : version.trim();
    }

    private void evictExpired() {
        entries.values().removeIf(entry -> entry.response() != null && entry.expired());
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // The body was already consumed for hashing, so hand the controller a fresh copy of it
    private static class BodyReplayingRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        BodyReplayingRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return in.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return in.read(b, off, len);
                }

                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
