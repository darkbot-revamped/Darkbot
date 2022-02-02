package com.github.manolo8.darkbot.gui.nearplayers;

import com.github.manolo8.darkbot.config.PlayerInfo;
import com.github.manolo8.darkbot.config.PlayerTag;
import com.github.manolo8.darkbot.gui.utils.UIUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NearPlayerRenderer extends JPanel implements ListCellRenderer<PlayerInfo> {

    private final JLabel playerName = new JLabel();
    private final JLabel id = new JLabel();

    public NearPlayerRenderer() {
        super(new MigLayout("ins 4px 0px 4px 5px, fill, h 28px!", "[50px!]8px![120px!]8px:push[]8px!", "[]"));
        id.setFont(id.getFont().deriveFont(9f));
        id.setHorizontalAlignment(SwingConstants.RIGHT);
        id.setVerticalAlignment(SwingConstants.BOTTOM);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends PlayerInfo> list, PlayerInfo value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        removeAll();

        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

        this.playerName.setText(value.username);
        this.id.setText(String.valueOf(value.userId));

        add(id, "grow");
        add(playerName, "grow");

        return this;
    }

    /**
     * Functions overridden for performance reasons:
     */
    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public void repaint() {}
    @Override
    public void revalidate() {}
    @Override
    public void repaint(long tm, int x, int y, int width, int height) {}
    @Override
    public void repaint(Rectangle r) {}
    @Override
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (propertyName == "text"
                || ((propertyName == "font" || propertyName == "foreground")
                && oldValue != newValue
                && getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey) != null)) {

            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }
    @Override
    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}
    @Override
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {}
    @Override
    public void firePropertyChange(String propertyName, short oldValue, short newValue) {}
    @Override
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {}
    @Override
    public void firePropertyChange(String propertyName, long oldValue, long newValue) {}
    @Override
    public void firePropertyChange(String propertyName, float oldValue, float newValue) {}
    @Override
    public void firePropertyChange(String propertyName, double oldValue, double newValue) {}
    @Override
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
}
