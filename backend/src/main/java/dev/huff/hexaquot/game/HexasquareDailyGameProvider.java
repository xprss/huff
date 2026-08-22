package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexasquareDtos.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Builds a deterministic, always-solvable city from a validated family of lane templates. */
@ApplicationScoped
public class HexasquareDailyGameProvider {
    public static final int RULES_VERSION = 1;
    public static final int SIZE = 24;
    private static final int[] START_ROWS = {1, 5, 9, 14, 18, 22};
    private static final int[] TARGET_ROWS = {2, 5, 8, 15, 18, 21};
    private static final String[] NAMES = {"Ada", "Bruno", "Cleo", "Dario", "Elettra", "Furio"};
    private static final String[] EMOJIS = {"🚲", "🚌", "🚶", "🛴", "🚕", "🚋"};

    @ConfigProperty(name = "app.hexasquare.seed") String seed;

    public GeneratedPuzzle puzzleFor(String date) {
        SplittableRandom random = randomFor(date);
        int characterCount = random.nextInt(3, 7);
        List<CharacterDto> characters = new ArrayList<>();
        List<TerminalDto> terminals = new ArrayList<>();
        List<List<CoordinateDto>> witnessPaths = new ArrayList<>();

        for (int index = 0; index < characterCount; index++) {
            String id = "personaggio-" + (index + 1);
            int startRow = START_ROWS[index];
            int targetRow = TARGET_ROWS[index];
            boolean exact = index % 2 == 0;
            int endColumn = exact ? 22 : 13 + index % 3;
            int pivotColumn = Math.min(18, 5 + index * 2);
            List<CoordinateDto> path = routedPath(startRow, targetRow, pivotColumn, endColumn);
            witnessPaths.add(path);

            CoordinateDto start = new CoordinateDto(startRow, 0);
            Quadrant startQuadrant = quadrant(start);
            Quadrant destinationQuadrant = quadrant(path.get(path.size() - 1));
            Quadrant forbidden = startQuadrant == Quadrant.BRUMAVIA || startQuadrant == Quadrant.SOLARIA
                ? (random.nextBoolean() ? Quadrant.VERDOMBRA : Quadrant.LUNARGENTO)
                : (random.nextBoolean() ? Quadrant.BRUMAVIA : Quadrant.SOLARIA);
            if (forbidden == destinationQuadrant) {
                forbidden = startQuadrant == Quadrant.BRUMAVIA || startQuadrant == Quadrant.SOLARIA
                    ? (destinationQuadrant == Quadrant.VERDOMBRA ? Quadrant.LUNARGENTO : Quadrant.VERDOMBRA)
                    : (destinationQuadrant == Quadrant.BRUMAVIA ? Quadrant.SOLARIA : Quadrant.BRUMAVIA);
            }

            GoalDto goal;
            terminals.add(new TerminalDto(id + "-partenza", id, TerminalKind.START, start));
            if (exact) {
                CoordinateDto destination = new CoordinateDto(targetRow, 23);
                goal = new GoalDto(GoalType.EXACT_CELL, destination, null);
                terminals.add(new TerminalDto(id + "-destinazione", id, TerminalKind.DESTINATION, destination));
            } else {
                goal = new GoalDto(GoalType.QUADRANT, null, destinationQuadrant);
            }
            characters.add(new CharacterDto(id, NAMES[index], EMOJIS[index], start, goal, List.of(forbidden)));
        }

        List<IncompatiblePairDto> incompatible = new ArrayList<>();
        incompatible.add(new IncompatiblePairDto(characters.get(0).id(), characters.get(1).id()));
        if (characterCount >= 5) incompatible.add(new IncompatiblePairDto(characters.get(2).id(), characters.get(4).id()));

        List<PlacementDto> witness = placementsFor(witnessPaths, characters);
        Set<CoordinateDto> reserved = new HashSet<>();
        witness.forEach(tile -> reserved.add(new CoordinateDto(tile.row(), tile.column())));
        terminals.forEach(terminal -> reserved.add(terminal.coordinate()));
        List<CoordinateDto> candidates = new ArrayList<>();
        for (int row = 0; row < SIZE; row++) for (int column = 0; column < SIZE; column++) {
            CoordinateDto coordinate = new CoordinateDto(row, column);
            if (!reserved.contains(coordinate)) candidates.add(coordinate);
        }
        shuffle(candidates, random);
        int obstacleCount = 87 + random.nextInt(40); // 15.1–21.9% of the 576 cells.
        List<CoordinateDto> obstacles = new ArrayList<>(candidates.subList(0, obstacleCount));
        obstacles.sort(Comparator.comparingInt(CoordinateDto::row).thenComparingInt(CoordinateDto::column));

        EnumMap<RoadType, Integer> inventory = new EnumMap<>(RoadType.class);
        for (RoadType type : RoadType.values()) inventory.put(type, 0);
        witness.forEach(tile -> inventory.merge(tile.type(), 1, Integer::sum));
        int extras = Math.max(2, Math.min(6, (int) Math.ceil(witness.size() * .10)));
        for (int extra = 0; extra < extras; extra++) {
            RoadType type = RoadType.values()[random.nextInt(RoadType.values().length)];
            inventory.merge(type, 1, Integer::sum);
        }

        SnapshotDto snapshot = new SnapshotDto(SIZE, quadrants(), List.copyOf(obstacles), List.copyOf(terminals),
            List.copyOf(characters), List.copyOf(incompatible), Collections.unmodifiableMap(inventory));
        return new GeneratedPuzzle(snapshot, List.copyOf(witness));
    }

    public record GeneratedPuzzle(SnapshotDto snapshot, List<PlacementDto> witness) {}

    private List<PlacementDto> placementsFor(List<List<CoordinateDto>> paths, List<CharacterDto> characters) {
        Map<CoordinateDto, EnumSet<Direction>> connectors = new HashMap<>();
        for (int index = 0; index < paths.size(); index++) {
            List<CoordinateDto> path = paths.get(index);
            for (int cell = 1; cell < path.size(); cell++) {
                CoordinateDto previous = path.get(cell - 1), current = path.get(cell);
                Direction direction = Direction.between(previous, current);
                connectors.computeIfAbsent(previous, ignored -> EnumSet.noneOf(Direction.class)).add(direction);
                connectors.computeIfAbsent(current, ignored -> EnumSet.noneOf(Direction.class)).add(direction.opposite());
            }
            connectors.computeIfAbsent(path.get(0), ignored -> EnumSet.noneOf(Direction.class)).add(Direction.W);
            // Exact goals connect to their marker; zone goals keep a harmless open connector.
            connectors.computeIfAbsent(path.get(path.size() - 1), ignored -> EnumSet.noneOf(Direction.class)).add(Direction.E);
        }
        return connectors.entrySet().stream()
            .sorted(Map.Entry.<CoordinateDto, EnumSet<Direction>>comparingByKey(
                Comparator.comparingInt(CoordinateDto::row).thenComparingInt(CoordinateDto::column)))
            .map(entry -> tile(entry.getKey(), entry.getValue()))
            .toList();
    }

    private PlacementDto tile(CoordinateDto coordinate, Set<Direction> directions) {
        RoadType type;
        int rotation;
        if (directions.size() == 4) { type = RoadType.CROSS; rotation = 0; }
        else if (directions.size() == 3) {
            type = RoadType.T_JUNCTION;
            if (!directions.contains(Direction.S)) rotation = 0;
            else if (!directions.contains(Direction.W)) rotation = 90;
            else if (!directions.contains(Direction.N)) rotation = 180;
            else rotation = 270;
        } else if (directions.contains(Direction.N) && directions.contains(Direction.S)) {
            type = RoadType.STRAIGHT; rotation = 0;
        } else if (directions.contains(Direction.E) && directions.contains(Direction.W)) {
            type = RoadType.STRAIGHT; rotation = 90;
        } else {
            type = RoadType.CURVE;
            if (directions.containsAll(Set.of(Direction.N, Direction.E))) rotation = 0;
            else if (directions.containsAll(Set.of(Direction.E, Direction.S))) rotation = 90;
            else if (directions.containsAll(Set.of(Direction.S, Direction.W))) rotation = 180;
            else rotation = 270;
        }
        return new PlacementDto(coordinate.row(), coordinate.column(), type, rotation);
    }

    private List<CoordinateDto> routedPath(int startRow, int targetRow, int pivotColumn, int endColumn) {
        List<CoordinateDto> path = new ArrayList<>();
        for (int column = 1; column <= pivotColumn; column++) path.add(new CoordinateDto(startRow, column));
        int step = Integer.compare(targetRow, startRow);
        for (int row = startRow + step; step != 0 && row != targetRow + step; row += step) path.add(new CoordinateDto(row, pivotColumn));
        for (int column = pivotColumn + 1; column <= endColumn; column++) path.add(new CoordinateDto(targetRow, column));
        return List.copyOf(path);
    }

    private List<QuadrantDto> quadrants() {
        return List.of(
            new QuadrantDto(Quadrant.BRUMAVIA, "Brumavia", 0, 11, 0, 11),
            new QuadrantDto(Quadrant.SOLARIA, "Solaria", 0, 11, 12, 23),
            new QuadrantDto(Quadrant.VERDOMBRA, "Verdombra", 12, 23, 0, 11),
            new QuadrantDto(Quadrant.LUNARGENTO, "Lunargento", 12, 23, 12, 23)
        );
    }

    static Quadrant quadrant(CoordinateDto coordinate) {
        if (coordinate.row() < 12) return coordinate.column() < 12 ? Quadrant.BRUMAVIA : Quadrant.SOLARIA;
        return coordinate.column() < 12 ? Quadrant.VERDOMBRA : Quadrant.LUNARGENTO;
    }

    private SplittableRandom randomFor(String date) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                (seed + ":" + date + ":v" + RULES_VERSION).getBytes(StandardCharsets.UTF_8));
            return new SplittableRandom(ByteBuffer.wrap(digest).getLong());
        } catch (Exception error) { throw new IllegalStateException("Cannot derive Hexasquare puzzle", error); }
    }

    private void shuffle(List<CoordinateDto> values, SplittableRandom random) {
        for (int index = values.size() - 1; index > 0; index--) {
            int other = random.nextInt(index + 1);
            CoordinateDto value = values.get(index); values.set(index, values.get(other)); values.set(other, value);
        }
    }

    enum Direction {
        N(-1,0), E(0,1), S(1,0), W(0,-1);
        final int rowDelta, columnDelta;
        Direction(int rowDelta, int columnDelta) { this.rowDelta=rowDelta; this.columnDelta=columnDelta; }
        Direction opposite() { return values()[(ordinal()+2)%4]; }
        static Direction between(CoordinateDto from, CoordinateDto to) {
            for (Direction direction : values()) if (from.row()+direction.rowDelta==to.row() && from.column()+direction.columnDelta==to.column()) return direction;
            throw new IllegalArgumentException("Non-adjacent path cells");
        }
    }
}
