package qa.booking.client;

import java.util.Map;
import java.util.UUID;

/** Immutable input. A new builder creates a unique room for each test. */
public record BookingRequest(String room, String start, String end) {
    public Map<String, Object> body() { return Map.of("room", room, "start", start, "end", end); }
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String room = "room-" + UUID.randomUUID();
        private String start = "2030-05-10";
        private String end = "2030-05-12";
        public Builder room(String value) { room = value; return this; }
        public Builder dates(String from, String to) { start = from; end = to; return this; }
        public BookingRequest build() { return new BookingRequest(room, start, end); }
    }
}
