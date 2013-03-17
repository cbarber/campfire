package com.bitdagger.campfire;

import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.plugin.Plugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;

/**
 * Event listener
 * @author bitdagger
 *
 */
public class EventListener implements Listener
{
	/**
	 * Reference to the parent plugin
	 */
	private Campfire _plugin;
	
	/**
	 * Reference to the WorldGuard plugin
	 */
	private WorldGuardPlugin _worldguard;
	
	/**
	 * Constructor
	 * Register the references to the parent plugin and WorldGuard plugin
	 * @param plugin Parent plugin
	 * @param wg WorldGuard plugin
	 */
	public EventListener( Campfire plugin, WorldGuardPlugin wg )
	{
		this._plugin = plugin;
		this._worldguard = wg;
	}
	
	
	/**
	 * Search for the WorldGuard plugin if we want it and mark the reference to it
	 * @param e
	 */
	@EventHandler( priority = EventPriority.LOW )
	public void onPluginLoad( PluginEnableEvent e )
	{
		//-- If we're not going to be using WorldGuard, don't bother looking for it
		if ( !this._plugin.getConfig().getBoolean( "WorldGuardPause" ) ) return;
		
		//-- If we already have a reference, great, we're done
		if ( this._worldguard != null ) return;
		
		//-- Check if the loaded plugin is WorldGuard, and if so make the reference
		Plugin p = e.getPlugin();
		if ( !p.getDescription().getName().equals( "WorldGuard" ) ) return;
		if ( !( p instanceof WorldGuardPlugin ) ) return;
		System.out.println( "[Campfire] Found WorldGuard!" );
		this._worldguard = ( WorldGuardPlugin ) p; 
	}
	
	/**
	 * Check if the WorldGuard plugin is disabled while we are using it
	 * @param e
	 */
	@EventHandler( priority = EventPriority.LOW )
	public void onPluginUnload( PluginDisableEvent e )
	{
		//-- If it's already null, nothing to do
		if ( this._worldguard == null ) return;
		
		//-- Check if the disabled plugin was WorldGuard, and if so delete the reference
		Plugin p = e.getPlugin(); 
		if ( !p.getDescription().getName().equals( "WorldGuard" ) ) return;
		if ( !( p instanceof WorldGuardPlugin ) ) return;
		System.out.println( "[Campfire] WorldGuard disabled!" );
		this._worldguard = null; 
	}	
	
	/**
	 * Update a player's WorldGuard protected status
	 * @param e
	 */
	@EventHandler( priority = EventPriority.LOW )
	public void onMove( PlayerMoveEvent e )
	{
		//-- Check that we are using WorldGuard areas, we have a valid reference, and the event wasn't cancelled
		if ( !this._plugin.getConfig().getBoolean( "WorldGuardPause" ) ) return;
		if ( this._worldguard == null ) return;
		if ( e.isCancelled() ) return;
		
		//-- Ignore OPs and players who have the campfire immunity flag
		Player player = e.getPlayer();
		if ( player.isOp() ) return;
		if ( player.hasPermission( "campfire.immune" ) ) return;
		
		//-- Ignore players not under protection
		DataManager manager = this._plugin.getDataManager();
		String playerName = player.getName();
		try {
			if ( !manager.playerProtected( playerName ) ) return;
		} catch ( CampfireDataException ex ) {
			ex.printStackTrace();
			return;
		}
		
		//-- Check if they are in NoPvP or Invincible regions
		ApplicableRegionSet region = this._worldguard.getRegionManager( player.getWorld() ).getApplicableRegions( player.getLocation() );
		boolean isProtected = !region.allows( DefaultFlag.PVP ) || region.allows( DefaultFlag.INVINCIBILITY );
		
		//-- Update the data manager with the current protection state
		try {
			boolean updated = manager.setPlayerWGProtected( isProtected, playerName );
			if ( !updated ) return; // The state didn't change, so don't spam messages
		} catch ( CampfireDataException ex ) {
			ex.printStackTrace();
			return;
		}
		
		//-- Send the user a message since protection state was updated
		if ( isProtected )
		{
			player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] Entering protected zone." );
			player.sendMessage( "Protection timer paused!" );
		} else {
			player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] Leaving protected zone." );
			player.sendMessage( "Protection timer resumed!" );
		}
	}
	
	/**
	 * Prevent PvP damage for players under protection
	 * @param e
	 */
	@EventHandler( priority = EventPriority.HIGH )
	public void onEntityDamageByEntity( EntityDamageByEntityEvent e )
	{
		//-- Make sure the entity is a player
		Player target = null;
		if ( e.getEntity() instanceof Player ) target = ( Player ) e.getEntity();
		if ( target == null ) return;
		
		//-- Ignore OPs and players who have the campfire immunity flag
		if ( target.isOp() ) return;
		if ( target.hasPermission( "campfire.immune" ) ) return;
		
		//-- Get their protected status
		String playerName = target.getName();
		DataManager manager = this._plugin.getDataManager();
		boolean isProtected = false;
		try {
			isProtected = manager.playerProtected( playerName );
		} catch( CampfireDataException ex ) {
			//-- They aren't in our list of players. Odd.
		}
		
		//-- Check if the player is being hurt by TnT
		Entity attackerEntity = e.getDamager(); 
		if ( attackerEntity instanceof TNTPrimed )
		{
			// TNT damage, prevent it if the player is protected
			if ( isProtected )
			{
				e.setCancelled( true );
				return;
			}
		}
		
		//-- Check if the player is being hurt by a falling anvil
		if ( attackerEntity instanceof org.bukkit.entity.FallingBlock )
		{
			// Anvil damage, prevent it if the player is protected
			if ( isProtected )
			{
				e.setCancelled( true );
				return;
			}
		}
				
		//-- Check if the attacker was a projectile shot by another player ( Arrow, egg, potion, etc )
		if ( attackerEntity instanceof Projectile ) attackerEntity = ((Projectile) attackerEntity).getShooter();
		
		//-- If it wasn't a player at this point, we don't care
		if ( !( attackerEntity instanceof Player ) ) return;
		Player attacker = (Player) attackerEntity;
		
		//-- Ignore self attacks, don't let them kill themselves
		if ( attacker.equals( target ) )
		{
			e.setCancelled( true );
			return;
		}
		
		//-- Ignore OPs and players who have the campfire immunity flag
		if ( attacker.isOp() ) return;
		if ( attacker.hasPermission( "campfire.immune" ) ) return;
		
		//-- If the attacker is under protection, cancel
		try {
			if ( manager.playerProtected( attacker.getName() ) )
			{
				attacker.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.GRAY + "] " + ChatColor.RED + "You cannot PvP at this time!" );
				e.setCancelled( true );
				return;
			}
		} catch( CampfireDataException ex ) {
			ex.printStackTrace();
			return;
		}
		
		//-- If the victim is under protection, cancel
		if ( isProtected )
		{
			attacker.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.GRAY + "] " + ChatColor.RED + "Player is protected from PvP!" );
			e.setCancelled( true );
			return;
		}
	}
	
	/**
	 * Add players to the data manager if they are new
	 * Let returning players know how much time they have left, if any
	 * @param e
	 */
	@EventHandler( priority = EventPriority.HIGH )
	public void onPlayerJoin( PlayerJoinEvent e  )
	{
		//-- Ignore OPs and players who have the campfire immunity flag
		Player player = e.getPlayer();
		if ( player.isOp() ) return;
		if ( player.hasPermission( "campfire.immune" ) ) return;
		
		//-- Add them to the data manager
		String playerName = player.getName();
		DataManager manager = this._plugin.getDataManager();
		boolean added = manager.addPlayer( playerName );
		
		//-- Send them a message
		if ( added )
		{
			player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] Starting protection!" );
			player.sendMessage( ChatColor.GRAY + "Type '/campfire' for info on PvP Protection" );
		} else {
			try {
				int timeleft = manager.getPlayerTimeLeft( playerName );
				if ( timeleft == 0 ) return; // Expired, stay silent
				player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] " + String.format("%02d", Math.round(timeleft/60.0)) + " min of protection left!" );
				player.sendMessage( ChatColor.GRAY + "Type '/campfire' for info on PvP Protection" );
			} catch( CampfireDataException ex ) {
				ex.printStackTrace();
				return;
			}
		}
		
		//-- Save the data to disk
		manager.savePlayerData();
	}
	
	/**
	 * Save player data when someone leaves
	 * @param e
	 */
	@EventHandler( priority = EventPriority.HIGH )
	public void onPlayerQuit( PlayerQuitEvent e  )
	{
		//-- Ignore OPs and players who have the campfire immunity flag
		Player player = e.getPlayer();
		if ( player.isOp() ) return;
		if ( player.hasPermission( "campfire.immune" ) ) return;
		
		//-- Save player data
		this._plugin.getDataManager().savePlayerData();
	}
	
	/**
	 * Let respawned players know how much time they have
	 * @param e
	 */
	@EventHandler( priority = EventPriority.HIGH )
	public void onPlayerRespawn( PlayerRespawnEvent e  )
	{
		//-- Ignore OPs and players who have the campfire immunity flag
		Player player = e.getPlayer();
		if ( player.isOp() ) return;
		if ( player.hasPermission( "campfire.immune" ) ) return;
		
		System.out.println( "DEBUG" );
				
		//-- Check if we're supposed to reset on death
		if ( this._plugin.getConfig().getBoolean( "ResetOnDeath" ) )
		{
			System.out.println( "DEBUG2" );
			//-- Reset them and let them know
			try {
				this._plugin.getDataManager().resetPlayer( player.getName() );
				player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] " + "You have died! Protection reset!" );
			} catch ( CampfireDataException ex ) {
				//-- Player isn't on our list. Odd.
			}
		}
		
		//-- Send them a message
		try {
			int timeleft = this._plugin.getDataManager().getPlayerTimeLeft( player.getName() );
			if ( timeleft == 0 ) return; // Expired, stay silent
			player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] " + String.format("%02d", Math.round(timeleft/60.0)) + " min of protection left!" );
			player.sendMessage( ChatColor.GRAY + "Type '/campfire' for info on PvP Protection" );
		} catch( CampfireDataException ex ) {
			ex.printStackTrace();
			return;
		}
	}
	
	/**
	 * Prevent protected players from using storage and hopper minecarts
	 * @param e
	 */
	@EventHandler( priority = EventPriority.HIGH )
	public void onPlayerInteractEntity( PlayerInteractEntityEvent e )
	{
		//-- Ignore all other entities
		Entity ent = e.getRightClicked();
		if ( !( ent instanceof StorageMinecart ) && !( ent instanceof HopperMinecart ) ) return;
		
		//-- Ignore  OPs and players who have the campfire immunity flag
		Player player = e.getPlayer();
		if ( player.isOp() ) return;
		if ( player.hasPermission( "campfire.immune" ) ) return;

		//-- Get protection status
		DataManager manager = this._plugin.getDataManager();
		String playerName = player.getName();
		boolean isProtected = false;
		try {
			isProtected = manager.playerProtected( playerName );
		} catch ( CampfireDataException ex ) {
			ex.printStackTrace();
			return;
		}
		
		//-- Ignore non-protected players
		if ( !isProtected ) return;
		
		//-- Disallow protected players from using it
		player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] " + ChatColor.RED + "You cannot use storage and hopper carts while protected!" );
		player.sendMessage( "Use '/campfire terminate' to end your protection early!" );
		e.setCancelled( true );
	}
	
	/**
	 * Prevent protected players from breaking storage and hopper minecarts
	 * @param e
	 */
	@EventHandler( priority = EventPriority.HIGH )
	public void onVehicleDamage( VehicleDamageEvent e )
	{
		//-- Ignore all other vehicles
		Vehicle v = e.getVehicle();  
		if ( !( v instanceof StorageMinecart ) && !( v instanceof HopperMinecart ) ) return;
		
		//-- Ignore non-players, OPs and players who have the campfire immunity flag
		Entity ent = e.getAttacker();
		if ( !( ent instanceof Player ) ) return;
		Player player = (Player)ent;
		if ( player.isOp() ) return;
		if ( player.hasPermission( "campfire.immune" ) ) return;

		//-- Get protection status
		DataManager manager = this._plugin.getDataManager();
		String playerName = player.getName();
		boolean isProtected = false;
		try {
			isProtected = manager.playerProtected( playerName );
		} catch ( CampfireDataException ex ) {
			ex.printStackTrace();
			return;
		}
		
		//-- Ignore non-protected players
		if ( !isProtected ) return;
		
		//-- Disallow protected players from breaking it
		player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] " + ChatColor.RED + "You cannot break storage and hopper carts while protected!" );
		player.sendMessage( "Use '/campfire terminate' to end your protection early!" );
		e.setCancelled( true );
	}
	
	/**
	 * Prevent the use of restricted items by and around protected players
	 * @param e
	 */
	@EventHandler( priority = EventPriority.HIGH )
	public void onPlayerInteract( PlayerInteractEvent e )
	{	
		//-- Ignore OPs and players who have the campfire immunity flag
		Player player = e.getPlayer();
		if ( player.isOp() ) return;
		if ( player.hasPermission( "campfire.immune" ) ) return;

		//-- Get protection status
		DataManager manager = this._plugin.getDataManager();
		String playerName = player.getName();
		boolean isProtected = false;
		try {
			isProtected = manager.playerProtected( playerName );
		} catch ( CampfireDataException ex ) {
			ex.printStackTrace();
			return;
		}
		
		//-- Check for restricted items
		Material itemInHand = player.getItemInHand().getType();
		boolean restrictedItem = ( itemInHand.compareTo( Material.FLINT_AND_STEEL ) == 0 || 
					itemInHand.compareTo( Material.FIREBALL ) == 0 ||
					itemInHand.compareTo( Material.LAVA_BUCKET ) == 0 ||
					itemInHand.compareTo( Material.TNT ) == 0 );
		
		//-- Prevent protected players from using restricted items and hoppers
		if ( isProtected )
		{
			// Block restricted items
			if ( restrictedItem || itemInHand.compareTo( Material.HOPPER ) == 0 || itemInHand.compareTo( Material.HOPPER_MINECART ) == 0 )
			{
				player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] " + ChatColor.RED + "You cannot use that item while protected!" );
				player.sendMessage( "Use '/campfire terminate' to end your protection early!" );
				e.setCancelled( true );
				return;
			}
			
			// Block chests and hoppers
			Block clicked = e.getClickedBlock();
			if ( clicked == null ) return; // No block clicked
			Material blocktype = clicked.getType();
			if ( blocktype.compareTo( Material.CHEST ) == 0 || blocktype.compareTo( Material.ENDER_CHEST ) == 0 )
			{
				player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] " + ChatColor.RED + "You cannot use chests while protected!" );
				player.sendMessage( "Use '/campfire terminate' to end your protection early!" );
				e.setCancelled( true );
				return;
			} else if ( blocktype.compareTo( Material.HOPPER ) == 0 )
			{
				player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] " + ChatColor.RED + "You cannot use hoppers while protected!" );
				player.sendMessage( "Use '/campfire terminate' to end your protection early!" );
				e.setCancelled( true );
				return;
			}
			
		}
		
		//-- Prevent non-protected players from using restricted items too close to protected players
		int radius = this._plugin.getConfig().getInt( "FireRadius" );
		if ( radius > 0 && restrictedItem && !isProtected )
		{
			// Get nearby players
			Iterator<Entity> nearby = player.getNearbyEntities( radius, radius, radius ).iterator();
			while ( nearby.hasNext() )
			{
				// We only care about players
				Entity ent = nearby.next();
				if ( !( ent instanceof Player ) ) continue;
				Player victim = (Player) ent;
					
				// Ignore the player, OPs, and players who have the campfire immunity flag
				if ( victim.equals( player ) ) continue;	// The player we're already checking around
				if ( victim.isDead() ) continue;			// Ignore dead people
				if ( victim.isOp() ) continue;
				if ( victim.hasPermission( "campfire.immune" ) ) continue;
				
				// If the victim is protected, cancel and tell the attacker 
				try {
					if ( manager.playerProtected( victim.getName() ) )
					{
						player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] " + ChatColor.RED + "Too close to protected player!" );
						e.setCancelled( true );
						return;
					}
				} catch( CampfireDataException ex ) {
					ex.printStackTrace();
					return;
				}
			}
		}
	}
}
