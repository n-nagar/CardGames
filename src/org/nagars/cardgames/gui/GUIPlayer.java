package org.nagars.cardgames.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.nagars.cardgames.core.Player;

public class GUIPlayer implements ActionListener 
{
	private static final String state_file = "CardPlayer.ini";
	private static Properties prop;
	private static GUIPlayer current_player;
	private JTextField name;
	private JTextField url;
	private JPanel ui;
	private String image_url;
	private BufferedImage bi;
	private Player host;
	

	private void setup(String n, String u)
	{
		ui = new JPanel();
		ui.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		ui.setLayout(new GridLayout(3,0));
		Box b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Name:"));
		b.add(Box.createRigidArea(new Dimension(5,0)));
		name = new JTextField(n);
		b.add(name);
		//b.add(Box.createHorizontalGlue());
		ui.add(b);
		b = new Box(BoxLayout.LINE_AXIS);
		b.add(new JLabel("Image URL:"));
		b.add(Box.createRigidArea(new Dimension(5,0)));
		url = new JTextField(u, 48);
		b.add(url);
		//b.add(Box.createHorizontalGlue());
		ui.add(b);
		b = new Box(BoxLayout.LINE_AXIS);
		b.add(Box.createHorizontalGlue());
		JButton jb = new JButton("Cancel");
		jb.addActionListener(this);
		b.add(jb, Box.RIGHT_ALIGNMENT);
		jb = new JButton("Save");
		jb.addActionListener(this);
		b.add(jb, Box.RIGHT_ALIGNMENT);
		ui.add(b);

		prop = new Properties();
	}

	public GUIPlayer() 
	{
		host = new Player();
		setup("", "");
	}

	public GUIPlayer(String name, String url)
	{
		host = new Player();
		setup(name, url);
	}

	public JPanel getPlayerUI()
	{
		return ui;
	}
	
	public static GUIPlayer getCurrentPlayer() throws Exception 
	{
		if (current_player != null)
			return current_player;
		
		synchronized(state_file)
		{
			FileInputStream f = null;
			try 
			{
				current_player = new GUIPlayer();
				prop = new Properties();
				f = new FileInputStream(state_file);
				prop.loadFromXML(f);
				current_player.setName(prop.getProperty("user"));
				current_player.name.setText(prop.getProperty("user"));
				current_player.setImageURL(prop.getProperty("user_image"));
				current_player.url.setText(current_player.image_url);
				System.out.printf("Wecome back %s\n", current_player.host.getName());			
			} catch (Exception e) 
			{
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
		}
		return current_player;
	}

	public BufferedImage getImage()
	{
		return (BufferedImage)bi;
	}
	
	public Player getPlayer()
	{
		return host;
	}
	
	public void setImageURL(String url)
	{
		try {
			this.image_url = url;
			URL img = new URL(url);
			host.setImageURL(img);
			synchronized(prop)
			{
				prop.put("user_image", url);
			}
			this.bi = loadImage(img);
		} catch (MalformedURLException e) 
		{
			// Show the error
		}
	}

	public void setName(String name)
	{
		host.setName(name);
		synchronized(prop)
		{
			prop.put("user", name);
		}	
	}
	
	
	public static BufferedImage loadImage(URL image_url)
	{
		BufferedImage bi = null;
		if (image_url != null)
		{
			try
			{
				bi = ImageIO.read(image_url);
			} catch(IOException e)
			{
				System.out.println(e);
				bi = null;
			}
		}
		return bi;
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
	
//	public JPanel getPlayerUI()
//	{
//		if (prop != null)
//			return new PlayerUI(prop.getProperty("user"), prop.getProperty("user_image"));
//		else
//			return new PlayerUI();
//	}
	

	@Override
	public void actionPerformed(ActionEvent arg0) 
	{
		Component f = ui;
		while ((f != null) && (!(f instanceof Window)))
			f = f.getParent();
		if (arg0.getActionCommand().equalsIgnoreCase("Cancel"))
		{
			if (f != null)
				f.setVisible(false);
		}
		else if (arg0.getActionCommand().equalsIgnoreCase("Save"))
		{
			if (name.getText().length() == 0)
				JOptionPane.showConfirmDialog(ui, "Name cannot be empty", "ERROR", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
			else if (url.getText().length() == 0)
				JOptionPane.showConfirmDialog(ui, "Image URL cannot be empty", "ERROR", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
			else
			{
				// Fetch the URL to validate
				setName(name.getText());
				setImageURL(url.getText());
				save();
				if (f != null)
					f.setVisible(false);
			}
		}
	}
}
