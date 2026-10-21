package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StorePageController {

	@GetMapping({ "/", "/index" })
	public String home() {
		return "store/index";
	}

	@GetMapping("/products")
	public String products() {
		return "store/products";
	}

	@GetMapping("/product-detail")
	public String productDetail() {
		return "store/product-detail";
	}

	@GetMapping("/cart")
	public String cart() {
		return "store/cart";
	}

	@GetMapping("/login")
	public String login() {
		return "store/login";
	}

	@GetMapping("/register")
	public String register() {
		return "auth/register";
	}

	@GetMapping("/checkout")
	public String checkout() {
		return "cart/checkout";
	}

	@GetMapping("/profile")
	public String profile() {
		return "account/profile";
	}

	@GetMapping("/orders")
	public String orders() {
		return "orders/list";
	}

	@GetMapping("/orders/{id}")
	public String orderDetail() {
		return "orders/detail";
	}

	@GetMapping("/staff/orders")
	public String staffOrders() {
		return "staff/orders";
	}

	@GetMapping("/staff/orders/{id}")
	public String staffOrderDetail() {
		return "staff/order-detail";
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

	@GetMapping("/admin")
	public String admin() {
		return "admin/index";
	}
}
