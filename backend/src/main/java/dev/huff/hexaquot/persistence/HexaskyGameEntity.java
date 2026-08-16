package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.game.HexaskyDtos.Status;
import dev.huff.hexaquot.game.HexaskyGameRecord;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "hexasky_games", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "puzzle_date"}))
public class HexaskyGameEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name = "user_id", nullable = false) public String userId;
    @Column(name = "puzzle_date", nullable = false, length = 10) public String puzzleDate;
    @Column(name = "rules_version", nullable = false) public int rulesVersion;
    @Column(name = "solution_json", nullable = false, columnDefinition = "TEXT") public String solutionJson;
    @Column(name = "proposal_json", columnDefinition = "TEXT") public String proposalJson;
    @Column(name = "event_log_json", nullable = false, columnDefinition = "TEXT") public String eventLogJson;
    @Column(name = "checks_used", nullable = false) public int checksUsed;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20) public Status status;
    @Column(name = "created_at", nullable = false) public String createdAt;
    @Column(name = "updated_at", nullable = false) public String updatedAt;
    @Column(name = "completed_at") public String completedAt;

    public static HexaskyGameEntity fromRecord(HexaskyGameRecord r) {
        HexaskyGameEntity e = new HexaskyGameEntity();
        e.id=r.id(); e.userId=r.userId(); e.puzzleDate=r.puzzleDate(); e.rulesVersion=r.rulesVersion();
        e.solutionJson=r.solutionJson(); e.proposalJson=r.proposalJson(); e.eventLogJson=r.eventLogJson();
        e.checksUsed=r.checksUsed(); e.status=r.status(); e.createdAt=r.createdAt(); e.updatedAt=r.updatedAt(); e.completedAt=r.completedAt();
        return e;
    }
    public HexaskyGameRecord toRecord() { return new HexaskyGameRecord(id,userId,puzzleDate,rulesVersion,solutionJson,proposalJson,eventLogJson,checksUsed,status,createdAt,updatedAt,completedAt); }
}
