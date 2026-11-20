package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DemoTest {

    private static final String EXPECTED_OUTPUT = """
            DOMESTIC 10kg STANDARD -> GHS 50.00
                zone cost                 50.00
                heavy-item surcharge       0.00
                service-level charge       0.00
            INTERNATIONAL 40kg EXPRESS -> GHS 967.50
                zone cost                625.00
                heavy-item surcharge      20.00
                service-level charge     322.50
            MOON 10kg STANDARD -> refused: No rates are configured for zone 'MOON'
            DOMESTIC -10kg STANDARD -> refused: Weight must be greater than 0 kg, got -10
            DOMESTIC 10kg OVERNIGHT -> refused: Unknown service level 'OVERNIGHT'; \
            expected one of [STANDARD, EXPRESS]
            """;

    @Test
    void printsItemisedQuotesAndTheReasonForEachRefusal() {
        var captured = new ByteArrayOutputStream();
        var originalOut = System.out;
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            Demo.main(new String[0]);
        } finally {
            System.setOut(originalOut);
        }

        String output = captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertEquals(EXPECTED_OUTPUT, output);
    }
}