import React from "react";
import { Minus, Plus } from "lucide-react";

const OUTPUT_SIZE = 512;
const MAX_ZOOM = 4;

type Point = { x: number; y: number };

/**
 * Lets the user frame a photo inside a circle: drag (finger or mouse) to move,
 * slider / pinch / wheel to zoom, arrow keys and +/- from the keyboard. The
 * framed square is exported as a 512×512 JPEG.
 */
export function AvatarCropper({
  src,
  onCancel,
  onConfirm
}: {
  src: string;
  onCancel: () => void;
  onConfirm: (photo: Blob) => void;
}) {
  const viewportRef = React.useRef<HTMLDivElement | null>(null);
  const imageRef = React.useRef<HTMLImageElement | null>(null);
  const pointers = React.useRef(new Map<number, Point>());
  const pinchDistance = React.useRef<number | null>(null);
  const [natural, setNatural] = React.useState<{ width: number; height: number } | null>(null);
  const [viewport, setViewport] = React.useState(280);
  const [zoom, setZoom] = React.useState(1);
  const [offset, setOffset] = React.useState<Point>({ x: 0, y: 0 });
  const [exporting, setExporting] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  React.useLayoutEffect(() => {
    const element = viewportRef.current;
    if (!element) return;
    const measure = () => setViewport(element.clientWidth);
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  React.useEffect(() => {
    viewportRef.current?.focus({ preventScroll: true });
  }, []);

  const baseScale = natural ? viewport / Math.min(natural.width, natural.height) : 1;
  const scale = baseScale * zoom;
  const displayed = natural ? { width: natural.width * scale, height: natural.height * scale } : { width: viewport, height: viewport };

  const clamp = React.useCallback((point: Point, nextZoom = zoom): Point => {
    if (!natural) return point;
    const nextScale = baseScale * nextZoom;
    const maxX = Math.max(0, (natural.width * nextScale - viewport) / 2);
    const maxY = Math.max(0, (natural.height * nextScale - viewport) / 2);
    return { x: Math.min(maxX, Math.max(-maxX, point.x)), y: Math.min(maxY, Math.max(-maxY, point.y)) };
  }, [baseScale, natural, viewport, zoom]);

  const applyZoom = React.useCallback((next: number) => {
    const bounded = Math.min(MAX_ZOOM, Math.max(1, next));
    setZoom(bounded);
    setOffset((current) => clamp(current, bounded));
  }, [clamp]);

  // Keep the framing valid when the viewport is resized.
  React.useEffect(() => {
    setOffset((current) => clamp(current));
  }, [clamp]);

  function onPointerDown(event: React.PointerEvent<HTMLDivElement>) {
    event.currentTarget.setPointerCapture(event.pointerId);
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY });
    pinchDistance.current = null;
  }

  function onPointerMove(event: React.PointerEvent<HTMLDivElement>) {
    const previous = pointers.current.get(event.pointerId);
    if (!previous) return;
    const current = { x: event.clientX, y: event.clientY };
    pointers.current.set(event.pointerId, current);
    if (pointers.current.size === 1) {
      setOffset((value) => clamp({ x: value.x + current.x - previous.x, y: value.y + current.y - previous.y }));
      return;
    }
    const [a, b] = [...pointers.current.values()];
    const distance = Math.hypot(a.x - b.x, a.y - b.y);
    if (pinchDistance.current) applyZoom(zoom * (distance / pinchDistance.current));
    pinchDistance.current = distance;
  }

  function onPointerUp(event: React.PointerEvent<HTMLDivElement>) {
    pointers.current.delete(event.pointerId);
    pinchDistance.current = null;
  }

  function onWheel(event: React.WheelEvent<HTMLDivElement>) {
    applyZoom(zoom * (event.deltaY < 0 ? 1.08 : 1 / 1.08));
  }

  function onKeyDown(event: React.KeyboardEvent<HTMLDivElement>) {
    const step = event.shiftKey ? 24 : 8;
    const moves: Record<string, Point> = {
      ArrowLeft: { x: step, y: 0 },
      ArrowRight: { x: -step, y: 0 },
      ArrowUp: { x: 0, y: step },
      ArrowDown: { x: 0, y: -step }
    };
    if (moves[event.key]) {
      event.preventDefault();
      setOffset((value) => clamp({ x: value.x + moves[event.key].x, y: value.y + moves[event.key].y }));
    } else if (event.key === "+" || event.key === "=") {
      event.preventDefault();
      applyZoom(zoom + 0.1);
    } else if (event.key === "-") {
      event.preventDefault();
      applyZoom(zoom - 0.1);
    }
  }

  async function confirm() {
    const image = imageRef.current;
    if (!image || !natural) return;
    setExporting(true);
    setError(null);
    try {
      const canvas = document.createElement("canvas");
      canvas.width = OUTPUT_SIZE;
      canvas.height = OUTPUT_SIZE;
      const context = canvas.getContext("2d");
      if (!context) throw new Error("canvas");
      const sourceSize = viewport / scale;
      const sourceX = (displayed.width / 2 - viewport / 2 - offset.x) / scale;
      const sourceY = (displayed.height / 2 - viewport / 2 - offset.y) / scale;
      context.fillStyle = "#ffffff";
      context.fillRect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE);
      context.imageSmoothingQuality = "high";
      context.drawImage(image, sourceX, sourceY, sourceSize, sourceSize, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE);
      const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", 0.9));
      if (!blob) throw new Error("blob");
      onConfirm(blob);
    } catch {
      setError("Non sono riuscito a preparare la foto. Prova con un'altra immagine.");
      setExporting(false);
    }
  }

  return (
    <div className="avatar-cropper" role="dialog" aria-modal="true" aria-labelledby="avatar-cropper-title">
      <header className="profile-sheet-bar">
        <button className="profile-sheet-text-button" type="button" onClick={onCancel} disabled={exporting}>
          Annulla
        </button>
        <h2 id="avatar-cropper-title">Inquadra la foto</h2>
        <button className="profile-sheet-text-button profile-sheet-save" type="button" onClick={() => void confirm()} disabled={!natural || exporting} aria-busy={exporting}>
          {exporting ? <span className="profile-sheet-spinner" aria-hidden="true" /> : null}
          {exporting ? "Preparo…" : "Usa foto"}
        </button>
      </header>
      <div className="avatar-cropper-body">
        <div
          ref={viewportRef}
          className="avatar-cropper-viewport"
          tabIndex={0}
          aria-label="Area di ritaglio: trascina per spostare, frecce per spostare, più e meno per ingrandire"
          onPointerDown={onPointerDown}
          onPointerMove={onPointerMove}
          onPointerUp={onPointerUp}
          onPointerCancel={onPointerUp}
          onWheel={onWheel}
          onKeyDown={onKeyDown}
        >
          <img
            ref={imageRef}
            src={src}
            alt=""
            draggable={false}
            onLoad={(event) => setNatural({ width: event.currentTarget.naturalWidth, height: event.currentTarget.naturalHeight })}
            onError={() => setError("Questa immagine non si apre. Prova con un'altra foto.")}
            style={{
              width: displayed.width,
              height: displayed.height,
              left: viewport / 2 - displayed.width / 2 + offset.x,
              top: viewport / 2 - displayed.height / 2 + offset.y,
              visibility: natural ? "visible" : "hidden"
            }}
          />
          <span className="avatar-cropper-mask" aria-hidden="true" />
        </div>
        <label className="avatar-cropper-zoom">
          <Minus size={16} aria-hidden="true" />
          <input
            type="range"
            min={1}
            max={MAX_ZOOM}
            step={0.01}
            value={zoom}
            onChange={(event) => applyZoom(Number(event.target.value))}
            aria-label="Zoom"
          />
          <Plus size={16} aria-hidden="true" />
        </label>
        <p className="avatar-cropper-hint">Trascina per spostare, pizzica o usa il cursore per ingrandire.</p>
        {error ? <p className="profile-sheet-error profile-sheet-error--block" role="alert">{error}</p> : null}
      </div>
    </div>
  );
}
