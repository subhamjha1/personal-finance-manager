package com.syfe.pfm.repository;

import com.syfe.pfm.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence access for {@link Category}. "Visible" means default categories plus the user's own. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("select c from Category c left join c.user u where c.user is null or u.id = :userId order by c.id")
    List<Category> findVisibleToUser(@Param("userId") Long userId);

    @Query("select c from Category c left join c.user u "
            + "where lower(c.name) = lower(:name) and (c.user is null or u.id = :userId)")
    Optional<Category> findVisibleByName(@Param("name") String name, @Param("userId") Long userId);

    boolean existsByNameIgnoreCaseAndUserIsNull(String name);
}
