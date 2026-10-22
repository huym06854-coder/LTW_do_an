package vn.iotstar.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.Branch;
import vn.iotstar.domain.enums.BranchStatus;
import vn.iotstar.exception.*;
import vn.iotstar.repository.BranchRepository;

@Service
public class BranchService {
    private final BranchRepository repository;

    public BranchService(BranchRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Branch> active() {
        return repository.findByStatusOrderByNameAsc(BranchStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Branch getActive(Long id) {
        Branch b = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
        if (b.getStatus() != BranchStatus.ACTIVE)
            throw new BusinessException("Chi nhánh đã ngừng hoạt động");
        return b;
    }

    @Transactional
    public Branch create(String code, String name, String address) {
        Branch b = new Branch();
        b.setCode(code.trim());
        b.setName(name.trim());
        b.setAddress(address.trim());
        return repository.save(b);
    }
}
