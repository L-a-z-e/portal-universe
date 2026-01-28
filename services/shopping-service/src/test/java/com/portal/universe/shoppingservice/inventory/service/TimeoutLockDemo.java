package com.portal.universe.shoppingservice.inventory.service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

/**
 * ReentrantLock의 타임아웃 기능 데모
 */
public class TimeoutLockDemo {
    
    public static void main(String[] args) throws InterruptedException {
        Lock lock = new ReentrantLock();
        
        System.out.println("=== 타임아웃 기능 비교 ===\n");
        
        // Thread 1: 락을 오래 점유
        Thread thread1 = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("[Thread-1] 락 획득! 10초간 점유...");
                Thread.sleep(10000);  // 10초 대기
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println("[Thread-1] 락 해제");
                lock.unlock();
            }
        });
        
        // Thread 2: 타임아웃으로 대기
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(500);  // Thread-1이 먼저 시작하도록
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            System.out.println("[Thread-2] 락 획득 시도... (최대 3초 대기)");
            
            try {
                // ✅ 3초만 기다리기!
                if (lock.tryLock(3, TimeUnit.SECONDS)) {
                    try {
                        System.out.println("[Thread-2] 락 획득 성공!");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    // ⏰ 타임아웃!
                    System.out.println("[Thread-2] ⏰ 타임아웃! 3초 동안 락을 못 얻음");
                    System.out.println("[Thread-2] → 다른 작업 수행 또는 에러 처리");
                }
            } catch (InterruptedException e) {
                System.out.println("[Thread-2] 인터럽트 발생");
            }
        });
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        System.out.println("\n=== synchronized였다면? ===");
        System.out.println("Thread-2가 Thread-1이 끝날 때까지 10초 내내 대기!");
        System.out.println("→ 프로그램이 응답 없음 상태");
    }
}
