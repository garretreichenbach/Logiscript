-- /lib/gui.lua
-- Reusable, resize-aware GUI components for LuaMade gfx2d.

local GUI = {}

local function clamp(v, minV, maxV)
	if v < minV then
		return minV
	end
	if v > maxV then
		return maxV
	end
	return v
end

local function round(n)
	return math.floor(n + 0.5)
end

-- ============================================================================
-- GUIManager: Main controller for GUI rendering and events
-- ============================================================================

local GUIManager = {}
GUIManager.__index = GUIManager

function GUIManager.new()
	local self = setmetatable({}, GUIManager)
	self.components = {}
	self.layers = {
		background = 0,
		grid = 2,
		panels = 4,
		components = 6,
		overlay = 8,
		effects = 10,
	}
	self.width = gfx2d.getWidth()
	self.height = gfx2d.getHeight()
	self.frameCount = 0
	self.running = false
	self.frameDelayMs = 33   -- ~30 FPS (kept for compatibility; run() is event-driven)
	self.dataRefreshMs = 500 -- how often to refresh data labels when idle (ms)
	self.layoutCallback = nil
	self.clearConsoleOnStart = true

	self.backgroundColor = { r = 0.03, g = 0.04, b = 0.07, a = 1.0 }
	self.borderColor = { r = 0.22, g = 0.4, b = 0.95, a = 0.85 }
	self.mouseOffsetX = 0
	self.mouseOffsetY = 0
	self.mouseState = {
		hasPosition = false,
		insideCanvas = false,
		uiX = 0,
		uiY = 0,
		rawX = nil,
		rawY = nil,
		leftDown = false,
	}

	self:_initializeLayers()

	return self
end

function GUIManager:_initializeLayers()
	for name, order in pairs(self.layers) do
		gfx2d.createLayer(name, order)
	end
end

function GUIManager:_rebuildLayers()
	gfx2d.clear()
	for name, order in pairs(self.layers) do
		gfx2d.createLayer(name, order)
	end
end

function GUIManager:_clearDynamicLayers()
	-- Clear per-frame layers to avoid command accumulation hitting gfx2d limits.
	gfx2d.clearLayer("grid")
	gfx2d.clearLayer("panels")
	gfx2d.clearLayer("components")
	gfx2d.clearLayer("overlay")
	gfx2d.clearLayer("effects")
end

function GUIManager:_checkWindowResize()
	local newWidth = gfx2d.getWidth()
	local newHeight = gfx2d.getHeight()
	if newWidth ~= self.width or newHeight ~= self.height then
		self.width = newWidth
		self.height = newHeight
		self:_rebuildLayers()
		return true
	end
	return false
end

function GUIManager:_drawBackground()
	gfx2d.setLayer("background")
	gfx2d.clearLayer("background")

	gfx2d.rect(
		0,
		0,
		self.width,
		self.height,
		self.backgroundColor.r,
		self.backgroundColor.g,
		self.backgroundColor.b,
		self.backgroundColor.a,
		true
	)

	gfx2d.rect(
		2,
		2,
		math.max(0, self.width - 4),
		math.max(0, self.height - 4),
		self.borderColor.r,
		self.borderColor.g,
		self.borderColor.b,
		self.borderColor.a,
		false
	)
end

function GUIManager:_applyLayout()
	if self.layoutCallback then
		self.layoutCallback(self, self.width, self.height)
	end

	for _, component in ipairs(self.components) do
		if component.applyLayout then
			component:applyLayout(self.width, self.height)
		end
	end
end

function GUIManager:addComponent(component)
	table.insert(self.components, component)
	if component.setManager then
		component:setManager(self)
	else
		component.manager = self
	end
end

function GUIManager:removeComponent(component)
	for i, comp in ipairs(self.components) do
		if comp == component then
			table.remove(self.components, i)
			break
		end
	end
end

function GUIManager:update(deltaTime)
	for _, component in ipairs(self.components) do
		if component.update then
			component:update(deltaTime)
		end
	end
end

function GUIManager:draw()
	self:_checkWindowResize()
	self:_applyLayout()

	-- Stage every clear + redraw atomically so the render thread never snapshots
	-- a partially-filled buffer (fixes flicker caused by per-operation locking).
	if gfx2d.beginBatch then gfx2d.beginBatch() end

	self:_drawBackground()
	self:_clearDynamicLayers()

	for _, component in ipairs(self.components) do
		if component:isVisible() then
			component:draw()
		end
	end

	if gfx2d.commitBatch then gfx2d.commitBatch() end

	self.frameCount = self.frameCount + 1
end

local function _eventTypeLower(e)
	if type(e) ~= "table" then
		return ""
	end
	local t = e.type or e.event or e.name
	if type(t) ~= "string" then
		return ""
	end
	return string.lower(t)
end

local function _coerceButtonName(raw)
	if type(raw) == "number" then
		if raw == 1 then return "left" end
		if raw == 2 then return "right" end
		if raw == 3 then return "middle" end
		if raw == 0 then return "left" end
		return "none"
	end
	if type(raw) == "string" then
		local v = string.lower(raw)
		if v == "1" or v == "left" then return "left" end
		if v == "2" or v == "right" then return "right" end
		if v == "3" or v == "middle" then return "middle" end
		if v == "0" or v == "none" then return "none" end
	end
	return "none"
end

function GUIManager:_isMouseEvent(e)
	if type(e) ~= "table" then
		return false
	end
	local t = _eventTypeLower(e)
	if t:find("mouse", 1, true) then
		return true
	end
	return e.uiX ~= nil or e.uiY ~= nil or e.x ~= nil or e.y ~= nil or e.mouseX ~= nil or e.mouseY ~= nil
end

function GUIManager:_normalizeMouseEvent(e)
	if type(e) ~= "table" then
		return nil
	end

	local t = _eventTypeLower(e)
	local action = e.action
	if type(action) == "string" then
		action = string.lower(action)
	else
		action = ""
	end

	local layout = nil
	if input and input.getUiLayout then
		local ok, result = pcall(input.getUiLayout)
		if ok and type(result) == "table" then
			layout = result
		end
	end

	local eventCanvasX = tonumber(e.canvasX)
	local eventCanvasY = tonumber(e.canvasY)
	local layoutCanvasX = layout and tonumber(layout.canvasX) or nil
	local layoutCanvasY = layout and tonumber(layout.canvasY) or nil
	local canvasOriginX = eventCanvasX or layoutCanvasX
	local canvasOriginY = eventCanvasY or layoutCanvasY

	local uiX = tonumber(e.uiX)
	local uiY = tonumber(e.uiY)

	-- If runtime doesn't provide uiX/uiY directly, derive local canvas coords.
	if uiX == nil then
		local absX = tonumber(e.x or e.mouseX or e.screenX)
		if absX ~= nil and canvasOriginX ~= nil then
			uiX = absX - canvasOriginX
		else
			uiX = absX
		end
	end
	if uiY == nil then
		local absY = tonumber(e.y or e.mouseY or e.screenY)
		if absY ~= nil and canvasOriginY ~= nil then
			uiY = absY - canvasOriginY
		else
			uiY = absY
		end
	end

	local rawX = uiX
	local rawY = uiY
	if uiX ~= nil then
		uiX = uiX + (self.mouseOffsetX or 0)
	end
	if uiY ~= nil then
		uiY = uiY + (self.mouseOffsetY or 0)
	end

	local insideCanvas = e.insideCanvas
	if insideCanvas == nil then
		local canvasW = layout and tonumber(layout.canvasWidth) or nil
		local canvasH = layout and tonumber(layout.canvasHeight) or nil
		if canvasW == nil or canvasH == nil then
			canvasW = self.width
			canvasH = self.height
		end
		if uiX ~= nil and uiY ~= nil then
			insideCanvas = (uiX >= 0 and uiX < canvasW and uiY >= 0 and uiY < canvasH)
		else
			insideCanvas = true
		end
	end

	local pressed = e.pressed == true
	local released = e.released == true
	if not pressed and not released then
		if t:find("down", 1, true) or action == "down" or action == "press" or action == "pressed" then
			pressed = true
		elseif t:find("up", 1, true) or action == "up" or action == "release" or action == "released" then
			released = true
		end
	end

	return {
		type = "mouse",
		insideCanvas = insideCanvas,
		uiX = uiX,
		uiY = uiY,
		rawX = rawX,
		rawY = rawY,
		button = _coerceButtonName(e.button or e.btn or e.mouseButton),
		pressed = pressed,
		released = released,
		raw = e,
	}
end

function GUIManager:_dispatchMouseEvent(e)
	local mouseEvent = self:_normalizeMouseEvent(e)
	if not mouseEvent then
		return
	end

	-- Keep latest cursor/left-button state globally so components can derive
	-- hover even when runtimes emit odd mouse move/release event shapes.
	if mouseEvent.uiX ~= nil and mouseEvent.uiY ~= nil then
		self.mouseState.hasPosition = true
		self.mouseState.uiX = mouseEvent.uiX
		self.mouseState.uiY = mouseEvent.uiY
		self.mouseState.rawX = mouseEvent.rawX
		self.mouseState.rawY = mouseEvent.rawY
	end
	self.mouseState.insideCanvas = mouseEvent.insideCanvas ~= false
	if mouseEvent.button == "left" then
		if mouseEvent.pressed then
			self.mouseState.leftDown = true
		elseif mouseEvent.released then
			self.mouseState.leftDown = false
		end
	end

	-- Walk component list back-to-front so topmost (last added) gets priority.
	for i = #self.components, 1, -1 do
		local comp = self.components[i]
		if comp:isVisible() and comp.onMouseEvent then
			if comp:onMouseEvent(mouseEvent) then
				return -- event consumed by a component
			end
		end
	end
end

function GUIManager:run(maxFrames)
	self.running = true
	self.frameCount = 0
	maxFrames = maxFrames or math.huge

	-- Own the mouse so clicks do not reach the terminal text layer.
	if input and input.clear then
		input.clear()
	end
	if input and input.consumeMouse then
		input.consumeMouse()
	end

	-- Initial draw so the UI is visible immediately.
	self:update(0)
	self:draw()

	-- Event-driven loop: only redraw when mouse state changes OR the data
	-- refresh interval fires.  This eliminates the clear→redraw flicker that
	-- occurs when layers are wiped every frame regardless of changes.
	--
	-- NOTE: input.waitFor(int timeoutMs) expects MILLISECONDS as an integer.
	-- Passing a fractional value (e.g. 0.05) coerces to int 0, which makes it
	-- behave as a non-blocking poll() — causing a hot busy-loop.
	local POLL_MS         = 50 -- milliseconds to block waiting for each event
	local refreshMs       = math.max(100, math.floor(self.dataRefreshMs or 500))
	local pollsPerRefresh = math.max(1, math.floor(refreshMs / POLL_MS))
	local pollCount       = 0

	while self.running and self.frameCount < maxFrames do
		local e = nil
		if input and input.waitFor then
			e = input.waitFor(POLL_MS)   -- pass ms directly; Java takes int timeoutMs
		elseif util and util.sleep then
			util.sleep(POLL_MS / 1000.0) -- util.sleep takes seconds (float OK)
		end

		local needsRedraw = false

		-- Mouse event → dispatch and redraw immediately so hover/press is visible.
		if e ~= nil and self:_isMouseEvent(e) then
			self:_dispatchMouseEvent(e)
			needsRedraw = true
		end

		-- Timed data refresh (approximate: POLL_MS × pollsPerRefresh ≈ refreshMs).
		pollCount = pollCount + 1
		if pollCount >= pollsPerRefresh then
			pollCount   = 0
			needsRedraw = true
		end

		if needsRedraw then
			self:update(POLL_MS / 1000.0)
			self:draw()
		end
	end

	self.running = false
end

function GUIManager:stop()
	self.running = false
end

function GUIManager:setFrameDelayMs(delayMs)
	self.frameDelayMs = math.max(1, math.floor(delayMs or 33))
end

function GUIManager:setDataRefreshMs(ms)
	self.dataRefreshMs = math.max(100, math.floor(ms or 500))
end

function GUIManager:setMouseOffset(offsetX, offsetY)
	self.mouseOffsetX = math.floor(tonumber(offsetX) or 0)
	self.mouseOffsetY = math.floor(tonumber(offsetY) or 0)
end

function GUIManager:setBackgroundColor(r, g, b, a)
	self.backgroundColor = { r = r, g = g, b = b, a = a }
end

function GUIManager:setBorderColor(r, g, b, a)
	self.borderColor = { r = r, g = g, b = b, a = a }
end

function GUIManager:setLayoutCallback(callback)
	self.layoutCallback = callback
end

GUI.GUIManager = GUIManager

-- ============================================================================
-- Component: Base class for all UI components
-- ============================================================================

local Component = {}
Component.__index = Component

function Component.new(x, y, width, height)
	local self = setmetatable({}, Component)
	self.x = x or 0
	self.y = y or 0
	self.width = width or 0
	self.height = height or 0
	self.visible = true
	self.manager = nil
	self.layer = "components"
	self.children = nil

	self.relativeRect = nil
	self.layoutCallback = nil

	return self
end

function Component:setManager(manager)
	self.manager = manager
	if self.children then
		for _, child in ipairs(self.children) do
			if child.setManager then
				child:setManager(manager)
			else
				child.manager = manager
			end
		end
	end
end

function Component:setPosition(x, y)
	self.x = x
	self.y = y
end

function Component:setSize(width, height)
	self.width = width
	self.height = height
end

function Component:getPosition()
	return self.x, self.y
end

function Component:getSize()
	return self.width, self.height
end

function Component:setVisible(visible)
	self.visible = visible
end

function Component:isVisible()
	return self.visible
end

function Component:setLayer(layerName)
	self.layer = layerName
end

function Component:setRelativeRect(rx, ry, rw, rh)
	-- Relative rectangle to current canvas: all values normalized in [0, 1].
	self.relativeRect = { rx = rx, ry = ry, rw = rw, rh = rh }
end

function Component:setLayoutCallback(callback)
	self.layoutCallback = callback
end

function Component:applyLayout(canvasW, canvasH)
	if self.relativeRect then
		local rx = clamp(self.relativeRect.rx, 0, 1)
		local ry = clamp(self.relativeRect.ry, 0, 1)
		local rw = clamp(self.relativeRect.rw, 0, 1)
		local rh = clamp(self.relativeRect.rh, 0, 1)
		self.x = round(canvasW * rx)
		self.y = round(canvasH * ry)
		self.width = math.max(0, round(canvasW * rw))
		self.height = math.max(0, round(canvasH * rh))
	end

	if self.layoutCallback then
		self.layoutCallback(self, canvasW, canvasH)
	end

	if self.children then
		for _, child in ipairs(self.children) do
			if child.applyLayout then
				child:applyLayout(canvasW, canvasH)
			end
		end
	end
end

function Component:pointInBounds(px, py)
	return px >= self.x and px < (self.x + self.width) and py >= self.y and py < (self.y + self.height)
end

function Component:draw()
end

function Component:update(_deltaTime)
end

-- Returns true if the event was consumed (stops further propagation).
function Component:onMouseEvent(_e)
	return false
end

GUI.Component = Component

-- ============================================================================
-- Panel: Container component for grouping other elements
-- ============================================================================

local Panel = setmetatable({}, { __index = Component })
Panel.__index = Panel

function Panel.new(x, y, width, height, title)
	local self = setmetatable(Component.new(x, y, width, height), Panel)
	self.title = title or ""
	self.backgroundColor = { r = 0.1, g = 0.12, b = 0.18, a = 0.8 }
	self.borderColor = { r = 0.4, g = 0.6, b = 0.95, a = 0.9 }
	self.titleColor = { r = 0.8, g = 0.9, b = 1.0, a = 1.0 }
	self.titleScale = 1
	self.children = {}
	self.layer = "panels"
	return self
end

function Panel:setBackgroundColor(r, g, b, a)
	self.backgroundColor = { r = r, g = g, b = b, a = a }
end

function Panel:setBorderColor(r, g, b, a)
	self.borderColor = { r = r, g = g, b = b, a = a }
end

function Panel:setTitleScale(scale)
	self.titleScale = math.max(1, math.floor(scale or 1))
end

function Panel:addChild(component)
	table.insert(self.children, component)
	if self.manager and component.setManager then
		component:setManager(self.manager)
	elseif self.manager then
		component.manager = self.manager
	end
end

function Panel:removeChild(component)
	for i, child in ipairs(self.children) do
		if child == component then
			table.remove(self.children, i)
			break
		end
	end
end

function Panel:update(deltaTime)
	for _, child in ipairs(self.children) do
		if child.update then
			child:update(deltaTime)
		end
	end
end

function Panel:onMouseEvent(e)
	-- Forward to children back-to-front so topmost child gets first pick.
	for i = #self.children, 1, -1 do
		local child = self.children[i]
		if child:isVisible() and child.onMouseEvent then
			if child:onMouseEvent(e) then
				return true
			end
		end
	end
	return false
end

function Panel:draw()
	if not self.manager then
		return
	end

	gfx2d.setLayer(self.layer)
	gfx2d.rect(
		self.x,
		self.y,
		self.width,
		self.height,
		self.backgroundColor.r,
		self.backgroundColor.g,
		self.backgroundColor.b,
		self.backgroundColor.a,
		true
	)

	gfx2d.rect(
		self.x,
		self.y,
		self.width,
		self.height,
		self.borderColor.r,
		self.borderColor.g,
		self.borderColor.b,
		self.borderColor.a,
		false
	)

	if self.title ~= "" then
		gfx2d.text(
			self.x + 3,
			self.y + 2,
			self.title,
			self.titleColor.r,
			self.titleColor.g,
			self.titleColor.b,
			self.titleColor.a,
			self.titleScale
		)
	end

	for _, child in ipairs(self.children) do
		if child:isVisible() then
			child:draw()
		end
	end
end

GUI.Panel = Panel

-- ============================================================================
-- Button: Interactive button component
-- ============================================================================

local Button = setmetatable({}, { __index = Component })
Button.__index = Button

function Button.new(x, y, width, height, label, onPress)
	local self = setmetatable(Component.new(x, y, width, height), Button)
	self.label = label or "Button"
	self.onPress = onPress or function()
	end
	self.hovered = false
	self.pressed = false
	self.enabled = true

	self.backgroundColor = { r = 0.15, g = 0.5, b = 0.8, a = 1.0 }
	self.hoverColor = { r = 0.2, g = 0.6, b = 0.95, a = 1.0 }
	self.pressedColor = { r = 0.1, g = 0.4, b = 0.7, a = 1.0 }
	self.disabledColor = { r = 0.15, g = 0.18, b = 0.22, a = 1.0 }
	self.borderColor = { r = 0.6, g = 0.8, b = 1.0, a = 0.9 }
	self.disabledBorderColor = { r = 0.35, g = 0.42, b = 0.5, a = 0.75 }
	self.textColor = { r = 1.0, g = 1.0, b = 1.0, a = 1.0 }
	self.disabledTextColor = { r = 0.75, g = 0.78, b = 0.82, a = 0.95 }
	self.labelScale = 1
	self.layer = "components"

	return self
end

function Button:setLabel(label)
	self.label = label
end

function Button:setOnPress(callback)
	self.onPress = callback or function()
	end
end

function Button:setNormalColor(r, g, b, a)
	self.backgroundColor = { r = r, g = g, b = b, a = a or 1.0 }
end

function Button:setHoverColor(r, g, b, a)
	self.hoverColor = { r = r, g = g, b = b, a = a or 1.0 }
end

function Button:setPressedColor(r, g, b, a)
	self.pressedColor = { r = r, g = g, b = b, a = a or 1.0 }
end

function Button:setDisabledColor(r, g, b, a)
	self.disabledColor = { r = r, g = g, b = b, a = a or 1.0 }
end

function Button:setDisabledBorderColor(r, g, b, a)
	self.disabledBorderColor = { r = r, g = g, b = b, a = a }
end

function Button:setDisabledTextColor(r, g, b, a)
	self.disabledTextColor = { r = r, g = g, b = b, a = a }
end

function Button:setLabelScale(scale)
	self.labelScale = math.max(1, math.floor(scale or 1))
end

function Button:setEnabled(enabled)
	self.enabled = not not enabled
	if not self.enabled then
		self.hovered = false
		self.pressed = false
	end
end

function Button:update(_deltaTime)
end

local function _drawButtonScanlineFill(x, y, width, height, color)
	local w = math.max(0, math.floor(width or 0))
	local h = math.max(0, math.floor(height or 0))
	if w <= 0 or h <= 0 then
		return
	end

	local x1 = x
	local x2 = x + w - 1
	for row = 0, h - 1 do
		local yRow = y + row
		gfx2d.line(x1, yRow, x2, yRow, color.r, color.g, color.b, color.a, 1.0)
	end
end

local function _buttonContainsPoint(btn, px, py, canvasHeight)
	if px == nil or py == nil then
		return false
	end

	-- Primary: top-left origin hit-test (expected coordinate space).
	if btn:pointInBounds(px, py) then
		return true
	end

	-- Fallback: some runtime/input paths report Y from bottom origin.
	if canvasHeight and canvasHeight > 0 then
		local flippedY = (canvasHeight - 1) - py
		if btn:pointInBounds(px, flippedY) then
			return true
		end
	end

	return false
end

function Button:onMouseEvent(e)
	if not self.enabled then
		self.hovered = false
		self.pressed = false
		return false
	end

	if e.insideCanvas == false then
		self.hovered = false
		self.pressed = false
		return false
	end

	local px = tonumber(e.uiX or e.x)
	local py = tonumber(e.uiY or e.y)
	if px == nil or py == nil then
		return false
	end

	local canvasHeight = self.manager and self.manager.height or nil
	local inside = _buttonContainsPoint(self, px, py, canvasHeight)
	local button = e.button or "left"

	if e.pressed and button == "left" then
		if inside then
			self.pressed = true
			self.hovered = true
			return true
		end
	elseif e.released and button == "left" then
		local wasPressed = self.pressed
		self.pressed = false
		self.hovered = inside
		if wasPressed and inside then
			self.onPress()
			return true
		end
	else
		-- Continuous hover tracking on mouse move / drag.
		self.hovered = inside
		if not inside then
			self.pressed = false
		end
	end

	return false
end

function Button:draw()
	if not self.manager then
		return
	end

	gfx2d.setLayer(self.layer)

	local color = self.backgroundColor
	local ms = self.manager.mouseState
	if self.enabled and ms and ms.hasPosition then
		local canvasHeight = self.manager and self.manager.height or nil
		local inside = ms.insideCanvas and _buttonContainsPoint(self, ms.uiX, ms.uiY, canvasHeight)
		self.hovered = inside
		if not inside and not ms.leftDown then
			self.pressed = false
		end
	end

	if not self.enabled then
		color = self.disabledColor
	elseif self.pressed then
		color = self.pressedColor
	elseif self.hovered then
		color = self.hoverColor
	end

	local borderColor = self.borderColor
	local textColor = self.textColor
	if not self.enabled then
		borderColor = self.disabledBorderColor
		textColor = self.disabledTextColor
	end

	-- Some engine builds still fail to rasterize filled rects reliably.
	-- Use scanline fill so button bodies are always visibly filled.
	_drawButtonScanlineFill(self.x, self.y, self.width, self.height, color)
	gfx2d.rect(
		self.x,
		self.y,
		self.width,
		self.height,
		borderColor.r,
		borderColor.g,
		borderColor.b,
		borderColor.a,
		false
	)

	-- Use simple text draw path for broad runtime compatibility.
	local scale = math.max(1, math.floor(self.labelScale or 1))
	-- Approximate pixel height of a character at this scale (standard 8px font).
	local charPixH = scale * 9
	local hPad = math.max(2, scale * 2)
	local maxChars = math.max(1, math.floor((self.width - hPad * 2) / scale))
	local label = self.label or ""
	if #label > maxChars then
		if maxChars <= 1 then
			label = "~"
		else
			label = label:sub(1, maxChars - 1) .. "~"
		end
	end

	local textX = self.x + hPad
	local textY = self.y + math.max(0, math.floor((self.height - charPixH) * 0.5))
	gfx2d.text(
		textX,
		textY,
		label,
		textColor.r,
		textColor.g,
		textColor.b,
		textColor.a,
		scale
	)
end

GUI.Button = Button

-- ============================================================================
-- Text: Simple text display component
-- ============================================================================

local Text = setmetatable({}, { __index = Component })
Text.__index = Text

function Text.new(x, y, content)
	local self = setmetatable(Component.new(x, y, 0, 1), Text)
	self.content = content or ""
	self.color = { r = 1.0, g = 1.0, b = 1.0, a = 1.0 }
	self.scale = 1
	self.maxWidth = nil
	self.maxHeight = nil
	self.align = "left"
	self.wrap = false
	self.layer = "components"

	self:updateSize()
	return self
end

function Text:setText(content)
	self.content = content or ""
	self:updateSize()
end

function Text:setColor(r, g, b, a)
	self.color = { r = r, g = g, b = b, a = a }
end

function Text:setScale(scale)
	self.scale = math.max(1, math.floor(scale or 1))
	self:updateSize()
end

function Text:setLayout(maxWidth, maxHeight, align, wrap)
	self.maxWidth = maxWidth
	self.maxHeight = maxHeight
	self.align = align or "left"
	self.wrap = wrap == true
end

function Text:updateSize()
	local lines = 1
	for _ in string.gmatch(self.content, "\n") do
		lines = lines + 1
	end
	self.width = string.len(self.content) * self.scale
	self.height = lines * self.scale
end

function Text:draw()
	if not self.manager then
		return
	end

	gfx2d.setLayer(self.layer)

	local x = type(self.x) == "number" and self.x or 0
	local y = type(self.y) == "number" and self.y or 0
	local scale = math.max(1, math.floor(type(self.scale) == "number" and self.scale or 1))

	local color = self.color or {}
	local r = type(color.r) == "number" and color.r or 1
	local g = type(color.g) == "number" and color.g or 1
	local b = type(color.b) == "number" and color.b or 1
	local a = type(color.a) == "number" and color.a or 1

	local maxWidth = type(self.maxWidth) == "number" and self.maxWidth or nil
	local maxHeight = type(self.maxHeight) == "number" and self.maxHeight or nil

	if maxWidth and maxHeight then
		gfx2d.text(
			x,
			y,
			self.content,
			r,
			g,
			b,
			a,
			scale,
			maxWidth,
			maxHeight,
			self.align,
			self.wrap
		)
	else
		gfx2d.text(
			x,
			y,
			self.content,
			r,
			g,
			b,
			a,
			scale
		)
	end
end

GUI.Text = Text

-- ============================================================================
-- HorizontalLayout: Arranges children left-to-right
-- ============================================================================

local HorizontalLayout = setmetatable({}, { __index = Component })
HorizontalLayout.__index = HorizontalLayout

function HorizontalLayout.new(x, y, height, spacing)
	local self = setmetatable(Component.new(x, y, 0, height or 0), HorizontalLayout)
	self.spacing = spacing or 2
	self.children = {}
	self.layer = "components"
	return self
end

function HorizontalLayout:addChild(component)
	table.insert(self.children, component)
	if self.manager and component.setManager then
		component:setManager(self.manager)
	elseif self.manager then
		component.manager = self.manager
	end
	self:recalculateLayout()
end

function HorizontalLayout:removeChild(component)
	for i, child in ipairs(self.children) do
		if child == component then
			table.remove(self.children, i)
			self:recalculateLayout()
			break
		end
	end
end

function HorizontalLayout:recalculateLayout()
	local currentX = self.x
	for _, child in ipairs(self.children) do
		child:setPosition(currentX, self.y)
		currentX = currentX + child.width + self.spacing
	end
	self.width = math.max(0, currentX - self.x - self.spacing)
end

function HorizontalLayout:setPosition(x, y)
	Component.setPosition(self, x, y)
	self:recalculateLayout()
end

function HorizontalLayout:update(deltaTime)
	for _, child in ipairs(self.children) do
		if child.update then
			child:update(deltaTime)
		end
	end
end

function HorizontalLayout:onMouseEvent(e)
	for i = #self.children, 1, -1 do
		local child = self.children[i]
		if child:isVisible() and child.onMouseEvent then
			if child:onMouseEvent(e) then
				return true
			end
		end
	end
	return false
end

function HorizontalLayout:draw()
	for _, child in ipairs(self.children) do
		if child:isVisible() then
			child:draw()
		end
	end
end

GUI.HorizontalLayout = HorizontalLayout

-- ============================================================================
-- VerticalLayout: Arranges children top-to-bottom
-- ============================================================================

local VerticalLayout = setmetatable({}, { __index = Component })
VerticalLayout.__index = VerticalLayout

function VerticalLayout.new(x, y, width, spacing)
	local self = setmetatable(Component.new(x, y, width or 0, 0), VerticalLayout)
	self.spacing = spacing or 1
	self.children = {}
	self.layer = "components"
	return self
end

function VerticalLayout:addChild(component)
	table.insert(self.children, component)
	if self.manager and component.setManager then
		component:setManager(self.manager)
	elseif self.manager then
		component.manager = self.manager
	end
	self:recalculateLayout()
end

function VerticalLayout:removeChild(component)
	for i, child in ipairs(self.children) do
		if child == component then
			table.remove(self.children, i)
			self:recalculateLayout()
			break
		end
	end
end

function VerticalLayout:recalculateLayout()
	local currentY = self.y
	for _, child in ipairs(self.children) do
		child:setPosition(self.x, currentY)
		currentY = currentY + child.height + self.spacing
	end
	self.height = math.max(0, currentY - self.y - self.spacing)
end

function VerticalLayout:setPosition(x, y)
	Component.setPosition(self, x, y)
	self:recalculateLayout()
end

function VerticalLayout:update(deltaTime)
	for _, child in ipairs(self.children) do
		if child.update then
			child:update(deltaTime)
		end
	end
end

function VerticalLayout:onMouseEvent(e)
	for i = #self.children, 1, -1 do
		local child = self.children[i]
		if child:isVisible() and child.onMouseEvent then
			if child:onMouseEvent(e) then
				return true
			end
		end
	end
	return false
end

function VerticalLayout:draw()
	for _, child in ipairs(self.children) do
		if child:isVisible() then
			child:draw()
		end
	end
end

GUI.VerticalLayout = VerticalLayout

-- ============================================================================
-- ModalDialog: Simple centered modal dialog overlay with message + buttons
-- ============================================================================

local ModalDialog = setmetatable({}, { __index = Panel })
ModalDialog.__index = ModalDialog

function ModalDialog.new(title, message)
	local self = setmetatable(Panel.new(0, 0, 40, 8, title or ""), ModalDialog)
	-- Render on overlay layer so it appears above other UI
	self.layer = "overlay"
	self.messageText = Text.new(0, 0, message or "")
	self.messageText:setColor(0.95, 0.95, 1.0, 1.0)
	self.messageText:setScale(1)
	self.messageText:setLayout(0, 0, "left", true)
	self:addChild(self.messageText)

	self.buttonLayout = HorizontalLayout.new(0, 0, 1, 3)
	self:addChild(self.buttonLayout)

	-- Allow external metadata (optional)
	self.data = {}

	return self
end

function ModalDialog:setMessage(msg)
	if self.messageText then
		self.messageText:setText(msg or "")
	end
end

function ModalDialog:addButton(label, callback)
	local b = Button.new(0, 0, 10, 3, label or "", callback)
	self.buttonLayout:addChild(b)
	return b
end

function ModalDialog:applyLayout(canvasW, canvasH)
	-- Center dialog and size relative to canvas
	local w = math.max(30, math.floor(canvasW * 0.5))
	local h = math.max(6, math.floor(canvasH * 0.22))
	local x = math.floor((canvasW - w) * 0.5)
	local y = math.floor((canvasH - h) * 0.5)
	self:setPosition(x, y)
	self:setSize(w, h)

	local pad = 3
	-- message occupies most of body above buttons
	local msgW = math.max(1, self.width - pad * 2)
	local msgH = math.max(1, self.height - (pad * 2) - 3)
	self.messageText:setPosition(self.x + pad, self.y + pad)
	self.messageText:setLayout(msgW, msgH, "left", true)

	-- place buttons near bottom
	local btnY = self.y + self.height - pad - 2
	self.buttonLayout:setPosition(self.x + pad, btnY)

	-- size buttons evenly
	local btnCount = #self.buttonLayout.children
	if btnCount > 0 then
		local totalSpacing = (btnCount - 1) * (self.buttonLayout.spacing or 0)
		local btnW = math.max(6, math.floor((self.width - pad * 2 - totalSpacing) / btnCount))
		for _, btn in ipairs(self.buttonLayout.children) do
			btn:setSize(btnW, 3)
		end
		self.buttonLayout:recalculateLayout()
	end
end

function ModalDialog:onMouseEvent(e)
	-- Always consume mouse events (modal). Forward to children so inner
	-- buttons remain interactive.
	if Panel.onMouseEvent(self, e) then
		return true
	end
	return true
end

GUI.ModalDialog = ModalDialog

return GUI
