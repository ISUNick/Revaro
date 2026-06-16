package com.revaro.repository;

import com.revaro.entity.Tag;
import com.revaro.enums.TagCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByCategory(TagCategory category);
    List<Tag> findAllByOrderByCategoryAscNameAsc();
    Optional<Tag> findByName(String name);
}
