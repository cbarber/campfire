package com.bitdagger.campfire;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Data manager 
 * @author bitdagger
 *
 */
public class DataManager
{
	/**
	 * Reference to the parent plugin
	 */
	private Campfire _plugin;
	
	/**
	 * Actual player data - Keyed by lowercase first name
	 * Saved to disk
	 */
	private HashMap<String,PlayerData> _playerData; 

	/**
	 * Constructor
	 * Register the reference to the parent plugin and do a quick integrity check on the config file
	 * @param plugin Parent plugin
	 */
	public DataManager( Campfire plugin )
	{
		//-- Config check
		FileConfiguration config = plugin.getConfig();
		if ( !config.contains( "Duration" ) ) config.set( "Duration", 60 * 60 );
		if ( !config.contains( "ResetOnDeath" ) ) config.set( "ResetOnDeath", true );
		if ( !config.contains( "WorldGuardPause" ) ) config.set( "WorldGuardPause", true );
		if ( !config.contains( "FireRadius" ) ) config.set( "FireRadius", 5 );
		plugin.saveConfig();
		
		//-- Save reference 
		this._plugin = plugin;
	}
	
	/**
	 * Load player data from disk
	 */
	@SuppressWarnings("unchecked")
	public void loadPlayerData()
	{
		//-- Create the object
		this._playerData = new HashMap<String,PlayerData>();
		
		//-- Try to load from disk
		try {
			ObjectInputStream ois = new ObjectInputStream( new FileInputStream( this._plugin.getDataFolder() + "/players.dat" ) );
			this._playerData = ( HashMap<String,PlayerData> ) ois.readObject();
			ois.close();
		} catch ( FileNotFoundException e ) { // Ignore it, just means we haven't saved yet
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		//-- Add everyone who's online right now, in case the plugin was enabled after boot
		for( Player player : this._plugin.getServer().getOnlinePlayers() )
		{
			// Ignore OPs and players who have the campfire immunity flag
			if ( player.isOp() ) continue;
			if ( player.hasPermission( "Campfire.Immune" ) ) continue;
			
			// Add them
			this.addPlayer( player.getName() );
		}
	}
	
	/**
	 * Save player data to disk
	 */
	public void savePlayerData()
	{
		try {
			ObjectOutputStream oos = new ObjectOutputStream( new FileOutputStream( this._plugin.getDataFolder() + "/players.dat" ) );
			oos.writeObject( this._playerData );
			oos.flush();
			oos.close();
		} catch ( Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Update player data
	 */
	public void update()
	{
		//-- Loop through everyone who's online
		for( Player player : this._plugin.getServer().getOnlinePlayers() )
		{
			// Ignore OPs and players who have the campfire immunity flag
			if ( player.isOp() ) continue;
			if ( player.hasPermission( "Campfire.Immune" ) ) continue;
			if ( player.isDead() ) continue;	// Ignore dead people too
			
			// Check for protection status
			String name = player.getName().toLowerCase();
			if ( !this._playerData.containsKey( name ) )
			{
				// They aren't on our list... Weird. Add them!
				this.addPlayer( name );
				continue;
			}
			PlayerData data = this._playerData.get( name );
			if ( !data.isProtected() ) continue;	// No need to update
			
			// Update the player data
			data.update();
			
			// Check for expiration
			long secondsLeft = this._plugin.getConfig().getLong("Duration") - ( data.getElapsed() / 1000 );
			if ( secondsLeft <= 0 )
			{
				// Protection has expired, so unprotect them and announce to the server they are unprotected
				data.unprotect();
				player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] You are now vulnerable!" );
				for ( Player other : this._plugin.getServer().getOnlinePlayers() )
				{
					other.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] Protection for " + name + " has expired!" );
				}
				continue;
			}
			
			// Send notification about the time left
			long minutes = Math.round(secondsLeft / 60.0);
			if ( minutes > 10 && minutes % 5 == 0 && secondsLeft % 60 == 0 )
			{
				// Before the last 10 minutes, send messages every 5 minutes on the minute
				player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + String.format( "] Expires in %d minute%s", minutes, ( minutes != 1 ? "s" : "" ) ) + "!" );
			} else if ( minutes <= 10 && secondsLeft % 60 == 0 ) {
				// During the last 10 minutes, send messages every minute on the minute
				player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + String.format( "] Expires in %d minute%s", minutes, ( minutes != 1 ? "s" : "" ) ) + "!" );
			}
		}
	}
	
	/**
	 * Reset a player's data
	 * @param name Player to reset
	 * @throws CampfireDataException 
	 */
	public void resetPlayer( String name ) throws CampfireDataException
	{
		name = name.toLowerCase();
		if ( !this._playerData.containsKey( name ) ) throw new CampfireDataException( "Player not found!" );
		this._playerData.put( name, new PlayerData() );
	}
	
	/**
	 * Remove a player from the protection list
	 * @param name Player to remove
	 * @throws CampfireDataException 
	 */
	public void removePlayer( String name ) throws CampfireDataException
	{
		name = name.toLowerCase();
		if ( !this._playerData.containsKey( name ) ) throw new CampfireDataException( "Player not found!" );
		this._playerData.remove( name );
	}

	/**
	 * Get the remaining protection time for a given player in seconds
	 * @param name Player to look up
	 * @return Remaining time in seconds
	 * @throws CampfireDataException
	 */
	public int getPlayerTimeLeft( String name ) throws CampfireDataException
	{
		name = name.toLowerCase();
		if ( !this._playerData.containsKey( name ) ) throw new CampfireDataException( "Player not found!" );
		PlayerData data = this._playerData.get( name );
		if ( !data.isProtected() ) return 0; // Protection has expired or terminated, 0 seconds remain
		long elapsed = data.getElapsed();
		return this._plugin.getConfig().getInt("Duration") - (((int)elapsed)/1000);
	}

	/**
	 * Update the current WorldGuard protection state for a player
	 * @param isProtected Current protection status
	 * @param name Player to update
	 * @return True if the value changed from its previous value
	 * @throws CampfireDataException
	 */
	public boolean setPlayerWGProtected( boolean isProtected, String name ) throws CampfireDataException
	{
		name = name.toLowerCase();
		if ( !this._playerData.containsKey( name ) ) throw new CampfireDataException( "Player not found!" );
		PlayerData data = this._playerData.get( name );
		if ( data.isWG() == isProtected ) return false;
		data.setWG( isProtected );
		return true;
	}

	/**
	 * Add a new user to the data set
	 * @param name Player's name to add
	 * @return True if they weren't already in the DB
	 */
	public boolean addPlayer( String name )
	{
		name = name.toLowerCase();
		if ( this._playerData.containsKey( name ) )
		{
			// Player already exists, reset their last update time
			this._playerData.get( name ).setLastUpdated();
			return false;
		}
		this._playerData.put( name, new PlayerData() );
		return true;
	}

	/**
	 * Is the given player currently under protection?
	 * @param name Player to check
	 * @return True if currently under protection
	 * @throws CampfireDataException
	 */
	public boolean playerProtected( String name ) throws CampfireDataException
	{
		name = name.toLowerCase();
		if ( !this._playerData.containsKey( name ) ) throw new CampfireDataException( "Player not found!" );
		return this._playerData.get( name ).isProtected();
	}
	
	/**
	 * Manually terminate the player's protection
	 * @param name Player name
	 * @throws CampfireDataException 
	 */
	public void terminate( String name ) throws CampfireDataException
	{
		name = name.toLowerCase();
		if ( !this._playerData.containsKey( name ) ) throw new CampfireDataException( "Player not found!" );
		this._playerData.get( name ).unprotect();
	}

}
