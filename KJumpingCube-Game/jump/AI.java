package jump61;

import java.util.Random;

import static jump61.Side.*;

/** An automated Player.
 *  @authors P. N. Hilfinger, Owen Doyle
 */
class AI extends Player {

    /** A new player of GAME initially COLOR that chooses moves automatically.
     *  SEED provides a random-number seed used for choosing moves.
     */
    AI(Game game, Side color, long seed) {
        super(game, color);
        _random = new Random(seed);
    }

    @Override
    String getMove() {
        Board board = getGame().getBoard();

        assert getSide() == board.whoseMove();
        int choice = searchForMove();
        getGame().reportMove(board.row(choice), board.col(choice));
        return String.format("%d %d", board.row(choice), board.col(choice));
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over. */
    private int searchForMove() {
        Board work = new Board(getBoard());
        assert getSide() == work.whoseMove();
        _foundMove = -1;
        setWinningValue(work);
        if (getSide() == RED) {
            minMax(work, 5, true, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        } else {
            minMax(work, 5, true, -1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        return _foundMove;
    }

    /** Return an array of valid moves on BOARD for COLOR. */
    int[] validMoves(Board board, Side color) {
        Side opponent = color.opposite();
        int numSquares = board.size() * board.size();
        int[] moves;

        if (opponent.equals(RED)) {
            moves = new int[numSquares - board.numRed()];
        } else if (opponent.equals(BLUE)) {
            moves = new int[numSquares - board.numBlue()];
        } else {
            throw new IllegalAccessError();
        }
        int moveIndex = 0;
        for (int i = 0; i < numSquares; i++) {
            if (board.isLegal(color, i)) {
                moves[moveIndex] = i;
                moveIndex += 1;
            }
        }
        return moves;
    }

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int minMax(Board board, int depth, boolean saveMove,
                       int sense, int alpha, int beta) {

        if (board.getWinner() != null || depth == 0) {
            return staticEval(board);
        }
        int bestVal;
        if (sense == 1) {
            bestVal = Integer.MIN_VALUE;
        } else {
            bestVal = Integer.MAX_VALUE;
        }
        int value;
        int[] moves = validMoves(board, board.whoseMove());
        for (int move : moves) {
            board.addSpot(board.whoseMove(), move);
            value = minMax(board, depth - 1, false, -sense, alpha, beta);
            board.undo();
            if ((sense == 1 && value > bestVal)
                    || (sense == -1 && value < bestVal)) {
                bestVal = value;
                if (saveMove) {
                    _foundMove = move;
                }
                if (sense == 1) {
                    alpha = Math.max(alpha, bestVal);
                } else {
                    beta = Math.min(beta, bestVal);
                }

                if (alpha >= beta) {
                    break;
                }
            }
        }
        return bestVal;
    }


    /** Return a heuristic estimate of the value of board position B.
     *  Use WINNINGVALUE to indicate a win for Red and -WINNINGVALUE to
     *  indicate a win for Blue. */
    private int staticEval(Board b) {
        int redVal = b.numRed();
        int blueVal = b.numBlue();
        int val = redVal - blueVal;
        return val;
    }

    /** A random-number generator used for move selection. */
    private Random _random;

    /** Used to convey moves discovered by minMax. */
    private int _foundMove;

    /** The winning value of the given board. */
    private int winningValue;

    /** Return the winning value. */
    int winningValue() {
        return winningValue;
    }

    /** Set the winning value based on Board B. */
    void setWinningValue(Board b) {
        winningValue = b.size() * b.size();
    }
}
