package vn.iotstar.controller.admin;

import java.time.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.service.ReportService;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {
    private final ReportService reports;

    public AdminReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping
    public String page(@RequestParam(required = false) Long branchId, Model model) {
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        model.addAttribute("total", reports.revenueByCompletedDate(branchId, end.minusDays(30), end));
        return "admin/reports";
    }
}
