package qa.booking.client;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import static io.restassured.RestAssured.given;

/** HTTP adapter. Tests own status, schema and business assertions. */
public final class BookingClient {
    private final String baseUrl;
    private final String token;
    public BookingClient(String baseUrl, String token) { this.baseUrl = baseUrl; this.token = token; }
    public static BookingClient authenticated(String baseUrl, String username) {
        Response response = new BookingClient(baseUrl, null).request()
                .body(Map.of("username", username, "password", "demo-password")).post("/auth/token");
        response.then().statusCode(200);
        return new BookingClient(baseUrl, response.jsonPath().getString("access_token"));
    }
    // Build per call: no shared mutable RestAssured globals/specifications in parallel tests.
    public RequestSpecification request() {
        var spec = new RequestSpecBuilder().setBaseUri(baseUrl).setContentType(ContentType.JSON)
                .setConfig(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 5_000)
                        .setParam("http.socket.timeout", 5_000)))
                .build();
        if (token != null) spec.header("Authorization", "Bearer " + token);
        return given().spec(spec);
    }
    public Response create(BookingRequest input, String key) {
        return request().header("Idempotency-Key", key).body(input.body()).post("/bookings");
    }
    public Response get(String id) { return request().get("/bookings/{id}", id); }
    public Response cancel(String id) { return request().delete("/bookings/{id}", id); }
    public Response list(String room) { return request().queryParam("room", room).get("/bookings"); }
}
