package qa.booking.tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import qa.booking.client.BookingRequest;
import qa.booking.support.ApiExtension;
import java.util.LinkedHashMap;
import java.util.UUID;
import static org.hamcrest.Matchers.*;

@ExtendWith(ApiExtension.class)
class IdempotencyTest {
    @Test void replayReturnsSameBookingWithoutDuplicate(ApiExtension.Api api) {
        var input = BookingRequest.builder().build(); String key = UUID.randomUUID().toString();
        var original = api.alice().create(input, key).then().statusCode(201).extract().response();
        api.alice().create(input, key).then().statusCode(200).body(equalTo(original.asString()));
        api.alice().list(input.room()).then().statusCode(200).body("items.size()", equalTo(1));
    }

    @Test void changedPayloadCannotReuseKey(ApiExtension.Api api) {
        var input = BookingRequest.builder().build(); String key = UUID.randomUUID().toString();
        api.alice().create(input, key).then().statusCode(201);
        var changed = BookingRequest.builder().room(input.room()).dates("2030-06-01", "2030-06-03").build();
        api.alice().create(changed, key).then().statusCode(409).body("error", equalTo("idempotency_conflict"));
        api.alice().list(input.room()).then().body("items.size()", equalTo(1));
    }

    @Test void keyScopeIsPerUser(ApiExtension.Api api) {
        String key = UUID.randomUUID().toString();
        api.alice().create(BookingRequest.builder().build(), key).then().statusCode(201);
        api.bob().create(BookingRequest.builder().build(), key).then().statusCode(201).body("owner", equalTo("bob"));
    }

    @Test void objectKeyOrderDoesNotChangeRequestIdentity(ApiExtension.Api api) {
        var input = BookingRequest.builder().build(); String key = UUID.randomUUID().toString();
        String id = api.alice().create(input, key).then().statusCode(201).extract().path("id");
        var reordered = new LinkedHashMap<String, Object>();
        reordered.put("end", input.end()); reordered.put("start", input.start()); reordered.put("room", input.room());
        api.alice().request().header("Idempotency-Key", key).body(reordered).post("/bookings")
                .then().statusCode(200).body("id", equalTo(id));
    }

    @Test void replayAfterCancellationDoesNotResurrectBooking(ApiExtension.Api api) {
        var input = BookingRequest.builder().build(); String key = UUID.randomUUID().toString();
        String id = api.alice().create(input, key).then().statusCode(201).extract().path("id");
        api.alice().cancel(id).then().statusCode(200);
        // The idempotency response is the original snapshot; GET is the current resource state.
        api.alice().create(input, key).then().statusCode(200).body("id", equalTo(id));
        api.alice().get(id).then().statusCode(200).body("status", equalTo("cancelled"));
        api.alice().list(input.room()).then().body("items.size()", equalTo(1));
    }
}
