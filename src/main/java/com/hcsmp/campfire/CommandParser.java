package com.hcsmp.campfire;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Command parser
 * @author bitdagger
 *
 */
public class CommandParser implements CommandExecutor
{
	/**
	 * Reference to the parent plugin
	 */
	private Campfire _plugin;
	
	/**
	 * List of users who have /campfire terminate'd
	 */
	private ArrayList<String> _terminated;
	
	/**
	 * Constructor
	 * Register the reference to the parent plugin and create the list of terminated players
	 * @param plugin Parent plugin
	 */
	public CommandParser( Campfire plugin )
	{
		this._plugin = plugin;
		this._terminated = new ArrayList<String>();
	}

	/**
	 * Process campfire commands
	 * @param sender Command sender
	 * @param command Command that was sent
	 * @param label
	 * @param args Arguments
	 */
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		//-- Check arguments
		if ( args.length == 0 ) return this.commandHelp( sender );
		
		//-- Command switch
		if ( args[0].equalsIgnoreCase( "about" ) ) return this.about( sender, args );
		if ( args[0].equalsIgnoreCase( "reload" ) ) return this.processReload( sender, args );
		if ( args[0].equalsIgnoreCase( "reset" ) ) return this.processReset( sender, args );
		if ( args[0].equalsIgnoreCase( "terminate" ) ) return this.processTerminate( sender, args );
		if ( args[0].equalsIgnoreCase( "confirm" ) ) return this.processConfirm( sender, args );
		if ( args[0].equalsIgnoreCase( "timeleft" ) ) return this.processTimeleft( sender, args );
		
		//-- Default to usage
		return this.commandHelp( sender );
	}
	
	/**
	 * Display about message
	 * @param sender Command sender
	 * @param args Arguments
	 * @return
	 */
	private boolean about( CommandSender sender, String[] args )
	{
		//-- Send message
		FileConfiguration config = this._plugin.getConfig();
		sender.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "]" );
		sender.sendMessage( "Campfire is a PvP protection plugin designed to help new players get on their feet. Players under protection " +
				" cannot engage in PvP, open chests, or use items such as TNT and lava buckets. " +
				"Protection lasts for " + ( config.getInt("Duration")/60 ) + "min" + ( config.getBoolean( "WorldGuardPause" ) ? " and pauses while in WorldGuard protected areas" : "" ) + ". " +
				"You can end your protection early by using the 'campfire terminate' command, and you can check anyone's protection status using the 'campfire timeleft' command." );
		return true;
	}
	
	/**
	 * Process the reload command
	 * @param sender Command sender
	 * @param args Arguments
	 * @return
	 */
	private boolean processReload( CommandSender sender, String[] args )
	{
		//-- Check their permissions
		if ( !sender.hasPermission( "campfire.reload" ) )
		{
			sender.sendMessage( ChatColor.RED + "You don't have permission to do that!" );
			return true;
		}
		
		//-- Reload the plugin
		this._plugin.reload();
		
		//-- Send message
		sender.sendMessage( "Reloaded!" );
		return true;
	}
	
	/**
	 * Process the reset command
	 * @param sender Command sender
	 * @param args Arguments
	 * @return
	 */
	private boolean processReset( CommandSender sender, String[] args )
	{
		//-- Check their permissions
		if ( !sender.hasPermission( "campfire.reset" ) )
		{
			sender.sendMessage( ChatColor.RED + "You don't have permission to do that!" );
			return true;
		}
		
		//-- Check for additional variables
		if ( args.length < 2 )
		{
			sender.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] Usage: " );
			sender.sendMessage( ChatColor.WHITE + "/campfire reset <player>" );
			sender.sendMessage( ChatColor.GRAY + "Resets a player's protection" );
			return true;
		}
		
		//-- Check if the target is OP or immune
		Player player = this._plugin.getServer().getPlayer( args[1] );
		if ( player != null )
		{
			try {
				if ( player.isOp() || player.hasPermission( "campfire.immune" ) )
				{
					this._plugin.getDataManager().removePlayer( args[1] );
					sender.sendMessage( "Protection reset for " + args[1] + "!" );
					return true;
				}
			} catch( CampfireDataException ex ) {
				sender.sendMessage( ChatColor.RED + "Failed to reset protection with exception: " + ex.getMessage() );
				return true;
			}
		}
		
		//-- Reset target
		try {
			this._plugin.getDataManager().resetPlayer( args[1] );
			sender.sendMessage( "Protection reset for " + args[1] + "!" );
		} catch( CampfireDataException e ) {
            sender.sendMessage( ChatColor.RED + "Failed to reset protection with exception: " + e.getMessage() );
			return true;
		}
		
		//-- Send the user a message if they are online
		Player target = this._plugin.getServer().getPlayer( args[1] );
		if ( target != null ) target.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] " + "Protection reset by an admin!" );
		return true;
	}
	
	/**
	 * Process the terminate command
	 * @param sender Command sender
	 * @param args Arguments
	 * @return
	 */
	private boolean processTerminate( CommandSender sender, String[] args )
	{
		//-- Check if the sender is a player
		if ( !( sender instanceof Player ) )
		{
			sender.sendMessage( ChatColor.RED + "This command is only for in-game players!" );
			return true;
		}
		
		//-- Ignore OPs and players with the campfire immune permission
		if ( sender.isOp() ) return true;
		if ( sender.hasPermission( "campfire.immune" ) ) return true;
		
		//-- Check if they have already expired
		DataManager manager = this._plugin.getDataManager();
		String name = sender.getName();
		try {
			if ( !manager.playerProtected( name ) )
			{
				sender.sendMessage( ChatColor.RED + "Your protection has already expired!" );
				return true;
			}
		} catch( CampfireDataException ex ) {
			ex.printStackTrace();
			return true;
		}
		
		//-- Show them the warning
		sender.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] " + "You will be vulnerable to PvP if you " +
		"end your protection. If you understand the risk, use '/campfire confirm' to proceed..." );
		
		//-- Flag them as having read the termination text
		this._terminated.add( name );
		return true;
	}
	
	/**
	 * Process the confirm command
	 * @param sender Command sender
	 * @param args Arguments
	 * @return
	 */
	private boolean processConfirm( CommandSender sender, String[] args )
	{
		//-- Check if the sender is a player
		if ( !( sender instanceof Player ) )
		{
			sender.sendMessage( ChatColor.RED + "This command is only for in-game players!" );
			return true;
		}
		
		//-- Ignore OPs and players with the campfire immune permission
		if ( sender.isOp() ) return true;
		if ( sender.hasPermission( "campfire.immune" ) ) return true;
		
		//-- Check they have read /campfire terminate first
		String name = sender.getName();
		DataManager manager = this._plugin.getDataManager();
		if ( !this._terminated.contains( name ) ) return true;
		this._terminated.remove( name );
		
		//-- Terminate it and let everyone know
		try {
			manager.terminate( name );
		} catch ( CampfireDataException ex ) {
			ex.printStackTrace();
		}
		for ( Player player : this._plugin.getServer().getOnlinePlayers() )
		{
			player.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] " + name + " terminated their protection!" );
		}
		sender.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] You are now vulnerable!" );
		return true;
		
	}
	
	/**
	 * Process the timeleft command
	 * @param sender Command sender
	 * @param args Arguments
	 * @return
	 */
	private boolean processTimeleft( CommandSender sender, String[] args )
	{
		// Check for additional variables
		String target;
		if ( args.length < 2 ) target = sender.getName();
		else target = args[1];
		
		// Get the time left for that player
		try {
			int timeleft = this._plugin.getDataManager().getPlayerTimeLeft( target );
			if ( timeleft == 0 )
			{
				sender.sendMessage( target + ": Protection expired!" );
				return true;
			}
			sender.sendMessage( target + ": " + String.format("%02d", Math.round(timeleft/60.0)) + " min of protection left!" );
		} catch ( CampfireDataException e ) {
			sender.sendMessage( ChatColor.RED + e.getMessage() );
		}
		return true;
		
	}
	
	/**
	 * Send the default command usage to the sender
	 * @param sender Command sender
	 */
	private boolean commandHelp( CommandSender sender )
	{
		sender.sendMessage( ChatColor.WHITE + "[" + ChatColor.GOLD + "PvP Protection" + ChatColor.WHITE + "] Usage: " );
		sender.sendMessage( ChatColor.WHITE + "/campfire about" );
		sender.sendMessage( ChatColor.GRAY + "About this plugin" );
		sender.sendMessage( ChatColor.WHITE + "/campfire terminate" );
		sender.sendMessage( ChatColor.GRAY + "Removes your protection early" );
		sender.sendMessage( ChatColor.WHITE + "/campfire timeleft [player]" );
		sender.sendMessage( ChatColor.GRAY + "Gives the duration left for a player's protection" );
		if ( sender.hasPermission( "campfire.reset" ) )
		{
			sender.sendMessage( ChatColor.WHITE + "/campfire reset <player> " );
			sender.sendMessage( ChatColor.GRAY + "Resets a player's protection status" );
		}
		if ( sender.hasPermission( "campfire.reload" ) )
		{
			sender.sendMessage( ChatColor.WHITE + "/campfire reload" );
			sender.sendMessage( ChatColor.GRAY + "Clean reload of the plugin" );
		}
		return true;
	}
}
