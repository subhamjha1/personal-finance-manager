package com.syfe.pfm.entity;

import jakarta.persistence.*;

/**
 * A transaction category. Default categories have no owner ({@code user == null}) and are shared
 * read-only by everyone; custom categories belong to exactly one user.
 */
@Entity
@Table(name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_category_user_name", columnNames = {"user_id", "name"}))
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false, length = 10)
    private CategoryType categoryType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    protected Category() {
    }

    public Category(String name, CategoryType type, User user) {
        this.name = name;
        this.categoryType = type;
        this.user = user;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public CategoryType getType() { return categoryType; }
    public User getUser() { return user; }

    /** @return true when this category was created by a user (as opposed to a built-in default) */
    public boolean isCustom() {
        return user != null;
    }
}
