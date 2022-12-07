.. LuaMade documentation master file, created by
   sphinx-quickstart on Tue Dec  6 13:32:53 2022.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

LuaMade Documentation 1.0.0
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
- Issue Tracker: https://github.com/garretreichenbach/Logiscript/issues
- Source Code: https://github.com/garretreichenbach/Logiscript

For Modders
===================================
To add LuaMade support to your mod, add the following to your mod's ``mod.json`` file:
 "dependencies": [
	-1,
    SMD_ID
  ],
Then add the Jar as a library to your project and that's it!

Adding new Lua functions and objects is easy, just create a class that extends ``LuaMadeUserData`` and register it in ``APIManager.registerClass()`` during your mod's onEnable().

You can also extend the functionality of existing LuaMadeUserData classes included with LuaMade by using ``LuaMadeAPIManager.addMethod()``.