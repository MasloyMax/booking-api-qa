package qa.booking.tests;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import qa.booking.client.BookingRequest;
import qa.booking.support.ApiExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.IntFunction;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApiExtension.class)
class ConcurrencyTest {
    private List<Response> simultaneously(IntFunction<Response> action) throws Exception {
        var ready = new CountDownLatch(8); var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Future<Response>>();
            for (int i = 0; i < 8; i++) {
                final int index = i;
                tasks.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start barrier timed out");
                    return action.apply(index);
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "All request workers must reach the barrier");
            start.countDown();
            var responses = new ArrayList<Response>();
            for (var task : tasks) responses.add(task.get(15, TimeUnit.SECONDS));
            return responses;
        } finally { start.countDown(); }
    }

    @Test void onlyOneConcurrentBookingCanWin(ApiExtension.Api api) throws Exception {
        var input = BookingRequest.builder().build();
        var responses = simultaneously(index -> api.alice().create(input, UUID.randomUUID().toString()));
        assertEquals(1L, responses.stream().filter(r -> r.statusCode() == 201).count());
        assertEquals(7L, responses.stream().filter(r -> r.statusCode() == 409).count());
        responses.stream().filter(r -> r.statusCode() == 409).forEach(r -> r.then().body("error", equalTo("room_unavailable")));
        api.alice().list(input.room()).then().statusCode(200).body("items.size()", equalTo(1));
    }

    @Test void concurrentRetriesCreateExactlyOneResource(ApiExtension.Api api) throws Exception {
        var input = BookingRequest.builder().build(); String key = UUID.randomUUID().toString();
        var responses = simultaneously(index -> api.alice().create(input, key));
        assertEquals(1L, responses.stream().filter(r -> r.statusCode() == 201).count());
        assertEquals(7L, responses.stream().filter(r -> r.statusCode() == 200).count());
        assertEquals(1L, responses.stream().map(r -> r.jsonPath().getString("id")).distinct().count());
        api.alice().list(input.room()).then().statusCode(200).body("items.size()", equalTo(1));
    }
}
