package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "word_explanations")
public class WordExplanationEntity extends PanacheEntityBase {
    @Id
    @Column(nullable = false, length = 6)
    public String word;

    @Column(nullable = false, columnDefinition = "text")
    public String explanation;

    @Column(name = "generated_at", nullable = false, length = 255)
    public String generatedAt;
}
