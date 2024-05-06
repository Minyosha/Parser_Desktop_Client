package gb;

import com.sun.net.httpserver.HttpServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Main extends JFrame {
    private JTextField portField;
    private JButton startButton;
    private JButton stopButton;
    private JButton exitButton;
    private HttpServer server;

    public Main() {
        setTitle("Parser client");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        portField = new JTextField("8082");
        portField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!portField.getText().matches("[0-9]*")) {
                    portField.setText("8082");
                }
            }
        });

        startButton = new JButton("Start client");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int port = Integer.parseInt(portField.getText());
                try {
                    server = HttpServer.create(new InetSocketAddress(port), 0);
                    server.createContext("/project", new MyHandler());
                    server.setExecutor(null);
                    new Thread(() -> {
                        server.start();
                    }).start();

                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        stopButton = new JButton("Stop client");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
        });

        exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        JPanel panel = new JPanel(new GridLayout(4, 1));
        panel.add(portField);
        panel.add(startButton);
        panel.add(stopButton);
        panel.add(exitButton);

        add(panel);
    }

    private void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Main frame = new Main();
                frame.setVisible(true);
            }
        });
    }
}