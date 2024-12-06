package com.adrianguenter.php_aliases;

import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Map;

public class SettingsComponent {

    private final JPanel panel;
    private final JBTable table;
    private final AliasTableModel tableModel;

    public SettingsComponent() {
        tableModel = new AliasTableModel();
        table = new JBTable(tableModel);
        panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        setDefaultColumnWidths();

        // Add right-click context menu
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteMenuItem = new JMenuItem("Delete");
        deleteMenuItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1 && selectedRow < tableModel.getRowCount() - 1) {
                tableModel.removeRow(selectedRow);
            }
        });
        popupMenu.add(deleteMenuItem);

        table.setComponentPopupMenu(popupMenu);
    }

    public JPanel getPanel() {
        return panel;
    }

    public Map<String, String> getAliasMappings() {
        return tableModel.getAliasMappings();
    }

    public void setAliasMappings(Map<String, String> aliasMappings) {
        tableModel.setAliasMappings(aliasMappings);
    }

    private void setDefaultColumnWidths() {
        SwingUtilities.invokeLater(() -> {
            int tableWidth = table.getWidth();
            if (tableWidth < 200) {
                return;
            }

            int aliasColumnWidth = (int) (tableWidth * 0.3); // 30%
            int fqnColumnWidth = (int) (tableWidth * 0.7);   // 70%

            TableColumn aliasColumn = table.getColumnModel().getColumn(0); // Alias column
            aliasColumn.setPreferredWidth(aliasColumnWidth);

            TableColumn fqnColumn = table.getColumnModel().getColumn(1);   // FQN column
            fqnColumn.setPreferredWidth(fqnColumnWidth);

            table.revalidate();
            table.repaint();
        });
    }
}
