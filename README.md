# Campfire - PvP Protection

## About
Campfire is a PvP Protection plugin designed to give new players a chance to get their footing before being attacked  
by more well-equipped players. While under PvP Protection,players can't take or receive PvP damage. Players' PvP protection  
expires after a configurable amount of time, or players may opt to terminate their protection early.  

Campfire does protect against any kind of environmental damage such as mobs, fall damage, and drowning.

## Options
The following options are available in the config.yml file:
* **Duration** *(integer)*   
The length of time players' PvP protection will last in seconds
* **ResetOnDeath** *(boolean)*   
Whether the players' protection starts over if they die. 
* **WorldGuardPause** *(boolean)*  
Pause the protection count-down when the player is in a WorldGuard region that has the AntiPvP flag or Invincible flag
* **FireRadius** *(integer)*  
Radius in blocks players are not allowed to start fires or place lava near protected players.  
This prevents indirect killing from fires or lava. Set to 0 to disable.

## Dependencies
* **WorldGuard** *(optional)*
* **WorldEdit** *(optional)*

## Permissions
* **campfire.immune** - Grants immunity to Campfire.  
Players with this permission will not be protected, and can attack those who are protected.
* **campfire.reset** - Allows player to use the reset command.  
Players with this permission can reset other players' protection status.
* **campfire.reload** - Allows player to use the reload command.  
Players with this permission can reload the plugin.

## License
Copyright (c) 2013 Matt Fields

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
