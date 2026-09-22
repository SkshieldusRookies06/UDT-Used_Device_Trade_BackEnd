package com.rookies6.udt.service;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.entity.ProductImage;
import com.rookies6.udt.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final FileStorageService fileStorageService;

    public ImageContent load(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        Resource resource = fileStorageService.load(FileStorageService.PRODUCTS, image.getStoredName());
        if (!resource.exists() || !resource.isReadable()) {
            log.warn("이미지 행은 있는데 파일이 없다 imageId={} storedName={}", imageId, image.getStoredName());
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return new ImageContent(resource, fileStorageService.contentType(image.getStoredName()));
    }

    public record ImageContent(Resource resource, String contentType) {
    }
}
