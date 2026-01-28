package com.portal.universe.shoppingservice.inventory.service;

/**
 * synchronized 동작 원리 이해를 위한 상세 설명
 */
public class SynchronizedExplained {
    
    public static void main(String[] args) {
        System.out.println("=== synchronized의 비밀 ===\n");
        
        Object lock = new Object();
        
        System.out.println("1. new Object() 생성:");
        System.out.println("   - 우리가 보는 것: 빈 객체");
        System.out.println("   - JVM이 추가하는 것:");
        System.out.println("     * Mark Word (64비트): 락 상태 저장");
        System.out.println("     * Class Pointer: Object.class 참조");
        System.out.println("     * Wait Queue: 대기 중인 스레드들\n");
        
        System.out.println("2. Mark Word 내부 (64비트 구조):");
        System.out.println("   ┌─────────────────────────────────┐");
        System.out.println("   │ 25비트: hash code               │");
        System.out.println("   │  4비트: age (GC 관련)           │");
        System.out.println("   │  1비트: biased lock 여부        │");
        System.out.println("   │  2비트: lock state ⭐          │");
        System.out.println("   │        00 = Lightweight locked  │");
        System.out.println("   │        01 = Unlocked            │");
        System.out.println("   │        10 = Heavyweight locked  │");
        System.out.println("   │        11 = GC marked           │");
        System.out.println("   └─────────────────────────────────┘\n");
        
        System.out.println("3. synchronized (lock) 실행 시:");
        System.out.println("   Step 1: JVM이 monitorenter 명령어 실행");
        System.out.println("   Step 2: Mark Word의 lock state 확인");
        System.out.println("   Step 3-A: Unlocked이면 → Locked로 변경, owner 설정");
        System.out.println("   Step 3-B: Locked이면 → Wait Queue에 추가, 스레드 정지\n");
        
        System.out.println("4. 블록 끝나면:");
        System.out.println("   Step 1: JVM이 monitorexit 명령어 실행");
        System.out.println("   Step 2: Mark Word를 Unlocked로 변경");
        System.out.println("   Step 3: Wait Queue에서 스레드 하나 깨움\n");
        
        System.out.println("5. 핵심 요약:");
        System.out.println("   - new Object()만 해도 락 메커니즘이 자동으로 붙음!");
        System.out.println("   - synchronized는 객체의 Mark Word를 활용");
        System.out.println("   - true/false 같은 필드 없어도 Mark Word에 상태 저장");
        System.out.println("   - JVM이 모든 걸 자동으로 처리");
    }
}
