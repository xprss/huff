package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.game.HexasquareSimulationRecord;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name="hexasquare_simulations", uniqueConstraints=@UniqueConstraint(columnNames={"game_id","request_id"}))
public class HexasquareSimulationEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name="game_id", nullable=false) public String gameId;
    @Column(name="request_id", nullable=false, length=128) public String requestId;
    @Column(name="placements_json", nullable=false, columnDefinition="TEXT") public String placementsJson;
    @Column(name="outcome_json", nullable=false, columnDefinition="TEXT") public String outcomeJson;
    @Column(name="successful", nullable=false) public boolean successful;
    @Column(name="created_at", nullable=false) public String createdAt;

    public HexasquareSimulationRecord toRecord() { return new HexasquareSimulationRecord(id,gameId,requestId,
        placementsJson,outcomeJson,successful,createdAt); }
}
