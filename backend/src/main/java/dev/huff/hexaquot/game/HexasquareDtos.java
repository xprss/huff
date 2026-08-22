package dev.huff.hexaquot.game;

import java.util.List;
import java.util.Map;

public final class HexasquareDtos {
    private HexasquareDtos() {}

    public enum Status { IN_PROGRESS, COMPLETED }
    public enum RoadType { STRAIGHT, CURVE, T_JUNCTION, CROSS }
    public enum Quadrant { BRUMAVIA, SOLARIA, VERDOMBRA, LUNARGENTO }
    public enum TerminalKind { START, DESTINATION }
    public enum GoalType { EXACT_CELL, QUADRANT }
    public enum CharacterResult { REACHED, UNREACHABLE, FORBIDDEN_QUADRANT, CONFLICT }

    public record CoordinateDto(int row, int column) {}
    public record QuadrantDto(Quadrant id, String name, int rowStart, int rowEnd, int columnStart, int columnEnd) {}
    public record TerminalDto(String id, String characterId, TerminalKind kind, CoordinateDto coordinate) {}
    public record GoalDto(GoalType type, CoordinateDto coordinate, Quadrant quadrant) {}
    public record CharacterDto(String id, String name, String emoji, CoordinateDto start, GoalDto goal,
                               List<Quadrant> forbiddenQuadrants) {}
    public record IncompatiblePairDto(String firstCharacterId, String secondCharacterId) {}
    public record PlacementDto(int row, int column, RoadType type, int rotation) {}
    public record PathDto(String characterId, List<CoordinateDto> cells) {}
    public record SnapshotDto(int size, List<QuadrantDto> quadrants, List<CoordinateDto> obstacles,
                              List<TerminalDto> terminals, List<CharacterDto> characters,
                              List<IncompatiblePairDto> incompatiblePairs, Map<RoadType, Integer> inventory) {}
    public record GameDto(String puzzleDate, int rulesVersion, Status status, List<PlacementDto> lastPlacements,
                          int simulationsCount, Integer usedCells, Integer remainingCells,
                          List<PathDto> paths, String completedAt) {}
    public record TodayDto(String puzzleDate, int rulesVersion, int size, List<QuadrantDto> quadrants,
                           List<CoordinateDto> obstacles, List<TerminalDto> terminals,
                           List<CharacterDto> characters, List<IncompatiblePairDto> incompatiblePairs,
                           Map<RoadType, Integer> inventory, GameDto game) {}
    public record SimulationRequestDto(String requestId, List<PlacementDto> placements) {}
    public record CharacterOutcomeDto(String characterId, CharacterResult result, List<String> conflictsWith) {}
    public record SimulationResultDto(String requestId, boolean success, List<CharacterOutcomeDto> characters,
                                      List<PathDto> paths, int usedCells, int remainingCells) {}
    public record SimulationActionDto(GameDto game, SimulationResultDto result, boolean replayed) {}
    public record StatsDto(int gamesStarted, int completed, double completionPercentage, int currentStreak,
                           int maxStreak, double averageCellsUsed, double averageCellsSaved,
                           int bestCellsSaved, double averageSimulationsPerWin) {}
}
