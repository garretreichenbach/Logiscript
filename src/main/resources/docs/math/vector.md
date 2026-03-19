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

## Notes

- All math operations mutate the vector in place and return `self` for chaining.
- Use `vec3i(x, y, z)` to construct a new vector.
- Use `vec3f(x, y, z)` for world-space vectors.
