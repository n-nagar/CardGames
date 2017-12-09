package org.nagars.cardgames.core;

import java.net.URL;
import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.Semaphore;

public class Rummy implements CardGame
{
	public enum RummyEvents
	{
		NEW_PLAYER, ACTIVE_PLAYER, GAME_JOKER, PLAYER_DECLARING, PLAYER_RESIGNED, 
		GAME_INPLAY, PLAYER_WON, PLAYER_LOST, GAME_OVER, CARD_DISCARDED, DRAW_FROM_DECK,
		DRAW_FROM_PILE, WAITING_FOR_PLAYERS, FORCED_DISCARD
	}

	private enum PlayerState { DEALING, EXPECTING_DRAW, EXPECTING_DISCARD, TURN_OVER };

	public static class PlayerEvent implements CardGame.GameEvent
	{
		private String id;
		private String name;
		private URL url;
		private RummyEvents eid;
		private int lost_points;

		private void initialize(RummyEvents e, RummyControl rc)
		{
			this.lost_points = 0;
			this.eid = e;
			this.id = Integer.toString(rc.getPlayerControl().getPlayer().hashCode());
			this.name = rc.getPlayerControl().getPlayer().getName();
			this.url = rc.getPlayerControl().getPlayer().getImageURL();			
		}
		
		PlayerEvent(RummyEvents e, RummyControl rc)
		{
			initialize(e, rc);
		}
		
		PlayerEvent(RummyEvents e, RummyControl rc, int points)
		{
			initialize(e, rc);
			this.lost_points = points;
		}

		@Override
		public int getEventId() 
		{
			return this.eid.ordinal();
		}

		@Override
		public String getMessage() 
		{
			return this.eid.toString() + ":" + name;
		}
		
		public String getPlayerName()
		{
			return this.name;
		}
		
		public String getPlayerID()
		{
			return this.id;
		}
		
		public URL getPlayerImage()
		{
			return this.url;
		}
		
		public int getPoints()
		{
			return this.lost_points;
		}
	}
	
	public static class CardEvent implements CardGame.GameEvent
	{
		private RummyEvents eid;
		private Card c;
		
		public CardEvent(RummyEvents e, Card c)
		{
			this.eid = e;
			this.c = c;
		}
		
		@Override
		public int getEventId() {
			return this.eid.ordinal();
		}

		public Card getCard()
		{
			return c;
		}
		@Override
		public String getMessage() 
		{
			return eid.toString() + ":" + c.toString();
		}
		
	}
	
	public static class RummyEvent implements CardGame.GameEvent
	{
		private RummyEvents e;
		
		RummyEvent(RummyEvents e)
		{
			this.e = e;
		}

		@Override
		public int getEventId() 
		{
			return e.ordinal();
		}

		@Override
		public String getMessage() 
		{
			return e.toString();
		}
		
	}
	private class EventPusher implements Runnable
	{
		private CardGame.GameEvent e;
		private PlayerControl pc;
		
		EventPusher(CardGame.GameEvent e, PlayerControl pc)
		{
			this.e = e;
			this.pc = pc;
		}
		
		public void run()
		{
			pc.handleEvent(e);
		}
	}

	private static final int max_cards = 13;
	private ArrayList<RummyControl> players;
	private final static int MAX_PLAYERS = 4;
	private Deck game_deck;
	private Card game_joker;
	private Stack<Card> throw_pile;
	private int current_player;
	private Player host;
	private int start_signal;
	private boolean game_over;
	private boolean game_on;

	public Rummy() 
	{
		players = new ArrayList<RummyControl>();
		current_player = 0;
		start_signal = 0;
		game_deck = null;
		throw_pile = null;
		game_on = false;
	}

	private void sendEventToPlayer(RummyControl rc, CardGame.GameEvent e)
	{
		new Thread(new EventPusher(e, rc.getPlayerControl())).run();
	}

	private void sendEventToPlayers(CardGame.GameEvent e, int ignore_player_idx)
	{
		for (int i = 0; i < players.size(); i++)
		{
			if (i != ignore_player_idx)
			{
				this.sendEventToPlayer(players.get(i), e);
				// Send info about other players to the new player
				if (e.getEventId() == RummyEvents.NEW_PLAYER.ordinal())
				{
					PlayerEvent pe = new PlayerEvent(RummyEvents.NEW_PLAYER, players.get(i));
					this.sendEventToPlayer(players.get(ignore_player_idx), pe);
				}
			}
		}
	}
	
	private void start(RummyControl rc)
	{
		boolean start_game = false;
		boolean game_started_while_waiting = false;
		
		if (!this.game_on)
		{
			synchronized(this)
			{
				if (!this.game_on)
					start_signal++;
				else
					game_started_while_waiting = true;

				if (start_signal == players.size())
				{
					this.game_on = true;
					start_game = true;
				}
			}
			
			if (start_game)
			{
				new Thread(new Runnable() {
					public void run()
					{
						gameOn();
					}
				}).start();
				rc.waitForGame();
			}
			else if (!game_started_while_waiting)
			{
				sendEventToPlayer(rc, new RummyEvent(RummyEvents.WAITING_FOR_PLAYERS));
				rc.waitForGame();
			}
			else
			{
				GameEvent e = new RummyEvent(RummyEvents.GAME_INPLAY);
				sendEventToPlayer(rc, e);
			}
		}
		else		
		{
			GameEvent e = new RummyEvent(RummyEvents.GAME_INPLAY);
			sendEventToPlayer(rc, e);
		}
	}
	
	private void gameOn()
	{
		int num_players = players.size();
		
		game_over = false;
		if ((game_deck == null) || ((num_players/2) != game_deck.getTotalDecks()))
		{
			game_deck = new Deck();
			if (num_players > 2)
			{
				System.out.println("2 decks in play");
				Deck extra_deck = new Deck();
				game_deck.add(extra_deck);
			}
		}
		else
		{
			game_deck.reset();
		}
		// Create a new throw pile
		throw_pile = new Stack<Card>();
		
		// Double shuffle to mix it up
		game_deck.shuffle();
		game_deck.shuffle();
		// Serve the cards
		try 
		{
    		for (int i = 0; i < max_cards; i++)
    		{
    			for (int j = 0; j < num_players; j++)
    			{
    				this.current_player = j;
    				// Card c = players.get(j).draw(false);
    				// players.get(j).getPlayerControl().addCard(c);
    				Card c = game_deck.draw();
    				players.get(j).dealtCard(c);
    			}
    		}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		// Draw a joker
		this.game_joker = game_deck.draw(true);
		// Notify all players about this
		CardEvent rce = new CardEvent(RummyEvents.GAME_JOKER, this.game_joker);
		this.sendEventToPlayers(rce, -1);
		
		current_player = 0;
		do
		{
			RummyControl pc;
			synchronized(players)
			{
				//  Players resigned while waiting to acquire
				//   lock
				if (players.size() == 0)
					break;
				// Current player resigned while waiting for lock
				//  so go back to the first player
				if (current_player > players.size())
					current_player = 0;
				pc = players.get(current_player);
			}
			PlayerEvent pe = new PlayerEvent(RummyEvents.ACTIVE_PLAYER, pc);
			this.sendEventToPlayers(pe, current_player);

			pc.startTurn();
			CardGame.TurnState ts = pc.getPlayerControl().playTurn(-1);
			if (ts == CardGame.TurnState.SHOW)
			{
				pe = new PlayerEvent(RummyEvents.PLAYER_DECLARING, pc);
				this.sendEventToPlayers(pe, current_player);
				// Player wants to declare
				Card [][] run = pc.getPlayerControl().declare();
				if (run == null)
				{
					this.remove(pc, new PlayerEvent(RummyEvents.PLAYER_LOST, pc, pc.leave()));
					start_signal--;
					// Penalty for the user who faked a show
					
				}
				else
				{
					int points = pc.leave(run);
					if (points > 0)
					{
						// Bad show
						this.remove(pc, new PlayerEvent(RummyEvents.PLAYER_LOST, pc, points));
						start_signal--;
					}
					else
					{
						pe = new PlayerEvent(RummyEvents.PLAYER_WON, pc);
						this.sendEventToPlayers(pe, -1);
						// User has won, so count other users
						game_over = true;
						// All other players must show as well
						for (int i = 0; i < players.size(); i++)
						{
							if (i != current_player)
							{
								RummyControl rc = players.get(i);
								run = rc.getPlayerControl().declare();
								this.sendEventToPlayers(new PlayerEvent(RummyEvents.PLAYER_LOST, rc, rc.leave(run)), -1);
							}
						}
					}
				}
			}
			else if (ts == TurnState.RESIGN)
			{
				this.remove(pc, new PlayerEvent(RummyEvents.PLAYER_RESIGNED, pc, pc.leave()));
				start_signal--;
			}
			else
				pc.finishTurn();
			
			if (players.size() > 0)
			{
				if (current_player > players.size())
					current_player = 0;
				else
					current_player = (current_player + 1) % players.size();
			}
		/*
		 *  Play as long as the game is not over and one player is playing a single
		 *   player game or more than one is still playing a multi-player game
		 */
		}while((!game_over) && ((start_signal > 1) || (num_players == 1)));
		
		// Remove any players still in the game
		while (players.size() > 0)
		{
			RummyControl rc = players.get(0);
			this.remove(rc, new RummyEvent(RummyEvents.GAME_OVER));
		}
		game_on = false;
	}

	

	public String getName() {
		return "Indian Rummy";
	}

	public int maxPlayers() 
	{
		return MAX_PLAYERS;
	}

	public int cardsPerPlayer() 
	{
		return max_cards;
	}

//	@Override
	public final ArrayList<Player> activePlayers() 
	{
		ArrayList<Player> pl = new ArrayList<Player>();
		for (RummyControl rp : players)
			pl.add(rp.getPlayerControl().getPlayer());
		return pl;
	}

	public Player getCurrentPlayer()
	{
		if ((players.size() > 0) && (current_player < players.size()))
			return players.get(current_player).getPlayerControl().getPlayer();
		return null;
	}
	
	public void remove(RummyControl gc, GameEvent msg_to_send)
	{
		this.sendEventToPlayers(msg_to_send, -1);
		synchronized(players)
		{
			if (players.remove(gc))
				this.sendEventToPlayer(gc, new RummyEvent(RummyEvents.GAME_OVER));
		}
		gc.signalGameOver();
	}
	@Override
	public GameControl join(PlayerControl p) 
	{
		RummyControl rp = null;
		int player_idx = -1;
		synchronized(players)
		{
			if (players.size() < MAX_PLAYERS)
			{
				rp = new RummyControl(p);
				players.add(rp);
				player_idx = players.indexOf(rp);			}
		}
		
		if (player_idx >= 0)
		{
			PlayerEvent pe = new PlayerEvent(RummyEvents.NEW_PLAYER, rp);
			this.sendEventToPlayers(pe, player_idx);
		}
		
		return rp;
	}
	
	private void resign(Rummy.RummyControl p, int points)
	{
		this.remove(p, new PlayerEvent(RummyEvents.PLAYER_RESIGNED, p, points));				
	}
	
	private int getIndex(Card.Suite s)
	{
		switch(s)
		{
		case CLUB: return 0;
		case DIAMOND: return 1;
		case HEART: return 2;
		case SPADE: return 3;
		default: return -1;
		}
	}
	
	private int getIndex(Card.Value v)
	{
		switch(v)
		{
		case ACE: return 14;
		case TWO: return 2;
		case THREE: return 3;
		case FOUR: return 4;
		case FIVE: return 5;
		case SIX: return 6;
		case SEVEN: return 7;
		case EIGHT: return 8;
		case NINE: return 9;
		case TEN: return 10;
		case JACK: return 11;
		case QUEEN: return 12;
		case KING: return 13;
		default: return -1;
		}
	}

	public int cardValue(Card c, boolean consider_suite) 
	{
		int cval = 0;
		int sval = 0;
		
		if (c == null)
			return 0;
		
		if (c.isJoker())
			return 0;
		
		if (consider_suite)
			sval = getIndex(c.getSuite());
		
		cval = getIndex(c.getValue());
		
		return (sval << 4) | cval;
	}

	/*
	 * returns 
	 * 		-1: next_card is not in sequence with prev_card
	 * 		val >= 0: next_card is in sequence with prev_card after using val jokers
	 */
	private int isNextSequenceCard(Card prev_card, Card this_card, int jokers_inbetween)
	{
		int ret = -1;
		
		if ((prev_card == null) || (this_card == null))
			return ret;
		
		if (prev_card.getSuite().compareTo(this_card.getSuite()) != 0)
			return ret;
		
		int pval = Rummy.this.cardValue(prev_card, false);
		int tval = Rummy.this.cardValue(this_card, false);
		
		do
		{
			int gap = tval - pval - 1;
			if ((pval + 1) == tval)
			{
				ret = 0;
				break;
			}
			else if ((gap > 0) && (gap <= jokers_inbetween))
			{
				ret = gap;
				break;
			}
			else if (prev_card.getValue() == Card.Value.ACE)
				pval = 1; 	// If the card is an ACE, try A,2,3 like sequence
			else
				break;
		} while (true);

		return ret;
	}
	
	private boolean isJoker(Card c)
	{
		return (c.isJoker() || (getIndex(c.getValue()) == getIndex(this.game_joker.getValue())));
	}
	
	public Hand getEmptyHand()
	{
		return new Hand(max_cards, 1);
	}
	
	private int getPoints(Card c)
	{
		int ret = getIndex(c.getValue());
		
		return (ret > 10)?10:ret;
	}
	
	private int getPoints(Card[] run)
	{
		int ret = 0;
		for (int j = 0; j < run.length; j++)
			if (!run[j].isJoker())
			{
				ret += getPoints(run[j]);
			}
		return ret;
	}
	
	private int getPoints(Hand hand)
	{
		int ret = 0;

		for (int i = 0; i < hand.size(); i++)
		{
			if (!hand.getCard(i).isJoker())
				ret += getPoints(hand.getCard(i));
		}
		return ret;
	}
	
	protected void sort(Hand hand)
	{
		for (int i = 0; i < hand.size() - 1; i++)
		{
			int min = i;
			int mv = cardValue(hand.getCard(i), true);
			for (int j = i+1; j < hand.size(); j++)
			{
				int cv = cardValue(hand.getCard(j), true);
				if (mv > cv)
				{
					min = j;
					mv = cv;
				}
			}
			if (min != i)
				hand.swap(min, i);
		}
	}
	
	private void sort(Card[] run)
	{
		for (int i = 0; i < run.length - 1; i++)
		{
			int min = i;
			int mv = (this.isJoker(run[i])?0:cardValue(run[min], true));
			for (int j = i+1; j < run.length; j++)
			{
				int cv = (this.isJoker(run[j])?0:cardValue(run[j], true));
				if (mv > cv)
				{
					min = j;
					mv = cv;
				}
			}
			if (min != i)
			{
				Card t = run[i];
				run[i] = run[min];
				run[min] = t;
			}
		}
	}
	
	/*
	 * Input
	 * 	Must have a sorted run, otherwise this will fail
	 * Returns:
	 *  -1: Not a sequence
	 *   0: Sequence without using any jokers
	 *   > 1: Sequence using jokers
	 */		
	public int isSequence(Card[] run)
	{
		if ((run == null) || (run.length == 0) || (run.length < 3))
			return -1;
		
		int idx = 0;
		int natural_jokers = 0;
		while ((idx < run.length) && this.isJoker(run[idx]))
		{
			if (run[idx].isJoker())
				natural_jokers++;
			idx++;
		}
		
		// A run of all jokers can be a sequence
		if (idx == run.length)
			return 1;
		
		int num_jokers = idx;
		int non_joker_idx = idx;
		int ret = 0;
		Card prev_card = run[idx++];
		while(idx < run.length)
		{
			Card this_card = run[idx++];
			
			// Check if this is the immediate next card
			int is_seq = this.isNextSequenceCard(prev_card, this_card, 0);
			if (is_seq < 0)
			{
				// Since the two cards don't match, check if
				//  they can match with jokers (if there are any). 
				is_seq = this.isNextSequenceCard(prev_card, this_card, num_jokers);
				if (is_seq < 0)
				{
					// One special check is if this card is an ACE. Sort will
					//  always sort ACE high, so it will be the last card in the
					//  sorted run. Check if the first non-joker card in this run
					//  can be the next card to ACE with the jokers remaining in-between
					//  Check to make sure this ACE is the last card
					//  like A,2,3 or A,*,*,4 or A,*,3
					if ((idx == run.length) && (this_card.getValue() == Card.Value.ACE))
					{
						is_seq = this.isNextSequenceCard(this_card, run[non_joker_idx], num_jokers);
						if (is_seq < 0)
						{
							ret = -1;
							break;
						}
						else
						{
							num_jokers -= is_seq;
							ret += is_seq;
							/*
							 * If matched with a joker, did it match with a game joker that
							 *  would be a natural sequence. Since there can only be
							 *  one joker that can pass this requirement, check for that
							 */
							if ((is_seq == 1) && (non_joker_idx > natural_jokers))
							{
								for (int i = 0; i < non_joker_idx; i++)
								{
									if (this.isNextSequenceCard(this_card, run[i], 0) == 0)
									{
										ret--;
									}
								}
							}
						}
					}
					else
					{
						ret = -1;
						break;
					}
				}
				else
				{
					num_jokers -= is_seq;
					ret += is_seq;
					/*
					 * If matched with a joker, did it match with a game joker that
					 *  would be a natural sequence. Since there can only be
					 *  one joker that can pass this requirement, check for that
					 */
					if ((is_seq == 1) && (non_joker_idx > natural_jokers))
					{
						for (int i = 0; i < non_joker_idx; i++)
						{
							if ((this.isNextSequenceCard(prev_card, run[i], 0) == 0) ||
								(this.isNextSequenceCard(run[i], this_card, 0) == 0))
							{
								ret--;
							}
						}
					}
					prev_card = this_card;
				}
			}
			else
				prev_card = this_card;
		}
		
		// If no jokers were used, then it was part of the sequence
		//  either at the beginning, or the end. Check to make sure
		//  we are not missing a pure sequence
		if ((num_jokers > 0) && (natural_jokers == 0) && (non_joker_idx == 1))
		{
			if ((this.isNextSequenceCard(run[0], run[1], 0) != 0) &&
				(this.isNextSequenceCard(run[run.length - 1], run[0], 0) != 0))
			{
				ret = 1;
			}
			
		}
		else
			ret += num_jokers;
		return ret;
	}
	/*
	 * Input
	 * 	Must have a sorted run, otherwise this will fail
	 * Returns:
	 *  true: if the run is a valid set
	 */
	public boolean isSet(Card[] run)
	{
		if ((run == null) || (run.length < 3) || (run.length > 4))
			return false;
		
		boolean ret = true;
		int idx = 0;
		
		while ((idx < run.length) && (this.isJoker(run[idx])))
			idx++;
		
		// A run of all jokers can be a set
		if (idx == run.length)
			return ret;

		boolean suit[] = new boolean[Card.Suite.values().length];
		for (int i = 0; i < suit.length; i++)
			suit[i] = false;
		
		int non_joker_idx = idx;
		suit[getIndex(run[idx].getSuite())] = true;
		for (int i = idx+1; i < run.length; i++)
		{
			if (!this.isJoker(run[i]))
			{
				if ((!suit[getIndex(run[i].getSuite())]) && (run[i].getValue() == run[non_joker_idx].getValue()))
					suit[getIndex(run[i].getSuite())] = true;
				else
				{
					ret = false;
					break;
				}
			}
		}
		return ret;
	}
	
	private int show(Hand.Show show_cards)
	{
		int points = 0;
		boolean pure = false;
		int sequences = 0;
		int sets = 0;
		int total_cards = 0;
		

		show_cards.print();
		Card[][] run = show_cards.get();
		if ((run != null) && (run.length > 0))
		{
			for (int i = 0; i < run.length; i++)
				total_cards += run[i].length;
			
			for (int i = 0; i < run.length; i++)
			{
				sort(run[i]);
				int ret = this.isSequence(run[i]);
				if (ret >= 0)
				{
					sequences++;
					if (ret == 0)
						pure = true;
				}
				else if (this.isSet(run[i]))
					sets++;
				else
					points += getPoints(run[i]);
			}
			
			if (!pure)
			{
				// No Pure sequence so its all lost
				points = getPoints(show_cards.getHand());
				System.out.printf("You've lost with %d points\n", points);
			}
			else if (total_cards < Rummy.max_cards)
			{
				// Not all cards are in the show, so, its a lost run
				points += getPoints(show_cards.getCardsNotShown());
				System.out.printf("You've lost with %d sets, %d sequences resuting in %d points\n", sets, sequences, points);
			}
			else if ((points == 0) && (sequences > 1))
			{
				Card[] close_cards = show_cards.getCardsNotShown();
				if ((close_cards != null) && (close_cards.length == 1))
				{
					System.out.printf("You've won with %d sets and %d sequences\n", sets, sequences);
					show_cards.getHand().removeCard(close_cards[0]);
					throw_pile.push((Card) close_cards[0]);
				}
			}
		}
		else
		{
			points = getPoints(show_cards.getHand());
			System.out.printf("You've lost with %d points\n", points);
		}
		
		return points;
	}

	protected class RummyControl implements CardGame.GameControl
	{
		private PlayerControl p;
		private Hand hand;
		private PlayerState player_state;
		private Semaphore game_wait;
		
		RummyControl(PlayerControl p)
		{
			this.player_state = PlayerState.DEALING;
			this.p = p;
			this.hand = getEmptyHand();
			this.game_wait = new Semaphore(0);
		}
		
		private void startTurn()
		{
			player_state = PlayerState.EXPECTING_DRAW;
		}
		
		private void waitForGame()
		{
			try {
				this.game_wait.acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void signalGameOver()
		{
			if (this.game_wait.hasQueuedThreads())
				this.game_wait.release();
		}
		
		public PlayerControl getPlayerControl()
		{
			return p;
		}
		
		public Hand getHand()
		{
			return this.hand;
		}
		
		private void dealtCard(Card c)
		{
			try 
			{
				hand.add(c);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			p.addCard(c);
		}
		
		public Card draw(boolean from_throwpile)
		{
			synchronized(Rummy.this)
			{
				if (Rummy.this.getCurrentPlayer() != p.getPlayer())
					return null;
			}
			
			if ((player_state != PlayerState.EXPECTING_DRAW) && (player_state != PlayerState.DEALING))
				return null;
			
			Card c = null;
			if (!hand.holdingExtraCards())
			{
				if (from_throwpile)
					c = throw_pile.pop();
				else
					c = game_deck.draw();
				
				if (from_throwpile)
					Rummy.this.sendEventToPlayers(new CardEvent(RummyEvents.DRAW_FROM_PILE, c), Rummy.this.current_player);
				else
					Rummy.this.sendEventToPlayers(new RummyEvent(RummyEvents.DRAW_FROM_DECK), Rummy.this.current_player);

				try {
					hand.add(c);
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
			
			if (player_state != PlayerState.DEALING)
				player_state = PlayerState.EXPECTING_DISCARD;
			return c;
		}
		
		private int leave()
		{
			int ret = 0;
			// Resigning from the game?
			ret = getPoints(this.hand);
			player_state = PlayerState.TURN_OVER;
			return ret;
		}
		
		private int leave(Card [][] run)
		{
			int ret = 0;
			if (run == null)
				ret = getPoints(this.hand);
			else
			{
				Hand.Show show = this.hand.new Show(run);
				ret = show(show);
			}
			player_state = PlayerState.TURN_OVER;
			return ret;
		}

		@Override
		public boolean discard(Card t) 
		{
			boolean ret = false;
			if ((hand.holdingExtraCards()) && (player_state == PlayerState.EXPECTING_DISCARD))
			{
				Rummy.this.sendEventToPlayers(new CardEvent(RummyEvents.CARD_DISCARDED, t), -1);
				if (this.hand.removeCard(t))
					throw_pile.push((Card)t);
				player_state = PlayerState.TURN_OVER;
				ret = true;
			}
			return ret;
		}
		
		@Override
		public void resign()
		{
			// Out of turn resignation of a player
			Rummy.this.resign(this, leave());
		}
		
		public void finishTurn()
		{
			if ((hand.holdingExtraCards()) && (player_state == PlayerState.EXPECTING_DISCARD))
			{
				// Pick a card to discard
				Card t = hand.getCard((int)(Math.random() * hand.size()));
				Rummy.this.sendEventToPlayer(this, new CardEvent(RummyEvents.FORCED_DISCARD, t));
				discard(t);
			}
		}
		
		@Override
		public void start() 
		{
			Rummy.this.start(this);
		}

		@Override
		public void sortHand() 
		{
			Rummy.this.sort(hand);
		}

		@Override
		public boolean isValid() 
		{
			return true;
		}		
	}
	
}

