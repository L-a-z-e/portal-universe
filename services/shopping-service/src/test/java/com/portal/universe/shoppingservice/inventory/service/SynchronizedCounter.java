package com.portal.universe.shoppingservice.inventory.service;

/**
 * synchronized를 사용한 Thread-Safe Counter
 */
public class SynchronizedCounter {
    private int count = 0;
    private final Object lock = new Object();  // 락으로 사용할 객체
    
    public void increment() {
        synchronized (lock) {  // 이 블록은 한 번에 한 스레드만!
            count++;
        }
    }
    
    public int getCount() {
        synchronized (lock) {
            return count;
        }
    }
    
    public void reset() {
        synchronized (lock) {
            count = 0;
        }
    }
}
