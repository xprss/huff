package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.game.HexasquareDtos.Status;
import dev.huff.hexaquot.game.HexasquareGameRecord;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "hexasquare_games", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "puzzle_date"}))
public class HexasquareGameEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name="user_id", nullable=false) public String userId;
    @Column(name="puzzle_date", nullable=false, length=10) public String puzzleDate;
    @Column(name="rules_version", nullable=false) public int rulesVersion;
    @Column(name="puzzle_json", nullable=false, columnDefinition="TEXT") public String puzzleJson;
    @Column(name="placements_json", columnDefinition="TEXT") public String placementsJson;
    @Column(name="canonical_paths_json", columnDefinition="TEXT") public String canonicalPathsJson;
    @Enumerated(EnumType.STRING) @Column(name="status", nullable=false, length=20) public Status status;
    @Column(name="simulations_count", nullable=false) public int simulationsCount;
    @Column(name="used_cells") public Integer usedCells;
    @Column(name="remaining_cells") public Integer remainingCells;
    @Column(name="created_at", nullable=false) public String createdAt;
    @Column(name="updated_at", nullable=false) public String updatedAt;
    @Column(name="completed_at") public String completedAt;

    public HexasquareGameRecord toRecord() { return new HexasquareGameRecord(id,userId,puzzleDate,rulesVersion,puzzleJson,
        placementsJson,canonicalPathsJson,status,simulationsCount,usedCells,remainingCells,createdAt,updatedAt,completedAt); }
}
