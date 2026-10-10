package com.syfe.pfm.config;

import com.syfe.pfm.entity.Category;
import com.syfe.pfm.entity.CategoryType;
import com.syfe.pfm.repository.CategoryRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Idempotently seeds the shared, read-only default categories at start-up. */
@Component
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<String, CategoryType> defaults = new LinkedHashMap<>();
        defaults.put("Salary", CategoryType.INCOME);
        defaults.put("Food", CategoryType.EXPENSE);
        defaults.put("Rent", CategoryType.EXPENSE);
        defaults.put("Transportation", CategoryType.EXPENSE);
        defaults.put("Entertainment", CategoryType.EXPENSE);
        defaults.put("Healthcare", CategoryType.EXPENSE);
        defaults.put("Utilities", CategoryType.EXPENSE);
        defaults.forEach((name, type) -> {
            if (!categoryRepository.existsByNameIgnoreCaseAndUserIsNull(name)) {
                categoryRepository.save(new Category(name, type, null));
            }
        });
    }
}
