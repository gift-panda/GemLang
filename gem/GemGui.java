package com.interpreter.gem;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.URISyntaxException;

public class GemGui {
    private static File selectedFile = null;
    private static boolean darkMode = false;
    public static JButton runButton;

    public static void main(String[] args) {
        UIManager.put("Label.opaque", Boolean.TRUE);
        UIManager.put("Label.font", new JLabel().getFont());
        UIManager.put("Label.foreground", Color.BLACK);
        UIManager.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        SwingUtilities.invokeLater(GemGui::createGUI);
    }

    private static void createGUI() {
        JFrame frame = new JFrame("Gem Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);

        JTextArea editorArea = new JTextArea();
        setupTextArea(editorArea);

        TerminalPanel terminalPanel = new TerminalPanel();
        terminalPanel.terminalPane.setEditable(false);

        JScrollPane editorScroll = new JScrollPane(editorArea);

        runButton = new JButton("\u25B6 Run");
        runButton.setFocusPainted(false);
        JButton loadButton = new JButton("\uD83D\uDCC2 Load File");
        loadButton.setFocusPainted(false);
        JButton clearButton = new JButton("\u274C Clear File");
        clearButton.setFocusPainted(false);
        JButton themeButton = new JButton("\uD83C\uDF19 Dark Mode");
        themeButton.setFocusPainted(false);
        JLabel fileLabel = new JLabel("");

        clearButton.setEnabled(false);

        JPanel topBar = new JPanel(new BorderLayout(10, 5));
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        leftButtons.add(loadButton);
        leftButtons.add(clearButton);
        leftButtons.add(themeButton);

        JPanel rightRun = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        rightRun.add(runButton);

        topBar.add(leftButtons, BorderLayout.WEST);
        topBar.add(rightRun, BorderLayout.EAST);

        JPanel selectedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        selectedPanel.add(fileLabel);

        JPanel topControls = new JPanel();
        topControls.setLayout(new BoxLayout(topControls, BoxLayout.Y_AXIS));
        topControls.add(topBar);
        topControls.add(selectedPanel);

        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.add(topControls, BorderLayout.NORTH);
        editorPanel.add(editorScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanel, terminalPanel);
        splitPane.setDividerLocation(500);
        applySplitPaneStyle(splitPane);

        frame.add(splitPane);
        frame.setVisible(true);

        editorArea.setBorder(new RoundedBorder(10));

        loadButton.addActionListener(e -> {
            FileDialog fileDialog = new FileDialog(frame, "Select a .gem file", FileDialog.LOAD);
            fileDialog.setDirectory(getJarDirPath());
            fileDialog.setFilenameFilter((dir, name) -> name.endsWith(".gem"));
            fileDialog.setVisible(true);

            if (fileDialog.getFile() != null) {
                selectedFile = new File(fileDialog.getDirectory(), fileDialog.getFile());
                try {
                    String content = new String(Files.readAllBytes(selectedFile.toPath()), StandardCharsets.UTF_8);
                    editorArea.setText(content);
                    fileLabel.setText(selectedFile.getName());
                    clearButton.setEnabled(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Failed to load file.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        clearButton.addActionListener(e -> {
            selectedFile = null;
            editorArea.setText("");
            fileLabel.setText("");
            clearButton.setEnabled(false);
        });

        themeButton.addActionListener(e -> {
            darkMode = !darkMode;
            themeButton.setText(darkMode ? "☀ Light Mode" : "🌙 Dark Mode");
            Color bg = darkMode ? new Color(0x1e1e1e) : Color.WHITE;
            Color fg = darkMode ? Color.WHITE : Color.BLACK;
            Color panelBg = darkMode ? new Color(0x2c2c2c) : null;
            Color divider = darkMode ? new Color(0x3c3c3c) : UIManager.getColor("SplitPane.background");

            editorArea.setBackground(bg);
            editorArea.setForeground(fg);

            editorArea.setCaretColor(fg);

            terminalPanel.setBackground(bg);
            terminalPanel.setForeground(fg);

            Font buttonFont = new Font("Segoe UI", Font.PLAIN, 12);
            Insets buttonPadding = new Insets(4, 8, 4, 8); // top, left, bottom, right

            JButton[] buttons = {loadButton, clearButton, themeButton, runButton};
            for (JButton btn : buttons) {
                btn.setFont(buttonFont);
                btn.setMargin(buttonPadding);
                btn.setBackground(panelBg);
                btn.setForeground(fg);
                btn.setOpaque(false);
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                btn.setBorder(new RoundedBorder(12));
            }


            Component[] panels = {
                    fileLabel, selectedPanel, topBar, leftButtons, rightRun,
                    editorScroll.getViewport(), terminalPanel
            };
            for (Component comp : panels) {
                comp.setBackground(panelBg);
                comp.setForeground(fg);
            }

            SwingUtilities.invokeLater(() -> {
                applySplitPaneStyle(splitPane);
                splitPane.setBackground(divider);
                splitPane.setUI(splitPane.getUI());
            });
        });

        themeButton.doClick();


        runButton.addActionListener(e -> {

            if(runButton.getText().contains("Stop")) {
                TerminalPanel.killPidViaShell(TerminalPanel.lastPid);
                SwingUtilities.invokeLater(() -> {terminalPanel.terminalPane.setEditable(false);runButton.setText("▶ Run");;});
                return;
            }
            try {
                terminalPanel.terminalPane.setEditable(true);

                KeyEvent pressEvent = new KeyEvent(terminalPanel.terminalPane, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n');
                KeyEvent releaseEvent = new KeyEvent(terminalPanel.terminalPane, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n');

                File tempFile = new File("main.gem");
                Files.writeString(tempFile.toPath(), editorArea.getText(), StandardCharsets.UTF_8);
                tempFile.deleteOnExit();

                String os = System.getProperty("os.name");
                String command = "";
                if (os.contains("win")) {
                    String ps = System.getenv("SystemRoot") + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
                    File powershell = new File(ps);
                    if (powershell.exists()) {
                        command = "Clear-Host; java -jar Interpreter.jar " + tempFile.getName() + "; Write-Output \"Process exited with code $LASTEXITCODE\"";
                    } else {
                        command = "cls & java -jar Interpreter.jar " + tempFile.getName() + " & echo Process exited with code %ERRORLEVEL%";
                    }
                } else {
                    command = "clear; java -jar Interpreter.jar " + tempFile.getName()+ "; echo \"Process exited with code $?\"";

                }

                for (char c : command.toCharArray()) {
                    KeyEvent keyEvent = new KeyEvent(
                            terminalPanel.terminalPane,
                            KeyEvent.KEY_TYPED,
                            System.currentTimeMillis(),
                            0,
                            KeyEvent.VK_UNDEFINED,
                            c
                    );
                    terminalPanel.terminalPane.dispatchEvent(keyEvent);
                }
                terminalPanel.terminalPane.dispatchEvent(pressEvent);
                terminalPanel.terminalPane.dispatchEvent(releaseEvent);

                runButton.setText("⏹ Stop");

            } catch (IOException ex) {
                System.out.println("Error writing temp file: " + ex.getMessage());
            }
        });
    }

    private static void setupTextArea(JTextArea area) {
        Font font = new Font("Noto Sans Mono", Font.PLAIN, 14);
        area.setFont(font);
        area.setMargin(new Insets(8, 8, 8, 8));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
    }

    private static void applySplitPaneStyle(JSplitPane splitPane) {
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider divider = super.createDefaultDivider();
                divider.setBackground(darkMode ? new Color(0x3c3c3c) : Color.LIGHT_GRAY);
                return divider;
            }
        });
        splitPane.setDividerSize(6);
    }

    public static String getJarDirPath() {
        try {
            File jarFile = new File(GemGui.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            File jarDir = jarFile.getParentFile();
            if (jarDir != null) {
                return jarDir.getAbsolutePath();
            } else {
                return null;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}

class RoundedBorder implements Border {
    private final int radius;
    public RoundedBorder(int radius) {
        this.radius = radius;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(this.radius+1, this.radius+1, this.radius+2, this.radius);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        g.setColor(c.getForeground());
        g.drawRoundRect(x + 5, y + 5, width - 10, height - 15, radius, radius);
    }
}

