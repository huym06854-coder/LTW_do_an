package vn.iotstar.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.Category;
import vn.iotstar.repository.CategoryRepository;
import vn.iotstar.exception.ResourceNotFoundException;

@Service
public class CategoryService {
    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Category> all() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Category get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
    }

    @Transactional
    public Category create(String name, String description) {
        Category c = new Category();
        c.setName(name.trim());
        c.setDescription(description);
        return repository.save(c);
    }
}
