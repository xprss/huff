package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.game.HexaflowDtos.PuzzleStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "hexaflow_puzzles")
public class HexaflowPuzzleEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name="puzzle_date", nullable=false, unique=true, length=10) public String puzzleDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) public PuzzleStatus status;
    @Column(name="theme_clue", nullable=false, length=500) public String themeClue;
    @Column(name="grid_json", nullable=false, columnDefinition="TEXT") public String gridJson;
    @Column(name="answers_json", nullable=false, columnDefinition="TEXT") public String answersJson;
    @Column(name="created_by", nullable=false) public String createdBy;
    @Column(name="updated_by", nullable=false) public String updatedBy;
    @Column(name="published_by") public String publishedBy;
    @Column(name="created_at", nullable=false) public String createdAt;
    @Column(name="updated_at", nullable=false) public String updatedAt;
    @Column(name="published_at") public String publishedAt;
}
