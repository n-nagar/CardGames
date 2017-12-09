import javax.swing.SwingUtilities;

import org.nagars.cardgames.gui.GUITable;


public class Rummy {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
    
    private static void createAndShowGUI() 
    {
    	try {
			GUITable t = new GUITable();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	
    }

}
