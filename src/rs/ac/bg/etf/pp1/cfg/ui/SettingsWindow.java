package rs.ac.bg.etf.pp1.cfg.ui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import rs.ac.bg.etf.pp1.cfg.structure.Parser;

public class SettingsWindow {

    private JTextField dotField, programField, tabField, folderField;

    private static JFrame frame;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SettingsWindow().createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        frame = new JFrame("Graf Toka Kontrole - Podešavanja");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(6, 3, 30, 20));
        
        JLabel infoLabel = new JLabel("Ksenija Bulatović - ETF 2023");
        
        panel.add(new JLabel());
        panel.add(infoLabel);
        panel.add(new JLabel());

        dotField = new JTextField("C:\\\\Program Files\\Graphviz\\bin\\dot.exe");
        programField = new JTextField("test/program.obj");
        tabField = new JTextField("test/sym_tab.mjtab");
        folderField = new JTextField(".\\temp");

        JButton dotButton = createBrowseButton(dotField, "exe");
        JButton programButton = createBrowseButton(programField, "obj");
        JButton tabButton = createBrowseButton(tabField, "mjtab");
        JButton folderButton = createBrowseButton(folderField, "folder");

        JButton saveButton = new JButton("Učitaj");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                load();
            }
        });

        panel.add(new JLabel("   Putanja do dot.exe fajla:"));
        panel.add(dotField);
        panel.add(dotButton);

        panel.add(new JLabel("   Putanja do .obj fajla:"));
        panel.add(programField);
        panel.add(programButton);

        panel.add(new JLabel("   Putanja do .mjtab fajla (opciono):"));
        panel.add(tabField);
        panel.add(tabButton);

        panel.add(new JLabel("   Putanja do foldera za čuvanje grafova:"));
        panel.add(folderField);
        panel.add(folderButton);
        
        panel.add(new JLabel());
        panel.add(saveButton);

        frame.getContentPane().add(panel);
        frame.setSize(800, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JButton createBrowseButton(JTextField textField, String fileType) {
        JButton browseButton = new JButton("Pronađi");
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	
                JFileChooser fileChooser = new JFileChooser();
                
                if (fileType.equals("folder")) {
            	    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                }
                else {
	                FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files", fileType);
	                fileChooser.setFileFilter(filter);
                }

                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    String selectedPath = fileChooser.getSelectedFile().getAbsolutePath();
                    textField.setText(selectedPath);
                }
            }
        });
        return browseButton;
    }

    private void load() {
        String dotPath = dotField.getText();
        String objPath = programField.getText();
        String mjTabPath = tabField.getText();
        String folderPath = folderField.getText();
      
        frame.dispose();
        Parser.runCfgApp(dotPath, objPath, mjTabPath, folderPath);
       
    }
}
