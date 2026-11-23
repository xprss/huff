package dev.huff.hexaquot.game.hexatrap;

import dev.huff.hexaquot.game.hexatrap.HexatrapFormula.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Recursive-descent parser of the case language. Precedence, from the loosest:
 * {@code <->}, {@code ->} (right associative), {@code OR}, {@code XOR}, {@code AND}, {@code NOT}.
 * Atoms are {@code fact=option} and {@code fact!=option}; the other primaries are
 * {@code TRUE}, {@code FALSE}, {@code TRUTHFUL(A)}, {@code SAYS(B, φ)}, {@code COUNT(k, φ1, φ2, ...)}.
 * Error messages are Italian because they are sent back to the authoring agent as feedback.
 */
public final class HexatrapFormulaParser {
    public static final class ParseException extends Exception {
        private final int column;

        ParseException(String message, int column) {
            super(message + " (colonna " + column + ")");
            this.column = column;
        }

        public int column() { return column; }
    }

    private enum Kind { IDENT, CHARACTER, NUMBER, KEYWORD, LPAREN, RPAREN, COMMA, EQ, NEQ, IMPLIES, IFF, END }

    private record Token(Kind kind, String text, int column) {}

    private static final Map<String, String> KEYWORDS = Map.of(
        "NOT", "NOT", "AND", "AND", "OR", "OR", "XOR", "XOR",
        "TRUE", "TRUE", "FALSE", "FALSE", "TRUTHFUL", "TRUTHFUL", "SAYS", "SAYS", "COUNT", "COUNT");

    private final List<Token> tokens;
    private int position;

    private HexatrapFormulaParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static HexatrapFormula parse(String source) throws ParseException {
        if (source == null || source.isBlank()) throw new ParseException("Formula vuota", 1);
        HexatrapFormulaParser parser = new HexatrapFormulaParser(tokenize(source));
        HexatrapFormula formula = parser.iff();
        Token next = parser.peek();
        if (next.kind() != Kind.END) throw new ParseException("Simbolo inatteso «" + next.text() + "»", next.column());
        return formula;
    }

    private static List<Token> tokenize(String source) throws ParseException {
        List<Token> result = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            int column = i + 1;
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '(') { result.add(new Token(Kind.LPAREN, "(", column)); i++; continue; }
            if (c == ')') { result.add(new Token(Kind.RPAREN, ")", column)); i++; continue; }
            if (c == ',') { result.add(new Token(Kind.COMMA, ",", column)); i++; continue; }
            if (c == '=') { result.add(new Token(Kind.EQ, "=", column)); i++; continue; }
            if (source.startsWith("!=", i)) { result.add(new Token(Kind.NEQ, "!=", column)); i += 2; continue; }
            if (c == '!') { result.add(new Token(Kind.KEYWORD, "NOT", column)); i++; continue; }
            if (source.startsWith("<->", i)) { result.add(new Token(Kind.IFF, "<->", column)); i += 3; continue; }
            if (source.startsWith("->", i)) { result.add(new Token(Kind.IMPLIES, "->", column)); i += 2; continue; }
            if (source.startsWith("&&", i)) { result.add(new Token(Kind.KEYWORD, "AND", column)); i += 2; continue; }
            if (source.startsWith("||", i)) { result.add(new Token(Kind.KEYWORD, "OR", column)); i += 2; continue; }
            if (c == '&') { result.add(new Token(Kind.KEYWORD, "AND", column)); i++; continue; }
            if (c == '|') { result.add(new Token(Kind.KEYWORD, "OR", column)); i++; continue; }
            if (Character.isDigit(c)) {
                int start = i;
                while (i < source.length() && Character.isDigit(source.charAt(i))) i++;
                result.add(new Token(Kind.NUMBER, source.substring(start, i), column));
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < source.length() && (Character.isLetterOrDigit(source.charAt(i)) || source.charAt(i) == '_')) i++;
                String word = source.substring(start, i);
                if (KEYWORDS.containsKey(word)) result.add(new Token(Kind.KEYWORD, word, column));
                else if (word.equals("A") || word.equals("B")) result.add(new Token(Kind.CHARACTER, word, column));
                else if (word.matches("[a-z][a-z0-9_]*")) result.add(new Token(Kind.IDENT, word, column));
                else throw new ParseException("Identificatore non valido «" + word
                    + "»: usa minuscole, cifre e _ per fatti e opzioni, A o B per i personaggi, parole chiave in maiuscolo", column);
                continue;
            }
            throw new ParseException("Carattere non valido «" + c + "»", column);
        }
        result.add(new Token(Kind.END, "fine della formula", source.length() + 1));
        return result;
    }

    private Token peek() { return tokens.get(position); }

    private Token next() { return tokens.get(position++); }

    private boolean keyword(String word) {
        Token token = peek();
        return token.kind() == Kind.KEYWORD && token.text().equals(word);
    }

    private Token expect(Kind kind, String description) throws ParseException {
        Token token = next();
        if (token.kind() != kind) throw new ParseException("Atteso " + description + ", trovato «" + token.text() + "»", token.column());
        return token;
    }

    private HexatrapFormula iff() throws ParseException {
        HexatrapFormula left = implies();
        while (peek().kind() == Kind.IFF) {
            next();
            left = new Iff(left, implies());
        }
        return left;
    }

    private HexatrapFormula implies() throws ParseException {
        HexatrapFormula premise = or();
        if (peek().kind() == Kind.IMPLIES) {
            next();
            return new Implies(premise, implies());
        }
        return premise;
    }

    private HexatrapFormula or() throws ParseException {
        List<HexatrapFormula> operands = new ArrayList<>(List.of(xor()));
        while (keyword("OR")) { next(); operands.add(xor()); }
        return operands.size() == 1 ? operands.get(0) : new Or(List.copyOf(operands));
    }

    private HexatrapFormula xor() throws ParseException {
        HexatrapFormula left = and();
        while (keyword("XOR")) { next(); left = new Xor(left, and()); }
        return left;
    }

    private HexatrapFormula and() throws ParseException {
        List<HexatrapFormula> operands = new ArrayList<>(List.of(unary()));
        while (keyword("AND")) { next(); operands.add(unary()); }
        return operands.size() == 1 ? operands.get(0) : new And(List.copyOf(operands));
    }

    private HexatrapFormula unary() throws ParseException {
        if (keyword("NOT")) { next(); return new Not(unary()); }
        return primary();
    }

    private HexatrapFormula primary() throws ParseException {
        Token token = next();
        switch (token.kind()) {
            case LPAREN -> {
                HexatrapFormula inner = iff();
                expect(Kind.RPAREN, "«)»");
                return inner;
            }
            case IDENT -> {
                Token operator = next();
                if (operator.kind() != Kind.EQ && operator.kind() != Kind.NEQ)
                    throw new ParseException("Dopo il fatto «" + token.text() + "» serve = oppure !=", operator.column());
                Token option = expect(Kind.IDENT, "un'opzione");
                return new FactIs(token.text(), option.text(), operator.kind() == Kind.NEQ);
            }
            case KEYWORD -> {
                switch (token.text()) {
                    case "TRUE": return new Const(true);
                    case "FALSE": return new Const(false);
                    case "TRUTHFUL": {
                        expect(Kind.LPAREN, "«(» dopo TRUTHFUL");
                        Token who = expect(Kind.CHARACTER, "A o B");
                        expect(Kind.RPAREN, "«)»");
                        return new Truthful(who.text());
                    }
                    case "SAYS": {
                        expect(Kind.LPAREN, "«(» dopo SAYS");
                        Token who = expect(Kind.CHARACTER, "A o B");
                        expect(Kind.COMMA, "«,» dopo il personaggio");
                        HexatrapFormula inner = iff();
                        expect(Kind.RPAREN, "«)»");
                        return new Says(who.text(), inner);
                    }
                    case "COUNT": {
                        expect(Kind.LPAREN, "«(» dopo COUNT");
                        Token number = expect(Kind.NUMBER, "il numero di formule vere");
                        List<HexatrapFormula> operands = new ArrayList<>();
                        while (peek().kind() == Kind.COMMA) { next(); operands.add(iff()); }
                        expect(Kind.RPAREN, "«)»");
                        int k = Integer.parseInt(number.text());
                        if (operands.size() < 2) throw new ParseException("COUNT richiede almeno due formule", token.column());
                        if (k > operands.size()) throw new ParseException("COUNT: k supera il numero di formule", number.column());
                        return new Count(k, List.copyOf(operands));
                    }
                    default:
                        throw new ParseException("Parola chiave inattesa «" + token.text() + "»", token.column());
                }
            }
            default -> throw new ParseException("Atteso un fatto, una parentesi o una parola chiave, trovato «" + token.text() + "»", token.column());
        }
    }
}
