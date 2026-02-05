# Elasticsearch 상품 인덱스 설계

## 개요

Shopping Service는 Elasticsearch를 활용하여 상품 검색 기능을 제공합니다. 이 문서에서는 상품 인덱스의 설계 원칙과 매핑 구조를 설명합니다.

## 인덱스 기본 정보

| 속성 | 값 |
|------|-----|
| 인덱스명 | `products` |
| 샤드 수 | 1 (개발) / 3 (운영 권장) |
| 레플리카 수 | 0 (개발) / 2 (운영 권장) |

## 인덱스 매핑

### products-mapping.json

```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "korean": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["lowercase", "nori_readingform"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "name": {
        "type": "text",
        "analyzer": "korean",
        "fields": {
          "keyword": { "type": "keyword" },
          "suggest": {
            "type": "completion",
            "analyzer": "korean"
          }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "korean"
      },
      "price": { "type": "double" },
      "stock": { "type": "integer" }
    }
  }
}
```

## 필드 설계

### 기본 필드

| 필드명 | 타입 | 용도 | 인덱싱 |
|--------|------|------|--------|
| `id` | long | 상품 ID (MySQL FK) | O |
| `name` | text | 상품명 검색 | O |
| `description` | text | 상품 설명 검색 | O |
| `price` | double | 가격 필터/정렬 | O |
| `stock` | integer | 재고 수량 필터 | O |

### name 필드 Multi-field

상품명 필드는 여러 용도로 사용되므로 multi-field로 설계합니다:

```json
"name": {
  "type": "text",
  "analyzer": "korean",
  "fields": {
    "keyword": {
      "type": "keyword"
    },
    "suggest": {
      "type": "completion",
      "analyzer": "korean"
    }
  }
}
```

| 서브 필드 | 타입 | 용도 |
|----------|------|------|
| `name` | text | 전문 검색 (형태소 분석) |
| `name.keyword` | keyword | 정확한 매칭, 정렬, 집계 |
| `name.suggest` | completion | 자동완성 기능 |

## Analyzer 설계

### Korean Analyzer

한국어 형태소 분석을 위해 Nori 플러그인을 사용합니다:

```json
"analysis": {
  "analyzer": {
    "korean": {
      "type": "custom",
      "tokenizer": "nori_tokenizer",
      "filter": ["lowercase", "nori_readingform"]
    }
  }
}
```

### Tokenizer 동작 예시

입력: "무선 블루투스 이어폰"

```
nori_tokenizer 결과:
- 무선
- 블루투스
- 이어폰
```

### Filter 설명

| Filter | 설명 |
|--------|------|
| `lowercase` | 소문자 변환 |
| `nori_readingform` | 한자를 한글로 변환 |

## 확장 인덱스 설계 (권장)

실제 운영 환경에서는 더 많은 필드가 필요합니다:

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "name": {
        "type": "text",
        "analyzer": "korean",
        "fields": {
          "keyword": { "type": "keyword" },
          "suggest": { "type": "completion" }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "korean"
      },
      "price": { "type": "double" },
      "discountPrice": { "type": "double" },
      "discountRate": { "type": "float" },
      "stock": { "type": "integer" },
      "category": {
        "type": "nested",
        "properties": {
          "id": { "type": "long" },
          "name": { "type": "keyword" },
          "depth": { "type": "integer" }
        }
      },
      "brand": {
        "type": "object",
        "properties": {
          "id": { "type": "long" },
          "name": { "type": "keyword" }
        }
      },
      "tags": { "type": "keyword" },
      "attributes": {
        "type": "nested",
        "properties": {
          "name": { "type": "keyword" },
          "value": { "type": "keyword" }
        }
      },
      "rating": { "type": "float" },
      "reviewCount": { "type": "integer" },
      "salesCount": { "type": "long" },
      "isActive": { "type": "boolean" },
      "createdAt": { "type": "date" },
      "updatedAt": { "type": "date" }
    }
  }
}
```

### 카테고리 필드 (Nested)

```json
"category": {
  "type": "nested",
  "properties": {
    "id": { "type": "long" },
    "name": { "type": "keyword" },
    "depth": { "type": "integer" }
  }
}
```

Nested 타입을 사용하는 이유:
- 카테고리 계층 구조를 정확히 표현
- 다중 카테고리 검색 시 정확한 필터링

### 속성 필드 (Faceted Search용)

```json
"attributes": {
  "type": "nested",
  "properties": {
    "name": { "type": "keyword" },
    "value": { "type": "keyword" }
  }
}
```

예시 데이터:
```json
{
  "attributes": [
    { "name": "color", "value": "black" },
    { "name": "size", "value": "M" },
    { "name": "material", "value": "cotton" }
  ]
}
```

## 인덱스 설정 최적화

### Refresh Interval

```json
{
  "settings": {
    "refresh_interval": "30s"
  }
}
```

- 검색 성능과 인덱싱 성능의 균형
- 실시간 검색이 필요없으면 늘려도 됨

### 복제본 설정

| 환경 | 샤드 수 | 레플리카 수 |
|------|--------|------------|
| 개발 | 1 | 0 |
| 스테이징 | 2 | 1 |
| 운영 | 3 | 2 |

## 인덱스 초기화 서비스

### IndexInitializationService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexInitializationService {

    private static final String INDEX_NAME = "products";
    private static final String MAPPING_FILE = "elasticsearch/products-mapping.json";

    private final ElasticsearchClient esClient;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void initializeIndices() {
        try {
            if (!indexExists(INDEX_NAME)) {
                createIndex(INDEX_NAME, MAPPING_FILE);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Elasticsearch indices", e);
        }
    }

    private boolean indexExists(String indexName) throws IOException {
        return esClient.indices()
            .exists(ExistsRequest.of(e -> e.index(indexName)))
            .value();
    }

    private void createIndex(String indexName, String mappingFile) throws IOException {
        ClassPathResource resource = new ClassPathResource(mappingFile);

        try (InputStream is = resource.getInputStream()) {
            JsonNode mapping = objectMapper.readTree(is);

            esClient.indices().create(CreateIndexRequest.of(c -> c
                .index(indexName)
                .withJson(new StringReader(mapping.toString()))
            ));

            log.info("Created Elasticsearch index: {}", indexName);
        }
    }
}
```

## 인덱스 관리

### 인덱스 재생성

스키마 변경이 필요한 경우:

```bash
# 1. 새 인덱스 생성
PUT /products_v2
{
  "settings": {...},
  "mappings": {...}
}

# 2. 데이터 복사
POST /_reindex
{
  "source": { "index": "products" },
  "dest": { "index": "products_v2" }
}

# 3. Alias 전환
POST /_aliases
{
  "actions": [
    { "remove": { "index": "products", "alias": "products_alias" }},
    { "add": { "index": "products_v2", "alias": "products_alias" }}
  ]
}
```

### Alias 사용 권장

```java
// 인덱스 직접 참조 대신 Alias 사용
private static final String INDEX_ALIAS = "products_alias";

SearchRequest request = SearchRequest.of(s -> s
    .index(INDEX_ALIAS)  // Alias 사용
    .query(...)
);
```

## Best Practices

### 1. 필드 타입 선택

| 용도 | 권장 타입 |
|------|----------|
| 전문 검색 | text |
| 정확한 매칭/필터 | keyword |
| 숫자 범위 검색 | long, integer, double |
| 날짜 범위 검색 | date |
| 자동완성 | completion |
| 복잡한 객체 | nested |

### 2. 매핑 변경 불가 필드

한번 생성된 필드의 타입은 변경 불가:
- text → keyword (X)
- integer → long (X)

해결책: 인덱스 재생성

### 3. Dynamic Mapping 비활성화

```json
{
  "mappings": {
    "dynamic": "strict",
    "properties": {...}
  }
}
```

예상치 못한 필드가 추가되는 것을 방지

## 관련 문서

- [Product Sync](./product-sync.md) - MySQL-ES 데이터 동기화
- [Search Service](./search-service.md) - 검색 서비스 로직
- [Autocomplete](./autocomplete.md) - 자동완성 구현
