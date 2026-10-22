package vn.iotstar.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.service.BranchService;

@Controller
@RequestMapping("/admin/branches")
public class AdminBranchController {
    private final BranchService branches;

    public AdminBranchController(BranchService branches) {
        this.branches = branches;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("branches", branches.active());
        return "admin/branches";
    }

    @PostMapping
    public String create(@RequestParam String code, @RequestParam String name, @RequestParam String address) {
        branches.create(code, name, address);
        return "redirect:/admin/branches";
    }
}
