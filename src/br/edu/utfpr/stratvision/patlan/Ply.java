package br.edu.utfpr.stratvision.patlan;

import java.util.regex.Pattern;

public final class Ply {

    /**
     * Regular expression to evaluate the generated SAN
     *
     * Mat/Pat/Nullité : (\\+{1,2}|#|\\(=\\))?
     * Petit roque : 0-0<Mat/Pat/Nullité>
     * Grand roque : 0-0-0<Mat/Pat/Nullité>
     * Pion sans prise : [a-h]([1-8]|[18][BKNQR])<Mat/Pat/Nullité>
     * Pion avec prise :
     * [a-h]x[a-h]((([1-8]|[18][BKNQR])<Mat/Pat/Nullité>)|([36]<Mat/Pat/Nullité> e\\.p\\.))
     * Pièces (sauf pion) : [BKNQR][a-h]?[1-8]?x?[a-h][1-8]<Mat/Pat/Nullité>
     */ 
    public static final Pattern SAN_VALIDATOR = Pattern.compile("^(0-0(\\+{1,2}|#|\\(=\\))?)|(0-0-0(\\+{1,2}|#|\\(=\\))?)|" +
                                                                "([a-h]([1-8]|[18][BKNQR])(\\+{1,2}|#|\\(=\\))?)|"          +
                                                                "([a-h]x[a-h]((([1-8]|[18][BKNQR])(\\+{1,2}|#|\\(=\\))?)|"  +
                                                                "([36](\\+{1,2}|#|\\(=\\))? e\\.p\\.)))|"                   +
                                                                "([BKNQR][a-h]?[1-8]?x?[a-h][1-8](\\+{1,2}|#|\\(=\\))?)$");
    private final Piece piece;
    private final Piece capturedPiece;
    private final Square sourceSquare;
    private final Square targetSquare;
    
    private Integer plyId;

    public Ply(final Piece piece, final Square source, final Square target) {
        
        assert piece        != null;
        assert source  != null;
        assert target != null;
        assert source  != target;

        this.piece          = piece;
        this.sourceSquare    = source;
        this.targetSquare   = target;
        this.capturedPiece = null;
    }

    public Ply(final Piece piece, final Square source, final Square target, final Piece captured) {
        
        assert piece        != null;
        assert source  != null;
        assert target != null;
        assert source  != target;
        
        assert (captured == null) || (piece.isWhite() != captured.isWhite());

        this.piece          = piece;
        this.sourceSquare    = source;
        this.targetSquare   = target;
        this.capturedPiece = captured;
    }

    public Piece getPiece() {
        return piece;
    }
    
    public Piece getCapturedPiece() {
        return  capturedPiece;
    }

    public Square getSourceSquare() {
        return sourceSquare;
    }

    public Square getTargetSquare() {
        return targetSquare;
    }

    public boolean isPromotion() {
        
        if (piece.getPieceType() == PieceType.PAWN) {

            int linhaDestino   = targetSquare.getLineIndex();
            boolean pecaBranca = piece.isWhite();

            if ((pecaBranca && linhaDestino == BitBoard.LINES - 1) || (!pecaBranca && linhaDestino == 0)) {
                return true;
            }
        }
        
        return false;
    }
    
    public int toId() {
        
        if (plyId == null) {
            
            int id = (piece.ordinal() << 20) + (sourceSquare.getIndex() << 14) + (targetSquare.getIndex() << 8);
            
            if (capturedPiece != null) {
                id += (capturedPiece.ordinal() + 1) << 4;
            }
            
            plyId = id;
        }

        return plyId;
    }
     
    @Override
    public boolean equals(final Object object) {
        
        if (this == object) {
            return true;
        }
        
        if (object == null){
            return false;
        }

        if (object instanceof Ply) {
            
            final Ply m = (Ply) object;
            
            return (sourceSquare.equals(m.sourceSquare)) && 
                   (piece          == m.piece)        && 
                   (targetSquare .equals(m.targetSquare)) &&
                   (capturedPiece == m.capturedPiece);
        }

        return false;
    }
    
    public String getSAN() {
        return getPiece().getFEN() + getSourceSquare().getFEN() + getTargetSquare().getFEN();
    }
    
    @Override
    public int hashCode() {
        return toId();
    }

    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append(piece);
        sb.append(" from ");
        sb.append(sourceSquare.getFEN());
        sb.append(" to ");
        sb.append(targetSquare.getFEN());
        
        if (capturedPiece != null){
            sb.append(", captured ").append(capturedPiece);
        }
        
        return sb.toString();
    }

    public static Ply getById(final int pId) {
        
        final Piece piece        = Piece.values()[(pId >> 20) & 0xF];
        final Square source  = Square.getByIndex((pId >> 14) & 0x3F);
        final Square target = Square.getByIndex((pId >> 8)  & 0x3F);
        final int  idCpt  = (pId >> 4) & 0xF;
        final Piece captured;
        
        if (idCpt <= 0) {
            captured = null;
        } else {
            captured = Piece.values()[idCpt - 1];
        }

        return new Ply(piece, source, target, captured);
    }
}