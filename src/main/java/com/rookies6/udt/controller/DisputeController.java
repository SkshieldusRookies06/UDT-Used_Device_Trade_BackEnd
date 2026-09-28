package com.rookies6.udt.controller;

import com.rookies6.udt.common.ApiResponse;
import com.rookies6.udt.dto.DisputeCreateRequest;
import com.rookies6.udt.dto.DisputeResponse;
import com.rookies6.udt.security.CurrentUser;
import com.rookies6.udt.service.DisputeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping(value = "/api/transactions/{transactionId}/disputes",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DisputeResponse> open(
            @PathVariable Long transactionId,
            @RequestPart("dispute") @Valid DisputeCreateRequest dispute,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        DisputeResponse response = disputeService.open(CurrentUser.id(), transactionId, dispute, files);
        return ApiResponse.of(response, "분쟁이 접수되었습니다");
    }

    @GetMapping("/api/disputes/{disputeId}/files/{fileId}")
    public ResponseEntity<Resource> file(@PathVariable Long disputeId, @PathVariable Long fileId) {
        DisputeService.FileContent content =
                disputeService.downloadForParty(disputeId, fileId, CurrentUser.id());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, content.contentDisposition())
                .body(content.resource());
    }
}
