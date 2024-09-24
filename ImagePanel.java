package me.chiranjeevi.facialrecognition;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.BufferedImage;


public class ImagePanel extends JPanel {
    private static final long serialVersionUID = -9145505585207919140L;
    /**
     * Image to be displayed to the user
     */
    private BufferedImage image;

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image != null)
            g.drawImage(image, 0, 0, null);
    }
}
