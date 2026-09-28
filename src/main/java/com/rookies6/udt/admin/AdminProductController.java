package com.rookies6.udt.admin;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.service.ProductService;
import com.rookies6.udt.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final TransactionService transactionService;

    @GetMapping("/admin/products")
    public String list(Model model) {
        model.addAttribute("products", productService.findInspectingProducts());
        return "admin/products";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/products/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            transactionService.approveInspection(id); // BR-01
        } catch (BusinessException e) {
            log.warn("상품 승인 실패 - productId={}, code={}", id, e.getErrorCode().name());
            redirectAttributes.addFlashAttribute("approveError", e.getErrorCode().getMessage());
        }
        return "redirect:/admin/products";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/products/{id}/reject")
    public String reject(@PathVariable Long id,
                         @ModelAttribute("reason") String reason,
                         Model model) {
        if (reason == null || reason.isBlank()) {
            model.addAttribute("rejectError", "반려 사유를 입력해주세요");
            model.addAttribute("products", productService.findInspectingProducts());
            return "admin/products"; // redirect 아님 — T-018 수용 기준
        }

        try {
            transactionService.rejectInspection(id, reason); // BR-02
        } catch (BusinessException e) {
            log.warn("상품 반려 실패 - productId={}, code={}", id, e.getErrorCode().name());
            model.addAttribute("rejectError", e.getErrorCode().getMessage());
            model.addAttribute("products", productService.findInspectingProducts());
            return "admin/products";
        }
        return "redirect:/admin/products";
    }
}