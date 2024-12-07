package com.adrianguenter.php_aliases;

import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

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
                tableModel.removeRowAt(selectedRow);
            }
        });
        popupMenu.add(deleteMenuItem);
        table.setComponentPopupMenu(popupMenu);

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }

                int column = table.columnAtPoint(e.getPoint());

                List<AliasMappingDraft.ValidationError> errors;
                if (!tableModel.rowIsLast(row) || !tableModel.rowIsEmpty(row)) {
                    errors = tableModel.getValidationErrorsAt(row, AliasTableModel.Column.forIndex(column));
                }
                else {
                    errors = new ArrayList<>();
                }

                table.setToolTipText(!errors.isEmpty()
                        ? String.join("\n", errors.stream().map(AliasMappingDraft.ValidationError::message).toList())
                        : null);
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                List<AliasMappingDraft.ValidationError> errors;
                if (!tableModel.rowIsLast(row) || !tableModel.rowIsEmpty(row)) {
                    errors = tableModel.getValidationErrorsAt(row, AliasTableModel.Column.forIndex(column));
                }
                else {
                    errors = new ArrayList<>();
                }

                if (!errors.isEmpty()) {
                    component.setBackground(new JBColor(JBColor.PINK, ColorUtil.darker(JBColor.RED, 10)));
                } else {
                    component.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                }

                return component;
            }
        });
    }

    public JPanel getPanel() {
        return panel;
    }

    public AliasTableModel getTableModel() {
        return tableModel;
    }

    private void setDefaultColumnWidths() {
        SwingUtilities.invokeLater(() -> {
            int tableWidth = table.getWidth();
            if (tableWidth < 200) {
                return;
            }

            int aliasColumnWidth = (int) (tableWidth * 0.3);
            int fqnColumnWidth = (int) (tableWidth * 0.7);

            TableColumn aliasColumn = table.getColumnModel().getColumn(AliasTableModel.Column.Alias.index());
            aliasColumn.setPreferredWidth(aliasColumnWidth);

            TableColumn fqnColumn = table.getColumnModel().getColumn(AliasTableModel.Column.Fqn.index());
            fqnColumn.setPreferredWidth(fqnColumnWidth);

            table.revalidate();
            table.repaint();
        });
    }
}
