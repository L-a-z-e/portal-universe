-- Coupon Rollback Lua Script
-- KEYS[1] = coupon:stock:{couponId} (쿠폰 재고)
-- KEYS[2] = coupon:issued:{couponId} (발급된 사용자 Set)
-- ARGV[1] = userId

-- Return: 롤백 후 남은 재고 수량

local stockKey = KEYS[1]
local issuedKey = KEYS[2]
local userId = ARGV[1]

-- 원자적으로 재고 복원 + 발급 기록 제거
local newStock = redis.call('INCRBY', stockKey, 1)
redis.call('SREM', issuedKey, userId)

return newStock
