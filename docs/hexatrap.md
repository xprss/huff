# Hexatrap · regole versione 1

La sfida giornaliera è disponibile su `/#/hexatrap`. Tutti ricevono lo stesso
numero segreto (1–9864), dossier e ruoli nella data definita da
`app.game.timezone`. `HUFF_HEXATRAP_SEED` configura un seme dedicato; SHA-256
separa dossier, ruoli, numero vero e numero falso. Le partite iniziate salvano
la configurazione completa, indipendentemente dai successivi aggiornamenti.

Un consigliere è sempre sincero, l'altro inverte ogni risposta sui sigilli.
Il dossier combina due o quattro sigilli accesi con uguaglianza, implicazione
o almeno uno acceso fra due sigilli. Il server enumera 64 configurazioni × due
assegnazioni dei ruoli. Il numero si sblocca quando le ipotesi rimaste concordano
sul leale. Il giocatore deve scegliere a chi rivolgersi: l'interfaccia non
indica il ruolo e gli appunti rimangono manuali. Il numero falso è stabile e
sempre diverso dal vero.

Ogni domanda valida, comprese ripetizioni e domande sul numero, costa 100 punti;
un inserimento errato costa 200. Si parte da 1000, senza soglia minima.
Gli inserimenti sono consentiti solo dopo lo sblocco e una domanda sul numero.
Non esistono sconfitte definitive. L'esempio nel tutorial non chiama le API.

## API e persistenza

Tutte le azioni includono `requestId`, `puzzleDate` e `rulesVersion`.

- `GET /api/hexatrap/today`: dossier pubblico e partita eventualmente iniziata.
- `POST /api/hexatrap/today/questions`: `type: "SEAL"`, `advisor: 0|1`,
  `seal: 0..5`; oppure `type: "NUMBER"`, `advisor: 0|1`, `seal: null`.
- `POST /api/hexatrap/today/submissions`: `number`, intero da 1 a 9864.
- `GET /api/hexatrap/today/events?puzzleDate=YYYY-MM-DD&before=N&limit=20`:
  eventi in ordine decrescente, cursore esclusivo `nextBefore`; limite 1–100.
- `GET /api/hexatrap/stats`, `GET /api/hexatrap/leaderboards` e
  `GET /api/leaderboards?game=hexatrap`.

Le risposte alle azioni includono partita, evento e `replayed`. Lo stesso
identificatore per utente restituisce la risposta originale; contenuto diverso
restituisce 409. Il blocco transazionale dell'utente serializza dispositivi
concorrenti, inclusa la prima azione. Dopo mezzanotte le operazioni già registrate
restano recuperabili, mentre le nuove azioni sulla vecchia data restituiscono 409.
La risposta originale può essere precedente allo stato attuale: il client aggiorna
la partita solo se la sequenza ricevuta non arretra e rilegge lo stato dal server.

Flyway V21 crea una partita unica per account/data e una tabella eventi senza
limite numerico. La pagina iniziale comprende solo gli ultimi 20 eventi; la
cronologia precedente viene letta su richiesta. Le ipotesi e fino a sei risposte
utili alla spiegazione finale sono memorizzate nella partita: aggiungere una
domanda non richiede di rileggere l'intera cronologia. Dopo ogni nuova azione la
cronologia visualizzata riparte dall'ultima pagina, evitando di ricaricare tutte
le pagine già esplorate. Le operazioni in attesa di conferma sono conservate sul
dispositivo e possono essere ritrasmesse con lo stesso identificatore.

Configurazione e ruoli compaiono soltanto alla vittoria. Prima, il numero compare
esclusivamente negli eventi delle domande sul numero. La spiegazione finale cita
le risposte effettive e mostra perché l'assegnazione opposta viola il dossier.

Le statistiche personali e pubbliche includono iniziate, vinte, serie, punti medi
e migliori e domande medie. Medie e record di punti riguardano le vittorie.
La classifica ordina per vittorie, poi somma dei punti delle vittorie, poi i
criteri preesistenti (data dell'ultima vittoria nella settimana, nickname negli
altri periodi). `points` è nullo nelle altre classifiche. Le vittorie contano
anche nelle statistiche generali e nelle medaglie settimanali.

## Verifiche

```sh
cd backend
mvn test
cd ../frontend
npm test
npm run build
```

La suite richiede Docker per il PostgreSQL isolato di Quarkus Dev Services.
Su host con poca RAM, compilare una volta con `mvn test-compile` ed eseguire
il profilo `ApiSecurityFilterTest` in una JVM separata dagli altri test tramite
`mvn -Dtest=ApiSecurityFilterTest surefire:test`. In questa verifica i test sono
stati divisi in 86 test con il profilo applicativo, 29 test unitari e 51 test
di autenticazione, tutti superati, mantenendo limiti di memoria e CPU.
`HexatrapRulesTest` verifica esaustivamente tutti i modelli, le coppie ordinate di
sigilli, le configurazioni e i ruoli, con strategia ottimale entro 3–5 domande.
Java e TypeScript condividono `backend/src/test/resources/hexatrap-v1-cases.json`.
I test di servizio coprono scorciatoie, penalità e punteggi negativi, recupero,
concorrenza, cambio data, persistenza della configurazione, paginazione,
statistiche e ordinamento delle classifiche.

Il controllo dell'interfaccia sulla build di produzione è stato eseguito a
390 × 844 e 1280 × 900 con API simulate: uso da tastiera, recupero idempotente
dopo una risposta di rete persa, cronologia oltre 20 eventi, penalità,
vittoria e completamento nella raccolta. I contratti reali e la concorrenza
sono verificati separatamente su PostgreSQL dai test backend.
