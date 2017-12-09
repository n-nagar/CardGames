package org.nagars.cardgames.multi;

import java.lang.reflect.Type;
import java.util.concurrent.Semaphore;

import org.nagars.cardgames.core.Card;
import org.nagars.cardgames.core.CardGame;
import org.nagars.cardgames.core.Hand;
import org.nagars.cardgames.core.Player;
import org.nagars.cardgames.core.Rummy;
import org.nagars.cardgames.multi.P2PEndPoint.EndPoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class MultiRummy extends Rummy implements P2PEndPoint.ClientHandler
{
	public class NetGameControl implements GameControl, P2PEndPoint.ReadHandler
	{
		private PlayerControl pc;
		private EndPoint ep;
		private Gson gson;
		private Hand hand;

		public NetGameControl(String server, PlayerControl pc)
		{
			// Connecting to a game server
			GsonBuilder gsonBilder = new GsonBuilder();
			gsonBilder.registerTypeAdapter(CardGame.GameEvent.class, new GameEventAdaptor());
			gson = gsonBilder.create();			
			this.pc = pc;
			this.ep = ptpe.connect(server);
			if (this.ep != null)
				this.ep.setReadHandler(this);
			this.hand = getEmptyHand();
		}
		
		public boolean isValid()
		{
			return (this.ep != null);
		}
		
		public String getConnectString()
		{
			return MultiRummy.this.getConnectString();
		}
		
		@Override
		public Card draw(boolean from_throwpile) 
		{
			if (!isValid())
				return null;
			
			Card c = null;
			try {
				String msg = gson.toJson(from_throwpile);
				String resp = ep.write(Methods.DRAW.ordinal(), msg, true);
				c = gson.fromJson(resp, Card.class);
				hand.add(c);
			} 
			catch (Exception e) 
			{
				this.ep.close();
				this.ep = null;
			}
			return c;
		}

		@Override
		public Hand getHand() 
		{
			if (!isValid())
				return null;
			if (hand!=null)
				return hand;
			else
			{
				try
				{
					String resp = ep.write(Methods.GET_HAND.ordinal(), "", true);
					Hand ret = gson.fromJson(resp, Hand.class);
					return ret;
				} 
				catch (Exception e) 
				{
					this.ep.close();
					this.ep = null;
				}
				return null;
			}
		}

		@Override
		public boolean discard(Card t) 
		{
			if (!isValid())
				return false;
			if (!hand.holdingExtraCards())
				return false;
			
			boolean ret = false;
			try
			{
				String msg = gson.toJson(t);
				String resp = ep.write(Methods.DISCARD.ordinal(), msg, true);
				if (resp != null)
				{
					ret = gson.fromJson(resp, boolean.class);
					if (ret)
						hand.removeCard(t);
				}
			}
			catch (Exception e)
			{
				this.ep.close();
				this.ep = null;				
			}
			return ret;
		}

		@Override
		public void sortHand() 
		{
			if (!isValid())
				return;
			try
			{
				ep.write(Methods.SORT_HAND.ordinal(), "", true);			
				if (hand != null)
					MultiRummy.this.sort(hand);
			}
			catch (Exception e)
			{
				this.ep.close();
				this.ep = null;				
			}
		}

		@Override
		public void resign() 
		{
			if (!isValid())
				return;
			try
			{
				ep.write(Methods.RESIGN.ordinal(), "", false);
			}
			catch (Exception e)
			{
				this.ep.close();
				this.ep = null;				
			}				
		}

		@Override
		public void start() 
		{
			if (!isValid())
				return;
			try
			{
				ep.write(Methods.START.ordinal(), "", true);
			}
			catch (Exception e)
			{
				this.ep.close();
				this.ep = null;				
			}
			
		}

		@Override
		public String process(int caller_id, String request) 
		{
			String ret = "";
			if (Methods.ADD_CARD.ordinal() == caller_id)
			{
				Card c = gson.fromJson(request, Card.class);
				if (hand.add(c) >= 0)
					pc.addCard(c);
			}
			else if (Methods.DECLARE.ordinal() == caller_id)
			{
				Card[][] show = pc.declare();
				ret = gson.toJson(show); 
			}
			else if (Methods.GET_PLAYER.ordinal() == caller_id)
			{
				Player p = pc.getPlayer();
				ret = gson.toJson(p);
			}
			else if (Methods.HANDLE_EVENT.ordinal() == caller_id)
			{
				GameEvent ge = gson.fromJson(request, GameEvent.class);
				pc.handleEvent(ge);
			}
			else if (Methods.PLAY_TURN.ordinal() == caller_id)
			{
				int time = gson.fromJson(request, int.class);
				TurnState ts = pc.playTurn(time);
				ret = gson.toJson(ts); 
			}
			else
				System.out.printf("NetGameControl: BadCommand(%d, %s)\n", caller_id, request);
			
			return ret;
		}

	}
	
	public class NetPlayerControl implements CardGame.PlayerControl, P2PEndPoint.ReadHandler
	{
		private EndPoint ep;
		private Gson gson;
		private GameControl gc;
		private Player gamer;
		
		private NetPlayerControl(EndPoint ep)
		{
			// Client connection to a game server
			this.ep = ep;
			GsonBuilder gsonBilder = new GsonBuilder();
			gsonBilder.registerTypeAdapter(CardGame.GameEvent.class, new GameEventAdaptor());
			gson = gsonBilder.create();			
			gamer = null;
		}
		
		@Override
		public Player getPlayer() 
		{
			if (gamer != null)
				return gamer;
			try
			{
				String resp = ep.write(Methods.GET_PLAYER.ordinal(), "", true);
				gamer = gson.fromJson(resp, Player.class);
			}
			catch (Exception e)
			{
				this.ep.close();
				this.ep = null;				
			}
			return gamer;
		}

		@Override
		public void addCard(Card c) 
		{
			try
			{
				String msg = gson.toJson(c);
				ep.write(Methods.ADD_CARD.ordinal(), msg, true);
			}
			catch (Exception e)
			{
				this.ep.close();
				this.ep = null;				
			}
		}

		public String getConnectString()
		{
			return MultiRummy.this.getConnectString();
		}
		
		@Override
		public TurnState playTurn(int time) 
		{
			try
			{
				String msg = gson.toJson(time);
				String resp = ep.write(Methods.PLAY_TURN.ordinal(), msg, true);
				TurnState ts = gson.fromJson(resp, TurnState.class);
				return ts;
			}
			catch (Exception e)
			{
				this.ep.close();
				this.ep = null;				
			}
			return TurnState.RESIGN;
		}

		@Override
		public Card[][] declare() 
		{
			try
			{
				String resp = ep.write(Methods.DECLARE.ordinal(), "", true);
				Card [][] ret = gson.fromJson(resp, Card[][].class);
				if (ret.length == 5)
					Thread.dumpStack();
				return ret;
			}
			catch (Exception e)
			{
				this.ep.close();
				this.ep = null;				
			}
			return null;
		}

		@Override
		public void handleEvent(CardGame.GameEvent e) 
		{
			try
			{
				String classname = e.getClass().getName();
				String msg = gson.toJson(e, CardGame.GameEvent.class);
				ep.write(Methods.HANDLE_EVENT.ordinal(), msg, true);
			}
			catch (Exception ex)
			{
				this.ep.close();
				this.ep = null;				
			}
		}

		@Override
		public String process(int caller_id, String request) 
		{
			String ret = "";
			if (Methods.DISCARD.ordinal() == caller_id)
			{
				Card c = gson.fromJson(request, Card.class);
				boolean success = gc.discard(c);
				ret = gson.toJson(success); 
			}
			else if (Methods.DRAW.ordinal() == caller_id)
			{
				boolean throw_pile = gson.fromJson(request, boolean.class);
				Card c = gc.draw(throw_pile);
				ret = gson.toJson(c); 
			}
			else if (Methods.GET_HAND.ordinal() == caller_id)
			{
				Hand h = gc.getHand();
				ret = gson.toJson(h);
			}
			else if (Methods.SORT_HAND.ordinal() == caller_id)
			{
				gc.sortHand();
			}
			else if (Methods.START.ordinal() == caller_id)
			{
				gc.start();
			}
			else if (Methods.RESIGN.ordinal() == caller_id)
			{
				gc.resign();
			}
			else
				System.out.printf("NetPlayerControl: BadCommand (%d, %s)\n", caller_id, request);
			
			return ret;
		}

	}

	P2PEndPoint ptpe;
	Gson gson;
	
	private enum Methods
	{
		DRAW, GET_HAND, DISCARD, SORT_HAND, START, GET_PLAYER, ADD_CARD,
		PLAY_TURN, DECLARE, HANDLE_EVENT, RESIGN		
	}
	
	public MultiRummy() 
	{
		// Game server
		super();
		gson = new Gson();
		ptpe = new P2PEndPoint(this);
	}
	
//	public void remove(RummyControl gc, GameEvent msg_to_send)
//	{
//		super.remove(gc, msg_to_send);
//		close();
//	}
	
	public void close()
	{
		if (ptpe != null)
			ptpe.close();
	}
	@Override
	public void process(EndPoint ep) 
	{
		NetPlayerControl npc = new NetPlayerControl(ep);
		ep.setReadHandler(npc);
		npc.gc = super.join(npc);
	}

	public String getConnectString()
	{
		return ptpe.toString();
	}
	

	@Override
	public GameControl join(PlayerControl p) 
	{
		return super.join(p);
	}
	
	public GameControl join(String server, PlayerControl p)
	{
		NetGameControl ncg = new NetGameControl(server, p);
		return ncg;
	}

}

class GameEventAdaptor  implements JsonSerializer<CardGame.GameEvent>, JsonDeserializer<CardGame.GameEvent>
{

	@Override
	public org.nagars.cardgames.core.CardGame.GameEvent deserialize(
			JsonElement json, Type arg1, JsonDeserializationContext context)
			throws JsonParseException 
	{
		JsonObject jsonObject = json.getAsJsonObject();
		JsonElement je = jsonObject.get("type");
		if (je == null)
			throw new JsonParseException("Object was not serialized by custom serializer");
		String type = je.getAsString();
		JsonElement element = jsonObject.get("properties");
		if (element == null)
			throw new JsonParseException("Object was not serialized by custom serializer");
		try 
		{
			return context.deserialize(element, Class.forName(type));
		} 
		catch (ClassNotFoundException cnfe)
		{
			throw new JsonParseException("Unknown element type: " + type, cnfe);
		}
	}

	@Override
	public JsonElement serialize(
			org.nagars.cardgames.core.CardGame.GameEvent src, Type arg1,
			JsonSerializationContext context) 
	{
		JsonObject result = new JsonObject();
		result.add("type", new JsonPrimitive(src.getClass().getName()));
		result.add("properties", context.serialize(src, src.getClass()));
		 
		return result;	
	}
	
}