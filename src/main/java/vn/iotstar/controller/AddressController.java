package vn.iotstar.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.address.AddressRequest;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.AddressService;

@Controller
public class AddressController {
    private final AddressService addresses;

    public AddressController(AddressService addresses) {
        this.addresses = addresses;
    }

    @PostMapping("/profile/addresses")
    public String create(@AuthenticationPrincipal CustomerUserDetails principal,
            @Valid @ModelAttribute("addressForm") AddressRequest request,
            BindingResult result, RedirectAttributes redirect) {
        return save(principal.getId(), null, request, result, redirect);
    }

    @PostMapping("/profile/addresses/{id}")
    public String update(@AuthenticationPrincipal CustomerUserDetails principal, @PathVariable Long id,
            @Valid @ModelAttribute("addressForm") AddressRequest request,
            BindingResult result, RedirectAttributes redirect) {
        addresses.getOwned(principal.getId(), id);
        return save(principal.getId(), id, request, result, redirect);
    }

    @PostMapping("/profile/addresses/{id}/delete")
    public String delete(@AuthenticationPrincipal CustomerUserDetails principal, @PathVariable Long id,
            RedirectAttributes redirect) {
        addresses.delete(principal.getId(), id);
        redirect.addFlashAttribute("addressSuccess", "Đã xóa địa chỉ giao hàng.");
        return "redirect:/profile?section=addresses";
    }

    @PostMapping("/profile/addresses/{id}/default")
    public String makeDefault(@AuthenticationPrincipal CustomerUserDetails principal, @PathVariable Long id,
            RedirectAttributes redirect) {
        addresses.makeDefault(principal.getId(), id);
        redirect.addFlashAttribute("addressSuccess", "Đã đặt địa chỉ giao hàng mặc định.");
        return "redirect:/profile?section=addresses";
    }

    private String save(Long userId, Long id, AddressRequest request, BindingResult result,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("addressForm", request);
            redirect.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "addressForm", result);
            return "redirect:/profile?section=addresses" + (id == null ? "" : "&editAddressId=" + id) + "#address-form";
        }
        addresses.save(userId, id, request);
        redirect.addFlashAttribute("addressSuccess",
                id == null ? "Đã thêm địa chỉ giao hàng." : "Đã cập nhật địa chỉ giao hàng.");
        return "redirect:/profile?section=addresses";
    }
}
