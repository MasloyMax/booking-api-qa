package qa.booking.support;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Owned by the JUnit root store, closed once after the test plan. */
public final class LocalApiServer implements AutoCloseable {
    private final Process process;
    private final String baseUrl;
    public LocalApiServer() {
        Process started = null;
        try {
            Files.createDirectories(Path.of("target"));
            ProcessBuilder builder = new ProcessBuilder(
                    System.getProperty("node.executable", "node"), "fixture/server.mjs")
                    .redirectError(Path.of("target", "fixture-stderr.log").toFile());
            builder.environment().put("PORT", "0");
            started = builder.start();
            process = started;
            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = CompletableFuture.supplyAsync(() -> {
                try { return reader.readLine(); }
                catch (Exception error) { throw new IllegalStateException(error); }
            }).get(Duration.ofSeconds(15).toMillis(), TimeUnit.MILLISECONDS);
            if (line == null || !line.matches("\\{\"port\":\\d+}")) {
                throw new IllegalStateException("Fixture did not report a port; see target/fixture-stderr.log");
            }
            baseUrl = "http://127.0.0.1:" + line.replaceAll("\\D", "");
        } catch (Exception error) {
            if (started != null) started.destroyForcibly();
            throw new IllegalStateException("Cannot start Node 24 fixture", error);
        }
    }
    public String baseUrl() { return baseUrl; }
    @Override public void close() throws Exception {
        process.destroy();
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }
}
