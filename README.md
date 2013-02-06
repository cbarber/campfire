# Campfire - PvP Protection

## About
Campfire is a PvP protection plugin for Minecraft. It allows new players to get on their feet before being attacked by 
more established players. While a player is under Campfire's PvP protection, they are immune to all PvP related damage.  

The PvP protection does not affect normal PvE damage such as fall damage, mobs, drowning, etc.

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
* **Campfire.Immune** - Grants immunity to Campfire.  
Players with this flag will not be protected, and can attack those who are protected.
