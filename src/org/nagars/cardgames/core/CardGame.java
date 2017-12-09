package org.nagars.cardgames.core;

import java.util.ArrayList;

public interface CardGame 
{
	public enum TurnState
	{
		CONTINUE, RESIGN, SHOW
	}
	
	public interface GameEvent
	{
		public int getEventId();
		public String getMessage();
	}
	
	public interface GameControl
	{
		public boolean isValid();
		public Card draw(boolean from_throwpile);
		public Hand getHand();
		public boolean discard(Card t);
		public void sortHand();
		public void start();
		public void resign();
	};
	
	public interface PlayerControl
	{
		public Player getPlayer();
		public void addCard(Card c);
		public TurnState playTurn(int time);
		public Card[][] declare();
		public void handleEvent(GameEvent e);
	}
	
//	public ArrayList<Player> activePlayers();
	public GameControl join(PlayerControl p);
}
