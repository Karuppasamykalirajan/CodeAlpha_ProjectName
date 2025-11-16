// GradeTrackerGUI.java (ASCII-only, fixed strings and no emoji)
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.nio.file.Files;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalDouble;

public class GradeTrackerGUI {
    private JFrame frame;
    private DefaultListModel<String> studentListModel;
    private JList<String> studentList;
    private Map<String, Student> students;
    private JTextArea detailsArea;
    private JLabel statusBar;
    private boolean darkTheme = false;
    private ChartPanel chartPanel;

    public GradeTrackerGUI() {
        students = new HashMap<>();
        initUI();
    }

    private void initUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        frame = new JFrame("Student Grade Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 660);
        frame.setLayout(new BorderLayout());

        studentListModel = new DefaultListModel<>();
        studentList = new JList<>(studentListModel);
        studentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        studentList.setCellRenderer(new StudentCellRenderer());
        JScrollPane leftScroll = new JScrollPane(studentList);
        leftScroll.setPreferredSize(new Dimension(260, 0));
        frame.add(leftScroll, BorderLayout.WEST);

        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        detailsArea.setBackground(new Color(250, 250, 252));
        detailsArea.setMargin(new Insets(10,10,10,10));

        chartPanel = new ChartPanel();
        chartPanel.setPreferredSize(new Dimension(320, 0));

        JPanel center = new JPanel(new BorderLayout());
        center.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
        center.add(chartPanel, BorderLayout.EAST);
        frame.add(center, BorderLayout.CENTER);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(25, 118, 210));

        JButton addStudentBtn = makeToolbarButton("Add Student");
        JButton addGradeBtn = makeToolbarButton("Add Grade");
        JButton setWeightBtn = makeToolbarButton("Set Weight");
        JButton removeBtn = makeToolbarButton("Remove");
        JButton saveBtn = makeToolbarButton("Save");
        JButton loadBtn = makeToolbarButton("Load");
        JButton exportSummaryBtn = makeToolbarButton("Export");
        JButton themeToggleBtn = makeToolbarButton("Toggle Theme");

        toolbar.add(addStudentBtn);
        toolbar.add(addGradeBtn);
        toolbar.add(setWeightBtn);
        toolbar.add(removeBtn);
        toolbar.addSeparator(new Dimension(12,0));
        toolbar.add(saveBtn);
        toolbar.add(loadBtn);
        toolbar.add(exportSummaryBtn);
        toolbar.addSeparator(new Dimension(12,0));
        toolbar.add(themeToggleBtn);

        frame.add(toolbar, BorderLayout.NORTH);

        statusBar = new JLabel(" Ready");
        statusBar.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        frame.add(statusBar, BorderLayout.SOUTH);

        addStudentBtn.addActionListener(e -> onAddStudent());
        addGradeBtn.addActionListener(e -> onAddGrade());
        setWeightBtn.addActionListener(e -> onSetWeight());
        removeBtn.addActionListener(e -> onRemoveStudent());
        saveBtn.addActionListener(e -> onSaveCsv());
        loadBtn.addActionListener(e -> onLoadCsv());
        exportSummaryBtn.addActionListener(e -> onExportSummary());
        themeToggleBtn.addActionListener(e -> toggleTheme());

        studentList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showSelectedStudentDetails(); });

        frame.setLocationRelativeTo(null);
        applyTheme();
        frame.setVisible(true);
    }

    private JButton makeToolbarButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(30, 136, 229));
        b.setBorder(BorderFactory.createEmptyBorder(6,12,6,12));
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return b;
    }

    private void onAddStudent() {
        String name = JOptionPane.showInputDialog(frame, "Enter student name:");
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(frame, "Name cannot be empty."); return; }
        String key = name.toLowerCase();
        if (students.containsKey(key)) { JOptionPane.showMessageDialog(frame, "Student already exists."); return; }
        Student s = new Student(name);
        students.put(key, s);
        studentListModel.addElement(name);
        statusBar.setText(" Added student: " + name);
    }

    private void onAddGrade() {
        String name = studentList.getSelectedValue();
        if (name == null) { JOptionPane.showMessageDialog(frame, "Select a student first."); return; }
        String subject = JOptionPane.showInputDialog(frame, "Enter subject:");
        if (subject == null) return; subject = subject.trim();
        String gradeStr = JOptionPane.showInputDialog(frame, "Enter grade (0-100):");
        if (gradeStr == null) return;
        try {
            int g = Integer.parseInt(gradeStr.trim());
            if (g < 0 || g > 100) throw new NumberFormatException();
            Student s = students.get(name.toLowerCase());
            s.addGrade(subject, g);
            showSelectedStudentDetails();
            statusBar.setText(" Added grade " + g + " to " + name + " (" + subject + ")");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid grade. Please enter integer 0-100.");
        }
    }

    private void onSetWeight() {
        String name = studentList.getSelectedValue();
        if (name == null) { JOptionPane.showMessageDialog(frame, "Select a student first."); return; }
        String subject = JOptionPane.showInputDialog(frame, "Enter subject to set weight for:");
        if (subject == null) return; subject = subject.trim();
        String wStr = JOptionPane.showInputDialog(frame, "Enter weight (e.g. 1.0):");
        if (wStr == null) return;
        try {
            double w = Double.parseDouble(wStr.trim());
            if (w <= 0) throw new NumberFormatException();
            Student s = students.get(name.toLowerCase());
            s.setSubjectWeight(subject, w);
            showSelectedStudentDetails();
            statusBar.setText(" Set weight " + w + " for " + subject + " (" + name + ")");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid weight. Please enter a positive number.");
        }
    }

    private void onRemoveStudent() {
        String name = studentList.getSelectedValue();
        if (name == null) { JOptionPane.showMessageDialog(frame, "Select a student first."); return; }
        int confirm = JOptionPane.showConfirmDialog(frame, "Remove " + name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            students.remove(name.toLowerCase());
            studentListModel.removeElement(name);
            detailsArea.setText("");
            chartPanel.setStudent(null);
            statusBar.setText(" Removed student: " + name);
        }
    }

    private void onSaveCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        int res = fc.showSaveDialog(frame);
        if (res != JFileChooser.APPROVE_OPTION) return;
        Path p = fc.getSelectedFile().toPath();
        if (!p.toString().toLowerCase().endsWith(".csv")) p = Paths.get(p.toString() + ".csv");
        try {
            CSVUtils.saveStudents(p, new ArrayList<>(students.values()));
            JOptionPane.showMessageDialog(frame, "Saved to " + p);
            statusBar.setText(" Saved CSV: " + p.getFileName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Failed to save: " + ex.getMessage());
        }
    }

    private void onLoadCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        int res = fc.showOpenDialog(frame);
        if (res != JFileChooser.APPROVE_OPTION) return;
        Path p = fc.getSelectedFile().toPath();
        try {
            List<Student> loaded = CSVUtils.loadStudents(p);
            for (Student s : loaded) {
                String key = s.getName().toLowerCase();
                if (students.containsKey(key)) {
                    Student existing = students.get(key);
                    for (String subj : s.getSubjects()) {
                        for (int g : s.getGrades(subj)) existing.addGrade(subj, g);
                        existing.setSubjectWeight(subj, s.getSubjectWeight(subj));
                    }
                } else {
                    students.put(key, s);
                    studentListModel.addElement(s.getName());
                }
            }
            JOptionPane.showMessageDialog(frame, "Loaded " + loaded.size() + " students from CSV.");
            statusBar.setText(" Loaded CSV: " + p.getFileName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Failed to load: " + ex.getMessage());
        }
    }

    private void onExportSummary() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        int res = fc.showSaveDialog(frame);
        if (res != JFileChooser.APPROVE_OPTION) return;
        Path p = fc.getSelectedFile().toPath();
        if (!p.toString().toLowerCase().endsWith(".csv")) p = Paths.get(p.toString() + ".csv");
        try (BufferedWriter bw = Files.newBufferedWriter(p)) {
            bw.write("name,overall_avg,weighted_avg,subjects_count,highest,lowest\n");
            for (Student s : students.values()) {
                String name = s.getName();
                String overall = s.getOverallAverage().isPresent() ? String.format("%.2f", s.getOverallAverage().getAsDouble()) : "-";
                String weighted = s.getWeightedAverage().isPresent() ? String.format("%.2f", s.getWeightedAverage().getAsDouble()) : "-";
                int subjCount = s.getSubjects().size();
                OptionalInt highest = s.getSubjects().stream()
                        .mapToInt(subj -> s.getSubjectHighest(subj).orElse(Integer.MIN_VALUE))
                        .filter(v -> v != Integer.MIN_VALUE).max();
                OptionalInt lowest = s.getSubjects().stream()
                        .mapToInt(subj -> s.getSubjectLowest(subj).orElse(Integer.MAX_VALUE))
                        .filter(v -> v != Integer.MAX_VALUE).min();
                String h = highest.isPresent() ? String.valueOf(highest.getAsInt()) : "-";
                String l = lowest.isPresent() ? String.valueOf(lowest.getAsInt()) : "-";
                bw.write(String.format("%s,%s,%s,%d,%s,%s\n", name, overall, weighted, subjCount, h, l));
            }
            JOptionPane.showMessageDialog(frame, "Summary exported to " + p);
            statusBar.setText(" Exported summary: " + p.getFileName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Failed to export summary: " + ex.getMessage());
        }
    }

    private void showSelectedStudentDetails() {
        String name = studentList.getSelectedValue();
        if (name == null) { detailsArea.setText(""); chartPanel.setStudent(null); return; }
        Student s = students.get(name.toLowerCase());
        StringBuilder sb = new StringBuilder();
        sb.append("Student: ").append(s.getName()).append("\n");
        sb.append("Subjects: ").append(s.getSubjects().size()).append("\n");
        for (String subj : s.getSubjects()) {
            sb.append("\n").append(subj).append(":\n");
            sb.append(s.subjectSummary(subj)).append("\n");
        }
        sb.append("\n");
        sb.append("Overall average: ").append(s.getOverallAverage().isPresent() ? String.format("%.2f", s.getOverallAverage().getAsDouble()) : "-").append("\n");
        sb.append("Weighted average: ").append(s.getWeightedAverage().isPresent() ? String.format("%.2f", s.getWeightedAverage().getAsDouble()) : "-").append("\n");
        detailsArea.setText(sb.toString());
        chartPanel.setStudent(s);
        statusBar.setText(" Viewing: " + s.getName());
    }

    private void toggleTheme() {
        darkTheme = !darkTheme;
        applyTheme();
    }

    private void applyTheme() {
        if (darkTheme) {
            detailsArea.setBackground(new Color(43,43,43));
            detailsArea.setForeground(new Color(220,220,220));
            studentList.setBackground(new Color(60,63,65));
            studentList.setForeground(new Color(220,220,220));
            statusBar.setBackground(new Color(40,40,40));
            statusBar.setForeground(new Color(200,200,200));
            frame.getContentPane().setBackground(new Color(48,50,52));
        } else {
            detailsArea.setBackground(new Color(250,250,252));
            detailsArea.setForeground(Color.BLACK);
            studentList.setBackground(Color.WHITE);
            studentList.setForeground(Color.DARK_GRAY);
            statusBar.setBackground(null);
            statusBar.setForeground(Color.DARK_GRAY);
            frame.getContentPane().setBackground(null);
        }
        frame.repaint();
    }

    private static class StudentCellRenderer extends JLabel implements ListCellRenderer<String> {
        public StudentCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
        }
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value);
            if (isSelected) {
                setBackground(new Color(0, 120, 215));
                setForeground(Color.WHITE);
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }
    private static class ChartPanel extends JPanel {
        private Student student;
        public ChartPanel() { setPreferredSize(new Dimension(320, 200)); }
        public void setStudent(Student s) { this.student = s; repaint(); }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(245,245,248));
            g2.fillRect(0,0,w,h);
            if (student == null || student.getSubjects().isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.drawString("No student selected or no subjects", 10, 20);
                return;
            }
            Map<String, Double> averages = new LinkedHashMap<>();
            double max = 0;
            for (String subj : student.getSubjects()) {
                OptionalDouble od = student.getSubjectAverage(subj);
                double v = od.isPresent() ? od.getAsDouble() : 0.0;
                averages.put(subj, v);
                if (v > max) max = v;
            }
            if (max < 10) max = 10;
            int padding = 30;
            int barAreaW = w - padding*2;
            int count = Math.max(1, averages.size());
            int barWidth = Math.max(20, barAreaW / count - 10);
            int x = padding;
            int baseY = h - padding - 20;
            g2.setColor(Color.DARK_GRAY);
            g2.drawLine(padding-5, baseY, w-padding+5, baseY);
            for (Map.Entry<String, Double> e : averages.entrySet()) {
                double val = e.getValue();
                int barH = (int) ((val / max) * (h - padding*3));
                int y = baseY - barH;
                g2.setColor(new Color(100, 149, 237));
                g2.fillRect(x, y, barWidth, barH);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(x, y, barWidth, barH);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                String label = e.getKey();
                int strW = g2.getFontMetrics().stringWidth(label);
                int lx = x + Math.max(0, (barWidth - strW)/2);
                g2.drawString(label, lx, baseY + 15);
                String valStr = String.format("%.0f", val);
                int valW = g2.getFontMetrics().stringWidth(valStr);
                g2.drawString(valStr, x + Math.max(0, (barWidth - valW)/2), y - 6);
                x += barWidth + 10;
            }
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GradeTrackerGUI::new);
    }
}
