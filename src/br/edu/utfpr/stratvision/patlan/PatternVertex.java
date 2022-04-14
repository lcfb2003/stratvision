package br.edu.utfpr.stratvision.patlan;

/**
 *
 * @author Luis Carlos F. Bueno - 14/11/2021
 */
public class PatternVertex {

    private String value;
    private Square square, sourceSquare;
    private PieceType piecetype;
    private Piece piece;
    private boolean isIndirect; // determines if the vertex is intermediary of a relation with 3 or more vertex
    private ScenarioAction.PieceOwner owner; //0 - PLAYER, 1 - OPPONENT
    public PatternVertex() {

    }
    /*
        Safe the source square of the vertex. This method is invoke before apply the foreseen moves formalized in the Pattern
        in order to evaluate the post-conditions.
     */
    public void resetSquare(Square square) { 
        this.sourceSquare = this.square;
        this.square = square;
    }
    // used to restore the source square after evalue the post-conditions
    public void restoreSquare() {
        this.square = this.sourceSquare;
    }
    
    public String getSAN() {
        if(value.length() == 3) { // the vertex instantiated refers to a piece
            return value.substring(0,1);
        } else { // refers to an empty square
            return "";
        }
    }
    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the casa
     */
    public Square getSquare() {
        return square;
    }

    /**
     * @param square the square to set
     */
    public void setSquare(Square square) {
        this.square = square;
    }

    /**
     * @return the piecetype
     */
    public PieceType getPieceType() {
        return piecetype;
    }

    /**
     * @param piecetype
     */
    public void setTipoPeca(PieceType piecetype) {
        this.piecetype = piecetype;
    }

    /**
     * @return the indirect
     */
    public boolean isIndirect() {
        return isIndirect;
    }

    /**
     * @param isIndirect the isIndirect to set
     */
    public void setIndirect(boolean isIndirect) {
        this.isIndirect = isIndirect;
    }

    /**
     * @return the owner
     */
    public ScenarioAction.PieceOwner getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(ScenarioAction.PieceOwner owner) {
        this.owner = owner;
    }

    /**
     * @return the piece
     */
    public Piece getPiece() {
        return piece;
    }

    /**
     * @param piece the piece to set
     */
    public void setPiece(Piece piece) {
        this.piece = piece;
    }
}
