local gfx2d = {}

function gfx2d.pack(r, g, b, a)
    local rr = math.max(0, math.min(255, math.floor((r or 0) + 0.5)))
    local gg = math.max(0, math.min(255, math.floor((g or 0) + 0.5)))
    local bb = math.max(0, math.min(255, math.floor((b or 0) + 0.5)))
    local aa = math.max(0, math.min(255, math.floor((a or 255) + 0.5)))
    return bit32.bor(bit32.lshift(rr, 24), bit32.lshift(gg, 16), bit32.lshift(bb, 8), aa)
end

function gfx2d.unpack(color)
    local value = color or 0
    local r = bit32.band(bit32.rshift(value, 24), 0xFF)
    local g = bit32.band(bit32.rshift(value, 16), 0xFF)
    local b = bit32.band(bit32.rshift(value, 8), 0xFF)
    local a = bit32.band(value, 0xFF)
    return r, g, b, a
end

function gfx2d.newBitmap(width, height, fillColor)
    local w = math.max(1, math.floor(width or 1))
    local h = math.max(1, math.floor(height or 1))
    local fill = fillColor or 0x00000000
    local pixels = {}
    for i = 1, (w * h) do
        pixels[i] = fill
    end
    return {
        width = w,
        height = h,
        pixels = pixels
    }
end

function gfx2d.setPixel(bitmap, x, y, color)
    if not bitmap or not bitmap.pixels or not bitmap.width or not bitmap.height then
        return false
    end
    local xi = math.floor(x or -1)
    local yi = math.floor(y or -1)
    if xi < 0 or yi < 0 or xi >= bitmap.width or yi >= bitmap.height then
        return false
    end
    bitmap.pixels[(yi * bitmap.width) + xi + 1] = color or 0xFFFFFFFF
    return true
end

function gfx2d.getPixel(bitmap, x, y)
    if not bitmap or not bitmap.pixels or not bitmap.width or not bitmap.height then
        return nil
    end
    local xi = math.floor(x or -1)
    local yi = math.floor(y or -1)
    if xi < 0 or yi < 0 or xi >= bitmap.width or yi >= bitmap.height then
        return nil
    end
    return bitmap.pixels[(yi * bitmap.width) + xi + 1]
end

function gfx2d.checkerBitmap(width, height, colorA, colorB, cellSize)
    local bitmap = gfx2d.newBitmap(width, height, colorA or 0x00000000)
    local c0 = colorA or 0xFFFFFFFF
    local c1 = colorB or 0x222222FF
    local cell = math.max(1, math.floor(cellSize or 1))

    for y = 0, bitmap.height - 1 do
        for x = 0, bitmap.width - 1 do
            local a = math.floor(x / cell)
            local b = math.floor(y / cell)
            gfx2d.setPixel(bitmap, x, y, ((a + b) % 2 == 0) and c0 or c1)
        end
    end
    return bitmap
end

function gfx2d.grayscaleBitmap(width, height, values, alpha)
    local bitmap = gfx2d.newBitmap(width, height, 0x00000000)
    local a = math.max(0, math.min(255, math.floor((alpha or 255) + 0.5)))
    for y = 0, bitmap.height - 1 do
        for x = 0, bitmap.width - 1 do
            local index = (y * bitmap.width) + x + 1
            local v = values and values[index] or 0
            local g = math.max(0, math.min(255, math.floor((v or 0) + 0.5)))
            bitmap.pixels[index] = gfx2d.pack(g, g, g, a)
        end
    end
    return bitmap
end

function gfx2d.textMaskBitmap(rows, onColor, offColor, onChars)
    if type(rows) ~= "table" or #rows == 0 then
        return gfx2d.newBitmap(1, 1, offColor or 0x00000000)
    end

    local width = 0
    for i = 1, #rows do
        width = math.max(width, #(rows[i] or ""))
    end
    local bitmap = gfx2d.newBitmap(width, #rows, offColor or 0x00000000)
    local on = onColor or 0xFFFFFFFF
    local allowed = onChars or "#@X1"

    local lookup = {}
    for i = 1, #allowed do
        lookup[string.sub(allowed, i, i)] = true
    end

    for y = 1, #rows do
        local row = rows[y] or ""
        for x = 1, #row do
            local ch = string.sub(row, x, x)
            if lookup[ch] then
                bitmap.pixels[((y - 1) * bitmap.width) + x] = on
            end
        end
    end

    return bitmap
end

function gfx2d.draw(gfxApi, x, y, bitmap)
    if not gfxApi or not bitmap or not bitmap.pixels then
        return false
    end
    return gfxApi.bitmap(x or 0, y or 0, bitmap.width or 1, bitmap.height or 1, bitmap.pixels)
end

return gfx2d
