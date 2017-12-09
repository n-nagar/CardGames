package org.nagars.cardgames.core;

import java.util.ArrayList;

public class Hand 
{
	public class Show
	{
		private ArrayList<ArrayList<Card>> show_cards;
		private ArrayList<Card> current;
		private boolean[] showing_cards;
		private boolean show_valid;

		private void initialize()
		{
			showing_cards = new boolean[Hand.this.mycards.length];
			show_cards = new ArrayList<ArrayList<Card>>();
			current = new ArrayList<Card>();
			show_valid = true;
		}
		Show()
		{
			initialize();
		}
		
		Show(Card[][] run)
		{
			initialize();
			if (run != null)
			{
				// Validate if the run given belongs in this hand
				boolean used[] = new boolean[Hand.this.mycards.length];
				for (int i = 0; i < used.length; i++)
					used[i] = false;
				
				for (int i = 0; (i < run.length) && show_valid ; i++)
				{
					for (int j = 0; j < run[i].length; j++)
					{
						boolean found = false;
						ArrayList<Integer> pos = Hand.this.getCardPosition(run[i][j]);
						for (Integer k : pos)
						{
							if (!used[k])
							{
								used[k] = true;
								this.add(k);
								found = true;
							}
						}
						if (!found)
						{
							// Was unable to locate this card in the hand
							// Mark this show as invalid
							show_valid = false;
							break;
						}
					}
					System.out.println();
					this.newGroup();
				}
				
				if (!show_valid)
				{
					System.out.println("Not a valid run in constructor");
					show_cards.clear();
				}
			}
		}
		
		public void add(int idx)
		{
			if (show_valid)
			{
				Card c = Hand.this.getCard(idx);
				if (c != null)
				{
					showing_cards[idx] = true;
					current.add(c);
				}
			}
		}
		
		public void remove(int idx)
		{
			if (!show_valid)
				return;
			
			Card c = Hand.this.getCard(idx);
			if (c != null)
			{
				if (!current.remove(c))
				{
					for (int i = 0; i < show_cards.size(); i++)
						if (show_cards.get(i).remove(c))
						{
							showing_cards[idx] = false;
							break;
						}
				}
				else
					showing_cards[idx] = false;
			}
		}
		
		public void newGroup()
		{
			if (!show_valid)
				return;
			if (current.size()>0)
			{
				show_cards.add(current);
				current = new ArrayList<Card>();
			}
		}
		
		public Card[] getCardsNotShown()
		{
			int len = showing_cards.length;
			for (int i = 0; i < showing_cards.length; i++)
				if (showing_cards[i])
					len--;
			
			Card c[] = new Card[len];
			int i_idx = 0;
			for (int i = 0; i < showing_cards.length; i++)
			{
				if (!showing_cards[i])
					c[i_idx++] = Hand.this.mycards[i];
			}
			return c;
		}
		
		public final Card[][] get()
		{
			newGroup();
			int len = show_cards.size();
			for (int i = 0; i < show_cards.size(); i++)
				if (show_cards.get(i).size() == 0)
					len--;
			
			Card[][] x = new Card[len][];
			for (int i = 0; i < x.length; i++)
			{
				len =show_cards.get(i).size();
				if (len > 0)
				{
					x[i] = new Card[len];
					ArrayList<Card> y = show_cards.get(i);
					for (int j = 0; j < len; j++)
					{
						x[i][j] = y.get(j);
					}
				}
			}
			return x;
		}
		
		public final Hand getHand()
		{
			return Hand.this;
		}
		
		public int getCurrentIndex()
		{
			return show_cards.size();
		}
		
		public void print()
		{
			for (int i = 0; i < show_cards.size(); i++)
			{
				System.out.print("[");
				for (int j = 0; j < show_cards.get(i).size(); j++)
					System.out.printf("%s, ", show_cards.get(i).get(j));
				System.out.println("]");
			}
			if (this.current != null)
			{
				System.out.print("[");
				for (int j = 0; j < current.size(); j++)
					System.out.printf("%s, ", current.get(j));
				System.out.println("]");
			}
			
			Card[] ns = this.getCardsNotShown();
			if ((ns == null) || (ns.length == 0))
				System.out.println("All cards used");
			else
			{
				System.out.println("Unused cards:");
				System.out.print("[");
				for (int j = 0; j < ns.length; j++)
					System.out.printf("%s, ", ns[j]);
				System.out.println("]");
			}
		}
	}
	
	private Card[] mycards;
	private Show show_cards;
	private int total_cards;
	private int extra_cards;
	
	// Hold card are card(s) that can be held while 
	//  it is the players turn but must be discarded
	//  by the end of the turn to get back to the
	//  standard hand size of num_cards
	public Hand(int num_cards, int num_hold)
	{
		if (num_cards < 0)
			num_cards = 0;
		if (num_hold < 0)
			num_hold = 0;
		mycards = new Card[num_cards + num_hold];
		total_cards = 0;
		extra_cards = num_hold;
	}
	
	/*
	 * Add a card at position i. Discard previous card in
	 * that position
	 */
	public int add(Card c)
	{
		if (total_cards >= mycards.length)
			return -1;
		
		mycards[total_cards] = c;
		return total_cards++;
	}
	
	public int size()
	{
		return total_cards;
	}
	
	public int maxCards()
	{
		return mycards.length;
	}
	
	public boolean holdingExtraCards()
	{
		return (total_cards > (mycards.length - extra_cards));
	}

	/*
	 * Move the card in position src to dst
	 * shifting cards along the way
	 */
	public boolean swap(int src, int dst)
	{
		if ((total_cards == 0) || (src < 0) || (src >= mycards.length) || (dst < 0) || (dst >= mycards.length))
			return false;
		
		Card c = mycards[dst];
		mycards[dst] = mycards[src];
		mycards[src] = c;
		return true;
	}

	public void clear()
	{
		for (int i = 0; i < total_cards; i++)
			mycards[i] = null;
		total_cards = 0;
	}
	
	private ArrayList<Integer> getCardPosition(Card c)
	{
		ArrayList<Integer> pos = new ArrayList<Integer>();
		if (c == null)
			return pos;
		for (int i = 0; i < total_cards; i++)
		{
			if (mycards[i].equals(c))
			{
				pos.add(i);
			}
		}
		return pos;
	}
	
	public final Card getCard(int i)
	{
		if ((i < 0) || (i >= total_cards))
			return (Card)null;
		
		return (Card)mycards[i];
	}
	
	public boolean removeCard(Card t)
	{
		ArrayList<Integer> pos = getCardPosition(t);
		if (pos.size() > 0)
		{
			int max = size();
			for (int i = pos.get(0); i < max - 1; i++)
			{
				swap(i, i+1);
			}
			total_cards--;
		}
		return true;
	}

	public boolean showMode()
	{
		return (show_cards != null);
	}
	
	public Show startShow()
	{
		show_cards = new Show();
		return show_cards;
	}
	
	public void clearShow()
	{
		show_cards = null;
	}
	
	public void printCards()
	{
		System.out.print(" ");
		for (int i = 0; i < total_cards; i++)
			System.out.printf("%3d  ", i);
		System.out.printf("\n[");
		for (int i = 0; i < total_cards; i++)
		{
			System.out.print(mycards[i]);
			if (i < total_cards-1)
				System.out.print(",");
		}
		System.out.println("]");
	}
}
