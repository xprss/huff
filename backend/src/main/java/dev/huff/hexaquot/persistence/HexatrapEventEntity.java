package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name="hexatrap_events", uniqueConstraints={
    @UniqueConstraint(columnNames={"game_id", "sequence"}),
    @UniqueConstraint(columnNames={"user_id", "request_id"})})
public class HexatrapEventEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name="game_id", nullable=false) public String gameId;
    @Column(name="user_id", nullable=false) public String userId;
    @Column(nullable=false) public long sequence;
    @Column(name="request_id", nullable=false, length=128) public String requestId;
    @Column(nullable=false, columnDefinition="TEXT") public String fingerprint;
    @Column(name="event_json", nullable=false, columnDefinition="TEXT") public String eventJson;
    @Column(name="response_json", nullable=false, columnDefinition="TEXT") public String responseJson;
}
