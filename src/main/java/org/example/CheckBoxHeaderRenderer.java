package org.example;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class CheckBoxHeaderRenderer extends JCheckBox implements TableCellRenderer {

    public CheckBoxHeaderRenderer(JCheckBox checkBox) {
        // Aynı model ve metni ayarlama
        this.setModel(checkBox.getModel());
        this.setText(checkBox.getText());

        // Font ayarlarını burada yapabilirsiniz
        this.setFont(new Font("Arial", Font.PLAIN, 13));

        this.setHorizontalAlignment(JCheckBox.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }
}
