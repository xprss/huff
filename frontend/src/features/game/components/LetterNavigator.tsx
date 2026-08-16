import React from "react";
import type { InputHandPreference } from "../../../types";

const CELL_STEP_PX = 16;
const THUMB_TRAVEL_PX = 84;

export function LetterNavigator({
  answerLength,
  canPlay,
  hand,
  label = "Navigazione lettere",
  selectedCellIndex,
  onSelectCell
}: {
  answerLength: number;
  canPlay: boolean;
  hand: InputHandPreference;
  label?: string;
  selectedCellIndex: number | null;
  onSelectCell: (index: number) => void;
}) {
  const originX = React.useRef(0);
  const pointerId = React.useRef<number | null>(null);
  const startCellIndex = React.useRef(0);
  const lastCellIndex = React.useRef(0);
  const startThumbOffset = React.useRef(0);
  const [dragOffset, setDragOffset] = React.useState(0);
  const [isEngaged, setIsEngaged] = React.useState(false);
  const [isFocused, setIsFocused] = React.useState(false);

  function moveSelection(direction: -1 | 1) {
    if (!canPlay) return;
    const current = selectedCellIndex ?? (direction > 0 ? -1 : answerLength);
    onSelectCell(Math.max(0, Math.min(answerLength - 1, current + direction)));
  }

  function thumbOffsetFor(index: number) {
    if (answerLength <= 1) return 0;
    return (index / (answerLength - 1) - 0.5) * THUMB_TRAVEL_PX;
  }

  function updateDrag(clientX: number) {
    const offset = clientX - originX.current;
    const halfTravel = THUMB_TRAVEL_PX / 2;
    setDragOffset(Math.max(-halfTravel - startThumbOffset.current, Math.min(halfTravel - startThumbOffset.current, offset)));
    const nextCellIndex = Math.max(
      0,
      Math.min(answerLength - 1, startCellIndex.current + Math.round(offset / CELL_STEP_PX))
    );
    if (nextCellIndex === lastCellIndex.current) return;
    lastCellIndex.current = nextCellIndex;
    onSelectCell(nextCellIndex);
  }

  function releasePointer() {
    pointerId.current = null;
    setDragOffset(0);
    setIsEngaged(false);
  }

  function onPointerDown(event: React.PointerEvent<HTMLDivElement>) {
    if (!canPlay || event.button !== 0) return;
    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    originX.current = event.clientX;
    pointerId.current = event.pointerId;
    startCellIndex.current = selectedCellIndex ?? 0;
    lastCellIndex.current = startCellIndex.current;
    startThumbOffset.current = thumbOffsetFor(startCellIndex.current);
    setDragOffset(0);
    setIsEngaged(true);
  }

  function onPointerMove(event: React.PointerEvent<HTMLDivElement>) {
    if (pointerId.current !== event.pointerId) return;
    updateDrag(event.clientX);
  }

  function onKeyDown(event: React.KeyboardEvent<HTMLDivElement>) {
    if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") return;
    event.preventDefault();
    moveSelection(event.key === "ArrowLeft" ? -1 : 1);
  }

  const selectedPosition = selectedCellIndex === null ? 1 : selectedCellIndex + 1;
  const restingThumbOffset = isEngaged ? startThumbOffset.current : thumbOffsetFor(selectedPosition - 1);

  return (
    <div className={`letter-navigator hand-${hand.toLowerCase()}`} aria-label={label}>
      <div
        className={`letter-joystick ${isEngaged || isFocused ? "engaged" : ""}`}
        style={
          {
            "--joystick-rest-offset": `${restingThumbOffset}px`,
            "--joystick-drag-offset": `${dragOffset}px`
          } as React.CSSProperties
        }
        role="slider"
        aria-label="Selettore della casella di input"
        aria-valuemin={1}
        aria-valuemax={answerLength}
        aria-valuenow={selectedPosition}
        aria-valuetext={`Casella ${selectedPosition} di ${answerLength}`}
        aria-disabled={!canPlay}
        tabIndex={canPlay ? 0 : -1}
        onFocus={() => setIsFocused(true)}
        onBlur={() => setIsFocused(false)}
        onKeyDown={onKeyDown}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={releasePointer}
        onPointerCancel={releasePointer}
      >
        <span className="letter-joystick-track" aria-hidden="true" />
        <span className="letter-joystick-thumb" aria-hidden="true" />
      </div>
    </div>
  );
}
