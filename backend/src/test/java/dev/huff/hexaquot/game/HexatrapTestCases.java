package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.hexatrap.HexatrapCaseValidator;
import dev.huff.hexaquot.game.hexatrap.HexatrapFixtures;

import java.time.LocalDate;
import java.util.List;

/** Installs the hand-written case as the ready case of a date. */
final class HexatrapTestCases {
    private HexatrapTestCases() {}

    static void install(HexatrapCaseStore store, String date) {
        if (store.ready(date).isPresent()) return;
        var definition = HexatrapFixtures.lanterna();
        int difficulty = dev.huff.hexaquot.game.hexatrap.HexatrapDifficulty.forDate(LocalDate.parse(date));
        var report = HexatrapCaseValidator.validate(definition, difficulty, 1L);
        if (!report.valid()) throw new IllegalStateException(report.issues().toString());
        if (!store.claim(date, difficulty, "sabotaggio", false)) throw new IllegalStateException("Case " + date + " not claimable.");
        store.markReady(date, definition, report.metrics(), List.of(), 1, "fixture");
    }
}
