package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexahackDtos.FreeCluesDto;
import dev.huff.hexaquot.game.HexahackDtos.Parity;
import dev.huff.hexaquot.game.HexahackDtos.ProbeComparison;
import dev.huff.hexaquot.game.HexahackDtos.ProbeRequest;
import dev.huff.hexaquot.game.HexahackDtos.ProbeResultDto;
import dev.huff.hexaquot.game.HexahackDtos.ProbeType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@ApplicationScoped
public class HexahackDailyGameProvider {
    public static final int CODE_LENGTH = 6;
    public static final int RULES_VERSION = 2;

    @ConfigProperty(name = "app.hexahack.seed", defaultValue = "${app.word.seed}:hexahack")
    String seed;

    public String solutionFor(String puzzleDate) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest((seed + ":" + puzzleDate).getBytes(StandardCharsets.UTF_8));
            int value = new BigInteger(1, hash).mod(BigInteger.valueOf(1_000_000)).intValue();
            return String.format("%06d", value);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot choose daily Hexahack solution", error);
        }
    }

    public FreeCluesDto freeClues(String solution) {
        validateCode(solution);
        int sum = solution.chars().map(value -> value - '0').sum();
        return new FreeCluesDto(sum, (int) solution.chars().distinct().count());
    }

    public ProbeResultDto probe(ProbeRequest request, String solution) {
        if (request == null || request.type() == null) throw new BadRequestException("Tipo di sonda mancante.");
        String requestId = validateRequestId(request.requestId());
        int position = validatePosition(request.position(), "Posizione");
        int digit = digitAt(solution, position);
        return switch (request.type()) {
            case PING -> {
                int threshold = validateThreshold(request.threshold());
                ProbeComparison comparison = digit < threshold ? ProbeComparison.BELOW
                    : digit > threshold ? ProbeComparison.ABOVE : ProbeComparison.EQUAL;
                yield new ProbeResultDto(requestId, ProbeType.PING, 1, position, null, threshold, comparison,
                    null, null, "Posizione " + position + ": " + comparison.name().toLowerCase() + " " + threshold);
            }
            case BIT_SCAN -> {
                Parity parity = digit % 2 == 0 ? Parity.EVEN : Parity.ODD;
                yield new ProbeResultDto(requestId, ProbeType.BIT_SCAN, 1, position, null, null, null,
                    parity, null, "Posizione " + position + ": " + parity.name().toLowerCase());
            }
            case LINK_TRACE -> {
                int other = validateOtherPosition(request.otherPosition(), position);
                int otherDigit = digitAt(solution, other);
                ProbeComparison comparison = digit < otherDigit ? ProbeComparison.BELOW
                    : digit > otherDigit ? ProbeComparison.ABOVE : ProbeComparison.EQUAL;
                yield new ProbeResultDto(requestId, ProbeType.LINK_TRACE, 1, position, other, null, comparison,
                    null, null, "Posizione " + position + " " + comparison.name().toLowerCase() + " posizione " + other);
            }
            case CHECKSUM -> {
                int other = validateOtherPosition(request.otherPosition(), position);
                int sum = digit + digitAt(solution, other);
                yield new ProbeResultDto(requestId, ProbeType.CHECKSUM, 2, position, other, null, null,
                    null, sum, "Posizioni " + position + "+" + other + " = " + sum);
            }
        };
    }

    public String validateCode(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (!code.matches("[0-9]{6}")) throw new BadRequestException("Inserisci esattamente 6 cifre.");
        return code;
    }

    public String validateRequestId(String rawRequestId) {
        String requestId = rawRequestId == null ? "" : rawRequestId.trim();
        if (requestId.isEmpty() || requestId.length() > 100 || !requestId.matches("[A-Za-z0-9._:-]+")) {
            throw new BadRequestException("requestId non valido.");
        }
        return requestId;
    }

    private int validatePosition(Integer position, String label) {
        if (position == null || position < 1 || position > CODE_LENGTH) {
            throw new BadRequestException(label + " non valida: usa un valore da 1 a 6.");
        }
        return position;
    }

    private int validateOtherPosition(Integer position, int first) {
        int other = validatePosition(position, "Seconda posizione");
        if (other == first) throw new BadRequestException("Le due posizioni devono essere diverse.");
        return other;
    }

    private int validateThreshold(Integer threshold) {
        if (threshold == null || threshold < 0 || threshold > 9) {
            throw new BadRequestException("La soglia deve essere una cifra da 0 a 9.");
        }
        return threshold;
    }

    private int digitAt(String solution, int position) {
        return solution.charAt(position - 1) - '0';
    }
}
