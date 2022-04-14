package br.edu.utfpr.stratvision.patlan;

import java.util.HashMap;
import java.util.Map;

public enum Piece {

    BLACK_PAWN('p',  false, PieceType.PAWN),
    BLACK_ROOK('r', false, PieceType.ROOK),
    BLACK_KNIGHT('n',false, PieceType.KNIGHT),
    BLACK_BISHOP('b', false, PieceType.BISHOP),
    BLACK_QUEEN('q',  false, PieceType.QUEEN),
    BLACK_KING('k',   false, PieceType.KING),
    
    WHITE_PAWN('P',  true, PieceType.PAWN),
    WHITE_ROOK('R', true, PieceType.ROOK),
    WHITE_KNIGHT('N',true, PieceType.KNIGHT),
    WHITE_BISHOP('B', true, PieceType.BISHOP),
    WHITE_QUEEN('Q',  true, PieceType.QUEEN),
    WHITE_KING('K',   true, PieceType.KING);
    
    private static final Map<Character, Piece> hashMap = new HashMap<>();

    static {
        for (final Piece p : values()) {
            hashMap.put(Character.valueOf(p.getFEN()), p);
        }
    }

    private char     FEN;        
    private String   description;
    private PieceType pieceType;    
    private boolean  isWhite;    
    
    private Piece(final char san, final boolean isWhite, final PieceType pieceType) {
        
        assert pieceType != null;

        this.FEN  = san;
        this.pieceType  = pieceType;
        this.isWhite = isWhite;
                
        switch(pieceType){
            case PAWN:
            case KNIGHT:
            case BISHOP:
            case KING:
                description = pieceType.getName() + " " + (isWhite?"WHITE":"BLACK");
                break;
                
            case ROOK:
            case QUEEN:
                description = pieceType.getName() + " " + (isWhite?"WHITE":"BLACK");
                break;
                
            default:
                throw new IllegalArgumentException("Piece type [" + pieceType + "] not supported!");
        }
    }

    public char getFEN() {
        return FEN;
    }

    public PieceType getPieceType() {
        
        assert pieceType != null;
        
        return pieceType;
    }

    public boolean isWhite() {
        return isWhite;
    }
        
    @Override
    public String toString(){
        return description;
    }
    
    public boolean equal(Piece piece){
        return piece != null &&  pieceType == piece.getPieceType() && isWhite == piece.isWhite();
    }
    
    public static Piece getByFEN(final char san) {
        return hashMap.get(san);
    }
}