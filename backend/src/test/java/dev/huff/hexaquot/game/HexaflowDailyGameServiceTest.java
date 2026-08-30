package dev.huff.hexaquot.game;

import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.persistence.HexaflowPuzzleEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class HexaflowDailyGameServiceTest {
    @Inject HexaflowDailyGameService service;
    @Inject HexaflowPuzzleService puzzles;
    @Inject DailyGameService clock;

    @Test @TestTransaction
    void supportsReverseSolutionsUniqueExtrasCompletionAndReplay() {
        HexaflowDtos.PuzzleDraftDto puzzle=createPuzzle();
        AppUser user=createUser();
        var initial=service.today(user);
        assertTrue(initial.available());
        assertNull(initial.game());
        assertEquals(2,initial.totalAnswers());

        List<Integer> reverseFlow=reversed(puzzle.answers().get(0).path());
        var flow=service.submitPath(user,new HexaflowDtos.PathRequest("flow",reverseFlow));
        assertEquals(HexaflowDtos.PathOutcome.FLOW,flow.result().outcome());
        assertTrue(service.submitPath(user,new HexaflowDtos.PathRequest("flow",reverseFlow)).replayed());
        assertEquals(1,service.today(user).game().foundAnswers().size());

        var firstExtra=service.submitPath(user,new HexaflowDtos.PathRequest("extra-1",List.of(1,2,3,4)));
        assertEquals(HexaflowDtos.PathOutcome.EXTRA,firstExtra.result().outcome());
        var duplicate=service.submitPath(user,new HexaflowDtos.PathRequest("extra-duplicate",List.of(1,2,3,4)));
        assertEquals(HexaflowDtos.PathOutcome.DUPLICATE,duplicate.result().outcome());
        service.submitPath(user,new HexaflowDtos.PathRequest("extra-2",List.of(7,8,9,10)));
        var thirdExtra=service.submitPath(user,new HexaflowDtos.PathRequest("extra-3",List.of(13,14,15,16)));
        assertEquals(3,thirdExtra.game().extraCount());

        var completed=service.submitPath(user,new HexaflowDtos.PathRequest("theme",reversed(puzzle.answers().get(1).path())));
        assertEquals(HexaflowDtos.GameStatus.COMPLETED,completed.game().status());
        assertEquals(2,completed.game().foundAnswers().size());
        var stats=service.stats(user);
        assertEquals(1,stats.started());assertEquals(1,stats.completed());
    }

    private HexaflowDtos.PuzzleDraftDto createPuzzle() {
        List<String> grid=new ArrayList<>();for(int i=0;i<48;i++)grid.add(String.valueOf((char)('A'+i%26)));
        List<Integer> flow=new ArrayList<>(),theme=new ArrayList<>();
        for(int row=0;row<8;row++){flow.add(row*6);if(row%2==0)for(int col=1;col<6;col++)theme.add(row*6+col);else for(int col=5;col>=1;col--)theme.add(row*6+col);}
        List<HexaflowDtos.AnswerDto> answers=List.of(
            new HexaflowDtos.AnswerDto("flow",word(grid,flow),HexaflowDtos.AnswerType.FLOW,flow),
            new HexaflowDtos.AnswerDto("theme",word(grid,theme),HexaflowDtos.AnswerType.THEME,theme));
        String now=Instant.now().toString();HexaflowPuzzleEntity entity=new HexaflowPuzzleEntity();entity.id=UUID.randomUUID().toString();entity.puzzleDate=clock.todayDate();entity.status=HexaflowDtos.PuzzleStatus.PUBLISHED;entity.themeClue="Alfabeto";entity.gridJson=puzzles.write(grid);entity.answersJson=puzzles.write(answers);entity.createdBy="test";entity.updatedBy="test";entity.publishedBy="test";entity.createdAt=now;entity.updatedAt=now;entity.publishedAt=now;entity.persist();
        return new HexaflowDtos.PuzzleDraftDto(entity.puzzleDate,entity.themeClue,grid,answers);
    }
    private AppUser createUser(){UserEntity e=new UserEntity();e.id="flow-"+UUID.randomUUID();e.displayName="Flow";e.nickname="@flow-"+UUID.randomUUID().toString().substring(0,8);e.profileEmoji="🌊";e.createdAt=Instant.now().toString();e.starAvailable=false;e.persist();return e.toAppUser(false);}
    private List<Integer> reversed(List<Integer> source){List<Integer> result=new ArrayList<>(source);Collections.reverse(result);return result;}
    private String word(List<String>grid,List<Integer>path){StringBuilder result=new StringBuilder();path.forEach(i->result.append(grid.get(i)));return result.toString();}
}
