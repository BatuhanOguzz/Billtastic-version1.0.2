package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

class DateTableCellRenderer extends DefaultTableCellRenderer {
    private final Set<Date> sentDates;
    private final Set<Date> unsentDates;

    public DateTableCellRenderer(Set<Date> sentDates, Set<Date> unsentDates) {
        this.sentDates = sentDates;
        this.unsentDates = unsentDates;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof Date) {
            Date date = (Date) value;
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, HH:mm:ss yyyy", new Locale("tr", "TR"));
            String formattedDate = dateFormat.format(date);
            setText(formattedDate);

            if (unsentDates.contains(date)) {
                if (date.before(new Date())) {
                    cell.setBackground(Color.RED); // Süresi geçmiş ve teslim edilmemiş
                } else {
                    cell.setBackground(Color.YELLOW); // Süresi gelmemiş
                }
            } else if (sentDates.contains(date)) {
                cell.setBackground(Color.GREEN); // Teslim edilmiş
            } else {
                cell.setBackground(Color.YELLOW); // Varsayılan olarak sarı
            }
        } else {
            cell.setBackground(Color.YELLOW); // Varsayılan olarak sarı
        }
        return cell;
    }
}
