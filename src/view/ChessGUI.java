package view;

import controller.Game;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import javax.swing.*;
import model.board.Move;
import model.board.Position;
import model.pieces.Piece;

public class ChessGUI extends JFrame {

    private static final int TILE_SIZE = 80;
    private static final int BOARD_SIZE = 8 * TILE_SIZE;

    private final Game game;
    private Position selected;
    private List<Position> possibleMoves = new ArrayList<>();
    private Position lastFrom = null, lastTo = null;

    private boolean playerIsWhite = true;
    private boolean vsAI = true;

    private final JLabel timerLabel = new JLabel("Tempo: 00:00", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel("Bom-jogo!", SwingConstants.LEFT);
    private int secondsElapsed = 0;
    private java.util.Timer gameTimer;

    // Painéis para peças capturadas
    private final JPanel capturedWhitePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
    private final JPanel capturedBlackPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));

    public ChessGUI() {
        this.playerIsWhite = askColor();
        this.vsAI = askVsAI();

        this.game = new Game();
        this.game.setHumanColor(playerIsWhite);
        this.game.setVsAI(vsAI);

        setTitle("Chess Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Ajuste de largura dos painéis de peças capturadas
        int capturedPanelWidth = 120; // largura suficiente para todas as peças capturadas
        setSize(BOARD_SIZE + capturedPanelWidth * 2, BOARD_SIZE + 80);

        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Top panel com status, timer e botão reiniciar
        JPanel top = new JPanel(new BorderLayout());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        timerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        top.add(statusLabel, BorderLayout.WEST);
        top.add(timerLabel, BorderLayout.EAST);

        JButton restartButton = new JButton("Reiniciar Jogo");
        top.add(restartButton, BorderLayout.CENTER);
        restartButton.addActionListener(e -> restartGame());

        add(top, BorderLayout.NORTH);

        // Painéis laterais para peças capturadas
        capturedWhitePanel.setPreferredSize(new Dimension(capturedPanelWidth, BOARD_SIZE));
        capturedWhitePanel.setBackground(Color.LIGHT_GRAY);
        capturedBlackPanel.setPreferredSize(new Dimension(capturedPanelWidth, BOARD_SIZE));
        capturedBlackPanel.setBackground(Color.LIGHT_GRAY);

        add(capturedWhitePanel, BorderLayout.EAST);
        add(capturedBlackPanel, BorderLayout.WEST);

        ChessPanel boardPanel = new ChessPanel();
        add(boardPanel, BorderLayout.CENTER);

        startTimer();

        // Movimento inicial da IA se jogador for preto
        if (!playerIsWhite && vsAI) {
            executeAIMove(boardPanel);
        }

        // Listener de clique
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (game.isGameOver()) {
                    JOptionPane.showMessageDialog(ChessGUI.this, "O jogo acabou.");
                    return;
                }

                int row = e.getY() / TILE_SIZE;
                int col = e.getX() / TILE_SIZE;
                if (row < 0 || row > 7 || col < 0 || col > 7) return;

                Position clicked = new Position(row, col);
                Piece clickedPiece = game.getBoard().get(clicked);

                if (selected == null) {
                    if (clickedPiece != null && clickedPiece.isWhite() == game.isWhiteToMove()) {
                        selected = clicked;
                        possibleMoves = game.legalMovesFromWithSpecialsForGui(selected);
                    }
                } else {
                    boolean moveMade = false;
                    Piece moving = game.getBoard().get(selected);
                    if (moving != null) {
                        for (Position p : possibleMoves) {
                            if (p.equals(clicked)) {
                                Piece captured = game.getBoard().get(clicked);
                                Move mv = Move.normal(selected, clicked, moving, captured);
                                if (game.makeMove(mv)) {
                                    lastFrom = selected;
                                    lastTo = clicked;
                                    moveMade = true;

                                    // Atualiza status
                                    statusLabel.setText("Bom-jogo!");
                                    if (game.inCheck(game.isWhiteToMove())) statusLabel.setText("Xeque!");
                                    if (game.isCheckmate(game.isWhiteToMove())) {
                                        JOptionPane.showMessageDialog(ChessGUI.this,
                                                "Xeque-mate! Vencedor: " + (moving.isWhite() ? "Brancas" : "Pretas"));
                                        if (gameTimer != null) gameTimer.cancel();
                                    }

                                    // Atualiza peças capturadas
                                    if (captured != null) {
                                        if (captured.isWhite()) {
                                            capturedWhitePanel.add(new JLabel(
                                                    ImageUtil.getPieceIcon(true, captured.getSymbol(), 24)));
                                        } else {
                                            capturedBlackPanel.add(new JLabel(
                                                    ImageUtil.getPieceIcon(false, captured.getSymbol(), 24)));
                                        }
                                        capturedWhitePanel.revalidate();
                                        capturedBlackPanel.revalidate();
                                    }
                                }
                                break;
                            }
                        }
                    }

                    selected = null;
                    possibleMoves.clear();
                    boardPanel.repaint();

                    // Movimento da IA com atraso
                    if (moveMade && vsAI && !game.isGameOver() && game.isWhiteToMove() != playerIsWhite) {
                        executeAIMove(boardPanel);
                    }
                }
                boardPanel.repaint();
            }
        });

        setVisible(true);
    }

    private void executeAIMove(ChessPanel boardPanel) {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // atraso de 1 segundo
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            SwingUtilities.invokeLater(() -> {
                Move ai = game.getAIMove();
                if (ai != null) {
                    Piece captured = game.getBoard().get(ai.getTo());
                    game.makeMove(ai);
                    lastFrom = ai.getFrom();
                    lastTo = ai.getTo();

                    statusLabel.setText("Bom-jogo!");
                    if (game.inCheck(game.isWhiteToMove())) statusLabel.setText("Xeque!");
                    if (game.isCheckmate(game.isWhiteToMove())) {
                        JOptionPane.showMessageDialog(ChessGUI.this,
                                "Xeque-mate! Vencedor: " + (game.isWhiteToMove() ? "Pretas" : "Brancas"));
                        if (gameTimer != null) gameTimer.cancel();
                    }

                    if (captured != null) {
                        if (captured.isWhite()) {
                            capturedWhitePanel.add(new JLabel(
                                    ImageUtil.getPieceIcon(true, captured.getSymbol(), 24)));
                        } else {
                            capturedBlackPanel.add(new JLabel(
                                    ImageUtil.getPieceIcon(false, captured.getSymbol(), 24)));
                        }
                        capturedWhitePanel.revalidate();
                        capturedBlackPanel.revalidate();
                    }

                    boardPanel.repaint();
                }
            });
        }).start();
    }

    private void restartGame() {
        game.newGame();
        secondsElapsed = 0;
        timerLabel.setText("Tempo: 00:00");
        capturedWhitePanel.removeAll();
        capturedBlackPanel.removeAll();
        capturedWhitePanel.revalidate();
        capturedBlackPanel.revalidate();
        capturedWhitePanel.repaint();
        capturedBlackPanel.repaint();
        statusLabel.setText("Bom-jogo!");
        selected = null;
        possibleMoves.clear();
        lastFrom = lastTo = null;

        if (gameTimer != null) gameTimer.cancel();
        startTimer();

        repaint();

        // Se reiniciar com jogador preto, IA joga de novo primeiro
        if (!playerIsWhite && vsAI) {
            executeAIMove((ChessPanel) getContentPane().getComponent(2));
        }
    }

    private boolean askColor() {
        String[] colors = {"Brancas", "Pretas"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Escolha sua cor:",
                "Cor",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                colors,
                colors[0]
        );
        return choice != 1;
    }

    private boolean askVsAI() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Deseja jogar contra a IA?",
                "Modo",
                JOptionPane.YES_NO_OPTION
        );
        return choice == JOptionPane.YES_OPTION;
    }

    private void startTimer() {
        gameTimer = new java.util.Timer();
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                secondsElapsed++;
                SwingUtilities.invokeLater(() -> {
                    int m = secondsElapsed / 60;
                    int s = secondsElapsed % 60;
                    timerLabel.setText(String.format("Tempo: %02d:%02d", m, s));
                });
            }
        }, 1000, 1000);
    }

    private class ChessPanel extends JPanel {
        ChessPanel() { setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE)); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    boolean light = (r + c) % 2 == 0;
                    g.setColor(light ? Color.WHITE : Color.RED);
                    g.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }

            if (lastFrom != null && lastTo != null) {
                g.setColor(new Color(30, 144, 255, 100));
                g.fillRect(lastFrom.getColumn() * TILE_SIZE, lastFrom.getRow() * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                g.fillRect(lastTo.getColumn() * TILE_SIZE, lastTo.getRow() * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }

            g.setColor(new Color(0, 200, 0, 100));
            for (Position p : possibleMoves) {
                g.fillRect(p.getColumn() * TILE_SIZE, p.getRow() * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }

            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    Position pos = new Position(r, c);
                    Piece p = game.getBoard().get(pos);
                    if (p != null) {
                        ImageIcon icon = ImageUtil.getPieceIcon(p.isWhite(), p.getSymbol(), TILE_SIZE);
                        g.drawImage(icon.getImage(), c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);
                    }
                }
            }

            if (selected != null) {
                g.setColor(Color.YELLOW);
                g.drawRect(selected.getColumn() * TILE_SIZE, selected.getRow() * TILE_SIZE, TILE_SIZE - 1, TILE_SIZE - 1);
                g.drawRect(selected.getColumn() * TILE_SIZE + 1, selected.getRow() * TILE_SIZE + 1, TILE_SIZE - 3, TILE_SIZE - 3);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChessGUI());
    }
}
