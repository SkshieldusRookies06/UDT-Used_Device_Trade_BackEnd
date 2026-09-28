package com.rookies6.udt.admin;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.service.DisputeService;
import com.rookies6.udt.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminDisputeController {

    private final DisputeService disputeService;          // 목록 조회 (BE-A)
    private final TransactionService transactionService;  // 강제 환불/확정 (BE-D)

    @GetMapping("/admin/disputes")
    public String list(Model model) {
        model.addAttribute("disputes", disputeService.findOpenDisputes());
        return "admin/disputes";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/disputes/{transactionId}/refund")
    public String refund(@PathVariable Long transactionId, RedirectAttributes redirectAttributes) {
        try {
            transactionService.forceRefund(transactionId); // BR-07
        } catch (BusinessException e) {
            log.warn("강제 환불 실패 - transactionId={}, code={}", transactionId, e.getErrorCode().getCode());
            redirectAttributes.addFlashAttribute("disputeError", e.getErrorCode().getMessage());
        }
        return "redirect:/admin/disputes";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/disputes/{transactionId}/confirm")
    public String confirm(@PathVariable Long transactionId, RedirectAttributes redirectAttributes) {
        try {
            transactionService.forceConfirm(transactionId); // BR-08
        } catch (BusinessException e) {
            log.warn("강제 구매확정 실패 - transactionId={}, code={}", transactionId, e.getErrorCode().getCode());
            redirectAttributes.addFlashAttribute("disputeError", e.getErrorCode().getMessage());
        }
        return "redirect:/admin/disputes";
    }
}