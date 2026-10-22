package qa.booking.tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import qa.booking.client.BookingClient;
import qa.booking.client.BookingRequest;
import qa.booking.support.ApiExtension;
import java.util.Map;
import java.util.UUID;
import static org.hamcrest.Matchers.*;

@ExtendWith(ApiExtension.class)
class AuthorizationTest {
    @Test void missingTokenIsRejected(ApiExtension.Api api) {
        api.anonymous().create(BookingRequest.builder().build(), UUID.randomUUID().toString())
                .then().statusCode(401).body("error", equalTo("unauthorized"));
    }

    @Test void forgedTokenIsRejected(ApiExtension.Api api) {
        new BookingClient(api.baseUrl(), "not-a-real-token").list("room")
                .then().statusCode(401).body("error", equalTo("unauthorized"));
    }

    @Test void invalidCredentialsAreRejected(ApiExtension.Api api) {
        api.anonymous().request().body(Map.of("username", "alice", "password", "wrong"))
                .post("/auth/token").then().statusCode(401).body("error", equalTo("invalid_credentials"));
    }

    @Test void anotherUserCannotReadCancelOrListBooking(ApiExtension.Api api) {
        var input = BookingRequest.builder().build();
        String id = api.alice().create(input, UUID.randomUUID().toString()).then().statusCode(201).extract().path("id");
        api.bob().get(id).then().statusCode(404);
        api.bob().cancel(id).then().statusCode(404);
        api.bob().list(input.room()).then().statusCode(200).body("items", empty());
        api.alice().get(id).then().statusCode(200).body("status", equalTo("confirmed"));
    }

    @Test void otherUsersBookingStillBlocksAvailability(ApiExtension.Api api) {
        var input = BookingRequest.builder().build();
        api.alice().create(input, UUID.randomUUID().toString()).then().statusCode(201);
        api.bob().create(input, UUID.randomUUID().toString()).then().statusCode(409).body("error", equalTo("room_unavailable"));
    }
}
