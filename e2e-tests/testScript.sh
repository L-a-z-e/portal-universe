#!/bin/bash

# 토큰 설정
ADMIN_TOKEN="eyJraWQiOiI0YmI3M2E3MC1hNjI3LTRkZWMtODkxOS1hYmMyYmFiOWVkNTciLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiYXVkIjoicG9ydGFsLWNsaWVudCIsIm5iZiI6MTc2MDM3NzE3MCwic2NvcGUiOlsicmVhZCIsIm9wZW5pZCIsInByb2ZpbGUiLCJ3cml0ZSJdLCJyb2xlcyI6WyJST0xFX0FETUlOIl0sImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9hdXRoLXNlcnZpY2UiLCJleHAiOjE3NjAzNzc0NzAsImlhdCI6MTc2MDM3NzE3MCwianRpIjoiOTY2MzJhOGItM2VhOC00Nzg0LTk1YzItNDE0OTQ0OTUyNWIwIn0.flzX9DqeggyTtbuzwJMUfSpNq5140CSpCSLHr5aNYVaFtmBKVENnvcxZkHKLOj7ec0CAZzOw3XsZa3hw6GPFDpj8XaeeoclzaWl6cvXr1M-eLqmwH0vkvXIUmgP-YXSF5SVlUp3KQ2rX82Sx-lUHL9S50UiGeIHt5H4FxJLqPVGyA083fWQkIScil-JuDboh9i1tTnQkDdT_dVyHLnp7aGJqJ1COzn6XbE2IvO0NejNX1S4r19_VcUcjRYU-c66gkTLCpKFtT_0kFNfZX_QuOyWOnS_wck8uNycNX8kyVEZDl9mECcNlefiZZUAAdVbTva03tlzj1eRgCquSR1M0Mw"
USER_TOKEN="eyJraWQiOiI0YmI3M2E3MC1hNjI3LTRkZWMtODkxOS1hYmMyYmFiOWVkNTciLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJmaW5hbHRlc3RAZXhhbXBsZS5jb20iLCJhdWQiOiJwb3J0YWwtY2xpZW50IiwibmJmIjoxNzYwMzc3MTk1LCJzY29wZSI6WyJyZWFkIiwib3BlbmlkIiwicHJvZmlsZSIsIndyaXRlIl0sInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC1zZXJ2aWNlIiwiZXhwIjoxNzYwMzc3NDk1LCJpYXQiOjE3NjAzNzcxOTUsImp0aSI6ImZkMGViOTVlLTNjNjktNDVhZS1iNjc3LWY4MzFmNjRiMDU4YyJ9.trEnlUScEvhKnVhgoXElblpurVafah_WR0gmh-i9U6iRN7g6MzlXIBMUvXU4we0xC3PC3HF1TG0Hp5WCpZ2XRnWRVt-N5ZI5jJqSIwsvEYSQmb4HuKSJQDIAytTBqBduXO3nadDL6dkowza8Oul6wvLMy94PeEunLmX1ELZtU57mmV-CrF9UK9BNP3JdOBCmeRjonfyJ3xb_ngA_r7jJcVDnU9aZ1wDyyTl5W_RdA9Fn-35q6HlmouUW0YeNK3F_AZuL5uN1x34Zb-PCxQPCa1jx-ahVBgEU60BjqUQ_kw1hWmIroqA5unhAagpusyW9hbrsz2YXYINTVgzul2yWaA"

echo "\n1. 공개 API - GET /api/blog (200 예상)"
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8080/api/blog

echo "\n2. 공개 API - GET /api/blog/123 (200 또는 404 예상)"
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8080/api/blog/123

echo "\n3. ADMIN API - POST (JWT 없음) (401 예상)"
curl -s -o /dev/null -w "HTTP %{http_code}\n" \
  -X POST http://localhost:8080/api/blog \
  -H "Content-Type: application/json" \
  -d '{"title":"test","content":"test"}'

echo "\n4. ADMIN API - POST (USER 토큰) (403 예상)"
curl -s -o /dev/null -w "HTTP %{http_code}\n" \
  -X POST http://localhost:8080/api/blog \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"test","content":"test"}'

echo "\n5. ADMIN API - POST (ADMIN 토큰) (200 예상)"
curl -s -o /dev/null -w "HTTP %{http_code}\n" \
  -X POST http://localhost:8080/api/blog \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"test","content":"test","categoryId":"tech"}'