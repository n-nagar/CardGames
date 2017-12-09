package org.nagars.cardgames.core;

import java.net.URL;

import org.nagars.cardgames.core.CardGame.GameEvent;

public class Player 
{

	private String name;
	private URL player_image;
	static final long serialVersionUID = 1;

	
	public Player()
	{
		name = null;
		player_image = null;
	}
	
	public final String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		if (name != null)
			this.name = new String(name);
	}
	
	
	public void setImageURL(URL image)
	{
		this.player_image = image;
	}
	
	public URL getImageURL() 
	{
		return player_image;
	}

	
	public Player getPlayer()
	{
		return this;
	}
}
