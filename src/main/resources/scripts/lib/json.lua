local json = {}

json.null = {}

local function isArray(tbl)
    local maxIndex = 0
    local count = 0
    for key, _ in pairs(tbl) do
        if type(key) ~= "number" or key < 1 or key % 1 ~= 0 then
            return false, 0
        end
        if key > maxIndex then
            maxIndex = key
        end
        count = count + 1
    end

    if maxIndex ~= count then
        return false, 0
    end

    return true, maxIndex
end

local function encodeString(value)
    local out = value
    out = out:gsub("\\", "\\\\")
    out = out:gsub('"', '\\"')
    out = out:gsub("\b", "\\b")
    out = out:gsub("\f", "\\f")
    out = out:gsub("\n", "\\n")
    out = out:gsub("\r", "\\r")
    out = out:gsub("\t", "\\t")
    return '"' .. out .. '"'
end

local function encodeValue(value)
    local valueType = type(value)

    if value == nil or value == json.null then
        return "null"
    elseif valueType == "boolean" then
        return value and "true" or "false"
    elseif valueType == "number" then
        if value ~= value or value == math.huge or value == -math.huge then
            error("json.encode: cannot encode NaN or infinite numbers")
        end
        return tostring(value)
    elseif valueType == "string" then
        return encodeString(value)
    elseif valueType == "table" then
        local array, size = isArray(value)
        local parts = {}

        if array then
            for i = 1, size do
                parts[#parts + 1] = encodeValue(value[i])
            end
            return "[" .. table.concat(parts, ",") .. "]"
        end

        for key, child in pairs(value) do
            if type(key) ~= "string" then
                error("json.encode: object keys must be strings")
            end
            parts[#parts + 1] = encodeString(key) .. ":" .. encodeValue(child)
        end

        return "{" .. table.concat(parts, ",") .. "}"
    end

    error("json.encode: unsupported type '" .. valueType .. "'")
end

function json.encode(value)
    return encodeValue(value)
end

function json.decode(input)
    if type(input) ~= "string" then
        error("json.decode: input must be a string")
    end

    local index = 1
    local length = #input

    local function decodeError(message)
        error("json.decode: " .. message .. " at position " .. index)
    end

    local function skipWhitespace()
        while index <= length do
            local c = input:sub(index, index)
            if c == " " or c == "\n" or c == "\r" or c == "\t" then
                index = index + 1
            else
                break
            end
        end
    end

    local parseValue

    local function parseString()
        index = index + 1
        local out = {}

        while index <= length do
            local c = input:sub(index, index)

            if c == '"' then
                index = index + 1
                return table.concat(out)
            end

            if c == "\\" then
                index = index + 1
                if index > length then
                    decodeError("unterminated escape")
                end

                local esc = input:sub(index, index)
                if esc == '"' or esc == "\\" or esc == "/" then
                    out[#out + 1] = esc
                elseif esc == "b" then
                    out[#out + 1] = "\b"
                elseif esc == "f" then
                    out[#out + 1] = "\f"
                elseif esc == "n" then
                    out[#out + 1] = "\n"
                elseif esc == "r" then
                    out[#out + 1] = "\r"
                elseif esc == "t" then
                    out[#out + 1] = "\t"
                elseif esc == "u" then
                    local hex = input:sub(index + 1, index + 4)
                    if #hex < 4 or not hex:match("^[0-9a-fA-F]+$") then
                        decodeError("invalid unicode escape")
                    end
                    local codepoint = tonumber(hex, 16)
                    if codepoint and codepoint <= 0x7F then
                        out[#out + 1] = string.char(codepoint)
                    else
                        out[#out + 1] = "?"
                    end
                    index = index + 4
                else
                    decodeError("invalid escape character")
                end
            else
                out[#out + 1] = c
            end

            index = index + 1
        end

        decodeError("unterminated string")
    end

    local function parseNumber()
        local start = index
        if input:sub(index, index) == "-" then
            index = index + 1
        end

        if input:sub(index, index) == "0" then
            index = index + 1
        else
            if not input:sub(index, index):match("%d") then
                decodeError("invalid number")
            end
            while input:sub(index, index):match("%d") do
                index = index + 1
            end
        end

        if input:sub(index, index) == "." then
            index = index + 1
            if not input:sub(index, index):match("%d") then
                decodeError("invalid number")
            end
            while input:sub(index, index):match("%d") do
                index = index + 1
            end
        end

        local exp = input:sub(index, index)
        if exp == "e" or exp == "E" then
            index = index + 1
            local sign = input:sub(index, index)
            if sign == "+" or sign == "-" then
                index = index + 1
            end
            if not input:sub(index, index):match("%d") then
                decodeError("invalid number")
            end
            while input:sub(index, index):match("%d") do
                index = index + 1
            end
        end

        local number = tonumber(input:sub(start, index - 1))
        if number == nil then
            decodeError("invalid number")
        end
        return number
    end

    local function parseArray()
        index = index + 1
        skipWhitespace()

        local out = {}
        if input:sub(index, index) == "]" then
            index = index + 1
            return out
        end

        while true do
            out[#out + 1] = parseValue()
            skipWhitespace()

            local c = input:sub(index, index)
            if c == "]" then
                index = index + 1
                return out
            end
            if c ~= "," then
                decodeError("expected ',' or ']' in array")
            end

            index = index + 1
            skipWhitespace()
        end
    end

    local function parseObject()
        index = index + 1
        skipWhitespace()

        local out = {}
        if input:sub(index, index) == "}" then
            index = index + 1
            return out
        end

        while true do
            if input:sub(index, index) ~= '"' then
                decodeError("expected string key")
            end
            local key = parseString()
            skipWhitespace()

            if input:sub(index, index) ~= ":" then
                decodeError("expected ':' after key")
            end
            index = index + 1
            skipWhitespace()

            out[key] = parseValue()
            skipWhitespace()

            local c = input:sub(index, index)
            if c == "}" then
                index = index + 1
                return out
            end
            if c ~= "," then
                decodeError("expected ',' or '}' in object")
            end

            index = index + 1
            skipWhitespace()
        end
    end

    parseValue = function()
        skipWhitespace()
        if index > length then
            decodeError("unexpected end of input")
        end

        local c = input:sub(index, index)
        if c == '"' then
            return parseString()
        elseif c == "{" then
            return parseObject()
        elseif c == "[" then
            return parseArray()
        elseif c == "-" or c:match("%d") then
            return parseNumber()
        elseif input:sub(index, index + 3) == "true" then
            index = index + 4
            return true
        elseif input:sub(index, index + 4) == "false" then
            index = index + 5
            return false
        elseif input:sub(index, index + 3) == "null" then
            index = index + 4
            return json.null
        end

        decodeError("unexpected token")
    end

    local value = parseValue()
    skipWhitespace()
    if index <= length then
        decodeError("trailing characters")
    end

    return value
end

return json

