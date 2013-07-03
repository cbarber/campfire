package com.hcsmp.campfire;

import java.io.Serializable;

/**
 * Holds player's campfire data
 * @author bitdagger
 *
 */
public class PlayerData implements Serializable
{
	/**
	 * Serializable ID
	 */
	private static final long serialVersionUID = -9108869362947930490L;

	/**
	 * Timestamp of the last update in milliseconds
	 */
	private long _lastUpdate;
	
	/**
	 * Amount of time elapsed in milliseconds
	 */
	private long _timeElapsed;
	
	/**
	 * WorldGuard protected zone flag
	 */
	private boolean _WGZone;
	
	/**
	 * Is the player protected from PvP
	 */
	private boolean _protected;
	
	/**
	 * Constructor
	 * Initialize defaults
	 */
	public PlayerData()
	{
		this._lastUpdate = System.currentTimeMillis();
		this._timeElapsed = 0;
		this._WGZone = false;
		this._protected = true;
	}

	/**
	 * Is the player being protected?
	 * @return
	 */
	public boolean isProtected()
	{
		return this._protected;
	}

	/**
	 * Is the player currently protected by a WG zone
	 * @return WG zone protected
	 */
	public boolean isWG()
	{
		return this._WGZone;
	}
	
	/**
	 * Set the WG zone status
	 * @param val
	 */
	public void setWG( boolean val )
	{
		this._WGZone = val;
	}

	/**
	 * Disable protection
	 */
	public void unprotect()
	{
		this._protected = false;
	}

	/**
	 * Get total elapsed time
	 * @return
	 */
	public long getElapsed()
	{
		return this._timeElapsed;
	}

	/**
	 * Update the time elapsed
	 */
	public void update()
	{
		if ( !this._WGZone )
		{
			this._timeElapsed += (System.currentTimeMillis() - this._lastUpdate);
		}
		this._lastUpdate = System.currentTimeMillis();
	}
	
	/**
	 * Set the last updated time
	 */
	public void setLastUpdated()
	{
		this._lastUpdate = System.currentTimeMillis();
	}
}