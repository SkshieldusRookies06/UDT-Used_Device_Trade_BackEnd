package com.rookies6.udt.controller;

import com.rookies6.udt.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    @GetMapping("/{id}/images/{imageId}")
    public ResponseEntity<Resource> image(@PathVariable Long id, @PathVariable Long imageId) {
        ProductImageService.ImageContent content = productImageService.load(id, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .body(content.resource());
    }
}
