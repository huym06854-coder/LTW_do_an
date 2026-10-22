package vn.iotstar.controller.staff;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class StaffDashboardController {
    @GetMapping({"/staff","/staff/dashboard"})public String dashboard(){return "redirect:/staff/orders";}
}
