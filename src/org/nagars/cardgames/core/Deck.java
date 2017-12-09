package org.nagars.cardgames.core;
import java.util.Stack;

public class Deck 
{
	private Stack<Card> cards;
	private Stack<Card> dealt_cards;
	private int num_decks;
	
	public Card createCard(Card.Suite s, Card.Value v)
	{
		return (Card)new Card(s, v);
	}
	
	public Card createCard(Card.Joker j)
	{
		return (Card)new Card(j);
	}
	
	private void initialize(int jokers)
	{
		if (jokers < 0)
			jokers = 0;
		// Allocate enough space for # of suites * # of cards + # of jokers. 
		cards = new Stack<Card>();
		dealt_cards = new Stack<Card>();
		
		for (Card.Suite s : Card.Suite.values())
		{
			for (Card.Value v : Card.Value.values())
			{
				cards.push(createCard(s, v));
			}
		}

		Card.Joker j = Card.Joker.BLACK;
		while (jokers-- > 0)
		{
			cards.push(createCard(j));
			if (j == Card.Joker.BLACK)
				j = Card.Joker.RED;
		}
		num_decks = 1;			
	}
	
	public Deck(int jokers)
	{
		initialize(jokers);
	}
	
	public Deck()
	{
		initialize(2);
	}
	
	// Add cards from another deck
	//  making this a larger deck
	public void add(Deck d)
	{
		cards.addAll(d.cards);
		num_decks++;
	}
	
	public int getTotalDecks()
	{
		return num_decks;
	}
	
	public void shuffle()
	{
		for (int i = cards.size() - 1; i >= 0; i--)
		{
			int k = (int) (Math.random() * i);
			Card temp = cards.set(k, cards.elementAt(i));
			cards.set(i, temp);
		}
	}
	
	public Card draw(boolean random)
	{
		if (!cards.empty())
		{
			if (!random)
				return dealt_cards.push(cards.pop());
			else
			{
				int idx = (int)(Math.random() * cards.size());
				return dealt_cards.push(cards.get(idx));
			}
		}
		else
			return null;
	}
	
	public Card draw()
	{
		return draw(false);
	}
	
	public int size()
	{
		return cards.size();
	}
	
	public void reset()
	{
		while (!dealt_cards.isEmpty())
			cards.push(dealt_cards.pop());
	}
	
	public String toString()
	{
		String s = "[ ";
		boolean first = true;
		
		for (Card c : cards)
		{
			if (!first)
				s = s + ", ";
			else
				first = false;
			s = s + c.toString();
		}
		s = s + "]";
		return s;
	}
	
	public static void main(String args[])
	{
		Deck d = new Deck();
		
		System.out.println(d);
		d.shuffle();
		System.out.println();
		System.out.println(d);
	}
}
