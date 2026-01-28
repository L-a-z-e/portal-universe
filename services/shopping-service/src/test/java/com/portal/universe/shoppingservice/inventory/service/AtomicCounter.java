package com.portal.universe.shoppingservice.inventory.service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * AtomicInteger를 사용한 Thread-Safe Counter
 */
public class AtomicCounter {
    private AtomicInteger count = new AtomicInteger(0);
    
    public void increment() {
        count.incrementAndGet();  // 원자적 증가
    }
    
    public int getCount() {
        return count.get();
    }
    
    public void reset() {
        count.set(0);
    }
}
