package org.nagars.cardgames.core;

public interface CardFactory<T extends Card>
{
	public T newCard(Card.Suite s, Card.Value v);
	public T newCard(Card.Joker j);
}