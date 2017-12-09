package org.nagars.cardgames.gui;


import java.awt.AWTEvent;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.nagars.cardgames.core.CardGame;
import org.nagars.cardgames.core.Player;
import org.nagars.cardgames.core.Rummy;
import org.nagars.cardgames.multi.MultiRummy;

public class GUITable
{
	@SuppressWarnings("serial")
	private class TableUI extends JPanel 
	{
		private Thread game_thread;
		private GUIRummy this_game;
		
		TableUI()
		{
			super(null);
		}
		
		public void start()
		{
			game_thread = new Thread(new Runnable(){
				public void run()
				{
					this_game.start();
				}
			});
			game_thread.start();
		}
		
		public Dimension getPreferredSize()
		{
			Dimension d = new Dimension();

			d.width = 640;
			d.height = 480;
			return d;
		}
		
	    public void paintComponent(Graphics g) 
	    {
	        super.paintComponent(g);   
	        setBackground(new Color(0, 120, 0));
	    
	    }		
	}


	public class TableMenu extends JMenuBar implements ActionListener
	{
		private static final long serialVersionUID = 1L;
		private JFrame parent_frame;
		private JMenuItem player_menu;
		private JMenuItem game_host_menu;
		private JMenuItem game_join_menu;
		private JMenuItem game_start_menu;
		private JLabel game_str;
		private Pattern[] connect_str;
		
		TableMenu()
		{
			super();
			connect_str = new Pattern[3];
			int i = 0;
			connect_str[i++] = Pattern.compile("tcp\\://[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\:[0-9]+");
			connect_str[i++] = Pattern.compile("tcp\\://.+\\:[0-9]+");
			connect_str[i++] = Pattern.compile("[0-9]+");

			
			JMenu jm1 = new JMenu("Game");
			player_menu = jm1.add("New Player...");
			player_menu.setActionCommand("NEWUSER");
			player_menu.addActionListener(this);
			jm1.addSeparator();
			game_host_menu = jm1.add("Host");
			game_host_menu.setActionCommand("HOST");
			game_host_menu.addActionListener(this);
			game_join_menu = jm1.add("Join...");
			game_join_menu.setActionCommand("JOIN");
			game_join_menu.addActionListener(this);

			game_start_menu = jm1.add("Start");
			game_start_menu.setActionCommand("START");
			game_start_menu.addActionListener(this);

			game_host_menu.setEnabled(false);
			game_join_menu.setEnabled(false);
			game_start_menu.setEnabled(false);

			jm1.add(game_host_menu);
			jm1.add(game_join_menu);
			jm1.add(game_start_menu);
			jm1.addSeparator();
			JMenuItem mi_host = jm1.add("Exit");
			mi_host.addActionListener(this);
			this.add(jm1);
			this.add(Box.createHorizontalGlue());
			game_str = new JLabel("<No Game>");
			this.add(game_str);
		}

		public void enableGameMenus()
		{
			if (GUITable.this.host != null)
			{
				game_host_menu.setEnabled(true);
				game_join_menu.setEnabled(true);
				game_start_menu.setEnabled(false);
				player_menu.setText("Edit Player...");
			}
			else
			{
				player_menu.setText("New Player...");
				game_host_menu.setEnabled(false);
				game_join_menu.setEnabled(false);
				game_start_menu.setEnabled(false);
			}
		}
		
		public void setGameInfo(String info)
		{
			this.game_str.setText(info);
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) 
		{
			for (Frame f : Frame.getFrames())
				if (f.isActive())
					parent_frame = (JFrame) f;
			if (arg0.getActionCommand().equalsIgnoreCase("Exit"))
			{
				parent_frame.dispatchEvent(new WindowEvent(parent_frame, WindowEvent.WINDOW_CLOSING));
			}
			else if (arg0.getActionCommand().equalsIgnoreCase("NEWUSER"))
			{
				showNewUserDialog(parent_frame);
			}
			else if (arg0.getActionCommand().equals("START"))
			{
				TableUI this_game = games.get("MainGame");
				if (this_game != null)
				{
					this_game.start();
					game_start_menu.setEnabled(false);
				}
			}
			else if (arg0.getActionCommand().equals("HOST"))
			{
				game_host_menu.setEnabled(false);
				game_join_menu.setEnabled(false);
				game_start_menu.setEnabled(true);
				GUITable.this.hostGame("MainGame");
			}
			else if (arg0.getActionCommand().equals("JOIN"))
			{
				Matcher m = null;
				Object options[] = { "Connect", "Cancel" };
				String host_str = null;
				JOptionPane jp = new JOptionPane("Enter host information", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, options[0]);
				jp.setWantsInput(true);
				JDialog jd = jp.createDialog(parent_frame.getContentPane(), "Join Game");
				do
				{
					jd.setVisible(true);
					Object selection = jp.getValue();
					if ((selection == null) || (selection == options[1]))
						break;
					
					host_str = (String)jp.getInputValue();
					for (int i = 0; i < connect_str.length; i++)
					{
						m = connect_str[i].matcher(host_str);
						if (m.matches())
							break;
					}
					
					if ((m != null) && m.matches())
						break;
					JOptionPane.showConfirmDialog(parent_frame.getContentPane(), 
													"Enter valid format for host information",
													"Error",
													JOptionPane.OK_OPTION,
													JOptionPane.ERROR_MESSAGE);
					m = null;
				} while (m == null);
				
				if (m != null)
				{
					game_host_menu.setEnabled(false);
					game_join_menu.setEnabled(false);
					game_start_menu.setEnabled(true);
					GUITable.this.joinGame(host_str, "MainGame");
				}
			}
		}
		
		public void showNewUserDialog(JFrame parent_frame)
		{
			if (GUITable.this.host == null)
				GUITable.this.host = new GUIPlayer();
			JPanel p = GUITable.this.host.getPlayerUI();
			JDialog d = new JDialog(parent_frame, true);
			d.setTitle("Player Details");
			d.setContentPane(p);
			d.pack();
			Rectangle frame_size = parent_frame.getBounds();
			Rectangle dialog_size = d.getBounds();
			dialog_size.x = frame_size.x + ((frame_size.width - dialog_size.width)/2);
			dialog_size.y = frame_size.y + ((frame_size.height - dialog_size.height)/2);
			d.setBounds(dialog_size);
			d.setVisible(true);
		}

	}


	private GUIPlayer host;
	private Hashtable<String, TableUI> games;
	private JFrame main_frame;
	private TableMenu main_menu;
	private GUIRummy game_gui;

    public GUITable() throws Exception
	{
        main_frame = new JFrame("Card Games");
        main_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        main_frame.setPreferredSize(new Dimension(357,323));
        main_frame.setResizable(false);
		main_menu = new TableMenu();
		main_frame.setJMenuBar(main_menu);
		
		Container cp = main_frame.getContentPane();
		cp.setLayout(new CardLayout());
		JLabel base = new JLabel("Use the game menu to start a game");
		base.setHorizontalAlignment(SwingConstants.CENTER);
		base.setVerticalAlignment(SwingConstants.CENTER);
		cp.add(base,"Base");
		base.setSize(cp.getSize());

        main_frame.pack();
        main_frame.setVisible(true);
        
        game_gui = null;
		games = new Hashtable<String, TableUI>();
		this.host = GUIPlayer.getCurrentPlayer();
		if (this.host == null)
		{
			main_menu.showNewUserDialog(main_frame);
			this.host = GUIPlayer.getCurrentPlayer();
			if (this.host != null)
				main_menu.enableGameMenus();
		}
		else
			main_menu.enableGameMenus();
			
	}
    
    public JPanel getGameUI(String table_name)
    {
    	return games.get(table_name);
    }
    
    public void setGameInfo(String title,String info)
    {
    	this.main_frame.setTitle(title);
    	this.main_menu.setGameInfo(info);
    }
    
    public void updateGameInfo(String info)
    {
    	this.main_menu.setGameInfo(info);    	
    }
    
    public Player getHost()
    {
    	return this.host.getPlayer();
    }
    
    public GUIPlayer getPlayer()
    {
    	return this.host;
    }
    public synchronized void hostGame(String table_name)
    {
    	if (game_gui != null)
    		return;
    	
    	TableUI game_ui = new TableUI();
    	this.games.put(table_name, game_ui);
    	Container cp = main_frame.getContentPane();
    	cp.add(game_ui, table_name);
    	((CardLayout)cp.getLayout()).show(cp, table_name);
    	game_ui.this_game = new GUIRummy(this, table_name);
    	return;
    }
    
    public synchronized void joinGame(String connect_str, String table_name)
    {
    	if (game_gui != null)
    		return;
    	
    	TableUI game_ui = new TableUI();
    	this.games.put(table_name, game_ui);
    	Container cp = main_frame.getContentPane();
    	cp.add(game_ui, table_name);
    	((CardLayout)cp.getLayout()).show(cp, table_name);
    	game_ui.this_game = new GUIRummy(this, connect_str, table_name);;
    	return;
    }
    
    public void gameOver(String table_name)
    {
    	TableUI game_ui = games.remove(table_name);
    	if (game_ui != null)
    	{
	    	main_frame.getContentPane().remove(game_ui);
	    	game_ui.removeAll();
	    	main_menu.enableGameMenus();
    	}
    }
}

