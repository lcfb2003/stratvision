package br.edu.utfpr.stratvision.patlan;

/**
 *
 * @author Luis Carlos F. Bueno - 15/11/21
 */
public enum PieceType {
    
    PAWN("Pawn", '\000'),
    ROOK("Rook", 'R'),
    KNIGHT("Knight", 'K'),
    BISHOP("Bishop", 'B'),
    QUEEN("Queen", 'Q'),
    KING("King", 'K');
    
    private final String name;
    private final char   SAN; 
        
    private PieceType(String name, char san) {
        
        if (name == null || name.trim().length() == 0){
            throw new IllegalArgumentException("Name invalid [" + name + "]");
        }
        
        this.name = name.trim();        
        this.SAN  = san;
    }
    
    public String getName(){
        return name;
    }
    
    public char getSAN() {
        return SAN;
    }
    
    @Override
    public String toString(){
        return name;
    }
}