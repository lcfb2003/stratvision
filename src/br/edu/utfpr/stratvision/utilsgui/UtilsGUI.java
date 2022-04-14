package br.edu.utfpr.stratvision.utilsgui;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 *
 * @author Luis C. F. Bueno - 23/11/2021
 */
public class UtilsGUI {
    
    public static void centerFrame(JFrame frame) {
        
         // frame.setLocationRelativeTo(null);  
        
         Dimension paneSize = frame.getSize();
         
         Dimension screenSize = frame.getToolkit().getScreenSize();
         frame.setLocation( (screenSize.width - paneSize.width) / 2, (screenSize.height - paneSize.height) / 2);
    }
    
    public static void addFormatedText(JTextPane pane, String word, AttributeSet set){  
        
        Document doc = pane.getStyledDocument();  
        
        try {  
            doc.insertString(doc.getLength(),word, set);  
        } catch (BadLocationException e) {  
            e.printStackTrace(System.err);
        }  
    } 
}
