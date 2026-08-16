package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexahackDtos.Parity;
import dev.huff.hexaquot.game.HexahackDtos.ProbeComparison;
import dev.huff.hexaquot.game.HexahackDtos.ProbeRequest;
import dev.huff.hexaquot.game.HexahackDtos.ProbeType;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HexahackDailyGameProviderTest {
    private final HexahackDailyGameProvider provider = new HexahackDailyGameProvider();

    @BeforeEach
    void configureSeed() {
        provider.seed = "hexahack-test-seed";
    }

    @Test
    void dailyCodeIsDeterministicAndKeepsLeadingZeroes() {
        String first = provider.solutionFor("2026-08-14");
        assertEquals(first, provider.solutionFor("2026-08-14"));
        assertNotEquals(first, provider.solutionFor("2026-08-15"));
        assertEquals(6, first.length());
        provider.validateCode("001122");
        assertThrows(BadRequestException.class, () -> provider.validateCode("1122"));
        assertThrows(BadRequestException.class, () -> provider.validateCode("12a456"));
    }

    @Test
    void freeCluesCountDuplicatesAndEveryProbeReturnsOnlyItsPurchasedAnswer() {
        assertEquals(27, provider.freeClues("001899").totalSum());
        assertEquals(4, provider.freeClues("001899").distinctDigits());

        var below = provider.probe(new ProbeRequest("ping", ProbeType.PING, 1, 5, null), "482731");
        assertEquals(ProbeComparison.BELOW, below.comparison());
        assertEquals(1, below.cost());
        assertEquals(null, below.sum());

        var odd = provider.probe(new ProbeRequest("bit", ProbeType.BIT_SCAN, 2, null, null), "482731");
        assertEquals(Parity.EVEN, odd.parity());

        var link = provider.probe(new ProbeRequest("link", ProbeType.LINK_TRACE, 3, null, 4), "482731");
        assertEquals(ProbeComparison.BELOW, link.comparison());

        var checksum = provider.probe(new ProbeRequest("sum", ProbeType.CHECKSUM, 5, null, 6), "482731");
        assertEquals(4, checksum.sum());
        assertEquals(2, checksum.cost());

        assertThrows(BadRequestException.class,
            () -> provider.probe(new ProbeRequest("same", ProbeType.CHECKSUM, 2, null, 2), "482731"));
    }

    @Test
    void sixChecksumsSolveEveryPossibleCodeWithinTwelveUnits() {
        for (int value = 0; value < 1_000_000; value++) {
            String code = String.format("%06d", value);
            int a = code.charAt(0) - '0';
            int b = code.charAt(1) - '0';
            int c = code.charAt(2) - '0';
            int d = code.charAt(3) - '0';
            int e = code.charAt(4) - '0';
            int f = code.charAt(5) - '0';
            int sum12 = a + b;
            int sum23 = b + c;
            int sum13 = a + c;
            int solvedA = (sum12 + sum13 - sum23) / 2;
            int solvedB = sum12 - solvedA;
            int solvedC = sum13 - solvedA;
            String solved = "%d%d%d%d%d%d".formatted(
                solvedA, solvedB, solvedC, a + d - solvedA, a + e - solvedA, a + f - solvedA
            );
            assertEquals(code, solved);
        }
        assertEquals(12, 6 * 2);
    }
}
