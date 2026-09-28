package com.rookies6.udt.admin;

import com.rookies6.udt.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 화면(T-018)이 링크하는 증빙 다운로드. 저장·권한 코드는 DisputeService 한 벌을 공유한다. */
@RestController
@RequiredArgsConstructor
public class AdminDisputeFileController {

    private final DisputeService disputeService;

    @GetMapping("/admin/disputes/{disputeId}/files/{fileId}")
    public ResponseEntity<Resource> file(@PathVariable Long disputeId, @PathVariable Long fileId) {
        DisputeService.FileContent content = disputeService.downloadForAdmin(disputeId, fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, content.contentDisposition())
                .body(content.resource());
    }
}
