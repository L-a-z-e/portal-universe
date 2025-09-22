package com.portal.universe.shoppingservice.dto;

public record ProductUpdateRequest(String name, String description, Double price, Integer stock) {
}
