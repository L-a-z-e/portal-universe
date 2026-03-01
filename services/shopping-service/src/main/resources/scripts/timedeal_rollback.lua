-- TimeDeal Rollback Lua Script
-- KEYS[1] = timedeal:stock:{dealId}:{productId} (타임딜 재고)
-- KEYS[2] = timedeal:purchased:{dealId}:{productId}:{userId} (사용자별 구매 수량)
-- ARGV[1] = rollback quantity

-- Return: 롤백 후 남은 재고 수량

local stockKey = KEYS[1]
local purchasedKey = KEYS[2]
local quantity = tonumber(ARGV[1])

-- 원자적으로 재고 복원 + 구매 수량 차감
local newStock = redis.call('INCRBY', stockKey, quantity)
local newPurchased = redis.call('DECRBY', purchasedKey, quantity)

-- 구매 수량이 0 이하면 키 삭제 (stale 키 방지)
if newPurchased <= 0 then
    redis.call('DEL', purchasedKey)
end

return newStock
