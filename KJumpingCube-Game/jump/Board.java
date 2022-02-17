package jump61;

import java.util.Arrays;
import java.util.Stack;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Formatter;
import java.util.function.Consumer;
import static jump61.Side.*;
import static jump61.Square.INITIAL;

/** Represents the state of a Jump61 game.  Squares are indexed either by
 *  row and column (between 1 and size()), or by square number, numbering
 *  squares by rows, with squares in row 1 numbered from 0 to size()-1, in
 *  row 2 numbered from size() to 2*size() - 1, etc. (i.e., row-major order).
 *
 *  A Board may be given a notifier---a Consumer<Board> whose
 *  .accept method is called whenever the Board's contents are changed.
 *
 *  @author Owen Doyle
 */
class Board {

    /** An uninitialized Board.  Only for use by subtypes. */
    protected Board() {
        _notifier = NOP;
    }

    /** An N x N board in initial configuration. */
    Board(int N) {
        this();
        clear(N);
    }

    /** A board whose initial contents are copied from BOARD0, but whose
     *  undo history is clear, and whose notifier does nothing. */
    Board(Board board0) {
        this(board0.size());
        copy(board0);
        _readonlyBoard = new ConstantBoard(this);
    }

    /** Set all values on the board to the INITIAL Square. */
    void initializeBoard() {
        for (int i = 0; i < size(); i++) {
            Arrays.fill(_squares[i], INITIAL);
        }
    }

    /** Returns a readonly version of this board. */
    Board readonlyBoard() {
        return _readonlyBoard;
    }

    /** (Re)initialize me to a cleared board with N squares on a side. Clears
     *  the undo-history and sets the number of moves to 0. */
    void clear(int N) {
        _size = N;
        setBoard(new Square[N][N]);
        _numRed = 0;
        _numBlue = 0;
        _numMoves = 0;
        _history = new Stack<Board>();
        initializeBoard();
        _cornerSquares = new int[numCorner()];
        _edgeSquares = new int[numEdge()];
        _middleSquares = new int[numMiddle()];
        classifySquares();
        _numRedCorner = 0;
        _numRedEdge  = 0;
        _numRedMiddle = 0;
        _numBlueCorner = 0;
        _numBlueEdge = 0;
        _numBlueMiddle = 0;
        announce();
    }

    /** Copy the contents of BOARD into me. */
    void copy(Board board) {
        clear(size());
        for (int i = 0; i < size(); i++) {
            for (int j = 0; j < size(); j++) {
                _squares[i][j] = board.get(i + 1, j + 1);
            }
        }

        setNumMoves(board.numMoves());
        setNumBlue(board.numBlue());
        setNumRed(board.numRed());
        _numRedCorner = board.numRedCorner();
        _numRedEdge  = board.numRedEdge();
        _numRedMiddle = board.numRedMiddle();
        _numBlueCorner = board.numBlueCorner();
        _numBlueEdge = board.numBlueEdge();
        _numBlueMiddle = board.numBlueMiddle();
    }

    /** Copy the contents of BOARD into me, without modifying my undo
     *  history. Assumes BOARD and I have the same size. */
    private void internalCopy(Board board) {
        assert size() == board.size();
        for (int i = 0; i < size(); i++) {
            for (int j = 0; j < size(); j++) {
                _squares[i][j] = board.get(i + 1, j + 1);
            }
        }
        setNumMoves(board.numMoves());
        setNumBlue(board.numBlue());
        setNumRed(board.numRed());
        _numRedCorner = board.numRedCorner();
        _numRedEdge  = board.numRedEdge();
        _numRedMiddle = board.numRedMiddle();
        _numBlueCorner = board.numBlueCorner();
        _numBlueEdge = board.numBlueEdge();
        _numBlueMiddle = board.numBlueMiddle();
    }

    /** Return the number of rows and of columns of THIS. */
    int size() {
        return _size;
    }

    /** Return the number of corner squares on the board. */
    public int numCorner() {
        if (size() == 1) {
            return 1;
        }
        return 4;
    }

    /** Return the number of non-corner squares on the edge of the board. */
    public int numEdge() {
        return (size() - 2) * 4;
    }

    /** Return the number of non-edge, non-corner squares in the board. */
    public int numMiddle() {
        return size() * size() - numEdge() - numCorner();
    }

    /** Instantiate and fill the cornerSquare, edgeSquare,
     *  and middleSquare arrays. */
    public void classifySquares() {
        int[] indices = new int[] {0, 0, 0};
        int numNeighbors;
        for (int i = 0; i < size() * size(); i++) {
            numNeighbors = neighbors(i);
            if (numNeighbors == 2) {
                _cornerSquares[indices[0]] = i;
                indices[0] += 1;
            } else if (numNeighbors == 3) {
                _edgeSquares[indices[1]] = i;
                indices[1] += 1;
            } else {
                _middleSquares[indices[2]] = i;
                indices[2] += 1;
            }
        }
    }

    /** The list of corner square indices for this board. */
    private int[] _cornerSquares;

    /** Set the list of corner squares to LIST. */
    void setCornerSquares(int[] list) {
        _cornerSquares = list;
    }

    /** Return the number of corner squares. */
    int[] cornerSquares() {
        return _cornerSquares;
    }

    /** The list of edge square indices for this board. */
    private int[] _edgeSquares;

    /** Set the list of edge squares to LIST. */
    void setEdgeSquares(int[] list) {
        _edgeSquares = list;
    }

    /** Return the number of edge squares. */
    int[] edgeSquares() {
        return _edgeSquares;
    }

    /** The list of middle square indices for this board. */
    private int[] _middleSquares;

    /** Set the list of middle squares to LIST. */
    void setMiddleSquares(int[] list) {
        _middleSquares = list;
    }

    /** Return the number of middle squares. */
    int[] middleSquares() {
        return _middleSquares;
    }

    /** Returns the contents of the square at row R, column C
     *  1 <= R, C <= size (). */
    Square get(int r, int c) {
        return get(sqNum(r, c));
    }

    /** Returns the contents of square #N, numbering squares by rows, with
     *  squares in row 1 number 0 - size()-1, in row 2 numbered
     *  size() - 2*size() - 1, etc. */
    Square get(int n) {
        return _squares[row(n) - 1][col(n) - 1];
    }

    /** Returns the total number of spots on the board. */
    int numPieces() {
        return size() * size() + numMoves();
    }

    /** Returns the Side of the player who would be next to move.  If the
     *  game is won, this will return the loser (assuming legal position). */
    Side whoseMove() {
        return ((numPieces() + size()) & 1) == 0 ? RED : BLUE;
    }

    /** Return true iff row R and column C denotes a valid square. */
    final boolean exists(int r, int c) {
        return 1 <= r && r <= size() && 1 <= c && c <= size();
    }

    /** Return true iff S is a valid square number. */
    final boolean exists(int s) {
        int N = size();
        return 0 <= s && s < N * N;
    }

    /** Return the row number for square #N. */
    final int row(int n) {
        return n / size() + 1;
    }

    /** Return the column number for square #N. */
    final int col(int n) {
        return n % size() + 1;
    }

    /** Return the square number of row R, column C. */
    final int sqNum(int r, int c) {
        return (c - 1) + (r - 1) * size();
    }

    /** Return a string denoting move (ROW, COL)N. */
    String moveString(int row, int col) {
        return String.format("%d %d", row, col);
    }

    /** Return a string denoting move N. */
    String moveString(int n) {
        return String.format("%d %d", row(n), col(n));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
        to square at row R, column C. */
    boolean isLegal(Side player, int r, int c) {
        return isLegal(player, sqNum(r, c));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
     *  to square #N. */
    boolean isLegal(Side player, int n) {
        return get(n).getSide().equals(player)
                || get(n).getSide().equals(WHITE);
    }

    /** Returns true iff PLAYER is allowed to move at this point. */
    boolean isLegal(Side player) {
        return whoseMove().equals(player);
    }


    /** Returns the winner of the current position, if the game is over,
     *  and otherwise null. */
    final Side getWinner() {
        if (numOfSide(RED) == size() * size()) {
            return RED;
        } else if (numOfSide(BLUE) == size() * size()) {
            return BLUE;
        }
        return null;
    }

    /** Return the number of squares of given SIDE. */
    int numOfSide(Side side) {
        if (side.equals(RED)) {
            return _numRed;
        } else if (side.equals(BLUE)) {
            return _numBlue;
        } else {
            return size() * size() - _numRed - _numBlue;
        }
    }

    /** Add a spot from PLAYER at row R, column C.  Assumes
     *  isLegal(PLAYER, R, C). */
    void addSpot(Side player, int r, int c) {
        markUndo();
        simpleAdd(player, r, c, 1);
        if (overfull(r, c)) {
            jump(sqNum(r, c));
        }
    }

    /** Add a spot from PLAYER at square #N.  Assumes isLegal(PLAYER, N). */
    void addSpot(Side player, int n) {
        addSpot(player, row(n), col(n));
    }

    /** Return True iff square R : C is over-full. */
    private boolean overfull(int r, int c) {
        int numNeighbors = neighbors(r, c);
        int numSpots = get(r, c).getSpots();
        return numSpots > numNeighbors;
    }

    /** Return True iff square #N is over-full. */
    private boolean overfull(int n) {
        return overfull(row(n), col(n));
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white). */
    void set(int r, int c, int num, Side player) {
        internalSet(r, c, num, player);
        announce();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white).  Does not announce
     *  changes. */
    private void internalSet(int r, int c, int num, Side player) {
        internalSet(sqNum(r, c), num, player);
    }

    /** Set the square #N to NUM spots (0 <= NUM), and give it color PLAYER
     *  if NUM > 0 (otherwise, white). Does not announce changes. */
    private void internalSet(int n, int num, Side player) {
        updateSquareCounts(n, player);
        setPlayerSquares(player, n);
        setNumMoves(numMoves() - get(n).getSpots() + num);
        _squares[row(n) - 1][col(n) - 1] = Square.square(player, num);
    }

    /** Update _numRed and _numBlue accordingly when square #N
     * is set to color PLAYER. Do NOT update the square itself,
     * just change the counts to account for the update. */
    private void updateSquareCounts(int n, Side player) {
        Square s = _squares[row(n) - 1][col(n) - 1];
        if  (!s.getSide().equals(player)) {
            incrementSquareCount(player, 1);
        }
        if (s.getSide().equals(player.opposite())) {
            incrementSquareCount(player.opposite(), -1);
        }
    }

    /** Increment COLOR's number of squares by VAL.
     *  If COLOR is white, nothing happens. */
    private void incrementSquareCount(Side color, int val) {
        if (color.equals(RED)) {
            _numRed = _numRed + val;
        } else if (color.equals(BLUE)) {
            _numBlue = _numBlue + val;
        }
    }

    /** Set PLAYER's square counter given the change at N. */
    void setPlayerSquares(Side player, int n) {
        Square s = _squares[row(n) - 1][col(n) - 1];
        if  (!s.getSide().equals(player)) {
            incrementPlayerSquares(player, n, 1);
        }
        if (s.getSide().equals(player.opposite())) {
            incrementPlayerSquares(player.opposite(), n, -1);
        }
    }

    /** Increment the appropriate square-counter for
     * PLAYER by VAL given the square #N. */
    void incrementPlayerSquares(Side player, int n, int val) {
        if (player.equals(RED)) {
            if (neighbors(n) == 2) {
                _numRedCorner += val;
            } else if (neighbors(n) == 3) {
                _numRedEdge += val;
            } else {
                _numRedMiddle += val;
            }
        } else if (player.equals(BLUE)) {
            if (neighbors(n) == 2) {
                _numBlueCorner += val;
            } else if (neighbors(n) == 3) {
                _numBlueEdge += val;
            } else {
                _numBlueMiddle += val;
            }
        }
    }

    /** Undo the effects of one move (that is, one addSpot command).  One
     *  can only undo back to the last point at which the undo history
     *  was cleared, or the construction of this Board. */
    void undo() {
        Board lastBoard = _history.pop();
        internalCopy(lastBoard);
    }

    /** Record the beginning of a move in the undo-history. */
    private void markUndo() {
        Board copiedBoard = new Board(this);
        _history.push(copiedBoard);
    }

    /** Add DELTASPOTS spots of side PLAYER to row R, column C,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int r, int c, int deltaSpots) {
        internalSet(r, c, deltaSpots + get(r, c).getSpots(), player);
    }

    /** Add DELTASPOTS spots of color PLAYER to square #N,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int n, int deltaSpots) {
        internalSet(n, deltaSpots + get(n).getSpots(), player);
    }

    /** Used in jump to keep track of squares needing processing.  Allocated
     *  here to cut down on allocations. */
    private final ArrayDeque<Integer> _workQueue = new ArrayDeque<>();

    /** Do all jumping on this board, assuming that initially, S is the only
     *  square that might be over-full. */
    private void jump(int S) {
        _workQueue.clear();
        _workQueue.add(S);
        while (!_workQueue.isEmpty() && getWinner() == null) {
            int n = _workQueue.removeLast();
            Side player = get(n).getSide();
            internalSet(n, 1, player);

            int r = row(n);
            int c = col(n);
            ArrayList<Integer> neighbors = getNeighbors(r, c);
            for (int i = 0; i < neighbors.size(); i++) {
                int neighbor = neighbors.get(i);
                simpleAdd(player, neighbor, 1);
                if (overfull(neighbor)) {
                    _workQueue.add(neighbor);
                }
            }
        }
    }

    /** Returns the valid neighbors of the Square at R : C. */
    ArrayList<Integer> getNeighbors(int r, int c) {
        ArrayList<Integer> neighbors = new ArrayList<>();
        if (exists(r, c - 1)) {
            neighbors.add(sqNum(r, c - 1));
        }
        if (exists(r - 1, c)) {
            neighbors.add(sqNum(r - 1, c));
        }
        if (exists(r, c + 1)) {
            neighbors.add(sqNum(r, c + 1));
        }
        if (exists(r + 1, c)) {
            neighbors.add(sqNum(r + 1, c));
        }
        return neighbors;
    }

    /** Returns my dumped representation. */
    @Override
    public String toString() {
        Formatter out = new Formatter();
        String white = "-";
        String red = "r";
        String blue = "b";
        String color;
        int spots;
        out.format("===\n");
        for (int r = 1; r <= size(); r++) {
            out.format("   ");
            for (int c = 1; c <= size(); c++) {
                if (get(r, c).getSide().equals(RED)) {
                    color = red;
                } else if (get(r, c).getSide().equals(BLUE)) {
                    color = blue;
                } else {
                    color = white;
                }
                spots = get(r, c).getSpots();
                out.format(" %1$s%2$s", spots, color);
            }
            out.format("\n");
        }
        out.format("===");
        return out.toString();
    }

    /** Returns an external rendition of me, suitable for human-readable
     *  textual display, with row and column numbers.  This is distinct
     *  from the dumped representation (returned by toString). */
    public String toDisplayString() {
        String[] lines = toString().trim().split("\\R");
        Formatter out = new Formatter();
        for (int i = 1; i + 1 < lines.length; i += 1) {
            out.format("%2d %s%n", i, lines[i].trim());
        }
        out.format("  ");
        for (int i = 1; i <= size(); i += 1) {
            out.format("%3d", i);
        }
        return out.toString();
    }

    /** Returns the number of neighbors of the square at row R, column C. */
    int neighbors(int r, int c) {
        int size = size();
        int n;
        n = 0;
        if (r > 1) {
            n += 1;
        }
        if (c > 1) {
            n += 1;
        }
        if (r < size) {
            n += 1;
        }
        if (c < size) {
            n += 1;
        }
        return n;
    }

    /** Returns the number of neighbors of square #N. */
    int neighbors(int n) {
        return neighbors(row(n), col(n));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        } else {
            Board B = (Board) obj;
            return this == obj;
        }
    }

    @Override
    public int hashCode() {
        return numPieces();
    }

    /** Set my notifier to NOTIFY. */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /** Take any action that has been set for a change in my state. */
    private void announce() {
        _notifier.accept(this);
    }

    /** A notifier that does nothing. */
    private static final Consumer<Board> NOP = (s) -> { };

    /** A read-only version of this Board. */
    private ConstantBoard _readonlyBoard;

    /** Use _notifier.accept(B) to announce changes to this board. */
    private Consumer<Board> _notifier;

    /** Stores the squares of the board. */
    private Square[][] _squares;

    /** Return the board's state, represented in _squares. */
    Square[][] getBoard() {
        return _squares;
    }

    /** Set the board's state to BOARD. */
    void setBoard(Square[][] board) {
        _squares = board;
    }

    /** The number of columns/rows of the board. */
    private int _size;

    /** The number of red squares on the board. */
    private int _numRed;

    /** Return the number of red squares on the board. */
    int numRed() {
        return _numRed;
    }

    /** Set the number of red squares on the board to NUM. */
    void setNumRed(int num) {
        _numRed = num;
    }

    /** The number of blue squares on the board. */
    private int _numBlue;

    /** Return the number of blue squares on the board. */
    int numBlue() {
        return _numBlue;
    }

    /** Set the number of blue squares on the board to NUM. */
    void setNumBlue(int num) {
        _numBlue = num;
    }

    /** The number of moves of both players leading up to this board. */
    private int _numMoves;

    /** Sets the _NUMMOVES variable to VAL. */
    void setNumMoves(int val) {
        _numMoves = val;
    }

    /** Returns the _NUMMOVES variable. */
    int numMoves() {
        return _numMoves;
    }

    /** The number of red corners on the current board state. */
    private int _numRedCorner;

    /** Return the number of red corners on the current board state. */
    int numRedCorner() {
        return _numRedCorner;
    }

    /** The number of red edges on the current board state. */
    private int _numRedEdge;

    /** Return the number of red edges on the current board state. */
    int numRedEdge() {
        return _numRedEdge;
    }

    /** The number of red middle squares on the current board state. */
    private int _numRedMiddle;

    /** Return the number of red middle squares on the current board state. */
    int numRedMiddle() {
        return _numRedMiddle;
    }

    /** The number of blue corners on the current board state. */
    private int _numBlueCorner;

    /** Return the number of blue corners on the current board state. */
    int numBlueCorner() {
        return _numBlueCorner;
    }

    /** The number of blue edges on the current board state. */
    private int _numBlueEdge;

    /** Return the number of blue edges on the current board state. */
    int numBlueEdge() {
        return _numBlueEdge;
    }

    /** The number of blue middle squares on the current board state. */
    private int _numBlueMiddle;

    /** Return the number of blue middle squares on the current board state. */
    int numBlueMiddle() {
        return _numBlueMiddle;
    }

    /** The history of this board. */
    private Stack<Board> _history;
}
