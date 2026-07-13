package io.tempo.json.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class TestResources {

    private TestResources() {
    }

    public static String read(String classpathLocation) {
        try (InputStream inputStream = TestResources.class.getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Test resource not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read test resource: " + classpathLocation, exception);
        }
    }
}
