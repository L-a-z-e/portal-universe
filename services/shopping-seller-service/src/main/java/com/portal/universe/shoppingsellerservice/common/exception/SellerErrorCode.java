package com.portal.universe.shoppingsellerservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SellerErrorCode implements ErrorCode {

    // Seller Errors (SL0XX)
    SELLER_NOT_FOUND(HttpStatus.NOT_FOUND, "SL001", "Seller not found"),
    SELLER_ALREADY_EXISTS(HttpStatus.CONFLICT, "SL002", "Seller already registered"),
    SELLER_SUSPENDED(HttpStatus.FORBIDDEN, "SL003", "Seller account is suspended"),
    SELLER_PENDING(HttpStatus.FORBIDDEN, "SL004", "Seller account is pending approval"),

    // Product Errors (SL1XX) - reuse S0XX codes for compatibility
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "SL101", "Product not found"),
    PRODUCT_NOT_OWNED(HttpStatus.FORBIDDEN, "SL102", "Product does not belong to this seller"),
    INVALID_PRODUCT_PRICE(HttpStatus.BAD_REQUEST, "SL103", "Product price must be greater than 0"),

    // Inventory Errors (SL2XX)
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "SL201", "Inventory not found for product"),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "SL202", "Insufficient stock available"),
    STOCK_RESERVATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SL203", "Failed to reserve stock"),
    STOCK_RELEASE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SL204", "Failed to release stock"),
    STOCK_DEDUCTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SL205", "Failed to deduct stock"),
    INVALID_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "SL206", "Stock quantity must be non-negative"),
    INVENTORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "SL207", "Inventory already exists for product"),
    CONCURRENT_STOCK_MODIFICATION(HttpStatus.CONFLICT, "SL208", "Stock was modified by another transaction"),

    // Coupon Errors (SL3XX)
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "SL301", "Coupon not found"),
    COUPON_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "SL302", "Coupon code already exists"),
    COUPON_EXHAUSTED(HttpStatus.CONFLICT, "SL303", "Coupon is exhausted"),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "SL304", "Coupon has expired"),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "SL305", "Coupon already issued to this user"),
    COUPON_NOT_STARTED(HttpStatus.BAD_REQUEST, "SL306", "Coupon issuance has not started yet"),
    COUPON_INACTIVE(HttpStatus.BAD_REQUEST, "SL307", "Coupon is not active"),

    // TimeDeal Errors (SL4XX)
    TIMEDEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "SL401", "Time deal not found"),
    TIMEDEAL_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "SL402", "Time deal is not active"),
    TIMEDEAL_INVALID_PERIOD(HttpStatus.BAD_REQUEST, "SL403", "Invalid time deal period"),
    TIMEDEAL_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "SL404", "Time deal product not found"),

    // Queue Errors (SL5XX)
    QUEUE_NOT_FOUND(HttpStatus.NOT_FOUND, "SL501", "Waiting queue not found");

    private final HttpStatus status;
    private final String code;
    private final String message;

    SellerErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
