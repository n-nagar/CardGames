package org.nagars.cardgames.gui;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.SwingUtilities;
import javax.swing.plaf.LayerUI;

import org.nagars.cardgames.core.Card;
import org.nagars.cardgames.core.CardGame;
import org.nagars.cardgames.core.Hand;
import org.nagars.cardgames.core.Hand.Show;

public class GUIHand
{
	public interface UserInputHandler
	{
		public boolean handleDiscard(int card_idx);
		public boolean handleSort();
		public Hand getPlayHand();
	}
	
	private Container ui;
	private Rectangle location;
	private UserInputHandler user_response;
	private boolean is_active;
	private GUICard[] this_hand;
	private int card_total;
	private Hand main_hand;
	private Show active_show;
	private Rectangle throw_area;
	public static final Color highlights[] = {Color.RED, Color.BLUE, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.YELLOW};

	
    private JLayer<JComponent> createLayer(GUICard c) {
        // This custom layerUI will fill the layer with translucent green
        // and print out all mouseMotion events generated within its borders
    	GUIHandLayerUI layerUI = new GUIHandLayerUI(); 
        // create the layer for the panel using our custom layerUI
        JLayer<JComponent> jl = new JLayer<JComponent>(c.getPanel(), layerUI);
        c.getPanel().setLocation(0, 0);
        return jl;
    }

	public GUIHand(Container c, Rectangle loc, Rectangle throw_area, UserInputHandler uh)
	{
		this.user_response = uh;
		this.is_active = false;
		this.throw_area = throw_area;
		main_hand = uh.getPlayHand();
		if (main_hand != null)
			this_hand = new GUICard[main_hand.maxCards()];
		this.card_total = 0;
		this.ui = c;
		this.active_show = null;
		this.location = loc;
		this.location.width = (this_hand.length - 1) * 15 + GUICard.getPreferredSize().width;
		this.location.height = GUICard.getPreferredSize().height;
	}

	public void add(Card c) throws Exception
	{
		if (main_hand.size() != (card_total+1))
			System.out.println("Hand does not match GUIHand in GUIHand.add");
		GUICard gc = new GUICard(c);
		this_hand[card_total++] = gc;

		JLayer<JComponent> jl = createLayer(gc);
		jl.setSize(GUICard.getPreferredSize());
		this.ui.add(jl);
		jl.setLocation(this.location.x + ((this.card_total-1) * 15), this.location.y);
		this.ui.setComponentZOrder(jl, 0);
	}
	
	private void move(int src, int dst)
	{
		GUICard s = this.this_hand[src];
		this_hand[dst] = s;
		s.getPanel().getParent().setLocation(this.location.x + (dst * 15), this.location.y);
	}

	private boolean swap(int src, int dst, boolean deep)
	{
		if (deep)
			deep = this.main_hand.swap(src, dst);
		else
			deep = true;
		if (deep)
		{
			GUICard d = this.this_hand[dst];
			GUICard s = this.this_hand[src];
			this.this_hand[src] = d;
			this.this_hand[dst] = s;
			Point t = d.getPanel().getParent().getLocation();
			Point q = s.getPanel().getParent().getLocation();
			d.getPanel().getParent().setLocation(q);
			s.getPanel().getParent().setLocation(t);
			return true;
		}
		return false;
	}

	private int locateCard(Card c)
	{
		int ret = -1;
		for (int k = 0; k < this.this_hand.length; k++)
			if (this_hand[k].getCard() == c)
			{
				ret = k;
				break;
			}
		return ret;
	}
	
	private int getCardPosition(GUICard c)
	{
		int ret = -1;
		for (int k = 0; k < this.this_hand.length; k++)
			if (this_hand[k] == c)
			{
				ret = k;
				break;
			}
		return ret;
	}
	
	public void setActive(boolean val)
	{
		this.is_active = val;
	}
	
	public void sort()
	{
		if (this.user_response == null)
			return;
		
		if (main_hand.size() != card_total)
			System.out.println("Hand does not match GUIHand in GUIHand.sort");
		this.user_response.handleSort();
		
		int max = this.card_total - 1;
		for (int i = max; i > 0; i--)
		{
			int src = locateCard(this.main_hand.getCard(i));
			
			// Locate this card in this hand
			if ((src >= 0) && (src != i))
				this.swap(src, i, false);
			this.ui.setComponentZOrder(this_hand[i].getPanel().getParent(), max-i);
		}
	}
	
	public Show startShow()
	{
		this.active_show = this.main_hand.startShow();
		return this.active_show;
	}
	
	public void clearShow()
	{
		// Clear the highlights from all cards
		Card[][] c = active_show.get();
		
		for (int i = 0; i < c.length; i++)
		{
			for (int j = 0; j < c[i].length; j++)
			{
				int k = this.locateCard(c[i][j]);
				if (k >= 0)
				{
					GUICard gc = this_hand[k];
					JLayer jl = (JLayer)gc.getPanel().getParent();
					if (jl != null)
					{
						((GUIHandLayerUI)jl.getUI()).clearShow();
						jl.repaint();
					}
				}
			}
		}
		active_show = null;
		main_hand.clearShow();
	}
	
	public boolean removeCard(int idx)
	{
		if ((idx < 0) || (idx >= this.card_total))
			return false;
		
		if (main_hand.size() != (card_total - 1))
			System.out.println("Hand does not match GUIHand in GUIHand.removeCard");
		
		GUICard t = this_hand[idx];
		if (t != null)
		{
			JLayer<JComponent> jl = (JLayer<JComponent>) t.getPanel().getParent();
			if (jl != null)
			{
				Rectangle r = jl.getBounds();
				jl.remove(t.getPanel());
				if (jl.getParent() != null)
				{
					Container p = jl.getParent();
					p.remove(jl);
					p.repaint(r.x, r.y, r.width, r.height);
				}
				
				// Shift everything one back
				for (int i = idx; i < this.card_total - 1; i++)
					this.move(i+1, i);				
			}
			else
				System.out.println("Did not find JLayer in removeCard");
			this.card_total--;
			return true;
		}
		return false;
	}

	public void printCards()
	{
		main_hand.printCards();
		System.out.print(" ");
		for (int i = 0; i < card_total; i++)
			System.out.printf("%3d  ", i);
		System.out.printf("\n<");
		for (int i = 0; i < card_total; i++)
		{
			System.out.print(this_hand[i].getCard());
			if (i < card_total-1)
				System.out.print(",");
		}
		System.out.println(">");
	}

	private class GUIHandLayerUI extends LayerUI<JComponent>
	{
		private static final long serialVersionUID = 1;
		int dx, dy = 0;
		boolean selected = false;
		int show_idx = -1;
		Point start_loc;
		int start_idx;

		public void clearShow()
		{
			show_idx = -1;
		}
		
		public void paint(Graphics g, JComponent c) {
	        // paint the layer as is
	        super.paint(g, c);
	        // fill it with the translucent green
	        if ((selected) || (show_idx >= 0))
	        {
	        	Graphics2D g2 = (Graphics2D) g.create();

	            int w = c.getWidth();
	            int h = c.getHeight();
	            g2.setComposite(AlphaComposite.getInstance(
	                    AlphaComposite.SRC_OVER, .5f));
	            if (show_idx >= 0)
	            {
	            	int i = show_idx % GUIHand.highlights.length;
	            	g2.setPaint(GUIHand.highlights[i]);                    	
	            }
	            else
	            	g2.setPaint(new GradientPaint(0, 0, Color.yellow, 0, h, Color.red));
	            g2.fillRect(0, 0, w, h);

	            g2.dispose();
	        }
	    }

	    public void installUI(JComponent c) {
	        super.installUI(c);
	        // enable mouse motion events for the layer's subcomponents
	        ((JLayer) c).setLayerEventMask(AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
	    }

	    public void uninstallUI(JComponent c) {
	        super.uninstallUI(c);
	        // reset the layer event mask
	        ((JLayer) c).setLayerEventMask(0);
	    }

	    // overridden method which catches MouseMotion events
	    public void eventDispatched(AWTEvent e, JLayer<? extends JComponent> l) 
	    {
			MouseEvent m = (MouseEvent)e;
			
			if ((m.getID() == MouseEvent.MOUSE_DRAGGED) && (!main_hand.showMode()))
			{
				l.setLocation(m.getLocationOnScreen().x - dx, m.getLocationOnScreen().y - dy);
			}
			else if (m.getID() == MouseEvent.MOUSE_PRESSED)
			{
				start_idx = getCardPosition(((GUICard.CardPanel)l.getComponent(1)).getCard());
				if (!main_hand.showMode())
				{
	    			start_loc = l.getLocation();
	    			((JComponent)m.getSource()).getParent().setComponentZOrder((JComponent)m.getSource(), 0);
	    			dx = m.getLocationOnScreen().x - l.getX();
	    			dy = m.getLocationOnScreen().y - l.getY();
	     			selected = true;
	    			l.repaint();
				}
				else
				{
					if (show_idx < 0)
					{
	    				show_idx = GUIHand.this.active_show.getCurrentIndex();
	    				GUIHand.this.active_show.add(start_idx);
					}
					else
					{
						show_idx = -1;
						GUIHand.this.active_show.remove(start_idx);
					}
					l.repaint();
				}
			}
			else if (m.getID() == MouseEvent.MOUSE_RELEASED)
			{
				if (!main_hand.showMode())
				{
	    			// Get the direction of motion
	    			int direction = (l.getX() - start_loc.x);
	    			direction = (direction < 0)?-1:1;
	    			
	    			// Is the mouse out of the location of the hand of cards?
	    			Point cur_loc = m.getLocationOnScreen();
	    			SwingUtilities.convertPointFromScreen(cur_loc, l.getParent());
	    			if (GUIHand.this.location.contains(cur_loc))
	    			{
	    				// Check where top left corner of the card
	    				//  is at to move
	        			cur_loc = m.getLocationOnScreen();
	        			cur_loc.x = l.getLocationOnScreen().x;
	        			SwingUtilities.convertPointFromScreen(cur_loc, l.getParent());
	        			Dimension dm = GUICard.getPreferredSize();

	        			// Search the hand for the card which is below this card
	        			int insert_idx = -1;
						for (int i = 0; i < card_total; i++)
						{
							if (i != start_idx)
							{
								Point p1 = this_hand[i].getPanel().getParent().getLocation();
								Point p2;
								if (i == (card_total - 1))
									p2 = new Point(p1.x + dm.width, p1.y);
								else
									p2 = this_hand[i+1].getPanel().getParent().getLocation();
									
								if ((cur_loc.x >= p1.x) && (cur_loc.x < p2.x /* dm.width */) &&
									(cur_loc.y >= p1.y) && (cur_loc.y <= p1.y + dm.height))
								{
									insert_idx = i;
									break;
								}
							}
							
						}

						// First move the card back to where it came from
	    				l.setLocation(start_loc);
						// If we are able to determine the location of drop, move there
	        			if (insert_idx > -1)
	        			{
	        				GUICard c = null;
	        				if ((direction < 0) && (insert_idx < card_total))
	        					c = this_hand[insert_idx];
	        				else if ((insert_idx+1) < card_total)
	        					c = this_hand[insert_idx + 1];
	        					
	        				if (c != null)
	        				{
	        					// Make the z-order of the dragged card the same as the place you are
	        					//  inserting into
	        					int zorder = l.getParent().getComponentZOrder(c.getPanel().getParent());
	        					l.getParent().setComponentZOrder(l, zorder);
	        				}
	        				int prev_idx = start_idx;
	        				for (int i = start_idx + direction; ((direction < 0) && (i >= insert_idx)) || ((direction > 0) && (i <= insert_idx)); i += direction)
	    					{
	        					GUIHand.this.swap(i, prev_idx, true);
	    						prev_idx = i;
	    					}
	        			}
	        			else
	        			{
	        				/* 
	        				 * Cannot find a spot so put it back and in its z-order 
	        				 * If it is the last card, then the z-order is just right
	        				 * */
	        				if (start_idx < (card_total - 1))
	        				{
	        					int zorder = l.getParent().getComponentZOrder(this_hand[start_idx+1].getPanel().getParent());
	        					l.getParent().setComponentZOrder(l, zorder);        					
	        				}
	        			}
	    			}
	    			else
	    			{
						// First move the card back to where it came from
	    				l.setLocation(start_loc);
        				if (start_idx < (card_total - 1))
        				{
        					int zorder = l.getParent().getComponentZOrder(this_hand[start_idx+1].getPanel().getParent());
        					l.getParent().setComponentZOrder(l, zorder);        					
        				}

	    				// Interpret the user gesture of moving out of bounds as wanting
	    				//  to discard the card if they are holding extra cards
	    				if (is_active && (throw_area.contains(cur_loc)) && (main_hand.holdingExtraCards()))
	    				{
	    					if (user_response != null)
	    						GUIHand.this.user_response.handleDiscard(start_idx);
	    				}
	    			}
			
	    			dx = dy = 0;
	    			selected = false;
	    			l.repaint();
				}
			}
		}	
	}
}

