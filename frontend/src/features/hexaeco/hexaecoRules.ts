import type { HexaecoBoardDto, HexaecoCommand, HexaecoTodayDto } from "../../types";

export const COMMANDS: readonly HexaecoCommand[] = ["UP", "RIGHT", "DOWN", "LEFT", "WAIT"];
export const COMMAND_LABELS: Record<HexaecoCommand, string> = { UP: "Su", RIGHT: "Destra", DOWN: "Giù", LEFT: "Sinistra", WAIT: "Aspetta" };
export const COMMAND_GLYPHS: Record<HexaecoCommand, string> = { UP: "↑", RIGHT: "→", DOWN: "↓", LEFT: "←", WAIT: "Ⅱ" };

export interface EcoState {
  readonly light: number;
  readonly echo: number;
  readonly queue: readonly [HexaecoCommand, HexaecoCommand];
}

export function initialState(board: HexaecoBoardDto): EcoState {
  return { light: board.lightStart, echo: board.echoStart, queue: ["WAIT", "WAIT"] };
}

export function destination(board: HexaecoBoardDto, position: number, command: HexaecoCommand) {
  const x = position % 5 + (command === "RIGHT" ? 1 : command === "LEFT" ? -1 : 0);
  const y = Math.floor(position / 5) + (command === "DOWN" ? 1 : command === "UP" ? -1 : 0);
  const target = y * 5 + x;
  const blocked = x < 0 || x >= 5 || y < 0 || y >= 5 || board.walls.includes(target);
  return { position: blocked ? position : target, blocked, wall: blocked && x >= 0 && x < 5 && y >= 0 && y < 5 ? target : null };
}

export function step(board: HexaecoBoardDto, state: EcoState, command: HexaecoCommand): EcoState {
  return {
    light: destination(board, state.light, command).position,
    echo: destination(board, state.echo, state.queue[0]).position,
    queue: [state.queue[1], command]
  };
}

export function hasWon(board: HexaecoBoardDto, state: EcoState) {
  return state.light === board.lightGoal && state.echo === board.echoGoal;
}

export function replay(board: HexaecoBoardDto, moves: readonly HexaecoCommand[]) {
  return moves.reduce((state, command) => step(board, state, command), initialState(board));
}

/** Remove complete state cycles only for transport, preserving the player's undo history. */
export function compactSolution(board: HexaecoBoardDto, moves: readonly HexaecoCommand[]) {
  const stateKey = (state: EcoState) => `${state.light}:${state.echo}:${state.queue.join(":")}`;
  let state = initialState(board);
  const keys = [stateKey(state)];
  const seen = new Map([[keys[0], 0]]);
  const compact: HexaecoCommand[] = [];
  for (const command of moves) {
    state = step(board, state, command);
    const key = stateKey(state);
    const previous = seen.get(key);
    if (previous !== undefined) {
      for (const removed of keys.splice(previous + 1)) seen.delete(removed);
      compact.length = previous;
    } else {
      compact.push(command);
      keys.push(key);
      seen.set(key, compact.length);
    }
  }
  return compact;
}

export function draftKey(userId: string, today: Pick<HexaecoTodayDto, "puzzleDate" | "rulesVersion">) {
  return `huff.hexaeco.draft.${encodeURIComponent(userId)}.${today.puzzleDate}.v${today.rulesVersion}`;
}

export function parseDraft(raw: string | null, board: HexaecoBoardDto): readonly HexaecoCommand[] {
  try {
    const draft = JSON.parse(raw ?? "null");
    if (draft?.board !== JSON.stringify(board) || !Array.isArray(draft.moves)) return [];
    let state = initialState(board);
    for (const command of draft.moves) {
      if (!COMMANDS.includes(command) || hasWon(board, state)) return [];
      state = step(board, state, command);
    }
    return draft.moves;
  } catch { return []; }
}

export function positionLabel(position: number) {
  return `riga ${Math.floor(position / 5) + 1}, colonna ${position % 5 + 1}`;
}
