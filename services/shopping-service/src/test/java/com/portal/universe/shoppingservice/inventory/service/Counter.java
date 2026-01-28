package com.portal.universe.shoppingservice.inventory.service;

/**
 * 카운터 클래스 - 동시성 테스트용
 */
public class Counter {
    private int count = 0;
    
    public void increment() {
        count++;
    }
    
    public int getCount() {
        return count;
    }
    
    public void reset() {
        count = 0;
    }
}
