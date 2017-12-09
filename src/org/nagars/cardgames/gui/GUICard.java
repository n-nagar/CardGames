package org.nagars.cardgames.gui;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.nagars.cardgames.core.Card;


public class GUICard 
{
	private Card this_card;
	private static BufferedImage images = null;
	private static final Dimension size = new Dimension(79,123);
	private JPanel card_panel;
	private boolean no_card;
	private boolean facedown;

	public GUICard(Card c) 
	{
		this_card = c;
		no_card = false;
		facedown = false;
		loadImage();
		card_panel = new CardPanel(this);
	}

	public GUICard()
    {
            no_card = true;
            facedown = true;
            loadImage();
            card_panel = new CardPanel(this);
    }
    
	public Card getCard()
	{
		return this_card;
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
		case ACE: return 1;
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
	
	public void setState(boolean facedown)
	{
		this.facedown = facedown;
	}
	
	public static final Dimension getPreferredSize()
	{
		return size;
	}
	
	public JPanel getPanel()
	{
		return card_panel;
	}
	
    private Image getImage() 
    {
    	if (images == null)
    		return null;
    	
    	int cx;
    	int cy;
    	
    	if (facedown)
    	{
    		// Facedown card in the image
    		cy = 4*123;
    		cx = 2*79;
    	}
    	else if (no_card)
    	{
    		cy = 4 * 123;
    		cx = 3 * 79;
    		return images.getSubimage(cx, cy, 79, 123);
    	}
    	else
    	{
    		if (this_card.isJoker())
    		{
    			cy = 4 * 123;
    			if (this_card.getJokerType() == Card.Joker.BLACK)
    				cx = 79;
    			else
    				cx = 0;
    		}
    		else
    		{
    			cy = getIndex(this_card.getSuite()) * 123;
    			cx = (getIndex(this_card.getValue()) - 1) * 79;
    		}    		
    	}
		return images.getSubimage(cx, cy, 79, 123);
    }
    
	private void loadImage() 
	{
		synchronized(this)
		{
			if (images == null)
			{
				//ClassLoader cl = Card.class.getClassLoader();
				try
				{
					images = ImageIO.read(getClass().getResourceAsStream("/Resources/cards.png"));
				} catch(IOException e)
				{
					System.out.println(e);
					images = null;
				}
			}
		}
	}

	public class CardPanel extends JPanel 
	{
		private GUICard card;
		private static final long serialVersionUID = 1;
		
		public CardPanel(GUICard c)
		{
			this.card = c;
			this.setSize(this.getPreferredSize());
			this.repaint();
		}
		
		public Dimension getPreferredSize()
		{
			return GUICard.getPreferredSize();
		}
		
		public final GUICard getCard()
		{
			return card;
		}
	    public void paintComponent(Graphics g) {
	        super.paintComponent(g);       

	    	g.drawImage(card.getImage(), 0, 0, GUICard.getPreferredSize().width, GUICard.getPreferredSize().height, null);
	    }  
	}
}
