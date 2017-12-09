package org.nagars.cardgames.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Stack;

import javax.swing.JPanel;

import org.nagars.cardgames.core.CardGame;


public class GUIThrowPile
{
	public interface UserInputHandler
	{
		public boolean handlePileDraw();
	}
	
	private JPanel pile_ui;
	private Stack<GUICard> this_pile;
	private Container parent_frame;
	private Rectangle deck_location;
	private GUICard empty_card;
	private int direction = 2;
	private boolean game_over = false;
	private CardGame.GameControl current_player;
	private boolean is_active;
	private UserInputHandler user_response;
	
	public GUIThrowPile(Container c, Rectangle loc, UserInputHandler uh)
	{
		current_player = null;
		is_active = false;
		user_response = uh;
		this_pile = new Stack<GUICard>();
		game_over = false;
		pile_ui = new JPanel(null);
		pile_ui.setSize(loc.width, loc.height);
		pile_ui.setOpaque(false);
		
		parent_frame = c;
		deck_location = loc;
		parent_frame.add(pile_ui);
		pile_ui.setLocation(loc.x, loc.y);
		
		empty_card = new GUICard();
		empty_card.setState(false);
        JPanel jp = empty_card.getPanel();
        Dimension cd = jp.getPreferredSize();
        int x = ((loc.width - cd.width)/2);
        x = (x < 0)?0:x;
        
        int y = ((loc.height - cd.height)/2);
        y = (y < 0)?0:y;
        
        pile_ui.add(jp);
        jp.setLocation(x, y);

	    pile_ui.addMouseListener(new MouseAdapter() {
	      public void mousePressed(MouseEvent e) 
	      {
	    	  if (is_active && (user_response != null))
	          {
	    		  user_response.handlePileDraw();
	          }
	      }
	  	});
	}
	
	public void setActive(boolean val)
	{
		this.is_active = val;
	}
		
	public void discard(GUICard c, boolean game_over)
	{
		int x = 0, y = 0;

		this.game_over = game_over;
		if (!this_pile.isEmpty())
		{
			Rectangle top_cr = this_pile.peek().getPanel().getBounds();
			if (((top_cr.x + direction + top_cr.width) > this.deck_location.width) || ((top_cr.x + direction) < 0))
				direction *= -1;

			x = top_cr.x + direction;
			y = top_cr.y + direction;
		}
		
		this_pile.push(c);
		if (game_over)
			c.setState(true);
		JPanel jp = ((GUICard)c).getPanel();
		pile_ui.add(jp);
		jp.setLocation(x, y);
		pile_ui.setComponentZOrder(jp, 0);
		pile_ui.repaint();
	}
	
	public void discard(GUICard c)
	{
		discard(c, false);
	}
	
	public GUICard draw()
	{
		GUICard c = this_pile.pop();

		if (c != null)
		{
			JPanel jp = ((GUICard)c).getPanel();
			pile_ui.remove(jp);
			pile_ui.repaint();
		}
		return c;
	}
}
