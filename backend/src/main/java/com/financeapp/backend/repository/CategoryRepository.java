package com.financeapp.backend.repository;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByUserOrderByNameAsc(AppUser user);
    Optional<Category> findByIdAndUser(Long id, AppUser user);
    boolean existsByUserAndNameIgnoreCase(AppUser user, String name);
    boolean existsByUserAndNameIgnoreCaseAndIdNot(AppUser user, String name, Long id);
}
