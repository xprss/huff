package dev.huff.hexaquot.game.hexatrap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HexatrapItalianRendererTest {
    private final HexatrapItalianRenderer renderer = new HexatrapItalianRenderer(HexatrapFixtures.lanterna());

    private static HexatrapFormula parse(String source) throws Exception {
        return HexatrapFormulaParser.parse(source);
    }

    @Test
    void writesTheOfficialRecordFromTheAtomPhrases() throws Exception {
        assertEquals("se la porta della torre era chiusa, allora è stato Ettore a spegnere la lanterna",
            renderer.formal(parse("porta=chiusa -> chi=ettore")));
        assertEquals("il cane ha abbaiato oppure la porta della torre era chiusa", renderer.formal(parse("cane=abbaiato OR porta=chiusa")));
        assertEquals("non è stato il mozzo a spegnere la lanterna", renderer.formal(parse("NOT chi=mozzo")));
        assertEquals("Ettore risponderebbe «sì» a: «È stata Livia a spegnere la lanterna?»", renderer.formal(parse("SAYS(A, chi=livia)")));
        assertEquals("Livia mente sempre", renderer.formal(parse("NOT TRUTHFUL(B)")));
        assertEquals("esattamente una di queste è vera: il cane ha abbaiato; la porta della torre era aperta",
            renderer.formal(parse("COUNT(1, cane=abbaiato, porta=aperta)")));
        assertEquals("il cane ha abbaiato e (la porta della torre era aperta oppure la lanterna si è spenta alle nove)",
            renderer.formal(parse("cane=abbaiato AND (porta=aperta OR ora=nove)")));
        assertEquals("Il diario è finito nella stufa?", renderer.question(parse("diario=stufa")));
        assertEquals("Ettore risponderebbe «sì» a: «Il diario è finito nella cassa?»", renderer.question(parse("SAYS(A, diario=cassa)")));
    }

    @Test
    void readsStatementsUnderEachHypothesis() throws Exception {
        var claim = parse("SAYS(A, chi=livia)");
        assertEquals("Non è stata Livia a spegnere la lanterna", renderer.statementReading(claim, "B", "B"));
        assertEquals("Non è stata Livia a spegnere la lanterna", renderer.statementReading(claim, "B", "A"));
        var disjunction = parse("cane=abbaiato OR porta=chiusa");
        assertEquals("Il cane non ha abbaiato e la porta della torre non era chiusa", renderer.statementReading(disjunction, "B", "B"));
        assertEquals("Nessuna informazione sui fatti", HexatrapItalianRenderer.capitalize(renderer.statementReading(parse("TRUTHFUL(A)"), "A", "B")));
        assertEquals("Contraddizione: questa ipotesi è impossibile",
            HexatrapItalianRenderer.capitalize(renderer.statementReading(parse("TRUTHFUL(B)"), "A", "A")));
    }

    @Test
    void readsAnswersUnderEachHypothesis() throws Exception {
        var question = parse("ora=undici");
        assertEquals("La lanterna non si è spenta alle undici", renderer.answerReading(question, "A", false, "B"));
        assertEquals("La lanterna si è spenta alle undici", renderer.answerReading(question, "A", false, "A"));
        assertEquals("Il diario non è finito nella cassa", renderer.answerReading(parse("SAYS(A, diario=cassa)"), "B", true, "B"));
        assertEquals("Il diario non è finito nella cassa", renderer.answerReading(parse("SAYS(A, diario=cassa)"), "B", true, "A"));
    }
}
