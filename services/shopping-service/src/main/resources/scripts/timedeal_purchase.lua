-- TimeDeal Purchase Lua Script
-- KEYS[1] = timedeal:stock:{dealId}:{productId} (타임딜 재고)
-- KEYS[2] = timedeal:purchased:{dealId}:{productId}:{userId} (사용자별 구매 수량)
-- ARGV[1] = 구매 요청 수량
-- ARGV[2] = 1인당 최대 구매 수량

-- Return values:
-- > 0: 구매 성공 (남은 재고 수량)
-- 0: 재고 소진
-- -1: 1인당 구매 제한 초과

local stockKey = KEYS[1]
local purchasedKey = KEYS[2]
local requestedQuantity = tonumber(ARGV[1])
local maxPerUser = tonumber(ARGV[2])

-- 사용자의 현재 구매 수량 확인
local currentPurchased = tonumber(redis.call('GET', purchasedKey) or 0)
if currentPurchased + requestedQuantity > maxPerUser then
    return -1
end

-- 현재 재고 확인
local currentStock = tonumber(redis.call('GET', stockKey) or 0)
if currentStock < requestedQuantity then
    return 0
end

-- 원자적으로 재고 감소
local newStock = redis.call('DECRBY', stockKey, requestedQuantity)
if newStock < 0 then
    -- 롤백: 재고가 음수가 되면 다시 증가
    redis.call('INCRBY', stockKey, requestedQuantity)
    return 0
end

-- 사용자 구매 수량 증가
redis.call('INCRBY', purchasedKey, requestedQuantity)

-- 남은 재고 반환
return newStock
