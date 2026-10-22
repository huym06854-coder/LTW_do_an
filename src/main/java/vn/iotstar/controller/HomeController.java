package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping({ "/", "/index" })
    public String home() {
        return "store/index";
    }

    @GetMapping("/about")
    public String about() {
        return "store/about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "store/contact";
    }

    @GetMapping("/news")
    public String news() {
        return "store/news";
    }

    @GetMapping("/offers")
    public String offers() {
        return "store/offers";
    }
}
