package qa.booking.tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import qa.booking.client.BookingRequest;
import qa.booking.support.ApiExtension;
import java.util.HashMap;
import java.util.UUID;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@ExtendWith(ApiExtension.class)
class ValidationTest {
    @ParameterizedTest(name = "reject dates {0} -> {1}")
    @CsvSource({
        "2030-05-12,2030-05-10", "2030-05-10,2030-05-10",
        "2030-02-30,2030-03-04", "2030-02-29,2030-03-04",
        "2030-13-01,2030-14-01", "not-a-date,2030-05-12",
        "2030-5-1,2030-05-12"
    })
    void invalidDateRanges(String start, String end, ApiExtension.Api api) {
        var input = BookingRequest.builder().dates(start, end).build();
        api.alice().create(input, UUID.randomUUID().toString()).then().statusCode(422)
                .body(matchesJsonSchemaInClasspath("schemas/error.json")).body("error", equalTo("invalid_booking"));
        api.alice().list(input.room()).then().statusCode(200).body("items", empty());
    }

    @Test void leapDayIsAcceptedInLeapYear(ApiExtension.Api api) {
        api.alice().create(BookingRequest.builder().dates("2032-02-29", "2032-03-01").build(),
                UUID.randomUUID().toString()).then().statusCode(201);
    }

    @ParameterizedTest @ValueSource(strings = {"", "room with spaces", "../private", "<script>alert(1)</script>"})
    void invalidRoomNames(String room, ApiExtension.Api api) {
        api.alice().create(BookingRequest.builder().room(room).build(), UUID.randomUUID().toString())
                .then().statusCode(422).body("error", equalTo("invalid_booking"));
    }

    @Test void oversizedRoomIsRejected(ApiExtension.Api api) {
        api.alice().create(BookingRequest.builder().room("a".repeat(81)).build(), UUID.randomUUID().toString())
                .then().statusCode(422);
    }

    @ParameterizedTest @ValueSource(strings = {"room", "start", "end"})
    void requiredFieldsCannotBeOmitted(String field, ApiExtension.Api api) {
        var body = new HashMap<>(BookingRequest.builder().build().body()); body.remove(field);
        api.alice().request().header("Idempotency-Key", UUID.randomUUID().toString()).body(body).post("/bookings")
                .then().statusCode(422).body("error", equalTo("invalid_booking"));
    }

    @Test void callerCannotInjectOwnerOrStatus(ApiExtension.Api api) {
        var body = new HashMap<>(BookingRequest.builder().build().body());
        body.put("owner", "bob"); body.put("status", "cancelled");
        api.alice().request().header("Idempotency-Key", UUID.randomUUID().toString()).body(body).post("/bookings")
                .then().statusCode(422);
    }

    @Test void malformedJsonReturns400(ApiExtension.Api api) {
        api.alice().request().body("{broken").post("/bookings").then().statusCode(400)
                .body(matchesJsonSchemaInClasspath("schemas/error.json")).body("error", equalTo("invalid_json"));
    }

    @Test void missingIdempotencyKeyReturns400(ApiExtension.Api api) {
        api.alice().request().body(BookingRequest.builder().build().body()).post("/bookings")
                .then().statusCode(400).body("error", equalTo("invalid_idempotency_key"));
    }
}
