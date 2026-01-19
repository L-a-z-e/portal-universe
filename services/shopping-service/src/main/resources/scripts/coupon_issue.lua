-- Coupon Issue Lua Script
-- KEYS[1] = coupon:stock:{couponId} (쿠폰 재고)
-- KEYS[2] = coupon:issued:{couponId} (발급된 사용자 Set)
-- ARGV[1] = userId
-- ARGV[2] = 최대 발급 수량

-- Return values:
-- 1: 발급 성공
-- 0: 재고 소진
-- -1: 이미 발급됨

local stockKey = KEYS[1]
local issuedKey = KEYS[2]
local userId = ARGV[1]
local maxQuantity = tonumber(ARGV[2])

-- 이미 발급받았는지 확인
if redis.call('SISMEMBER', issuedKey, userId) == 1 then
    return -1
end

-- 현재 재고 확인
local currentStock = tonumber(redis.call('GET', stockKey) or 0)
if currentStock <= 0 then
    return 0
end

-- 원자적으로 재고 감소
local newStock = redis.call('DECR', stockKey)
if newStock < 0 then
    -- 롤백: 재고가 음수가 되면 다시 증가
    redis.call('INCR', stockKey)
    return 0
end

-- 발급 사용자 기록
redis.call('SADD', issuedKey, userId)

return 1
