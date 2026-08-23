import React from "react";
import { Eraser, HelpCircle, Maximize2, Play, Redo2, RotateCw, Share2, Undo2, X, ZoomIn, ZoomOut } from "lucide-react";
import type {
  HexasquareCharacterOutcomeDto,
  HexasquarePlacementDto,
  HexasquareRoadType,
  HexasquareSimulationActionDto,
  HexasquareStatsDto,
  HexasquareTodayDto
} from "../../types";

const CELL_SIZE = 34;
const TUTORIAL_KEY = "huff.hexasquare.tutorial.v1";
const ROAD_TYPES: readonly HexasquareRoadType[] = ["STRAIGHT", "CURVE", "T_JUNCTION", "CROSS"];
const ROAD_LABELS: Record<HexasquareRoadType, string> = { STRAIGHT: "Rettilineo", CURVE: "Curva", T_JUNCTION: "Incrocio a T", CROSS: "Incrocio" };
const QUADRANT_LABELS = { BRUMAVIA: "Brumavia", SOLARIA: "Solaria", VERDOMBRA: "Verdombra", LUNARGENTO: "Lunargento" } as const;
type Tool = HexasquareRoadType | "ERASER";
type Rotation = 0 | 90 | 180 | 270;

export function HexasquareView({ today, stats, busy, onSimulate, onError, onComplete, onOpenStats }: {
  today: HexasquareTodayDto;
  stats: HexasquareStatsDto;
  busy: boolean;
  onSimulate: (request: { requestId: string; placements: readonly HexasquarePlacementDto[] }) => Promise<HexasquareSimulationActionDto>;
  onError: (message: string) => void;
  onComplete: (action: HexasquareSimulationActionDto) => void;
  onOpenStats: () => void;
}) {
  const draftKey = `huff.hexasquare.draft.${today.puzzleDate}`;
  const [placements, setPlacements] = React.useState<readonly HexasquarePlacementDto[]>(() => initialPlacements(today, draftKey));
  const [tool, setTool] = React.useState<Tool>("STRAIGHT");
  const [rotation, setRotation] = React.useState<Rotation>(90);
  const [undo, setUndo] = React.useState<readonly (readonly HexasquarePlacementDto[])[]>([]);
  const [redo, setRedo] = React.useState<readonly (readonly HexasquarePlacementDto[])[]>([]);
  const [result, setResult] = React.useState<HexasquareSimulationActionDto["result"] | null>(null);
  const [tutorialOpen, setTutorialOpen] = React.useState(() => localStorage.getItem(TUTORIAL_KEY) !== "true");
  const [zoom, setZoom] = React.useState(.75);
  const viewportRef = React.useRef<HTMLDivElement | null>(null);
  const complete = today.game?.status === "COMPLETED";
  const blocked = React.useMemo(() => new Set([...today.obstacles, ...today.terminals.map((terminal) => terminal.coordinate)].map(coordinateKey)), [today.obstacles, today.terminals]);
  const obstacleSet = React.useMemo(() => new Set(today.obstacles.map(coordinateKey)), [today.obstacles]);
  const terminalMap = React.useMemo(() => new Map(today.terminals.map((terminal) => [coordinateKey(terminal.coordinate), terminal])), [today.terminals]);
  const placementMap = React.useMemo(() => new Map(placements.map((placement) => [coordinateKey(placement), placement])), [placements]);
  const failedCharacterIds = React.useMemo(() => new Set(result?.characters.filter((outcome)=>outcome.result!=="REACHED").map((outcome)=>outcome.characterId)??[]),[result]);
  const pathMap = React.useMemo(() => {
    const map = new Map<string, number[]>();
    (result?.success ? result.paths : today.game?.paths ?? []).forEach((path, index) => path.cells.forEach((cell) => map.set(coordinateKey(cell), [...(map.get(coordinateKey(cell)) ?? []), index])));
    return map;
  }, [result, today.game?.paths]);
  const usedByType = React.useMemo(() => placements.reduce((counts, placement) => ({ ...counts, [placement.type]: counts[placement.type] + 1 }), emptyRoadCounts()), [placements]);

  React.useEffect(() => {
    if (!complete) localStorage.setItem(draftKey, JSON.stringify(placements));
  }, [complete, draftKey, placements]);

  React.useEffect(() => {
    if (!today.game) return;
    setPlacements(today.game.lastPlacements);
    if (today.game.status === "COMPLETED") localStorage.removeItem(draftKey);
  }, [draftKey, today.game]);

  React.useEffect(() => { window.setTimeout(fitBoard, 0); }, [today.puzzleDate]);

  function commit(next: readonly HexasquarePlacementDto[]) {
    setUndo((history) => [...history.slice(-49), placements]);
    setRedo([]); setPlacements(next); setResult(null);
  }

  function editCell(row: number, column: number) {
    if (complete || blocked.has(`${row}:${column}`)) return;
    const current = placementMap.get(`${row}:${column}`);
    if (tool === "ERASER") {
      if (current) commit(placements.filter((placement) => placement !== current));
      return;
    }
    if (current?.type === tool) {
      rotateCell(row, column);
      return;
    }
    if ((!current || current.type !== tool) && usedByType[tool] >= (today.inventory[tool] ?? 0)) { onError(`Hai esaurito: ${ROAD_LABELS[tool]}.`); return; }
    const next = placements.filter((placement) => placement !== current);
    commit([...next, { row, column, type: tool, rotation }]);
  }

  function rotateCell(row: number, column: number) {
    if (complete) return;
    const current=placementMap.get(`${row}:${column}`); if(!current)return;
    commit(placements.map((placement)=>placement===current?{...placement,rotation:((placement.rotation+90)%360) as Rotation}:placement));
  }
  function eraseCell(row:number,column:number) { const current=placementMap.get(`${row}:${column}`);if(!complete&&current)commit(placements.filter((placement)=>placement!==current)); }

  function undoEdit() {
    const previous=undo[undo.length-1]; if(!previous)return;
    setUndo((history)=>history.slice(0,-1)); setRedo((history)=>[...history,placements]); setPlacements(previous); setResult(null);
  }
  function redoEdit() {
    const next=redo[redo.length-1]; if(!next)return;
    setRedo((history)=>history.slice(0,-1)); setUndo((history)=>[...history,placements]); setPlacements(next); setResult(null);
  }

  async function simulate() {
    try {
      const action=await onSimulate({requestId:requestId(),placements});
      setPlacements(action.game.lastPlacements); setResult(action.result);
      if(action.result.success){localStorage.removeItem(draftKey);onComplete(action);}
    } catch(error) { onError(error instanceof Error?error.message:"Simulazione non riuscita."); }
  }

  function fitBoard() {
    const viewport=viewportRef.current; if(!viewport)return;
    const next=Math.max(.32,Math.min(1,(Math.min(viewport.clientWidth,viewport.clientHeight)-24)/(today.size*CELL_SIZE)));
    setZoom(next);
    viewport.scrollTo({ left: 0, top: 0, behavior: "smooth" });
  }

  async function share() {
    const saved=today.game?.remainingCells??result?.remainingCells??0;
    const text=`🏙️ Hexasquare ${today.puzzleDate}\n✅ Rete completata · ${today.game?.simulationsCount??1} simulazioni · ${saved} caselle risparmiate`;
    try { if(navigator.share)await navigator.share({title:"HexaQuot · Hexasquare",text,url:`${location.origin}/#/hexasquare`}); else await navigator.clipboard.writeText(`${text}\n${location.origin}/#/hexasquare`); }
    catch { /* The share sheet can be cancelled. */ }
  }

  return <section className="hexasquare-view" aria-label="Hexasquare">
    <header className="hexasquare-intro"><div><p className="eyebrow">Pianificazione quotidiana</p><h2>Hexasquare</h2><p>Collega ogni viaggiatore al suo obiettivo rispettando quartieri vietati e incompatibilità.</p></div><button className="icon-button" type="button" onClick={()=>setTutorialOpen(true)} aria-label="Apri il tutorial"><HelpCircle /></button></header>
    <div className="hexasquare-layout">
      <div className="hexasquare-workspace">
        <div className="hexasquare-tools" aria-label="Scegli lo strumento da usare sulla griglia">
          {ROAD_TYPES.map((type)=><button key={type} type="button" disabled={complete||usedByType[type]>=today.inventory[type]} className={tool===type?"selected":""} onClick={()=>setTool(type)} aria-pressed={tool===type} title={`${ROAD_LABELS[type]}: ${today.inventory[type]-usedByType[type]} disponibili`}><RoadGlyph type={type} rotation={rotation}/><span>{ROAD_LABELS[type]}</span><strong aria-label={`${today.inventory[type]-usedByType[type]} disponibili`}>{today.inventory[type]-usedByType[type]}</strong></button>)}
          <button type="button" disabled={complete} className={tool==="ERASER"?"selected":""} onClick={()=>setTool("ERASER")} aria-pressed={tool==="ERASER"}><Eraser/><span>Rimuovi</span></button>
        </div>
        <div className="hexasquare-commandbar">
          <p className="hexasquare-active-tool">{tool === "ERASER" ? <><Eraser/> Rimuovi strade</> : <><RoadGlyph type={tool} rotation={rotation}/> <span>{ROAD_LABELS[tool]}</span></>}</p>
          <button className="hexasquare-rotate" type="button" onClick={()=>setRotation(((rotation+90)%360) as Rotation)} disabled={complete||tool==="ERASER"} title="Ruota lo strumento attivo di 90 gradi"><RotateCw/> Ruota <span>{rotation}°</span></button>
          <button type="button" onClick={undoEdit} disabled={complete||!undo.length} title="Annulla l'ultima modifica" aria-label="Annulla l'ultima modifica"><Undo2/><span>Annulla</span></button>
          <button type="button" onClick={redoEdit} disabled={complete||!redo.length} title="Ripristina l'ultima modifica" aria-label="Ripristina l'ultima modifica"><Redo2/><span>Ripristina</span></button>
          <span className="hexasquare-zoom"><button type="button" onClick={()=>setZoom((value)=>Math.max(.3,value-.1))} aria-label="Riduci zoom"><ZoomOut/></button><strong>{Math.round(zoom*100)}%</strong><button type="button" onClick={()=>setZoom((value)=>Math.min(1.8,value+.1))} aria-label="Aumenta zoom"><ZoomIn/></button><button type="button" onClick={fitBoard} aria-label="Adatta tabellone"><Maximize2/></button></span>
        </div>
        <p className="hexasquare-navigation-hint">Scorri la mappa con mouse o dito. Seleziona una strada e tocca una casella per posarla; ritocca la stessa strada per ruotarla.</p>
        <div ref={viewportRef} className="hexasquare-viewport">
          <div className="hexasquare-canvas" style={{width:today.size*CELL_SIZE*zoom+24,height:today.size*CELL_SIZE*zoom+24}}>
            <div className="hexasquare-board" role="grid" aria-label="Griglia urbana 24 per 24" style={{width:today.size*CELL_SIZE,height:today.size*CELL_SIZE,gridTemplateColumns:`repeat(${today.size}, ${CELL_SIZE}px)`,transform:`scale(${zoom})`}}>
            {Array.from({length:today.size*today.size},(_,index)=>{const row=Math.floor(index/today.size),column=index%today.size,key=`${row}:${column}`,placement=placementMap.get(key),terminal=terminalMap.get(key),character=terminal?today.characters.find((entry)=>entry.id===terminal.characterId):null,paths=pathMap.get(key)??[];return <button type="button" role="gridcell" key={key} data-row={row} data-column={column} className={`hexasquare-cell quadrant-${quadrantFor(row,column).toLowerCase()} ${obstacleSet.has(key)?"obstacle":""} ${terminal?"terminal":""} ${terminal&&failedCharacterIds.has(terminal.characterId)?"failed-character":""} ${paths.length?"routed":""}`} aria-label={cellLabel(row,column,placement,terminal?.kind,character?.name)} onClick={()=>editCell(row,column)} onKeyDown={(event)=>handleCellKey(event,row,column,()=>editCell(row,column),()=>rotateCell(row,column),()=>eraseCell(row,column))} disabled={false}>
              {obstacleSet.has(key)?<span aria-hidden="true">▦</span>:terminal?<span className="terminal-marker" aria-hidden="true">{character?.emoji}<small>{terminal.kind==="START"?"P":"D"}</small></span>:placement?<RoadGlyph type={placement.type} rotation={placement.rotation}/>:null}
              {paths.map((path)=><i className={`route-dot route-${path%6}`} key={path} aria-hidden="true"/>)}
            </button>;})}
            </div>
          </div>
        </div>
        {!complete?<button className="hexasquare-simulate" type="button" onClick={()=>void simulate()} disabled={busy}><Play/> {busy?"Simulazione…":"Simula"}</button>:null}
        {result&&!result.success?<FailurePanel outcomes={result.characters} today={today}/>:null}
        {complete?<section className="hexasquare-success"><h3>Rete completata</h3><p><strong>{today.game?.usedCells}</strong> caselle usate · <strong>{today.game?.remainingCells}</strong> risparmiate · <strong>{today.game?.simulationsCount}</strong> simulazioni</p><div><button type="button" onClick={()=>void share()}><Share2/> Condividi</button><button type="button" onClick={onOpenStats}>Statistiche</button></div></section>:null}
      </div>
      <aside className="hexasquare-legend"><h3>Viaggiatori</h3>{today.characters.map((character)=><article className={failedCharacterIds.has(character.id)?"failed-character":""} key={character.id}><header><span>{character.emoji}</span><strong>{character.name}</strong></header><p>{character.goal.type==="EXACT_CELL"?`Destinazione: riga ${(character.goal.coordinate?.row??0)+1}, colonna ${(character.goal.coordinate?.column??0)+1}`:`Destinazione: una strada in ${QUADRANT_LABELS[character.goal.quadrant!]}`}</p><small>Vietati: {character.forbiddenQuadrants.map((quadrant)=>QUADRANT_LABELS[quadrant]).join(", ")}</small></article>)}<h3>Incompatibilità</h3>{today.incompatiblePairs.map((pair)=>{const first=today.characters.find((character)=>character.id===pair.firstCharacterId),second=today.characters.find((character)=>character.id===pair.secondCharacterId);return <p key={`${pair.firstCharacterId}:${pair.secondCharacterId}`} className="incompatibility">{first?.emoji} {first?.name} ↮ {second?.emoji} {second?.name}</p>;})}<h3>Quartieri</h3><div className="quadrant-legend">{today.quadrants.map((quadrant)=><span className={`quadrant-${quadrant.id.toLowerCase()}`} key={quadrant.id}>{quadrant.name}</span>)}</div><p className="stats-note">{stats.completed} reti completate · serie {stats.currentStreak}</p></aside>
    </div>
    {tutorialOpen?<div className="modal-backdrop" role="presentation" onMouseDown={()=>closeTutorial(setTutorialOpen)}><section className="modal hexasquare-tutorial" role="dialog" aria-modal="true" aria-labelledby="hexasquare-tutorial-title" onMouseDown={(event)=>event.stopPropagation()}><header className="modal-head"><h2 id="hexasquare-tutorial-title">Come si gioca</h2><button className="close-button" type="button" onClick={()=>closeTutorial(setTutorialOpen)} aria-label="Chiudi"><X/></button></header><ol><li>Scegli una strada, ruotala e posala su una cella libera.</li><li>Collega le partenze agli obiettivi senza attraversare i quartieri vietati.</li><li>I viaggiatori incompatibili non possono usare la stessa casella. Simula quante volte vuoi.</li></ol><p>Da tastiera: frecce per muoverti, Invio per posare, R per ruotare, Canc per rimuovere.</p><button className="launch-cta" type="button" onClick={()=>closeTutorial(setTutorialOpen)}>Inizia a progettare</button></section></div>:null}
  </section>;
}

function RoadGlyph({type,rotation}:{type:HexasquareRoadType;rotation:number}) { return <svg className="road-glyph" viewBox="0 0 32 32" aria-hidden="true" style={{transform:`rotate(${rotation}deg)`}}><path d={type==="STRAIGHT"?"M16 0V32":type==="CURVE"?"M16 0V16H32":type==="T_JUNCTION"?"M0 16H32M16 16V0":"M0 16H32M16 0V32"}/></svg>; }
function emptyRoadCounts():Record<HexasquareRoadType,number>{return{STRAIGHT:0,CURVE:0,T_JUNCTION:0,CROSS:0};}
function coordinateKey(coordinate:{row:number;column:number}){return `${coordinate.row}:${coordinate.column}`;}
function quadrantFor(row:number,column:number){if(row<12)return column<12?"BRUMAVIA":"SOLARIA";return column<12?"VERDOMBRA":"LUNARGENTO";}
function requestId(){return globalThis.crypto?.randomUUID?.()??`${Date.now()}-${Math.random().toString(16).slice(2)}`;}
function initialPlacements(today:HexasquareTodayDto,draftKey:string):readonly HexasquarePlacementDto[]{if(today.game?.status==="COMPLETED")return today.game.lastPlacements;try{const draft=JSON.parse(localStorage.getItem(draftKey)??"null");if(Array.isArray(draft))return draft;}catch{/* Ignore damaged local drafts. */}return today.game?.lastPlacements??[];}
function closeTutorial(setOpen:(open:boolean)=>void){localStorage.setItem(TUTORIAL_KEY,"true");setOpen(false);}
function cellLabel(row:number,column:number,placement?:HexasquarePlacementDto,terminal?:"START"|"DESTINATION",name?:string){const content=terminal?`${terminal==="START"?"partenza":"destinazione"} di ${name}`:placement?`${ROAD_LABELS[placement.type]}, rotazione ${placement.rotation} gradi`:"libera";return `Riga ${row+1}, colonna ${column+1}, ${content}`;}
function handleCellKey(event:React.KeyboardEvent<HTMLButtonElement>,row:number,column:number,place:()=>void,rotate:()=>void,erase:()=>void){let nextRow=row,nextColumn=column;if(event.key==="ArrowUp")nextRow--;else if(event.key==="ArrowDown")nextRow++;else if(event.key==="ArrowLeft")nextColumn--;else if(event.key==="ArrowRight")nextColumn++;else if(event.key==="Enter"||event.key===" "){event.preventDefault();place();return;}else if(event.key.toLowerCase()==="r"){event.preventDefault();rotate();return;}else if(event.key==="Delete"||event.key==="Backspace"){event.preventDefault();erase();return;}else return;event.preventDefault();document.querySelector<HTMLButtonElement>(`.hexasquare-cell[data-row="${Math.max(0,Math.min(23,nextRow))}"][data-column="${Math.max(0,Math.min(23,nextColumn))}"]`)?.focus();}
function FailurePanel({outcomes,today}:{outcomes:readonly HexasquareCharacterOutcomeDto[];today:HexasquareTodayDto}){const failed=outcomes.filter((outcome)=>outcome.result!=="REACHED");const labels={UNREACHABLE:"nessun percorso collegato",FORBIDDEN_QUADRANT:"il percorso entra in un quartiere vietato",CONFLICT:"conflitto con un viaggiatore incompatibile",REACHED:"arrivato"};return <section className="hexasquare-failures" aria-live="polite"><h3>La rete va rivista</h3>{failed.map((outcome)=>{const character=today.characters.find((entry)=>entry.id===outcome.characterId);return <p key={outcome.characterId}><span>{character?.emoji}</span><strong>{character?.name}:</strong> {labels[outcome.result]}{outcome.conflictsWith.length?` (${outcome.conflictsWith.map((id)=>today.characters.find((entry)=>entry.id===id)?.name??id).join(", ")})`:""}</p>;})}</section>;}
