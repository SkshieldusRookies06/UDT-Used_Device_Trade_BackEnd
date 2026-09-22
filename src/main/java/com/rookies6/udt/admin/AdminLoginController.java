package com.rookies6.udt.admin;

// TODO(T-018) BE-B — 로그인 화면 GET만 있다. 검수·분쟁 화면 컨트롤러는 T-018에서 이 패키지에 추가한다

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminLoginController {

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }
}
