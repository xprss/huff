package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.game.HexaflowDtos.GameStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name="hexaflow_games", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","puzzle_date"}))
public class HexaflowGameEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name="user_id", nullable=false) public String userId;
    @Column(name="puzzle_id", nullable=false) public String puzzleId;
    @Column(name="puzzle_date", nullable=false, length=10) public String puzzleDate;
    @Column(name="found_answers_json", nullable=false, columnDefinition="TEXT") public String foundAnswersJson;
    @Column(name="extra_sequences_json", nullable=false, columnDefinition="TEXT") public String extraSequencesJson;
    @Column(name="hinted_answers_json", nullable=false, columnDefinition="TEXT") public String hintedAnswersJson;
    @Column(name="hints_used", nullable=false) public int hintsUsed;
    @Column(name="event_log_json", nullable=false, columnDefinition="TEXT") public String eventLogJson;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) public GameStatus status;
    @Column(name="created_at", nullable=false) public String createdAt;
    @Column(name="updated_at", nullable=false) public String updatedAt;
    @Column(name="completed_at") public String completedAt;
}
