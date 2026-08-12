package dev.huff.hexaquot.game;

import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.persistence.HexadigitGameEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class HexadigitDailyGameServiceTest {
    @Inject HexadigitDailyGameService service;
    @Inject HexadigitDailyGameProvider provider;
    @Inject DailyGameService clock;

    @Test @TestTransaction
    void createsOnlyAfterAValidGuessAndCanWinWithTheDailyCode() {
        AppUser user = user();
        assertNull(service.today(user).game());
        assertThrows(BadRequestException.class, () -> service.guess(user, "123"));
        assertEquals(0, HexadigitGameEntity.count("userId", user.id()));

        String solution = provider.solutionFor(clock.todayDate());
        HexadigitGameDto result = service.guess(user, solution);
        assertEquals(GameStatus.WON, result.status());
        assertEquals(solution, result.solution());
        assertEquals(1, result.guesses().size());
    }

    private AppUser user() {
        return new AppUser("hexadigit-test-" + UUID.randomUUID(), null, "Digit", "@digit", "😀", null, false);
    }
}
