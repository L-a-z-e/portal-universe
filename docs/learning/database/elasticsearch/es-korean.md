# Elasticsearch Korean Analysis

Elasticsearch에서 한글 검색을 위한 Nori 분석기 설정과 최적화 방법을 학습합니다.

---

## 목차

1. [한글 분석의 특수성](#1-한글-분석의-특수성)
2. [Nori 분석기](#2-nori-분석기)
3. [Nori Tokenizer](#3-nori-tokenizer)
4. [Nori Token Filters](#4-nori-token-filters)
5. [커스텀 분석기 구성](#5-커스텀-분석기-구성)
6. [실전 설정 예제](#6-실전-설정-예제)

---

## 1. 한글 분석의 특수성

### 한글 vs 영어 분석 차이

```
┌─────────────────────────────────────────────────────────────────────┐
│                  영어 vs 한글 텍스트 분석                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  영어 (공백 기반 분리 가능)                                          │
│  ┌────────────────────────────────────────────┐                     │
│  │  Input: "I love programming"               │                     │
│  │  Tokens: ["i", "love", "programming"]      │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
│  한글 (형태소 분석 필요)                                             │
│  ┌────────────────────────────────────────────┐                     │
│  │  Input: "아버지가방에들어가신다"            │                     │
│  │                                             │                     │
│  │  공백 분리: ["아버지가방에들어가신다"] ❌    │                     │
│  │                                             │                     │
│  │  형태소 분석:                               │                     │
│  │    ["아버지", "가", "방", "에",             │                     │
│  │     "들어가", "시", "ㄴ다"] ✓              │                     │
│  └────────────────────────────────────────────┘                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 한글 형태소 종류

| 형태소 | 설명 | 예시 |
|--------|------|------|
| 명사 (N) | 사물의 이름 | 컴퓨터, 프로그래밍, 개발자 |
| 동사 (V) | 동작, 행위 | 가다, 먹다, 개발하다 |
| 형용사 (VA) | 상태, 성질 | 빠르다, 예쁘다, 새롭다 |
| 부사 (MAG) | 동사/형용사 수식 | 매우, 아주, 빨리 |
| 조사 (J) | 문법적 관계 | 이/가, 을/를, 에서 |
| 어미 (E) | 용언의 끝 | -다, -습니다, -고 |

---

## 2. Nori 분석기

### Nori 플러그인 설치

```bash
# Elasticsearch bin 디렉토리에서 실행
bin/elasticsearch-plugin install analysis-nori

# Docker 환경
FROM docker.elastic.co/elasticsearch/elasticsearch:8.11.0
RUN bin/elasticsearch-plugin install analysis-nori
```

### 기본 Nori 분석기 테스트

```json
POST /_analyze
{
  "analyzer": "nori",
  "text": "동해물과 백두산이 마르고 닳도록"
}
```

**응답:**
```json
{
  "tokens": [
    { "token": "동해", "start_offset": 0, "end_offset": 2, "type": "word" },
    { "token": "물", "start_offset": 2, "end_offset": 3, "type": "word" },
    { "token": "과", "start_offset": 3, "end_offset": 4, "type": "word" },
    { "token": "백두", "start_offset": 5, "end_offset": 7, "type": "word" },
    { "token": "산", "start_offset": 7, "end_offset": 8, "type": "word" },
    { "token": "이", "start_offset": 8, "end_offset": 9, "type": "word" },
    { "token": "마르", "start_offset": 10, "end_offset": 12, "type": "word" },
    { "token": "고", "start_offset": 12, "end_offset": 13, "type": "word" },
    { "token": "닳", "start_offset": 14, "end_offset": 15, "type": "word" },
    { "token": "도록", "start_offset": 15, "end_offset": 17, "type": "word" }
  ]
}
```

### 분석기 비교

```json
// Standard Analyzer (한글 미지원)
POST /_analyze
{
  "analyzer": "standard",
  "text": "삼성전자가 새로운 스마트폰을 출시했습니다"
}
// 결과: ["삼성전자가", "새로운", "스마트폰을", "출시했습니다"]

// Nori Analyzer (한글 형태소 분석)
POST /_analyze
{
  "analyzer": "nori",
  "text": "삼성전자가 새로운 스마트폰을 출시했습니다"
}
// 결과: ["삼성", "전자", "가", "새롭", "ㄴ", "스마트폰", "을", "출시", "하", "았", "습니다"]
```

---

## 3. Nori Tokenizer

### decompound_mode 옵션

복합어 분해 방식을 설정합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                  decompound_mode 비교                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  입력: "삼성전자"                                                    │
│                                                                      │
│  ┌──────────────┬─────────────────────────────────────────────────┐ │
│  │  Mode        │  Output                                         │ │
│  ├──────────────┼─────────────────────────────────────────────────┤ │
│  │  none        │  ["삼성전자"]                                    │ │
│  │              │  복합어 분해 안함                                │ │
│  ├──────────────┼─────────────────────────────────────────────────┤ │
│  │  discard     │  ["삼성", "전자"]                                │ │
│  │  (기본값)     │  분해된 토큰만 출력                              │ │
│  ├──────────────┼─────────────────────────────────────────────────┤ │
│  │  mixed       │  ["삼성전자", "삼성", "전자"]                    │ │
│  │              │  원본 + 분해 토큰 모두 출력                       │ │
│  └──────────────┴─────────────────────────────────────────────────┘ │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

```json
// decompound_mode 테스트
PUT /test_decompound
{
  "settings": {
    "analysis": {
      "tokenizer": {
        "nori_none": {
          "type": "nori_tokenizer",
          "decompound_mode": "none"
        },
        "nori_discard": {
          "type": "nori_tokenizer",
          "decompound_mode": "discard"
        },
        "nori_mixed": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed"
        }
      }
    }
  }
}

// 테스트
POST /test_decompound/_analyze
{
  "tokenizer": "nori_mixed",
  "text": "삼성전자"
}
```

### 사용자 사전 (user_dictionary)

고유명사, 신조어 등을 등록합니다.

```
# config/userdict_ko.txt
삼성전자
네이버웹툰
카카오페이
배달의민족
코로나19
챗GPT
```

```json
PUT /products
{
  "settings": {
    "analysis": {
      "tokenizer": {
        "nori_user_dict": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed",
          "user_dictionary": "userdict_ko.txt"
        }
      },
      "analyzer": {
        "korean_analyzer": {
          "type": "custom",
          "tokenizer": "nori_user_dict"
        }
      }
    }
  }
}
```

### user_dictionary_rules (인라인 정의)

```json
PUT /products
{
  "settings": {
    "analysis": {
      "tokenizer": {
        "nori_tokenizer_custom": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed",
          "user_dictionary_rules": [
            "삼성전자",
            "네이버웹툰",
            "카카오페이",
            "배달의민족"
          ]
        }
      }
    }
  }
}
```

### discard_punctuation 옵션

```json
{
  "tokenizer": {
    "nori_tokenizer": {
      "type": "nori_tokenizer",
      "discard_punctuation": false
    }
  }
}
// false: 문장부호 유지 (기본값 true: 제거)
```

---

## 4. Nori Token Filters

### nori_part_of_speech (품사 필터)

특정 품사를 제거합니다.

```json
PUT /products
{
  "settings": {
    "analysis": {
      "filter": {
        "nori_pos_filter": {
          "type": "nori_part_of_speech",
          "stoptags": [
            "E",    // 어미
            "IC",   // 감탄사
            "J",    // 조사
            "MAG",  // 일반 부사
            "MAJ",  // 접속 부사
            "MM",   // 관형사
            "SP",   // 공백
            "SSC",  // 닫는 괄호
            "SSO",  // 여는 괄호
            "SC",   // 구분자
            "SE",   // 줄임표
            "XPN",  // 접두사
            "XSA",  // 형용사 파생 접미사
            "XSN",  // 명사 파생 접미사
            "XSV",  // 동사 파생 접미사
            "UNA",  // 알 수 없는 것
            "NA",   // 분석 불능
            "VSV"   // 긍정 지정사
          ]
        }
      }
    }
  }
}
```

### 주요 품사 태그 (POS Tags)

| 태그 | 설명 | 예시 | 필터 권장 |
|------|------|------|----------|
| NNG | 일반 명사 | 컴퓨터, 사과 | 유지 |
| NNP | 고유 명사 | 서울, 삼성 | 유지 |
| NNB | 의존 명사 | 것, 수, 줄 | 선택적 |
| VV | 동사 | 가다, 먹다 | 유지 |
| VA | 형용사 | 예쁘다, 크다 | 유지 |
| J | 조사 | 이/가, 을/를 | 제거 권장 |
| E | 어미 | -다, -습니다 | 제거 권장 |
| XS* | 접미사 | -적, -화 | 제거 권장 |

### nori_readingform (한자 → 한글)

한자를 한글 발음으로 변환합니다.

```json
PUT /products
{
  "settings": {
    "analysis": {
      "filter": {
        "nori_readingform_filter": {
          "type": "nori_readingform"
        }
      }
    }
  }
}

// 테스트
POST /products/_analyze
{
  "tokenizer": "nori_tokenizer",
  "filter": ["nori_readingform"],
  "text": "大韓民國"
}
// 결과: ["대한민국"]
```

### nori_number (숫자 처리)

```json
{
  "filter": {
    "nori_number_filter": {
      "type": "nori_number"
    }
  }
}

// "一二三" → "123"
// "壹萬貳仟參佰肆拾伍" → "12345"
```

---

## 5. 커스텀 분석기 구성

### 기본 한글 분석기

```json
PUT /korean_index
{
  "settings": {
    "analysis": {
      "tokenizer": {
        "nori_tokenizer": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed",
          "discard_punctuation": true
        }
      },
      "filter": {
        "nori_pos_filter": {
          "type": "nori_part_of_speech",
          "stoptags": ["E", "J", "SC", "SE", "SF", "SP", "SSC", "SSO", "SY", "NA"]
        }
      },
      "analyzer": {
        "korean_analyzer": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": [
            "nori_pos_filter",
            "nori_readingform",
            "lowercase"
          ]
        }
      }
    }
  }
}
```

### 검색용 vs 인덱싱용 분석기 분리

```json
PUT /products
{
  "settings": {
    "analysis": {
      "tokenizer": {
        "nori_mixed": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed",
          "user_dictionary_rules": ["삼성전자", "네이버"]
        },
        "nori_discard": {
          "type": "nori_tokenizer",
          "decompound_mode": "discard"
        }
      },
      "filter": {
        "pos_filter": {
          "type": "nori_part_of_speech",
          "stoptags": ["E", "J", "VCP", "XSA", "XSN", "XSV"]
        }
      },
      "analyzer": {
        "korean_index_analyzer": {
          "type": "custom",
          "tokenizer": "nori_mixed",
          "filter": ["pos_filter", "nori_readingform", "lowercase"]
        },
        "korean_search_analyzer": {
          "type": "custom",
          "tokenizer": "nori_discard",
          "filter": ["pos_filter", "nori_readingform", "lowercase"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "analyzer": "korean_index_analyzer",
        "search_analyzer": "korean_search_analyzer"
      }
    }
  }
}
```

### 분석기 분리 효과

```
┌─────────────────────────────────────────────────────────────────────┐
│              Index Analyzer vs Search Analyzer                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  문서: "삼성전자 스마트폰"                                           │
│                                                                      │
│  Index Analyzer (mixed mode):                                        │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  인덱싱 토큰: ["삼성전자", "삼성", "전자", "스마트폰"]          │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  검색어: "삼성"                                                      │
│                                                                      │
│  Search Analyzer (discard mode):                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  검색 토큰: ["삼성"]                                           │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  결과: "삼성" 토큰이 인덱스의 "삼성" 토큰과 매칭 ✓                   │
│                                                                      │
│  장점:                                                               │
│  - "삼성" 검색 시 "삼성전자" 문서 검색됨                             │
│  - "삼성전자" 검색 시에도 "삼성전자" 문서 검색됨                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 6. 실전 설정 예제

### E-commerce 상품 검색

```json
PUT /products
{
  "settings": {
    "index": {
      "number_of_shards": 3,
      "number_of_replicas": 1
    },
    "analysis": {
      "char_filter": {
        "special_char_filter": {
          "type": "mapping",
          "mappings": [
            "& => and",
            "+ => plus"
          ]
        }
      },
      "tokenizer": {
        "nori_mixed": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed",
          "user_dictionary_rules": [
            "삼성전자", "LG전자", "네이버",
            "아이폰", "갤럭시", "맥북",
            "에어팟", "버즈", "워치"
          ]
        }
      },
      "filter": {
        "pos_filter": {
          "type": "nori_part_of_speech",
          "stoptags": [
            "E", "IC", "J", "MAG", "MAJ",
            "MM", "SP", "SSC", "SSO", "SC",
            "SE", "XPN", "XSA", "XSN", "XSV",
            "UNA", "NA"
          ]
        },
        "synonym_filter": {
          "type": "synonym",
          "synonyms": [
            "스마트폰, 핸드폰, 휴대폰",
            "노트북, 랩탑",
            "이어폰, 이어버드",
            "tv, 티비, 텔레비전"
          ]
        }
      },
      "analyzer": {
        "korean_index": {
          "type": "custom",
          "char_filter": ["special_char_filter"],
          "tokenizer": "nori_mixed",
          "filter": ["pos_filter", "nori_readingform", "lowercase"]
        },
        "korean_search": {
          "type": "custom",
          "char_filter": ["special_char_filter"],
          "tokenizer": "nori_mixed",
          "filter": ["pos_filter", "synonym_filter", "nori_readingform", "lowercase"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "analyzer": "korean_index",
        "search_analyzer": "korean_search",
        "fields": {
          "keyword": {
            "type": "keyword"
          },
          "ngram": {
            "type": "text",
            "analyzer": "ngram_analyzer"
          }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "korean_index",
        "search_analyzer": "korean_search"
      },
      "brand": {
        "type": "keyword"
      },
      "category": {
        "type": "keyword"
      },
      "price": {
        "type": "integer"
      }
    }
  }
}
```

### 자동완성 (Autocomplete)

```json
PUT /products_autocomplete
{
  "settings": {
    "analysis": {
      "tokenizer": {
        "edge_ngram_tokenizer": {
          "type": "edge_ngram",
          "min_gram": 1,
          "max_gram": 20,
          "token_chars": ["letter", "digit"]
        }
      },
      "analyzer": {
        "autocomplete_index": {
          "type": "custom",
          "tokenizer": "edge_ngram_tokenizer",
          "filter": ["lowercase"]
        },
        "autocomplete_search": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase"]
        },
        "korean_autocomplete": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["lowercase", "edge_ngram_filter"]
        }
      },
      "filter": {
        "edge_ngram_filter": {
          "type": "edge_ngram",
          "min_gram": 1,
          "max_gram": 20
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "suggest": {
        "type": "text",
        "analyzer": "autocomplete_index",
        "search_analyzer": "autocomplete_search"
      },
      "suggest_korean": {
        "type": "text",
        "analyzer": "korean_autocomplete",
        "search_analyzer": "standard"
      },
      "name": {
        "type": "keyword"
      }
    }
  }
}

// 자동완성 검색
GET /products_autocomplete/_search
{
  "query": {
    "multi_match": {
      "query": "갤럭",
      "fields": ["suggest", "suggest_korean"]
    }
  }
}
```

### 블로그 검색

```json
PUT /blog_posts
{
  "settings": {
    "analysis": {
      "tokenizer": {
        "nori_tokenizer": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed"
        }
      },
      "filter": {
        "pos_filter": {
          "type": "nori_part_of_speech",
          "stoptags": ["E", "J", "SC", "SP"]
        },
        "korean_stop": {
          "type": "stop",
          "stopwords": [
            "의", "가", "이", "은", "들", "는",
            "좀", "잘", "걍", "과", "도", "를",
            "으로", "자", "에", "와", "한", "하다"
          ]
        }
      },
      "analyzer": {
        "korean_content": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["pos_filter", "korean_stop", "lowercase"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "korean_content",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "content": {
        "type": "text",
        "analyzer": "korean_content"
      },
      "tags": {
        "type": "keyword"
      },
      "author": {
        "type": "keyword"
      },
      "created_at": {
        "type": "date"
      }
    }
  }
}
```

### 분석기 테스트

```json
// 분석 결과 확인
POST /products/_analyze
{
  "analyzer": "korean_index",
  "text": "삼성전자 갤럭시 S24 스마트폰 출시"
}

// 필드별 분석 테스트
POST /products/_analyze
{
  "field": "name",
  "text": "아이폰 15 프로맥스 케이스"
}

// 토큰 상세 정보
POST /products/_analyze
{
  "analyzer": "korean_index",
  "text": "동해물과 백두산이",
  "explain": true
}
```

---

## 핵심 요약

```
┌─────────────────────────────────────────────────────────────────────┐
│               Korean Analysis Quick Reference                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Nori 설치                                                          │
│  └── bin/elasticsearch-plugin install analysis-nori                 │
│                                                                      │
│  Nori Tokenizer 옵션                                                │
│  ├── decompound_mode: none | discard | mixed                        │
│  ├── user_dictionary: 사용자 사전 파일                               │
│  └── user_dictionary_rules: 인라인 사전                              │
│                                                                      │
│  Nori Token Filters                                                 │
│  ├── nori_part_of_speech: 품사 필터링                               │
│  ├── nori_readingform: 한자 → 한글 변환                             │
│  └── nori_number: 한자 숫자 → 아라비아 숫자                          │
│                                                                      │
│  권장 설정                                                           │
│  ├── Index: mixed mode (복합어 + 분해어 모두 저장)                   │
│  ├── Search: discard mode (검색어만 분석)                            │
│  └── 조사/어미 필터링으로 노이즈 제거                                 │
│                                                                      │
│  자주 제거하는 품사 (stoptags)                                       │
│  ├── E: 어미                                                         │
│  ├── J: 조사                                                         │
│  ├── SP, SC, SE: 공백, 구분자, 줄임표                               │
│  └── XS*: 접미사                                                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 다음 단계

- [Query DSL](./es-query-dsl.md) - 한글 검색 쿼리 작성
- [Spring Integration](./es-spring-integration.md) - Spring Boot 연동
- [Optimization](./es-optimization.md) - 성능 최적화
