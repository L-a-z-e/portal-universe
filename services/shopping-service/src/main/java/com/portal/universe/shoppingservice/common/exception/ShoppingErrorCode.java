package com.portal.universe.shoppingservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 쇼핑(Shopping) 서비스에서 발생하는 비즈니스 예외에 대한 오류 코드를 정의하는 열거형 클래스입니다.
 *
 * 에러코드 체계:
 * - S0XX: Product (S001-S010)
 * - S1XX: Cart (S101-S110)
 * - S2XX: Order (S201-S220)
 * - S3XX: Payment (S301-S315)
 * - S4XX: Inventory (S401-S410)
 * - S5XX: Delivery (S501-S510)
 * - S9XX: Saga/System (S901-S910)
 */
@Getter
public enum ShoppingErrorCode implements ErrorCode {

    // ========================================
    // Product Errors (S0XX)
    // ========================================
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Product not found"),
    PRODUCT_ALREADY_EXISTS(HttpStatus.CONFLICT, "S002", "Product with this name already exists"),
    PRODUCT_INACTIVE(HttpStatus.BAD_REQUEST, "S003", "Product is currently inactive"),
    INVALID_PRODUCT_PRICE(HttpStatus.BAD_REQUEST, "S004", "Product price must be greater than 0"),
    INVALID_PRODUCT_QUANTITY(HttpStatus.BAD_REQUEST, "S005", "Product quantity must be greater than 0"),
    INVALID_PRICE(HttpStatus.BAD_REQUEST, "S006", "Invalid price"),
    INVALID_STOCK_QUANTITY_PARAM(HttpStatus.BAD_REQUEST, "S007", "Invalid stock quantity"),
    PRODUCT_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "S008", "Product name already exists"),
    CANNOT_DELETE_PRODUCT_WITH_ORDERS(HttpStatus.CONFLICT, "S009", "Cannot delete product with active orders"),
    STOCK_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S010", "Stock update failed"),

    // ========================================
    // Cart Errors (S1XX)
    // ========================================
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "S101", "Cart not found"),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "S102", "Cart item not found"),
    CART_ALREADY_CHECKED_OUT(HttpStatus.BAD_REQUEST, "S103", "Cart has already been checked out"),
    CART_EMPTY(HttpStatus.BAD_REQUEST, "S104", "Cart is empty"),
    CART_ITEM_QUANTITY_EXCEEDED(HttpStatus.BAD_REQUEST, "S105", "Requested quantity exceeds available stock"),
    CART_ITEM_ALREADY_EXISTS(HttpStatus.CONFLICT, "S106", "Product already exists in cart"),
    INVALID_CART_ITEM_QUANTITY(HttpStatus.BAD_REQUEST, "S107", "Cart item quantity must be greater than 0"),

    // ========================================
    // Order Errors (S2XX)
    // ========================================
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "S201", "Order not found"),
    ORDER_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "S202", "Order has already been cancelled"),
    ORDER_CANNOT_BE_CANCELLED(HttpStatus.BAD_REQUEST, "S203", "Order cannot be cancelled in current status"),
    ORDER_ALREADY_PAID(HttpStatus.BAD_REQUEST, "S204", "Order has already been paid"),
    ORDER_NOT_PAID(HttpStatus.BAD_REQUEST, "S205", "Order has not been paid yet"),
    ORDER_ALREADY_SHIPPED(HttpStatus.BAD_REQUEST, "S206", "Order has already been shipped"),
    ORDER_ALREADY_DELIVERED(HttpStatus.BAD_REQUEST, "S207", "Order has already been delivered"),
    ORDER_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S208", "Failed to create order"),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "S209", "Invalid order status transition"),
    ORDER_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "S210", "Order amount does not match"),
    INVALID_SHIPPING_ADDRESS(HttpStatus.BAD_REQUEST, "S211", "Invalid shipping address"),
    ORDER_USER_MISMATCH(HttpStatus.FORBIDDEN, "S212", "Order does not belong to current user"),

    // ========================================
    // Payment Errors (S3XX)
    // ========================================
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "S301", "Payment not found"),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "S302", "Payment has already been completed"),
    PAYMENT_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "S303", "Payment has already been cancelled"),
    PAYMENT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S304", "Payment processing failed"),
    PAYMENT_REFUND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S305", "Payment refund failed"),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "S306", "Invalid payment amount"),
    PAYMENT_METHOD_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "S307", "Payment method not supported"),
    PAYMENT_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "S308", "Payment request timed out"),
    PAYMENT_CANCELLED_BY_USER(HttpStatus.BAD_REQUEST, "S309", "Payment cancelled by user"),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "S310", "Insufficient balance for payment"),
    PG_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "S311", "Payment gateway connection error"),
    PAYMENT_CANNOT_BE_CANCELLED(HttpStatus.BAD_REQUEST, "S312", "Payment cannot be cancelled in current status"),
    PAYMENT_USER_MISMATCH(HttpStatus.FORBIDDEN, "S313", "Payment does not belong to current user"),

    // ========================================
    // Inventory Errors (S4XX)
    // ========================================
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "S401", "Inventory not found for product"),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "S402", "Insufficient stock available"),
    STOCK_RESERVATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S403", "Failed to reserve stock"),
    STOCK_RELEASE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S404", "Failed to release stock"),
    STOCK_DEDUCTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S405", "Failed to deduct stock"),
    INVALID_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "S406", "Stock quantity must be non-negative"),
    INVENTORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "S407", "Inventory already exists for product"),
    CONCURRENT_STOCK_MODIFICATION(HttpStatus.CONFLICT, "S408", "Stock was modified by another transaction"),

    // ========================================
    // Delivery Errors (S5XX)
    // ========================================
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "S501", "Delivery not found"),
    DELIVERY_ALREADY_SHIPPED(HttpStatus.BAD_REQUEST, "S502", "Delivery has already been shipped"),
    DELIVERY_ALREADY_DELIVERED(HttpStatus.BAD_REQUEST, "S503", "Delivery has already been delivered"),
    DELIVERY_CANNOT_BE_CANCELLED(HttpStatus.BAD_REQUEST, "S504", "Delivery cannot be cancelled in current status"),
    DELIVERY_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S505", "Failed to create delivery"),
    INVALID_DELIVERY_STATUS(HttpStatus.BAD_REQUEST, "S506", "Invalid delivery status transition"),
    DELIVERY_CARRIER_NOT_AVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "S507", "Delivery carrier not available"),

    // ========================================
    // Coupon Errors (S6XX)
    // ========================================
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "S601", "Coupon not found"),
    COUPON_EXHAUSTED(HttpStatus.CONFLICT, "S602", "Coupon is exhausted"),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "S603", "Coupon has expired"),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "S604", "Coupon already issued to this user"),
    COUPON_NOT_STARTED(HttpStatus.BAD_REQUEST, "S605", "Coupon issuance has not started yet"),
    COUPON_INACTIVE(HttpStatus.BAD_REQUEST, "S606", "Coupon is not active"),
    COUPON_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "S607", "Coupon code already exists"),
    USER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "S608", "User coupon not found"),
    USER_COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "S609", "User coupon has already been used"),
    USER_COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "S610", "User coupon has expired"),

    // ========================================
    // TimeDeal Errors (S7XX)
    // ========================================
    TIMEDEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "S701", "Time deal not found"),
    TIMEDEAL_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "S702", "Time deal is not active"),
    TIMEDEAL_EXPIRED(HttpStatus.BAD_REQUEST, "S703", "Time deal has expired"),
    TIMEDEAL_SOLD_OUT(HttpStatus.CONFLICT, "S704", "Time deal product is sold out"),
    TIMEDEAL_PURCHASE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "S705", "Purchase limit exceeded for this time deal"),
    TIMEDEAL_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S706", "Time deal product not found"),
    TIMEDEAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "S707", "Time deal already exists for this product"),
    TIMEDEAL_INVALID_PERIOD(HttpStatus.BAD_REQUEST, "S708", "Invalid time deal period"),

    // ========================================
    // Queue Errors (S8XX) - Reserved for future use
    // ========================================
    QUEUE_ALREADY_ENTERED(HttpStatus.CONFLICT, "S802", "Already entered in the queue"),
    QUEUE_NOT_FOUND(HttpStatus.NOT_FOUND, "S803", "Queue entry not found"),
    QUEUE_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "S804", "Queue token has expired"),
    QUEUE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "S805", "Not allowed to enter yet"),

    // ========================================
    // Saga/System Errors (S9XX)
    // ========================================
    SAGA_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S901", "Saga execution failed"),
    SAGA_COMPENSATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S902", "Saga compensation failed"),
    SAGA_NOT_FOUND(HttpStatus.NOT_FOUND, "S903", "Saga state not found"),
    SAGA_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "S904", "Saga has already been completed"),
    SAGA_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "S905", "Saga execution timed out"),

    // ========================================
    // Search Errors (S10XX)
    // ========================================
    SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S1001", "Search operation failed"),
    INVALID_SEARCH_QUERY(HttpStatus.BAD_REQUEST, "S1002", "Invalid search query"),
    INDEX_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "S1003", "Search index not found"),
    SUGGEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S1004", "Autocomplete suggestion failed");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ShoppingErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
