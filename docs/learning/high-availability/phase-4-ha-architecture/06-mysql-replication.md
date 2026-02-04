# MySQL 복제 설정

MySQL Primary-Replica 복제를 구성합니다.

---

## 학습 목표

- [ ] MySQL 복제의 개념을 이해한다
- [ ] Primary-Replica 구조를 구성할 수 있다
- [ ] 읽기 분산을 구현할 수 있다

---

## 1. MySQL 복제란?

### 구조

```
┌─────────────┐     복제      ┌─────────────┐
│   Primary   │ ───────────→  │   Replica   │
│ (Read/Write)│     (async)   │ (Read Only) │
└─────────────┘               └─────────────┘
       ↑                            ↑
    쓰기 요청                    읽기 요청
```

### 복제 방식

| 방식 | 설명 | 일관성 |
|------|------|--------|
| 비동기 | 기본, 약간의 지연 | 약함 |
| 반동기 | 최소 1개 Replica 확인 | 중간 |
| 동기 (Group Replication) | 모든 노드 확인 | 강함 |

---

## 2. 간소화된 구성 (개념 설명)

### Primary 설정

```yaml
# my.cnf
[mysqld]
server-id = 1
log_bin = mysql-bin
binlog_format = ROW
gtid_mode = ON
enforce_gtid_consistency = ON
```

### Replica 설정

```yaml
# my.cnf
[mysqld]
server-id = 2
relay_log = relay-bin
read_only = ON
```

---

## 3. Spring Boot 읽기/쓰기 분리

### DataSource 설정

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public DataSource routingDataSource() {
        ReplicationRoutingDataSource routingDataSource = new ReplicationRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("primary", primaryDataSource());
        targetDataSources.put("replica", replicaDataSource());
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource());
        return routingDataSource;
    }
}
```

### Transactional ReadOnly 활용

```java
@Transactional(readOnly = true)  // Replica로 라우팅
public List<Product> findAll() {
    return productRepository.findAll();
}

@Transactional  // Primary로 라우팅
public Product save(Product product) {
    return productRepository.save(product);
}
```

---

## 4. 효과

| 항목 | 단일 MySQL | Primary-Replica |
|------|-----------|-----------------|
| 쓰기 가용성 | 단일 장애점 | (수동 Failover) |
| 읽기 확장성 | 제한적 | 수평 확장 |
| 백업 | 서비스 영향 | Replica에서 수행 |

---

## 다음 단계

[07-graceful-shutdown.md](./07-graceful-shutdown.md) - Graceful Shutdown 구현
