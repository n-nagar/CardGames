package org.nagars.cardgames.multi;
import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nagars.cardgames.multi.P2PEndPoint.EndPoint;

public class P2PEndPoint extends Thread implements CompletionHandler<AsynchronousSocketChannel,P2PEndPoint.AsyncStates>
{
	public class P2PException extends Exception
	{
		P2PException(Exception e)
		{
			super(e);
		}
		
	}
	public enum AsyncStates
	{
		SERVER, GAME, PLAYER, READ, WRITE
	}
	
	private enum MessageType
	{
		REQUEST, RESPONSE
	}
	
	public interface ClientHandler
	{
		public void process(EndPoint ep);
	}
	
	public interface ReadHandler
	{
		public String process(int caller_id, String request);
	}
	
	private class Transaction
	{
		String response;
		Semaphore sem;
		
		Transaction()
		{
			sem = new Semaphore(0);
		}
		
		public void waitForResponse()
		{
			try 
			{
				sem.acquire();
			} 
			catch (InterruptedException e) 
			{
				// TODO Auto-generated catch block
			}
		}
		public void setResponse(String res)
		{
			response = res;
			sem.release();
		}		
	}
	
	public class EndPoint implements CompletionHandler<Integer,AsyncStates>
	{
		AsynchronousSocketChannel endpoint;
		ByteBuffer read_buf;
		ByteBuffer write_buf;
		Hashtable<Integer, Transaction> tasks;
		ReadHandler read_handler;
		private int sequence;
		boolean valid;
		
		private EndPoint(AsynchronousSocketChannel a)
		{
			try
			{
				a.setOption(StandardSocketOptions.TCP_NODELAY, true);
				this.read_buf = ByteBuffer.allocate(1024);
				this.write_buf = ByteBuffer.allocate(1024);
				this.read_handler = null;
				tasks = new Hashtable<Integer, Transaction>();
				this.endpoint = a;
				this.sequence = 0;
				endpoint.read(read_buf, AsyncStates.READ, this);
				valid = true;
			}
			catch (IOException e)
			{
				valid = false;
			}
		}

		public void setReadHandler(ReadHandler handler)
		{
			this.read_handler = handler;
		}
		
		public String write(int caller_id, String msg, boolean wait_for_response) throws P2PException
		{
			return write(caller_id, msg, wait_for_response, MessageType.REQUEST, 0);
		}
		
		private String write(int caller_id, String msg, boolean wait_for_response, MessageType msg_type, int resp_seq) throws P2PException
		{
			if (msg == null)
				return null;
			
			//@todo: Ensure that there is now pending write operation
			//
			Transaction t = null;
			int next_seq = 0;
			synchronized(this)
			{
				if (msg_type == MessageType.REQUEST)
				{
						next_seq = this.sequence++;
				}
				else
					next_seq = resp_seq;
				int msg_len = 16 +  msg.length();
				if (write_buf.capacity() < msg_len)
					write_buf = ByteBuffer.allocate(msg_len);
				write_buf.clear();
				write_buf.putInt(msg.length());
				write_buf.putInt(next_seq);
				write_buf.putInt(caller_id);
				write_buf.putShort((short)(msg_type.ordinal())); // 0 for request, 1 for response
				write_buf.putShort((short)(wait_for_response?1:0));
				write_buf.put(msg.getBytes());
				write_buf.flip();
				
				if (wait_for_response)
				{
					t = new Transaction();
					tasks.put(next_seq, t);
				}
				
				Future<Integer> write_handle = endpoint.write(write_buf);
				try
				{
					int len = write_handle.get();
					if (len < msg_len)
						System.out.println("Wrote less than the whole packet");
				}
				catch (Exception cce)
				{
					// Close this end point
					try 
					{
						endpoint.close();
					} 
					catch (IOException e) 
					{
					}
					endpoint = null;
					this.valid = false;
					cce.printStackTrace();
					if (t != null)
						tasks.remove(next_seq);
					throw new P2PException(cce);
				}
			}
			
			if (t != null)
			{
				t.waitForResponse();
				tasks.remove(next_seq);
				return t.response;
			}
			
			return null;
		}
		
		@Override
		public void completed(Integer count, AsyncStates state) 
		{
			if (state == AsyncStates.READ)
			{
				//System.out.printf("Read: %d\n", count);
				if (count > 0)
				{
					read_buf.flip();
					// Read the length of the string
					int len = read_buf.getInt();
					int seq = read_buf.getInt();
					int caller_id = read_buf.getInt();
					short mto = read_buf.getShort();
					short wait_for_resp = read_buf.getShort();
					byte[] b = new byte[len];
					read_buf.get(b);
					String msg = new String(b);
					read_buf.clear();
					endpoint.read(read_buf, AsyncStates.READ, this);
					Transaction t = null;
					if (mto == MessageType.RESPONSE.ordinal())
					{
						t = tasks.get(seq);
						if (t != null)
						{
							t.setResponse(msg);
						}
					}
					else if (this.read_handler != null)
					{
						String resp = this.read_handler.process(caller_id, msg);
						if (wait_for_resp == 1)
						{
							// The oteher end is waiting for this response
							try 
							{
								this.write(caller_id, ((resp == null)?"":resp), false, MessageType.RESPONSE, seq);
							} 
							catch (P2PException e) 
							{
							}
						}
					}
				}
				else if (count < 0)
				{
					try 
					{
						endpoint.close();
					} 
					catch (IOException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		@Override
		public void failed(Throwable arg0, AsyncStates arg1) 
		{
			// Not sure what failed, so clear out any waiting tasks
			Enumeration<Transaction> et = tasks.elements();
			while (et.hasMoreElements())
			{
				Transaction t = et.nextElement();
				t.setResponse("");
			}
		}
		
		public void close()
		{
			try
			{
				Enumeration<Transaction> e=this.tasks.elements();
				while (e.hasMoreElements())
				{
					Transaction t = e.nextElement();
					t.sem.release();
				}
				if (endpoint != null)
					endpoint.close();
			}
			catch(Exception e)
			{
				
			}
		}
	}
	
	AsynchronousServerSocketChannel listener;
	AsynchronousChannelGroup p2p_group;
	Future<AsynchronousSocketChannel> listen_handle;
	Hashtable<AsynchronousSocketChannel, EndPoint> endpoints;
	ClientHandler client_handler;
	
	public static final int port_range_start = 9268;
	public static final int port_range = 24;
	private int port;
	
	private int getNewPort()
	{
		int n = (int)(Math.random() * port_range);
		return port_range_start + n;		
	}
	
	public P2PEndPoint(ClientHandler handler) 
	{
		listener = null;
		listen_handle = null;
		client_handler = handler;
		endpoints = new Hashtable<AsynchronousSocketChannel, EndPoint>();
		
		int attempts = 5;
		port = port_range_start;
		do
		{
			try
			{
				p2p_group = AsynchronousChannelGroup.withFixedThreadPool(25, Executors.defaultThreadFactory());
				listener = AsynchronousServerSocketChannel.open(p2p_group).bind(new InetSocketAddress(port));
				this.start();
			}
			catch(AlreadyBoundException aeb)
			{
				port = this.getNewPort();
			}
			catch(BindException be)
			{
				port = this.getNewPort();
			}
			catch(IOException e)
			{
				attempts = 0;
			}
		} while ((--attempts > 0) && ((listener == null)));
	}

	public String toString()
	{
		String ret = "<Not Bound>";
		
		if (listener != null)
		{
			ret = "tcp://" + getIPAddress() + ":" + Integer.toString(port);
		}
		return ret;
	}

	private String getIPAddress()
	{
		String ret = null;
		try
		{
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements())
			{
			    NetworkInterface current = interfaces.nextElement();
			    if (!current.isUp() || current.isLoopback() || current.isVirtual()) 
			    	continue;
			    Enumeration<InetAddress> addresses = current.getInetAddresses();
			    while (addresses.hasMoreElements())
			    {
			        InetAddress current_addr = addresses.nextElement();
			        if (current_addr.isLoopbackAddress()) 
			        	continue;
			        if (current_addr instanceof Inet4Address)
			        	ret = current_addr.getHostAddress();
			    }
			}
		}
		catch(Exception e)
		{
			ret = "localhost";
		}
		return ret;
	}
	
	public EndPoint connect(String server)
	{
		EndPoint ret = null;
		String ip = null;
		String port = null;
		
		Pattern p = Pattern.compile("tcp\\://([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\\:([0-9]+)");
		Matcher m = p.matcher(server);
		if (m.matches())
		{
			ip = m.group(1);
			port = m.group(2);
		}
		else
		{
			p = Pattern.compile("tcp\\://(.+)\\:([0-9]+)");
			m = p.matcher(server);
			if (m.matches())
			{
				ip = m.group(1);
				port = m.group(2);
			}
			else
			{
				p = Pattern.compile("[0-9]+");
				m = p.matcher(server);
				if (m.matches())
				{
					ip = "localhost";
					port = m.group(0);
				}
			}
		}
		
		if (m.matches())
		{
			try
			{
				AsynchronousSocketChannel client = AsynchronousSocketChannel.open(p2p_group);
				if ((client.isOpen()) && (ip != null) && (port != null))
				{
					Future<Void> connect_handle = client.connect(new InetSocketAddress(ip, Integer.valueOf(port)));
					connect_handle.get();
					ret = new EndPoint(client);
					endpoints.put(client, ret);
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (ExecutionException ee)
			{
				ee.printStackTrace();
			}
			catch (InterruptedException ie)
			{
				ie.printStackTrace();
			}
		}
		return ret;
	}

	public void run()
	{
		if (listener != null)
		{
			try
			{
				for(;;)
				{
					listen_handle = listener.accept();
					AsynchronousSocketChannel sock = listen_handle.get();
					EndPoint ep = new EndPoint(sock);
					endpoints.put(sock, ep);
					if (this.client_handler != null)
						this.client_handler.process(ep);
				}
			}
			catch (Exception ce)
			{
				listen_handle.cancel(true);
			}
		}
	}
	
	public void close()
	{
		if ((listener != null) && (listen_handle != null))
		{
			listen_handle.cancel(true);
			try 
			{
				listener.close();
			} 
			catch (Exception e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (endpoints != null)
		{
			Enumeration<EndPoint> eps = endpoints.elements();
			while (eps.hasMoreElements())
			{
				EndPoint e = eps.nextElement();
				e.close();
			}
		}
		
		try
		{
			if (!p2p_group.isShutdown()) {
			    // once the group is shut down no more channels can be created with it
				p2p_group.shutdown();
			}
			if (!p2p_group.isTerminated()) {
			    // forcibly shutdown, the channel will be closed and the accept will abort
				p2p_group.shutdownNow();
			}
			// the group should be able to terminate now, wait for a maximum of 10 seconds
			p2p_group.awaitTermination(10, TimeUnit.SECONDS);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	@Override
	public void completed(AsynchronousSocketChannel sock, AsyncStates state) 
	{
		if (state == AsyncStates.SERVER)
		{
			// Accept a new connection and issue a read
			EndPoint ep = new EndPoint(sock);
			endpoints.put(sock, ep);
			if (this.client_handler != null)
				this.client_handler.process(ep);
		}
	}

	@Override
	public void failed(Throwable arg0, AsyncStates arg1) 
	{
		System.out.println("Failed to accept something");
	}

	public static void main(String[] args) throws P2PException
	{
		P2PEndPoint ptpe = new P2PEndPoint(new HandleRead());
		if (args.length > 0)
		{
			EndPoint ep  = ptpe.connect(args[0]);
			for(;;)
			{
				Scanner s = new Scanner(System.in);
				String resp = ep.write(1, s.nextLine(), true);
				if (resp != null)
					System.out.println(resp);
			}
		}
	}
}

class HandleRead implements P2PEndPoint.ClientHandler, P2PEndPoint.ReadHandler
{
	EndPoint ep;
	
	@Override
	public String process(int caller_id, String request) 
	{
		// TODO Auto-generated method stub
		return request.toUpperCase();
	}

	@Override
	public void process(EndPoint ep) 
	{
		this.ep = ep;
		ep.setReadHandler(this);
	}
}