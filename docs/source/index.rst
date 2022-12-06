.. LuaMade documentation master file, created by
   sphinx-quickstart on Tue Dec  6 13:32:53 2022.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

LuaMade Documentation $project.version
===================================

.. toctree::
   :maxdepth: 2
   :caption: Contents:

Description
===================================
LuaMade is a StarMade mod that adds scriptable Lua Computers to the game. They can be programmed, auto-run, fetch scripts from the web and more.

Authors
-----------------------------------
- TheDerpGamer (TheDerpGamer#0027)
- LupoCani (lupoCani#6558)

Misc Info
-----------------------------------
- Issue Tracker: github.com/$project/$project/issues
- Source Code: github.com/$project/$project

For Modders
===================================
To add LuaMade support to your mod, add the following to your mod's ``mod.json`` file:
 "dependencies": [
	-1,
    SMD_ID
  ],
Then add the Jar as a library to your project and that's it!
Adding new Lua functions and objects is easy, just create a class that extends ``LuaMadeAPIObject`` and register it's definition in onEnable() using LuaModAPI.addDefinition(<LuaCodeToGetObjectInstance>, <ObjectClass>).
For example, to add a new function ``printStatus`` to a new Lua Object ``CustomSystem``, you would do the following:
```
public class CustomSystem extends LuaMadeAPIObject {
    
}
```
Then the following code would be added to onEnable():
```
LuaModAPI.addDefinition("getMySystem()", Entity.class, CustomSystem.class);
```
