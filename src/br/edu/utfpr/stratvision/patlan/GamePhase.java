package br.edu.utfpr.stratvision.patlan;

/**
 *
 * @author Luis C. F. Bueno
 * This class defines the values of pieces during a game phase: start, middle and end game
 * The user can define diferent values for each piece kind
 */

public class GamePhase {

    public static final int STARTGAME = 0;
    public static final int MIDDLEGAME = 1;
    public static final int ENDGAME = 2;
    
    private int phase;
    private int queenValue;
    private int rookValue;
    private int knightValue;
    private int bishopValue;
    private int pawnValue;
    
    /**
     * @return the phase
     */
    public int getPhase() {
        return phase;
    }

    /**
     * @param phase the phase to set
     */
    public void setPhase(int phase) {
        this.phase = phase;
    }

    /**
     * @return the queenValue
     */
    public int getQueenValue() {
        return queenValue;
    }

    /**
     * @param queenValue the queenValue to set
     */
    public void setQueenValue(int queenValue) {
        this.queenValue = queenValue;
    }

    /**
     * @return the rookValue
     */
    public int getRookValue() {
        return rookValue;
    }

    /**
     * @param rookValue the rookValue to set
     */
    public void setRookValue(int rookValue) {
        this.rookValue = rookValue;
    }

    /**
     * @return the knightValue
     */
    public int getKnightValue() {
        return knightValue;
    }

    /**
     * @param knightValue the knightValue to set
     */
    public void setKnightValue(int knightValue) {
        this.knightValue = knightValue;
    }

    /**
     * @return the bishopValue
     */
    public int getBishopValue() {
        return bishopValue;
    }

    /**
     * @param bishopValue the bishopValue to set
     */
    public void setBishopValue(int bishopValue) {
        this.bishopValue = bishopValue;
    }

    /**
     * @return the pawnValue
     */
    public int getPawnValue() {
        return pawnValue;
    }

    /**
     * @param pawnValue the pawnValue to set
     */
    public void setPawnValue(int pawnValue) {
        this.pawnValue = pawnValue;
    }
    
    public int getValueBySAN(String psan) {
        switch(psan.toUpperCase()) {
            case "P" : return pawnValue;
            case "R" : return rookValue;
            case "N" : return knightValue;
            case "B" : return bishopValue;
            case "Q" : return queenValue;
            default: return 0;
        }
    }
}
