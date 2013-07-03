package com.hcsmp.campfire;

/**
 * Exceptions for Campfire's data management class 
 * @author bitdagger
 *
 */
public class CampfireDataException extends Exception
{
	/**
	 * Serial Number
	 */
	private static final long serialVersionUID = 6446300984890743742L;
	
	/**
	 * Constructor
	 * @param string Message to hold
	 */
	public CampfireDataException( String message )
	{
		super( message );
	}
}
