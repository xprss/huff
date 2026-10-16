package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexatrapDtos.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.SplittableRandom;

@ApplicationScoped
public class HexatrapDailyGameProvider {
    @ConfigProperty(name = "app.hexatrap.seed") String seed;
    public Puzzle puzzleFor(String date) {
        LocalDate.parse(date);
        var random = random(date, "dossier");
        int first = random.nextInt(6), second = (first + 1 + random.nextInt(5)) % 6;
        var dossier = new Dossier(random.nextBoolean() ? 2 : 4,
            Relation.values()[random.nextInt(3)], first, second);
        var configurations = HexatrapRules.hypotheses(dossier).stream().filter(h -> h % 2 == 0).toList();
        int configuration = configurations.get(random.nextInt(configurations.size())) / 2;
        int secret = random(date, "secret").nextInt(1, HexatrapRules.MAX_NUMBER + 1);
        int falseNumber = 1 + (secret - 1 + random(date, "false").nextInt(1, HexatrapRules.MAX_NUMBER)) % HexatrapRules.MAX_NUMBER;
        return new Puzzle(dossier, configuration, random(date, "roles").nextInt(2), secret, falseNumber);
    }
    private SplittableRandom random(String date, String domain) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                (seed + "|hexatrap|v1|" + date + "|" + domain).getBytes(StandardCharsets.UTF_8));
            return new SplittableRandom(ByteBuffer.wrap(digest).getLong());
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
