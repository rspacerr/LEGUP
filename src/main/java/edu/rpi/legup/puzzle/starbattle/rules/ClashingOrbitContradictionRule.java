package edu.rpi.legup.puzzle.starbattle.rules;

import edu.rpi.legup.model.gameboard.Board;
import edu.rpi.legup.model.gameboard.PuzzleElement;
import edu.rpi.legup.model.rules.ContradictionRule;
import edu.rpi.legup.model.tree.TreeNode;
import edu.rpi.legup.model.tree.TreeTransition;
import edu.rpi.legup.puzzle.starbattle.StarBattleBoard;
import edu.rpi.legup.puzzle.starbattle.StarBattleCell;
import edu.rpi.legup.puzzle.starbattle.StarBattleCellType;

import java.awt.*;

public class ClashingOrbitContradictionRule extends ContradictionRule {

    public ClashingOrbitContradictionRule() {
        super("STBL-CONT-0003",
                "Clashing Orbit",
                "No two stars can be adjacent to each other.",
                "INSERT IMAGE NAME HERE");
    }

    /**
     * Checks whether the transition has a contradiction at the specific puzzleElement index using this rule
     *
     * @param board         board to check contradiction
     * @param puzzleElement equivalent puzzleElement
     * @return null if the transition contains a contradiction at the specified puzzleElement,
     * otherwise error message
     */
    @Override
    public String checkContradictionAt(Board board, PuzzleElement puzzleElement) {
        StarBattleBoard starbattleBoard = (StarBattleBoard) board;
        StarBattleCell cell = (StarBattleCell) starbattleBoard.getPuzzleElement(puzzleElement);

        if (cell.getType() != StarBattleCellType.STAR) {
            return super.getNoContradictionMessage();
        }

        // check neighboring cells for a star
        Point location = cell.getLocation();

        int rowStart  = Math.max( location.x - 1, 0 );
        int rowEnd = Math.min( location.x + 1, starbattleBoard.getSize() - 1 );
        int colStart  = Math.max( location.y - 1, 0 );
        int colEnd = Math.min( location.y + 1, starbattleBoard.getSize() - 1 );

        for (int row = rowStart; rowStart <= rowEnd; row++) {
            for (int col = colStart; colStart <= colEnd; col++) {
                if (starbattleBoard.getCell(row, col).getType() == StarBattleCellType.STAR) {
                    return null;
                }
            }
        }

        return super.getNoContradictionMessage();
    }
}