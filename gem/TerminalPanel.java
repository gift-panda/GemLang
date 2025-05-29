package com.interpreter.gem;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.regex.*;

public class TerminalPanel extends JPanel {
    public final JTextPane terminalPane;
    private final StyledDocument doc;
    private int promptPosition;
    private BufferedWriter processInput;
    private Process shellProcess;
    public static Long lastPid = null;  // Store PID from jar

    private static final Pattern PID_PATTERN = Pattern.compile("JAR_PID:(\\d+)");

    public TerminalPanel() {
        super(new BorderLayout());

        terminalPane = new JTextPane() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                super.paintComponent(g);
            }
        };

        terminalPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        terminalPane.setMargin(new Insets(5, 5, 5, 5));
        terminalPane.setBackground(new Color(0x1e1e1e));
        terminalPane.setForeground(Color.WHITE);
        terminalPane.setCaretColor(Color.WHITE);

        doc = terminalPane.getStyledDocument();
        JScrollPane scrollPane = new JScrollPane(terminalPane);
        add(scrollPane, BorderLayout.CENTER);

        ((AbstractDocument) doc).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String str, AttributeSet attr)
                    throws BadLocationException {
                if (offset >= promptPosition) {
                    super.insertString(fb, offset, str, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String str, AttributeSet attr)
                    throws BadLocationException {
                if (offset >= promptPosition) {
                    super.replace(fb, offset, length, str, attr);
                }
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length)
                    throws BadLocationException {
                if (offset >= promptPosition) {
                    super.remove(fb, offset, length);
                }
            }
        });

        terminalPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                try {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        e.consume();
                        String input = doc.getText(promptPosition, doc.getLength() - promptPosition).trim();
                        appendText("\n");

                        if (processInput != null) {
                            processInput.write(input + "\n");
                            processInput.flush();
                        }

                    } else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        if (terminalPane.getCaretPosition() <= promptPosition) {
                            e.consume();
                        }

                    } else if (e.isControlDown()) {
                        if (e.getKeyCode() == KeyEvent.VK_C) {
                            e.consume();
                            if (lastPid != null) {
                                killPidViaShell(lastPid);
                            } else {
                                // fallback: send Ctrl+C to process input stream
                                if (processInput != null) {
                                    processInput.write('\003'); // Ctrl+C char
                                    processInput.flush();
                                }
                            }
                        } else if (e.getKeyCode() == KeyEvent.VK_D) {
                            e.consume();
                            if (processInput != null) {
                                processInput.write('\004');
                                processInput.flush();
                                processInput.close();
                            }
                        }
                    }
                } catch (IOException | BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });

        launchShell();
    }

    public static void killPidViaShell(Long pid) {
        new Thread(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                Process killProcess;
                if (os.contains("win")) {
                    // Windows: use taskkill to send CTRL+C equivalent (actually force kill here)
                    killProcess = new ProcessBuilder("taskkill", "/PID", pid.toString(), "/T", "/F").start();
                } else {
                    // Unix/Linux/Mac: use bash to send SIGINT
                    killProcess = new ProcessBuilder("/bin/bash", "-c", "kill -SIGINT " + pid).start();
                }
                killProcess.waitFor();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void appendText(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.insertString(doc.getLength(), text, null);
                terminalPane.setCaretPosition(doc.getLength());
                promptPosition = doc.getLength();
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void appendAnsiText(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                int pos = 0;
                SimpleAttributeSet currentAttr = new SimpleAttributeSet();

                while (pos < text.length()) {
                    int escIndex = text.indexOf("\033[", pos);
                    if (escIndex == -1) {
                        processOutputLine(text.substring(pos));
                        break;
                    }

                    if (escIndex > pos) {
                        processOutputLine(text.substring(pos, escIndex));
                    }

                    int seqEnd = escIndex + 2;
                    while (seqEnd < text.length() && !Character.isLetter(text.charAt(seqEnd))) {
                        seqEnd++;
                    }
                    if (seqEnd >= text.length()) break;

                    String code = text.substring(escIndex + 2, seqEnd);
                    char command = text.charAt(seqEnd);

                    if (command == 'm') {
                        currentAttr = parseAnsiColor(code);
                    } else if (command == 'J') {
                        if ("2".equals(code)) {
                            doc.remove(0, doc.getLength());
                            promptPosition = 0;
                            terminalPane.setCaretPosition(0);
                        }
                    } else if (command == 'H') {
                        doc.remove(0, doc.getLength());
                        promptPosition = 0;
                        terminalPane.setCaretPosition(0);
                    }

                    pos = seqEnd + 1;
                }

                terminalPane.setCaretPosition(doc.getLength());
                promptPosition = doc.getLength();

            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    public void processOutputLine(String line) throws BadLocationException {
        Matcher m = PID_PATTERN.matcher(line);
        if (m.find()) {
            lastPid = Long.parseLong(m.group(1));
        } else {
            if(line.contains("Process exited with code")) {
                GemGui.runButton.setText("\u25B6 Run");
                terminalPane.setEditable(false);
            }
            terminalPane.setCaretPosition(terminalPane.getDocument().getLength());
            doc.insertString(doc.getLength(), line, null);
        }
    }


    private SimpleAttributeSet parseAnsiColor(String code) {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        if (code.isEmpty() || code.equals("0")) {
            StyleConstants.setForeground(attr, Color.WHITE);
            StyleConstants.setBold(attr, false);
        } else {
            String[] codes = code.split(";");
            for (String c : codes) {
                switch (c) {
                    case "0":
                        StyleConstants.setForeground(attr, Color.WHITE);
                        StyleConstants.setBold(attr, false);
                        break;
                    case "1":
                        StyleConstants.setBold(attr, true);
                        break;
                    case "30":
                        StyleConstants.setForeground(attr, Color.BLACK);
                        break;
                    case "31":
                        StyleConstants.setForeground(attr, Color.RED);
                        break;
                    case "32":
                        StyleConstants.setForeground(attr, new Color(0, 128, 0));
                        break;
                    case "33":
                        StyleConstants.setForeground(attr, Color.YELLOW);
                        break;
                    case "34":
                        StyleConstants.setForeground(attr, Color.BLUE);
                        break;
                    case "35":
                        StyleConstants.setForeground(attr, new Color(128, 0, 128));
                        break;
                    case "36":
                        StyleConstants.setForeground(attr, Color.CYAN);
                        break;
                    case "37":
                        StyleConstants.setForeground(attr, Color.LIGHT_GRAY);
                        break;
                    case "90":
                        StyleConstants.setForeground(attr, Color.DARK_GRAY);
                        break;
                    case "91":
                        StyleConstants.setForeground(attr, Color.PINK);
                        break;
                    case "92":
                        StyleConstants.setForeground(attr, new Color(0, 255, 0));
                        break;
                    case "93":
                        StyleConstants.setForeground(attr, Color.ORANGE);
                        break;
                    case "94":
                        StyleConstants.setForeground(attr, new Color(173, 216, 230));
                        break;
                    case "95":
                        StyleConstants.setForeground(attr, new Color(255, 105, 180));
                        break;
                    case "96":
                        StyleConstants.setForeground(attr, Color.CYAN.brighter());
                        break;
                    case "97":
                        StyleConstants.setForeground(attr, Color.WHITE);
                        break;
                }
            }
        }
        return attr;
    }

    private void launchShell() {
        new Thread(() -> {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder builder;

            if (os.contains("win")) {
                String ps = System.getenv("SystemRoot") + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
                File powershell = new File(ps);
                if (powershell.exists()) {
                    builder = new ProcessBuilder(ps);
                } else {
                    builder = new ProcessBuilder("cmd.exe");
                }
            } else {
                builder = new ProcessBuilder("/bin/bash");
            }

            builder.environment().put("TERM", "xterm");
            builder.redirectErrorStream(true);

            try {
                shellProcess = builder.start();
                InputStreamReader reader = new InputStreamReader(shellProcess.getInputStream());
                processInput = new BufferedWriter(new OutputStreamWriter(shellProcess.getOutputStream()));

                char[] buffer = new char[1024];
                int n;
                StringBuilder sb = new StringBuilder();

                while ((n = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, n);
                    appendAnsiText(sb.toString());
                    sb.setLength(0);
                }
            } catch (IOException e) {
                appendText("Error starting shell: " + e.getMessage() + "\n");
            }
        }).start();
    }
}
