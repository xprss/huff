package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "hexatrap_actions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "request_id"}),
    @UniqueConstraint(columnNames = {"game_id", "action_key"}),
    @UniqueConstraint(columnNames = {"game_id", "sequence"})})
public class HexatrapActionEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name = "game_id", nullable = false) public String gameId;
    @Column(name = "user_id", nullable = false) public String userId;
    @Column(nullable = false) public int sequence;
    @Column(name = "request_id", nullable = false, length = 128) public String requestId;
    @Column(nullable = false, columnDefinition = "TEXT") public String fingerprint;
    @Column(nullable = false, length = 10) public String kind;
    @Column(name = "action_key", nullable = false, length = 64) public String actionKey;
    @Column(name = "response_json", nullable = false, columnDefinition = "TEXT") public String responseJson;
    @Column(name = "created_at", nullable = false) public String createdAt;
}
