package qa.booking.tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import qa.booking.client.BookingRequest;
import qa.booking.support.ApiExtension;
import java.util.UUID;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@ExtendWith(ApiExtension.class)
class BookingLifecycleTest {
    @Test void createReadCancelAndReleaseAvailability(ApiExtension.Api api) {
        var input = BookingRequest.builder().build();
        var created = api.alice().create(input, UUID.randomUUID().toString());
        created.then().statusCode(201).header("Location", startsWith("/bookings/"))
                .body(matchesJsonSchemaInClasspath("schemas/booking.json"))
                .body("room", equalTo(input.room()), "owner", equalTo("alice"), "status", equalTo("confirmed"));
        String id = created.jsonPath().getString("id");
        api.alice().get(id).then().statusCode(200).body("id", equalTo(id));
        api.alice().cancel(id).then().statusCode(200).body("status", equalTo("cancelled"));
        api.alice().get(id).then().statusCode(200).body("status", equalTo("cancelled"));
        api.alice().create(input, UUID.randomUUID().toString()).then().statusCode(201).body("id", not(equalTo(id)));
    }

    @Test void adjacentIntervalsAreAllowed(ApiExtension.Api api) {
        var input = BookingRequest.builder().dates("2030-05-10", "2030-05-12").build();
        api.alice().create(input, UUID.randomUUID().toString()).then().statusCode(201);
        var adjacent = BookingRequest.builder().room(input.room()).dates("2030-05-12", "2030-05-14").build();
        api.alice().create(adjacent, UUID.randomUUID().toString()).then().statusCode(201);
        api.alice().list(input.room()).then().statusCode(200).body("items.size()", equalTo(2));
    }

    @Test void sameDatesInDifferentRoomsAreAllowed(ApiExtension.Api api) {
        api.alice().create(BookingRequest.builder().build(), UUID.randomUUID().toString()).then().statusCode(201);
        api.alice().create(BookingRequest.builder().build(), UUID.randomUUID().toString()).then().statusCode(201);
    }

    @Test void cancellationIsIdempotent(ApiExtension.Api api) {
        String id = api.alice().create(BookingRequest.builder().build(), UUID.randomUUID().toString())
                .then().statusCode(201).extract().path("id");
        api.alice().cancel(id).then().statusCode(200).body("status", equalTo("cancelled"));
        api.alice().cancel(id).then().statusCode(200).body("status", equalTo("cancelled"));
    }

    @Test void unknownBookingReturnsStructured404(ApiExtension.Api api) {
        api.alice().get(UUID.randomUUID().toString()).then().statusCode(404)
                .body(matchesJsonSchemaInClasspath("schemas/error.json")).body("error", equalTo("booking_not_found"));
    }
}
