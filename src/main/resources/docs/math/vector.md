# Vector API

`LuaVec3i` (Vec3i wrapper) is used for integer grid coordinates such as block/sector/system positions.

`LuaVec3f` (Vec3f wrapper) is used for world-space positions with fractional precision.

## LuaVec3i Reference

## Reference

- `getX()` / `getY()` / `getZ()`
Returns the corresponding integer component.

- `setX(x: Integer)` / `setY(y: Integer)` / `setZ(z: Integer)`
Sets the corresponding component.

- `add(vec: LuaVec3i)`
Adds `vec` component-wise. Mutates and returns `self`.

- `sub(vec: LuaVec3i)`
Subtracts `vec` component-wise. Mutates and returns `self`.

- `mul(vec: LuaVec3i)`
Multiplies component-wise by `vec`. Mutates and returns `self`.

- `div(vec: LuaVec3i)`
Divides component-wise by `vec`. Mutates and returns `self`.

- `scale(factor: Number)`
Multiplies all components by `factor`. Mutates and returns `self`.

- `absolute()`
Makes all components non-negative. Mutates and returns `self`.

- `negate()`
Negates all components. Mutates and returns `self`.

- `size()`
Returns the Euclidean length as a `Double`.

- `toVec3f()`
Creates a `LuaVec3f` copy of the vector.

## LuaVec3f Reference

- `getX()` / `getY()` / `getZ()`
Returns the corresponding float component.

- `setX(x: Number)` / `setY(y: Number)` / `setZ(z: Number)`
Sets the corresponding component.

- `add(vec: LuaVec3f)`
Adds `vec` component-wise. Mutates and returns `self`.

- `sub(vec: LuaVec3f)`
Subtracts `vec` component-wise. Mutates and returns `self`.

- `mul(vec: LuaVec3f)`
Multiplies component-wise by `vec`. Mutates and returns `self`.

- `div(vec: LuaVec3f)`
Divides component-wise by `vec`. Mutates and returns `self`.

- `scale(factor: Number)`
Multiplies all components by `factor`. Mutates and returns `self`.

- `absolute()`
Makes all components non-negative. Mutates and returns `self`.

- `negate()`
Negates all components. Mutates and returns `self`.

- `size()`
Returns the Euclidean length as a `Double`.

- `toVec3i()`
Creates a `LuaVec3i` copy of the vector by truncating each component toward zero.

## Notes

- All math operations mutate the vector in place and return `self` for chaining.
- Use `vec3i(x, y, z)` to construct a new vector.
- Use `vec3f(x, y, z)` for world-space vectors.
- `LuaVec3i` can also be converted to `LuaVec3f` with `toVec3f()`.
- `LuaVec3f` can also be converted to `LuaVec3i` with `toVec3i()`.
