package org.nagars.cardgames.gui;


import java.awt.Color;
import org.nagars.cardgames.multi.MultiRummy;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.nagars.cardgames.core.Card;
import org.nagars.cardgames.core.CardGame;
import org.nagars.cardgames.core.CardGame.GameEvent;
import org.nagars.cardgames.core.CardGame.TurnState;
import org.nagars.cardgames.core.Hand;
import org.nagars.cardgames.core.Player;
import org.nagars.cardgames.core.Rummy;
import org.nagars.cardgames.core.Rummy.PlayerEvent;
import org.nagars.cardgames.core.Rummy.RummyEvents;

public class GUIRummy implements CardGame.PlayerControl, ActionListener, 
						GUIHand.UserInputHandler, GUIThrowPile.UserInputHandler
{
	private GUITable table;
	private String table_name;
	private RummyPlayerUI[] players_ui;
	private CardStack game_deck;
	private GUICard game_joker;
	private GUIThrowPile throw_pile;
	private Rectangle throw_area;
	private int current_player;
	private RummyPlayerUI active_player;
	private Container ui;
	private GUIHand hand;
	private Hand.Show show_cards;
	private JButton sort_btn;
	private JButton declare_btn;
	private JButton leave_btn;
	private MultiRummy game;
	private CardGame.GameControl game_controller;
	private Semaphore turn_sem;
	private TurnState turn_ret;
	private boolean playing_turn;

	public GUIRummy(GUITable table, String table_name)
	{
		this.table = table;
		this.table_name = table_name;
		game = new MultiRummy();
		initUI();
		this.game_controller = game.join(this);
		if (this.game_controller != null)
		{
			if (this.game_controller.isValid())
				this.table.updateGameInfo("Waiting for players");
			else
				this.table.updateGameInfo("Game did not start");
		}
		else
			this.table.updateGameInfo("Game did not start");
	}
	
	public GUIRummy(GUITable table, String server_connect_str, String table_name)
	{
		this.table = table;
		this.table_name = table_name;
		game = new MultiRummy();
		initUI();
		this.game_controller = game.join(server_connect_str, this);
		if (this.game_controller != null)
		{
			if (this.game_controller.isValid())
				this.table.updateGameInfo("Joined game");
			else
				this.table.updateGameInfo("Game did not start");
		}
		else
			this.table.updateGameInfo("Game did not start");
	}
	
	private void initUI()
	{
		
		if (this.table == null)
			return;

		this.ui = this.table.getGameUI(this.table_name);
		this.turn_sem = new Semaphore(0);
		this.playing_turn = false;
		this.table.setGameInfo(game.getName() + " - " + game.getConnectString(), game.getName() + " ready");
		players_ui = new RummyPlayerUI[4];
		players_ui[0] = new RummyPlayerUI(ui, new Rectangle(249, 10, 48, 48));
		players_ui[1] = new RummyPlayerUI(ui, new Rectangle(305, 10, 48, 48));
		players_ui[2] = new RummyPlayerUI(ui, new Rectangle(249, 82, 48, 48));
		players_ui[3] = new RummyPlayerUI(ui, new Rectangle(305, 82, 48, 48));
		current_player = 0;
		
		Rectangle loc = new Rectangle(5, 10, 95, 135);
		game_deck = new CardStack(loc);
		ui.add(game_deck);
		game_deck.setLocation(loc.x, loc.y);
		
		throw_area = new Rectangle(147, 10, 95, 135); 
		throw_pile = new GUIThrowPile(ui, throw_area, this);


		sort_btn = new JButton("Sort");
		sort_btn.setFont(new Font("sans", Font.BOLD, 10));
		sort_btn.setSize(new Dimension(70, 30));
		sort_btn.setActionCommand("SORT");
		sort_btn.addActionListener(this);
		this.ui.add(sort_btn);
		sort_btn.setLocation(283, 150);
		
		declare_btn = new JButton("Show");
		declare_btn.setFont(new Font("sans", Font.BOLD, 10));				
		declare_btn.setSize(new Dimension(70, 30));
		declare_btn.setActionCommand("SHOW");
		declare_btn.addActionListener(this);
		this.ui.add(declare_btn);
		declare_btn.setLocation(283, 185);
		
		leave_btn = new JButton("Leave");
		leave_btn.setFont(new Font("sans", Font.BOLD, 10));
		leave_btn.setSize(new Dimension(70, 30));
		leave_btn.setActionCommand("LEAVE");
		leave_btn.addActionListener(this);
		this.ui.add(leave_btn);
		leave_btn.setLocation(283, 220);
		
		this.addPlayer(table.getPlayer());
		ui.repaint();
	}
	
	public void start() 
	{
		if ((this.game_controller == null) || !this.game_controller.isValid())
			return;
		
		this.active_player=null;
		this.hand = new GUIHand(GUIRummy.this.ui, new Rectangle(5, 150, 10, 10), throw_area, this);

		game_controller.start();
	}

	private void setJoker(Card c)
	{
		game_joker = new GUICard(c);
        JPanel jp = this.game_joker.getPanel();
        this.ui.add(jp);
        jp.setLocation(62, 18);		
	}
	

	@Override
	public boolean handleDiscard(int card_idx) 
	{
		boolean ret = game_controller.discard(game_controller.getHand().getCard(card_idx));
		if (ret)
		{
			this.hand.removeCard(card_idx);
			this.hand.setActive(false);
			this.throw_pile.setActive(false);
			this.doneTurn();
		}
		return ret;
	}

	@Override
	public boolean handleSort() 
	{
		this.game_controller.sortHand();
		return true;
	}

	@Override
	public boolean handlePileDraw() 
	{
		if (this.game_controller.getHand().holdingExtraCards())
			return false;
		
		Card c = game_controller.draw(true);
		if (c != null)
		{
			this.throw_pile.draw();
			try 
			{
				this.addCard(c);
			} 
			catch (Exception e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}
	
	@Override
	public Hand getPlayHand() 
	{
		return this.game_controller.getHand();
	}

	public void addPlayer(GUIPlayer p) 
	{
		if (current_player < players_ui.length)
		{
			players_ui[current_player].setImage(p.getImage());
			players_ui[current_player].setName(p.getPlayer().getName());
			players_ui[current_player].id = null;
			players_ui[current_player].repaint();
			current_player++;
		}
	}

	public void addPlayer(Rummy.PlayerEvent p) 
	{
		if (current_player < players_ui.length)
		{
			table.updateGameInfo(p.getPlayerName() + " added");
			players_ui[current_player].setImage(GUIPlayer.loadImage(p.getPlayerImage()));
			players_ui[current_player].setName(p.getPlayerName());
			players_ui[current_player].id = p.getPlayerID();
			players_ui[current_player].repaint();
			current_player++;
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) 
	{
		if (e != null)
		{
			if ((e.getActionCommand().equals("SORT")) && (hand != null))
			{
				hand.sort();
			}
			else if (e.getActionCommand().equals("LEAVE"))
			{
				if (this.playing_turn)
				{
					turn_ret = TurnState.RESIGN;
					this.doneTurn();
				}
				else if (this.game_controller != null)
					this.game_controller.resign();
					
			}
			else if (e.getActionCommand().equals("SHOW"))
			{
				turn_ret = TurnState.SHOW;
				this.doneTurn();
			}
			else if (e.getActionCommand().equals("MARK"))
			{
				this.show_cards.newGroup();
			}
			else if (e.getActionCommand().equals("DONE"))
			{
				this.hand.clearShow();
				this.doneTurn();
			}
			else if (e.getActionCommand().equals("CANCEL"))
			{
				this.hand.clearShow();
				this.sort_btn.setText("Sort");
				this.sort_btn.setActionCommand("SORT");
				
				this.declare_btn.setText("Show");
				this.declare_btn.setActionCommand("SHOW");
				
				this.leave_btn.setText("Leave");
				this.leave_btn.setActionCommand("LEAVE");				
				this.doneTurn();
			}
		}
	}


	@Override
	public Player getPlayer() 
	{
		return (this.table != null)?this.table.getHost():null;
	}


	@Override
	public void addCard(Card c) 
	{
		try {
			this.hand.add(c);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void doneTurn()
	{
		this.turn_sem.release();
	}
	
	@Override
	public TurnState playTurn(int time) 
	{
		this.declare_btn.setEnabled(true);
		turn_ret = TurnState.CONTINUE;
		if (this.active_player != null)
			this.active_player.setActive(false);
		this.active_player = players_ui[0];
		this.active_player.setActive(true);
		this.throw_pile.setActive(true);
		this.hand.setActive(true);
		table.updateGameInfo("Your turn");
		this.playing_turn = true;
		this.turn_sem.acquireUninterruptibly();
		this.playing_turn = false;
		this.declare_btn.setEnabled(false);
		return turn_ret;
	}


	@Override
	public Card[][] declare() 
	{
		Card[][] ret = null;
		
		this.show_cards = this.hand.startShow();
		this.sort_btn.setText("Mark");
		this.sort_btn.setActionCommand("MARK");
		
		this.declare_btn.setText("Done");
		this.declare_btn.setActionCommand("DONE");
		this.declare_btn.setEnabled(true);
		
		this.leave_btn.setText("Abort");
		this.leave_btn.setActionCommand("CANCEL");
		table.updateGameInfo("Declare your cards");
		this.turn_sem.acquireUninterruptibly();
		if (this.show_cards != null)
		{
			ret = this.show_cards.get();
		}
		
		this.sort_btn.setText("Sort");
		this.sort_btn.setActionCommand("SORT");
		
		this.declare_btn.setText("Show");
		this.declare_btn.setActionCommand("SHOW");
		this.declare_btn.setEnabled(false);
		
		this.leave_btn.setText("Leave");
		this.leave_btn.setActionCommand("LEAVE");						
		return ret;
	}

	private RummyEvents getRummyEvent(int evt)
	{
		for ( RummyEvents re : RummyEvents.values())
		{
			if (re.ordinal() == evt)
				return re;
		}
		return null;
	}

	private RummyPlayerUI getPlayerUI(PlayerEvent pe)
	{
		if (pe == null)
			return null;
		
		RummyPlayerUI rui = null;
		for (int i = 0; i < this.current_player; i++)
		{
			if ((players_ui[i].id != null) && (players_ui[i].id.equals(pe.getPlayerID())))
			{
				rui = players_ui[i];
				break;
			}
		}
		return rui;
	}
	
	@Override
	public void handleEvent(GameEvent e) 
	{
		RummyPlayerUI pui;
		
		switch (getRummyEvent(e.getEventId()))
		{
		case NEW_PLAYER:
			this.addPlayer((Rummy.PlayerEvent)e);
			break;
		case GAME_OVER:
			JOptionPane.showMessageDialog(table.getGameUI(table_name), "Game Over");
			this.game.close();
			this.game = null;
			table.gameOver(table_name);
			break;
		case ACTIVE_PLAYER:
			if (this.active_player != null)
				this.active_player.setActive(false);
			this.active_player = getPlayerUI((PlayerEvent)e);
			if (this.active_player != null)
			{
				table.updateGameInfo(active_player.name + " playing");
				this.active_player.setActive(true);
			}
			break;
		case CARD_DISCARDED:
			Card c = ((Rummy.CardEvent)e).getCard();
			this.throw_pile.discard(new GUICard(c));
			break;
		case DRAW_FROM_PILE:
			this.throw_pile.draw();
			break;
		case GAME_JOKER:
			this.setJoker(((Rummy.CardEvent)e).getCard());
            break;
		case GAME_INPLAY:
			table.updateGameInfo("Game already in play.");
			break;
		case PLAYER_DECLARING:
			pui = getPlayerUI((PlayerEvent)e);
			if (pui != null)
			{
				table.updateGameInfo(pui.name + " declaring");				
			}
			break;
		case PLAYER_LOST:
			pui = getPlayerUI((PlayerEvent)e);
			if (pui != null)
			{
				int points = ((PlayerEvent)e).getPoints();
				table.updateGameInfo(pui.name + " lost " + points + " points");				
			}
			else
				JOptionPane.showMessageDialog(table.getGameUI(table_name), "You lost " + ((PlayerEvent)e).getPoints() + " points");

			break;
			
		case PLAYER_RESIGNED:
			pui = getPlayerUI((PlayerEvent)e);
			if (pui != null)
			{
				int points = ((PlayerEvent)e).getPoints();
				table.updateGameInfo(pui.name + " resigned and lost " + points + " points");				
			}
			else
				JOptionPane.showMessageDialog(table.getGameUI(table_name), "You resigned and lost " + ((PlayerEvent)e).getPoints() + " points");
			break;
		case PLAYER_WON:
			pui = getPlayerUI((PlayerEvent)e);
			if (pui != null)
			{
				int points = ((PlayerEvent)e).getPoints();
				table.updateGameInfo(pui.name + " won!");				
			}
			else
				JOptionPane.showMessageDialog(table.getGameUI(table_name), "You won!");
			break;
		case WAITING_FOR_PLAYERS:
			table.updateGameInfo("Waiting for others...");
			break;
		}
	}		

	private static class RummyPlayerUI extends JPanel
	{
		private static BufferedImage default_image = null;
		private BufferedImage my_image=null;
		private String name;
		private String id;
		private Font f;
		private Container ui;
		private Rectangle loc;
		
		RummyPlayerUI(Container c, Rectangle loc)
		{
			super(null);
			f = new Font("SansSerif", Font.PLAIN, 10);
			this.ui = c;
			this.loc = loc;
			resetPlayer();
			loadImage();
			this.setOpaque(false);
	        this.setSize(this.getPreferredSize());
	        this.ui.add(this);
	        this.setLocation(loc.x, loc.y);
		}
	
		public void setActive(boolean active)
		{
			if (active)
				this.setBorder(BorderFactory.createLineBorder(Color.RED));
			else
				this.setBorder(BorderFactory.createEmptyBorder());
			this.repaint();
			
		}
		
		public void setDeclaring()
		{
			this.setBorder(BorderFactory.createEtchedBorder());
		}
		
		public void resetPlayer()
		{
			this.my_image = null;
			this.id = null;
			this.name = "<No Player>";
			this.repaint();
		}
		
		public Dimension getPreferredSize() 
		{
			return new Dimension(loc.width, loc.height + 12);
		}
		
		private Rectangle getStringBounds(Graphics2D g2, String str) 
		{
			FontRenderContext frc = g2.getFontRenderContext();
			GlyphVector gv = g2.getFont().createGlyphVector(frc, str);
			return gv.getPixelBounds(null, 0f, 0f);
		}
	    
		public void setImage(BufferedImage bi)
		{
			my_image = bi;
		}
		
		public void setName(String name)
		{
			this.name = name;
		}
		
	    public void paintComponent(Graphics g) 
	    {
	        super.paintComponent(g);
	        BufferedImage bi = my_image;
	        if (bi == null)
	        	bi = default_image;
	        if (bi != null)
	        {
	        	g.drawImage(bi, 0, 0, loc.width, loc.height, /* 0, 0, default_image.getWidth(), default_image.getHeight(),*/ null);
	        	g.setFont(f);
	        	g.setColor(Color.WHITE);
	        	Rectangle r = getStringBounds((Graphics2D) g, name);
	        	int offset = (loc.width - r.width)/2;
	        	offset = (offset < 0)?0:offset;
	        	g.drawString(name, offset, loc.height + r.height);
	        }
	        
	    }  
	
	    private synchronized void loadImage() 
		{
			if (default_image == null)
			{
				//ClassLoader cl = Card.class.getClassLoader();
				try
				{
					default_image = ImageIO.read(getClass().getClassLoader().getResourceAsStream("Resources/nouser.png"));
				} catch(IOException e)
				{
					System.out.println(e);
					default_image = null;
				}
			}
		}	
	}
	
	class CardStack extends JPanel 
	{
		private static final long serialVersionUID = 1;
		private BufferedImage deck_image;
	
		public CardStack(Rectangle loc)
		{
			super(null);
			loadImage();
			this.setOpaque(false);
	        this.setSize(this.getPreferredSize());
	
	        addMouseListener(new MouseAdapter() {
		      public void mousePressed(MouseEvent e) 
		      {
		    	  // One way to find out if it is my turn
		    	  //  is if another thread is waiting on me to finish
		    	  if (GUIRummy.this.turn_sem.hasQueuedThreads())
		    	  {
		    		  if (!game_controller.getHand().holdingExtraCards())
		    		  {
				    	  Card c = GUIRummy.this.game_controller.draw(false);
				    	  if (c != null)
				    		  GUIRummy.this.addCard(c);
		    		  }
		    	  }
		      }
		  	});
	    }
		
		public Dimension getPreferredSize() 
		{
			Dimension d = null;
			if (deck_image != null)
				d = new Dimension(deck_image.getWidth(), deck_image.getHeight());
			return d;
		}
		
	
	    public void paintComponent(Graphics g) {
	        super.paintComponent(g);       

	        if (deck_image != null)
	        	g.drawImage(deck_image, 0, 0, deck_image.getWidth(), deck_image.getHeight(), null);
	        
	    }  
	    
		private void loadImage() 
		{
			if (deck_image == null)
			{
				//ClassLoader cl = Card.class.getClassLoader();
				try
				{
					deck_image = ImageIO.read(getClass().getResourceAsStream("/Resources/CardDeck.png"));
				} 
				catch(IOException e)
				{
					System.out.println(e);
					deck_image = null;
				}
			}
		}	
	}

}
