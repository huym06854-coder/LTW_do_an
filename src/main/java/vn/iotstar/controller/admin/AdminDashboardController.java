package vn.iotstar.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.iotstar.repository.*;

@Controller
public class AdminDashboardController {
    private final UserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final BranchRepository branches;

    public AdminDashboardController(UserRepository users, ProductRepository products, OrderRepository orders,
            BranchRepository branches) {
        this.users = users;
        this.products = products;
        this.orders = orders;
        this.branches = branches;
    }

    @GetMapping({ "/admin", "/admin/dashboard" })
    public String dashboard(Model model) {
        model.addAttribute("userCount", users.count());
        model.addAttribute("productCount", products.count());
        model.addAttribute("orderCount", orders.count());
        model.addAttribute("branchCount", branches.count());
        return "admin/index";
    }
}
