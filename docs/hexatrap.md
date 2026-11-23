# Hexatrap · regole versione 2 · «Il caso del giorno»

Hexatrap è un'indagine logica quotidiana in stile «cavalieri e furfanti». Due
interlocutori: uno dice **sempre** la verità, l'altro mente **sempre**, su ogni
affermazione e su ogni risposta. Il giocatore deve capire chi mente e
ricostruire i fatti chiave del caso. Ogni caso è scritto nella notte
dall'agente Mastra `openai/gpt-6-luna`, verificato in modo esaustivo dal
backend e salvato in database; durante la partita non c'è alcuna chiamata AI.

## La partita

- **Dossier**: titolo, ambientazione, formato (furto, alibi, sabotaggio, mappa,
  eredità, codice, scomparsa, falsario, avvelenamento, contrabbando) e
  difficoltà da 1 a 5 (lunedì 1, martedì 2, mercoledì e giovedì 3, venerdì 4,
  weekend 5).
- **Mondo nascosto**: da 3 a 6 fatti, ciascuno con 2–4 opzioni (al massimo 4096
  mondi). Un'ipotesi è la coppia (mondo, bugiardo).
- **Certezze** (0–4, gratuite): sempre vere, solo sui fatti.
- **Deposizioni** (2–4 per interlocutore, gratuite): vere se le pronuncia il
  sincero, false se le pronuncia il bugiardo.
- **Domande** (3–5 per interlocutore, 5 minuti l'una, una sola volta): ogni
  domanda ha un destinatario fisso, perché chiedere la stessa cosa all'altro
  darebbe esattamente la stessa informazione. Il server risponde Sì/No.
- **Reperti** (3–6, 8, 12 o 16 minuti): il titolo è visibile, il contenuto
  (sempre vero) si scopre esaminandolo.
- **Budget**: 60 minuti. La somma dei costi supera sempre il budget di almeno 15
  minuti: non si può comprare tutto. A tempo esaurito resta il verdetto.
- **Verdetto**: unico e definitivo, chi mente e il valore di 1–2 fatti del
  verdetto. Esiti `SOLVED` (vittoria), `PARTIAL` (bugiardo giusto,
  ricostruzione sbagliata) o `WRONG`. Lo stato resta `IN_PROGRESS`/`WON`/`LOST`.
- **Punti** (solo `SOLVED`): `300 + 100 × difficoltà + 10 × minuti rimasti`.
- **Verbale**: sotto ogni frase il server mostra la formulazione ufficiale,
  scritta a modelli fissi a partire dalle frasi atomiche delle opzioni; fa fede
  sul testo narrativo.
- **Lente delle ipotesi** (facoltativa): per ogni deposizione e risposta mostra
  cosa implica sui fatti se mente l'uno o l'altro. È una riscrittura
  dell'informazione pubblica, non combina gli indizi.
- **Taccuino**: fatti × opzioni a tre stati e sospetto per interlocutore,
  salvato solo sul dispositivo.
- **Rivelazione**: verità, la più piccola prova fra gli elementi raccolti dal
  giocatore, il percorso più economico, il par di un investigatore esperto,
  l'epilogo e un testo di condivisione senza spoiler.

## Il linguaggio delle formule

```
atomi        fatto=opzione   fatto!=opzione   TRUE   FALSE
connettivi   NOT  >  AND  >  XOR  >  OR  >  ->  >  <->    (-> associativa a destra)
conteggio    COUNT(k, φ1, φ2, ...)     esattamente k formule vere
ruoli        TRUTHFUL(A)               A dice sempre la verità
             SAYS(B, φ)                cosa risponderebbe B se gli si chiedesse φ
```

Semantica, con ipotesi h = (mondo, bugiardo):

- `SAYS(X, φ) = φ XOR mente(X)`;
- deposizione di S coerente sse `φ == sincero(S)`;
- risposta di S alla domanda φ: `φ == sincero(S)`;
- certezze e reperti: `φ` vera.

Limiti: al massimo 16 nodi e profondità 4, niente `SAYS` dentro `SAYS`, nessun
personaggio cita la propria risposta, certezze e reperti parlano solo di fatti.
Ne emergono i trucchi classici: «Sei tu il sincero?» è una **trappola** senza
informazione; «L'altro direbbe X?» è un **oracolo** che rivela ¬X.

## Validazione (autorità del backend)

`game/hexatrap/HexatrapCaseValidator` rifiuta un caso se:

- la struttura non rispetta conteggi, lunghezze, id, icone ammesse e costi;
- una formula non si analizza, cita fatti o opzioni inesistenti o viola i limiti;
- la soluzione dichiarata contraddice certezze, reperti o ruoli;
- una certezza è sempre vera o una deposizione è compatibile con tutto;
- certezze e deposizioni rivelano già chi mente;
- con tutte le domande e tutti i reperti bugiardo e fatti del verdetto restano
  incerti;
- il verdetto si indovina con probabilità maggiore di 1/12;
- tutte le azioni costano meno di 75 minuti, basta una sola azione, il percorso
  ottimale supera 40 minuti o un esperto non chiude entro 60;
- ci sono più di 2 trappole o più di 2 oracoli.

Le fasce di difficoltà sono preferenze *soft*: bilanciamento iniziale dei ruoli
fra 25% e 75%, numero minimo di azioni combinate (2 nei giorni facili, 3 negli
altri) e par esperto fra `10 + 4d` e `60 × (0.45 + 0.07d)` minuti.
`HexatrapCaseAnalyzer` enumera ogni ipotesi come bitset e calcola il
certificato minimo (ricerca esatta con limite superiore greedy), il par
esperto (scelta greedy dell'azione con la miglior riduzione attesa per minuto)
e la mediana di 200 indagini casuali con seme derivato dalla data.

## Generazione

`HexatrapCaseScheduler` gira a mezzanotte (`HEXATRAP_GENERATION_CRON`), ogni 15
minuti (`HEXATRAP_GENERATION_RETRY_CRON`) e all'avvio, e assicura i casi da oggi
a oggi + `HEXATRAP_LOOKAHEAD_DAYS` (1: il caso è pronto un giorno prima). Un
lease su `hexatrap_cases` impedisce generazioni concorrenti.
`HexatrapCaseGenerator`, fino a `HEXATRAP_MAX_ATTEMPTS` volte:

1. chiede una bozza a `POST /internal/hexatrap/draft` con difficoltà, formato,
   costi, icone e titoli/nomi/formati degli ultimi 21 giorni da evitare;
2. la valida; i problemi diventano feedback per la bozza successiva;
3. manda i soli testi a `POST /internal/hexatrap/audit`: il revisore li traduce
   in formule senza conoscere quelle dell'autore e il backend ne confronta
   l'equivalenza su tutte le ipotesi ammesse dalle certezze
   (`HEXATRAP_AUDIT=true`); ogni frase che non dice ciò che dice la sua formula
   torna all'autore;
4. accetta il primo caso perfetto, oppure all'ultimo tentativo il primo caso
   valido e revisionato con soli avvisi di difficoltà.

Nessuna transazione resta aperta durante le chiamate all'agente; ogni fase è
registrata in `hexatrap_generation_attempts`. Una rigenerazione fallita
conserva il caso pronto precedente.

## API

Tutte le azioni includono `requestId`, `puzzleDate` e `rulesVersion` (2); lo
stesso identificatore restituisce la risposta originale, contenuto diverso
restituisce 409, e le azioni di un account sono serializzate.

- `GET /api/hexatrap/today`: `{ available, caseFile, game }`. Il dossier non
  contiene formule, soluzione, contenuto dei reperti non esaminati né epilogo.
- `POST /api/hexatrap/today/questions` `{ questionId }`,
  `POST /api/hexatrap/today/clues` `{ clueId }`,
  `POST /api/hexatrap/today/verdict` `{ liar, facts }`.
- `GET /api/hexatrap/stats`, `GET /api/hexatrap/leaderboards`,
  `GET /api/leaderboards?game=hexatrap`.
- Admin (privilegio `can_manage_hexatrap_cases`):
  `GET /api/admin/hexatrap/cases?from&to`, `GET /api/admin/hexatrap/cases/{date}`
  (caso completo con soluzione, verbali e tentativi),
  `POST /api/admin/hexatrap/cases/{date}/generate` (202; solo da oggi ai
  prossimi 7 giorni e se nessuno ha ancora giocato quella data).

Finché `HEXATRAP_ENABLED` è `false` il gioco è un'anteprima per i soli admin:
`/api/me` restituisce `hexatrapAvailable` e `hexatrapPublic`, e per gli altri
le rotte di gioco rispondono 404.

## Persistenza

Flyway `V26__hexatrap_cases.sql` sostituisce le tabelle della v1, mai
pubblica: `hexatrap_cases` (un caso per data, stato, lease, caso, metriche,
avvisi), `hexatrap_generation_attempts`, `hexatrap_games` (una partita per
account e data) e `hexatrap_actions` (idempotenza e unicità di ogni domanda e
reperto per partita). Il ruolo statistico in sola lettura non riceve accesso a
`hexatrap_cases`, che contiene le soluzioni.

## Configurazione

```bash
HEXATRAP_ENABLED=false                    # true: pubblico per tutti
HEXATRAP_GENERATION_ENABLED=true
HEXATRAP_GENERATION_CRON=0 0 0 * * ?
HEXATRAP_GENERATION_RETRY_CRON=0 */15 * * * ?
HEXATRAP_LOOKAHEAD_DAYS=1
HEXATRAP_MAX_ATTEMPTS=4
HEXATRAP_AUDIT=true
HEXATRAP_AGENT_TIMEOUT_SECONDS=240
HUFF_HEXATRAP_SEED=...                    # rotazione dei formati e simulazioni
```

L'agente usa `GAME_STATS_AGENT_URL`, `GAME_STATS_AGENT_TOKEN` e `OPENAI_API_KEY`
già esistenti.

## Verifiche

```sh
cd backend && mvn test
cd frontend && npm test && npm run build
cd agents && npx tsc --noEmit && npm run build
```

I test del motore (`game/hexatrap/*Test`) non richiedono Docker e usano il caso
scritto a mano `backend/src/test/resources/hexatrap/case-lanterna.json`. I test
di generazione usano `FakeHexatrapAgent`, che serve bozze in coda e simula
revisioni corrette o divergenti.
