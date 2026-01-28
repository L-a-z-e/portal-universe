package com.portal.universe.shoppingservice.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 제어를 배우기 위한 테스트
 * 단계별로 직접 작성하면서 배워봅시다!
 */
class MyFirstConcurrencyTest {

    @Test
    @DisplayName("Step 1: 가장 기본적인 테스트 - 더하기가 잘 되는지 확인")
    void basicTest() {
        // given (준비)
        int a = 5;
        int b = 3;
        
        // when (실행)
        int result = a + b;
        
        // then (검증)
        assertThat(result).isEqualTo(8);
    }


    @Test
    @DisplayName("Step 2: Counter가 증가하는지 테스트")
    void counterIncrementTest() {
        // given
        Counter counter = new Counter();

        // when
        counter.increment();
        counter.increment();
        counter.increment();

        // then
        assertThat(counter.getCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Step 3: Counter 10회 증가")
    void counterIncrement10Times() {
        // given
        Counter counter = new Counter();

        // when
        for (int i = 0; i < 10; i++) {
            counter.increment();
        }

        // then
        assertThat(counter.getCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("Step 4: Thread 10번 출력")
    void threadBasic() throws InterruptedException {
        // given
        // Runnable: thread가 실행할 작업
        Runnable task = () -> {
            for (int i =0; i < 10; i++) {
                System.out.println("Thread 실행 중: " + i);
            }
        };

        Thread thread = new Thread(task);
        thread.start();

        thread.join();

        System.out.println("main thread 종료");
    }

    @Test
    @DisplayName("Step 5: 2개 thread 각각 5번씩 증가")
    void twoThreadsIncrement() throws InterruptedException {
        // given
        Counter counter = new Counter();

        // when
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                counter.increment();
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                counter.increment();
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // then
        assertThat(counter.getCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("Step 6: 동시성 문제를 명확히 보기 - 반복 횟수 증가")
    void showRaceConditionClearly() throws InterruptedException {
        // given
        Counter counter = new Counter();
        int iterations = 100000;  // 10만번!
        
        // when
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                counter.increment();
            }
        });
        
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                counter.increment();
            }
        });
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        // then
        int expected = iterations * 2;  // 200,000
        int actual = counter.getCount();
        
        System.out.println("=".repeat(50));
        System.out.println("예상값: " + expected);
        System.out.println("실제값: " + actual);
        System.out.println("손실: " + (expected - actual));
        System.out.println("=".repeat(50));
        
        // 이제 실패할 확률이 높음!
        // assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    @DisplayName("Step 7: 10번 반복 테스트 - 실패가 한 번이라도 나오는지")
    void repeatTest() throws InterruptedException {
        int failures = 0;
        
        for (int test = 0; test < 10; test++) {
            Counter counter = new Counter();
            int iterations = 10000;
            
            Thread thread1 = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    counter.increment();
                }
            });
            
            Thread thread2 = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    counter.increment();
                }
            });
            
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            int expected = iterations * 2;
            int actual = counter.getCount();
            
            if (actual != expected) {
                failures++;
                System.out.println("테스트 #" + (test + 1) + " 실패! " + "예상: " + expected + ", 실제: " + actual);
            }
        }
        
        System.out.println("\n총 10번 중 " + failures + "번 실패");
    }

    @Test
    @DisplayName("Step 8: AtomicInteger로 해결 (10만번 테스트)")
    void atomicIntegerTest() throws InterruptedException {
        // given
        AtomicCounter counter = new AtomicCounter();
        int iterations = 100000;

        // when
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                counter.increment();
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                counter.increment();
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // then
        int expected = iterations * 2;
        int actual = counter.getCount();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Step 9: synchronized로 해결")
    void synchronizedCounterTest() throws InterruptedException {
        // given
        SynchronizedCounter counter = new SynchronizedCounter();
        int iterations = 100000;

        // when
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                counter.increment();
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                counter.increment();
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // then
        int expected = iterations * 2;
        int actual = counter.getCount();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Step 10: ReentrantLock으로 해결")
    void lockedCounterTest() throws InterruptedException {
        // given
        LockedCounter counter = new LockedCounter();
        int iterations = 100000;

        // when
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                counter.increment();
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                counter.increment();
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // then
        int expected = iterations * 2;
        int actual = counter.getCount();

        assertThat(actual).isEqualTo(expected);
    }


    @Test
    @DisplayName("Step 11: 성능 비교 - 3가지 방법")
    void performanceComparison() throws InterruptedException {
        int iterations = 1000000;  // 100만번

        // 1. AtomicInteger
        AtomicCounter atomicCounter = new AtomicCounter();
        long atomicStart = System.currentTimeMillis();

        Thread a1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                atomicCounter.increment();
            }
        });
        Thread a2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                atomicCounter.increment();
            }
        });

        a1.start();
        a2.start();
        a1.join();
        a2.join();

        long atomicTime = System.currentTimeMillis() - atomicStart;

        // 2. synchronized
        SynchronizedCounter syncCounter = new SynchronizedCounter();
        long syncStart = System.currentTimeMillis();

        Thread s1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                syncCounter.increment();
            }
        });
        Thread s2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                syncCounter.increment();
            }
        });

        s1.start();
        s2.start();
        s1.join();
        s2.join();

        long syncTime = System.currentTimeMillis() - syncStart;

        // 3. ReentrantLock
        LockedCounter lockCounter = new LockedCounter();
        long lockStart = System.currentTimeMillis();

        Thread l1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                lockCounter.increment();
            }
        });
        Thread l2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                lockCounter.increment();
            }
        });

        l1.start();
        l2.start();
        l1.join();
        l2.join();

        long lockTime = System.currentTimeMillis() - lockStart;

        // 결과 출력
        System.out.println("\n" + "=".repeat(60));
        System.out.println("성능 비교 (100만번 x 2 스레드)");
        System.out.println("=".repeat(60));
        System.out.println("1. AtomicInteger:   " + atomicTime + "ms (결과: " + atomicCounter.getCount() + ")");
        System.out.println("2. synchronized:    " + syncTime + "ms (결과: " + syncCounter.getCount() + ")");
        System.out.println("3. ReentrantLock:   " + lockTime + "ms (결과: " + lockCounter.getCount() + ")");
        System.out.println("=".repeat(60));

        // 정확성 검증
        assertThat(atomicCounter.getCount()).isEqualTo(iterations * 2);
        assertThat(syncCounter.getCount()).isEqualTo(iterations * 2);
        assertThat(lockCounter.getCount()).isEqualTo(iterations * 2);
    }
}
