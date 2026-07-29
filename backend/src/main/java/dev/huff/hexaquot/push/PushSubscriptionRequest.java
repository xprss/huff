package dev.huff.hexaquot.push;

public record PushSubscriptionRequest(String endpoint, Keys keys) {
    public record Keys(String p256dh, String auth) {
    }
}
