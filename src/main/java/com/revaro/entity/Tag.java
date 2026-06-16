package com.revaro.entity;

import com.revaro.enums.TagCategory;
import jakarta.persistence.*;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagCategory category;

    public Tag() {}

    public Tag(String name, TagCategory category) {
        this.name = name;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TagCategory getCategory() { return category; }
    public void setCategory(TagCategory category) { this.category = category; }
}
