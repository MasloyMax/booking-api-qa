package qa.booking.support;

import org.junit.jupiter.api.extension.*;
import qa.booking.client.BookingClient;

public final class ApiExtension implements ParameterResolver {
    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(ApiExtension.class);
    public record Api(BookingClient alice, BookingClient bob, BookingClient anonymous, String baseUrl) {}
    @Override public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == Api.class;
    }
    @Override public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        var server = context.getRoot().getStore(NS)
                .computeIfAbsent("server", key -> new LocalApiServer(), LocalApiServer.class);
        String url = server.baseUrl();
        return new Api(BookingClient.authenticated(url, "alice"), BookingClient.authenticated(url, "bob"),
                new BookingClient(url, null), url);
    }
}
