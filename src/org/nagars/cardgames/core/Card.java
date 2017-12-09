package org.nagars.cardgames.core;

public class Card //implements Comparable<Card>
{
	public static enum Suite
	{
		CLUB('\u2663'), SPADE('\u2660'), DIAMOND('\u2666'), HEART('\u2665');
		
		private final char symbol;
		
		private Suite(char sym) { symbol = sym; }
		
		public String toString() { return Character.toString(symbol); }
		
	};
	
	public static enum Value
	{
		ACE("A "), TWO("2 "), THREE("3 "), FOUR("4 "), FIVE("5 "), SIX("6 "), SEVEN("7 "), EIGHT("8 "), NINE("9 "), TEN("10"), JACK("J "), QUEEN("Q "), KING("K ");
		
		private final String value;
		
		private Value(String val) { value = val; }
				
		public String toString() { return value; }
	};
	
	public static enum Joker
	{
		BLACK, RED
	};
	
	private final Value val;
	private final Suite s;
	private final boolean joker;
	private final Joker jval;
	
	public Card(Suite s, Value val)
	{
		this.s = s;
		this.val = val;
		// Joker values are set to dummy
		jval = Joker.BLACK;
		joker = false;
	}

	public Card(Joker j)
	{
		joker = true;
		jval = j;
		// Dummy values to compensate for warnings about final variables
		this.s = Suite.CLUB;
		this.val = Value.ACE;
	}
	
	public boolean equals(Card c)
	{
		boolean ret = false;
		if (this.joker)
		{
			if (c.joker && (this.jval == c.jval))
				ret = true;
		}
		else if ((!c.joker) && (this.s == c.s) && (this.val == c.val))
			ret = true;
		return ret;
	}
	
	public Value getValue()
	{
		return val;
	}
	
	public boolean isJoker()
	{
		return joker;
	}
	
	public Joker getJokerType()
	{
		return jval;
	}
	
	public Suite getSuite()
	{
		return s;
	}

	public String toString()
	{
		if (!this.joker)
			return this.s.toString() + ":" + this.val.toString();
		else
			return "<J>";
	}

 }
