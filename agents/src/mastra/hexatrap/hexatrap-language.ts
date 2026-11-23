/** The case language, shared by the author and auditor instructions. */
export const HEXATRAP_LANGUAGE = `Linguaggio delle formule (una stringa per elemento):
- Atomi: fatto=opzione oppure fatto!=opzione, usando gli id esatti del catalogo (minuscole, cifre, _). Costanti TRUE e FALSE.
- Connettivi, dal più forte al più debole: NOT, AND, XOR, OR, -> (implicazione, associativa a destra), <-> (equivalenza). Usa le parentesi per chiarezza.
- COUNT(k, φ1, φ2, ...): esattamente k delle formule sono vere.
- TRUTHFUL(A) / TRUTHFUL(B): quel personaggio dice sempre la verità.
- SAYS(A, φ) / SAYS(B, φ): ciò che quel personaggio risponderebbe («sì» = vero) se gli si chiedesse φ.
- Personaggi solo con A e B maiuscoli; parole chiave sempre in maiuscolo.
Esempi: "chi=mozzo", "porta=chiusa -> chi=ettore", "diario!=pozzo OR cane=abbaiato", "COUNT(1, ora=nove, porta=aperta)", "SAYS(A, chi=livia)".`;
