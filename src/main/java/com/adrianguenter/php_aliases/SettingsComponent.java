package com.adrianguenter.php_aliases;

import com.adrianguenter.lib.AutoCompletionData;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SettingsComponent {
    private class FqnCellEditor extends AbstractCellEditor implements TableCellEditor {
        // StringsCompletionWithCacheProvider
        private static class CompletionProvider extends TextFieldWithAutoCompletion.StringsCompletionProvider {

            final Map<String, AutoCompletionData> variants;

            CompletionProvider(Map<String, AutoCompletionData> variants, @Nullable Icon icon) {
                super(Collections.unmodifiableSet(variants.keySet()), icon);

                this.variants = variants;
            }

            @Override
            protected Icon getIcon(@NotNull String item) {
                return this.variants.get(item).icon();
            }

            @Override
            protected @Nullable String getTailText(@NotNull String item) {
                return this.variants.get(item).tailText();
            }

            @Override
            protected String getTypeText(@NotNull String item) {
                return this.variants.get(item).typeText();
            }

            @Override
            protected @NotNull String getLookupString(@NotNull String item) {
                var lookupString = this.variants.get(item).lookupString();

                return lookupString != null ? lookupString : item;
            }
        }

        private final TextFieldWithAutoCompletion<String> textField;

        FqnCellEditor(Map<String, AutoCompletionData> completions) {
            CompletionProvider completionProvider = new CompletionProvider(completions, null);
            this.textField = new TextFieldWithAutoCompletion<>(
                    SettingsComponent.this.project, completionProvider, true, null);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.textField.setText(value != null ? value.toString() : "");
            return textField;
        }

        @Override
        public Object getCellEditorValue() {
            return this.textField.getText();
        }
    }

    private final Project project;
    private final JPanel panel;
    private final JBTable table;
    private final AliasTableModel tableModel;

    public SettingsComponent(Project project, Map<String, AutoCompletionData> knownFqns, Runnable validationListener) {
        this.project = project;
        this.tableModel = new AliasTableModel(validationListener, knownFqns.keySet());
        this.table = new JBTable(this.tableModel);
        this.panel = new JPanel(new BorderLayout());
        this.panel.add(new JScrollPane(this.table), BorderLayout.CENTER);

        setDefaultColumnWidths();

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteMenuItem = new JMenuItem("Delete");
        deleteMenuItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                return;
            }

            this.tableModel.removeRowAt(selectedRow);
            this.table.revalidate();
            this.table.repaint();
        });
        popupMenu.add(deleteMenuItem);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            private void showPopupMenu(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row == -1) {
                    return;
                }

                table.setRowSelectionInterval(row, row);
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row == -1) {
                    return;
                }

                var column = AliasTableModel.Column.forIndex(table.columnAtPoint(e.getPoint()));

                String text = "";

                var warnings = tableModel.getValidationWarningsAt(row, column);

                if (!warnings.isEmpty()) {
                    text += String.join("\n", warnings.stream().map(v -> "WARNING: " + v.message()).toList());
                }

                List<AliasMappingDraft.ValidationError> errors = null;
                if (!tableModel.rowIsLast(row) || !tableModel.rowIsEmpty(row)) {
                    errors = tableModel.getValidationErrorsAt(row, column);
                }

                if (errors != null && !errors.isEmpty()) {
                    text += String.join("\n", errors.stream().map(v -> "ERROR: " + v.message()).toList());
                }

                table.setToolTipText(text.trim());
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                Color background = table.getBackground(); // JBColor.background();
                Color foreground = table.getForeground(); // JBColor.foreground();

                if (!tableModel.rowIsLast(row) || !tableModel.rowIsEmpty(row)) {
                    var warnings = tableModel.getValidationWarningsAt(row, AliasTableModel.Column.forIndex(column));
                    var errors = tableModel.getValidationErrorsAt(row, AliasTableModel.Column.forIndex(column));

                    if (!errors.isEmpty()) {
                        background = new JBColor(JBColor.PINK, ColorUtil.darker(JBColor.RED, 10));
                        if (isSelected) {
                            background = ColorUtil.blendColorsInRgb(table.getSelectionBackground(), background, 0.5);
                        }
                    } else if (!warnings.isEmpty()) {
                        background = new JBColor(ColorUtil.brighter(JBColor.YELLOW, 5), ColorUtil.darker(JBColor.YELLOW, 10));
                        if (isSelected) {
                            background = ColorUtil.blendColorsInRgb(table.getSelectionBackground(), background, 0.5);
                        }
                    } else {
                        if (isSelected) {
                            background = table.getSelectionBackground();
                        }
                    }
                }

                component.setBackground(background);
                component.setForeground(foreground);

                return component;
            }
        });

        table.getColumnModel()
                .getColumn(AliasTableModel.Column.Fqn.index())
                .setCellEditor(new FqnCellEditor(knownFqns));
    }

    public JPanel getPanel() {
        return panel;
    }

    public AliasTableModel getTableModel() {
        return tableModel;
    }

    private void setDefaultColumnWidths() {
        SwingUtilities.invokeLater(() -> {
            int totalWidth = table.getWidth() - 100;
            if (totalWidth < 200) {
                return;
            }

            int aliasColumnWidth = (int) (totalWidth * 0.30);
            int fqnColumnWidth = (int) (totalWidth * 0.70);

            TableColumn aliasColumn = table.getColumnModel().getColumn(AliasTableModel.Column.Alias.index());
            aliasColumn.setPreferredWidth(aliasColumnWidth);

            TableColumn fqnColumn = table.getColumnModel().getColumn(AliasTableModel.Column.Fqn.index());
            fqnColumn.setPreferredWidth(fqnColumnWidth);

            table.revalidate();
            table.repaint();
        });
    }
}
