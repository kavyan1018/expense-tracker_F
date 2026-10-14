package com.expensetracker.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TxnType type;

    @Column(length = 7)
    private String color;

    @Column(nullable = false)
    private boolean archived;

    protected Category() {
    }

    public Category(Long userId, String name, TxnType type, String color) {
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.color = color;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public TxnType getType() { return type; }
    public String getColor() { return color; }
    public boolean isArchived() { return archived; }

    public void setName(String name) { this.name = name; }
    public void setColor(String color) { this.color = color; }
    public void setArchived(boolean archived) { this.archived = archived; }
}
