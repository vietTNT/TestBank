package View;

import Controller.TestController;
import model.Test; // Giả sử bạn có model.TestModel (hoặc model.Test)

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date; // Cho ngày tạo mẫu
import java.util.List;
import java.awt.event.ActionEvent; 
public class TestManagementPanel extends JPanel {
	
	private JTable testTable;
    private DefaultTableModel tableModel;
    private JButton btnCreateTest, btnEditTest, btnDeleteTest, btnViewDetails;
    private JTextField txtSearchTest;
    private JButton btnSearchTestAction;
    private TestController controller;
    private JButton btnCreateRandomTest;
    private Color evenRowColor;
    private Color oddRowColor;
    private JButton  btnExportDOCWithAnswers;
    
    public TestManagementPanel(TestController controller) {
        this.controller = controller;
        initializeUIColors();
        setLayout(new BorderLayout(10, 15));
        setBorder(new EmptyBorder(20, 25, 20, 25));
        setBackground(UIManager.getColor("Panel.background"));

        initTopPanel();
        initCenterPanel();

        if (this.controller == null) {
            System.err.println("TestManagementPanel: Controller is null");
            loadSampleData(); 
        }
    }
    private void initializeUIColors() {
        evenRowColor = UIManager.getColor("Table.alternateRowColor");
        if (evenRowColor == null) evenRowColor = new Color(242, 245, 250);
        oddRowColor = UIManager.getColor("Table.background");
        if (oddRowColor == null) oddRowColor = Color.WHITE;
    }

    public void setController(TestController controller) {
        this.controller = controller;
    }

    private void initTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(25, 0));
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

   
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setOpaque(false);
        toolBar.setBorder(null);
       
        
        btnCreateTest = createToolBarButton("Tạo Đề Mới");
        btnEditTest = createToolBarButton("Sửa Đề");
        btnDeleteTest = createToolBarButton("Xóa Đề");
        btnViewDetails = createToolBarButton("Xem Chi Tiết");
        btnCreateRandomTest = createToolBarButton("Tạo Ngẫu Nhiên"); 
        
        btnExportDOCWithAnswers = createToolBarButton("DOC (Đề+Đáp án)"); 
        
        toolBar.add(btnCreateTest);
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(btnEditTest);
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(btnDeleteTest);
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(btnViewDetails);
       
        toolBar.add(Box.createHorizontalStrut(20)); // Khoảng cách trước nút mới
        toolBar.add(btnCreateRandomTest);
        toolBar.add(Box.createHorizontalGlue()); 


        toolBar.add(Box.createHorizontalGlue()); // Đẩy sang phải
    
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(btnExportDOCWithAnswers); 
        toolBar.add(Box.createHorizontalStrut(5));
    
        topPanel.add(toolBar, BorderLayout.CENTER); 

        // Search Panel (có thể đặt ở một dòng riêng nếu muốn)
        JPanel searchContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0,0));
        searchContainer.setOpaque(false);
         JPanel searchPanel = new JPanel() {
             @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(UIManager.getColor("TextField.background"));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2d.setColor(UIManager.getColor("Component.borderColor"));
                g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 30, 30);
                g2d.dispose();
            }
        };
        searchPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
        searchPanel.setOpaque(false);
      

        txtSearchTest = new JTextField(18);
        txtSearchTest.putClientProperty("JTextField.placeholderText", "Tìm kiếm đề thi...");
        txtSearchTest.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearchTest.setBorder(null);
        txtSearchTest.setOpaque(false);

        btnSearchTestAction = new JButton(IconUtils.createImageIcon("search.png", "Tìm kiếm", 20, 20));
        btnSearchTestAction.setBorder(null);
        btnSearchTestAction.setContentAreaFilled(false);
        btnSearchTestAction.setFocusPainted(false);
        btnSearchTestAction.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearchTestAction.setToolTipText("Tìm kiếm");

        searchPanel.add(txtSearchTest);
        searchPanel.add(btnSearchTestAction);
        searchContainer.add(searchPanel);
        topPanel.add(searchContainer, BorderLayout.EAST); // Đặt search ở EAST của topPanel

        add(topPanel, BorderLayout.NORTH);

        // Add actions
        btnCreateTest.addActionListener(this::createTestAction);
        btnEditTest.addActionListener(this::editTestAction);
        btnDeleteTest.addActionListener(this::deleteTestAction);
        btnViewDetails.addActionListener(this::viewDetailsAction);
        btnSearchTestAction.addActionListener(this::searchTestAction);
        txtSearchTest.addActionListener(this::searchTestAction);
        

  
        btnExportDOCWithAnswers.addActionListener(e -> exportDocAction(true));  
        btnCreateRandomTest.addActionListener(this::createRandomTestAction);
    }
    
    private void exportPdfAction(boolean includeAnswers) {
        int testId = getSelectedTestId();
        if (testId != -1 && controller != null) controller.exportTestToPdfWithOptions(testId, includeAnswers);
        else if (controller == null) showErrorDialog();
    }
    private void exportDocAction(boolean includeAnswers) {
        int testId = getSelectedTestId();
        if (testId != -1 && controller != null) controller.exportTestToDocxWithOptions(testId, includeAnswers);
        else if (controller == null) showErrorDialog();
    }
    private void generateAnswerKeyAction(ActionEvent e) {
        int testId = getSelectedTestId();
        if (testId != -1 && controller != null) controller.generateAndSaveAnswerKeyFile(testId);
        else if (controller == null) showErrorDialog();
    }
    
    private JButton createToolBarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(5, 8, 5, 8));
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        return button;
    }
    
    private void initCenterPanel() {
        String[] columnNames = {"ID Đề", "Tên Đề Thi", "Mô Tả", "Số Câu Hỏi", "Ngày Tạo"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        testTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? evenRowColor : oddRowColor);
                    c.setForeground(UIManager.getColor("Table.foreground"));
                } else {
                     c.setBackground(UIManager.getColor("Table.selectionBackground"));
                     c.setForeground(UIManager.getColor("Table.selectionForeground"));
                }
                if (c instanceof JComponent) {
                    ((JComponent) c).setBorder(new EmptyBorder(10, 12, 10, 12));
                }
                return c;
            }
        };
        testTable.setRowHeight(40);
        testTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        testTable.setAutoCreateRowSorter(true);
        testTable.setGridColor(UIManager.getColor("Table.gridColor"));
        testTable.setShowHorizontalLines(true);
        testTable.setShowVerticalLines(true);
        testTable.setIntercellSpacing(new Dimension(1,1));

        JTableHeader header = testTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.setBackground(UIManager.getColor("TableHeader.background"));
        header.setForeground(UIManager.getColor("TableHeader.foreground"));
        header.setPreferredSize(new Dimension(header.getWidth(), 45));
        header.setReorderingAllowed(false);
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        headerRenderer.setBorder(new EmptyBorder(0,12,0,12));

        TableColumnModel columnModel = testTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(60);
        columnModel.getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){{setHorizontalAlignment(CENTER);}});
        columnModel.getColumn(1).setPreferredWidth(300); 
        columnModel.getColumn(2).setPreferredWidth(400); 
        columnModel.getColumn(3).setPreferredWidth(100);
        columnModel.getColumn(3).setCellRenderer(new DefaultTableCellRenderer(){{setHorizontalAlignment(CENTER);}});
        columnModel.getColumn(4).setPreferredWidth(150); 
        columnModel.getColumn(4).setCellRenderer(new DefaultTableCellRenderer(){{setHorizontalAlignment(CENTER);}});


        testTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2 && testTable.getSelectedRow() != -1) {
                    viewDetailsAction(null); // Hoặc editTestAction(null) tùy logic
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(testTable);
        scrollPane.setBorder(new LineBorder(UIManager.getColor("Component.borderColor"), 1));
        scrollPane.getViewport().setBackground(oddRowColor);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadSampleData() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        tableModel.addRow(new Object[]{1, "Đề N5 Tổng Hợp 01", "Kiểm tra ngữ pháp, từ vựng, đọc hiểu N5", 50, sdf.format(new Date())});
        tableModel.addRow(new Object[]{2, "Đề N4 Nghe Hiểu Phần 1", "Các bài nghe ngắn về chủ đề hàng ngày", 25, sdf.format(new Date())});
        tableModel.addRow(new Object[]{3, "Đề N3 Kanji Chuyên Sâu", "Tập trung vào các Kanji thường gặp N3", 100, sdf.format(new Date())});
    }

    public void displayTests(List<Test> tests) { // Sử dụng TestModel (hoặc model.Test)
        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        if (tests != null) {
            for (Test t : tests) {
                tableModel.addRow(new Object[]{
                        t.getTestId(),
                        t.getTestName(),
                        t.getDescription(),
                        t.getQuestionsInTest() != null ? t.getQuestionsInTest().size() : 0, // Số câu hỏi
                        t.getCreatedAt() != null ? sdf.format(t.getCreatedAt()) : "N/A"
                });
            }
        }
    }
    
    private void createTestAction(ActionEvent e) {
        if (controller != null) {
             controller.openCreateTestDialog();
        
        } else {
            JOptionPane.showMessageDialog(this, "Controller chưa được thiết lập.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editTestAction(ActionEvent e) {
        int selectedRow = testTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = testTable.convertRowIndexToModel(selectedRow);
            int testId = (int) tableModel.getValueAt(modelRow, 0);
            if (controller != null) {
                 controller.openEditTestDialog(testId);
                JOptionPane.showMessageDialog(this, "Mở Dialog Sửa Đề Thi ID: " + testId + " (Controller xử lý).");
            } else {
                JOptionPane.showMessageDialog(this, "Controller chưa được thiết lập.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đề thi để sửa.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void deleteTestAction(ActionEvent e) {
        int selectedRow = testTable.getSelectedRow();
       if (selectedRow >= 0) {
           int modelRow = testTable.convertRowIndexToModel(selectedRow);
           int testId = (int) tableModel.getValueAt(modelRow, 0);
           String testName = (String) tableModel.getValueAt(modelRow, 1);
           int confirm = JOptionPane.showConfirmDialog(this,
                   "Bạn có chắc chắn muốn xóa đề thi:\n\"" + testName + "\" (ID: " + testId + ")?",
                   "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
           if (confirm == JOptionPane.YES_OPTION) {
               if (controller != null) {
                    controller.deleteTest(testId);
              
               } else {
                    JOptionPane.showMessageDialog(this, "Controller chưa được thiết lập.", "Lỗi", JOptionPane.ERROR_MESSAGE);
               }
           }
       } else {
           JOptionPane.showMessageDialog(this, "Vui lòng chọn một đề thi để xóa.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
       }
   }

   private void viewDetailsAction(ActionEvent e) {
       int selectedRow = testTable.getSelectedRow();
       if (selectedRow >= 0) {
           int modelRow = testTable.convertRowIndexToModel(selectedRow);
           int testId = (int) tableModel.getValueAt(modelRow, 0);
            if (controller != null) {
                controller.viewTestDetails(testId);
         
           } else {
               JOptionPane.showMessageDialog(this, "Controller chưa được thiết lập.", "Lỗi", JOptionPane.ERROR_MESSAGE);
           }
       } else {
           JOptionPane.showMessageDialog(this, "Vui lòng chọn một đề thi để xem chi tiết.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
       }
   }
   
   private void searchTestAction(ActionEvent e) {
       String searchTerm = txtSearchTest.getText();
       if (controller != null) {
           controller.searchTests(searchTerm);
      
       } else {
            JOptionPane.showMessageDialog(this, "Controller chưa được thiết lập.", "Lỗi", JOptionPane.ERROR_MESSAGE);
       }
   }





   private void createRandomTestAction(ActionEvent e) {
       if (controller != null) controller.openCreateRandomizedTestDialog();
       else showErrorDialog();
   }

   private int getSelectedTestId() {
       int selectedRow = testTable.getSelectedRow();
       if (selectedRow >= 0) {
           int modelRow = testTable.convertRowIndexToModel(selectedRow);
           return (int) tableModel.getValueAt(modelRow, 0);
       } else {
           JOptionPane.showMessageDialog(this, "Vui lòng chọn một đề thi.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
           return -1;
       }
   }
   private void showErrorDialog() {
       JOptionPane.showMessageDialog(this, "Controller chưa được thiết lập.", "Lỗi", JOptionPane.ERROR_MESSAGE);
   }
    
}
