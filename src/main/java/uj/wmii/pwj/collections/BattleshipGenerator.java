package uj.wmii.pwj.collections;

import java.util.Random;
import java.util.Stack;
import java.util.Vector;

public interface BattleshipGenerator {

    String generateMap();

    static BattleshipGenerator defaultInstance() {
        int width = 10;
        int height = 10;
        int[] numBattleships = {4, 3, 2, 1};
        return new DiegosBattleshipGenerator(height, width, numBattleships);
    }

}

class DiegosBattleshipGenerator implements BattleshipGenerator {
    int totalNumBattleships;
    int[] battleshipSizes;
    CellArray array;

    DiegosBattleshipGenerator(int height, int width, int[] numBattleships) {
        totalNumBattleships = 0;
        for (int i : numBattleships) {
            totalNumBattleships += i;
        }
        battleshipSizes = new int[totalNumBattleships + 1];
        array = new CellArray(height, width);

        // initializes all battleship sizes in decreasing order
        // since heuristically it's easier to find space for a big battleship first
        int count = 0;
        for (int i = numBattleships.length - 1; i >= 0; i--) {
            for (int j = 0; j < numBattleships[i]; j++) {
                battleshipSizes[count] = i + 1;
                count++;
            }
        }
        battleshipSizes[totalNumBattleships] = 0;
    }

    // this method implements backtracking,
    // we select a random free Cell to start growing each battleship
    // and then grow the battleship from any of its available neighboring Cells
    // at random until it's the right size
    //
    // if at any point we can't proceed, we go to the last step,
    // mark it unavailable and try again
    @Override
    public String generateMap() {
        Stack<BattleshipLayer> finishedBattleshipLayers = new Stack<>();
        int numFinishedLayers;
        BattleshipLayer workingLayer = new BattleshipLayer(battleshipSizes[0]);

        // keeps trying to create new battleships until all succeed
        while((numFinishedLayers = finishedBattleshipLayers.size()) != totalNumBattleships) {
            if (workingLayer.generateBattleship(array)) {
                finishedBattleshipLayers.add(workingLayer);
                if (numFinishedLayers + 1 < totalNumBattleships) {
                    workingLayer = new BattleshipLayer(battleshipSizes[numFinishedLayers + 1]);
                }
            }
            else {
                if (numFinishedLayers == 0) {
                    return "It's impossible to generate such a map.";
                }
                // current attempt failed, need to recreate the last battleship
                else {
                    workingLayer = finishedBattleshipLayers.pop();
                }
            }
        }
        return array.toString();
    }
}

class BattleshipLayer {
    int battleshipSize;
    Stack<CellLayer> finishedCellLayers;
    CellContainer freeCells;

    BattleshipLayer(int battleshipSize) {
        this.battleshipSize = battleshipSize;
        finishedCellLayers = null;
    }

    boolean generateBattleship(CellArray array) {
        CellLayer workingLayer;
        if (finishedCellLayers == null) {
            finishedCellLayers = new Stack<>();
            workingLayer = new CellLayer();
            freeCells = array.getFreeCells();
        }
        else {
            workingLayer = finishedCellLayers.pop();
        }

        int numFinishedLayers;
        // keeps track of the Cells assigned to the current battleship
        CellContainer placedCells = new CellContainer(array.height, array.width);

        // keeps trying to create new Cells until all succeed
        while ((numFinishedLayers = finishedCellLayers.size()) != battleshipSize) {
            if(workingLayer.generateCell(array, freeCells, placedCells)) {
                finishedCellLayers.add(workingLayer);
                workingLayer = new CellLayer();
            }
            else {
                if (numFinishedLayers == 0) {
                    // it's impossible to create this battleship
                    return false;
                }
                else {
                    // current attempt failed, need to recreate the last Cell
                    workingLayer = finishedCellLayers.pop();
                }
            }
        }
        return true;
    }
}

class CellLayer {
    CellContainer availableCells;
    Cell chosenCell;

    boolean generateCell(CellArray array, CellContainer freeCells, CellContainer previouslyPlacedCells) {
        // checks if we've already tried to place a Cell using this CellLayer
        if (availableCells == null) {
            // checks if this is the first Cell in the battleship
            if (previouslyPlacedCells.isEmpty()) {
                // we can use any of the free Cells
                availableCells = new CellContainer(freeCells);
            }
            else {
                // we can only use free Cells adjacent to the placed ones
                availableCells = getAvailableCells(array, freeCells, previouslyPlacedCells);
            }
        }
        else {
            // removes the last cell and marks it as unavailable for this CellLayer
            previouslyPlacedCells.removeLast();
            array.setCellFree(chosenCell);
            freeCells.push(chosenCell);
        }

        if (!availableCells.isEmpty()) {
            chosenCell = availableCells.popRandomCell();
            array.setCellOccupied(chosenCell);
            freeCells.remove(chosenCell);
            previouslyPlacedCells.push(chosenCell);
            return true;
        }
        else {
            return false;
        }
    }

    // generates all Cells where the battleship can grow
    CellContainer getAvailableCells(CellArray array, CellContainer freeCells, Stack<Cell> previouslyPlacedCells) {
        CellContainer result = new CellContainer(array.getHeight(), array.getWidth());
        for (Cell c : previouslyPlacedCells) {
            Vector<Cell> neighbors = array.getAllAdjacentNeighbors(c);
            for (Cell cc : neighbors) {
                if (freeCells.contains(cc)) {
                    result.push(cc);
                }
            }
        }
        return result;
    }
}

// a class to hold a Stack<Cell>, tell in O(1) time whether it contains a Cell,
// and pseudo-randomly choose one of its members with a uniform distribution
class CellContainer extends Stack<Cell> {
    Random rand = new Random();
    boolean[][] contains;

    CellContainer(CellContainer other) {
        for (Cell c : other) {
            super.push(c);
        }
        this.contains = new boolean[other.contains.length][];
        for (int i = 0; i < other.contains.length; i++) {
            this.contains[i] = other.contains[i].clone();
        }
        this.rand = new Random();
    }

    CellContainer(int height, int width) {
        contains = new boolean[height][width];
        for (int i = 0; i < height; i++) {
            contains[i] = new boolean[width];
            for (int j = 0; j < width; j++) {
                contains[i][j] = false;
            }
        }
    }

    Cell popRandomCell() {
        int index = rand.nextInt(this.size());
        this.swapWithFinalElement(index);
        return this.pop();
    }

    @Override
    public Cell pop() {
        Cell result = super.pop();
        contains[result.height][result.width] = false;
        return result;
    }


    @Override
    public Cell push(Cell c) {
        if(!contains[c.height][c.width]) {
            super.push(c);
            contains[c.height][c.width] = true;
        }
        return c;
    }

    @Override
    public boolean remove(Object o) {
        Cell c = (Cell) o;
        contains[c.height][c.width] = false;
        return super.remove(c);
    }

    boolean contains(Cell c) {
        return contains[c.height][c.width];
    }

    void swapWithFinalElement(int index) {
        Cell temp = this.get(index);
        this.setElementAt(this.set(this.size() - 1, temp), index);
    }
}


// the class handling the current state of growing battleships
// and whether cells are occupied or have neighbors
class CellArray {
    int height, width;
    CellContainer candidateCells;
    Cell[][] array;

    CellArray(int height, int width) {
        this.height = height;
        this.width = width;
        candidateCells = new CellContainer(height, width);

        array = new Cell[height][width];
        for (int i = 0; i < height; i++) {
            array[i] = new Cell[width];
            for (int j = 0; j < width; j++) {
                array[i][j] = new Cell(i, j);
                candidateCells.push(array[i][j]);
            }
        }
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    void setCellOccupied(Cell targetCell) {
        targetCell.setOccupied();
        for (Cell c : getAllNeighbors(targetCell)) {
            c.addNeighbor();
            candidateCells.remove(c);
        }
        candidateCells.remove(targetCell);
    }

    void setCellFree(Cell targetCell) {
        targetCell.setFree();
        for (Cell c : getAllNeighbors(targetCell)) {
            c.removeNeighbor();
            if (!c.hasNeighbors() && !c.isOccupied) {
                candidateCells.push(c);
            }
        }
        if (!targetCell.hasNeighbors()) {
            candidateCells.push(targetCell);
        }
    }

    // generates a snapshot of which Cells are free at the moment of execution
    CellContainer getFreeCells() {
        return new CellContainer(candidateCells);
    }

    Vector<Cell> getAllAdjacentNeighbors(Cell targetCell) {
        Vector<Cell> neighbors = new Vector<>();
        int y = targetCell.height;
        int x = targetCell.width;
        if (isValidCell(x, y + 1)) {
            neighbors.add(array[y + 1][x]);
        }
        if (isValidCell(x, y - 1)) {
            neighbors.add(array[y - 1][x]);
        }
        if (isValidCell(x + 1, y)) {
            neighbors.add(array[y][x + 1]);
        }
        if (isValidCell(x - 1, y)) {
            neighbors.add(array[y][x - 1]);
        }
        return neighbors;
    }

    Vector<Cell> getAllNeighbors(Cell targetCell) {
        Vector<Cell> neighbors = new Vector<>();
        int x, y;
        for(int i = -1; i <= 1; i++) {
            x = targetCell.height + i;
            for(int j = -1; j <= 1; j++) {
                y = targetCell.width + j;
                if (isValidCell(x, y) && !(i == 0 && j == 0)) {
                    neighbors.add(array[x][y]);
                }
            }
        }
        return neighbors;
    }

    boolean isValidCell(int x, int y) {
        return 0 <= x && x < width && 0 <= y && y < height;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Cell[] row : array) {
            for (Cell c : row) {
                result.append(
                        c.isOccupied() ? '#' : '.'
                );
            }
        }
        return result.toString();
    }
}

class Cell {
    int height, width;
    boolean isOccupied;

    // for simplicity, "neighbors" in method and field names means "occupied neighbors"
    // unless preceded by a different adjective

    // keeps track of how many neighbors the Cell has, either adjacent or diagonal
    int numNeighbors;

    Cell(int height, int width) {
        this.height = height;
        this.width = width;
        isOccupied = false;
        numNeighbors = 0;
    }

    void setOccupied() {
        isOccupied = true;
    }

    void setFree() {
        isOccupied = false;
    }

    boolean isOccupied() {
        return isOccupied;
    }

    void addNeighbor() {
        numNeighbors++;
    }

    void removeNeighbor() {
        numNeighbors--;
    }

    boolean hasNeighbors() {
        return numNeighbors > 0;
    }
}
