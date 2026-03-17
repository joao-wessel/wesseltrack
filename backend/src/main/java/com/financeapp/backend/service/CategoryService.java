package com.financeapp.backend.service;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.Category;
import com.financeapp.backend.dto.CategoryRequest;
import com.financeapp.backend.dto.CategoryResponse;
import com.financeapp.backend.repository.ExpenseRepository;
import com.financeapp.backend.repository.CategoryRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final CurrentUserService currentUserService;

    public List<CategoryResponse> list() {
        AppUser user = currentUserService.requireCurrentUser();
        return categoryRepository.findAllByUserOrderByNameAsc(user).stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName(), category.getColor()))
                .toList();
    }

    public CategoryResponse create(CategoryRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        if (categoryRepository.existsByUserAndNameIgnoreCase(user, request.name().trim())) {
            throw new EntityExistsException("Categoria já cadastrada.");
        }

        Category category = categoryRepository.save(Category.builder()
                .user(user)
                .name(request.name().trim())
                .color(request.color())
                .build());
        return new CategoryResponse(category.getId(), category.getName(), category.getColor());
    }

    public Category requireOwnedCategory(Long id, AppUser user) {
        return categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        Category category = requireOwnedCategory(id, user);

        if (categoryRepository.existsByUserAndNameIgnoreCaseAndIdNot(user, request.name().trim(), id)) {
            throw new EntityExistsException("Categoria já cadastrada.");
        }

        category.setName(request.name().trim());
        category.setColor(request.color());
        category = categoryRepository.save(category);
        return new CategoryResponse(category.getId(), category.getName(), category.getColor());
    }

    public void delete(Long id) {
        AppUser user = currentUserService.requireCurrentUser();
        Category category = requireOwnedCategory(id, user);

        if (expenseRepository.countByUserAndCategory(user, category) > 0) {
            throw new IllegalArgumentException("Não é possível excluir uma categoria já usada em despesas.");
        }

        categoryRepository.delete(category);
    }
}
