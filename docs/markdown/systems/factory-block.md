# Factory Block

Factory blocks are wrapped automatically when using `peripheral.wrap` or `peripheral.getRelative`. They extend `Block`, so all standard block methods are available.

## Typical usage

```lua
local factory = peripheral.wrapRelative("front", "factory")

if factory == nil then
  print("No factory found")
  return
end

print("Type:", factory:getFactoryType())
print("Active:", factory:isActive())
print("Producing:", factory:isProducing())
print("Progress:", factory:getProgress())

local recipe = factory:getRecipe()
if recipe ~= nil then
  local inputs = recipe:getInputs()
  for i = 1, #inputs do
    print("Needs:", inputs[i]:getCount(), "x", inputs[i]:getId())
  end
  local outputs = recipe:getOutputs()
  for i = 1, #outputs do
    print("Produces:", outputs[i]:getCount(), "x", outputs[i]:getId())
  end
end

-- Start producing a specific block type
factory:setProductionTarget(477)  -- block type ID
factory:setProductionLimit(64)    -- stop after 64 items
factory:setActive(true)
```

## Factory type names

When wrapping explicitly with `peripheral.wrap(block, asType)`:

- `"factory"` — any factory type (auto-selects subtype)
- `"basicfactory"` / `"basic_factory"`
- `"standardfactory"` / `"standard_factory"`
- `"advancedfactory"` / `"advanced_factory"`
- `"microassembler"` / `"micro_assembler"`
- `"capsuleassembler"` / `"capsule_assembler"`

## Reference

### Status

- `getFactoryType()`
Returns a string identifying the factory sub-type: `"basic"`, `"standard"`, `"advanced"`, `"micro_assembler"`, or `"capsule_assembler"`.

- `isProducing()`
Returns `true` when the factory is actively consuming power to run a recipe.

- `getProgress()`
Returns the current cycle progress as a value between `0.0` and `1.0`, based on the power charge level. Returns `0.0` when idle.

- `getPowerLevel()`
Returns the raw power charge level between `0.0` and `1.0`. Rises toward `1.0` over each cycle and resets after each step fires.

- `getBakeTime()`
Returns the duration of one production cycle in milliseconds, accounting for enhancer modifiers.

- `getCapability()`
Returns the factory's capability level (1 + connected enhancer count). Higher capability produces more items per cycle.

- `getRecipe()`
Returns the currently active `FactoryRecipe`, or `nil` if no recipe is loaded.

### Control

- `setActive(active<Boolean>)`
Starts (`true`) or stops (`false`) production. Inherited from `Block`.

- `setProductionTarget(blockType<Integer>)`
Sets the block type ID this factory should produce. The factory will use the built-in recipe associated with that block type. Has no effect on micro assemblers or capsule assemblers (fixed recipes). Use `clearProductionTarget()` to unset.

- `getProductionTarget()`
Returns the block type ID set as the production target, or `nil` if none is set.

- `clearProductionTarget()`
Removes the production target, returning the factory to recipe-slot-driven mode.

- `setProductionLimit(limit<Integer>)`
Caps the total number of items this factory will produce. Set to `0` for unlimited production.

- `getProductionLimit()`
Returns the current production limit, or `0` if unlimited.

- `getInventory()`
Returns the factory's own `Inventory`, which holds ingredients and output items. Inherited from `Block`.

## FactoryRecipe

Returned by `getRecipe()`.

- `getBakeTime()`
Returns the base bake time for this recipe in seconds.

- `getProductCount()`
Returns the number of product chains in this recipe (usually `1`).

- `getInputs()`
Returns an `ItemStack[]` of required input materials for the first product chain.

- `getInputs(chainIndex<Integer>)`
Returns inputs for the specified product chain index (0-based).

- `getOutputs()`
Returns an `ItemStack[]` of produced output items for the first product chain.

- `getOutputs(chainIndex<Integer>)`
Returns outputs for the specified product chain index (0-based).

## Notes

- `setActive(false)` immediately halts production at the next cycle boundary.
- `setProductionTarget` only works for macro factories (basic, standard, advanced).
- Micro assemblers and capsule assemblers have fixed recipes that cannot be overridden.
- The factory's inventory (slot 0) can also hold a recipe meta item, which takes precedence over `setProductionTarget` when present.
- Production limit counts the total produced output, not the number of cycles.
