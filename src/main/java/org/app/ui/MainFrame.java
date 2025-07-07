package org.app.ui;

import org.app.generator.JsonGenerator;
import org.app.model.ExcelDto;
import org.app.reader.ExcelReader;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.List;

public class MainFrame extends JFrame {
    private final JTextArea logArea = new JTextArea();

    public MainFrame() {
        super("Generare JSON din excel.");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 400);

        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);

        JButton browseBtn = new JButton("Browse Excel File");
        browseBtn.addActionListener(e -> onBrowse());

        JPanel pnl = new JPanel(new GridLayout(1,1,10,10));
        pnl.add(browseBtn);

        getContentPane().add(pnl, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
    }

    private void onBrowse() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(false);

        // Add this line to restrict selection to xlsx files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx");
        fc.setFileFilter(filter);

        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File file = fc.getSelectedFile();
        try {
            log("Processing " + file);
            List<ExcelDto> dtos = ExcelReader.read(file);
            JsonGenerator.generate(dtos, file, this::log);
            JOptionPane.showMessageDialog(this, "Success");
        } catch (Exception ex) {
            log("ERROR: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }
}
