package com.bitdagger.campfire;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

/**
 * Campfire is a PvP Protection plugin designed to give new players a chance to get their
 * footing before being attacked by more well-equipped players. While under PvP Protection, 
 * players can't take or receive PvP damage. Players' PvP protection expires after a configurable 
 * amount of time, or players may opt to terminate their protection early. Campfire does 
 * protect against any kind of environmental damage such as mobs, fall damage, and drowning. 
 *  
 * @author bitdagger
 *
 */
public class Campfire extends JavaPlugin
{
	/**
	 * Data management class
	 */
	private DataManager _manager;
	
	/**
	 * Holds the worker thread for updating player data
	 */
	private int _thread;
	
	/**
	 * Register events and command executor, load the data manager and prepare the plugin for use.
	 */
	public void onEnable()
	{	
		//-- Create the data manager and load data from disk
		this._manager = new DataManager( this );
		this._manager.loadPlayerData();
				
		//-- Check for WorldGuard if we need to use it
		WorldGuardPlugin plugin = null;
		if ( this.getConfig().getBoolean( "WorldGuardPause" ) )
		{
			Plugin p = this.getServer().getPluginManager().getPlugin( "WorldGuard" );
			if ( p != null && p instanceof WorldGuardPlugin )
			{
				System.out.println( "[Campfire] Found WorldGuard" );
				plugin = ( WorldGuardPlugin ) p;
			}
		}
		
		//-- Register events and command executor
		this.getServer().getPluginManager().registerEvents( new EventListener( this, plugin ), this );
		this.getCommand("campfire").setExecutor( new CommandParser( this ) );
		
		//-- Start the task to update player data
        final DataManager manager = this._manager;
        this._thread = this.getServer().getScheduler().scheduleSyncRepeatingTask( this, new Runnable()
        {
            public void run() { manager.update(); }
        }, 20L, 20L ); // Update every second if the clock is perfect
	}

	
	/**
	 * Save the plugin's state and get ready to shutdown 
	 */
	public void onDisable()
	{
		//-- Kill the update thread
		this.getServer().getScheduler().cancelTask( this._thread );

		//-- Save the existing player data
		this._manager.savePlayerData();
	}
	
	/**
	 * Get the reference to the data manager
	 * @return Data manager
	 */
	public DataManager getDataManager()
	{
		return this._manager;
	}
}
