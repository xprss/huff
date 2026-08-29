package dev.huff.hexaquot.game;

import jakarta.enterprise.context.ApplicationScoped;
import java.text.Normalizer;
import java.util.*;

@ApplicationScoped
public class HexaflowPuzzleValidator {
    public List<HexaflowDtos.ValidationErrorDto> validate(HexaflowDtos.PuzzleDraftDto draft) {
        List<HexaflowDtos.ValidationErrorDto> errors = new ArrayList<>();
        if (draft == null) return List.of(error("MISSING", "puzzle", "Puzzle mancante.", null, null));
        List<String> grid = draft.grid() == null ? List.of() : draft.grid();
        if (grid.size() != 48) errors.add(error("GRID_SIZE", "grid", "La griglia deve contenere 48 celle.", null, null));
        for (int i=0;i<grid.size();i++) if (grid.get(i)==null || !grid.get(i).trim().toUpperCase(Locale.ROOT).matches("[A-Z]"))
            errors.add(error("GRID_LETTER", "grid", "Ogni cella deve contenere una lettera A-Z.", null, i));
        List<HexaflowDtos.AnswerDto> answers = draft.answers()==null ? List.of() : draft.answers();
        long flows=answers.stream().filter(a->a!=null&&a.type()==HexaflowDtos.AnswerType.FLOW).count();
        long themes=answers.stream().filter(a->a!=null&&a.type()==HexaflowDtos.AnswerType.THEME).count();
        if(flows!=1) errors.add(error("FLOW_COUNT","answers","Serve esattamente un Flusso.",null,null));
        if(themes<1) errors.add(error("THEME_COUNT","answers","Serve almeno una parola tema.",null,null));
        Set<Integer> covered=new HashSet<>(); Set<String> ids=new HashSet<>();
        for(int ai=0;ai<answers.size();ai++) {
            HexaflowDtos.AnswerDto a=answers.get(ai);
            if(a==null){errors.add(error("ANSWER_MISSING","answers","Risposta non valida.",ai,null));continue;}
            if(a.id()==null||a.id().isBlank()||!ids.add(a.id())) errors.add(error("ANSWER_ID","answers","Ogni risposta deve avere un id univoco.",ai,null));
            String normalized=normalize(a.label());
            if(normalized.length()<4) errors.add(error("ANSWER_LENGTH","answers","Le risposte devono avere almeno quattro lettere.",ai,null));
            List<Integer> path=a.path()==null?List.of():a.path(); Set<Integer> own=new HashSet<>(); StringBuilder letters=new StringBuilder();
            for(int pi=0;pi<path.size();pi++) {
                Integer cell=path.get(pi);
                if(cell==null||cell<0||cell>=48){errors.add(error("CELL_RANGE","answers","Indice cella non valido.",ai,cell));continue;}
                if(!own.add(cell)) errors.add(error("CELL_REUSED","answers","Una risposta non può riutilizzare una cella.",ai,cell));
                if(!covered.add(cell)) errors.add(error("CELL_OVERLAP","answers","I percorsi delle risposte devono essere disgiunti.",ai,cell));
                if(pi>0 && !adjacent(path.get(pi-1),cell)) errors.add(error("NOT_ADJACENT","answers","Le celle consecutive devono essere adiacenti.",ai,cell));
                if(cell<grid.size()&&grid.get(cell)!=null) letters.append(grid.get(cell).trim().toUpperCase(Locale.ROOT));
            }
            if(!normalized.equals(letters.toString())) errors.add(error("LABEL_MISMATCH","answers","L'etichetta non coincide con le lettere del percorso.",ai,null));
            if(a.type()==HexaflowDtos.AnswerType.FLOW && !touchesOppositeSides(path)) errors.add(error("FLOW_SIDES","answers","Il Flusso deve toccare due lati opposti.",ai,null));
        }
        if(covered.size()!=48) errors.add(error("GRID_COVERAGE","answers","I percorsi devono coprire tutte le 48 celle.",null,null));
        return List.copyOf(errors);
    }
    public static String normalize(String value) {
        if(value==null)return "";
        return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toUpperCase(Locale.ROOT).replaceAll("[ \\'’-]","");
    }
    public static boolean adjacent(int a,int b){int ar=a/6,ac=a%6,br=b/6,bc=b%6;return a!=b&&Math.abs(ar-br)<=1&&Math.abs(ac-bc)<=1;}
    private boolean touchesOppositeSides(List<Integer> p){if(p==null)return false;boolean top=false,bottom=false,left=false,right=false;for(int c:p){top|=c<6;bottom|=c>=42;left|=c%6==0;right|=c%6==5;}return top&&bottom||left&&right;}
    private HexaflowDtos.ValidationErrorDto error(String c,String f,String m,Integer a,Integer cell){return new HexaflowDtos.ValidationErrorDto(c,f,m,a,cell);}
}
