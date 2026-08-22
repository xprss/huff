package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexasquareDtos.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

import java.util.*;

/** Deterministic shortest-path and conflict-based search over a submitted road graph. */
@ApplicationScoped
public class HexasquareSolver {
    private static final int MAX_CBS_NODES = 20_000;

    public List<PlacementDto> validatePlacements(SnapshotDto puzzle, List<PlacementDto> placements) {
        if (placements == null) throw new BadRequestException("La disposizione è obbligatoria.");
        if (placements.size() > puzzle.size() * puzzle.size()) throw new BadRequestException("La disposizione è troppo grande.");
        Set<CoordinateDto> blocked = new HashSet<>(puzzle.obstacles());
        puzzle.terminals().forEach(terminal -> blocked.add(terminal.coordinate()));
        Set<CoordinateDto> occupied = new HashSet<>();
        EnumMap<RoadType,Integer> used = new EnumMap<>(RoadType.class);
        List<PlacementDto> validated = new ArrayList<>();
        for (PlacementDto placement : placements) {
            if (placement == null || placement.type() == null) throw new BadRequestException("Casella stradale non valida.");
            if (placement.row() < 0 || placement.row() >= puzzle.size() || placement.column() < 0 || placement.column() >= puzzle.size()) {
                throw new BadRequestException("Coordinate fuori dalla griglia.");
            }
            if (placement.rotation() != 0 && placement.rotation() != 90 && placement.rotation() != 180 && placement.rotation() != 270) {
                throw new BadRequestException("La rotazione deve essere 0, 90, 180 o 270 gradi.");
            }
            CoordinateDto coordinate = new CoordinateDto(placement.row(), placement.column());
            if (!occupied.add(coordinate)) throw new BadRequestException("Una cella può contenere una sola strada.");
            if (blocked.contains(coordinate)) throw new BadRequestException("Non puoi costruire su ostacoli o terminali.");
            int count = used.merge(placement.type(), 1, Integer::sum);
            if (count > puzzle.inventory().getOrDefault(placement.type(), 0)) {
                throw new BadRequestException("Inventario " + placement.type() + " superato.");
            }
            validated.add(new PlacementDto(placement.row(), placement.column(), placement.type(), placement.rotation()));
        }
        validated.sort(Comparator.comparingInt(PlacementDto::row).thenComparingInt(PlacementDto::column));
        return List.copyOf(validated);
    }

    public SimulationResultDto solve(SnapshotDto puzzle, String requestId, List<PlacementDto> validated) {
        Map<CoordinateDto,PlacementDto> roads = new HashMap<>();
        validated.forEach(tile -> roads.put(new CoordinateDto(tile.row(), tile.column()), tile));
        Map<String,List<CoordinateDto>> rootPaths = new LinkedHashMap<>();
        List<CharacterOutcomeDto> failures = new ArrayList<>();

        for (CharacterDto character : sortedCharacters(puzzle)) {
            List<CoordinateDto> path = shortestPath(character, roads, Set.of(), false);
            if (path == null) {
                CharacterResult reason = shortestPath(character, roads, Set.of(), true) == null
                    ? CharacterResult.UNREACHABLE : CharacterResult.FORBIDDEN_QUADRANT;
                failures.add(new CharacterOutcomeDto(character.id(), reason, List.of()));
            } else rootPaths.put(character.id(), path);
        }
        if (!failures.isEmpty()) {
            Set<String> failedIds = new HashSet<>(); failures.forEach(outcome -> failedIds.add(outcome.characterId()));
            for (CharacterDto character : sortedCharacters(puzzle)) if (!failedIds.contains(character.id())) {
                failures.add(new CharacterOutcomeDto(character.id(), CharacterResult.REACHED, List.of()));
            }
            failures.sort(Comparator.comparing(CharacterOutcomeDto::characterId));
            return result(requestId, false, failures, List.of(), validated.size(), remaining(puzzle, validated));
        }

        Map<String,List<CoordinateDto>> solution = conflictBasedSearch(puzzle, roads, rootPaths);
        if (solution == null) {
            Map<String,SortedSet<String>> conflicts = conflictingCharacters(puzzle, rootPaths);
            List<CharacterOutcomeDto> outcomes = sortedCharacters(puzzle).stream().map(character -> {
                List<String> others = List.copyOf(conflicts.getOrDefault(character.id(), new TreeSet<>()));
                return new CharacterOutcomeDto(character.id(), others.isEmpty() ? CharacterResult.REACHED : CharacterResult.CONFLICT, others);
            }).toList();
            return result(requestId, false, outcomes, List.of(), validated.size(), remaining(puzzle, validated));
        }
        List<PathDto> paths = sortedCharacters(puzzle).stream()
            .map(character -> new PathDto(character.id(), List.copyOf(solution.get(character.id())))).toList();
        List<CharacterOutcomeDto> outcomes = sortedCharacters(puzzle).stream()
            .map(character -> new CharacterOutcomeDto(character.id(), CharacterResult.REACHED, List.of())).toList();
        return result(requestId, true, outcomes, paths, validated.size(), remaining(puzzle, validated));
    }

    private SimulationResultDto result(String requestId, boolean success, List<CharacterOutcomeDto> outcomes,
                                       List<PathDto> paths, int used, int remaining) {
        return new SimulationResultDto(requestId, success, List.copyOf(outcomes), List.copyOf(paths), used, remaining);
    }

    private Map<String,List<CoordinateDto>> conflictBasedSearch(SnapshotDto puzzle,
                                                                Map<CoordinateDto,PlacementDto> roads,
                                                                Map<String,List<CoordinateDto>> initial) {
        Comparator<SearchNode> comparator = Comparator.comparingInt(SearchNode::cost).thenComparingLong(SearchNode::sequence);
        PriorityQueue<SearchNode> open = new PriorityQueue<>(comparator);
        long sequence = 0;
        open.add(new SearchNode(Map.of(), copyPaths(initial), cost(initial), sequence++));
        Set<String> visited = new HashSet<>();
        int expanded = 0;
        while (!open.isEmpty() && expanded++ < MAX_CBS_NODES) {
            SearchNode node = open.remove();
            Conflict conflict = firstConflict(puzzle, node.paths());
            if (conflict == null) return node.paths();
            for (String characterId : List.of(conflict.first(), conflict.second())) {
                Map<String,Set<CoordinateDto>> constraints = copyConstraints(node.constraints());
                constraints.computeIfAbsent(characterId, ignored -> new HashSet<>()).add(conflict.cell());
                String signature = constraintSignature(constraints);
                if (!visited.add(signature)) continue;
                CharacterDto character = puzzle.characters().stream().filter(value -> value.id().equals(characterId)).findFirst().orElseThrow();
                List<CoordinateDto> replacement = shortestPath(character, roads, constraints.get(characterId), false);
                if (replacement == null) continue;
                Map<String,List<CoordinateDto>> paths = copyPaths(node.paths());
                paths.put(characterId, replacement);
                open.add(new SearchNode(constraints, paths, cost(paths), sequence++));
            }
        }
        return null;
    }

    private List<CoordinateDto> shortestPath(CharacterDto character, Map<CoordinateDto,PlacementDto> roads,
                                             Set<CoordinateDto> constraints, boolean ignoreForbiddenQuadrants) {
        ArrayDeque<CoordinateDto> queue = new ArrayDeque<>();
        Map<CoordinateDto,CoordinateDto> parent = new HashMap<>();
        for (Direction direction : Direction.values()) {
            CoordinateDto neighbor = move(character.start(), direction);
            PlacementDto road = roads.get(neighbor);
            if (road == null || !connectors(road).contains(direction.opposite())) continue;
            if (!allowed(character, neighbor, constraints, ignoreForbiddenQuadrants)) continue;
            if (parent.putIfAbsent(neighbor, neighbor) == null) queue.add(neighbor);
        }
        while (!queue.isEmpty()) {
            CoordinateDto current = queue.remove();
            if (isGoal(character.goal(), current, roads.get(current))) return reconstruct(parent, current);
            for (Direction direction : Direction.values()) { // Enum declaration is the canonical N/E/S/W order.
                CoordinateDto neighbor = move(current, direction);
                PlacementDto currentRoad = roads.get(current), nextRoad = roads.get(neighbor);
                if (nextRoad == null || !connectors(currentRoad).contains(direction) || !connectors(nextRoad).contains(direction.opposite())) continue;
                if (parent.containsKey(neighbor) || !allowed(character, neighbor, constraints, ignoreForbiddenQuadrants)) continue;
                parent.put(neighbor, current); queue.add(neighbor);
            }
        }
        return null;
    }

    private boolean allowed(CharacterDto character, CoordinateDto coordinate, Set<CoordinateDto> constraints,
                            boolean ignoreForbiddenQuadrants) {
        return !constraints.contains(coordinate) && (ignoreForbiddenQuadrants || !character.forbiddenQuadrants().contains(HexasquareDailyGameProvider.quadrant(coordinate)));
    }

    private boolean isGoal(GoalDto goal, CoordinateDto road, PlacementDto tile) {
        if (goal.type() == GoalType.QUADRANT) return HexasquareDailyGameProvider.quadrant(road) == goal.quadrant();
        for (Direction direction : Direction.values()) {
            if (move(road, direction).equals(goal.coordinate())) return connectors(tile).contains(direction);
        }
        return false;
    }

    private List<CoordinateDto> reconstruct(Map<CoordinateDto,CoordinateDto> parent, CoordinateDto goal) {
        List<CoordinateDto> reversed = new ArrayList<>();
        CoordinateDto current = goal;
        while (true) {
            reversed.add(current);
            CoordinateDto previous = parent.get(current);
            if (previous.equals(current)) break;
            current = previous;
        }
        Collections.reverse(reversed);
        return List.copyOf(reversed);
    }

    private Conflict firstConflict(SnapshotDto puzzle, Map<String,List<CoordinateDto>> paths) {
        return puzzle.incompatiblePairs().stream()
            .sorted(Comparator.comparing(IncompatiblePairDto::firstCharacterId).thenComparing(IncompatiblePairDto::secondCharacterId))
            .map(pair -> {
                Set<CoordinateDto> second = new HashSet<>(paths.get(pair.secondCharacterId()));
                return paths.get(pair.firstCharacterId()).stream().filter(second::contains).findFirst()
                    .map(cell -> new Conflict(pair.firstCharacterId(), pair.secondCharacterId(), cell)).orElse(null);
            }).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private Map<String,SortedSet<String>> conflictingCharacters(SnapshotDto puzzle, Map<String,List<CoordinateDto>> paths) {
        Map<String,SortedSet<String>> conflicts = new HashMap<>();
        for (IncompatiblePairDto pair : puzzle.incompatiblePairs()) {
            if (Collections.disjoint(paths.get(pair.firstCharacterId()), paths.get(pair.secondCharacterId()))) continue;
            conflicts.computeIfAbsent(pair.firstCharacterId(), ignored -> new TreeSet<>()).add(pair.secondCharacterId());
            conflicts.computeIfAbsent(pair.secondCharacterId(), ignored -> new TreeSet<>()).add(pair.firstCharacterId());
        }
        return conflicts;
    }

    private Set<Direction> connectors(PlacementDto placement) {
        return switch (placement.type()) {
            case CROSS -> EnumSet.allOf(Direction.class);
            case STRAIGHT -> placement.rotation() % 180 == 0 ? EnumSet.of(Direction.N,Direction.S) : EnumSet.of(Direction.E,Direction.W);
            case CURVE -> switch (placement.rotation()) {
                case 0 -> EnumSet.of(Direction.N,Direction.E);
                case 90 -> EnumSet.of(Direction.E,Direction.S);
                case 180 -> EnumSet.of(Direction.S,Direction.W);
                default -> EnumSet.of(Direction.W,Direction.N);
            };
            case T_JUNCTION -> switch (placement.rotation()) {
                case 0 -> EnumSet.of(Direction.N,Direction.E,Direction.W);
                case 90 -> EnumSet.of(Direction.N,Direction.E,Direction.S);
                case 180 -> EnumSet.of(Direction.E,Direction.S,Direction.W);
                default -> EnumSet.of(Direction.N,Direction.S,Direction.W);
            };
        };
    }

    private int remaining(SnapshotDto puzzle, List<PlacementDto> placements) {
        return puzzle.inventory().values().stream().mapToInt(Integer::intValue).sum() - placements.size();
    }
    private int cost(Map<String,List<CoordinateDto>> paths) { return paths.values().stream().mapToInt(List::size).sum(); }
    private List<CharacterDto> sortedCharacters(SnapshotDto puzzle) { return puzzle.characters().stream().sorted(Comparator.comparing(CharacterDto::id)).toList(); }
    private Map<String,List<CoordinateDto>> copyPaths(Map<String,List<CoordinateDto>> source) { Map<String,List<CoordinateDto>> copy=new LinkedHashMap<>(); source.forEach((key,value)->copy.put(key,List.copyOf(value))); return copy; }
    private Map<String,Set<CoordinateDto>> copyConstraints(Map<String,Set<CoordinateDto>> source) { Map<String,Set<CoordinateDto>> copy=new HashMap<>(); source.forEach((key,value)->copy.put(key,new HashSet<>(value))); return copy; }
    private String constraintSignature(Map<String,Set<CoordinateDto>> constraints) { List<String> entries=new ArrayList<>(); constraints.forEach((id,cells)->cells.forEach(cell->entries.add(id+":"+cell.row()+":"+cell.column()))); Collections.sort(entries); return String.join("|",entries); }
    private CoordinateDto move(CoordinateDto coordinate, Direction direction) { return new CoordinateDto(coordinate.row()+direction.rowDelta,coordinate.column()+direction.columnDelta); }

    private enum Direction { N(-1,0),E(0,1),S(1,0),W(0,-1); final int rowDelta,columnDelta; Direction(int rowDelta,int columnDelta){this.rowDelta=rowDelta;this.columnDelta=columnDelta;} Direction opposite(){return values()[(ordinal()+2)%4];} }
    private record Conflict(String first,String second,CoordinateDto cell) {}
    private record SearchNode(Map<String,Set<CoordinateDto>> constraints, Map<String,List<CoordinateDto>> paths, int cost, long sequence) {}
}
