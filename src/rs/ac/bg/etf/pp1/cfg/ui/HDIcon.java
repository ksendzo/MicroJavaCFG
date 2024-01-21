package rs.ac.bg.etf.pp1.cfg.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class HDIcon implements Icon
{
    private Icon icon;
    private double scale = 0.5;

    public HDIcon(Icon icon)
    {
        this.icon = icon;
    }

    public int getIconWidth()
    {
        return (int)(icon.getIconWidth() * scale);
    }

    public int getIconHeight()
    {
        return (int)(icon.getIconHeight() * scale);
    }
    
    public Image getImage() {
    	return ((ImageIcon)icon).getImage();
    }
    
    public void setScale(double scale) {
    	this.scale = scale;
    }
    
    public void paintIcon(Component c, Graphics g, int x, int y)
    {		
        Graphics2D g2d = (Graphics2D)g.create();

        AffineTransform at = g2d.getTransform();

        int scaleX = (int)(x * at.getScaleX());
        int scaleY = (int)(y * at.getScaleY());

        int offsetX = (int)(icon.getIconWidth() * (at.getScaleX() - 1) / 2);
        int offsetY = (int)(icon.getIconHeight() * (at.getScaleY() - 1) / 2);

        int locationX = scaleX + offsetX;
        int locationY = scaleY + offsetY;

        //  Reset scaling to 1.0 by concatenating an inverse scale transfom

        AffineTransform scaled = AffineTransform.getScaleInstance(scale * 1.0 / at.getScaleX(), scale * 1.0 / at.getScaleY());
        at.concatenate( scaled );
        g2d.setTransform( at );

        icon.paintIcon(c, g2d, locationX, locationY);

        g2d.dispose();
    }
}

