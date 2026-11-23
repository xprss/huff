package dev.huff.hexaquot.game;

import dev.huff.hexaquot.auth.AdminPrivileges;
import dev.huff.hexaquot.auth.AppUser;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class HexatrapAdminServiceTest {
    @Inject HexatrapAdminService service;
    @Inject HexatrapCaseStore store;
    @Inject DailyGameService clock;

    private final AppUser admin = new AppUser("admin", null, "Admin", "@admin", null, null, null, true,
        new AdminPrivileges(true, true, true, true, true, true));
    private final AppUser hexaflowOnly = new AppUser("flow", null, "Flow", "@flow", null, null, null, true,
        new AdminPrivileges(true, true, false, false, true, false));

    @BeforeEach
    void installTodaysCase() {
        HexatrapTestCases.install(store, clock.todayDate());
    }

    @Test
    void requiresTheHexatrapPrivilege() {
        assertThrows(ForbiddenException.class, () -> service.list(hexaflowOnly, null, null));
        assertThrows(ForbiddenException.class, () -> service.detail(null, clock.todayDate()));
        assertThrows(ForbiddenException.class, () -> service.generate(hexaflowOnly, clock.todayDate()));
    }

    @Test
    void showsTheCaseWithItsSolutionRecordsAndMetrics() {
        String today = clock.todayDate();
        var summary = service.list(admin, null, null).stream().filter(c -> c.puzzleDate().equals(today)).findFirst().orElseThrow();
        assertEquals("READY", summary.status());
        assertEquals(10, summary.metrics().path("certificateCost").asInt());
        var detail = service.detail(admin, today);
        assertEquals("B", detail.caseFile().path("solution").path("liar").asText());
        assertEquals("Se la porta della torre era chiusa, allora è stato Ettore a spegnere la lanterna", detail.records().get("k_chiave"));
        assertThrows(jakarta.ws.rs.NotFoundException.class, () -> service.detail(admin, "2001-01-01"));
        assertThrows(jakarta.ws.rs.BadRequestException.class, () -> service.detail(admin, "domani"));
    }

    @Test
    void regeneratesOnlyUpcomingUnplayedDatesWithAConfiguredAgent() {
        String yesterday = LocalDate.parse(clock.todayDate()).minusDays(1).toString();
        assertEquals(409, assertThrows(WebApplicationException.class, () -> service.generate(admin, yesterday)).getResponse().getStatus());
        String farAway = LocalDate.parse(clock.todayDate()).plusDays(30).toString();
        assertEquals(409, assertThrows(WebApplicationException.class, () -> service.generate(admin, farAway)).getResponse().getStatus());
        String tomorrow = LocalDate.parse(clock.todayDate()).plusDays(1).toString();
        // The test profile has no agent token.
        assertEquals(503, assertThrows(WebApplicationException.class, () -> service.generate(admin, tomorrow)).getResponse().getStatus());
    }
}
