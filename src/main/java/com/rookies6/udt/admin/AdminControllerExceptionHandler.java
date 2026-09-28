package com.rookies6.udt.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice(basePackages = "com.rookies6.udt.admin")
public class AdminControllerExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception e,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        log.error("관리자 화면에서 예상치 못한 오류 발생 - uri={}", request.getRequestURI(), e);
        redirectAttributes.addFlashAttribute("adminError", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin");
    }
}