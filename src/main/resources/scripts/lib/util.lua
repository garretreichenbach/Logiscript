local util = {}

function util.clamp(value, minValue, maxValue)
    local low = math.min(minValue, maxValue)
    local high = math.max(minValue, maxValue)
    return math.max(low, math.min(high, value))
end

function util.round(value)
    if value >= 0 then
        return math.floor(value + 0.5)
    end
    return math.ceil(value - 0.5)
end

function util.startsWith(value, prefix)
    if value == nil or prefix == nil then
        return false
    end
    return string.sub(value, 1, #prefix) == prefix
end

function util.endsWith(value, suffix)
    if value == nil or suffix == nil then
        return false
    end
    if suffix == "" then
        return true
    end
    return string.sub(value, -#suffix) == suffix
end

function util.split(value, delimiter)
    if value == nil then
        return {}
    end
    if delimiter == nil or delimiter == "" then
        return { value }
    end

    local out = {}
    local start = 1
    while true do
        local first, last = string.find(value, delimiter, start, true)
        if not first then
            table.insert(out, string.sub(value, start))
            break
        end
        table.insert(out, string.sub(value, start, first - 1))
        start = last + 1
    end
    return out
end

function util.join(values, delimiter)
    if values == nil then
        return ""
    end
    return table.concat(values, delimiter or "")
end

function util.padRight(value, width, fill)
    local text = tostring(value or "")
    local targetWidth = math.max(0, width or 0)
    local fillChar = fill or " "
    while #text < targetWidth do
        text = text .. fillChar
    end
    return text
end

function util.padLeft(value, width, fill)
    local text = tostring(value or "")
    local targetWidth = math.max(0, width or 0)
    local fillChar = fill or " "
    while #text < targetWidth do
        text = fillChar .. text
    end
    return text
end

return util
