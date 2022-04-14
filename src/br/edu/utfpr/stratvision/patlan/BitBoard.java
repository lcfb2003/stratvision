package br.edu.utfpr.stratvision.patlan;

import br.edu.utfpr.stratvision.utils.ChessColor;
import br.edu.utfpr.stratvision.utils.PGNMove;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author Luis C. F. Bueno
 * Defines a bitboard representation of chess 
 */
public class BitBoard implements Cloneable {

    //<editor-fold defaultstate="collapsed" desc="Variables and fields">
    public static final int SQUARES = 64;
    public static final int LINES = 8;
    public static final int COLUMNS = 8;
    private final int pliesCount = 0;
    private final int movesCount = 0;
    private boolean isWhite = true; // true if the player searching is white.
    
    public Bits bits;
    public long unsafeforW, unsafeforB;
    public static final long[] bitMasks = new long[SQUARES];
    public int[] visited = new int[SQUARES]; // used in the kingPathTo method
    public String chessBoard[][] = {
        {" ", " ", " ", " ", " ", " ", " ", " "},
        {" ", " ", " ", " ", " ", " ", " ", " "},
        {" ", " ", " ", " ", " ", " ", " ", " "},
        {" ", " ", " ", " ", " ", " ", " ", " "},
        {" ", " ", " ", " ", " ", " ", " ", " "},
        {" ", " ", " ", " ", " ", " ", " ", " "},
        {" ", " ", " ", " ", " ", " ", " ", " "},
        {" ", " ", " ", " ", " ", " ", " ", " "}};

    public double[][] MyDefensesValue = new double[8][8]; //sum of the values of the pieces that defend a square directly/indirectly 
    public double[][] YourDefensesValue = new double[8][8]; //sum of the values of the pieces that defend a square directly/indirectly 
    public int[][] MyDefensesCount = new int[8][8]; //counting of the pieces that defend a square directly or indirectly 
    public int[][] YourDefensesCount = new int[8][8]; //counting of the pieces that defend a square directly or indirectly 
    
    public int[][][] MyDefensesCountByType = new int[5][8][8]; // counting of the pieces that defend a square directly by type
    public int[][][] YourDefensesCountByType = new int[5][8][8]; // counting of the pieces that defend a square directly by type

    // save the relative value of defenses 
    // relative value = defending piece value * (col + lin * 8)
    // these matrixs can be used to verify if only certain piece of the scenario is defending any square,
    // using its heuristic value
    public double[][] MyRelativeDefensesValue = new double[8][8];
    public double[][] YourRelativeDefensesValue = new double[8][8];

    //sum of the pieces directly defending a square
    public double[][] MyDirectDefensesValue = new double[8][8];
    public double[][] ValorSuasDefesasDiretas = new double[8][8];
    public int[][] MyDirectDefenses = new int[8][8];
    public int[][] YourDirectDefenses = new int[8][8];
    
    private int stack;
    
    static {
        for (int i = 0; i < SQUARES; i++) {
            bitMasks[i] = 1L << i;
        }
    }
    
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    @Override
    public BitBoard clone() throws CloneNotSupportedException {
        try {
            BitBoard b = (BitBoard) super.clone();
            b.bits = this.bits.Clone();
            b.visited = this.visited.clone();
            b.chessBoard = (String[][]) string2DClone(this.chessBoard);
            b.MyDefensesValue = double2DClone(this.MyDefensesValue);
            b.YourDefensesValue = double2DClone(this.YourDefensesValue);
            b.MyDefensesCount = int2DClone(this.MyDefensesCount);
            b.YourDefensesCount = int2DClone(this.YourDefensesCount);
            b.MyDirectDefensesValue = double2DClone(MyDirectDefensesValue);
            b.ValorSuasDefesasDiretas = double2DClone(ValorSuasDefesasDiretas);
            b.MyDirectDefenses = int2DClone(MyDirectDefenses);
            b.YourDirectDefenses = int2DClone(YourDirectDefenses);
            b.MyRelativeDefensesValue = double2DClone(MyRelativeDefensesValue);
            b.YourRelativeDefensesValue = double2DClone(YourRelativeDefensesValue);
            b.MyDefensesCountByType = int3DClone(MyDefensesCountByType);
            b.YourDefensesCountByType = int3DClone(YourDefensesCountByType);
            return b;
        } catch (CloneNotSupportedException ex) {
            throw ex;
        }
    }

    private int[][][] int3DClone(int[][][] a) {
        int[][][] b = new int[a.length][][];
        for (int i = 0; i < a.length; i++) {
            b[i] = int2DClone(a[i]);
            System.arraycopy(a[i], 0, b[i], 0, a[i].length);
        }
        return b;
    }
    
    private int[][] int2DClone(int[][] a) {
        int[][] b = new int[a.length][];
        for (int i = 0; i < a.length; i++) {
            b[i] = new int[a[i].length];
            System.arraycopy(a[i], 0, b[i], 0, a[i].length);
        }
        return b;
    }

    private double[][] double2DClone(double[][] a) {
        double[][] b = new double[a.length][];
        for (int i = 0; i < a.length; i++) {
            b[i] = new double[a[i].length];
            System.arraycopy(a[i], 0, b[i], 0, a[i].length);
        }
        return b;
    }

    private String[][] string2DClone(String[][] a) {
        String[][] b = new String[a.length][];
        for (int i = 0; i < a.length; i++) {
            b[i] = new String[a[i].length];
            System.arraycopy(a[i], 0, b[i], 0, a[i].length);
        }
        return b;
    }

    public void arrayToBitboards() {
        String Binary;
        bits = new Bits();
        for (int i = 0; i < SQUARES; i++) {
            Binary = "0000000000000000000000000000000000000000000000000000000000000000";
            Binary = Binary.substring(i + 1) + "1" + Binary.substring(0, i);
            int lin = i / LINES, col = i % COLUMNS;
            switch (chessBoard[lin][col]) {
                case "P":
                    bits.WP += convertStringToBitboard(Binary);
                    break;
                case "N":
                    bits.WN += convertStringToBitboard(Binary);
                    break;
                case "B":
                    bits.WB += convertStringToBitboard(Binary);
                    break;
                case "R":
                    bits.WR += convertStringToBitboard(Binary);
                    break;
                case "Q":
                    bits.WQ += convertStringToBitboard(Binary);
                    break;
                case "K":
                    bits.WK += convertStringToBitboard(Binary);
                    break;
                case "p":
                    bits.BP += convertStringToBitboard(Binary);
                    break;
                case "n":
                    bits.BN += convertStringToBitboard(Binary);
                    break;
                case "b":
                    bits.BB += convertStringToBitboard(Binary);
                    break;
                case "r":
                    bits.BR += convertStringToBitboard(Binary);
                    break;
                case "q":
                    bits.BQ += convertStringToBitboard(Binary);
                    break;
                case "k":
                    bits.BK += convertStringToBitboard(Binary);
                    break;
            }
        }
    }

    public String getFen() {
        final StringBuilder res = new StringBuilder();

        // piece positions //
        for (int y = 0; y < LINES; y++) {

            int vide = 0;

            for (int x = 0; x < COLUMNS; x++) {

                String piece = chessBoard[y][x];

                if (piece.equals(" ")) {
                    vide++;
                } else {
                    if (vide > 0) {
                        res.append((char) ('0' + vide));
                        vide = 0;
                    }
                    res.append(piece);
                }
            }

            if (vide > 0) {
                res.append((char) ('0' + vide));
            }
            if (y != 7) {
                res.append('/');
            }
        }

        res.append(' ');

        // Active player //
        if (bits.WhiteToMove) {
            res.append('w');
        } else {
            res.append('b');
        }

        res.append(' ');

        // Castling //
        boolean castling = false;

        if (bits.CWK) {
            res.append('K');
            castling = true;
        }

        if (bits.CWQ) {
            res.append('Q');
            castling = true;
        }

        if (bits.CBK) {
            res.append('k');
            castling = true;
        }

        if (bits.CBQ) {
            res.append('q');
            castling = true;
        }

        if (!castling) {
            res.append('-');
        }

        res.append(' ');
        res.append('-'); //en-passant does not matter for this search
        res.append(' ');

        // Number of plies without pawn capture //
        res.append(Integer.toString(pliesCount));

        res.append(' ');

        // Number of moves //
        res.append(Integer.toString(movesCount == 0 ? 1 : movesCount));

        return res.toString();
    }
    //not chess960 compatible
    public void importFEN(String fen) throws Exception {
        if (fen == null) {
            throw new NullPointerException("FEN must not be null!");
        }

        final String[] fields = fen.split(" ");

        if (fields.length != 6) {
            throw new Exception("FEN is not incorrect [" + fen + ']', null);
        }

        int rank = 7;
        int charIndex = 0;
        String fenString = fen;
        int row = 0, col = 0;
        while (fenString.charAt(charIndex) != ' ') {
            String f = String.valueOf(fenString.charAt(charIndex));
            if (f.equals("/")) {
                if (col != 8 || rank <= 0) {
                    throw new Exception("Piece positions are incorrect in FEN [" + fields[0] + ']', null);
                }
                rank--;
                row++;
                col = 0;
            } else {
                if ("12345678".contains(f)) {
                    col += Integer.parseInt(f);
                } else if ("KQRBNPkqrbnp".contains(f)) {
                    chessBoard[row][col++] = f;
                } else {
                    throw new Exception("Piece identifiers invalid in FEN: " + f);
                }
            }
            charIndex++;
        }
        arrayToBitboards();
        bits.CBK = false;
        bits.CBQ = false;
        bits.CWK = false;
        bits.CWQ = false;
        bits.WhiteToMove = (fenString.charAt(++charIndex) == 'w');
        assignsPlayer(bits.WhiteToMove);
        charIndex += 2;
        while (fenString.charAt(charIndex) != ' ') {
            switch (fenString.charAt(charIndex++)) {
                case '-':
                    break;
                case 'K':
                    bits.CWK = true;
                    break;
                case 'Q':
                    bits.CWQ = true;
                    break;
                case 'k':
                    bits.CBK = true;
                    break;
                case 'q':
                    bits.CBQ = true;
                    break;
                default:
                    break;
            }
        }
        if (fenString.charAt(++charIndex) != '-') {
            bits.EP = FileMasks8[fenString.charAt(charIndex++) - 'a'];
        }

        unsafeforW = unsafeForWhite();
        unsafeforB = unsafeForBlack();
    }

    // associates the pieces of the player's color with the set of bits M or Y
    public void assignsPlayer(boolean playerColor) { 
        this.isWhite = playerColor;
        if (playerColor) { // white
            bits.MB = bits.WB;
            bits.MK = bits.WK;
            bits.MN = bits.WN;
            bits.MP = bits.WP;
            bits.MQ = bits.WQ;
            bits.MR = bits.WR;
            bits.SB = bits.BB;
            bits.SK = bits.BK;
            bits.SN = bits.BN;
            bits.SP = bits.BP;
            bits.SQ = bits.BQ;
            bits.SR = bits.BR;
        } else {
            bits.MB = bits.BB;
            bits.MK = bits.BK;
            bits.MN = bits.BN;
            bits.MP = bits.BP;
            bits.MQ = bits.BQ;
            bits.MR = bits.BR;
            bits.SB = bits.WB;
            bits.SK = bits.WK;
            bits.SN = bits.WN;
            bits.SP = bits.WP;
            bits.SQ = bits.WQ;
            bits.SR = bits.WR;
        }
    }

    public long convertStringToBitboard(String Binary) {
        if (Binary.charAt(0) == '0') {//not going to be a negative number
            return Long.parseLong(Binary, 2);
        } else {
            return Long.parseLong("1" + Binary.substring(2), 2) * 2;
        }
    }

    public String drawArray(Bits bits) {
        String cb[][] = new String[8][8];
        for (int i = 0; i < SQUARES; i++) {
            cb[i / LINES][i % COLUMNS] = " ";
        }
        for (int i = 0; i < SQUARES; i++) {
            if (((bits.WP >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "P";
            }
            if (((bits.WN >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "N";
            }
            if (((bits.WB >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "B";
            }
            if (((bits.WR >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "R";
            }
            if (((bits.WQ >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "Q";
            }
            if (((bits.WK >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "K";
            }
            if (((bits.BP >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "p";
            }
            if (((bits.BN >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "n";
            }
            if (((bits.BB >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "b";
            }
            if (((bits.BR >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "r";
            }
            if (((bits.BQ >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "q";
            }
            if (((bits.BK >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "k";
            }
        }
        StringBuilder d = new StringBuilder();
        for (int i = 0; i < LINES; i++) {
            d.append(Arrays.toString(cb[i]));
            d.append("\n");
        }
        return d.toString();
    }

    public void bitsToArray(Bits bits) {
        String cb[][] = new String[8][8];
        for (int i = 0; i < SQUARES; i++) {
            cb[i / LINES][i % COLUMNS] = " ";
        }
        for (int i = 0; i < SQUARES; i++) {
            if (((bits.WP >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "P";
            }
            if (((bits.WN >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "N";
            }
            if (((bits.WB >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "B";
            }
            if (((bits.WR >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "R";
            }
            if (((bits.WQ >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "Q";
            }
            if (((bits.WK >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "K";
            }
            if (((bits.BP >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "p";
            }
            if (((bits.BN >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "n";
            }
            if (((bits.BB >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "b";
            }
            if (((bits.BR >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "r";
            }
            if (((bits.BQ >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "q";
            }
            if (((bits.BK >> i) & 1) == 1) {
                cb[i / LINES][i % COLUMNS] = "k";
            }
        }
    }

    public void drawBitboard(long bitBoard) {
        String chessB[][] = new String[8][8];
        for (int i = 0; i < SQUARES; i++) {
            chessB[i / LINES][i % COLUMNS] = "";
        }
        for (int i = 0; i < SQUARES; i++) {
            if (((bitBoard >>> i) & 1) == 1) {
                chessB[i / LINES][i % COLUMNS] = "1";
            }
            if ("".equals(chessB[i / LINES][i % COLUMNS])) {
                chessB[i / LINES][i % COLUMNS] = " ";
            }
        }
        for (int i = 0; i < 8; i++) {
            System.out.println(Arrays.toString(chessB[i]));
        }
        System.out.println("");
    }
    //</editor-fold>

    //<editor-fold desc="Bitboard constants and masks">
    static final String BINARYWHITESQUARES = "1010101001010101101010100101010110101010010101011010101001010101";
    public static final long WHITESQUARES = new BigInteger(BINARYWHITESQUARES, 2).longValue();
    public static final long BLACKSQUARES = ~WHITESQUARES;
    long FILE_A = 72340172838076673L;
    long FILE_H = -9187201950435737472L;
    long FILE_AB = 217020518514230019L;
    long FILE_GH = -4557430888798830400L;
    long RANK_1 = -72057594037927936L;
    long RANK_4 = 1095216660480L;
    long RANK_5 = 4278190080L;
    long RANK_8 = 255L;
    long CENTRE = 103481868288L;
    long EXTENDED_CENTRE = 66229406269440L;
    long KING_SIDE = -1085102592571150096L;
    long QUEEN_SIDE = 1085102592571150095L;
    long KING_SPAN = 460039L;
    long KNIGHT_SPAN = 43234889994L;
    long CASTLE_ROOKS[] = {63, 56, 7, 0};
    long RankMasks8[]
            =/*from rank1 to rank8*/ {
                0xFFL, 0xFF00L, 0xFF0000L, 0xFF000000L, 0xFF00000000L, 0xFF0000000000L, 0xFF000000000000L, 0xFF00000000000000L
            };

    long FileMasks8[]
            =/*from fileA to FileH*/ {
                0x101010101010101L, 0x202020202020202L, 0x404040404040404L, 0x808080808080808L,
                0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L, 0x8080808080808080L
            };

    long DiagonalMasks8[]
            =/*from top left to bottom right*/ {
                0x1L, 0x102L, 0x10204L, 0x1020408L, 0x102040810L, 0x10204081020L, 0x1020408102040L,
                0x102040810204080L, 0x204081020408000L, 0x408102040800000L, 0x810204080000000L,
                0x1020408000000000L, 0x2040800000000000L, 0x4080000000000000L, 0x8000000000000000L
            };

    long AntiDiagonalMasks8[]
            =/*from top right to bottom left*/ {
                0x80L, 0x8040L, 0x804020L, 0x80402010L, 0x8040201008L, 0x804020100804L, 0x80402010080402L,
                0x8040201008040201L, 0x4020100804020100L, 0x2010080402010000L, 0x1008040201000000L,
                0x804020100000000L, 0x402010000000000L, 0x201000000000000L, 0x100000000000000L
            };

    long HAndVMoves(int s) {
        //REMINDER: requires bits.OCCUPIED to be up to date
        long binaryS = 1L << s;
        long possibilitiesHorizontal = (bits.OCCUPIED - 2 * binaryS) ^ Long.reverse(Long.reverse(bits.OCCUPIED) - 2 * Long.reverse(binaryS));
        long possibilitiesVertical = ((bits.OCCUPIED & FileMasks8[s % COLUMNS]) - (2 * binaryS)) ^ Long.reverse(Long.reverse(bits.OCCUPIED & FileMasks8[s % COLUMNS]) - (2 * Long.reverse(binaryS)));
        return (possibilitiesHorizontal & RankMasks8[s / LINES]) | (possibilitiesVertical & FileMasks8[s % COLUMNS]);
    }

    long DAndAntiDMoves(int s) {
        //REMINDER: requires bits.OCCUPIED to be up to date
        long binaryS = 1L << s;
        long possibilitiesDiagonal = ((bits.OCCUPIED & DiagonalMasks8[(s / LINES) + (s % COLUMNS)]) - (2 * binaryS)) ^ Long.reverse(Long.reverse(bits.OCCUPIED & DiagonalMasks8[(s / LINES) + (s % COLUMNS)]) - (2 * Long.reverse(binaryS)));
        long possibilitiesAntiDiagonal = ((bits.OCCUPIED & AntiDiagonalMasks8[(s / LINES) + 7 - (s % COLUMNS)]) - (2 * binaryS)) ^ Long.reverse(Long.reverse(bits.OCCUPIED & AntiDiagonalMasks8[(s / LINES) + 7 - (s % COLUMNS)]) - (2 * Long.reverse(binaryS)));
        return (possibilitiesDiagonal & DiagonalMasks8[(s / LINES) + (s % COLUMNS)]) | (possibilitiesAntiDiagonal & AntiDiagonalMasks8[(s / LINES) + 7 - (s % COLUMNS)]);
    }

    public void initiateStandardChess() {
        String cB[][] = {
            {"r", "n", "b", "q", "k", "b", "n", "r"},
            {"p", "p", "p", "p", "p", "p", "p", "p"},
            {" ", " ", " ", " ", " ", " ", " ", " "},
            {" ", " ", " ", " ", " ", " ", " ", " "},
            {" ", " ", " ", " ", " ", " ", " ", " "},
            {" ", " ", " ", " ", " ", " ", " ", " "},
            {"P", "P", "P", "P", "P", "P", "P", "P"},
            {"R", "N", "B", "Q", "K", "B", "N", "R"}};
        chessBoard = cB;
        arrayToBitboards();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Moves">
    
    //<editor-fold desc="Make Moves">
    public void makeMoveWrong(String move, Bits bits) {
        //can not opperate on a single board since moves are not backwards compatible
        bits.EP = 0;
        if (Character.isDigit(move.charAt(3))) {
            int start = (Character.getNumericValue(move.charAt(0)) * 8) + (Character.getNumericValue(move.charAt(1)));
            int end = (Character.getNumericValue(move.charAt(2)) * 8) + (Character.getNumericValue(move.charAt(3)));
            if ((bits.WK & (1L << start)) != 0) {//white castle move
                bits.WK ^= (1L << start);
                bits.WK ^= (1L << end);
                if (end > start) {//kingside
                    bits.WR ^= (1L << 63);
                    bits.WR ^= (1L << 61);
                    bits.CWK = false;
                } else {//queenside
                    bits.WR ^= (1L << 56);
                    bits.WR ^= (1L << 59);
                    bits.CWQ = false;
                }
            } else if ((bits.BK & (1L << start)) != 0) {//black castle move
                bits.WK ^= (1L << start);
                bits.WK ^= (1L << end);
                if (end > start) {//kingside
                    bits.WR ^= (1L << 7);
                    bits.WR ^= (1L << 5);
                    bits.CBK = false;
                } else {//queenside
                    bits.WR ^= 1L;
                    bits.WR ^= (1L << 3);
                    bits.CBQ = false;
                }
            } else {//'regular' move
                //clear destination:
                bits.WP &= ~(1L << end);
                bits.WN &= ~(1L << end);
                bits.WB &= ~(1L << end);
                bits.WR &= ~(1L << end);
                bits.WQ &= ~(1L << end);
                bits.WK &= ~(1L << end);
                //move piece:
                if ((bits.WP & (1L << start)) != 0) {
                    bits.WP ^= (1L << start);
                    bits.WP ^= (1L << end);
                    if ((end - start) == 16) {//pawn double push
                        bits.EP = FileMasks8['0' - move.charAt(1)];
                    }
                } else if ((bits.BP & (1L << start)) != 0) {
                    bits.BP ^= (1L << start);
                    bits.BP ^= (1L << end);
                    if ((start - end) == 16) {//pawn double push
                        bits.EP = FileMasks8['0' - move.charAt(1)];
                    }
                } else if ((bits.WN & (1L << start)) != 0) {
                    bits.WN ^= (1L << start);
                    bits.WN ^= (1L << end);
                } else if ((bits.BN & (1L << start)) != 0) {
                    bits.BN ^= (1L << start);
                    bits.BN ^= (1L << end);
                } else if ((bits.WB & (1L << start)) != 0) {
                    bits.WB ^= (1L << start);
                    bits.WB ^= (1L << end);
                } else if ((bits.BB & (1L << start)) != 0) {
                    bits.BB ^= (1L << start);
                    bits.BB ^= (1L << end);
                } else if ((bits.WR & (1L << start)) != 0) {
                    bits.WR ^= (1L << start);
                    bits.WR ^= (1L << end);
                } else if ((bits.BR & (1L << start)) != 0) {
                    bits.BR ^= (1L << start);
                    bits.BR ^= (1L << end);
                } else if ((bits.WQ & (1L << start)) != 0) {
                    bits.WQ ^= (1L << start);
                    bits.WQ ^= (1L << end);
                } else if ((bits.BQ & (1L << start)) != 0) {
                    bits.BQ ^= (1L << start);
                    bits.BQ ^= (1L << end);
                } else if ((bits.WK & (1L << start)) != 0) {
                    bits.WK ^= (1L << start);
                    bits.WK ^= (1L << end);
                    bits.CWK = false;
                    bits.CWQ = false;
                } else if ((bits.BK & (1L << start)) != 0) {
                    bits.BK ^= (1L << start);
                    bits.BK ^= (1L << end);
                    bits.CBK = false;
                    bits.CBQ = false;
                } else {
                    System.out.print("error: can not move empty piece");
                }
            }
        } else if (move.charAt(3) == 'P') {//pawn promotion
            //y1,y2,Promotion Type,"P"
            if (Character.isUpperCase(move.charAt(2)))//white piece promotion
            {
                bits.WP ^= (RankMasks8[6] & FileMasks8[move.charAt(0) - '0']);
                switch (move.charAt(2)) {
                    case 'N':
                        bits.WN ^= (RankMasks8[7] & FileMasks8[move.charAt(1) - '0']);
                        break;
                    case 'B':
                        bits.WB ^= (RankMasks8[7] & FileMasks8[move.charAt(1) - '0']);
                        break;
                    case 'R':
                        bits.WR ^= (RankMasks8[7] & FileMasks8[move.charAt(1) - '0']);
                        break;
                    case 'Q':
                        bits.WQ ^= (RankMasks8[7] & FileMasks8[move.charAt(1) - '0']);
                        break;
                }
            } else {//black piece promotion
                bits.BP ^= (RankMasks8[1] & FileMasks8[move.charAt(0) - '0']);
                switch (move.charAt(2)) {
                    case 'n':
                        bits.BN ^= (RankMasks8[0] & FileMasks8[move.charAt(1) - '0']);
                        break;
                    case 'b':
                        bits.BB ^= (RankMasks8[0] & FileMasks8[move.charAt(1) - '0']);
                        break;
                    case 'r':
                        bits.BR ^= (RankMasks8[0] & FileMasks8[move.charAt(1) - '0']);
                        break;
                    case 'q':
                        bits.BQ ^= (RankMasks8[0] & FileMasks8[move.charAt(1) - '0']);
                        break;
                }
            }
        } else if (move.charAt(3) == 'E') {//en passant move
            if (move.charAt(2) == 'w') {//white move
                //y1,y2,"BE"
                bits.WP ^= (RankMasks8[4] & FileMasks8['0' - move.charAt(0)]);//remove white pawn
                bits.WP ^= (RankMasks8[5] & FileMasks8['0' - move.charAt(1)]);//add white pawn
                bits.BP ^= (RankMasks8[4] & FileMasks8['0' - move.charAt(1)]);//remove black pawn)
            } else {//black move
                bits.BP ^= (RankMasks8[3] & FileMasks8['0' - move.charAt(0)]);//remove black pawn
                bits.BP ^= (RankMasks8[2] & FileMasks8['0' - move.charAt(1)]);//add black pawn
                bits.WP ^= (RankMasks8[3] & FileMasks8['0' - move.charAt(1)]);//remove white pawn)
            }
        } else {
            System.out.print("error: not a valid move type");
        }
    }

    /*
     long WPt=makeMove(WP, move.substring(i,i+4), 'P'), WNt=makeMove(WN, move.substring(i,i+4), 'N'),
     WBt=makeMove(WB, move.substring(i,i+4), 'B'), WRt=makeMove(WR, move.substring(i,i+4), 'R'),
     WQt=makeMove(WQ, move.substring(i,i+4), 'Q'), WKt=makeMove(WK, move.substring(i,i+4), 'K'),
     BPt=makeMove(BP, move.substring(i,i+4), 'p'), BNt=makeMove(BN, move.substring(i,i+4), 'n'),
     BBt=makeMove(BB, move.substring(i,i+4), 'b'), BRt=makeMove(BR, move.substring(i,i+4), 'r'),
     BQt=makeMove(BQ, move.substring(i,i+4), 'q'), BKt=makeMove(BK, move.substring(i,i+4), 'k'),
     EPt=makeMoveEP(move.substring(i,i+4));
     */
    // make the move and resets the from square
    private void makeSingleMove(int fromFile, int fromRank, int toFile, int toRank) {
        chessBoard[toRank][toFile] = chessBoard[fromRank][fromFile];
        chessBoard[fromRank][fromFile] = " ";
    }

    //make the move according to the model of ChessD and XadrezLivre - for tests purpose
    public void makeMoveBoard(String movAtual) {
        Bits bc = bits.Clone();
        int ixo = movAtual.charAt(0) - 'a';
        int iyo = 8 - Integer.parseInt(movAtual.substring(1, 2));
        int ixd = movAtual.charAt(2) - 'a';
        int iyd = 8 - Integer.parseInt(movAtual.substring(3, 4));
        if (movAtual.length() == 4) { //normal move, castling, en passant e promotion to queen
            // if the target square is empty and the file is different from the source and is a moving pawn
            if (chessBoard[iyd][ixd].equals(" ") && ixo != ixd
                    && // en passant
                    chessBoard[iyo][ixo].toUpperCase().equals("P")) { // erases the opponent pawn
                chessBoard[iyd][ixo] = " ";
            } else if (chessBoard[iyo][ixo].toUpperCase().equals("P")) { // promotion
                if (iyd == 0) { // white pawn promotion
                    chessBoard[iyd][ixd] = "Q";
                } else if (iyd == 7) { //black pawn promotion
                    chessBoard[iyd][ixd] = "q";
                }
            } else if ((Math.abs(ixo - ixd) > 1)
                    && // castling 
                    chessBoard[iyo][ixo].toUpperCase().equals("K")) {
                if (ixd == 6) { //king castling
                    chessBoard[iyd][5] = chessBoard[iyd][7];
                    chessBoard[iyd][7] = " ";
                } else if (ixd == 2) { // queen castling
                    chessBoard[iyd][3] = chessBoard[iyd][0];
                    chessBoard[iyd][0] = " ";
                }
            }
            if (chessBoard[iyo][ixo].equals("K")) { // disables white castling
                bc.CWK = false;
                bc.CWQ = false;
            }
            if (chessBoard[iyo][ixo].equals("k")) { // disables black castling
                bc.CBK = false;
                bc.CBQ = false;
            }
            if (chessBoard[iyo][ixo].equals("R")) { // disables the white castling if rook is moving
                if (iyo == 7) {
                    bc.CWK = false;
                }
                if (iyo == 0) {
                    bc.CWQ = false;
                }
            }
            if (chessBoard[iyo][ixo].equals("r")) { // disables the black castling if rook is moving
                if (iyo == 7) {
                    bc.CBK = false;
                }
                if (iyo == 0) {
                    bc.CBQ = false;
                }
            }
            chessBoard[iyd][ixd] = chessBoard[iyo][ixo];
        } else { //promotion
            chessBoard[iyd][ixd] = (chessBoard[iyo][ixo].equals("P") ? movAtual.substring(4).toUpperCase() : movAtual.substring(4));
        }
        chessBoard[iyo][ixo] = " ";
        arrayToBitboards();
        bits.CBK = bc.CBK;
        bits.CWK = bc.CWK;
        bits.CBQ = bc.CBQ;
        bits.CWQ = bc.CWQ;
        bits.EP = bc.EP;
        bits.WhiteToMove = !bc.WhiteToMove;
        assignsPlayer(bits.WhiteToMove);
    }

    //make move using PGN format
    public boolean makeMovePGN(PGNMove m) {
        if (!m.isEndGameMarked()) {
            Bits bc = bits.Clone();
            if (m.isCastle()) { // king and queen side castling
                if (m.isKingSideCastle()) {
                    if (m.getColor() == ChessColor.WHITE) {
                        makeSingleMove(7, 7, 5, 7);
                        makeSingleMove(4, 7, 6, 7);
                        bc.CWK = false;
                        bc.CWQ = false;
                    } else {
                        makeSingleMove(7, 0, 5, 0);
                        makeSingleMove(4, 0, 6, 0);
                        bc.CBK = false;
                        bc.CBQ = false;
                    }
                } else if (m.isQueenSideCastle()) {
                    if (m.getColor() == ChessColor.WHITE) {
                        makeSingleMove(0, 7, 3, 7);
                        makeSingleMove(4, 7, 2, 7);
                        bc.CWK = false;
                        bc.CWQ = false;
                    } else {
                        makeSingleMove(0, 0, 3, 0);
                        makeSingleMove(4, 0, 2, 0);
                        bc.CBK = false;
                        bc.CBQ = false;
                    }
                }
            } else {
                int fromFile, fromRank, toFile, toRank;
                if (m.getFromSquare() == null || m.getToSquare() == null) {
                    return false;
                }
                fromFile = m.getFromSquare().charAt(0) - 'a';
                fromRank = '8' - m.getFromSquare().charAt(1);
                toFile = m.getToSquare().charAt(0) - 'a';
                toRank = '8' - m.getToSquare().charAt(1);
                bc.EP = 0;
                if (m.isEnpassant() && !m.isEnpassantCapture()) { //enables en-passant
                    int enpFile = m.getEnpassantPieceSquare().charAt(0) - 'a';
                    bc.EP = FileMasks8[enpFile];
                }
                if (m.isEnpassantCapture()) { // en-passant capture
                    makeSingleMove(fromFile, fromRank, toFile, toRank); //moves the pawn
                    int enpFile = m.getEnpassantPieceSquare().charAt(0) - 'a';
                    int enpRank = '8' - m.getEnpassantPieceSquare().charAt(1);
                    makeSingleMove(enpFile, enpRank, enpFile, enpRank); // erases the captured pawn
                } else if (m.isPromoted()) { //pawn promoting
                    String piece = (m.getColor() == ChessColor.WHITE ? m.getPromotion() : m.getPromotion().toLowerCase());
                    makeSingleMove(fromFile, fromRank, fromFile, fromRank); // erases the promoted pawn
                    chessBoard[toRank][toFile] = piece; // replaces with the promotion piece
                } else { // normal move
                    if (m.getPiece().equals("K") && m.getColor() == ChessColor.WHITE) { // kings move disables castling
                        bc.CWK = false;
                        bc.CWQ = false;
                    } else if (m.getPiece().equals("K") && m.getColor() == ChessColor.BLACK) {
                        bc.CBK = false;
                        bc.CBQ = false;
                    }
                    if (m.getPiece().equals("R") && m.getColor() == ChessColor.WHITE) { // rook move disables castling
                        bc.CWK = (fromFile == 7 && fromRank == 7) ? false : bc.CWK;
                        bc.CWQ = (fromFile == 0 && fromRank == 7) ? false : bc.CWQ;
                    } else if (m.getPiece().equals("R") && m.getColor() == ChessColor.BLACK) {
                        bc.CBK = (fromFile == 7 && fromRank == 0) ? false : bc.CBK;
                        bc.CBQ = (fromFile == 0 && fromRank == 0) ? false : bc.CBQ;
                    }
                    // captured rook disables castling
                    if (m.isCaptured() && chessBoard[toRank][toFile].equalsIgnoreCase("R")) {
                        bc.CWK = (toFile == 7 && toRank == 7) ? false : bc.CWK;
                        bc.CWQ = (toFile == 0 && toRank == 7) ? false : bc.CWQ;
                        bc.CBK = (toFile == 7 && toRank == 0) ? false : bc.CBK;
                        bc.CBQ = (toFile == 0 && toRank == 0) ? false : bc.CBQ;
                    }
                    makeSingleMove(fromFile, fromRank, toFile, toRank);
                }
            }
            //printChessBoard(m.getMove());
            arrayToBitboards();
            bits.CBK = bc.CBK;
            bits.CWK = bc.CWK;
            bits.CBQ = bc.CBQ;
            bits.CWQ = bc.CWQ;
            bits.EP = bc.EP;
            bits.WhiteToMove = !bc.WhiteToMove;
            assignsPlayer(bits.WhiteToMove);
            return true;
        } else {
            return false;
        }
    }

    // returns true if valid
    public boolean makeMove(String move, char peca) { 
        int start;
        boolean isValid;
        if (Character.isDigit(move.charAt(3))) {//'regular' move
            start = (Character.getNumericValue(move.charAt(0)) * 8) + (Character.getNumericValue(move.charAt(1)));
            if (((1L << start) & bits.WK) != 0) {
                bits.CWK = false;
                bits.CWQ = false;
            } else if (((1L << start) & bits.BK) != 0) {
                bits.CBK = false;
                bits.CBQ = false;
            } else if (((1L << start) & bits.WR & (1L << 63)) != 0) {
                bits.CWK = false;
            } else if (((1L << start) & bits.WR & (1L << 56)) != 0) {
                bits.CWQ = false;
            } else if (((1L << start) & bits.BR & (1L << 7)) != 0) {
                bits.CBK = false;
            } else if (((1L << start) & bits.BR & 1L) != 0) {
                bits.CBQ = false;
            }
        }

        bits.EP = makeMoveEP(bits.WP | bits.BP, move.substring(0, 4));
        bits.WR = makeMoveCastle(bits.WR, bits.WK | bits.BK, move.substring(0, 4), 'R');
        bits.BR = makeMoveCastle(bits.BR, bits.WK | bits.BK, move.substring(0, 4), 'r');
        bits.WP = makeMove(bits.WP, move.substring(0, 4), 'P');
        bits.WN = makeMove(bits.WN, move.substring(0, 4), 'N');
        bits.WB = makeMove(bits.WB, move.substring(0, 4), 'B');
        bits.WR = makeMove(bits.WR, move.substring(0, 4), 'R');
        bits.WQ = makeMove(bits.WQ, move.substring(0, 4), 'Q');
        bits.WK = makeMove(bits.WK, move.substring(0, 4), 'K');
        bits.BP = makeMove(bits.BP, move.substring(0, 4), 'p');
        bits.BN = makeMove(bits.BN, move.substring(0, 4), 'n');
        bits.BB = makeMove(bits.BB, move.substring(0, 4), 'b');
        bits.BR = makeMove(bits.BR, move.substring(0, 4), 'r');
        bits.BQ = makeMove(bits.BQ, move.substring(0, 4), 'q');
        bits.BK = makeMove(bits.BK, move.substring(0, 4), 'k');
        //check if the king is threaten
        if ("KQRNBP".indexOf(peca) > 0) { // white moved
            isValid = ((bits.WK & unsafeForWhite()) == 0);
        } else {
            isValid = ((bits.BK & unsafeForBlack()) == 0);
        }
        if (isValid) {
            bits.WhiteToMove = !bits.WhiteToMove;
            bitsToArray(bits); //adjusts the board according to bitboard
        }
        return isValid;
    }

    public long makeMove(long board, String move, char type) {
        if (Character.isDigit(move.charAt(3))) {//'regular' move
            int start = (Character.getNumericValue(move.charAt(0)) * 8) + (Character.getNumericValue(move.charAt(1)));
            int end = (Character.getNumericValue(move.charAt(2)) * 8) + (Character.getNumericValue(move.charAt(3)));
            if (((board >>> start) & 1) == 1) {
                board &= ~(1L << start);
                board |= (1L << end);
            } else {
                board &= ~(1L << end);
            }
        } else if (move.charAt(3) == 'P') {//pawn promotion
            int start, end;
            if (Character.isUpperCase(move.charAt(2))) {
                start = Long.numberOfTrailingZeros(FileMasks8[move.charAt(0) - '0'] & RankMasks8[1]);
                end = Long.numberOfTrailingZeros(FileMasks8[move.charAt(1) - '0'] & RankMasks8[0]);
            } else {
                start = Long.numberOfTrailingZeros(FileMasks8[move.charAt(0) - '0'] & RankMasks8[6]);
                end = Long.numberOfTrailingZeros(FileMasks8[move.charAt(1) - '0'] & RankMasks8[7]);
            }
            if (type == move.charAt(2)) {
                board |= (1L << end);
            } else {
                board &= ~(1L << start);
                board &= ~(1L << end);
            }
        } else if (move.charAt(3) == 'E') {//en passant
            int start, end;
            if (move.charAt(2) == 'W') {
                start = Long.numberOfTrailingZeros(FileMasks8[move.charAt(0) - '0'] & RankMasks8[3]);
                end = Long.numberOfTrailingZeros(FileMasks8[move.charAt(1) - '0'] & RankMasks8[2]);
                board &= ~(FileMasks8[move.charAt(1) - '0'] & RankMasks8[3]);
            } else {
                start = Long.numberOfTrailingZeros(FileMasks8[move.charAt(0) - '0'] & RankMasks8[4]);
                end = Long.numberOfTrailingZeros(FileMasks8[move.charAt(1) - '0'] & RankMasks8[5]);
                board &= ~(FileMasks8[move.charAt(1) - '0'] & RankMasks8[4]);
            }
            if (((board >>> start) & 1) == 1) {
                board &= ~(1L << start);
                board |= (1L << end);
            }
        } else {
            System.out.print("ERROR: Invalid move type");
        }
        return board;
    }

    public long makeMoveCastle(long rookBoard, long kingBoard, String move, char type) {
        int start = (Character.getNumericValue(move.charAt(0)) * 8) + (Character.getNumericValue(move.charAt(1)));
        if ((((kingBoard >>> start) & 1) == 1) && (("0402".equals(move)) || ("0406".equals(move)) || ("7472".equals(move)) || ("7476".equals(move)))) {//'regular' move
            if (type == 'R') {
                switch (move) {
                    case "7472":
                        rookBoard &= ~(1L << CASTLE_ROOKS[1]);
                        rookBoard |= (1L << (CASTLE_ROOKS[1] + 3));
                        break;
                    case "7476":
                        rookBoard &= ~(1L << CASTLE_ROOKS[0]);
                        rookBoard |= (1L << (CASTLE_ROOKS[0] - 2));
                        break;
                }
            } else {
                switch (move) {
                    case "0402":
                        rookBoard &= ~(1L << CASTLE_ROOKS[3]);
                        rookBoard |= (1L << (CASTLE_ROOKS[3] + 3));
                        break;
                    case "0406":
                        rookBoard &= ~(1L << CASTLE_ROOKS[2]);
                        rookBoard |= (1L << (CASTLE_ROOKS[2] - 2));
                        break;
                }
            }
        }
        return rookBoard;
    }

    public long makeMoveEP(long board, String move) {
        if (Character.isDigit(move.charAt(3))) {
            int start = (Character.getNumericValue(move.charAt(0)) * 8) + (Character.getNumericValue(move.charAt(1)));
            if ((Math.abs(move.charAt(0) - move.charAt(2)) == 2) && (((board >>> start) & 1) == 1)) {//pawn double push
                return FileMasks8[move.charAt(1) - '0'];
            }
        }
        return 0;
    }

    public void makePatternMoves(Pattern pattern, HashMap<String, PatternVertex> vertices, int ip) {
        ArrayList<String> movtosOrig = pattern.getMoves();
        pattern.getTacticalMoves().clear();
        movtosOrig.forEach((ms) -> {
            String[] m = new String[2];
            /*
                Adjusts the square in the vertexes structure of the pattern to evaluate the post-conditions
            */
            String var = ms.split(",")[0];
            String key = "I_" + ip + "_" + var;
            PatternVertex v1 = vertices.get(key);
            m[0] = v1.getSquare().toString();
            var = ms.split(",")[1];
            key = "I_" + ip + "_" + var;
            PatternVertex v2 = vertices.get(key);
            m[1] = v2.getSquare().toString();
            pattern.getTacticalMoves().add(new Ply(v1.getPiece(), v1.getSquare(), v2.getSquare()));

            makeMoveBoard(m[0] + m[1]);
            //adjust the square in the vertices structure of the HLP to evaluate post-conditions
            v1.resetSquare(v2.getSquare());
        });
    }

    public void undoPatternMoves(ArrayList<String> movimentos, ArrayList<String> movtosOrig, HashMap<String, PatternVertex> vertices, int ip) {
        int mx = 0;
        for (String ms : movimentos) {
            //restores the original square of the vertices to continue evaluating another found instances of the pattern
            String var = movtosOrig.get(mx).split(",")[0];
            String key = "I_" + ip + "_" + var;
            PatternVertex v = vertices.get(key);
            v.restoreSquare();
            mx++;
        }
    }

    //</editor-fold>
    
    //<editor-fold desc="Possible moves - returns long">
    public void initWhite() {
        bits.BLACK_PIECES = bits.BP | bits.BN | bits.BB | bits.BR | bits.BQ | bits.BK;
        bits.WHITE_PIECES = bits.WP | bits.WN | bits.WB | bits.WR | bits.WQ | bits.WK;
        bits.NOT_MY_PIECES = ~(bits.WP | bits.WN | bits.WB | bits.WR | bits.WQ | bits.WK);
        bits.MY_PIECES = bits.WP | bits.WN | bits.WB | bits.WR | bits.WQ | bits.WK;
        bits.YOUR_PIECES = bits.BP | bits.BN | bits.BB | bits.BR | bits.BQ | bits.BK;
        bits.OCCUPIED = bits.WP | bits.WN | bits.WB | bits.WR | bits.WQ | bits.WK | bits.BP | bits.BN | bits.BB | bits.BR | bits.BQ | bits.BK;
        bits.EMPTY = ~bits.OCCUPIED;
    }

    public void initBlack() {
        bits.BLACK_PIECES = bits.BP | bits.BN | bits.BB | bits.BR | bits.BQ | bits.BK;
        bits.WHITE_PIECES = bits.WP | bits.WN | bits.WB | bits.WR | bits.WQ | bits.WK;
        bits.NOT_MY_PIECES = ~(bits.BP | bits.BN | bits.BB | bits.BR | bits.BQ | bits.BK);
        bits.MY_PIECES = bits.BP | bits.BN | bits.BB | bits.BR | bits.BQ | bits.BK;
        bits.YOUR_PIECES = bits.WP | bits.WN | bits.WB | bits.WR | bits.WQ | bits.WK;
        bits.OCCUPIED = bits.WP | bits.WN | bits.WB | bits.WR | bits.WQ | bits.WK | bits.BP | bits.BN | bits.BB | bits.BR | bits.BQ | bits.BK;
        bits.EMPTY = ~bits.OCCUPIED;
    }

    public long possibleNLong(long N) {
        long list = 0;
        long i = N & ~(N - 1);
        long possibility;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            if (iLocation > 18) {
                possibility = KNIGHT_SPAN << (iLocation - 18);
            } else {
                possibility = KNIGHT_SPAN >> (18 - iLocation);
            }
            if (iLocation % COLUMNS < 4) {
                possibility &= ~FILE_GH & bits.NOT_MY_PIECES;
            } else {
                possibility &= ~FILE_AB & bits.NOT_MY_PIECES;
            }
            list = list | possibility;
            N &= ~i;
            i = N & ~(N - 1);
        }
        return list;
    }

    public long possibleNSecureLong(long INSEGURO, long N) {
        return possibleNLong(N) & ~INSEGURO;
    }

    public long possibleBLong(long B) {
        long i = B & ~(B - 1);
        long result = 0;
        long possibility;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = DAndAntiDMoves(iLocation) & bits.NOT_MY_PIECES;
            result = result | possibility;
            B &= ~i;
            i = B & ~(B - 1);
        }
        return result;
    }

    public long possibleBSecureLong(long INSEGURO, long B) {
        return possibleBLong(B) & ~INSEGURO;
    }

    public long possibleRLong(long R) {
        long list = 0;
        long i = R & ~(R - 1);
        long possibility;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = HAndVMoves(iLocation) & bits.NOT_MY_PIECES;
            list = list | possibility;
            R &= ~i;
            i = R & ~(R - 1);
        }
        return list;
    }

    public long possibleRSecureLong(long INSEGURO, long R) {
        return possibleRLong(R) & ~INSEGURO;
    }

    public long possibleQLong(long Q) {
        long list = 0;
        long i = Q & ~(Q - 1);
        long possibility;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = (HAndVMoves(iLocation) | DAndAntiDMoves(iLocation)) & bits.NOT_MY_PIECES;
            list = list | possibility;
            Q &= ~i;
            i = Q & ~(Q - 1);
        }
        return list;
    }

    public long possibleQSecureLong(long INSEGURO, long Q) {
        return possibleQLong(Q) & ~INSEGURO;
    }

    public long possibleKLong(long K) {
        long possibility;
        int iLocation = Long.numberOfTrailingZeros(K);
        if (iLocation > 9) {
            possibility = KING_SPAN << (iLocation - 9);
        } else {
            possibility = KING_SPAN >> (9 - iLocation);
        }
        if (iLocation % COLUMNS < 4) {
            possibility &= ~FILE_GH & bits.NOT_MY_PIECES;
        } else {
            possibility &= ~FILE_AB & bits.NOT_MY_PIECES;
        }
        return possibility;
    }

    public long possibleKSecureLong(long INSEGURO, long K) {
        long possibility;
        possibility = possibleKLong(K) & ~INSEGURO;
        return possibility;
    }

    public long possibleWPLong(long WP, long BP, long EP) {
        long list = 0;
        //x1,y1,x2,y2
        long PAWN_MOVES = (WP >> 7) & bits.NOT_MY_PIECES & bits.OCCUPIED & ~RANK_8 & ~FILE_A;//capture right
        //long possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;

        PAWN_MOVES = (WP >> 9) & bits.NOT_MY_PIECES & bits.OCCUPIED & ~RANK_8 & ~FILE_H;//capture left
        //possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;

        PAWN_MOVES = (WP >> 8) & bits.EMPTY & ~RANK_8;//move 1 forward
        //possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;

        PAWN_MOVES = (WP >> 16) & bits.EMPTY & (bits.EMPTY >> 8) & RANK_4;//move 2 forward
        //possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;

        //y1,y2,Promotion Type,"P"
        PAWN_MOVES = (WP >> 7) & bits.NOT_MY_PIECES & bits.OCCUPIED & RANK_8 & ~FILE_A;//pawn promotion by capture right
        //possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;

        PAWN_MOVES = (WP >> 9) & bits.NOT_MY_PIECES & bits.OCCUPIED & RANK_8 & ~FILE_H;//pawn promotion by capture left
        //possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;

        PAWN_MOVES = (WP >> 8) & bits.EMPTY & RANK_8;//pawn promotion by move 1 forward
        //possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;

        //y1,y2,"WE"
        //en passant right
        long possibility = (WP << 1) & BP & RANK_5 & ~FILE_A & EP;//shows piece to remove, not the destination
        list = list | possibility;

        //en passant left
        possibility = (WP >> 1) & BP & RANK_5 & ~FILE_H & EP;//shows piece to remove, not the destination
        list = list | possibility;
        return list;
    }

    public long possibleWPSecureLong(long INSEGURO, long WP, long BP, long EP) {
        return possibleWPLong(WP, BP, EP) & ~INSEGURO;
    }

    public long possibleBPLong(long BP, long WP, long EP) {
        long list = 0;
        //x1,y1,x2,y2
        long PAWN_MOVES = (BP << 7) & bits.NOT_MY_PIECES & bits.OCCUPIED & ~RANK_1 & ~FILE_H;//capture right
        //long possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;
        PAWN_MOVES = (BP << 9) & bits.NOT_MY_PIECES & bits.OCCUPIED & ~RANK_1 & ~FILE_A;//capture left
        //possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;
        PAWN_MOVES = (BP << 8) & bits.EMPTY & ~RANK_1;//move 1 forward
        //possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;
        PAWN_MOVES = (BP << 16) & bits.EMPTY & (bits.EMPTY << 8) & RANK_5;//move 2 forward
        //possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;
        PAWN_MOVES = (BP << 7) & bits.NOT_MY_PIECES & bits.OCCUPIED & RANK_1 & ~FILE_H;//pawn promotion by capture right
        //possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;
        PAWN_MOVES = (BP << 9) & bits.NOT_MY_PIECES & bits.OCCUPIED & RANK_1 & ~FILE_A;//pawn promotion by capture left
        //possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;
        PAWN_MOVES = (BP << 8) & bits.EMPTY & RANK_1;//pawn promotion by move 1 forward
        //possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        list = list | PAWN_MOVES;
        //y1,y2,"BE"
        //en passant right
        long possibility = (BP >> 1) & WP & RANK_4 & ~FILE_H & EP;//shows piece to remove, not the destination
        list = list | possibility;
        //en passant left
        possibility = (BP << 1) & WP & RANK_4 & ~FILE_A & EP;//shows piece to remove, not the destination
        list = list | possibility;
        return list;
    }

    public long possibleBPSecureLong(long INSEGURO, long BP, long WP, long EP) {
        return possibleBPLong(BP, WP, EP) & ~INSEGURO;
    }
    //</editor-fold>

    //<editor-fold desc="Possible moves - returns String">
    public String possibleMovesW(Bits bits) {
        bits.NOT_MY_PIECES = ~(bits.WP | bits.WN | bits.WB | bits.WR | bits.WQ | bits.WK | bits.BK);//added BK to avoid illegal capture
        bits.MY_PIECES = bits.WP | bits.WN | bits.WB | bits.WR | bits.WQ;//omitted WK to avoid illegal capture
        bits.OCCUPIED = bits.WP | bits.WN | bits.WB | bits.WR | bits.WQ | bits.WK | bits.BP | bits.BN | bits.BB | bits.BR | bits.BQ | bits.BK;
        bits.EMPTY = ~bits.OCCUPIED;
        String list = possibleWP(bits.WP, bits.BP, bits.EP)
                + possibleN(bits.OCCUPIED, bits.WN)
                + possibleB(bits.OCCUPIED, bits.WB)
                + possibleR(bits.OCCUPIED, bits.WR)
                + possibleQ(bits.OCCUPIED, bits.WQ)
                + possibleK(bits.OCCUPIED, bits.WK)
                + possibleCW(bits.WR, bits.CWK, bits.CWQ);
        return list;
    }

    public String possibleMovesB(Bits bits) {
        bits.NOT_MY_PIECES = ~(bits.BP | bits.BN | bits.BB | bits.BR | bits.BQ | bits.BK | bits.WK);//added WK to avoid illegal capture
        bits.MY_PIECES = bits.BP | bits.BN | bits.BB | bits.BR | bits.BQ;//omitted BK to avoid illegal capture
        bits.OCCUPIED = bits.WP | bits.WN | bits.WB | bits.WR | bits.WQ | bits.WK | bits.BP | bits.BN | bits.BB | bits.BR | bits.BQ | bits.BK;
        bits.EMPTY = ~bits.OCCUPIED;
        String list = possibleBP(bits.BP, bits.WP, bits.EP)
                + possibleN(bits.OCCUPIED, bits.BN)
                + possibleB(bits.OCCUPIED, bits.BB)
                + possibleR(bits.OCCUPIED, bits.BR)
                + possibleQ(bits.OCCUPIED, bits.BQ)
                + possibleK(bits.OCCUPIED, bits.BK)
                + possibleCB(bits.BR, bits.CBK, bits.CBQ);
        return list;
    }

    public String possibleWP(long WP, long BP, long EP) {
        String list = "";
        //x1,y1,x2,y2
        long PAWN_MOVES = (WP >> 7) & bits.NOT_MY_PIECES & bits.OCCUPIED & ~RANK_8 & ~FILE_A;//capture right
        long possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index / LINES + 1) + (index % COLUMNS - 1)
                    + (index / LINES) + (index % COLUMNS);
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        PAWN_MOVES = (WP >> 9) & bits.NOT_MY_PIECES & bits.OCCUPIED & ~RANK_8 & ~FILE_H;//capture left
        possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index / LINES + 1) + (index % COLUMNS + 1)
                    + (index / LINES) + (index % COLUMNS);
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        PAWN_MOVES = (WP >> 8) & bits.EMPTY & ~RANK_8;//move 1 forward
        possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index / LINES + 1) + (index % COLUMNS)
                    + (index / LINES) + (index % COLUMNS);
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        PAWN_MOVES = (WP >> 16) & bits.EMPTY & (bits.EMPTY >> 8) & RANK_4;//move 2 forward
        possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index / LINES + 2) + (index % COLUMNS)
                    + (index / LINES) + (index % COLUMNS);
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        //y1,y2,Promotion Type,"P"
        PAWN_MOVES = (WP >> 7) & bits.NOT_MY_PIECES & bits.OCCUPIED & RANK_8 & ~FILE_A;//pawn promotion by capture right
        possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index % COLUMNS - 1) + (index % COLUMNS) + "QP"
                    + (index % COLUMNS - 1) + (index % COLUMNS) + "RP"
                    + (index % COLUMNS - 1) + (index % COLUMNS) + "BP"
                    + (index % COLUMNS - 1) + (index % COLUMNS) + "NP";
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        PAWN_MOVES = (WP >> 9) & bits.NOT_MY_PIECES & bits.OCCUPIED & RANK_8 & ~FILE_H;//pawn promotion by capture left
        possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index % COLUMNS + 1) + (index % COLUMNS) + "QP"
                    + (index % COLUMNS + 1) + (index % COLUMNS) + "RP"
                    + (index % COLUMNS + 1) + (index % COLUMNS) + "BP"
                    + (index % COLUMNS + 1) + (index % COLUMNS) + "NP";
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        PAWN_MOVES = (WP >> 8) & bits.EMPTY & RANK_8;//pawn promotion by move 1 forward
        possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index % COLUMNS) + (index % COLUMNS) + "QP"
                    + (index % COLUMNS) + (index % COLUMNS) + "RP"
                    + (index % COLUMNS) + (index % COLUMNS) + "BP"
                    + (index % COLUMNS) + (index % COLUMNS) + "NP";
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        //y1,y2,"WE"
        //en passant right
        possibility = (WP << 1) & BP & RANK_5 & ~FILE_A & EP;//shows piece to remove, not the destination
        if (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index % COLUMNS - 1) + (index % COLUMNS) + "WE";
        }
        //en passant left
        possibility = (WP >> 1) & BP & RANK_5 & ~FILE_H & EP;//shows piece to remove, not the destination
        if (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index % COLUMNS + 1) + (index % COLUMNS) + "WE";
        }
        return list;
    }

    public String possibleBP(long BP, long WP, long EP) {
        String list = "";
        //x1,y1,x2,y2
        long PAWN_MOVES = (BP << 7) & bits.NOT_MY_PIECES & bits.OCCUPIED & ~RANK_1 & ~FILE_H;//capture right
        long possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index / LINES - 1) + (index % COLUMNS + 1)
                    + (index / LINES) + (index % COLUMNS);
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        PAWN_MOVES = (BP << 9) & bits.NOT_MY_PIECES & bits.OCCUPIED & ~RANK_1 & ~FILE_A;//capture left
        possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index / LINES - 1) + (index % COLUMNS - 1)
                    + (index / LINES) + (index % COLUMNS);
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        PAWN_MOVES = (BP << 8) & bits.EMPTY & ~RANK_1;//move 1 forward
        possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index / LINES - 1) + (index % COLUMNS)
                    + (index / LINES) + (index % COLUMNS);
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        PAWN_MOVES = (BP << 16) & bits.EMPTY & (bits.EMPTY << 8) & RANK_5;//move 2 forward
        possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index / LINES - 2) + (index % COLUMNS)
                    + (index / LINES) + (index % COLUMNS);
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        //y1,y2,Promotion Type,"P"
        PAWN_MOVES = (BP << 7) & bits.NOT_MY_PIECES & bits.OCCUPIED & RANK_1 & ~FILE_H;//pawn promotion by capture right
        possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index % COLUMNS + 1) + (index % COLUMNS) + "qP"
                    + (index % COLUMNS + 1) + (index % COLUMNS) + "rP"
                    + (index % COLUMNS + 1) + (index % COLUMNS) + "bP"
                    + (index % COLUMNS + 1) + (index % COLUMNS) + "nP";
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        PAWN_MOVES = (BP << 9) & bits.NOT_MY_PIECES & bits.OCCUPIED & RANK_1 & ~FILE_A;//pawn promotion by capture left
        possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index % COLUMNS - 1) + (index % COLUMNS) + "qP"
                    + (index % COLUMNS - 1) + (index % COLUMNS) + "rP"
                    + (index % COLUMNS - 1) + (index % COLUMNS) + "bP"
                    + (index % COLUMNS - 1) + (index % COLUMNS) + "nP";
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        PAWN_MOVES = (BP << 8) & bits.EMPTY & RANK_1;//pawn promotion by move 1 forward
        possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        while (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index % COLUMNS) + (index % COLUMNS) + "qP"
                    + (index % COLUMNS) + (index % COLUMNS) + "rP"
                    + (index % COLUMNS) + (index % COLUMNS) + "bP"
                    + (index % COLUMNS) + (index % COLUMNS) + "nP";
            PAWN_MOVES &= ~possibility;
            possibility = PAWN_MOVES & ~(PAWN_MOVES - 1);
        }
        //y1,y2,"BE"
        //en passant right
        possibility = (BP >> 1) & WP & RANK_4 & ~FILE_H & EP;//shows piece to remove, not the destination
        if (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index % COLUMNS + 1) + (index % COLUMNS) + "BE";
        }
        //en passant left
        possibility = (BP << 1) & WP & RANK_4 & ~FILE_A & EP;//shows piece to remove, not the destination
        if (possibility != 0) {
            int index = Long.numberOfTrailingZeros(possibility);
            list += "" + (index % COLUMNS - 1) + (index % COLUMNS) + "BE";
        }
        return list;
    }

    public String possibleN(long OCCUPIED, long N) {
        String list = "";
        long i = N & ~(N - 1);
        long possibility;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            if (iLocation > 18) {
                possibility = KNIGHT_SPAN << (iLocation - 18);
            } else {
                possibility = KNIGHT_SPAN >> (18 - iLocation);
            }
            if (iLocation % COLUMNS < 4) {
                possibility &= ~FILE_GH & bits.NOT_MY_PIECES;
            } else {
                possibility &= ~FILE_AB & bits.NOT_MY_PIECES;
            }
            long j = possibility & ~(possibility - 1);
            while (j != 0) {
                int index = Long.numberOfTrailingZeros(j);
                list += "" + (iLocation / LINES) + (iLocation % COLUMNS)
                        + (index / LINES) + (index % COLUMNS);
                possibility &= ~j;
                j = possibility & ~(possibility - 1);
            }
            N &= ~i;
            i = N & ~(N - 1);
        }
        return list;
    }

    public String possibleB(long OCCUPIED, long B) {
        String list = "";
        long i = B & ~(B - 1);
        long possibility;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = DAndAntiDMoves(iLocation) & bits.NOT_MY_PIECES;
            long j = possibility & ~(possibility - 1);
            while (j != 0) {
                int index = Long.numberOfTrailingZeros(j);
                list += "" + (iLocation / LINES) + (iLocation % COLUMNS)
                        + (index / LINES) + (index % COLUMNS);
                possibility &= ~j;
                j = possibility & ~(possibility - 1);
            }
            B &= ~i;
            i = B & ~(B - 1);
        }
        return list;
    }

    public String possibleR(long OCCUPIED, long R) {
        String list = "";
        long i = R & ~(R - 1);
        long possibility;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = HAndVMoves(iLocation) & bits.NOT_MY_PIECES;
            long j = possibility & ~(possibility - 1);
            while (j != 0) {
                int index = Long.numberOfTrailingZeros(j);
                list += "" + (iLocation / LINES) + (iLocation % COLUMNS)
                        + (index / LINES) + (index % COLUMNS);
                possibility &= ~j;
                j = possibility & ~(possibility - 1);
            }
            R &= ~i;
            i = R & ~(R - 1);
        }
        return list;
    }

    public String possibleQ(long OCCUPIED, long Q) {
        String list = "";
        long i = Q & ~(Q - 1);
        long possibility;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = (HAndVMoves(iLocation) | DAndAntiDMoves(iLocation)) & bits.NOT_MY_PIECES;
            long j = possibility & ~(possibility - 1);
            while (j != 0) {
                int index = Long.numberOfTrailingZeros(j);
                list += "" + (iLocation / LINES) + (iLocation % COLUMNS)
                        + (index / LINES) + (index % COLUMNS);
                possibility &= ~j;
                j = possibility & ~(possibility - 1);
            }
            Q &= ~i;
            i = Q & ~(Q - 1);
        }
        return list;
    }

    public String possibleK(long OCCUPIED, long K) {
        String list = "";
        long possibility;
        int iLocation = Long.numberOfTrailingZeros(K);
        if (iLocation > 9) {
            possibility = KING_SPAN << (iLocation - 9);
        } else {
            possibility = KING_SPAN >> (9 - iLocation);
        }
        if (iLocation % COLUMNS < 4) {
            possibility &= ~FILE_GH & bits.NOT_MY_PIECES;
        } else {
            possibility &= ~FILE_AB & bits.NOT_MY_PIECES;
        }
        long j = possibility & ~(possibility - 1);
        while (j != 0) {
            int index = Long.numberOfTrailingZeros(j);
            list += "" + (iLocation / LINES) + (iLocation % COLUMNS)
                    + (index / LINES) + (index % COLUMNS);
            possibility &= ~j;
            j = possibility & ~(possibility - 1);
        }
        return list;
    }

    public String possibleKSecure(long INSEGURO, long K) {
        String list = "";
        long possibility;
        int iLocation = Long.numberOfTrailingZeros(K);
        if (iLocation > 9) {
            possibility = KING_SPAN << (iLocation - 9);
        } else {
            possibility = KING_SPAN >> (9 - iLocation);
        }
        if (iLocation % COLUMNS < 4) {
            possibility &= ~FILE_GH & bits.NOT_MY_PIECES;
        } else {
            possibility &= ~FILE_AB & bits.NOT_MY_PIECES;
        }
        possibility &= ~INSEGURO;
        long j = possibility & ~(possibility - 1);
        while (j != 0) {
            int index = Long.numberOfTrailingZeros(j);
            list += "" + (iLocation / LINES) + (iLocation % COLUMNS)
                    + (index / LINES) + (index % COLUMNS);
            possibility &= ~j;
            j = possibility & ~(possibility - 1);
        }
        return list;
    }

    public String possibleCW(long WR, boolean CWK, boolean CWQ) {
        String list = "";
        long UNSAFE = unsafeForWhite();
        if (CWK && (((1L << CASTLE_ROOKS[0]) & WR) != 0)) {
            if (((bits.OCCUPIED | UNSAFE) & ((1L << 61) | (1L << 62))) == 0) {
                list += "7476";
            }
        }
        if (CWQ && (((1L << CASTLE_ROOKS[1]) & WR) != 0)) {
            if (((bits.OCCUPIED | UNSAFE) & ((1L << 57) | (1L << 58) | (1L << 59))) == 0) {
                list += "7472";
            }
        }
        return list;
    }

    public String possibleCB(long BR, boolean CBK, boolean CBQ) {
        String list = "";
        long UNSAFE = unsafeForBlack();
        if (CBK && (((1L << CASTLE_ROOKS[2]) & BR) != 0)) {
            if (((bits.OCCUPIED | UNSAFE) & ((1L << 5) | (1L << 6))) == 0) {
                list += "0406";
            }
        }
        if (CBQ && (((1L << CASTLE_ROOKS[3]) & BR) != 0)) {
            if (((bits.OCCUPIED | UNSAFE) & ((1L << 1) | (1L << 2) | (1L << 3))) == 0) {
                list += "0402";
            }
        }
        return list;
    }

    public String moveToAlgebra(String move) {
        String append = "";
        int start = 0, end = 0;
        if (Character.isDigit(move.charAt(3))) {//'regular' move
            start = (Character.getNumericValue(move.charAt(0)) * 8) + (Character.getNumericValue(move.charAt(1)));
            end = (Character.getNumericValue(move.charAt(2)) * 8) + (Character.getNumericValue(move.charAt(3)));
        } else if (move.charAt(3) == 'P') {//pawn promotion
            if (Character.isUpperCase(move.charAt(2))) {
                start = Long.numberOfTrailingZeros(FileMasks8[move.charAt(0) - '0'] & RankMasks8[1]);
                end = Long.numberOfTrailingZeros(FileMasks8[move.charAt(1) - '0'] & RankMasks8[0]);
            } else {
                start = Long.numberOfTrailingZeros(FileMasks8[move.charAt(0) - '0'] & RankMasks8[6]);
                end = Long.numberOfTrailingZeros(FileMasks8[move.charAt(1) - '0'] & RankMasks8[7]);
            }
            append = "" + Character.toLowerCase(move.charAt(2));
        } else if (move.charAt(3) == 'E') {//en passant
            if (move.charAt(2) == 'W') {
                start = Long.numberOfTrailingZeros(FileMasks8[move.charAt(0) - '0'] & RankMasks8[3]);
                end = Long.numberOfTrailingZeros(FileMasks8[move.charAt(1) - '0'] & RankMasks8[2]);
            } else {
                start = Long.numberOfTrailingZeros(FileMasks8[move.charAt(0) - '0'] & RankMasks8[4]);
                end = Long.numberOfTrailingZeros(FileMasks8[move.charAt(1) - '0'] & RankMasks8[5]);
            }
        }
        String returnMove = "";
        returnMove += (char) ('a' + (start % COLUMNS));
        returnMove += (char) ('8' - (start / LINES));
        returnMove += (char) ('a' + (end % COLUMNS));
        returnMove += (char) ('8' - (end / LINES));
        returnMove += append;
        return returnMove;
    }
    //</editor-fold>
    
    //<editor-fold desc="Piece attacks and defenses">
    public String directAttacks(String FEN, int pRow, int pCol) {
        String who = "";
        if ("rnbqkp".contains(FEN)) {
            initBlack();
        } else {
            initWhite();
        }
        int iPos = (7 - pRow) * 8 + pCol;
        long R = 1L << iPos;
        //R = R & bits.NOT_MY_PIECES;
        switch (FEN.toLowerCase().charAt(0)) {
            case 'r':
                who = possibleR(bits.OCCUPIED, R);
                break;
            case 'n':
                who = possibleN(bits.OCCUPIED, R);
                break;
            case 'b':
                who = possibleB(bits.OCCUPIED, R);
                break;
            case 'q':
                who = possibleQ(bits.OCCUPIED, R);
                break;
            case 'k':
                who = possibleK(bits.OCCUPIED, R);
                break;
            case 'p': {
                if (FEN.equals("P")) {
                    who = possibleWP(R, bits.BP, bits.EP);
                } else {
                    who = possibleBP(R, bits.WP, bits.EP);
                }
            }
            break;
        }
        return who;
    }

    public long directAttacksLong(String FEN, int pLin, int pCol) {
        long quem = 0;
        long inseguroRei;
        boolean pecaBranca = Character.isUpperCase(FEN.charAt(0));
        if (pecaBranca) {
            initWhite();
            inseguroRei = unsafeForWhite();
        } else {
            initBlack();
            inseguroRei = unsafeForBlack();
        }
        int iPos = (7 - pLin) * 8 + pCol;
        long R = 1L << iPos;

        //R = R & bits.NOT_MY_PIECES;
        switch (String.valueOf(FEN).toLowerCase().charAt(0)) {
            case 'r':
                quem = possibleRLong(R);
                break;
            case 'n':
                quem = possibleNLong(R);
                break;
            case 'b':
                quem = possibleBLong(R);
                break;
            case 'q':
                quem = possibleQLong(R);
                break;
            case 'k':
                quem = possibleKSecureLong(inseguroRei, R);
                break;
            case 'p': {
                if (FEN.equals("P")) {
                    quem = possibleWPLong(R, bits.BP, bits.EP);
                } else {
                    quem = possibleBPLong(R, bits.WP, bits.EP);
                }
            }
            break;
        }
        return quem;
    }

    public String directDefenses(String FEN, int pLin, int pCol) {
        String quem = "";
        if ("rnbqkp".contains(FEN)) {
            initWhite();
        } else {
            initBlack();
        }
        int iPos = (7 - pLin) * 8 + pCol;
        long R = 1L << iPos;
        //R = R & bits.NOT_MY_PIECES;
        switch (String.valueOf(FEN).toLowerCase().charAt(0)) {
            case 'r':
                quem = possibleR(bits.OCCUPIED, R); // movimentos possíveis
                break;
            case 'n':
                quem = possibleN(bits.OCCUPIED, R);
                break;
            case 'b':
                quem = possibleB(bits.OCCUPIED, R);
                break;
            case 'q':
                quem = possibleQ(bits.OCCUPIED, R);
                break;
            case 'k':
                quem = possibleK(bits.OCCUPIED, R);
                break;
            case 'p': {
                if (FEN.equals("P")) {
                    quem = possibleWP(R, bits.BP, bits.EP);
                } else {
                    quem = possibleBP(R, bits.WP, bits.EP);
                }
            }
            break;
        }
        return quem;
    }

    public long directDefensesLong(String FEN, int pLin, int pCol, boolean brancas) {
        long quem = 0;
        if ("rnbqkp".contains(FEN)) {
            initWhite();
        } else {
            initBlack();
        }
        int iPos = (7 - pLin) * 8 + pCol;
        long R = 1L << iPos;
        //R = R & bits.NOT_MY_PIECES;
        switch (String.valueOf(FEN).toLowerCase().charAt(0)) {
            case 'r':
                quem = possibleRLong(R);
                break;
            case 'n':
                quem = possibleNLong(R);
                break;
            case 'b':
                quem = possibleBLong(R);
                break;
            case 'q':
                quem = possibleQLong(R);
                break;
            case 'k':
                quem = possibleKLong(R);
                break;
            case 'p': {
                if (FEN.equals("P")) {
                    quem = possibleWPLong(R, bits.BP, bits.EP);
                } else {
                    quem = possibleBPLong(R, bits.WP, bits.EP);
                }
            }
            break;
        }
//        System.out.println("----------------------------------");
//        System.out.println("Defesas peça: " + FEN + " - " + pCol + "," + pLin);
        quem &= bits.OCCUPIED;
//        this.drawBitboard(quem);
        return quem;
    }
    //</editor-fold>

    //</editor-fold>
    
    //<editor-fold desc="Unsafe squares">
    public long unsafeFor(int indRow, int indCol, boolean brancas) {
        int indCasa = (7 - indRow) * 8 + indCol;
        assert (indCasa >= 0) && (indCasa < SQUARES);
        if (brancas) {
            return bitMasks[indCasa] & unsafeforW;
        } else {
            return bitMasks[indCasa] & unsafeforB;
        }
    }

    public long unsafeForMe() {
        if (isWhite) {
            return unsafeForWhite();
        } else {
            return unsafeForBlack();
        }
    }

    public long unsafeForOpponent() {
        if (!isWhite) {
            return unsafeForWhite();
        } else {
            return unsafeForBlack();
        }
    }

    public long unsafeForBlack() {
        Bits bC = bits.Clone();
        long unsafe;
        bits.OCCUPIED = bC.WP | bC.WN | bC.WB | bC.WR | bC.WQ | bC.WK | bC.BP | bC.BN | bC.BB | bC.BR | bC.BQ;
        //pawn
        unsafe = ((bC.WP >>> 7) & ~FILE_A);//pawn capture right
        unsafe |= ((bC.WP >>> 9) & ~FILE_H);//pawn capture left
        long possibility;
        //knight
        long i = bC.WN & ~(bC.WN - 1);
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            if (iLocation > 18) {
                possibility = KNIGHT_SPAN << (iLocation - 18);
            } else {
                possibility = KNIGHT_SPAN >> (18 - iLocation);
            }
            if (iLocation % COLUMNS < 4) {
                possibility &= ~FILE_GH;
            } else {
                possibility &= ~FILE_AB;
            }
            unsafe |= possibility;
            bC.WN &= ~i;
            i = bC.WN & ~(bC.WN - 1);
        }
        //bishop/queen
        long QB = bC.WQ | bC.WB;
        i = QB & ~(QB - 1);
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = DAndAntiDMoves(iLocation);
            unsafe |= possibility;
            QB &= ~i;
            i = QB & ~(QB - 1);
        }
        //rook/queen
        long QR = bC.WQ | bC.WR;
        i = QR & ~(QR - 1);
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = HAndVMoves(iLocation);
            unsafe |= possibility;
            QR &= ~i;
            i = QR & ~(QR - 1);
        }
        //king
        int iLocation = Long.numberOfTrailingZeros(bC.WK);
        if (iLocation > 9) {
            possibility = KING_SPAN << (iLocation - 9);
        } else {
            possibility = KING_SPAN >> (9 - iLocation);
        }
        if (iLocation % COLUMNS < 4) {
            possibility &= ~FILE_GH;
        } else {
            possibility &= ~FILE_AB;
        }
        unsafe |= possibility;
        bits.OCCUPIED |= bC.BK;
        return unsafe;
    }

    public long unsafeForWhite() {
        Bits bC = bits.Clone();
        long unsafe;
        bits.OCCUPIED = bC.WP | bC.WN | bC.WB | bC.WR | bC.WQ | bC.BP | bC.BN | bC.BB | bC.BR | bC.BQ | bC.BK;
        //pawn
        unsafe = ((bC.BP << 7) & ~FILE_H);//pawn capture right
        unsafe |= ((bC.BP << 9) & ~FILE_A);//pawn capture left
        long possibility;
        //knight
        long i = bC.BN & ~(bC.BN - 1);
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            if (iLocation > 18) {
                possibility = KNIGHT_SPAN << (iLocation - 18);
            } else {
                possibility = KNIGHT_SPAN >> (18 - iLocation);
            }
            if (iLocation % COLUMNS < 4) {
                possibility &= ~FILE_GH;
            } else {
                possibility &= ~FILE_AB;
            }
            unsafe |= possibility;
            bC.BN &= ~i;
            i = bC.BN & ~(bC.BN - 1);
        }
        //bishop/queen
        long QB = bC.BQ | bC.BB;
        i = QB & ~(QB - 1);
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = DAndAntiDMoves(iLocation);
            unsafe |= possibility;
            QB &= ~i;
            i = QB & ~(QB - 1);
        }
        //rook/queen
        long QR = bC.BQ | bC.BR;
        i = QR & ~(QR - 1);
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = HAndVMoves(iLocation);
            unsafe |= possibility;
            QR &= ~i;
            i = QR & ~(QR - 1);
        }
        //king
        int iLocation = Long.numberOfTrailingZeros(bC.BK);
        if (iLocation > 9) {
            possibility = KING_SPAN << (iLocation - 9);
        } else {
            possibility = KING_SPAN >> (9 - iLocation);
        }
        if (iLocation % COLUMNS < 4) {
            possibility &= ~FILE_GH;
        } else {
            possibility &= ~FILE_AB;
        }
        unsafe |= possibility;
        bits.OCCUPIED |= bC.WK;
        return unsafe;
    }

    public int countSetBits(Long n) {
        int valor = Long.bitCount(n);
        return valor;
    }

    public boolean check(boolean isWhite) {
        if (!isWhite) {
            initBlack();
        } else {
            initWhite();
        }

        long K = isWhite ? bits.WK : bits.BK;

        // check if the king is unsafe
        long UNSAFE = isWhite ? unsafeForWhite() : unsafeForBlack();
//        System.out.println(drawArray(bits));
        return ((K & UNSAFE) == K && !checkMate(isWhite));
    }

    public ArrayList<Piece> piecesAttackingKingOf(boolean isWhite) {
        ArrayList<Piece> pieces = new ArrayList();
        if (!isWhite) {
            initBlack();
        } else {
            initWhite();
        }

        long K = isWhite ? bits.WK : bits.BK;

        // verifies if king is unsafe
        long UNSAFE = isWhite ? unsafeForWhite() : unsafeForBlack();
        if (((K & UNSAFE) == K && !checkMate(isWhite))) {
            if (isWhite) {
                if ((K & possibleBPLong(bits.BP, bits.WP, bits.EP)) > 0) {
                    pieces.add(Piece.BLACK_PAWN);
                }
                if ((K & possibleBLong(bits.BB)) > 0) {
                    pieces.add(Piece.BLACK_BISHOP);
                }
                if ((K & possibleNLong(bits.BN)) > 0) {
                    pieces.add(Piece.BLACK_KNIGHT);
                }
                if ((K & possibleRLong(bits.BR)) > 0) {
                    pieces.add(Piece.BLACK_ROOK);
                }
                if ((K & possibleNLong(bits.BQ)) > 0) {
                    pieces.add(Piece.BLACK_QUEEN);
                }
            } else {
                if ((K & possibleBPLong(bits.WP, bits.BP, bits.EP)) > 0) {
                    pieces.add(Piece.WHITE_PAWN);
                }
                if ((K & possibleBLong(bits.WB)) > 0) {
                    pieces.add(Piece.WHITE_BISHOP);
                }
                if ((K & possibleNLong(bits.WN)) > 0) {
                    pieces.add(Piece.WHITE_KNIGHT);
                }
                if ((K & possibleRLong(bits.WR)) > 0) {
                    pieces.add(Piece.WHITE_ROOK);
                }
                if ((K & possibleNLong(bits.WQ)) > 0) {
                    pieces.add(Piece.WHITE_QUEEN);
                }
            }
        }
        return pieces;
    }

    public boolean checkMate(boolean isWhite) {
        String moves;
        if (!isWhite) {
            initBlack();
            moves = possibleMovesB(bits);
        } else {
            initWhite();
            moves = possibleMovesW(bits);
        }

        long K = isWhite ? bits.WK : bits.BK;

        // verify if king has an unsafe move
        long UNSAFE = isWhite ? unsafeForWhite() : unsafeForBlack();
        if ((K & UNSAFE) == K) { // king is unsafe
            long poss = possibleKSecureLong(UNSAFE, K);
            if (poss == 0) { // the king can't move
                // checks if some piece can move to defeat the threat and safe the king
                Bits bAux = bits.Clone();
                int ml = moves.length();
                for (int i = 0; i < ml; i += 4) {
                    String move = moves.substring(i, i + 4);
                    if (makeMove(move, isWhite ? 'K' : 'k')) {
                        bits = bAux.Clone();
                        return false;
                    }
                    bits = bAux.Clone();
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Pieces getters">
    public String getPiece(int col, int row) {
        if (col < 0 || col >= COLUMNS || row < 0 || row >= LINES) {
            return " ";
        }
        return chessBoard[7 - row][col];
    }

    public Piece getEnumPiece(int col, int row) {
        Piece p = null;
        switch (chessBoard[7 - row][col]) {
            case "K":
                p = Piece.WHITE_KING;
                break;
            case "Q":
                p = Piece.WHITE_QUEEN;
                break;
            case "R":
                p = Piece.WHITE_ROOK;
                break;
            case "N":
                p = Piece.WHITE_KNIGHT;
                break;
            case "B":
                p = Piece.WHITE_BISHOP;
                break;
            case "P":
                p = Piece.WHITE_PAWN;
                break;
            case "k":
                p = Piece.BLACK_KING;
                break;
            case "q":
                p = Piece.BLACK_QUEEN;
                break;
            case "r":
                p = Piece.BLACK_ROOK;
                break;
            case "n":
                p = Piece.BLACK_KNIGHT;
                break;
            case "b":
                p = Piece.BLACK_BISHOP;
                break;
            case "p":
                p = Piece.BLACK_PAWN;
                break;
        }
        return p;
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Defenses handling and getters">
    
    //Resets the matrix of the heuristic value of the defenses
    public void resetDefenses() {
        for (int i = 0; i < 8; i++) {
            Arrays.fill(MyDefensesValue[i], 0);
            Arrays.fill(YourDefensesValue[i], 0);
            Arrays.fill(MyDefensesCount[i], 0);
            Arrays.fill(YourDefensesCount[i], 0);
            Arrays.fill(MyDirectDefenses[i], 0);
            Arrays.fill(YourDirectDefenses[i], 0);
            Arrays.fill(MyDirectDefensesValue[i], 0);
            Arrays.fill(ValorSuasDefesasDiretas[i], 0);
            Arrays.fill(MyRelativeDefensesValue[i], 0);
            Arrays.fill(YourRelativeDefensesValue[i], 0);
        }
        for (int i = 0; i < 5; i++) {
            for(int j=0; j < 8; j++) {
               Arrays.fill(MyDefensesCountByType[i][j], 0);  
               Arrays.fill(YourDefensesCountByType[i][j], 0);  
            }
        }
    }
    
    //Defenses by type of piece - index of types definition:
    //In order: P = 0, R = 1, N = 2, B = 3, Q = 4 -> Kings has no index
    public void addDefenses(int col, int row, String pSan, double value, boolean isDirect) {
        row = 7 - row;
        if (!pSan.substring(1, 2).equalsIgnoreCase("K")) {
            int colAux = pSan.charAt(2) - 'a';
            int rowAux = 7 - (pSan.charAt(3) - '1');
            String pieceType = pSan.substring(1,2);
            int type = 0;
            switch(pieceType.toUpperCase()) {
                case "P" : type = 0; break;
                case "R" : type = 1; break;
                case "N" : type = 2; break;
                case "B" : type = 3; break;
                case "Q" : type = 4; break;
            }
            if (pSan.startsWith("m")) {
                addMyDefenses(col, row, value, value * ((colAux + rowAux * LINES) + 1), isDirect, type);
            } else {
                addYourDefenses(col, row, value, value * ((colAux + rowAux * LINES) + 1), isDirect, type);
            }
        }
    }

    public void addMyDefenses(int col, int row, double value, double relValue, boolean isDirect, int pieceType) {
        if (isDirect) {
            MyRelativeDefensesValue[row][col] += relValue;
            MyDirectDefensesValue[row][col] += value;
            MyDirectDefenses[row][col]++;
            MyDefensesCountByType[pieceType][row][col]+=1;
        } else {
            MyDefensesValue[row][col] += value;
            MyDefensesCount[row][col]++;
        }
    }

    public void addYourDefenses(int col, int row, double value, double relValue, boolean isDirect, int pieceType) {
        if (isDirect) {
            YourRelativeDefensesValue[row][col] += relValue;
            ValorSuasDefesasDiretas[row][col] += value;
            YourDirectDefenses[row][col]++;
            YourDefensesCountByType[pieceType][row][col]++;
        } else {
            YourDefensesValue[row][col] += value;
            YourDefensesCount[row][col]++;
        }
    }

    public double getMyKingDefensesRelativeValue(int col, int row) {
        return MyRelativeDefensesValue[7 - row][col];
    }

    public double getYourKingDefensesRelativeValue(int col, int row) {
        return YourRelativeDefensesValue[7 - row][col];
    }

    public double getMyDirectDefensesValue(int col, int row) {
        return MyDirectDefensesValue[7 - row][col];
    }

    public double getYourDirectDefensesValue(int col, int row) {
        return ValorSuasDefesasDiretas[7 - row][col];
    }

    public double getMyDefensesValue(int col, int row) {
        return MyDefensesValue[7 - row][col];
    }

    public double getYourDefensesValue(int col, int row) {
        return YourDefensesValue[7 - row][col];
    }

    public int getMyDirectDefenses(int col, int row) {
        return MyDirectDefenses[7 - row][col];
    }

    public int getYourDirectDefenses(int col, int row) {
        return YourDirectDefenses[7 - row][col];
    }

    public int getMyDefenses(int col, int row) {
        return MyDefensesCount[7 - row][col];
    }

    public int getYourDefenses(int col, int row) {
        return YourDefensesCount[7 - row][col];
    }
    // Calulates the number of pieces of the parameter pieceType defending a square
    public int getDefendingPiecesByType(int columnIndex, int rowIndex, String pieceType) {
        int col = columnIndex;
        int row = 7 - rowIndex;
        int type = 0;
        
        switch(pieceType.toUpperCase()) {
            case "P" : type = 0; break;
            case "R" : type = 1; break;
            case "N" : type = 2; break;
            case "B" : type = 3; break;
            case "Q" : type = 4; break;
        }
        
        char s = pieceType.charAt(0);
        if(Character.isUpperCase(s)) {
            return MyDefensesCountByType[type][row][col];
        } else {
            return YourDefensesCountByType[type][row][col];
        }
    }

    public void printDefenses() {
        System.out.println("My defenses");
        for (int i = 0; i < 8; i++) {
            System.out.println(Arrays.toString(MyDefensesValue[i]));
        }
        System.out.println("Your defenses");
        for (int i = 0; i < 8; i++) {
            System.out.println(Arrays.toString(YourDefensesValue[i]));
        }
    }

    private void printChessBoard(String lance) {
        System.out.println("Move: " + lance);
        System.out.println("------------------------------------");
        for (int i = 0; i < 8; i++) {
            System.out.println(Arrays.toString(chessBoard[i]));
        }
        System.out.println("------------------------------------");
    }

//</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Support functions">
    /* 
        Defines the bit of the passed square as chess algebric notation
    */
    public long setBitSquare(String alg) {
        int col = "abcdefgh".indexOf(alg.substring(0, 1));
        int row = Integer.parseInt(alg.substring(1)) - 1; // less 1 to put into the zero-seven coordinate system
        return setBitSquare(row, col);
    }

    //Defines the bit of the square passed as coordinates
    public long setBitSquare(int row, int col) {
        int i = (7 - row); // convert into the bitboard coordinate system: left -> right top -> bottom
        String Binary = "0000000000000000000000000000000000000000000000000000000000000000";
        Binary = Binary.substring(i + 1) + "1" + Binary.substring(0, i);
        return convertStringToBitboard(Binary);
    }

    public void InitVisitedSquares(int source) {
        Arrays.fill(visited, 0);
        visited[source] = 1; // this point in the matrix starts as visited, it avoids unnecessary recursions
    }

    public boolean isAdjacent(int sRow, int sCol, int dRow, int dCol) {
        return (Math.abs(sRow - dRow) <= 1 && Math.abs(sCol - dCol) <= 1
                && (dRow != sRow || dCol != sCol));
    }

    public boolean isSecure(long bitSecure, int row, int col) {
        long ix = row * 8 + col;
        long bit = 1L << ix;
        return (bitSecure & bit) > 0;
    }

    public int kingPathTo(boolean isWhite, long bitSecure, int sRow, int sCol, int dRow, int dCol) {
        if(isWhite) {
            initWhite();
        } else {
            initBlack();
        }
        bitSecure = bitSecure & bits.NOT_MY_PIECES & ~bits.MY_PIECES;
        stack++;
        if (isAdjacent(sRow, sCol, dRow, dCol)) {
            stack--;
            return 1;
        }
        visited[sRow * 8 + sCol] = 1;
        for (int linW = 0; linW < LINES; linW++) {
            for (int colW = 0; colW < LINES; colW++) {
                if (isAdjacent(sRow, sCol, linW, colW)
                        && isSecure(bitSecure, linW, colW)
                        && visited[linW * 8 + colW] == 0) {
                    if (kingPathTo(isWhite,bitSecure, linW, colW, dRow, dCol) == 1) {
                        stack--;
                        return 1;
                    }
                }
            }
        }
        return 0;
    }

    public int wedgesBetween(int fromRow, int fromCol, int toRow, int toCol, boolean isWhite) {
        long bitSL = 0;
        long w;
        long maskO = 1L << (fromRow * 8 + fromCol);
        if (isWhite) {
            initWhite();
        } else {
            initBlack();
        }
        if (fromRow == toRow || fromCol == toCol) { 
            if (fromRow > toRow) {
                int t = fromRow;
                fromRow = toRow;
                toRow = t;
            }
            if (fromCol > toCol) {
                int t = fromCol;
                fromCol = toCol;
                toCol = t;
            }

            for (int rank = 0; rank < 8; rank++) {
                for (int file = 0; file < 8; file++) {
                    if (rank > fromRow && rank <= toRow && fromCol == toCol && file == fromCol) {
                        long mask = 1L << (rank * 8 + file);
                        bitSL |= mask;
                    } else if (fromRow == toRow && rank == fromRow && file > fromCol && file <= toCol) {
                        long mask = 1L << (rank * 8 + file);
                        bitSL |= mask;
                    }
                }
            }
        } else { 
            int dy = fromRow < toRow ? 1 : -1;
            int dx = fromCol < toCol ? 1 : -1;
            int rank = fromRow;
            int file = fromCol;
            while (rank != toRow && file != toCol) {
                long n = (rank * 8 + file);
                long mask = 1L << n;
                bitSL |= mask;
                rank += dy;
                file += dx;
            }
        }
        if (!isWhite) {
            w = (possibleBLong(bits.BB)
                    | possibleNLong(bits.BN)
                    | possibleQLong(bits.BQ & ~maskO) // prevents the interposition from the origin
                    | possibleRLong(bits.BR & ~maskO) // prevents the interposition from the origin
                    | possibleBPLong(bits.BP, bits.WP, bits.EP))
                    & bitSL;
        } else {
            w = (possibleBLong(bits.WB)
                    | possibleNLong(bits.WN)
                    | possibleQLong(bits.WQ & ~maskO) // prevents the interposition from the origin
                    | possibleRLong(bits.WR & ~maskO) // prevents the interposition from the origin
                    | possibleWPLong(bits.WP, bits.BP, bits.EP))
                    & bitSL;
        }
        return Long.bitCount(w);
    }

    //</editor-fold>
    
}
