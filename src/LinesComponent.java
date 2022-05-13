// https://stackoverflow.com/questions/5801734/how-to-draw-lines-in-java
// Antonio Fernandez 
// 301393610

package src;

import javax.swing.JComboBox;
import java.awt.*;
import java.util.LinkedList;

import javax.swing.*;

public class LinesComponent extends JComponent {
    private class Line {
        private int x1;
        private int x2;
        private int y1;
        private int y2;
        private Color color;

        public Line(int x1, int y1, int x2, int y2, Color c) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = c;
        }
    }

    private LinkedList linesList = new LinkedList<Line>();

    
    public void insertLine(int x1, int y1, int x2, int y2) {
        insertLine(x1, y1, x2, y2, Color.blue);
    }

    public void insertLine(int x1, int y1, int x2, int y2, Color c) {
        linesList.add(new Line(x1, y1, x2, y2, c));
        repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (int i = 0; i < linesList.size(); i++) {
            Line l = (Line)linesList.get(i);

            g.setColor(l.color);
            g.drawLine(l.x1, l.y1, l.x2, l.y2);
        }
    }
}
