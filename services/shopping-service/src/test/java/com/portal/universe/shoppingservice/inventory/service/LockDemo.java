package com.portal.universe.shoppingservice.inventory.service;

/**
 * synchronized의 내부 동작을 이해하기 위한 데모
 */
public class LockDemo {
    
    public static void main(String[] args) throws InterruptedException {
        Object lock = new Object();
        
        System.out.println("=== 락 동작 시뮬레이션 ===\n");
        
        // Thread 1
        Thread thread1 = new Thread(() -> {
            System.out.println("[Thread-1] synchronized 진입 시도...");
            
            synchronized (lock) {
                System.out.println("[Thread-1] ✅ 락 획득! (Mark Word: LOCKED, owner=Thread-1)");
                System.out.println("[Thread-1] 작업 중...");
                
                try {
                    Thread.sleep(2000);  // 2초 대기
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                System.out.println("[Thread-1] 작업 완료");
            }
            System.out.println("[Thread-1] 🔓 락 해제 (Mark Word: UNLOCKED)");
        });
        
        // Thread 2
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(500);  // Thread-1이 먼저 시작하도록
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            System.out.println("[Thread-2] synchronized 진입 시도...");
            System.out.println("[Thread-2] ⏳ Mark Word가 LOCKED 상태! 대기 큐에 추가됨");
            
            synchronized (lock) {
                System.out.println("[Thread-2] ✅ 락 획득! (Mark Word: LOCKED, owner=Thread-2)");
                System.out.println("[Thread-2] 작업 중...");
                System.out.println("[Thread-2] 작업 완료");
            }
            System.out.println("[Thread-2] 🔓 락 해제 (Mark Word: UNLOCKED)");
        });
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        System.out.println("\n=== 모든 작업 완료 ===");
    }
}