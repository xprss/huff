package dev.huff.hexaquot.game;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HexaflowPuzzleValidatorTest {
    private final HexaflowPuzzleValidator validator = new HexaflowPuzzleValidator();

    @Test void acceptsDisjointFullCoverageAndOppositeSideFlow() {
        assertTrue(validator.validate(validPuzzle()).isEmpty());
    }

    @Test void normalizesAccentsSpacesApostrophesAndDashes() {
        assertEquals("ACQUAPURA", HexaflowPuzzleValidator.normalize("Àcqua-pur'a"));
    }

    @Test void reportsIncompleteDraftWithoutPreventingItsRepresentation() {
        var errors=validator.validate(new HexaflowDtos.PuzzleDraftDto("2026-09-01","",List.of("A"),List.of()));
        assertTrue(errors.stream().anyMatch(e->e.code().equals("GRID_SIZE")));
        assertTrue(errors.stream().anyMatch(e->e.code().equals("FLOW_COUNT")));
        assertTrue(errors.stream().anyMatch(e->e.code().equals("GRID_COVERAGE")));
    }

    @Test void rejectsOverlapJumpsAndFlowWithoutOppositeSides() {
        var valid=validPuzzle();
        var broken=List.of(
            new HexaflowDtos.AnswerDto("flow","AAAAAAAA",HexaflowDtos.AnswerType.FLOW,List.of(7,8,9,10,16,15,14,13)),
            new HexaflowDtos.AnswerDto("theme","AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",HexaflowDtos.AnswerType.THEME,valid.answers().get(1).path())
        );
        var codes=validator.validate(new HexaflowDtos.PuzzleDraftDto(valid.puzzleDate(),valid.themeClue(),valid.grid(),broken)).stream().map(HexaflowDtos.ValidationErrorDto::code).toList();
        assertTrue(codes.contains("CELL_OVERLAP"));
        assertTrue(codes.contains("FLOW_SIDES"));
    }

    private HexaflowDtos.PuzzleDraftDto validPuzzle() {
        List<String> grid=Collections.nCopies(48,"A");
        List<Integer> flow=new ArrayList<>(),theme=new ArrayList<>();
        for(int row=0;row<8;row++){flow.add(row*6);if(row%2==0)for(int col=1;col<6;col++)theme.add(row*6+col);else for(int col=5;col>=1;col--)theme.add(row*6+col);}
        return new HexaflowDtos.PuzzleDraftDto("2026-09-01","Acqua",grid,List.of(
            new HexaflowDtos.AnswerDto("flow","AAAAAAAA",HexaflowDtos.AnswerType.FLOW,flow),
            new HexaflowDtos.AnswerDto("theme","A".repeat(40),HexaflowDtos.AnswerType.THEME,theme)));
    }
}
