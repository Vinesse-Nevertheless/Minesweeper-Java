package minesweeper;

import java.util.*;

enum Cell {
    X("X") {
        @Override
        String getCellState() {
            return "X";
        }
    },
    O(".") {
        @Override
        String getCellState() {
            return ".";
        }
    },
    SLASH("/") {
        @Override
        String getCellState() {
            return "/";
        }
    },
    STAR("*") {
        @Override
        String getCellState() {
            return "*";
        }
    };

    public final String cellState;

    Cell(String cellState) {
        this.cellState = cellState;
    }

    String getCellState() {
        return cellState;
    }

}

public class Main {

    public static void main(String[] args) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.dispatch();
    }
}

class Dispatcher {

    static boolean hasUsedFirstFree = false;
    Set<List<Integer>> markedBeforeFree = new HashSet<>();

    public void dispatch() {
        Board b = new Board(9, 9, new UserInput().getRequestedMinesNum());

        while (!Validation.isValidMineRequest(b.mineNum, b.getRow(), b.getCol())) {
            b = new Board(9, 9, new UserInput().getRequestedMinesNum());
        }

        b.makeExternalBoard();
        b.printBoard();
        UserInput ui = new UserInput(b);
        Game game = new Game(ui, b);

        while (!hasUsedFirstFree) {
            String[] coordinateCommand = ui.getPlayerMove();

            if (coordinateCommand.length == 3) {
                int x = Integer.parseInt(coordinateCommand[0]);
                int y = Integer.parseInt(coordinateCommand[1]);
                String command = coordinateCommand[2];

                if (command.equals("free")) {
                    hasUsedFirstFree = true;
                    b.makeInternalBoard(new int[]{x, y});
                    game.play(coordinateCommand, markedBeforeFree);
                    return;
                } else if (command.equals("mine")) {
                    String curr = b.getCurrBoard()[x][y];
                    if (curr.equals(Cell.STAR.getCellState())) {
                        b.changeBoard(x, y, Cell.O.getCellState());
                        markedBeforeFree.remove(List.of(x, y));
                    } else if (curr.equals(Cell.O.getCellState())) {
                        b.changeBoard(x, y, Cell.STAR.getCellState());
                        markedBeforeFree.add(List.of(x, y));
                    }
                    b.printBoard();
                }
            }
        }
    }
}

class UserInput {
    Scanner in = new Scanner(System.in);
    Board board;

    public UserInput() {
    }

    public UserInput(Board board) {
        this.board = board;
    }

    int getRequestedMinesNum() {
        System.out.print("How many mines do you want on the field? ");
        String mineNum = in.nextLine();

        if (!Validation.isValidNum(mineNum)) {
            return -1;
        }
        return Integer.parseInt(mineNum);
    }

    String[] getPlayerMove() {

        System.out.println("Set/unset mines marks or claim a cell as free: ");
        String coordinatesCommand = in.nextLine();

        String[] coordCommand = Validation.validateCoordinateCommand(coordinatesCommand, board);
        String[] cartesian;

        if (coordCommand.length == 3) {
            cartesian = makeCartesianHelper(coordCommand);
        } else {
            return new String[]{};
        }

        return cartesian;
    }

    String[] makeCartesianHelper(String[] coordinatesCommand) {
        int x = Integer.parseInt(coordinatesCommand[1]) - 1;
        int y = Integer.parseInt(coordinatesCommand[0]) - 1;

        return new String[]{String.valueOf(x), String.valueOf(y), coordinatesCommand[2]};
    }

    void closeScanner() {
        in.close();
    }
}

class Game {
    UserInput ui;
    Board b;
    Set<List<Integer>> markedMines;
    Set<List<Integer>> wrongGuesses;
    Set<List<Integer>> revealedEmpties;
    boolean[][] seen;

    int[][] directions = {
            {0, 1},
            {0, -1},
            {-1, -1},
            {-1, 0},
            {-1, 1,},
            {1, -1},
            {1, 0},
            {1, 1}
    };

    public Game(UserInput ui, Board b) {
        this.ui = ui;
        this.b = b;
        seen = new boolean[b.getRow()][b.getCol()];
    }

    void includeMarkedBeforeFreeCells(Set<List<Integer>> markedBeforeFree) {

        for (List<Integer> marked : markedBeforeFree) {
            //these are already Cartesian coordinates
            int x = marked.get(0);
            int y = marked.get(1);

            markCoordinate(x, y);
        }

    }

    void play(String[] freCoord, Set<List<Integer>> markedBeforeFree) {

        markedMines = new HashSet<>();
        wrongGuesses = new HashSet<>();
        revealedEmpties = new HashSet<>();

        includeMarkedBeforeFreeCells(markedBeforeFree);

        String[] move = freCoord;
        while (true) {

            /*
            You can win by either marking all the bombs correctly, so that the
            unfound bombs list is empty.

            Or you can win by revealing all the safe cells so tha only unexplored
            mines are left.
             */
            if (move.length == 3 && canMakeMove(move)) {
                b.printBoard();
                if ((markedMines.size() == b.getMineSet().size() && wrongGuesses.isEmpty())
                        || revealedEmpties.size() == b.emptySet.size()) {
                    announceWinner();
                    return;
                }
            } else {
                return;
            }


            move = ui.getPlayerMove();
        }
    }

    void announceWinner() {
        System.out.println("Congratulations! You found all the mines!");
        ui.closeScanner();
    }

    boolean canMakeMove(String[] move) {
        //already Cartesian with 0 bases offset
        int x = Integer.parseInt(move[0]);
        int y = Integer.parseInt(move[1]);
        String command = move[2];

        String saved = b.getSavedBoard()[x][y];
        String curr = b.getCurrBoard()[x][y];
        switch (command) {
            case "free" -> {
                //if bomb, explode all bombs and give game over message
                if (saved.equals(Cell.X.getCellState())) {
                    revealAllBombs();
                    announceLoser();
                    return false;
                }
                //if is free square with dot, reveal all surrounding free cells
                else if (saved.equals(Cell.O.getCellState())) {
                    revealSurroundingFreeCells(x, y);
                } else {
                    //reveal single free cell with number
                    revealSingleNumCell(x, y);
                }
            }

            case "mine" -> {
                //if current board cell is marked, unmark it
                //if they have unmarked a mine, remove it from the list
                if (curr.equals(Cell.STAR.getCellState())) {
                    unmarkCoordinate(x, y);
                    // if current board cell is dot, mark it with *
                    //and check to see if there's a mine in that location on the saved board
                } else if (curr.equals(Cell.O.getCellState())) {
                    markCoordinate(x, y);
                }
                //if current board cell is already revealed, do nothing
            }

            default -> System.out.println("No such command.");
        }


        return true;
    }

    void revealAllBombs() {
        for (List<Integer> coor : b.getMineSet()) {
            int r = coor.get(0);
            int c = coor.get(1);

            b.changeBoard(r, c, Cell.X.getCellState());
        }
    }

    void announceLoser() {
        b.printBoard();
        System.out.println("You stepped on a mine and failed!");
    }

    /*
    I don't need to managing adding non-bomb cells to its list because
    if a bomb is accidentally revealed the game is over.
     */
    void revealSingleNumCell(int x, int y) {
        revealedEmpties.add(List.of(x, y));
        b.changeBoard(x, y, b.getSavedBoard()[x][y]);
    }

    void revealSurroundingFreeCells(int r, int c) {
        Queue<List<Integer>> coordQ = new LinkedList<>();
        coordQ.add(List.of(r, c));

        b.changeBoard(r, c, Cell.SLASH.getCellState());
        seen[r][c] = true;
        revealedEmpties.add(List.of(r, c));

        while (!coordQ.isEmpty()) {
            List<Integer> coordinates = coordQ.poll();
            int x = coordinates.get(0);
            int y = coordinates.get(1);

            String[][] board = b.getSavedBoard();

            for (int[] dir : directions) {

                int nextRow = dir[0] + x;
                int nextCol = dir[1] + y;

                if (nextRow < 0 || nextRow >= b.getRow() || nextCol < 0 || nextCol >= b.getCol()) {
                    continue;
                }

                if (!seen[nextRow][nextCol] && !board[nextRow][nextCol].equals(Cell.X.getCellState())) {
                    //reveal empty dot cell with a slash
                    if (board[nextRow][nextCol].equals(Cell.O.getCellState())) {
                        b.changeBoard(nextRow, nextCol, Cell.SLASH.getCellState());
                        revealedEmpties.add(List.of(nextRow, nextCol));
                        seen[nextRow][nextCol] = true;
                        coordQ.add(List.of(nextRow, nextCol));
                        //reveal a empty numbered cell with its number
                    } else if (!board[nextRow][nextCol].equals(Cell.X.getCellState())) {
                        revealedEmpties.add(List.of(nextRow, nextCol));
                        b.changeBoard(nextRow, nextCol, board[nextRow][nextCol]);
                        seen[nextRow][nextCol] = true;
                        coordQ.add(List.of(nextRow, nextCol));
                    }
                }
            }
        }
    }

    void markCoordinate(int x, int y) {
        //marked a real bomb, so add to list
        if (b.getMineSet().contains(List.of(x, y))) {  // constant time
            markedMines.add(List.of(x, y));
            //marked incorrectly as a bomb so add to wrong guesses
        } else {
            wrongGuesses.add(List.of(x, y));
        }

        //change current board to reflect marked
        b.changeBoard(x, y, Cell.STAR.getCellState());
    }


    void unmarkCoordinate(int x, int y) {
        //removed a real bomb that is needed to win
        if (b.getMineSet().contains(List.of(x, y))) {  // constant time
            markedMines.remove(List.of(x, y));
            //removed an incorrect guess
        } else {
            wrongGuesses.remove(List.of(x, y));
        }

        //external board only shows dots unless cell is revealed
        b.changeBoard(x, y, Cell.O.getCellState());
    }
}

class Board {
    int row;
    int col;
    int mineNum;
    int[][] mineFreq;
    Set<List<Integer>> mineSet = new HashSet<>();
    Set<List<Integer>> emptySet = new HashSet<>();
    List<Integer> listOfMineCoord;

    String[][] currBoard;
    String[][] savedBoard;

    int[][] directions = {
            {0, 1},
            {0, -1},
            {-1, -1},
            {-1, 0},
            {-1, 1,},
            {1, -1},
            {1, 0},
            {1, 1}
    };

    public Board(int row, int col, int mineNum) {
        this.row = row;
        this.col = col;
        this.mineNum = mineNum;
    }

    public Set<List<Integer>> getMineSet() {
        return this.mineSet;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    String[][] getCurrBoard() {
        return this.currBoard;
    }

    String[][] getSavedBoard() {
        return this.savedBoard;
    }

    void changeBoard(int r, int c, String marking) {
        currBoard[r][c] = marking;
    }

    void makeInternalBoard(int[] freeCoor) {

        randomizeMinePlacement(freeCoor);

        mineFreq = new int[row][col];

        for (int r = 0; r < row; r++) {
            for (int c = 0; c < col; c++) {
                if (!mineSet.contains(List.of(r, c))) {
                    countSurroundingMines(r, c);
                }
            }
        }
        saveInternalBoard();
    }

    void randomizeMinePlacement(int[] freeCoor) {
        while (mineSet.size() < mineNum) {
            listOfMineCoord = new ArrayList<>();
            Random randomInt = new Random();
            int r = randomInt.nextInt(row);
            int c = randomInt.nextInt(col);

            int freeX = freeCoor[1];
            int freeY = freeCoor[0];

            if (freeX == c && freeY == r) {
                continue;
            }
            listOfMineCoord.add(r);
            listOfMineCoord.add(c);
            mineSet.add(listOfMineCoord);

        }
    }

    void countSurroundingMines(int r, int c) {

        for (int[] dir : directions) {

            int nextRow = dir[0] + r;
            int nextCol = dir[1] + c;

            if (nextRow < 0 || nextRow >= row || nextCol < 0 || nextCol >= col) {
                continue;
            }

            if (mineSet.contains(List.of(nextRow, nextCol)) && !mineSet.contains(List.of(r, c))) {
                mineFreq[r][c]++;
            }
        }

    }

    void saveInternalBoard() {

        savedBoard = new String[row][col];

        for (int r = 0; r < row; r++) {
            for (int c = 0; c < col; c++) {
                if (mineSet.contains(List.of(r, c))) {
                    savedBoard[r][c] = Cell.X.getCellState();
                } else if (mineFreq[r][c] > 0) {
                    savedBoard[r][c] = String.valueOf(mineFreq[r][c]);
                    emptySet.add(List.of(r, c));
                } else {
                    savedBoard[r][c] = Cell.O.getCellState();
                    emptySet.add(List.of(r, c));
                }
            }
        }
    }

    void makeExternalBoard() {

        if (currBoard == null) {
            currBoard = new String[row][col];

            for (int r = 0; r < row; r++) {
                for (int c = 0; c < col; c++) {
                    currBoard[r][c] = Cell.O.getCellState();
                }
            }
        }
    }

    void printBoard() {
        StringBuilder rowhead = new StringBuilder(" ");
        StringBuilder headerFooter = new StringBuilder("-");

        for (int i = 0; i < row + 2; i++) {
            if (i == 0 || i == row + 1) {
                rowhead.append("|");
                headerFooter.append("|");
            } else {
                rowhead.append(i);
                headerFooter.append("-");
            }
        }

        System.out.println(rowhead);
        System.out.println(headerFooter);

        for (int r = 0; r < currBoard.length; r++) {
            System.out.print(r + 1 + "|");
            for (int c = 0; c < currBoard[0].length; c++) {
                if (currBoard[r][c].equals(Cell.X.getCellState())) {
                    System.out.print(Cell.X.getCellState());
                } else {
                    System.out.print(currBoard[r][c]);
                }
            }
            System.out.println("|");
        }

        System.out.println(headerFooter);
    }
}

class Validation {

    static boolean isValidMineRequest(int num, int row, int col) {

        if (num >= (row * col)) {
            System.out.println("Please enter whole positive number that is less than the maximum number of cells" +
                    " in the grid.");
            return false;
        }
        return true;
    }

    static boolean isValidNum(String num) {

        try {
            int n = Integer.parseInt(num);
            if (n < 1) {
                System.out.println("Please enter whole positive number that is greater than 0 and less than the maximum number of cells" +
                        " in the grid.");
                return false;
            } else {
                return true;
            }
        } catch (NumberFormatException ignore) {

        }

        return false;
    }

    static String[] validateCoordinateCommand(String coordinates, Board b) {
        String[] xy = coordinates.split(" ");

        if (xy.length != 3) {
            System.out.println("Request must include mine coordinates and a command.");
            return new String[]{};
        }

        try {
            if (isValidNum(xy[0]) && isValidNum(xy[1])) {
                int x = Integer.parseInt(xy[0]);
                int y = Integer.parseInt(xy[1]);

                if (x > b.getRow() || y > b.getCol() || x < 1 || y < 1) {
                    System.out.println("Coordinate is not within the mine field");
                    return new String[]{};
                }
            }
            if (!xy[2].equals("free") && !xy[2].equals("mine")) {
                System.out.println("Option not recognized.");
                return new String[]{};
            }
        } catch (NumberFormatException ignore) {

        }

        return xy;
    }
}