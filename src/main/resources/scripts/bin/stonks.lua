-- /bin/exchange.lua
-- STARMADE COMMODITY EXCHANGE: interactive galactic market tracker.
-- Samples the trade network every few seconds and renders every actively-stocked
-- item in a category tree with live price history, search, and cheeky headlines.

local SAMPLE_INTERVAL_MS = 3000
local FRAME_DELAY_MS     = 66
local HISTORY_SIZE       = 80
local ROW_H              = 18
local HEADER_Y           = 8
local SEARCH_Y           = 34
local SEARCH_H           = 20
local ROWS_TOP_Y         = 78
local HEADLINE_THRESHOLD = 1.0

-- ---- state ----------------------------------------------------------------

local stocks            = {}   -- typeId -> stock record
local categoryExpanded  = {}   -- name -> bool (default: expanded)
local searchQuery       = ""
local searchFocused     = false
local rows              = {}   -- flattened visible rows
local selectedIdx       = 1    -- index in `rows`
local scrollOffset      = 0    -- first visible row
local offers            = {}   -- list of TradeOffer for the current selection
local offersFor         = nil  -- typeId the offers were fetched for
local offersScroll      = 0
local sampleCount       = 0
local lastSampleMs      = -1
local headline          = "Awaiting first tick..."
local diagNodes         = 0
local diagSnapshotSize  = 0
local diagStockedSize   = 0

-- ---- utilities ------------------------------------------------------------

local function clamp(v, lo, hi)
    if v < lo then return lo elseif v > hi then return hi else return v end
end

local function formatNumber(n)
    if n == nil then return "-" end
    local s = tostring(math.floor(n))
    local out, cnt = "", 0
    for i = #s, 1, -1 do
        out = s:sub(i, i) .. out
        cnt = cnt + 1
        if cnt % 3 == 0 and i > 1 then out = "," .. out end
    end
    return out
end

local function pushHistory(stock, price)
    stock.history[#stock.history + 1] = price
    if #stock.history > HISTORY_SIZE then
        table.remove(stock.history, 1)
    end
end

local function current(stock)
    return stock.history[#stock.history] or 0
end

local function pctChange(stock)
    local n = #stock.history
    if n < 2 then return 0 end
    local prev = stock.history[n - 1]
    local cur  = stock.history[n]
    if prev == 0 then return 0 end
    return (cur - prev) / prev * 100
end

-- ---- category tree build --------------------------------------------------

local function rebuildRows()
    local byCategory = {}
    for _, s in pairs(stocks) do
        local cat = s.category or "Other"
        byCategory[cat] = byCategory[cat] or {}
        table.insert(byCategory[cat], s)
    end

    local catList = {}
    for k in pairs(byCategory) do table.insert(catList, k) end
    table.sort(catList)

    local q = searchQuery:lower()
    local filtering = q ~= ""

    local priorSelected = nil
    if rows[selectedIdx] and rows[selectedIdx].kind == "item" then
        priorSelected = rows[selectedIdx].stock
    end

    rows = {}
    for _, cat in ipairs(catList) do
        local matched = {}
        for _, s in ipairs(byCategory[cat]) do
            if not filtering or s.name:lower():find(q, 1, true) then
                table.insert(matched, s)
            end
        end
        if #matched > 0 then
            table.sort(matched, function(a, b) return a.name < b.name end)
            local expanded = categoryExpanded[cat]
            if expanded == nil then expanded = true end
            if filtering then expanded = true end
            table.insert(rows, { kind = "header", category = cat, count = #matched, expanded = expanded })
            if expanded then
                for _, s in ipairs(matched) do
                    table.insert(rows, { kind = "item", stock = s, category = cat })
                end
            end
        end
    end

    -- Restore selection to same stock if still present, else first item.
    selectedIdx = 0
    if priorSelected then
        for i, r in ipairs(rows) do
            if r.kind == "item" and r.stock == priorSelected then
                selectedIdx = i; break
            end
        end
    end
    if selectedIdx == 0 then
        for i, r in ipairs(rows) do
            if r.kind == "item" then selectedIdx = i; break end
        end
    end
    if selectedIdx == 0 then selectedIdx = 1 end
end

-- ---- sampling -------------------------------------------------------------

local function sample()
    local snapshot     = trade:getMarketSnapshot()
    diagNodes          = trade:getNodeCount() or 0
    diagSnapshotSize   = #snapshot
    diagStockedSize    = 0
    for _, entry in ipairs(snapshot) do
        local totalStock = entry:getTotalStock() or 0
        if totalStock > 0 then
            diagStockedSize = diagStockedSize + 1
            local typeId = entry:getId()
            local name   = entry:getName() or "?"
            local info   = entry:getInfo()
            local category = (info and info:getCategory()) or "Other"
            local buy    = entry:getAvgBuy()
            local sell   = entry:getAvgSell()
            local price
            if buy and sell then price = math.floor((buy + sell) / 2)
            elseif buy then price = buy
            elseif sell then price = sell end
            if price and price > 0 then
                local s = stocks[typeId]
                if s == nil then
                    s = { id = typeId, name = name, history = {}, category = category, totalStock = totalStock, offerCount = 0 }
                    stocks[typeId] = s
                end
                s.name       = name
                s.category   = category
                s.totalStock = totalStock
                s.offerCount = (entry:getBuyOfferCount() or 0) + (entry:getSellOfferCount() or 0)
                pushHistory(s, price)
            end
        end
    end
    sampleCount = sampleCount + 1
    rebuildRows()
end

-- ---- market sentiment & headlines ----------------------------------------

local function marketSentiment()
    if next(stocks) == nil then return "OPENING BELL", 0.6, 0.6, 0.6 end
    local up, down = 0, 0
    for _, s in pairs(stocks) do
        local c = pctChange(s)
        if c > 0.05 then up = up + 1
        elseif c < -0.05 then down = down + 1 end
    end
    if up > down * 1.5 then return "BULL MARKET", 0.3, 1.0, 0.4
    elseif down > up * 1.5 then return "BEAR MARKET", 1.0, 0.3, 0.35
    else return "MIXED", 0.9, 0.85, 0.4 end
end

local UP_TEMPLATES = {
    "%s SURGES %+.1f%%, analysts baffled",
    "RALLY: %s climbs %+.1f%% in thin trading",
    "TO THE MOON: %s up %+.1f%%",
    "%s breaks out, gains %+.1f%%",
    "Shopkeepers bullish on %s (%+.1f%%)",
}
local DOWN_TEMPLATES = {
    "%s CRASHES %.1f%%, traders blame oversupply",
    "BLOODBATH: %s down %.1f%%",
    "%s tumbles %.1f%%, circuit breakers tripped",
    "Panic selling in %s (%.1f%%)",
    "%s hits support, slips %.1f%%",
}
local FLAT_TEMPLATES = {
    "Trading flat. Station owners napping.",
    "Dealers unchanged. Coffee break continues.",
    "Market snoozing; volume at historic lows.",
}

local function refreshHeadline()
    local bestUp, bestUpPct = nil, 0
    local bestDown, bestDownPct = nil, 0
    local n = 0
    for _, s in pairs(stocks) do
        n = n + 1
        local c = pctChange(s)
        if c > bestUpPct then bestUp, bestUpPct = s, c end
        if c < bestDownPct then bestDown, bestDownPct = s, c end
    end
    if n < 2 then headline = "Scanning trade network..." return end
    if bestUpPct > -bestDownPct and bestUpPct > HEADLINE_THRESHOLD then
        headline = string.format(UP_TEMPLATES[math.random(#UP_TEMPLATES)], bestUp.name, bestUpPct)
    elseif -bestDownPct > HEADLINE_THRESHOLD then
        headline = string.format(DOWN_TEMPLATES[math.random(#DOWN_TEMPLATES)], bestDown.name, bestDownPct)
    else
        headline = FLAT_TEMPLATES[math.random(#FLAT_TEMPLATES)]
    end
end

-- ---- layout helpers -------------------------------------------------------

local function splitX(w) return math.floor(w * 0.55) end

local function treePaneBottom(h)
    -- Leave room for the offers pane below (minimum 120px) plus the footer.
    local availableH = h - ROWS_TOP_Y - 30
    local treeH = math.floor(availableH * 0.6)
    return ROWS_TOP_Y + treeH
end

local function offersPaneBounds(h)
    local top    = treePaneBottom(h) + 24
    local bottom = h - 30
    return top, bottom
end

local function visibleRowCount(h)
    return math.max(1, math.floor((treePaneBottom(h) - ROWS_TOP_Y) / ROW_H))
end

local function offersRowCount(h)
    local top, bottom = offersPaneBounds(h)
    return math.max(1, math.floor((bottom - top) / ROW_H))
end

local function currentStock()
    local r = rows[selectedIdx]
    if r and r.kind == "item" then return r.stock end
    return nil
end

local function refreshOffers()
    local s = currentStock()
    if s == nil then
        offers = {}; offersFor = nil; offersScroll = 0
        return
    end
    if offersFor == s.id and #offers > 0 then return end
    local all = trade:findAllOffers(s.id)
    -- Sort: player-buy first, then by price asc for buys / desc for sells.
    table.sort(all, function(a, b)
        local ab, bb = a:isPlayerBuy(), b:isPlayerBuy()
        if ab ~= bb then return ab end
        if ab then return a:getPrice() < b:getPrice() end
        return a:getPrice() > b:getPrice()
    end)
    offers = all
    offersFor = s.id
    offersScroll = 0
end

-- ---- rendering ------------------------------------------------------------

local function drawBackground(w, h)
    gfx2d.setLayer("bg")
    gfx2d.clearLayer("bg")
    gfx2d.rect(0, 0, w, h, 0.02, 0.03, 0.05, 1.0, true)
    gfx2d.rect(0, 0, w, 26, 0.08, 0.12, 0.2, 1.0, true)
    gfx2d.line(0, 26, w, 26, 0.4, 0.55, 0.85, 1.0, 1)
    gfx2d.rect(0, h - 26, w, 26, 0.05, 0.08, 0.12, 1.0, true)
    gfx2d.line(0, h - 27, w, h - 27, 0.4, 0.55, 0.85, 1.0, 1)
    local sx = splitX(w)
    gfx2d.line(sx, 26, sx, h - 26, 0.25, 0.35, 0.55, 1.0, 1)
end

local function drawHeader(w)
    gfx2d.setLayer("hud")
    gfx2d.text(8, HEADER_Y, "STARMADE COMMODITY EXCHANGE", 0.95, 0.98, 1.0, 1.0, 1)
    local sent, sr, sg, sb = marketSentiment()
    local right = string.format("Tick #%d  %s", sampleCount, sent)
    local rx = math.max(8, w - (#right * 6) - 10)
    gfx2d.text(rx, HEADER_Y, right, sr, sg, sb, 1.0, 1)
end

local function drawSearchBar(w)
    gfx2d.setLayer("hud")
    local sx = splitX(w)
    local barW = sx - 20
    local bx = 8
    local by = SEARCH_Y

    gfx2d.rect(bx, by, barW, SEARCH_H, 0.05, 0.08, 0.12, 1.0, true)
    local bordR, bordG, bordB = 0.3, 0.4, 0.6
    if searchFocused then bordR, bordG, bordB = 0.4, 0.8, 1.0 end
    gfx2d.rect(bx, by, barW, SEARCH_H, bordR, bordG, bordB, 1.0, false)
    local label
    if searchQuery == "" and not searchFocused then
        label = "Search items... (click or press /)"
        gfx2d.text(bx + 6, by + 4, label, 0.4, 0.5, 0.6, 1.0, 1)
    else
        label = searchQuery .. (searchFocused and "_" or "")
        gfx2d.text(bx + 6, by + 4, label, 0.9, 0.95, 1.0, 1.0, 1)
    end
end

local function drawTree(w, h)
    gfx2d.setLayer("hud")
    local sx = splitX(w)
    local tableX = 8
    local tableW = sx - tableX - 8
    local rowCount = visibleRowCount(h)

    -- Column positions within the item row (header uses a different layout)
    local colName   = tableX + 16
    local colPrice  = tableX + math.floor(tableW * 0.55)
    local colChg    = tableX + math.floor(tableW * 0.74)
    local colStock  = tableX + math.floor(tableW * 0.87)

    -- Column labels
    gfx2d.text(tableX + 16,  ROWS_TOP_Y - 16, "NAME",   0.5, 0.7, 0.95, 1.0, 1)
    gfx2d.text(colPrice,     ROWS_TOP_Y - 16, "PRICE",  0.5, 0.7, 0.95, 1.0, 1)
    gfx2d.text(colChg,       ROWS_TOP_Y - 16, "CHG",    0.5, 0.7, 0.95, 1.0, 1)
    gfx2d.text(colStock,     ROWS_TOP_Y - 16, "GALSTK", 0.5, 0.7, 0.95, 1.0, 1)
    gfx2d.line(tableX, ROWS_TOP_Y - 2, tableX + tableW, ROWS_TOP_Y - 2, 0.3, 0.45, 0.7, 1.0, 1)

    if #rows == 0 then
        gfx2d.text(tableX, ROWS_TOP_Y + 12,
            searchQuery ~= "" and ("No matches for \"" .. searchQuery .. "\".")
                               or "No stocked items found.",
            0.75, 0.75, 0.75, 1.0, 1)
        if searchQuery == "" then
            gfx2d.text(tableX, ROWS_TOP_Y + 32, string.format("trade nodes in registry: %d", diagNodes), 0.75, 0.85, 0.55, 1.0, 1)
            gfx2d.text(tableX, ROWS_TOP_Y + 50, string.format("types in market snapshot: %d", diagSnapshotSize), 0.75, 0.85, 0.55, 1.0, 1)
            gfx2d.text(tableX, ROWS_TOP_Y + 68, string.format("stocked types: %d", diagStockedSize), 0.75, 0.85, 0.55, 1.0, 1)
        end
        return
    end

    local firstRow = scrollOffset + 1
    local lastRow  = math.min(#rows, firstRow + rowCount - 1)

    for i = firstRow, lastRow do
        local r = rows[i]
        local y = ROWS_TOP_Y + (i - firstRow) * ROW_H

        if r.kind == "header" then
            gfx2d.rect(tableX - 2, y - 2, tableW + 4, ROW_H, 0.05, 0.1, 0.18, 1.0, true)
            local arrow = r.expanded and "v" or ">"
            gfx2d.text(tableX,     y, arrow, 0.6, 0.85, 1.0, 1.0, 1)
            gfx2d.text(tableX + 14, y, r.category, 0.7, 0.9, 1.0, 1.0, 1)
            gfx2d.text(tableX + tableW - 40, y, "(" .. tostring(r.count) .. ")", 0.55, 0.7, 0.85, 1.0, 1)
        else
            local s = r.stock
            local chg = pctChange(s)
            local cr, cg, cb = 0.85, 0.85, 0.85
            if chg > 0.05 then cr, cg, cb = 0.3, 1.0, 0.4
            elseif chg < -0.05 then cr, cg, cb = 1.0, 0.35, 0.4 end

            if i == selectedIdx then
                gfx2d.rect(tableX - 2, y - 2, tableW + 4, ROW_H, 0.08, 0.15, 0.25, 1.0, true)
                gfx2d.rect(tableX - 2, y - 2, tableW + 4, ROW_H, 0.4, 0.6, 0.9, 1.0, false)
            end

            local arrow = chg > 0.05 and "^" or (chg < -0.05 and "v" or "-")
            gfx2d.text(colName,  y, s.name, 0.95, 0.98, 1.0, 1.0, 1, colPrice - colName - 4, 14, "left", false)
            gfx2d.text(colPrice, y, formatNumber(current(s)), 0.95, 0.98, 1.0, 1.0, 1)
            gfx2d.text(colChg,   y, string.format("%s%.1f%%", chg >= 0 and "+" or "", chg), cr, cg, cb, 1.0, 1)
            gfx2d.text(colStock, y, formatNumber(s.totalStock), 0.8, 0.8, 0.8, 1.0, 1)
            gfx2d.text(tableX + tableW - 8, y, arrow, cr, cg, cb, 1.0, 1)
        end
    end

    -- Scroll indicator
    if #rows > rowCount then
        local sbX = tableX + tableW + 2
        local sbH = rowCount * ROW_H
        gfx2d.rect(sbX, ROWS_TOP_Y, 3, sbH, 0.1, 0.15, 0.2, 1.0, true)
        local thumbH = math.max(10, math.floor(sbH * rowCount / #rows))
        local thumbY = ROWS_TOP_Y + math.floor(sbH * scrollOffset / #rows)
        gfx2d.rect(sbX, thumbY, 3, thumbH, 0.4, 0.55, 0.85, 1.0, true)
    end
end

local function drawOffers(w, h)
    gfx2d.setLayer("hud")
    local sx = splitX(w)
    local paneX = 8
    local paneW = sx - 16
    local top, bottom = offersPaneBounds(h)
    local rowCount = offersRowCount(h)

    -- Title
    local s = currentStock()
    local title = s and ("OFFERS - " .. s.name) or "OFFERS"
    gfx2d.text(paneX, top - 18, title, 0.5, 0.7, 0.95, 1.0, 1)

    -- Column setup
    local colDir   = paneX
    local colNode  = paneX + 48
    local colSec   = paneX + math.floor(paneW * 0.55)
    local colPrice = paneX + math.floor(paneW * 0.76)
    local colAmt   = paneX + math.floor(paneW * 0.90)

    gfx2d.text(colDir,   top - 2, "DIR",   0.5, 0.7, 0.95, 1.0, 1)
    gfx2d.text(colNode,  top - 2, "NODE",  0.5, 0.7, 0.95, 1.0, 1)
    gfx2d.text(colSec,   top - 2, "SECTOR", 0.5, 0.7, 0.95, 1.0, 1)
    gfx2d.text(colPrice, top - 2, "PRICE", 0.5, 0.7, 0.95, 1.0, 1)
    gfx2d.text(colAmt,   top - 2, "AMT",   0.5, 0.7, 0.95, 1.0, 1)
    gfx2d.line(paneX, top + 12, paneX + paneW, top + 12, 0.3, 0.45, 0.7, 1.0, 1)

    if s == nil then
        gfx2d.text(paneX, top + 18, "(no item selected)", 0.6, 0.6, 0.65, 1.0, 1)
        return
    end
    if #offers == 0 then
        gfx2d.text(paneX, top + 18, "No active offers for this item.", 0.65, 0.65, 0.7, 1.0, 1)
        return
    end

    local first = offersScroll + 1
    local last  = math.min(#offers, first + rowCount - 1)
    for i = first, last do
        local o = offers[i]
        local y = top + 14 + (i - first) * ROW_H
        local dir = o:isPlayerBuy() and "BUY" or "SELL"
        local cr, cg, cb
        if o:isPlayerBuy() then cr, cg, cb = 0.3, 1.0, 0.4
        else cr, cg, cb = 1.0, 0.5, 0.45 end

        local node = o:getNode()
        local nodeName = (node and node:getStationName()) or "(unknown)"
        local sec = node and node:getSector()
        local secStr = sec and string.format("%d,%d,%d", sec:getX(), sec:getY(), sec:getZ()) or "?"

        gfx2d.text(colDir,   y, dir,         cr, cg, cb, 1.0, 1)
        gfx2d.text(colNode,  y, nodeName,    0.92, 0.95, 1.0, 1.0, 1, colSec - colNode - 4, 14, "left", false)
        gfx2d.text(colSec,   y, secStr,      0.75, 0.8, 0.9, 1.0, 1)
        gfx2d.text(colPrice, y, formatNumber(o:getPrice()), 0.95, 0.98, 1.0, 1.0, 1)
        gfx2d.text(colAmt,   y, formatNumber(o:getAmount()), 0.8, 0.8, 0.8, 1.0, 1)
    end

    -- Scroll indicator for offers pane.
    if #offers > rowCount then
        local sbX = paneX + paneW + 2
        local sbH = rowCount * ROW_H
        gfx2d.rect(sbX, top + 14, 3, sbH, 0.1, 0.15, 0.2, 1.0, true)
        local thumbH = math.max(10, math.floor(sbH * rowCount / #offers))
        local thumbY = top + 14 + math.floor(sbH * offersScroll / #offers)
        gfx2d.rect(sbX, thumbY, 3, thumbH, 0.4, 0.55, 0.85, 1.0, true)
    end
end

local function drawChart(w, h)
    gfx2d.setLayer("hud")
    local x0 = splitX(w) + 16
    local y0 = 36
    local cw = math.max(40, w - x0 - 12)
    local ch = math.max(60, h - y0 - 40)

    local s = currentStock()
    if s == nil then
        gfx2d.text(x0, y0, "Select an item to view history.", 0.7, 0.75, 0.8, 1.0, 1)
        return
    end

    gfx2d.text(x0, y0, s.name, 0.95, 0.98, 1.0, 1.0, 2)
    local chg = pctChange(s)
    local cr, cg, cb = 0.85, 0.85, 0.85
    if chg > 0.05 then cr, cg, cb = 0.3, 1.0, 0.4
    elseif chg < -0.05 then cr, cg, cb = 1.0, 0.35, 0.4 end
    gfx2d.text(x0,       y0 + 26, formatNumber(current(s)) .. " cr", cr, cg, cb, 1.0, 1)
    gfx2d.text(x0 + 120, y0 + 28, string.format("%s%.2f%%", chg >= 0 and "+" or "", chg), cr, cg, cb, 1.0, 1)
    gfx2d.text(x0,       y0 + 44, string.format("%s  -  %d offers  -  %s in galactic stock",
        s.category or "?", s.offerCount or 0, formatNumber(s.totalStock)), 0.6, 0.75, 0.9, 1.0, 1)

    local ax, ay = x0, y0 + 64
    local aw, ah = cw, ch - 64
    gfx2d.rect(ax, ay, aw, ah, 0.04, 0.07, 0.11, 1.0, true)
    gfx2d.rect(ax, ay, aw, ah, 0.18, 0.28, 0.45, 1.0, false)

    local n = #s.history
    if n < 2 then
        gfx2d.text(ax + 8, ay + 8, "Collecting data... (" .. n .. "/2)", 0.7, 0.7, 0.7, 1.0, 1)
        return
    end

    local minV, maxV = s.history[1], s.history[1]
    for _, v in ipairs(s.history) do
        if v < minV then minV = v end
        if v > maxV then maxV = v end
    end
    if maxV == minV then minV = minV - 1; maxV = maxV + 1 end
    local pad = (maxV - minV) * 0.1
    minV = minV - pad; maxV = maxV + pad

    for i = 1, 3 do
        local gy = ay + math.floor(ah * i / 4)
        gfx2d.line(ax + 1, gy, ax + aw - 1, gy, 0.12, 0.18, 0.28, 1.0, 1)
    end

    local stepX = aw / math.max(1, HISTORY_SIZE - 1)
    local firstX = ax + (HISTORY_SIZE - n) * stepX
    local function toY(v) return ay + ah - math.floor((v - minV) / (maxV - minV) * (ah - 4)) - 2 end

    for i = 2, n do
        local px = firstX + (i - 2) * stepX
        local nx = firstX + (i - 1) * stepX
        local py = toY(s.history[i - 1])
        local ny = toY(s.history[i])
        local lr, lg, lb
        if s.history[i] >= s.history[i - 1] then lr, lg, lb = 0.3, 1.0, 0.4
        else lr, lg, lb = 1.0, 0.35, 0.4 end
        gfx2d.line(math.floor(px), py, math.floor(nx), ny, lr, lg, lb, 1.0, 2)
    end

    gfx2d.text(ax + 4, ay + 4,       formatNumber(maxV), 0.55, 0.65, 0.85, 1.0, 1)
    gfx2d.text(ax + 4, ay + ah - 14, formatNumber(minV), 0.55, 0.65, 0.85, 1.0, 1)
end

local function drawFooter(w, h)
    gfx2d.setLayer("hud")
    gfx2d.text(8, h - 18, headline, 0.85, 0.95, 0.7, 1.0, 1, w - 16, 16, "left", false)
end

-- ---- input ----------------------------------------------------------------

local function ensureSelectionVisible(h)
    local rowCount = visibleRowCount(h)
    if rowCount < 1 then return end
    if selectedIdx < scrollOffset + 1 then
        scrollOffset = selectedIdx - 1
    elseif selectedIdx > scrollOffset + rowCount then
        scrollOffset = selectedIdx - rowCount
    end
    scrollOffset = clamp(scrollOffset, 0, math.max(0, #rows - rowCount))
end

local function moveSelection(delta)
    if #rows == 0 then return end
    local i = selectedIdx + delta
    -- Skip over header rows.
    while i >= 1 and i <= #rows and rows[i].kind ~= "item" do
        i = i + (delta > 0 and 1 or -1)
    end
    if i >= 1 and i <= #rows then selectedIdx = i end
end

local function toggleCategoryAt(rowIdx)
    local r = rows[rowIdx]
    if r == nil or r.kind ~= "header" then return end
    local current = categoryExpanded[r.category]
    if current == nil then current = true end
    categoryExpanded[r.category] = not current
    rebuildRows()
end

local function handleMouse(e, w, h)
    local sx = splitX(w)
    local treeBottom = treePaneBottom(h)

    if e.pressed and e.button == "left" and e.uiX and e.uiY then
        -- Search bar hit test
        local barY, barW = SEARCH_Y, sx - 20
        if e.uiX >= 8 and e.uiX <= 8 + barW and e.uiY >= barY and e.uiY <= barY + SEARCH_H then
            searchFocused = true
            return
        end
        -- Click inside the tree pane
        if e.uiX >= 4 and e.uiX <= sx - 2
                and e.uiY >= ROWS_TOP_Y - 2 and e.uiY <= treeBottom then
            local offset = math.floor((e.uiY - (ROWS_TOP_Y - 2)) / ROW_H)
            local rowIdx = scrollOffset + 1 + offset
            if rows[rowIdx] then
                if rows[rowIdx].kind == "header" then
                    toggleCategoryAt(rowIdx)
                else
                    selectedIdx = rowIdx
                    refreshOffers()
                end
            end
            searchFocused = false
            return
        end
        searchFocused = false
    end
    if e.wheel and e.wheel ~= 0 then
        -- Route wheel by which pane the cursor is in.
        if e.uiX and e.uiX <= sx and e.uiY then
            if e.uiY <= treeBottom then
                local rc = visibleRowCount(h)
                scrollOffset = clamp(scrollOffset - e.wheel * 3, 0, math.max(0, #rows - rc))
            else
                local rc = offersRowCount(h)
                offersScroll = clamp(offersScroll - e.wheel * 3, 0, math.max(0, #offers - rc))
            end
        end
    end
end

-- Logiscript's "key" field comes from LWJGL Keyboard (StarMade's GLFW wrapper
-- aliases Keyboard constants). Values below match org.lwjgl.input.Keyboard.
local K_BACKSPACE = 14
local K_ESCAPE    = 1
local K_ENTER     = 28
local K_SLASH     = 53
local K_UP        = 200
local K_DOWN      = 208
local K_PAGEUP    = 201
local K_PAGEDOWN  = 209

local function handleKey(e, w, h)
    if not e.down then return end
    if searchFocused then
        if e.key == K_ESCAPE then
            searchQuery = ""
            searchFocused = false
            rebuildRows()
        elseif e.key == K_ENTER then
            searchFocused = false
        elseif e.key == K_BACKSPACE then
            if #searchQuery > 0 then
                searchQuery = searchQuery:sub(1, -2)
                rebuildRows()
                ensureSelectionVisible(h)
            end
        elseif e.char and e.char ~= "" and e.char:byte() >= 32 and e.char:byte() < 127 then
            searchQuery = searchQuery .. e.char
            rebuildRows()
            ensureSelectionVisible(h)
        end
    else
        if e.key == K_SLASH or (e.char == "/") then
            searchFocused = true
        elseif e.key == K_UP then
            moveSelection(-1); ensureSelectionVisible(h); refreshOffers()
        elseif e.key == K_DOWN then
            moveSelection(1); ensureSelectionVisible(h); refreshOffers()
        elseif e.key == K_PAGEUP then
            moveSelection(-visibleRowCount(h)); ensureSelectionVisible(h); refreshOffers()
        elseif e.key == K_PAGEDOWN then
            moveSelection(visibleRowCount(h)); ensureSelectionVisible(h); refreshOffers()
        end
    end
end

local function handleInput(w, h)
    while true do
        local e = input.poll()
        if e == nil then return end
        if e.type == "key"   then handleKey(e, w, h)
        elseif e.type == "mouse" then handleMouse(e, w, h) end
    end
end

-- ---- main -----------------------------------------------------------------

gfx2d.clear()
if gfx2d.setAutoScale then gfx2d.setAutoScale(true) end
if gfx2d.setCanvasSize and gfx2d.getViewportWidth and gfx2d.getViewportHeight then
    local vw, vh = gfx2d.getViewportWidth(), gfx2d.getViewportHeight()
    if vw and vh and vw > 0 and vh > 0 then gfx2d.setCanvasSize(vw, vh) end
end
gfx2d.createLayer("bg",  0)
gfx2d.createLayer("hud", 8)

math.randomseed(console:getTime())

print("StarMade Stock Exchange starting...")
print(trade:getDiagnostic())

sample()
refreshHeadline()
refreshOffers()
lastSampleMs = console:getTime()

while true do
    local w, h = gfx2d.getWidth(), gfx2d.getHeight()
    handleInput(w, h)

    local now = console:getTime()
    if now - lastSampleMs >= SAMPLE_INTERVAL_MS then
        sample()
        refreshHeadline()
        offersFor = nil  -- force re-query so prices/amounts stay fresh
        refreshOffers()
        lastSampleMs = now
    end

    if gfx2d.beginBatch then gfx2d.beginBatch() end
    gfx2d.clearLayer("hud")
    drawBackground(w, h)
    drawHeader(w)
    drawSearchBar(w)
    drawTree(w, h)
    drawOffers(w, h)
    drawChart(w, h)
    drawFooter(w, h)
    if gfx2d.commitBatch then gfx2d.commitBatch() end

    util.sleep(FRAME_DELAY_MS)
end
