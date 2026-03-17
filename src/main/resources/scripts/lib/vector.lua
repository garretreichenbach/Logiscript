local vector = {}

local vec3 = {}
vec3.__index = vec3

local function newVec3(x, y, z)
    return setmetatable({ x = x or 0, y = y or 0, z = z or 0 }, vec3)
end

function vector.new(x, y, z)
    return newVec3(x, y, z)
end

function vector.copy(value)
    if value == nil then
        return newVec3(0, 0, 0)
    end
    return newVec3(value.x or value.getX and value:getX() or 0, value.y or value.getY and value:getY() or 0, value.z or value.getZ and value:getZ() or 0)
end

function vec3:add(other)
    local rhs = vector.copy(other)
    self.x = self.x + rhs.x
    self.y = self.y + rhs.y
    self.z = self.z + rhs.z
    return self
end

function vec3:sub(other)
    local rhs = vector.copy(other)
    self.x = self.x - rhs.x
    self.y = self.y - rhs.y
    self.z = self.z - rhs.z
    return self
end

function vec3:scale(amount)
    self.x = self.x * amount
    self.y = self.y * amount
    self.z = self.z * amount
    return self
end

function vec3:length()
    return math.sqrt(self.x * self.x + self.y * self.y + self.z * self.z)
end

function vec3:abs()
    self.x = math.abs(self.x)
    self.y = math.abs(self.y)
    self.z = math.abs(self.z)
    return self
end

function vec3:unpack()
    return self.x, self.y, self.z
end

return vector
