package com.expense.tracking.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"parent", "subcategories"})
@ToString(exclude = {"parent", "subcategories"})
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonBackReference
    private Category parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonManagedReference
    private List<Category> subcategories = new ArrayList<>();
    
    @Column(length = 7) // For hex color codes like #FF5733
    private String color;
    
    @Column(length = 500)
    private String description;
    
    // Helper method to check if this is a root category
    public boolean isRootCategory() {
        return parent == null;
    }
    
    // Helper method to get the full path of the category
    public String getFullPath() {
        if (parent == null) {
            return name;
        }
        return parent.getFullPath() + " > " + name;
    }
}