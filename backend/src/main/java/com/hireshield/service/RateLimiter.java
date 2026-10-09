package com.hireshield.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.hireshield.exception.TooManyRequestsException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

/**
 * Simple in-memory limit for the public text check (per IP for guests, per user when logged in).
 * Fine for one instance; with several instances this would move to Redis.
 */
@Service
public class RateLimiter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int guestPerMinute;

    public RateLimiter(@Value("${app.rate-limit.guest-checks-per-minute}") int guestPerMinute) {
        this.guestPerMinute = guestPerMinute;
    }

    public void check(String key, boolean loggedIn) {
        int perMinute = loggedIn ? guestPerMinute * 3 : guestPerMinute;
        Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(perMinute).refillGreedy(perMinute, Duration.ofMinutes(1)).build())
                .build());
        if (!bucket.tryConsume(1)) {
            throw new TooManyRequestsException("Too many checks. Please wait a minute and try again.");
        }
    }

    @Scheduled(fixedDelay = 30, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    void cleanUp() {
        buckets.clear();
    }
}
