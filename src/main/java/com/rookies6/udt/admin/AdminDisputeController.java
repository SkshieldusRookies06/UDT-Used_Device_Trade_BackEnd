package com.rookies6.udt.admin;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.entity.DisputeDecision; // ⚠️ 실제 패키지 경로 BE-A에게 확인 필요
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminDisputeController {

    private final TransactionService transactionService; // 목록 조회(읽기 전용)에만 사용 — 아래 참고
    private final DisputeService disputeService;          // 분쟁 처리는 이 한 메서드로만 위임 (BE-A · T-010)

    @GetMapping("/admin/disputes")
    public String list(Model model) {
        model.addAttribute("disputes", transactionService.findOpenDisputes());
        return "admin/disputes";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/disputes/{disputeId}/refund")
    public String refund(@PathVariable Long disputeId,
                          @RequestParam(required = false) String adminMemo,
                          RedirectAttributes redirectAttributes) {
        try {
            disputeService.resolve(disputeId, DisputeDecision.REFUND, adminMemo);
        } catch (BusinessException e) {
            log.warn("강제 환불 실패 - disputeId={}, code={}", disputeId, e.getErrorCode().getCode());
            redirectAttributes.addFlashAttribute("disputeError", e.getErrorCode().getMessage());
        }
        return "redirect:/admin/disputes";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/disputes/{disputeId}/confirm")
    public String confirm(@PathVariable Long disputeId,
                           @RequestParam(required = false) String adminMemo,
                           RedirectAttributes redirectAttributes) {
        try {
            disputeService.resolve(disputeId, DisputeDecision.CONFIRM, adminMemo);
        } catch (BusinessException e) {
            log.warn("강제 구매확정 실패 - disputeId={}, code={}", disputeId, e.getErrorCode().getCode());
            redirectAttributes.addFlashAttribute("disputeError", e.getErrorCode().getMessage());
        }
        return "redirect:/admin/disputes";
    }
}