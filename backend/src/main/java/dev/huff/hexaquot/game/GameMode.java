package dev.huff.hexaquot.game;

public enum GameMode {
    CLASSIC("Classica"),
    MISCHIEVOUS_MOUSE("Topolino dispettoso");

    private final String label;

    GameMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
