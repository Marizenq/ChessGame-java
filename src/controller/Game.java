package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JOptionPane;
import model.board.Board;
import model.board.Move;
import model.board.Position;
import model.pieces.*;

public class Game {

    private Board board;
    private boolean whiteToMove = true;
    private boolean gameOver = false;

    private Position enPassantTarget = null;
    private final List<String> history = new ArrayList<>();

    // Control players
    private boolean humanIsWhite = true;
    private boolean vsAI = true;

    public Game() {
        this.board = new Board();
        setupPieces();
    }

    // Private ctor for snapshots if needed
    private Game(boolean empty) { /* intentionally empty */ }

    // Public getters
    public Board getBoard() { return board; }
    public boolean isWhiteToMove() { return whiteToMove; }
    public boolean isGameOver() { return gameOver; }
    public List<String> history() { return Collections.unmodifiableList(history); }

    public void setHumanColor(boolean isWhite) {
        this.humanIsWhite = isWhite;
        this.whiteToMove = true; // whites always start
    }

    public void setVsAI(boolean vsAI) { this.vsAI = vsAI; }

    public void newGame() {
        this.board = new Board();
        this.whiteToMove = true;
        this.gameOver = false;
        this.enPassantTarget = null;
        this.history.clear();
        setupPieces();
    }

    // --- Provide all possible moves for the current side (used by AI)
    public List<Move> getAllPossibleMoves() {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position from = new Position(r, c);
                Piece p = board.get(from);
                if (p != null && p.isWhite() == whiteToMove) {
                    List<Position> poss = legalMovesFromWithSpecials(from);
                    for (Position to : poss) {
                        Piece captured = board.get(to);
                        moves.add(Move.normal(from, to, p, captured));
                    }
                }
            }
        }
        return moves;
    }

    // Simple AI: prefer captures, otherwise random
    public Move getAIMove() {
        List<Move> all = getAllPossibleMoves();
        if (all.isEmpty()) return null;

        // captures first
        Move bestCap = null;
        int bestVal = -1;
        for (Move m : all) {
            if (m.getCaptured() != null) {
                int v = pieceValue(m.getCaptured());
                if (v > bestVal) { bestVal = v; bestCap = m; }
            }
        }
        if (bestCap != null) return bestCap;

        return all.get((int)(Math.random()*all.size()));
    }

    private int pieceValue(Piece p) {
        if (p == null) return 0;
        switch (p.getSymbol()) {
            case "P": return 1;
            case "N": case "B": return 3;
            case "R": return 5;
            case "Q": return 9;
            case "K": return 1000;
            default: return 0;
        }
    }

    public boolean makeMove(Move move) {
        if (move == null || gameOver) return false;
        Position from = move.getFrom();
        Position to = move.getTo();
        Piece p = board.get(from);
        if (p == null || p.isWhite() != whiteToMove) return false;

        List<Position> legal = legalMovesFromWithSpecials(from);
        if (!legal.contains(to)) return false;

        boolean isPawn = p instanceof Pawn;
        boolean isKing = p instanceof King;
        int dCol = Math.abs(to.getColumn() - from.getColumn());
        Piece capturedBefore = board.get(to);

        // Castling
        if (isKing && dCol == 2) {
            int row = from.getRow();
            board.set(to, p); board.set(from, null); p.setMoved(true);
            if (to.getColumn() == 6) {
                Piece rook = board.get(new Position(row, 7));
                board.set(new Position(row, 5), rook);
                board.set(new Position(row, 7), null);
                if (rook != null) rook.setMoved(true);
            } else {
                Piece rook = board.get(new Position(row, 0));
                board.set(new Position(row, 3), rook);
                board.set(new Position(row, 0), null);
                if (rook != null) rook.setMoved(true);
            }
            enPassantTarget = null;
            whiteToMove = !whiteToMove;
            if (isCheckmate(whiteToMove)) { gameOver=true; }
            return true;
        }

        // En passant
        boolean diagonal = from.getColumn() != to.getColumn();
        boolean toIsEmpty = board.get(to) == null;
        boolean isEnPassant = isPawn && diagonal && toIsEmpty && to.equals(enPassantTarget);

        if (isEnPassant) {
            board.set(to, p);
            board.set(from, null);
            int dir = p.isWhite() ? 1 : -1;
            Position victim = new Position(to.getRow() + dir, to.getColumn());
            board.set(victim, null);
            p.setMoved(true);
            enPassantTarget = null;
            whiteToMove = !whiteToMove;
            if (isCheckmate(whiteToMove)) { gameOver = true; }
            return true;
        }

        // Promotion
        if (isPawn && (p.isWhite() ? to.getRow() == 0 : to.getRow() == 7)) {
            String[] options = {"Rainha", "Torre", "Bispo", "Cavalo"};
            String choice = (String) JOptionPane.showInputDialog(
                    null,
                    "Escolha a peça para promoção:",
                    "Promoção de Peão",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            Piece np;
            switch (choice) {
                case "Torre": np = new Rook(board, p.isWhite()); break;
                case "Bispo": np = new Bishop(board, p.isWhite()); break;
                case "Cavalo": np = new Knight(board, p.isWhite()); break;
                default: np = new Queen(board, p.isWhite()); break;
            }
            np.setMoved(true);
            board.set(from, null);
            board.set(to, np);

            if (capturedBefore instanceof King) { gameOver = true; }
            whiteToMove = !whiteToMove;
            enPassantTarget = null;
            if (isCheckmate(whiteToMove)) { gameOver = true; }
            return true;
        }

        // Normal move
        board.set(to, p);
        board.set(from, null);
        p.setMoved(true);

        if (isPawn && Math.abs(to.getRow() - from.getRow()) == 2) {
            int mid = (to.getRow() + from.getRow())/2;
            enPassantTarget = new Position(mid, from.getColumn());
        } else { enPassantTarget = null; }

        // Switch side and check game state
        whiteToMove = !whiteToMove;
        if (isCheckmate(whiteToMove)) {
            gameOver = true;
        } else if (isStalemate(whiteToMove)) {
            gameOver = true;
            JOptionPane.showMessageDialog(null, "Empate por xeque-pato!");
        }

        return true;
    }

    public boolean inCheck(boolean whiteSide) {
        Position k = findKing(whiteSide);
        if (k == null) return true;
        return isSquareAttacked(k, whiteSide);
    }

    public boolean isCheckmate(boolean whiteSide) {
        if (!inCheck(whiteSide)) return false;
        for (int r=0;r<8;r++) for (int c=0;c<8;c++) {
            Position from = new Position(r,c);
            Piece piece = board.get(from);
            if (piece != null && piece.isWhite() == whiteSide) {
                for (Position to : legalMovesFromWithSpecials(from)) {
                    Game g = snapshotShallow();
                    g.forceMoveNoChecks(from, to);
                    if (!g.inCheck(whiteSide)) return false;
                }
            }
        }
        return true;
    }

    // Detecta xeque-pato (stalemate)
    public boolean isStalemate(boolean whiteSide) {
        if (inCheck(whiteSide)) return false; // não é xeque-pato se estiver em xeque
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position from = new Position(r, c);
                Piece piece = board.get(from);
                if (piece != null && piece.isWhite() == whiteSide) {
                    List<Position> moves = legalMovesFromWithSpecials(from);
                    if (!moves.isEmpty()) return false;
                }
            }
        }
        return true;
    }

    public List<Position> legalMovesFromWithSpecialsForGui(Position from) {
        return new ArrayList<>(legalMovesFromWithSpecials(from));
    }

    private List<Position> legalMovesFromWithSpecials(Position from) {
        Piece p = board.get(from);
        if (p == null || p.isWhite() != whiteToMove) return List.of();

        List<Position> moves = new ArrayList<>(p.getPossibleMoves());

        // En Passant
        if (p instanceof Pawn && enPassantTarget != null) {
            int dir = p.isWhite() ? -1 : 1;
            if (from.getRow() + dir == enPassantTarget.getRow()
                    && Math.abs(from.getColumn() - enPassantTarget.getColumn()) == 1) {
                Piece victim = board.get(new Position(enPassantTarget.getRow() - dir, enPassantTarget.getColumn()));
                if (victim instanceof Pawn && victim.isWhite() != p.isWhite()) moves.add(enPassantTarget);
            }
        }

        // Castling
        if (p instanceof King && !p.hasMoved() && !inCheck(p.isWhite())) {
            int row = from.getRow();
            if (canCastle(row, 4, 7, 5, 6, p.isWhite())) moves.add(new Position(row,6));
            if (canCastle(row, 4, 0, 3, 2, p.isWhite())) moves.add(new Position(row,2));
        }

        moves.removeIf(to -> {
            Piece tgt = board.get(to);
            return (tgt instanceof King) && (tgt.isWhite() != p.isWhite());
        });

        moves.removeIf(to -> leavesKingInCheck(from, to));
        return moves;
    }

    private boolean canCastle(int row, int kingCol, int rookCol, int passCol1, int passCol2, boolean whiteSide) {
        Piece rook = board.get(new Position(row, rookCol));
        if (!(rook instanceof Rook) || rook.hasMoved()) return false;
        int step = (rookCol > kingCol) ? 1 : -1;
        for (int c = kingCol + step; c != rookCol; c += step) if (board.get(new Position(row,c)) != null) return false;
        Position p1 = new Position(row, passCol1);
        Position p2 = new Position(row, passCol2);
        if (isSquareAttacked(p1, whiteSide) || isSquareAttacked(p2, whiteSide)) return false;
        return true;
    }

    private boolean leavesKingInCheck(Position from, Position to) {
        Piece mover = board.get(from);
        if (mover == null) return true;
        Game g = snapshotShallow();
        g.forceMoveNoChecks(from, to);
        return g.inCheck(mover.isWhite());
    }

    private boolean isSquareAttacked(Position sq, boolean sideToProtect) {
        int r = sq.getRow(), c = sq.getColumn();

        int dir = sideToProtect ? -1 : 1;
        int rp = r - dir;
        if (rp >= 0 && rp < 8) {
            if (c-1 >=0) {
                Piece p = board.get(new Position(rp,c-1));
                if (p instanceof Pawn && p.isWhite() != sideToProtect) return true;
            }
            if (c+1 < 8) {
                Piece p = board.get(new Position(rp,c+1));
                if (p instanceof Pawn && p.isWhite() != sideToProtect) return true;
            }
        }

        int[][] KJUMPS = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] d : KJUMPS) {
            int rr = r + d[0], cc = c + d[1];
            if (rr>=0 && rr<8 && cc>=0 && cc<8) {
                Piece p = board.get(new Position(rr,cc));
                if (p instanceof Knight && p.isWhite() != sideToProtect) return true;
            }
        }

        for (int dr=-1; dr<=1; dr++) for (int dc=-1; dc<=1; dc++) {
            if (dr==0 && dc==0) continue;
            int rr = r+dr, cc = c+dc;
            if (rr>=0 && rr<8 && cc>=0 && cc<8) {
                Piece p = board.get(new Position(rr,cc));
                if (p instanceof King && p.isWhite() != sideToProtect) return true;
            }
        }

        int[][] ROOK_DIRS = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : ROOK_DIRS) {
            int rr=r+d[0], cc=c+d[1];
            while (rr>=0 && rr<8 && cc>=0 && cc<8) {
                Piece p = board.get(new Position(rr,cc));
                if (p != null) {
                    if (p.isWhite() != sideToProtect && (p instanceof Rook || p instanceof Queen)) return true;
                    break;
                }
                rr += d[0]; cc += d[1];
            }
        }

        int[][] BISHOP_DIRS = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : BISHOP_DIRS) {
            int rr=r+d[0], cc=c+d[1];
            while (rr>=0 && rr<8 && cc>=0 && cc<8) {
                Piece p = board.get(new Position(rr,cc));
                if (p != null) {
                    if (p.isWhite() != sideToProtect && (p instanceof Bishop || p instanceof Queen)) return true;
                    break;
                }
                rr += d[0]; cc += d[1];
            }
        }

        return false;
    }

    private void forceMoveNoChecks(Position from, Position to) {
        Piece p = board.get(from);
        if (p == null) return;

        int dCol = Math.abs(to.getColumn() - from.getColumn());
        boolean isPawn = p instanceof Pawn;
        boolean isKing = p instanceof King;

        boolean diagonal = from.getColumn() != to.getColumn();
        boolean toIsEmpty = board.get(to) == null;
        boolean ep = isPawn && diagonal && toIsEmpty && enPassantTarget != null && to.equals(enPassantTarget);
        boolean castle = isKing && dCol == 2;

        board.set(to, p);
        board.set(from, null);
        p.setMoved(true);

        if (ep) {
            int dir = p.isWhite() ? 1 : -1;
            Position victim = new Position(to.getRow() + dir, to.getColumn());
            board.set(victim, null);
        }

        if (castle) {
            int row = to.getRow();
            if (to.getColumn() == 6) {
                Piece rook = board.get(new Position(row,7));
                board.set(new Position(row,5), rook);
                board.set(new Position(row,7), null);
                if (rook != null) rook.setMoved(true);
            } else if (to.getColumn() == 2) {
                Piece rook = board.get(new Position(row,0));
                board.set(new Position(row,3), rook);
                board.set(new Position(row,0), null);
                if (rook != null) rook.setMoved(true);
            }
        }
    }

    private Position findKing(boolean whiteSide) {
        for (int r=0;r<8;r++) for (int c=0;c<8;c++) {
            Position pos = new Position(r,c);
            Piece p = board.get(pos);
            if (p instanceof King && p.isWhite() == whiteSide) return pos;
        }
        return null;
    }

    private Game snapshotShallow() {
        Game g = new Game(true);
        g.board = this.board.copy();
        g.whiteToMove = this.whiteToMove;
        g.gameOver = this.gameOver;
        g.enPassantTarget = (this.enPassantTarget == null) ? null : new Position(this.enPassantTarget.getRow(), this.enPassantTarget.getColumn());
        g.history.addAll(this.history);
        return g;
    }

    private void addHistory(String s) { history.add(s); }

    private String coord(Position p) {
        char file = (char)('a' + p.getColumn());
        int rank = 8 - p.getRow();
        return "" + file + rank;
    }

    private void setupPieces() {
        // White back rank (row 7)
        board.placePiece(new Rook(board, true), new Position(7, 0));
        board.placePiece(new Knight(board, true), new Position(7, 1));
        board.placePiece(new Bishop(board, true), new Position(7, 2));
        board.placePiece(new Queen(board, true), new Position(7, 3));
        board.placePiece(new King(board, true), new Position(7, 4));
        board.placePiece(new Bishop(board, true), new Position(7, 5));
        board.placePiece(new Knight(board, true), new Position(7, 6));
        board.placePiece(new Rook(board, true), new Position(7, 7));
        for (int c = 0; c < 8; c++) board.placePiece(new Pawn(board, true), new Position(6, c));

        // Black back rank (row 0)
        board.placePiece(new Rook(board, false), new Position(0, 0));
        board.placePiece(new Knight(board, false), new Position(0, 1));
        board.placePiece(new Bishop(board, false), new Position(0, 2));
        board.placePiece(new Queen(board, false), new Position(0, 3));
        board.placePiece(new King(board, false), new Position(0, 4));
        board.placePiece(new Bishop(board, false), new Position(0, 5));
        board.placePiece(new Knight(board, false), new Position(0, 6));
        board.placePiece(new Rook(board, false), new Position(0, 7));
        for (int c = 0; c < 8; c++) board.placePiece(new Pawn(board, false), new Position(1, c));
    }
}
