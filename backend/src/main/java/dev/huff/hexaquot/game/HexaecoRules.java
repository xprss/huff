package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexaecoDtos.BoardDto;
import dev.huff.hexaquot.game.HexaecoDtos.Command;
import java.util.List;

/** Both lights move simultaneously. A blocked light still consumes its command. */
public final class HexaecoRules {
    public static final int SIDE = 5;
    public static final int RULES_VERSION = 1;
    public static final int MAX_MOVES = 20_000;

    private HexaecoRules() {}

    public record State(int light, int echo, Command next, Command later) {}

    public static State initial(BoardDto board) {
        return new State(board.lightStart(), board.echoStart(), Command.WAIT, Command.WAIT);
    }

    public static State step(BoardDto board, State state, Command command) {
        return new State(move(board, state.light(), command), move(board, state.echo(), state.next()), state.later(), command);
    }

    public static boolean won(BoardDto board, State state) {
        return state.light() == board.lightGoal() && state.echo() == board.echoGoal();
    }

    public static boolean solves(BoardDto board, List<Command> commands) {
        State state = initial(board);
        for (int index = 0; index < commands.size(); index++) {
            state = step(board, state, commands.get(index));
            // Play ends at the first simultaneous arrival, even with pending commands.
            if (won(board, state)) return index == commands.size() - 1;
        }
        return false;
    }

    private static int move(BoardDto board, int position, Command command) {
        int x = position % SIDE;
        int y = position / SIDE;
        switch (command) {
            case UP -> y--;
            case RIGHT -> x++;
            case DOWN -> y++;
            case LEFT -> x--;
            case WAIT -> { return position; }
        }
        if (x < 0 || x >= SIDE || y < 0 || y >= SIDE) return position;
        int destination = y * SIDE + x;
        return board.walls().contains(destination) ? position : destination;
    }
}
