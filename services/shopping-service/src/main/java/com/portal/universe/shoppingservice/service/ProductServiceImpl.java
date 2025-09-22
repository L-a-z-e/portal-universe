package com.portal.universe.shoppingservice.service;

import com.portal.universe.shoppingservice.domain.Product;
import com.portal.universe.shoppingservice.dto.ProductCreateRequest;
import com.portal.universe.shoppingservice.dto.ProductResponse;
import com.portal.universe.shoppingservice.dto.ProductUpdateRequest;
import com.portal.universe.shoppingservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product newProduct = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .build();

        Product savedProduct = productRepository.save(newProduct);

        return convertToResponse(savedProduct);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));

        return convertToResponse(product);
    }

    @Override
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));

        product.update(
                request.name(),
                request.description(),
                request.price(),
                request.stock()
        );

        Product updatedProduct = productRepository.save(product);

        return convertToResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found");
        }

        productRepository.deleteById(productId);
    }

    private ProductResponse convertToResponse(Product product) {
        return new ProductResponse(product.getId(), product.getDescription(), product.getPrice(), product.getStock());
    }

}