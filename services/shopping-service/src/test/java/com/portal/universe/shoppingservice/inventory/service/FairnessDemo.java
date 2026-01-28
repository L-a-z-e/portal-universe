package com.portal.universe.shoppingservice.inventory.service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock의 공정성(Fairness) 기능 데모
 */
public class FairnessDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 1. 불공정 락 (기본값) ===");
        testLock(new ReentrantLock(false));  // false = 불공정
        
        Thread.sleep(1000);
        System.out.println("\n");
        
        System.out.println("=== 2. 공정 락 ===");
        testLock(new ReentrantLock(true));   // true = 공정
    }
    
    private static void testLock(Lock lock) throws InterruptedException {
        // 5개 스레드 생성
        for (int i = 1; i <= 5; i++) {
            final int threadNum = i;
            new Thread(() -> {
                System.out.println("[Thread-" + threadNum + "] 대기 시작");
                
                lock.lock();
                try {
                    System.out.println("[Thread-" + threadNum + "] ✅ 락 획득!");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }).start();
            
            Thread.sleep(50);  // 순서대로 시작하도록
        }
        
        Thread.sleep(2000);  // 모든 스레드 종료 대기
    }
}
