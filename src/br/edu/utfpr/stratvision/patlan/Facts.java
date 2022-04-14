package br.edu.utfpr.stratvision.patlan;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import alice.tuprolog.*;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Luis Carlos F. Bueno - 12/11/2021 This class creates the tuProlog
 * theory
 */
public class Facts {

    //<editor-fold desc="Fields and varibles">
    private final static Charset ENCODING = StandardCharsets.UTF_8;
    private BitBoard activeBitboard, myBitboard, yourBitboard;
    private boolean isWhite; // true if white is searching 
    private final String[] alg = new String[]{"a", "b", "c", "d", "e", "f", "g", "h"};

    private GamePhase gamePhase;
    private final Prolog prolog;
    private final int mode; // from PatternEvaluator authoring mode constants
    private Theory oponnentTheory; //the reverse theory from the opponent view point
    private Theory myTheory;    //theory from the player view point
    private boolean isMyTheoryActive = true;
    //</editor-fold>
    
    //<editor-fold desc="Constructors">
    public Facts(boolean isWhite, Prolog prolog, GamePhase gamePhase, int mode) {
        this.isWhite = isWhite;
        this.prolog = prolog;
        this.mode = mode;
        this.gamePhase = gamePhase;
    }
    //</editor-fold>
    
    //<editor-fold desc="Fact generators">
    private void addPositionalFacts(ArrayList<String> fatos) {
        String pAuxSan, pAuxPos;
        String pPos, pSan;
        String n1, n2, r1, r2; // indicates if the object belongs to the side (white or black) searching
        String pAux, p;
        // atomic null facts necessary to avoid runtime errors
        String[] nulos = new String[]{
            "p(null)", "q(null)", "k(null)", "b(null)", "r(null)", "n(null)",
            "protbyme(null)", "protbyyou(null)", "south(null,null)", "west(null,null)",
            "pattack(null,null,null)", // possible attack
            "iattack(null,null,null)", // indirect attack
            "attacks(null,null)",
            "defends(null,null)",
            "move(null,null)",
            "pdefense(null,null,null)", //possible defense
            "idefense(null,null,null)", //indirect defense
            "xequemate(null,null)"
        };

        for (int col = 0; col < 8; col++) {
            for (int lin = 0; lin < 8; lin++) {
                pAuxPos = alg[col] + (lin + 1);
                fatos.add("square(c" + pAuxPos + ")");
                p = getActiveBitboard().getPiece(col, lin);
                if (!p.equals(" ")) {
                    if (isWhite) {
                        n1 = "m";
                        r1 = "my(";
                        n2 = "y";
                        r2 = "your(";
                    } else {
                        n1 = "y";
                        r1 = "your(";
                        n2 = "m";
                        r2 = "my(";
                    }
                    pPos = alg[col] + (lin + 1);
                    pSan = (Character.isUpperCase(p.charAt(0)) ? n1 : n2) + p + pPos;
                    fatos.add(String.valueOf(p).toLowerCase() + "(" + pSan + ")");
                    fatos.add((Character.isUpperCase(p.charAt(0)) ? r1 : r2) + pSan + ")");
                    createDirectSemanticFacts(fatos, p, pSan, lin, col, n1, n2);

                    // oeste
                    pAux = getActiveBitboard().getPiece(col + 1, lin);
                    if (!pAux.equals(" ")) {
                        pAuxPos = alg[col + 1] + (lin + 1);
                        pAuxSan = (Character.isUpperCase(pAux.charAt(0)) ? n1 : n2) + pAux + pAuxPos;
                        if (isWhite) {
                            fatos.add("west(" + pSan + "," + pAuxSan + ")");
                        } else {
                            fatos.add("west(" + pAuxSan + "," + pSan + ")");
                        }
                    }

                    if (lin == 7) { // the last facts are already set on line 6
                        break;
                    }

                    //NE / SO
                    pAux = getActiveBitboard().getPiece(col + 1, lin + 1);
                    if (!pAux.equals(" ")) {
                        pAuxPos = alg[col + 1] + (lin + 2);
                        pAuxSan = (Character.isUpperCase(pAux.charAt(0)) ? n1 : n2) + pAux + pAuxPos;
                        if (isWhite) {
                            fatos.add("so(" + pSan + "," + pAuxSan + ")");
                        } else {
                            fatos.add("so(" + pAuxSan + "," + pSan + ")");
                        }
                    }
                    //NO / SE
                    pAux = getActiveBitboard().getPiece(col - 1, lin + 1);
                    if (!pAux.equals(" ")) {
                        pAuxPos = alg[col - 1] + (lin + 2);
                        pAuxSan = (Character.isUpperCase(pAux.charAt(0)) ? n1 : n2) + pAux + pAuxPos;
                        if (isWhite) {
                            fatos.add("se(" + pSan + "," + pAuxSan + ")");
                        } else {
                            fatos.add("se(" + pAuxSan + "," + pSan + ")");
                        }
                    }
                    //sul
                    pAux = getActiveBitboard().getPiece(col, lin + 1);
                    if (!pAux.equals(" ")) {
                        pAuxPos = alg[col] + (lin + 2);
                        pAuxSan = (Character.isUpperCase(pAux.charAt(0)) ? n1 : n2) + pAux + pAuxPos;
                        if (isWhite) {
                            fatos.add("south(" + pSan + "," + pAuxSan + ")");
                        } else {
                            fatos.add("south(" + pAuxSan + "," + pSan + ")");
                        }
                    }
                }
            }
        }
        // generation of indirect facts
        for (int col = 0; col < 8; col++) {
            for (int lin = 0; lin < 8; lin++) {
                p = getActiveBitboard().getPiece(col, lin);
                if (!p.equals(" ")) {
                    if (isWhite) {
                        n1 = "m";
                        n2 = "y";
                    } else {
                        n1 = "y";
                        n2 = "m";
                    }
                    pPos = alg[col] + (lin + 1);
                    pSan = (Character.isUpperCase(p.charAt(0)) ? n1 : n2) + p + pPos;
                    createIndirectSemanticFacts(fatos, p, pSan, lin, col, n1, n2);
                }
            }
        }

        //inference rules
        fatos.add("east(X,Y):-west(Y,X)");
        fatos.add("north(X,Y):-south(Y,X)");
        fatos.add("ne(X,Y):-so(Y,X)");
        fatos.add("no(X,Y):-se(Y,X)");
        fatos.add("defendedbyme(C,F) :- (my(C);square(C)), mydefenses(X,F), member(C,X)");
        fatos.add("defendedbyyou(C) :- (your(C);square(C)), yourdefenses(X), member(C,X)");
        fatos.add("undefendedbyme(C,F):- (my(C);square(C)), mydefenses(X,m), not(member(C,X))");
        fatos.add("undefendedbyyou(C):- (your(C);square(C)), yourdefenses(X), not(member(C,X))");
        fatos.add("mydefenses(Squares,F) :- findall(C,(my(Y), (defends(Y,C);idefense(Y,F,C)), not(Y=F)), Squares)");
        fatos.add("yourdefenses(Squares) :- findall(C,(your(Y), defends(Y,C)), Squares)");
        fatos.addAll(Arrays.asList(nulos));
    }

    /*
        Verify if the pattern is an indirect attack/defense or an indirect threat/protection.
        A pattern is said to be an indirect attack/defense when the indirect attacked pieces are
        in the scope of the piece which is attacking or defending. Ex. the rook can indirect attack or defend
        only the pieces which are in his line or column.
        @pSan - the algebric letter of the piece which is attacking or defending
     */
    private boolean isPossible(String pSan, int linO, int colO, int linI, int colI, int linD, int colD) {
        boolean threat = false;
        switch (pSan.substring(1, 2).toUpperCase()) {
            case "K": {
                threat = (Math.abs(colD - colO) > 1 || Math.abs(linD - linO) > 1);
            }
            break;
            case "Q": {
                threat = !((linO == linD && linD == linI)
                        || (colO == colD && colD == colI)
                        || (Math.abs(colD - colO) == Math.abs(linO - linD)
                        && Math.abs(colD - colI) == Math.abs(linI - linD)));
            }
            break;
            case "R": {
                threat = !(linO == linD || colO == colD);
            }
            break;
            case "B": {
                threat = !(Math.abs(colD - colO) == Math.abs(linO - linD));
            }
            break;
            case "N":
            case "P": {
                threat = true;
            }
            break;
        }
        return threat;
    }

    private void addDirectAttacks(ArrayList<String> facts, String pSan, String moves, String n1, String n2) {
        int ml = moves.length();
        for (int ic = 0; ic < ml; ic += 4) {
            String move = moves.substring(ic, ic + 4);
            if (Character.isDigit(move.charAt(3))) {//'regular' move
                int lin = (7 - Character.getNumericValue(move.charAt(2)));
                int col = (Character.getNumericValue(move.charAt(3)));
                String pAux = getActiveBitboard().getPiece(col, lin);
                String pAuxPos = alg[col] + (lin + 1);
                String pAuxSan = (Character.isUpperCase(pAux.charAt(0)) ? n1 : n2) + pAux + pAuxPos;
                if (!pAux.equals(" ")) { // direct attack if the square is not empty
                    facts.add("attacks(" + pSan + "," + pAuxSan + ")"); // attacks the piece but also can move to the square
                    facts.add("move(" + pSan + ",c" + pAuxPos + ")");
                    if (!pSan.substring(1, 2).equalsIgnoreCase("P")) {
                        getActiveBitboard().addDefenses(col, lin, pSan, gamePhase.getValueBySAN(pSan.substring(1, 2)), true);
                        getActiveBitboard().addDefenses(col, lin, pSan, gamePhase.getValueBySAN(pSan.substring(1, 2)), false);
                        facts.add("defends(" + pSan + ",c" + pAuxPos + ")"); // defends the square and
                    }
                } else { // if the square is empty adds the movement and square defenses
                    if (!pSan.substring(1, 2).equalsIgnoreCase("P")) {// if it's not a pawn adds squares defenses
                        facts.add("defends(" + pSan + ",c" + pAuxPos + ")");
                        getActiveBitboard().addDefenses(col, lin, pSan, gamePhase.getValueBySAN(pSan.substring(1, 2)), true);
                        getActiveBitboard().addDefenses(col, lin, pSan, gamePhase.getValueBySAN(pSan.substring(1, 2)), false);
                    }
                    facts.add("move(" + pSan + ",c" + pAuxPos + ")");
                }
            }
        }
    }

    @SuppressWarnings("empty-statement")
    private void addIAttacks(ArrayList<String> facts, String pSan, int linO, int colO, String moves, String n1, String n2, boolean porDefesa) {
        boolean isPawn = pSan.substring(1, 2).equalsIgnoreCase("p");
        int lin = 0, col = 0;
        Bits bitsClone = getActiveBitboard().bits.Clone();
        int ml = moves.length();
        for (int ic = 0; ic < ml; ic += 4) {
            String move = moves.substring(ic, ic + 4);
            if (Character.isDigit(move.charAt(3))) {//'regular' move
                lin = (7 - Character.getNumericValue(move.charAt(2)));
                col = (Character.getNumericValue(move.charAt(3)));
            } else if (moves.charAt(3) == 'P') {//pawn promotion
                lin = (7 - Character.getNumericValue(move.charAt(0)));
                col = (Character.getNumericValue(move.charAt(1)));
            } else if (moves.charAt(3) == 'E') {//en passant
                lin = (7 - Character.getNumericValue(move.charAt(0)));
                col = (Character.getNumericValue(move.charAt(1)));
            }
            String pAux = getActiveBitboard().getPiece(col, lin);

            //verifies conditios for indirect attacks through defenses
            //if the defending piece is a pawn there is not a indirect defense.
            if (porDefesa && (pAux.equals(" ") || isPawn)) {
                continue;
            }

            String pAuxPos = alg[col] + (lin + 1);
            String pAuxSan = (Character.isUpperCase(pAux.charAt(0)) ? n1 : n2) + pAux + pAuxPos; //owner + SAN + square
            if (!activeBitboard.makeMove(move, pSan.charAt(1))) {
                activeBitboard.bits = bitsClone.Clone();
                continue;
            }
            //verifies if exits a checkmate as result of the movement
            if (!porDefesa && getActiveBitboard().checkMate(!isWhite)) {
                facts.add("mate(" + pSan + ",c" + pAuxPos + ")");
            }
            int link = 0, colk = 0;
            String movimentos = getActiveBitboard().directAttacks(String.valueOf(pSan.charAt(1)), lin, col);
            int mlx = movimentos.length();
            for (int ik = 0; ik < mlx; ik += 4) {
                String movek = movimentos.substring(ik, ik + 4);
                if (Character.isDigit(movek.charAt(3))) {//'regular' move
                    link = (7 - Character.getNumericValue(movek.charAt(2)));
                    colk = (Character.getNumericValue(movek.charAt(3)));
                } else if (moves.charAt(3) == 'P') {//pawn promotion
                    link = (7 - Character.getNumericValue(move.charAt(0)));
                    colk = (Character.getNumericValue(move.charAt(1)));
                } else if (moves.charAt(3) == 'E') {//en passant
                    link = (7 - Character.getNumericValue(move.charAt(0)));
                    colk = (Character.getNumericValue(move.charAt(1)));
                }
                //ignores if the move is to the source square (previous state)
                if (link == linO && colk == colO) {
                    continue;
                }

                String pAuxk = getActiveBitboard().getPiece(colk, link); //the piece being checked for the attack
                String pAuxPosk = alg[colk] + (link + 1);
                String pAuxSank = (Character.isUpperCase(pAuxk.charAt(0)) ? n1 : n2) + pAuxk + pAuxPosk;
                if (!pAuxk.equals(" ")) { // target square is not empty
                    String d = "attacks(" + pSan + "," + pAuxSank + ")";
                    if (!facts.contains(d)) { // verifies if already exists a direct attack to the target piece pAuxSank
                        if (pAux.equals(" ")) { //if the original move is to a empty square
                            facts.add("pattack(" + pSan + "," + "c" + pAuxPos + "," + pAuxSank + ")");
//                                    if(!byDefense){ 
//                                        facts.add("pdefense(" + pSan + "," + "c" + pAuxPos + ",c" + pAuxPosk + ")");
//                                        bitboard.addDefenses(colk, link, pSan, etapa.getValueBySAN(pSan.substring(1, 2)));
//                                    }

                        } else {
                            if (!isPossible(pSan, linO, colO, lin, col, link, colk)) {
                                facts.add("iattack(" + pSan + "," + pAuxSan + "," + pAuxSank + ")");
                                facts.add("iattack(" + pSan + ",c" + pAuxPos + "," + pAuxSank + ")");
//                                        if(!byDefense){ 
//                                            facts.add("idefense(" + pSan + ",c" + pAuxPos + ",c" + pAuxSank + ")");
//                                            bitboard.addDefenses(colk, link, pSan, etapa.getValueBySAN(pSan.substring(1, 2)));
//                                        }
                            } else {
                                /*
                                   Konown issue:
                                   
                                   Here exists a conflict that can produce false negatives trying to stop that 
                                   possible attacks by a defended piece be included on the theory, like this case:
                                   
                                   3r3k/1p1b1Qbp/1n2B1p1/p5N1/Pq6/8/1P4PP/R6K w - - 0 1 philidor mate
                                   the knight has to do an attack to the King by the Queen square.
                                   
                                   However, if we let the fact be included on the theory, it can produces false positives
                                   as in the case of Anastasia mate in the position below:
                                   
                                   Q7/p1p1k1pp/1p6/8/5r1b/8/P3nPPK/RN3R2 b - - 0 1 
                                
                                   In this case, the rook can attack the King through his Bishop.
                                
                                   The decision taken is to include the fact. It is better having a false positive than a negative one.
                                
                                 */

//                                if (!byDefense) { 
                                facts.add("pattack(" + pSan + "," + pAuxSan + "," + pAuxSank + ")");
                                facts.add("pattack(" + pSan + ",c" + pAuxPos + "," + pAuxSank + ")");
                                //facts.add("pdefense(" + pSan + ",c" + pAuxPos + ",c" + pAuxPosk + ")");
//                                  bitboard.addDefenses(colk, link, pSan, etapa.getValueBySAN(pSan.substring(1, 2)));
//                                }
                            }
                        }
                    }
                } else {
                    String d = "defende(" + pSan + ",c" + pAuxPosk + ")";
                    if (!facts.contains(d)) { // verifies if not exists a direct defense of the target square
                        //verifies if the target is not out of the range of the source piece
                        if (!isPossible(pSan, linO, colO, lin, col, link, colk)) {
                            if (!pAux.equals(" ")) {
                                facts.add("idefense(" + pSan + "," + pAuxSan + ",c" + pAuxPosk + ")");
                            }
                            facts.add("idefense(" + pSan + "," + "c" + pAuxPos + ",c" + pAuxPosk + ")");
                            getActiveBitboard().addDefenses(colk, link, pSan, gamePhase.getValueBySAN(pSan.substring(1, 2)), false);
                        }
                    }
                }
            }
            activeBitboard.bits = bitsClone.Clone(); // restores the original state
        }
    }

    private void addDirectDefenses(ArrayList<String> fatos, String pSan, String moves, String n1, String n2) {
        int ml = moves.length();
        for (int ic = 0; ic < ml; ic += 4) {
            String move = moves.substring(ic, ic + 4);
            if (Character.isDigit(move.charAt(3))) {//'regular' move
                int lin = (7 - Character.getNumericValue(move.charAt(2)));
                int col = (Character.getNumericValue(move.charAt(3)));
                String pAux = getActiveBitboard().getPiece(col, lin);
                if (!pAux.equals(" ")) { // direct defenses
                    String pAuxPos = alg[col] + (lin + 1);
                    String pAuxSan = (Character.isUpperCase(pAux.charAt(0)) ? n1 : n2) + pAux + pAuxPos;
                    fatos.add("defends(" + pSan + "," + pAuxSan + ")"); // piece defense
                    if (!pSan.substring(1, 2).equalsIgnoreCase("P")) {// if it's not a pawn add square defenses
                        fatos.add("defends(" + pSan + ",c" + pAuxPos + ")");
                        getActiveBitboard().addDefenses(col, lin, pSan, gamePhase.getValueBySAN(pSan.substring(1, 2)), true);
                        getActiveBitboard().addDefenses(col, lin, pSan, gamePhase.getValueBySAN(pSan.substring(1, 2)), false);
                    }
                }
            }
        }
    }

    private void addIDefenses(ArrayList<String> fatos, String pSan, int linO, int colO, String moves, String n1, String n2, boolean porDefesa) {
        boolean isPawn = pSan.substring(1, 2).equalsIgnoreCase("P");
        int lin = 0, col = 0;
        Bits bC = getActiveBitboard().bits.Clone(); // save the bitboard actual state
        int ml = moves.length();
        for (int ic = 0; ic < ml; ic += 4) {
            String move = moves.substring(ic, ic + 4);
            if (Character.isDigit(move.charAt(3))) {//'regular' move
                lin = (7 - Character.getNumericValue(move.charAt(2)));
                col = (Character.getNumericValue(move.charAt(3)));
            } else if (moves.charAt(3) == 'P') {//pawn promotion
                lin = (7 - Character.getNumericValue(move.charAt(0)));
                col = (Character.getNumericValue(move.charAt(1)));
            } else if (moves.charAt(3) == 'E') {//en passant
                lin = (7 - Character.getNumericValue(move.charAt(0)));
                col = (Character.getNumericValue(move.charAt(1)));
            }
            String pAux = getActiveBitboard().getPiece(col, lin); // piece / intermediary square
            if (porDefesa && (pAux.equals(" ") || isPawn)) { // if intermediary square is empty or
                //the piece being evaluated is a pawn and is defending another piece, there is no indirect defense
                //because pawns defends only diagonal squares
                continue;
            }
            String pAuxPos = alg[col] + (lin + 1);
            String pAuxSan = (Character.isUpperCase(pAux.charAt(0)) ? n1 : n2) + pAux + pAuxPos;
            if (isPawn) {
                defesasIndPorPeoes(fatos, pSan.substring(1, 2), pSan, pAuxPos, lin, col);
                continue;
            }

            if (!activeBitboard.makeMove(move, pSan.charAt(1))) {
                activeBitboard.bits = bC.Clone(); //returns to the initial state of the board
                continue;
            }
            int link = 0, colk = 0; // coordinates of the target square of the next piece moves
            String movimentos = getActiveBitboard().directDefenses(String.valueOf(pSan.charAt(1)), lin, col);
            for (int ik = 0; ik < movimentos.length(); ik += 4) {
                String movek = movimentos.substring(ik, ik + 4);
                if (Character.isDigit(movek.charAt(3))) {//'regular' move
                    link = (7 - Character.getNumericValue(movek.charAt(2)));
                    colk = (Character.getNumericValue(movek.charAt(3)));
                } else if (moves.charAt(3) == 'P') {//pawn promotion
                    link = (7 - Character.getNumericValue(move.charAt(0)));
                    colk = (Character.getNumericValue(move.charAt(1)));
                } else if (moves.charAt(3) == 'E') {//en passant
                    link = (7 - Character.getNumericValue(move.charAt(0)));
                    colk = (Character.getNumericValue(move.charAt(1)));
                }
                if (link == linO && colk == colO) {
                    continue; // ignores if the move is to the same square it was in the initial state
                }
                String pAuxk = getActiveBitboard().getPiece(colk, link);
                String pAuxPosk = alg[colk] + (link + 1);
                String pAuxSank = (Character.isUpperCase(pAuxk.charAt(0)) ? n1 : n2) + pAuxk + pAuxPosk;
                if (!pAuxk.equals(" ")) { // not empty target square
                    String d = "defends(" + pSan + "," + pAuxSank + ")";
                    if (pAux.equals(" ")) { // empty intermediary square
                        if (!fatos.contains(d)) {
                            if (isPossible(pSan, linO, colO, lin, col, link, colk)) {
                                fatos.add("pdefense(" + pSan + "," + "c" + pAuxPos + "," + pAuxSank + ")");
                                fatos.add("pdefense(" + pSan + "," + "c" + pAuxPos + ",c" + pAuxPosk + ")");
                            }
                        }
                    } else {
                        // to exist an effective indirect defense over the target square 'pAuxPosk' for heuristic evaluations
                        // the target square must be within the range of action of the piece
                        String a = "defense(" + pAuxSan + ",c" + pAuxPosk + ")";
                        if (fatos.contains(a)) {
                            getActiveBitboard().addDefenses(colk, link, pSan, gamePhase.getValueBySAN(pSan.substring(1, 2)), false);
                        }
                        if (!isPossible(pSan, linO, colO, lin, col, link, colk)) {
                            fatos.add("idefense(" + pSan + "," + pAuxSan + "," + pAuxSank + ")");
                            fatos.add("idefense(" + pSan + "," + pAuxSan + ",c" + pAuxPosk + ")");
                            fatos.add("idefense(" + pSan + "," + "c" + pAuxPos + "," + pAuxSank + ")");
                            fatos.add("idefense(" + pSan + "," + "c" + pAuxPos + ",c" + pAuxPosk + ")");
                        } else {
                            fatos.add("pdefense(" + pSan + "," + pAuxSan + "," + pAuxSank + ")");
                            fatos.add("pdefense(" + pSan + "," + "c" + pAuxPos + "," + pAuxSank + ")");
                            fatos.add("pdefense(" + pSan + "," + "c" + pAuxPos + ",c" + pAuxPosk + ")");
                        }
                    }
                } else {
                    if (isPossible(pSan, linO, colO, lin, col, link, colk)) {
                        fatos.add("pdefense(" + pSan + "," + "c" + pAuxPos + ",c" + pAuxPosk + ")");
                    }
                }
            }
            activeBitboard.bits = bC.Clone(); // returns bitboard to the original state
        }
    }

    //special case
    private void addPawnDefenses(ArrayList<String> facts, String peca, String pSan, int pLin, int pCol) {
        String fato = "defense(";
        if (peca.equalsIgnoreCase("P")) {
            int linAux = (pLin + (peca.equals("P") ? 1 : -1));
            if (pCol > 0) {
                // if it is a black pawn decreases 1 of pLin
                String pAuxPos = alg[pCol - 1] + (pLin + 1 + (peca.equals("P") ? 1 : -1));
                facts.add(fato + pSan + ",c" + pAuxPos + ")");
                getActiveBitboard().addDefenses(pCol - 1, linAux, pSan, gamePhase.getValueBySAN(peca), true);
                getActiveBitboard().addDefenses(pCol - 1, linAux, pSan, gamePhase.getValueBySAN(peca), false);
            }
            if (pCol < 7) {
                String pAuxPos = alg[pCol + 1] + (pLin + 1 + (peca.equals("P") ? 1 : -1));
                facts.add(fato + pSan + ",c" + pAuxPos + ")");
                getActiveBitboard().addDefenses(pCol + 1, linAux, pSan, gamePhase.getValueBySAN(peca), true);
                getActiveBitboard().addDefenses(pCol + 1, linAux, pSan, gamePhase.getValueBySAN(peca), false);
            }
        }
    }

    private void defesasIndPorPeoes(ArrayList<String> fatos, String peca, String pSan, String pSanPos, int pLin, int pCol) {
        // if it is a pawn adds squares defenses to the right and left square where it can capture
        if (peca.equalsIgnoreCase("P")) {
            String fato = "defesapossivel(";
            // discount 1 from pLin if it is a black pawn
            int linAux = (pLin + (peca.equals("P") ? 1 : -1));
            if (pCol > 0) {
                String pAuxPos = alg[pCol - 1] + (linAux + 1); 
                fatos.add(fato + pSan + ",c" + pSanPos + ",c" + pAuxPos + ")");
            }
            if (pCol < 7) {
                String pAuxPos = alg[pCol + 1] + (linAux + 1);
                fatos.add(fato + pSan + ",c" + pSanPos + ",c" + pAuxPos + ")");
            }
        }
    }

    // add direct facts
    private void createDirectSemanticFacts(ArrayList<String> fatos, String peca, String pSan, int pLin, int pCol, String n1, String n2) {
        addPawnDefenses(fatos, peca, pSan, pLin, pCol);
        String ataques = getActiveBitboard().directAttacks(peca, pLin, pCol); 
        String defesas = getActiveBitboard().directDefenses(peca, pLin, pCol); 
        addDirectAttacks(fatos, pSan, ataques, n1, n2); 
        addDirectDefenses(fatos, pSan, defesas, n1, n2);
    }

    // add indirect facts (1 move ahead)
    private void createIndirectSemanticFacts(ArrayList<String> fatos, String peca, String pSan, int pLin, int pCol, String n1, String n2) {
        String ataques = getActiveBitboard().directAttacks(peca, pLin, pCol);
        String defesas = getActiveBitboard().directDefenses(peca, pLin, pCol);
        addIAttacks(fatos, pSan, pLin, pCol, ataques, n1, n2, false); // through attacks
        addIAttacks(fatos, pSan, pLin, pCol, defesas, n1, n2, true);  // through defenses
        addIDefenses(fatos, pSan, pLin, pCol, ataques, n1, n2, false); // through attacks
        addIDefenses(fatos, pSan, pLin, pCol, defesas, n1, n2, true);  // through defenses
    }

    public void createMyTheory(BitBoard bitboard) throws Exception {
        bitboard.resetDefenses();
        bitboard.assignsPlayer(isWhite);
        isMyTheoryActive = true;
        activeBitboard = bitboard;
        myBitboard = bitboard.clone();
        yourBitboard = bitboard.clone();
        yourBitboard.assignsPlayer(!isWhite);
        StringBuilder fatosS = new StringBuilder();
        ArrayList<String> fatos = new ArrayList<>();
        try {
            addPositionalFacts(fatos);
            Collections.sort(fatos);

            fatos.stream().forEach((s) -> {
                fatosS.append(s).append(".\n");
            });
            if (mode == PatternEvaluator.AUTHORING_MODE) { //saves a temporary file
                File ftemp;
                String property = "java.io.tmpdir";
                String tempDir = System.getProperty(property);

                File filePath = new File(tempDir);
                if (Files.isDirectory(filePath.toPath())) {
                    File fList[] = filePath.listFiles();
                    for (File pes : fList) {
                        if (pes.getName().endsWith(".pl")) {
                            pes.delete();
                        }
                    }
                }
                ftemp = File.createTempFile("kbpl", ".pl", filePath);
                try (BufferedWriter writer = Files.newBufferedWriter(ftemp.toPath(), ENCODING)) {
                    writer.write(fatosS.toString(), 0, fatosS.length());
                }
            }
            Theory t = new Theory(fatosS.toString());
            myTheory = t;
            prolog.setTheory(t);
        } catch (InvalidTheoryException ex) {
            throw ex;
        }
    }

    public void createOpponentTheory() throws Exception {
        isWhite = !isWhite; //changes the player
        BitBoard original = activeBitboard.clone();
        activeBitboard = yourBitboard;
        activeBitboard.resetDefenses();
        getActiveBitboard().assignsPlayer(isWhite);
        ArrayList<String> fatos = new ArrayList<>();
        try {
            addPositionalFacts(fatos);
            Collections.sort(fatos);
            StringBuilder fatosS = new StringBuilder();
            fatos.stream().forEach((s) -> {
                fatosS.append(s).append(".\n");
            });
            oponnentTheory = new Theory(fatosS.toString());
        } catch (InvalidTheoryException ex) {
            throw ex;
        }
        isWhite = !isWhite; //restores the player and bitboard
        activeBitboard = original;
    }

    //</editor-fold>
    
    //<editor-fold desc="Base facts queries">
    public ArrayList<String> factQuery(String fato) {
        ArrayList<String> instancias = new ArrayList<>();
        try {
            SolveInfo q;
            q = prolog.solve(fato);
            while (q.isSuccess()) {
                String solution = q.toString().replaceAll(" / ", "=").substring(5);
                if (solution != null) {
                    if (!termosRepetidos(q.getBindingVars())) {
                        instancias.add(solution);
                    }
                }
                if (q.hasOpenAlternatives()) {
                    q = prolog.solveNext();
                } else {
                    break;
                }
            }
        } catch (MalformedGoalException | NoSolutionException | NoMoreSolutionException ex) {
            return instancias;
        }
        return instancias;
    }
    
    // This method exchanges the active theory, it is use always when there is a pattern search from the opponent point of view
    // E.g. A player is searching a pattern and wants to know if the opponent has the same pattern
    public void changeTheory() throws CloneNotSupportedException {
        try {
            isMyTheoryActive = !isMyActiveTheory();
            activeBitboard = isMyActiveTheory() ? myBitboard.clone() : yourBitboard.clone();
            prolog.setTheory(isMyActiveTheory() ? myTheory : oponnentTheory);
        } catch (InvalidTheoryException ex) {
            Logger.getLogger(Facts.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // eliminates the instances whose answer has equal terms which characterizes that they are repeated 
    // as Sk1 = Sf1;
    private boolean termosRepetidos(List<Var> values) {
        boolean rep = false;
        ArrayList<Term> termos = new ArrayList<>();
        values.stream().forEach((v) -> {
            termos.add(v.getTerm());
        });
        for (Term t : termos) {
            rep = rep || Collections.frequency(termos, t) > 1;
            if (rep) {
                break;
            }
        }
        return rep;
    }

//</editor-fold>

    public BitBoard getActiveBitboard() {
        return activeBitboard;
    }

    public void setActiveBitboard(BitBoard bb) {
        activeBitboard = bb;
    }

    public boolean isMyActiveTheory() {
        return isMyTheoryActive;
    }
}
