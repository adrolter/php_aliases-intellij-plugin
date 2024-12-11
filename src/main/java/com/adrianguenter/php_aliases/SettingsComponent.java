package com.adrianguenter.php_aliases;

import com.adrianguenter.lib.AutoCompletionData;
import com.adrianguenter.lib.AutoCompletionDataProvider;
import com.adrianguenter.lib.FqnValidator;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.table.JBTable;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.PhpIndexImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SettingsComponent {

    private final Border cellPaddingBorder = BorderFactory.createEmptyBorder(3, 3, 3, 3);
    private final Project project;
    private final JPanel panel;
    private final JBTable table;
    private final AliasTableModel tableModel;

    public SettingsComponent(
            Project project,
            Runnable validationListener
    ) {
        var phpIndex = (PhpIndexImpl) PhpIndex.getInstance(project);

        FqnValidator fqnValidator = (String fqn) -> !phpIndex.getClassesByFQN(fqn).isEmpty()
                || !phpIndex.getInterfacesByFQN(fqn).isEmpty()
                || !phpIndex.getTraitsByFQN(fqn).isEmpty()
                || phpIndex.getAllChildNamespacesFqns("\\").contains(fqn);

        var autoCompletionDataProvider = project.getService(AutoCompletionDataProvider.class);

        this.project = project;
        this.tableModel = new AliasTableModel(validationListener, fqnValidator);
        this.table = new JBTable(this.tableModel);
        this.panel = new JPanel(new BorderLayout());
        this.panel.add(new JScrollPane(this.table), BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            int totalWidth = this.table.getWidth() - 100;
            if (totalWidth < 200) {
                return;
            }

            int aliasColumnWidth = (int) (totalWidth * 0.30);
            int fqnColumnWidth = (int) (totalWidth * 0.70);

            TableColumn aliasColumn = this.table.getColumnModel().getColumn(AliasTableModel.Column.Alias.index());
            aliasColumn.setPreferredWidth(aliasColumnWidth);

            TableColumn fqnColumn = this.table.getColumnModel().getColumn(AliasTableModel.Column.Fqn.index());
            fqnColumn.setPreferredWidth(fqnColumnWidth);

            this.table.revalidate();
            this.table.repaint();
        });

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteMenuItem = new JMenuItem("Delete", AllIcons.General.Delete);
        deleteMenuItem.addActionListener(e -> {
            int selectedRow = this.table.getSelectedRow();
            if (selectedRow == -1) {
                return;
            }

            this.tableModel.removeRowAt(selectedRow);
            this.table.revalidate();
            this.table.repaint();
        });
        popupMenu.add(deleteMenuItem);

        this.table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    this.showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    this.showPopupMenu(e);
                }
            }

            private void showPopupMenu(MouseEvent event) {
                int rowIndex = SettingsComponent.this.table.rowAtPoint(event.getPoint());
                if (rowIndex == -1) {
                    return;
                }

                SettingsComponent.this.table.setRowSelectionInterval(rowIndex, rowIndex);
                deleteMenuItem.setEnabled(!SettingsComponent.this.getTableModel().rowIsLast(rowIndex));
                popupMenu.show(event.getComponent(), event.getX(), event.getY());
            }
        });

        this.table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int row = SettingsComponent.this.table.rowAtPoint(event.getPoint());
                if (row == -1) {
                    return;
                }

                var column = AliasTableModel.Column.forIndex(SettingsComponent.this.table.columnAtPoint(event.getPoint()));

                String text = "";

                var warnings = SettingsComponent.this.tableModel.getValidationWarningsAt(row, column);

                if (!warnings.isEmpty()) {
                    text += String.join("\n", warnings.stream().map(v -> "⚠\uFE0F " + v.message()).toList());
                }

                List<AliasMappingDraft.ValidationError> errors = null;
                if (!SettingsComponent.this.tableModel.rowIsLast(row) || !SettingsComponent.this.tableModel.rowIsEmpty(row)) {
                    errors = SettingsComponent.this.tableModel.getValidationErrorsAt(row, column);
                }

                if (errors != null && !errors.isEmpty()) {
                    text += String.join("\n", errors.stream().map(v -> "\uD83D\uDED1 " + v.message()).toList());
                }

                if (!text.isEmpty()) {
                    text = """
                            <ul>%s</ul>
                            """.formatted(text);
                }

                SettingsComponent.this.table.setToolTipText(text);
            }
        });

        this.table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                //noinspection SwitchStatementWithTooFewBranches
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DELETE -> {
                        int selectedRow = SettingsComponent.this.table.getSelectedRow();
                        if (selectedRow != -1 && !SettingsComponent.this.getTableModel().rowIsLast(selectedRow)) {
                            SettingsComponent.this.tableModel.removeRowAt(selectedRow);

                            if (selectedRow >= SettingsComponent.this.table.getRowCount()) {
                                selectedRow = SettingsComponent.this.table.getRowCount() - 1;
                            }

                            SettingsComponent.this.table.setRowSelectionInterval(selectedRow, selectedRow);
                        }
                    }
                }
            }
        });

        this.table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final Color errorBackgroundLight = Color.decode("#ffaaaa");
            private static final Color errorBackgroundDark = Color.decode("#622727");
            private static final Color warningBackgroundLight = Color.decode("#ffffaa");
            private static final Color warningBackgroundDark = Color.decode("#565600");

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
                JLabel component = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);

                component.setBorder(BorderFactory.createCompoundBorder(
                        component.getBorder(),
                        SettingsComponent.this.cellPaddingBorder
                ));

                var column = AliasTableModel.Column.forIndex(columnIndex);
                var background = table.getBackground(); // JBColor.background();
                var foreground = table.getForeground(); // JBColor.foreground();
                Icon icon = null;

                if (!SettingsComponent.this.tableModel.rowIsLast(rowIndex) || !SettingsComponent.this.tableModel.rowIsEmpty(rowIndex)) {
                    var warnings = SettingsComponent.this.tableModel.getValidationWarningsAt(rowIndex, column);
                    var errors = SettingsComponent.this.tableModel.getValidationErrorsAt(rowIndex, column);

                    //noinspection StatementWithEmptyBody
                    if (hasFocus) {
                        // Noop
                    } else if (!errors.isEmpty()) {
                        if (column == AliasTableModel.Column.Fqn) {
                            icon = AllIcons.General.Error;
                        }
                        background = new JBColor(errorBackgroundLight, errorBackgroundDark);
                    } else if (!warnings.isEmpty()) {
                        if (column == AliasTableModel.Column.Fqn) {
                            icon = AllIcons.General.Warning;
                        }
                        background = new JBColor(warningBackgroundLight, warningBackgroundDark);
                    } else {
                        if (column == AliasTableModel.Column.Fqn) {
                            var autoCompletionData = autoCompletionDataProvider.forFqn(SettingsComponent.this.tableModel.getValueAt(rowIndex, column)).orElse(null);
                            if (autoCompletionData != null) {
                                icon = autoCompletionData.icon();
                            }
                        }

                        if (isSelected) {
                            background = table.getSelectionBackground();
                        }
                    }
                }

                component.setBackground(background);
                component.setForeground(foreground);
                component.setIcon(icon);

                return component;
            }
        });

        this.table.getColumnModel()
                .getColumn(AliasTableModel.Column.Fqn.index())
                .setCellEditor(new FqnCellEditor(phpIndex));
    }

    public JPanel getPanel() {
        return this.panel;
    }

    public AliasTableModel getTableModel() {
        return this.tableModel;
    }

    private class FqnCellEditor
            extends AbstractCellEditor
            implements TableCellEditor {

        private final TextFieldWithAutoCompletion<String> textField;

        FqnCellEditor(
                PhpIndexImpl phpIndex
        ) {
            var completionProvider = new PhpClasslikeCompletionProvider(phpIndex, null);

            this.textField = new TextFieldWithAutoCompletion<>(
                    SettingsComponent.this.project, completionProvider, true, null);

            this.textField.setBorder(BorderFactory.createCompoundBorder(
                    this.textField.getBorder(),
                    SettingsComponent.this.cellPaddingBorder
            ));

            /// Cell keyboard events
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
                var source = event.getSource() instanceof EditorComponentImpl
                        ? (EditorComponentImpl) event.getSource()
                        : null;

                if (source == null || source.getEditor() != this.textField.getEditor()) {
                    return false;
                }

                if (event.getID() == KeyEvent.KEY_PRESSED && event.getKeyCode() == KeyEvent.VK_ENTER) {
                    this.stopCellEditing();
                    event.consume();
                    return true;
                }

                return false;
            });
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column
        ) {
            this.textField.setText(value != null ? value.toString() : "");

            return this.textField;
        }

        @Override
        public Object getCellEditorValue() {
            return this.textField.getText();
        }

        // StringsCompletionWithCacheProvider
        private class PhpClasslikeCompletionProvider
                extends TextFieldWithAutoCompletion.StringsCompletionProvider {

            private final PhpIndexImpl phpIndex;
            private final AutoCompletionDataProvider autoCompletionDataProvider;

            PhpClasslikeCompletionProvider(
                    PhpIndexImpl phpIndex,
                    @Nullable Icon icon
            ) {
                super(null, icon);

                this.phpIndex = phpIndex;
                this.autoCompletionDataProvider = SettingsComponent.this.project.getService(AutoCompletionDataProvider.class);
            }

            @Override
            public @NotNull Collection<String> getItems(
                    String prefix,
                    boolean cached,
                    CompletionParameters parameters
            ) {
                if (cached) {
                    return new ArrayList<>();
                }

                var matcher = new PlainPrefixMatcher(prefix);

                var classFqns = this.phpIndex.getAllClassFqns(matcher).stream().filter(v -> !this.phpIndex.getClassesByFQN(v).isEmpty()).toList();
                var fqns = new HashSet<>(classFqns);

                var interfaceFqns = this.phpIndex.getAllInterfacesFqns(matcher).stream().filter(v -> !this.phpIndex.getInterfacesByFQN(v).isEmpty()).toList();
                fqns.addAll(interfaceFqns);

                var traitFqns = this.phpIndex.getAllTraitsFqns(matcher).stream().filter(v -> !this.phpIndex.getTraitsByFQN(v).isEmpty()).toList();
                fqns.addAll(traitFqns);

                var namespaceFqns = this.phpIndex.getAllChildNamespacesFqns("\\").stream().filter(v -> v.contains(prefix) && !this.phpIndex.getNamespacesByName(v).isEmpty()).toList();
                fqns.addAll(namespaceFqns);

                return fqns;
            }

            @Override
            protected @Nullable Icon getIcon(@NotNull String item) {
                return this.getData(item).map(AutoCompletionData::icon).orElse(null);
            }

            @Override
            protected @Nullable String getTailText(@NotNull String item) {
                return this.getData(item).map(AutoCompletionData::tailText).orElse(null);
            }

            @Override
            protected @Nullable String getTypeText(@NotNull String item) {
                return this.getData(item).map(AutoCompletionData::typeText).orElse(null);
            }

            @Override
            protected @NotNull String getLookupString(@NotNull String item) {
                return this.getData(item).map(AutoCompletionData::lookupString).orElse(item);
            }

            @Override
            public @NotNull String getPrefix(@NotNull String text, int offset) {
                String rawPrefix = super.getPrefix(text, offset);
                if (rawPrefix == null) {
                    return "";
                }

                return rawPrefix.replaceFirst("^\\\\+", "");
            }

            private Optional<AutoCompletionData> getData(String fqn) {
                return this.autoCompletionDataProvider.forFqn(fqn);
            }
        }
    }
}
