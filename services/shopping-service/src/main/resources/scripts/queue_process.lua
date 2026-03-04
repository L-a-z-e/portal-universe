-- Queue Process Lua Script
-- 원자적으로 CHECK(입장 인원) + POP(대기열) + ADD(입장 목록) 수행
-- TOCTOU 방지: 세 연산 사이에 다른 스케줄러가 개입할 수 없음
--
-- KEYS[1] = queue:entered:{eventType}:{eventId} (입장 완료 Set)
-- KEYS[2] = queue:waiting:{eventType}:{eventId} (대기열 Sorted Set)
-- ARGV[1] = maxCapacity (최대 입장 인원)
-- ARGV[2] = batchSize (한 번에 처리할 최대 인원)
--
-- Return: 입장 처리된 entryToken 목록 (빈 목록이면 처리 없음)

local enteredKey = KEYS[1]
local waitingKey = KEYS[2]
local maxCapacity = tonumber(ARGV[1])
local batchSize = tonumber(ARGV[2])

-- CHECK: 현재 입장 인원 확인
local enteredCount = redis.call('SCARD', enteredKey)
local availableSlots = maxCapacity - enteredCount

if availableSlots <= 0 then
    return {}
end

local toProcess = math.min(availableSlots, batchSize)

-- 대기열이 비어있으면 종료
if redis.call('ZCARD', waitingKey) == 0 then
    return {}
end

-- POP: 대기열 상위 N명 꺼내기 (FIFO, score=timestamp)
local popped = redis.call('ZPOPMIN', waitingKey, toProcess)

-- ADD: 꺼낸 토큰들을 입장 목록에 추가
local tokens = {}
for i = 1, #popped, 2 do
    local token = popped[i]
    redis.call('SADD', enteredKey, token)
    table.insert(tokens, token)
end

return tokens
