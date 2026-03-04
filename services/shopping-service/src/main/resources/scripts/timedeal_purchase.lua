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

-- Phase 1: 사용자별 구매 제한 검증 (Early Return)
local currentPurchased = tonumber(redis.call('GET', purchasedKey) or 0)
if currentPurchased + requestedQuantity > maxPerUser then
    return -1
end

-- Phase 2: 재고 확인 (Early Return - 재고 소진 후 Write 부하 방지)
local currentStock = tonumber(redis.call('GET', stockKey) or 0)
if currentStock < requestedQuantity then
    return 0
end

-- Phase 3: 원자적 재고 차감
local newStock = redis.call('DECRBY', stockKey, requestedQuantity)

-- Phase 4: 사용자 구매 수량 기록
redis.call('INCRBY', purchasedKey, requestedQuantity)

return newStock
