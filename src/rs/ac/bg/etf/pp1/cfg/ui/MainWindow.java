package rs.ac.bg.etf.pp1.cfg.ui;
import javax.imageio.ImageIO;
import javax.swing.*;

import rs.ac.bg.etf.pp1.cfg.structure.Block;
import rs.ac.bg.etf.pp1.cfg.structure.Parser;
import rs.ac.bg.etf.pp1.cfg.structure.Instruction;
import rs.ac.bg.etf.pp1.cfg.tab.TabLoader;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MainWindow {
	
	private static String folderPath;
	
	private static JLabel imageLabel;
	private static Point origin = new Point();
    private static JScrollPane imageScrollPanel;
    
    private static double scaleFactor = 0.5;
    private static double ZOOM_FACTOR = 1.2;
    
    private static JLabel currentFunctionLabel = new JLabel();
    
    private static Color color1 = new Color(0xfafafa);
    private static Color color2 = new Color(0xeaeaea);
    private static Color fontColor = Color.black;
    private static Color hoverColor = new Color(0xf14f21);
    private static Color selectedItemColor = new Color(0xb9ff66);
    
    private static Font myFont = new Font("Book Antiqua", Font.PLAIN, 16);
	
    public static void runApp(String folderPathInput) {
    	folderPath = folderPathInput;
    	
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Graf Kontrole Toka");
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JPanel mainPanel = new JPanel(new BorderLayout());

            JScrollPane leftScrollPanel = createScrollableTextPanel("Left Panel", Color.WHITE);
            imageScrollPanel = createScrollableImagePanel((((JPanel)((JViewport)leftScrollPanel.getComponent(0)).getComponent(0)).getComponent(0)).getName());

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScrollPanel, imageScrollPanel);
            splitPane.setResizeWeight(0.33); // Initial divider position

            JPanel topPanel = createTopPanel();            
            mainPanel.add(topPanel, BorderLayout.NORTH);
            mainPanel.add(splitPane, BorderLayout.CENTER);
            frame.getContentPane().add(mainPanel);

            frame.setVisible(true);
        });
    }

    private static JPanel createTopPanel() {
    	JPanel topPanel =  new JPanel(new GridLayout(1, 2));     
    	
        JPanel labelPanel = new JPanel();
		currentFunctionLabel.setText("selected: " + currentSelected.getName());
		labelPanel.add(currentFunctionLabel);
		topPanel.add(labelPanel);      
	
     	return topPanel;
    }
    
    private static JScrollPane createScrollableTextPanel(String labelText, Color bgColor) {
    	JPanel panel = new JPanel(new GridLayout(0,1));
    	panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));    	

        for (int functionToWrite = 0; functionToWrite < Parser.functions.size(); functionToWrite++) {
        	JPanel functionPanel = new JPanel(new GridLayout(0,1));
        	functionPanel.setBackground(functionToWrite%2==0?color1:color2);
        	functionPanel.setBorder(BorderFactory.createLineBorder(functionToWrite%2==0?color1:color2, 4));
        	
        	if (Parser.withTab)
        		functionPanel.setName(TabLoader.globalMethods.get(Parser.functions.get(functionToWrite).get(0).startAdr).getName());
        	else 
        		functionPanel.setName("" + Parser.functions.get(functionToWrite).get(0).startAdr);
        	
        	JLabel Namelabel = new JLabel(functionPanel.getName());
    		Namelabel.setForeground(fontColor);
    		Namelabel.setFont(myFont);
    		Namelabel.setHorizontalAlignment(JLabel.CENTER); 
    		functionPanel.add(Namelabel);
        	
            for (Block b: Parser.functions.get(functionToWrite)) {
            	for (Instruction instr: b.instructions) {
            		JLabel label = new JLabel(instr.toString());
            		label.setForeground(fontColor);
            		label.setFont(myFont);
            		label.setHorizontalAlignment(JLabel.LEFT); 
            		functionPanel.add(label);
            	}
            }
            addListeners(functionPanel);
            panel.add(functionPanel);
            if (functionToWrite == 0) {
            	currentSelected = functionPanel;

            	currentSelected = functionPanel;
            	currentSelectedColor = functionPanel.getBackground();
            	
            	functionPanel.setBackground(selectedItemColor);
            	for (Component c: currentSelected.getComponents()) {
        			c.setForeground(Color.black);
        		}
            	
            	currentFunctionLabel.setText("selected: " + functionPanel.getName());
            }
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }
    
    
    
    public static JPanel currentSelected = null;
    public static Color currentSelectedColor = null;
    
    private static void addListeners(JPanel panel) {
    	panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Change the border color on hover
                panel.setBorder(BorderFactory.createLineBorder(hoverColor, 4));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Restore the original border color on exit
            	panel.setBorder(null);
                panel.setBorder(BorderFactory.createLineBorder(e.getComponent().getBackground(), 4));
            }
            @Override
            public void mouseClicked(MouseEvent e) {
            	select(panel);
            	
//	            createImage(name);
            }
        });
    }
    
    private static void select(JPanel panel) {
    	if (currentSelected != null) {
    		currentSelected.setBackground(currentSelectedColor);
    		currentSelected.setBorder(BorderFactory.createLineBorder(currentSelectedColor, 4));
    	}
    	currentSelected = panel;
    	currentSelectedColor = panel.getBackground();
    	
    	panel.setBackground(selectedItemColor);
    	loadFunction(panel.getName());
    	
    	currentFunctionLabel.setText("selected: " + panel.getName());
    }
    
    private static void loadFunction(String functionName) {
    	String name = folderPath + "\\" + functionName + ".png";
    	HDIcon newImageIcon = createImageIcon(name);
        imageLabel.setIcon(newImageIcon);
    }
    
    private static JScrollPane createScrollableImagePanel(String imageName) {
    	BufferedImage bufferedImage;
		try {
			bufferedImage = ImageIO.read(new File(folderPath + "\\" + imageName + ".png"));
			Graphics2D g2d = bufferedImage.createGraphics();
			g2d.drawImage(
			        bufferedImage, 
			        0, 
			        0, 
			        bufferedImage.getWidth(), 
			        bufferedImage.getHeight(), 
			        null
			);
			g2d.dispose();
			
			ImageIO.write(
			        bufferedImage, 
			        "png", 
			        new File(folderPath + "\\" + imageName)
			);
		HDIcon imageIcon = createImageIcon(folderPath + "\\" + imageName);

        imageLabel = new JLabel(imageIcon);
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setBackground(new Color(0xeeeeee));
        imageLabel.setOpaque(true);

        JScrollPane scrollPane = new JScrollPane(imageLabel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBackground(new Color(0xeeeeee));

        scrollPane.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                origin.x = e.getX();
                origin.y = e.getY();
      
            }
        });

        scrollPane.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                JViewport viewPort = scrollPane.getViewport();
                Point p = viewPort.getViewPosition();
                int deltaX = origin.x - e.getX();
                int deltaY = origin.y - e.getY();
//                if (currentPosition.x + deltaX < 0) deltaX = - currentPosition.x ;
//                if (currentPosition.y + deltaY < 0) deltaY = - currentPosition.y ;
                p.translate(deltaX, deltaY);
                viewPort.setViewPosition(p);
                origin.x = e.getX();
                origin.y = e.getY();
//                currentPosition.x += deltaX;
//                currentPosition.y += deltaY;
            }
        });
        
        scrollPane.addMouseWheelListener(new MouseAdapter() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                if (notches < 0) {
                    // Zoom in
                    scaleFactor *= ZOOM_FACTOR;
                } else {
                    // Zoom out
                    scaleFactor /= ZOOM_FACTOR;
                }
                updateImageZoom(imageLabel, scaleFactor);
            }
        });
        
        return scrollPane;
        	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }
    
    private static void updateImageZoom(JLabel label, double scale) {
    	HDIcon imageIcon = (HDIcon) label.getIcon();
    	imageIcon.setScale(scale);
    	
    	Image image = imageIcon.getImage();
    	
    	int newWidth = (int) (image.getWidth(null) * scale);
        int newHeight = (int) (image.getHeight(null) * scale);
//        Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
//        label.setIcon(new NoScalingIcon(new ImageIcon(scaledImage)));
        label.repaint();
    }
    
   private static HDIcon createImageIcon(String filePath) {
        try {
            Image image = ImageIO.read(new File(filePath));
            return new HDIcon(new ImageIcon(image));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
