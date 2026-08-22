package dev.huff.hexaquot.game;

import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexasquareDtos.*;
import dev.huff.hexaquot.persistence.HexasquareSimulationEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class HexasquareDailyGameServiceTest {
    @Inject HexasquareDailyGameService service;
    @Inject HexasquareDailyGameProvider provider;
    @Inject DailyGameService clock;

    @Test @TestTransaction
    void failedAttemptsAreUnlimitedReplayIsIdempotentAndWitnessCompletes() {
        AppUser user=user();
        SimulationActionDto first=service.simulate(user,new SimulationRequestDto("empty-1",List.of()));
        assertFalse(first.result().success()); assertEquals(Status.IN_PROGRESS,first.game().status());
        SimulationActionDto replay=service.simulate(user,new SimulationRequestDto("empty-1",List.of()));
        assertTrue(replay.replayed()); assertEquals(1,replay.game().simulationsCount());
        for(int attempt=2;attempt<=5;attempt++) service.simulate(user,new SimulationRequestDto("empty-"+attempt,List.of()));
        assertEquals(5,service.today(user).game().simulationsCount());

        List<PlacementDto> witness=provider.puzzleFor(clock.todayDate()).witness();
        SimulationActionDto win=service.simulate(user,new SimulationRequestDto("win",witness));
        assertTrue(win.result().success()); assertEquals(Status.COMPLETED,win.game().status());
        assertEquals(6,win.game().simulationsCount()); assertFalse(win.game().paths().isEmpty());
        assertEquals(6,HexasquareSimulationEntity.count("gameId",findGameId(user.id())));
        assertEquals(1,service.stats(user).completed());
        WebApplicationException conflict=assertThrows(WebApplicationException.class,
            ()->service.simulate(user,new SimulationRequestDto("after-win",witness)));
        assertEquals(409,conflict.getResponse().getStatus());
        assertTrue(service.simulate(user,new SimulationRequestDto("empty-1",List.of())).replayed());
    }

    private String findGameId(String userId) {
        return dev.huff.hexaquot.persistence.HexasquareGameEntity.<dev.huff.hexaquot.persistence.HexasquareGameEntity>find("userId",userId).firstResult().id;
    }
    private AppUser user(){UserEntity entity=new UserEntity();entity.id="hexasquare-test-"+UUID.randomUUID();entity.displayName="Square";entity.nickname="@square-"+UUID.randomUUID().toString().substring(0,8);entity.profileEmoji="🏙️";entity.createdAt=Instant.now().toString();entity.starAvailable=false;entity.persist();return entity.toAppUser(false);}
}
