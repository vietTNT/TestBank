package View;

//import controller.QuestionController; // Giả sử bạn có
import model.Question; // Giả sử bạn có

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import Controller.QuestionController;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class QuestionManagementPanel extends JPanel {

    private JTable questionTable;
    private DefaultTableModel tableModel;
    private JButton btnAddQuestion, btnEditQuestion, btnDeleteQuestion;
    private JTextField txtSearch;
    private JButton btnSearchAction; // Đổi tên để tránh nhầm lẫn
    private QuestionController controller;
    private JButton btnImportFromImage; // Nút mới
    
    
    // Màu sắc cho JTable
    private Color evenRowColor = UIManager.getColor("Table.alternateRowColor"); // Lấy từ L&F
    private Color oddRowColor = UIManager.getColor("Table.background");
    private Color tableGridColor = UIManager.getColor("Table.gridColor");
    private Color tableHeaderBackground = UIManager.getColor("TableHeader.background");
    private Color tableHeaderForeground = UIManager.getColor("TableHeader.foreground");


    public QuestionManagementPanel(QuestionController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 15)); // Tăng khoảng cách dọc
        setBorder(new EmptyBorder(15, 20, 15, 20)); // Tăng padding
        setBackground(UIManager.getColor("Panel.background"));

        initTopPanel();
        initCenterPanel();
        initBottomPanel(); // Có thể thêm panel phân trang hoặc thông tin thêm ở đây

        if (this.controller == null) {
            System.err.println("QuestionManagementPanel: Controller is null during construction. UI Test Mode.");
        }
  //      loadSampleData();
    }
    public void setController(QuestionController controller) {
        this.controller = controller;
        
    }
    private void initTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(20, 0)); // Tăng khoảng cách giữa toolbar và search
        topPanel.setOpaque(false); // Trong suốt để lấy nền của QuestionManagementPanel

        // --- Toolbar ---
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setOpaque(false);
        toolBar.setBorder(new EmptyBorder(5,0,5,0));

  
        btnAddQuestion = createToolBarButton("Thêm Câu hỏi");
        btnEditQuestion = createToolBarButton("Sửa Câu hỏi");
        btnDeleteQuestion = createToolBarButton("Xóa Câu hỏi");
        btnImportFromImage = createToolBarButton("Nhập từ Ảnh");
        
        toolBar.add(btnAddQuestion);
        toolBar.addSeparator(new Dimension(8,0));
        toolBar.add(Box.createHorizontalStrut(20));
        
        toolBar.add(btnEditQuestion);
        toolBar.addSeparator(new Dimension(8,0));
        toolBar.add(Box.createHorizontalStrut(20));
        
        toolBar.add(btnDeleteQuestion);
        toolBar.addSeparator(new Dimension(8,0));
        toolBar.add(Box.createHorizontalStrut(20));
        
        toolBar.add(btnImportFromImage);
        btnImportFromImage.addActionListener(e -> {
                 if (controller != null) { // controller là QuestionController
                     ImageToTestDialog importDialog = new ImageToTestDialog(
                         (Frame) SwingUtilities.getWindowAncestor(this),
                         controller
                     );
                     importDialog.setVisible(true);
                 }
             });
        topPanel.add(toolBar, BorderLayout.WEST);

        // --- Search Panel ---
        JPanel searchContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        searchContainer.setOpaque(false);

        JPanel searchPanel = new JPanel() { // Panel tùy chỉnh với bo góc
             @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(UIManager.getColor("TextField.background"));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25); // Bo góc cho search panel
                g2d.setColor(UIManager.getColor("Component.borderColor"));
                g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 25, 25);
                g2d.dispose();
            }
        };
        searchPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8)); // Padding bên trong
        searchPanel.setOpaque(false);
   ;
        
        txtSearch = new JTextField(25);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm kiếm câu hỏi...");
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearch.setBorder(null); // Bỏ viền mặc định của JTextField
        txtSearch.setOpaque(false); // Trong suốt để nền của searchPanel hiển thị

        btnSearchAction = new JButton(IconUtils.createImageIcon("Search.png", "Search", 18, 18));
        btnSearchAction.setBorder(null);
        btnSearchAction.setContentAreaFilled(false);
        btnSearchAction.setFocusPainted(false);
        btnSearchAction.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearchAction.setToolTipText("Tìm kiếm");
        
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearchAction);
        searchContainer.add(searchPanel);
        topPanel.add(searchContainer, BorderLayout.EAST);


        add(topPanel, BorderLayout.NORTH);

        // Add actions
        btnAddQuestion.addActionListener(this::addQuestionAction);
        btnEditQuestion.addActionListener(this::editQuestionAction);
        btnDeleteQuestion.addActionListener(this::deleteQuestionAction);
        btnSearchAction.addActionListener(this::searchAction);
        txtSearch.addActionListener(this::searchAction); // Tìm kiếm khi nhấn Enter
    }
    
    private JButton createToolBarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(8, 18, 8, 18)); 

        return button;
    }


    private void initCenterPanel() {
        String[] columnNames = {"ID", "Nội dung câu hỏi", "Loại", "Mức độ", "Đa phương tiện", "Ghi chú"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        questionTable = new JTable(tableModel) {
            // Vẽ màu xen kẽ cho các hàng
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? evenRowColor : oddRowColor);
                } else {
                     c.setBackground(UIManager.getColor("Table.selectionBackground"));
                }
                // Thêm padding cho cell
                if (c instanceof JComponent) {
                    ((JComponent) c).setBorder(new EmptyBorder(5, 8, 5, 8));
                }
                return c;
            }
        };

        questionTable.setRowHeight(45); // Tăng chiều cao hàng
        questionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        questionTable.setAutoCreateRowSorter(true);
        questionTable.setGridColor(tableGridColor);

        questionTable.setShowGrid(true); // Hiển thị đường kẻ ngang và dọc
        questionTable.setIntercellSpacing(new Dimension(1, 1)); // Khoảng cách giữa các cell (chỉ dọc nếu ngang là 0)

        // Tùy chỉnh Header
        JTableHeader header = questionTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.setBackground(tableHeaderBackground);
        header.setForeground(tableHeaderForeground);
        header.setPreferredSize(new Dimension(header.getWidth(), 50)); // Tăng chiều cao header
        header.setReorderingAllowed(false); // Không cho kéo thả cột
        
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        headerRenderer.setBorder(new EmptyBorder(0,12,0,12)); // Padding cho header
        
        // Tùy chỉnh Renderer cho các cột
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        
        TableColumnModel columnModel = questionTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(60);  // ID
        columnModel.getColumn(0).setCellRenderer(centerRenderer);
        columnModel.getColumn(1).setPreferredWidth(500); // Nội dung
        columnModel.getColumn(2).setPreferredWidth(130); // Loại
        columnModel.getColumn(3).setPreferredWidth(100);  // Mức độ
        columnModel.getColumn(3).setCellRenderer(centerRenderer);
        columnModel.getColumn(4).setPreferredWidth(150); // Đa phương tiện (kết hợp audio/image)
        columnModel.getColumn(4).setCellRenderer(new MediaIndicatorRenderer());
        columnModel.getColumn(5).setPreferredWidth(200); // Ghi chú


        questionTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table =(JTable) mouseEvent.getSource();
                Point point = mouseEvent.getPoint();
                int row = table.rowAtPoint(point);
                if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    editQuestionAction(null);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(questionTable);
        scrollPane.setBorder(new LineBorder(UIManager.getColor("Component.borderColor"), 1)); // Viền cho JScrollPane
        scrollPane.getViewport().setBackground(UIManager.getColor("Table.background")); // Nền cho viewport
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void initBottomPanel() {
        // Panel này có thể dùng để hiển thị thông tin phân trang, số lượng câu hỏi, v.v.
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
 
    }



    
    public void displayQuestions(List<Question> questions) {
        tableModel.setRowCount(0);
        if (questions != null) {
            for (Question q : questions) {
                tableModel.addRow(new Object[]{
                        q.getQuestionId(),
                        q.getQuestionText(),
                        q.getQuestionType(),
                        q.getDifficultyLevel(),
                        new MediaInfo(q.getAudioFile() != null, q.getImageFile() != null),
                        q.getNotes()
                });
            }
        }
     
    }

    private void addQuestionAction(ActionEvent e) {

    	  if (controller != null) {
              controller.openAddQuestionDialog();
          } 
    }

    private void editQuestionAction(ActionEvent e) {
    	 int selectedRow = questionTable.getSelectedRow();
         if (selectedRow >= 0) {
             int modelRow = questionTable.convertRowIndexToModel(selectedRow);
             int questionId = (int) tableModel.getValueAt(modelRow, 0);
             if (controller != null) {
                 controller.openEditQuestionDialog(questionId);
             } 
         } else {
             JOptionPane.showMessageDialog(this, "Vui lòng chọn một câu hỏi để sửa.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
         }
    }

    private void deleteQuestionAction(ActionEvent e) {
    	int selectedRow = questionTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = questionTable.convertRowIndexToModel(selectedRow);
            int questionId = (int) tableModel.getValueAt(modelRow, 0);
            String questionText = (String) tableModel.getValueAt(modelRow, 1);
            int confirm = JOptionPane.showConfirmDialog(this,
                  "Bạn có chắc chắn muốn xóa câu hỏi:\n\"" + questionText.substring(0, Math.min(questionText.length(), 50)) + "...\"?",
                  "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (controller != null && confirm == JOptionPane.YES_OPTION) {
                controller.deleteQuestion(questionId);
            } else {
                 
            }
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một câu hỏi để xóa.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void searchAction(ActionEvent e) {
    	 String searchTerm = txtSearch.getText();
         if (controller != null) {
            controller.searchQuestions(searchTerm);
        } 
    }

    // Lớp nội bộ để lưu trữ thông tin media cho bảng
    private static class MediaInfo {
        boolean hasAudio;
        boolean hasImage;
        public MediaInfo(boolean hasAudio, boolean hasImage) {
            this.hasAudio = hasAudio;
            this.hasImage = hasImage;
        }
    }

    // Lớp nội bộ để render icon cho cột Đa phương tiện
    private static class MediaIndicatorRenderer extends DefaultTableCellRenderer {
        private ImageIcon audioIcon;
        private ImageIcon imageIcon;
        private JPanel panel;
        private JLabel audioLabel;
        private JLabel imageLabel;

        public MediaIndicatorRenderer() {
            audioIcon = IconUtils.createImageIcon("music.png", "Audio", 18, 18);
            imageIcon = IconUtils.createImageIcon("image.png", "Image", 18, 18);
            
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
            panel.setOpaque(true); // Để màu nền của cell được vẽ
            audioLabel = new JLabel(audioIcon);
            imageLabel = new JLabel(imageIcon);
            panel.add(audioLabel);
            panel.add(imageLabel);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            // Lấy màu nền từ prepareRenderer của JTable
            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
            } else {
                panel.setBackground(row % 2 == 0 ? table.getBackground() : UIManager.getColor("Table.alternateRowColor"));
                 if (row % 2 == 0) {
                     panel.setBackground(UIManager.getColor("Table.background"));
                 } else {
                     // Cần một cách để lấy màu oddRowColor từ QuestionManagementPanel
                     // Tạm thời dùng một màu gần đúng hoặc UIManager key nếu có
                     Color oddColor = UIManager.getColor("Table.alternateRowColor");
                     if (oddColor == null) oddColor = table.getBackground().brighter(); // Fallback
                     panel.setBackground(oddColor);
                 }
            }

            if (value instanceof MediaInfo) {
                MediaInfo info = (MediaInfo) value;
                audioLabel.setVisible(info.hasAudio);
                imageLabel.setVisible(info.hasImage);
            } else {
                audioLabel.setVisible(false);
                imageLabel.setVisible(false);
            }
            return panel;
        }
    }
}
