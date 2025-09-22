package com.portal.universe.shoppingservice.dto;

public record ProductCreateRequest(String name, String description, Double price, Integer stock) {
}
