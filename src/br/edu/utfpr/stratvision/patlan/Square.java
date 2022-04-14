package br.edu.utfpr.stratvision.patlan;

import java.util.ArrayList;

/**
 * @author Luis Carlos Ferreira Bueno - 08/11/2021
 */
public class Square implements Comparable<Square> {    
    
    private static final Square[] SQUARES;
    private static final ArrayList<Square> arrayListSquares;
    
    static {
        SQUARES = new Square[BitBoard.SQUARES];
        
        int index = 0;
        
        arrayListSquares = new ArrayList<>();
        
        for (int row = 1; row <= BitBoard.LINES; row++) {
            for (int col = 1; col <= BitBoard.COLUMNS; col++) {                
                SQUARES[index] = new Square(col, row);
                arrayListSquares.add(SQUARES[index]);
                index++;
            }
        }
    }
    
    private int column;
    private int columnIndex;    
    private int line;
    private int lineIndex;    
    private int arrayIndex;
    
    private String FEN;
    
    private boolean iswhite;    

    private Square(int column, int row){

        if (column < 1 || column > BitBoard.LINES){
            throw new IllegalArgumentException("Invalid column value [" + column + "]");
        }
        
        this.column  = column;
        columnIndex = column - 1;
                
        if (row < 1  || row > BitBoard.LINES){
            throw new IllegalArgumentException("Invalid line value [" + row + "]");
        }
        
        this.line  = row;
        lineIndex = row - 1;
        
        arrayIndex  = columnIndex + lineIndex * BitBoard.LINES;
        
        FEN = columnName(column) + row;
       
        if (row % 2 == 1) {
            iswhite = !(column % 2 == 1);
        } else {
            iswhite = (column % 2 == 1);
        }
    }

    public Square() {
        
    }

    public String getFEN() {
        return FEN;
    }
    
    public int getColumn() {
        return column;
    }
   
    public int getColumnIndex() {
        return columnIndex;
    }
    
    public int getLine() {
        return line;
    }

    public int getLineIndex() {
        return lineIndex;
    }
    
    public int getIndex() {
        return arrayIndex;
    }
    
    public boolean isWhite() {
        return iswhite;
    }
        
    public static String columnName(int col){       
        return String.valueOf( (char) ('a' + (col - 1)) );
    }
    
    @Override
    public String toString(){
        return FEN;
    }
    
    @Override
    public int compareTo(final Square square) {
        
        if (square == null) {
            throw new NullPointerException();
        }

        return getFEN().compareTo(square.getFEN());
    }

    @Override
    public boolean equals(final Object object) {
        
        if (object == this) {
            return true;
        }

        if (!(object instanceof Square)) {
            return false;
        }

        final Square o = (Square) object;
        
        return arrayIndex == o.arrayIndex;
    }
    
    @Override
    public int hashCode() {
        return arrayIndex;
    }

    public static Square getByFEN(final String squareName) {
        if (squareName == null) {
            throw new NullPointerException("Square name cannot be null.");
        }
        
        if (squareName.length() != 2) {
            throw new IllegalArgumentException("Square name is not valid [" + squareName + ']');
        }
        return getByIndexes(squareName.charAt(0) - 'a', squareName.charAt(1) - '1');
    }

    public static Square getByIndex(final int index) {
        
        assert (index >= 0) && (index < BitBoard.SQUARES);

        return SQUARES[index];
    }
    
    public static Square getByAlgebric(String alg) {
        if(alg.length()==2) {
            return getByFEN(alg);
        } else {
            return getByFEN(alg.substring(1));
        }
    }
    
    public static Square getByIndexes(final int columnIndex, final int rowIndex) {
        
        if ((columnIndex < 0) || (columnIndex >= BitBoard.COLUMNS)) {
            throw new IllegalArgumentException("Column index invalid [" + columnIndex + ']');
        }
        
        if ((rowIndex < 0)  || (rowIndex  >= BitBoard.LINES)) {
            throw new IllegalArgumentException("Line index invalid [" + rowIndex + ']');
        }

        return SQUARES[columnIndex + rowIndex * BitBoard.COLUMNS];
    }

    public static ArrayList<Square> getAllBoard(){
        return  arrayListSquares;
    }
}