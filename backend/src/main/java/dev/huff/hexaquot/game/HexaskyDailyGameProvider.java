package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexaskyDtos.VisibilityDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Deterministic 4×4 Sudoku-style skyscraper puzzles. The catalogue is exhaustively checked for a unique clue signature. */
@ApplicationScoped
public class HexaskyDailyGameProvider {
    /** Version 1 had row and column constraints only; version 2 also requires unique 2×2 quadrants. */
    public static final int RULES_VERSION = 2;
    public static final int SIDE = 4;
    private static final List<List<Integer>> LEGACY_UNIQUE_PUZZLES = uniquePuzzles(false);
    private static final List<List<Integer>> UNIQUE_PUZZLES = uniquePuzzles(true);

    @ConfigProperty(name = "app.hexasky.seed") String seed;

    public List<Integer> solutionFor(String date) {
        return solutionFor(date, RULES_VERSION);
    }

    public List<Integer> solutionFor(String date, int rulesVersion) {
        try {
            List<List<Integer>> puzzles = puzzlesFor(rulesVersion);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest((seed + ":" + date + ":v" + rulesVersion).getBytes(StandardCharsets.UTF_8));
            int index = Math.floorMod(java.nio.ByteBuffer.wrap(digest).getInt(), puzzles.size());
            return puzzles.get(index);
        } catch (Exception error) { throw new IllegalStateException("Cannot derive Hexasky puzzle", error); }
    }

    public VisibilityDto cluesFor(List<Integer> grid) {
        List<Integer> top=new ArrayList<>(), right=new ArrayList<>(), bottom=new ArrayList<>(), left=new ArrayList<>();
        for(int col=0;col<SIDE;col++) { top.add(visible(grid,col,4)); bottom.add(visible(grid,12+col,-4)); }
        for(int row=0;row<SIDE;row++) { left.add(visible(grid,row*4,1)); right.add(visible(grid,row*4+3,-1)); }
        return new VisibilityDto(List.copyOf(top),List.copyOf(right),List.copyOf(bottom),List.copyOf(left));
    }

    public List<Integer> validateSolution(List<Integer> candidate) {
        return validateSolution(candidate, RULES_VERSION);
    }

    public List<Integer> validateSolution(List<Integer> candidate, int rulesVersion) {
        boolean requiresQuadrants = rulesVersion == RULES_VERSION;
        if (!requiresQuadrants && rulesVersion != 1) throw new BadRequestException("Versione delle regole non valida.");
        if(candidate == null || candidate.size()!=16) throw new BadRequestException("Inserisci tutte le 16 celle.");
        for(int row=0;row<SIDE;row++) {
            Set<Integer> values=new HashSet<>();
            for(int col=0;col<SIDE;col++) { int value=requireValue(candidate.get(row*4+col)); values.add(value); }
            if(values.size()!=SIDE) throw new BadRequestException("Ogni riga deve contenere i numeri da 1 a 4 senza ripetizioni.");
        }
        for(int col=0;col<SIDE;col++) {
            Set<Integer> values=new HashSet<>(); for(int row=0;row<SIDE;row++) values.add(requireValue(candidate.get(row*4+col)));
            if(values.size()!=SIDE) throw new BadRequestException("Ogni colonna deve contenere i numeri da 1 a 4 senza ripetizioni.");
        }
        if (requiresQuadrants) {
            for (int row = 0; row < SIDE; row += 2) for (int col = 0; col < SIDE; col += 2) {
                Set<Integer> values = new HashSet<>();
                for (int r = row; r < row + 2; r++) for (int c = col; c < col + 2; c++) values.add(requireValue(candidate.get(r * SIDE + c)));
                if (values.size() != SIDE) throw new BadRequestException("Ogni quadrante 2 per 2 deve contenere i numeri da 1 a 4 senza ripetizioni.");
            }
        }
        return List.copyOf(candidate);
    }
    public String validateRequestId(String requestId) {
        if(requestId==null || requestId.isBlank() || requestId.length()>128) throw new BadRequestException("requestId non valido.");
        return requestId;
    }
    private int requireValue(Integer value) { if(value==null || value<1 || value>4) throw new BadRequestException("Usa solo i numeri da 1 a 4."); return value; }
    private int visible(List<Integer> grid, int start, int step) { int max=0,count=0; for(int i=0;i<SIDE;i++){int value=grid.get(start+i*step);if(value>max){max=value;count++;}}return count; }
    private static String signature(VisibilityDto c) { return c.top()+"/"+c.right()+"/"+c.bottom()+"/"+c.left(); }
    private static List<List<Integer>> puzzlesFor(int rulesVersion) {
        return switch (rulesVersion) {
            case 1 -> LEGACY_UNIQUE_PUZZLES;
            case RULES_VERSION -> UNIQUE_PUZZLES;
            default -> throw new BadRequestException("Versione delle regole non valida.");
        };
    }
    private static List<List<Integer>> uniquePuzzles(boolean requiresQuadrants) {
        List<List<Integer>> all=new ArrayList<>(); enumerate(new ArrayList<>(), all, requiresQuadrants);
        Map<String,List<List<Integer>>> groups=new HashMap<>();
        for(List<Integer> grid:all) { VisibilityDto c=clues(grid); groups.computeIfAbsent(signature(c),ignored->new ArrayList<>()).add(grid); }
        return groups.values().stream().filter(list->list.size()==1).map(list->List.copyOf(list.get(0))).toList();
    }
    private static void enumerate(List<Integer> cells,List<List<Integer>> output, boolean requiresQuadrants) {
        if(cells.size()==16){output.add(List.copyOf(cells));return;}
        int row=cells.size()/4,col=cells.size()%4;
        for(int value=1;value<=4;value++) { if(inRow(cells,row,value)||inColumn(cells,row,col,value)||(requiresQuadrants&&inQuadrant(cells,row,col,value)))continue;cells.add(value);enumerate(cells,output,requiresQuadrants);cells.remove(cells.size()-1); }
    }
    private static boolean inRow(List<Integer> c,int row,int v){for(int col=0;col<4;col++){int i=row*4+col;if(i<c.size()&&c.get(i)==v)return true;}return false;}
    private static boolean inColumn(List<Integer> c,int row,int col,int v){for(int r=0;r<row;r++)if(c.get(r*4+col)==v)return true;return false;}
    private static boolean inQuadrant(List<Integer> c, int row, int col, int value) { int firstRow=row/2*2,firstCol=col/2*2; for(int r=firstRow;r<=row;r++)for(int column=firstCol;column<firstCol+2;column++){int index=r*SIDE+column;if(index<c.size()&&c.get(index)==value)return true;}return false; }
    private static VisibilityDto clues(List<Integer> grid) { List<Integer> t=new ArrayList<>(),r=new ArrayList<>(),b=new ArrayList<>(),l=new ArrayList<>();for(int i=0;i<4;i++){t.add(visibleStatic(grid,i,4));b.add(visibleStatic(grid,12+i,-4));l.add(visibleStatic(grid,i*4,1));r.add(visibleStatic(grid,i*4+3,-1));}return new VisibilityDto(t,r,b,l); }
    private static int visibleStatic(List<Integer> grid,int start,int step){int max=0,count=0;for(int i=0;i<4;i++){int value=grid.get(start+i*step);if(value>max){max=value;count++;}}return count;}
}
