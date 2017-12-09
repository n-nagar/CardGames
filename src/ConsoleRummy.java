import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nagars.cardgames.core.*;
import org.nagars.cardgames.core.CardGame.GameEvent;
import org.nagars.cardgames.core.CardGame.TurnState;
import org.nagars.cardgames.core.Rummy;
import org.nagars.cardgames.core.Rummy.PlayerEvent;
import org.nagars.cardgames.core.Rummy.RummyEvents;
import org.nagars.cardgames.multi.MultiRummy;

public class ConsoleRummy implements CardGame.PlayerControl
{
	private static final ConsoleRummy spcg = new ConsoleRummy();
	private static final String state_file = "CardPlayer.ini";
	private static Properties prop;
	private Player current_player;
	CardGame cg;
	CardGame.GameControl gc;
	Pattern[] cmds;
	Pattern[] show_cmds;
	Pattern[] main_cmds;
	Hand.Show show;
	Card game_joker;
	MultiRummy ncg;
	Scanner input;
	
	private ConsoleRummy() 
	{
		game_joker = null;
		FileInputStream f = null;
		input = new Scanner(System.in);
		try 
		{
			current_player = new Player();
			prop = new Properties();
			f = new FileInputStream(state_file);
			prop.loadFromXML(f);
			current_player.setName(prop.getProperty("user"));
			String url_str = prop.getProperty("user_image");
			if (url_str!=null)
			{
				URL img = new URL(url_str);
				current_player.setImageURL(img);
			}
			System.out.printf("Wecome back %s\n", current_player.getName());			
		} catch (Exception e) 
		{
			System.out.println(e.getMessage());
			if ((current_player.getName() == null) || (current_player.getName().length() == 0))
				current_player = null;
		}
		finally
		{
			try {
				if (f != null)
					f.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				;
			}
		}
		
		if (current_player == null)
		{
			Scanner s = input;
			current_player = new Player();

			System.out.printf("Enter your name: ");
			current_player.setName(s.nextLine());
			try {
				current_player.setImageURL(new URL("http://www.dunkindonuts.com/etc/designs/dunkindonuts/images/DD_Logo_share.jpg"));
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			prop.put("user", current_player.getName());
			save();
		}

		main_cmds = new Pattern[5];
		int i = 0;
		main_cmds[i++] = Pattern.compile("[Hh]([Oo][Ss][Tt])?");
		main_cmds[i++] = Pattern.compile("[Jj]([oO][iI][nN])? (tcp\\://[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\:[0-9]+)");
		main_cmds[i++] = Pattern.compile("[Ss]([Tt][Aa][Rr][Tt])?");
		main_cmds[i++] = Pattern.compile("[Qq]([Uu][iI][Tt])?");
		main_cmds[i++] = Pattern.compile("[Jj]([oO][iI][nN])? ([0-9]+)");

		cmds = new Pattern[7];
		i = 0;
		cmds[i++] = Pattern.compile("[Dd]([rR][aA][wW])?");
		cmds[i++] = Pattern.compile("[Pp]([iI][cC][kK])?");
		cmds[i++] = Pattern.compile("[Dd]([iI][sS][cC][aA][rR][dD])? (1?[0-9]{1})");
		cmds[i++] = Pattern.compile("[Mm]([oO][vV][eE])? (1?[0-9]{1}) (1?[0-9]{1})");
		cmds[i++] = Pattern.compile("[Rr]([eE][sS][iI][gG][nN])?");
		cmds[i++] = Pattern.compile("[Ss]([hH][oO][wW])?");
		cmds[i++] = Pattern.compile("[Ss]([oO][rR][tT])?");

		show_cmds = new Pattern[3];
		i = 0;
		show_cmds[i++] = Pattern.compile("(1?[0-9][ ,]+)(1?[0-9][ ,]+)+(1?[0-9])");
		show_cmds[i++] = Pattern.compile("[Dd]([eE][lL][eE][tT][eE])? (1?[0-9]{1})");
		show_cmds[i++] = Pattern.compile("[Dd]([oO][nN][eE])?");
	}

	public static final ConsoleRummy getGame()
	{
		return spcg;
	}
	
	public void gameLoop()
	{
		Scanner s = input;
		boolean done = false;
		MultiRummy nr = null;

		gc = null;
		nr = new MultiRummy();
		System.out.printf("Connect address: %s\n", nr.getConnectString());
		do
		{
			System.out.printf(">> ");
			String cmd = s.nextLine();
			
			int i;
			Matcher m = null;
			
			for (i = 0; i < main_cmds.length; i++)
			{
				m = main_cmds[i].matcher(cmd);
				if (m.matches())
					break;
			}
			
			switch (i)
			{
			case 0:
				gc = nr.join(this);
				break;
			case 1:
				String connect_str = m.group(2);
				gc = nr.join(connect_str, this);
				break;
			case 4:
				String local_str = "tcp://localhost:" + m.group(2);
				gc = nr.join(local_str, this);
				break;
			case 2:
				if (gc != null)
					gc.start();
				//gc = null;
				break;
			case 3:
				done = true;
				nr.close();
				nr = null;
				break;
			default:
				System.out.println("Bad command");	
			}
		} while (!done);
		return;
	}
	
	public synchronized void save()
	{
		FileOutputStream f = null;
		try {
			f = new FileOutputStream(state_file);
			prop.storeToXML(f, "CardGame Player");
		} catch (Exception e) {
			System.out.println("No save file");
		}
		finally
		{
			try {
				if (f != null)
					f.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				;
			}
		}
	}
	
	public void startRummy()
	{
		cg = new MultiRummy();
		gc = cg.join(this);
		gc.start();
		((MultiRummy)cg).close();
	}
	
	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws ClassNotFoundException 
	{
		ConsoleRummy spcg = ConsoleRummy.getGame();
		spcg.gameLoop();
		System.exit(0);
	}

	@Override
	public Player getPlayer() 
	{
		return current_player;
	}

	@Override
	public void addCard(Card c) 
	{
//		com.google.gson.Gson gs = new com.google.gson.Gson();
//		String s = gs.toJson(c);
//		System.out.println(s);
//		Card c1 = gs.fromJson(s, Card.class);
//		System.out.println(c1);
	}

	@Override
	public TurnState playTurn(int time) 
	{
		Scanner s = input;
		boolean done_turn = false;
		TurnState ret = TurnState.CONTINUE;
		do
		{
			Hand hand = gc.getHand();
			hand.printCards();
			System.out.printf("GAME>> ");
			String cmd = s.nextLine();
			
			int i;
			Matcher m = null;
			
			for (i = 0; i < cmds.length; i++)
			{
				m = cmds[i].matcher(cmd);
				if (m.matches())
					break;
			}
			
			switch (i)
			{
			case 0:
			case 1:
				if (!hand.holdingExtraCards())
				{
					Card c = gc.draw((i == 1));
					if (c != null)
						System.out.printf("Drew %s\n", c);
					else
						System.out.println("No card");					
				}
				else
					System.out.println("Cannot draw another card in this turn");
				break;
			case 2:
				String cnt = m.group(2);
				if (cnt != null)
				{
					int cno = Integer.valueOf(cnt);
					Card c = hand.getCard(cno);
					if (c != null)
					{
						done_turn = gc.discard(c);
						if (!done_turn)
							System.out.printf("Cannot discard at this time\n");
					}
					else
						System.out.printf("Did not find card #%d\n", cno);
				}
				break;
			case 3:
				String src = m.group(2);
				String dst = m.group(3);
				if ((src != null) && (dst != null))
				{
					int sno = Integer.valueOf(src);
					int dno = Integer.valueOf(dst);
					if (sno > dno)
					{
						while (dno < sno)
						{
							hand.swap(sno, sno-1);
							sno--;
						}
					}
					else if (sno < dno)
					{
						while (sno < dno)
						{
							hand.swap(sno, sno+1);
							sno++;
						}
					}
				}
				break;
			case 4:
				ret = TurnState.RESIGN;
				done_turn = true;
				break;
			case 5:
				ret = TurnState.SHOW;
				done_turn = true;
				break;
			case 6:
				gc.sortHand();
				break;
			default:
				System.out.println("Bad command");
			}
		} while(!done_turn);
		return ret;
	}

	@Override
	public Card[][] declare() 
	{
		ArrayList<ArrayList<Integer>> show;
		//Hand.Show show = gc.getHand().startShow();
		
		show = new ArrayList<ArrayList<Integer>>();
		Scanner s = input;
		boolean done_turn = false;
		do
		{
			System.out.printf("SHOW>>: ");
			String cmd = s.nextLine();
			
			int i;
			Matcher m = null;
			
			for (i = 0; i < show_cmds.length; i++)
			{
				m = show_cmds[i].matcher(cmd);
				if (m.matches())
					break;
			}
			
			switch (i)
			{
			case 0:
				ArrayList<Integer> run = new ArrayList<Integer>();
				StringTokenizer st = new StringTokenizer(cmd, " ,");
				while ((st != null) && (st.hasMoreTokens()))
				{
					int idx = Integer.valueOf(st.nextToken());
					run.add(idx);
				}
				show.add(run);
				break;
			case 1:
				String num = m.group(2);
				if (num != null)
				{
					Integer idx = new Integer(num);
					for (int l = 0; l < show.size(); l++)
						if (show.get(l).contains(idx))
							show.get(l).remove(idx);
				}
				break;
			case 2:
				done_turn = true;
				break;
			default:
				System.out.println("Bad command");	
			}
		} while(!done_turn);
		
		Card ret[][] = new Card[show.size()][];
		Hand h = this.gc.getHand();
		for (int l = 0; l < ret.length; l++)
		{
			ret[l] = new Card[show.get(l).size()];
			for (int m = 0; m < ret[l].length; m++)
				ret[l][m] =  h.getCard(show.get(l).get(m));
		}
		return ret;
	}

	@Override
	public void handleEvent(GameEvent e) 
	{
		System.out.println();
		if (e.getEventId() == RummyEvents.GAME_JOKER.ordinal())
		{
			Rummy.CardEvent ce = (Rummy.CardEvent)e;
			game_joker = ce.getCard();
			System.out.println(e.getMessage());
		}
		else if (e.getEventId() == RummyEvents.PLAYER_LOST.ordinal())
		{
			PlayerEvent pe = (PlayerEvent)e;
			System.out.printf("%s lost by %d points\n", pe.getPlayerName(), pe.getPoints());
			pe.getPoints();
		}
		else if (e.getEventId() == RummyEvents.GAME_OVER.ordinal())
		{
			System.out.printf("Game Over!!\n");
			
		}
		else
			System.out.println(e.getMessage());

	}

}
