package br.edu.utfpr.stratvision.patlan;

import alice.tuprolog.Prolog;
import br.edu.utfpr.stratvision.persistence.PatternDAO;
import br.edu.utfpr.stratvision.utils.LogFile;
import br.edu.utfpr.stratvision.utilsgui.UtilsGUI;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 *
 * @author Luis Carlos F. Bueno - 16/11/2021
 * This class evaluates (interpret) a HLP searching the pattern in a specific position
 */
public class PatternEvaluator {

    //<editor-fold desc="Fields">
    // in this mode the environment saves the theories in temp files
    public final static int AUTHORING_MODE = 0; 
    // in this mode the theories resides only in memory for better performance, used in pgn bases search
    public final static int SEARCH_MODE = 1; 
    // used in game mode (for future use)
    public final static int GAME_MODE = 2;  // 

    private JTextPane jTextPane;
    private boolean isLogActive;
    private boolean isWhite; // who is searching
    public boolean isConditionMatched; // indicates if the conditions matches in the instance being evaluated
    private Facts bf;
    ScriptEngineManager scrMgr;
    ScriptEngine interpret;
    private int actualInstanceIndex; // index of the prolog instance for conditions evaluation 
    private double boardHeuristicValue;
    private Prolog prolog;
    private final int mode;
    private int proofCount = 0; // counter of functions resulting true
    private GamePhase gamePhase;
    private Pattern searchPattern;
    private HashMap<String, PatternVertex> vertices, verticesClone;
    private final ArrayList<String> functionCalls = new ArrayList<>();
    private LogFile logFile;

    private static Style styleNormal = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
    private static final SimpleAttributeSet styleBoldBlack;  
    private static final SimpleAttributeSet styleBoldBlue;   
    private static final SimpleAttributeSet styleBoldOrange; 
    private static final SimpleAttributeSet styleBoldRed; 

    static {
        styleBoldBlue = new SimpleAttributeSet();
        StyleConstants.setBold(styleBoldBlue, true);
        StyleConstants.setForeground(styleBoldBlue, Color.BLUE);

        styleBoldRed = new SimpleAttributeSet();
        StyleConstants.setBold(styleBoldRed, true);
        StyleConstants.setForeground(styleBoldRed, Color.RED);

        styleBoldOrange = new SimpleAttributeSet();
        StyleConstants.setBold(styleBoldOrange, true);
        StyleConstants.setForeground(styleBoldOrange, Color.ORANGE);

        styleBoldBlack = new SimpleAttributeSet();
        StyleConstants.setBold(styleBoldBlack, true);
        StyleConstants.setForeground(styleBoldBlack, Color.BLACK);
    }
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public PatternEvaluator(LogFile logFile, int mode) throws Exception {
        this.logFile = logFile;
        this.isLogActive = (logFile != null);
        this.mode = mode;
        scrMgr = new ScriptEngineManager();
        interpret = scrMgr.getEngineByName("JavaScript");
    }

    public PatternEvaluator(boolean isWhite, LogFile logFile, JTextPane jText, int mode) throws Exception {
        this(logFile, mode);
        this.isWhite = isWhite;
        this.jTextPane = jText;
        this.prolog = new Prolog();
    }
    //</editor-fold>

    //<editor-fold desc="Evaluation methods">
    // search method for the authoring mode
    // displays messages in the output editor window
    public Pattern evaluatesPattern(Pattern searchPat) throws Exception {
        if (isLogActive) {
            logFile.registerSeparatorLine();
        }
        record("\nSearch pattern [" + searchPat.getName()+ "]", styleNormal);
        record("\nSearch fact [" + searchPat.getToProlog() + "]", styleBoldBlack);
        searchPattern = searchPat;
        ArrayList<String> rawInstances = getBf().factQuery(searchPat.getToProlog());
        ArrayList<ArrayList<String>> instanceMoves = new ArrayList<>();
        vertices = instantiateScenarioShape(rawInstances, searchPat.Instances, instanceMoves);
        record("\nInstances found: " + searchPat.Instances.size(), styleBoldBlack);
        // if there are conditions, evaluates them for each instance of the scenario found
        if (searchPat.getPreConditionToJava().trim().length() > 0 && searchPat.Instances.size() > 0) {
            interpret.put("analisys", this);
            String preCondition = searchPat.getPreConditionToJavaOutput();
            String postCondition = searchPat.getPostConditionToJava();
            BitBoard clone = getBitboard().clone();
            for (int i = 0; i < searchPat.Instances.size(); i++) {
                actualInstanceIndex = i;
                record("\nValidating instance: " + actualInstanceIndex, styleBoldBlack);
                try {
                    isConditionMatched = false;
                    functionCalls.clear();
                    if (!preCondition.trim().isEmpty()) {
                        interpret.eval(preCondition);
                    } else { // if there is no preconditions advances to do tactical moves
                        isConditionMatched = true;
                    }
                    if (isConditionMatched) { 
                        //evaluates post conditions of the pattern
                        if (!postCondition.trim().isEmpty()) {
                            isConditionMatched = false;
                            // applies the tactical moves to the board
                            Facts bfAux = getBf();
                            getBitboard().makePatternMoves(searchPat, vertices, actualInstanceIndex);
                            recalculatesDefenses(getBitboard());
                            System.out.println(getBitboard().drawArray(getBitboard().bits));
                            interpret.eval(postCondition);
                            setBf(bfAux);
                            getBf().setActiveBitboard(clone.clone());
                            getBitboard().undoPatternMoves(instanceMoves.get(i), searchPat.getMoves(), vertices, actualInstanceIndex);
                        } else {
                            isConditionMatched = true;
                        }
                        if (isConditionMatched) {
                            searchPat.OKInstances.add(searchPat.Instances.get(i));
                        }
                    }
                    record("\nEvaluation: " + (isConditionMatched ? "TRUE" : "FALSE"), styleBoldRed);
                    for (int f = 0; f < functionCalls.size(); f++) {
                        record("\n   --> " + functionCalls.get(f), styleBoldRed);
                    }
                } catch (ScriptException e) {
                    record("\nEvaluation error: " + e.getMessage(), styleBoldRed);
                } catch (IllegalArgumentException e) {
                    record("\nEvaluation error: " + e.getMessage(), styleBoldRed);
                }
            }
        } else {
            searchPat.OKInstances.addAll(searchPat.Instances);
        }
        return searchPat;
    }

    //method for the search in pgn game bases 
    public Pattern searchPGNGame(Pattern searchPat, ArrayList<ArrayList<ScenarioAction>> sceneInst, ArrayList<ArrayList<ScenarioAction>> sceneOK) throws Exception {
        try {
            searchPattern = searchPat;
            BitBoard clone = getBitboard().clone();
            ArrayList<String> instbrutas = getBf().factQuery(searchPat.getToProlog());
            if (instbrutas.size() > 0) {
                ArrayList<ArrayList<String>> movtosInst = new ArrayList<>();
                vertices = instantiateScenarioShape(instbrutas, sceneInst, movtosInst);
                // evaluates the pattern defined conditions 
                if (searchPattern.getPreConditionToJava().trim().length() > 0 && sceneInst.size() > 0) {
                    String condition = searchPattern.getPreConditionToJavaOutput();
                    interpret.put("analysis", this);
                    functionCalls.clear();
                    for (int i = 0; i < sceneInst.size(); i++) {
                        actualInstanceIndex = i;
                        try {
                            isConditionMatched = false;
                            functionCalls.add("Processing instance: " + actualInstanceIndex);
                            if (!condition.trim().isEmpty()) {
                                interpret.eval(condition);
                            } else {
                                isConditionMatched = true;
                            }
                            if (isConditionMatched) {
                                if (searchPat.getPostConditionToJava().trim().length() > 0) {
                                    isConditionMatched = false;
                                    Facts bfAux = getBf(); 
                                    getBitboard().makePatternMoves(searchPat, vertices, actualInstanceIndex);
                                    recalculatesDefenses(getBitboard()); 
                                    interpret.eval(searchPat.getPostConditionToJava());
                                    setBf(bfAux); 
                                    getBf().setActiveBitboard(clone.clone()); 
                                    getBitboard().undoPatternMoves(movtosInst.get(i), searchPat.getMoves(), vertices, actualInstanceIndex);
                                } else {
                                    isConditionMatched = true;
                                }
                                if (isConditionMatched) {
                                    sceneOK.add(sceneInst.get(i));
                                    return searchPattern; 
                                }
                            }
                        } catch (ScriptException e) {
                            System.out.println(e.getMessage());
                        } catch (IllegalArgumentException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                } else {
                    sceneOK.addAll(searchPat.Instances);
                }
            }
            return searchPattern;
        } catch (CloneNotSupportedException exception) {
            System.out.println(exception.getMessage());
            throw exception;
        }
    }
    
    /* 
        An idea of strategic heuristic value to be applied to a position, in the trial of estimate how closed to the 
        pattern a position is, considering exclusively the knowledge represented in the pattern and using the following weight
        ponderations:
        40% if the minimal material is present to set the pattern scenario
        30% if the positional factor (scenario and actions) is present:
            - each abstract action of the scenario is checked in incremental fashion;
            - for each existing action on the board it is added 1/n of the total number of actions described. The result is
              multiplied by 0.3;
        30% for the initial conditions: for each condition evaluated as true 1/n is added to the conditional value. The result
            is multiplyed by 0.3;
    */
    public double calculatePattern(Pattern sPattern, ArrayList<ArrayList<ScenarioAction>> rawInstances, ArrayList<ArrayList<ScenarioAction>> matchInstances) throws Exception {
        double result = 0;
        try {
            BitBoard bitClone = getBitboard().clone();
            Pattern pattern = sPattern.clone();

            if (!pattern.getExclusiveSet().isEmpty() && SETOFPIECES(pattern.getExclusiveSet(), bitClone)
                    || (pattern.getExclusiveSet().isEmpty() && MATERIAL(pattern.getST(), bitClone))) {
                result += 0.4 * pattern.getWeight();
            } else {
                return 0;
            }

            if (pattern.isRecursiveSearch()) {
                getBf().createOpponentTheory();
            }
            int inc = pattern.getScenarioActions().size();
            ArrayList<String> instbrutas;
            do {
                instbrutas = getBf().factQuery(pattern.getToProlog());
                if (instbrutas.isEmpty()) {
                    inc--;
                    pattern.getScenarioActions().remove(inc); 
                    if (inc > 0) {
                        pattern.prepareStatement();
                    }
                }
            } while (instbrutas.isEmpty() || inc > 1);
            if (instbrutas.isEmpty()) { 
                return (result + (pattern.getPreConditionToJava().trim().length() == 0 ? 0.3f : 0)) * pattern.getWeight();
            }
            result += (inc / sPattern.getScenarioActions().size() * 0.3f);
            //if all actions were found 
            if (instbrutas.size() > 0 && inc == sPattern.getScenarioActions().size()) {
                proofCount = 0;
                ArrayList<ArrayList<String>> movtosInst = new ArrayList<>();
                vertices = instantiateScenarioShape(instbrutas, rawInstances, movtosInst);
                if (pattern.getPreConditionToJava().trim().length() > 0 && rawInstances.size() > 0) {
                    interpret.put("analisys", this);
                    functionCalls.clear();
                    for (int i = 0; i < rawInstances.size(); i++) {
                        actualInstanceIndex = i;
                        try {
                            functionCalls.add("Processing instance: " + actualInstanceIndex);
                            if (isConditionMatched) {
                                result += 0.3f;
                            } else {
                                inc = 0;
                                for (String s : pattern.getPreConditions()) { 
                                    isConditionMatched = false;
                                    interpret.eval(s);
                                    if (isConditionMatched) {
                                        inc++;
                                    }
                                }
                                result += inc / pattern.getPreConditions().size() * 0.3f;
                            }
                            return result * pattern.getWeight();
                        } catch (ScriptException e) {
                            System.out.println(e.getMessage());
                        } catch (IllegalArgumentException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
            return 0;
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            throw exception;
        }
    }

    private void instantiateMoves(ArrayList<String> instanceMoves, String var, String val) {
        for (int i = 0; i < instanceMoves.size(); i++) {
            instanceMoves.set(i, instanceMoves.get(i).replaceAll("\\b" + var + "\\b", val));
        }
    }

    // the instantiation of the scenario does two important tasks:
    // 1) creates a hashmap for the PatternVertex object (where each actor is a vertex) in order to be used on the conditional functions
    // 2) creates a clone of ScenarioShapeControl ArrayList, replacing each ScenarioShape by the prolog query instantiated values 
    public HashMap<String, PatternVertex> instantiateScenarioShape(ArrayList<String> rawInstances, ArrayList<ArrayList<ScenarioAction>> scenarioInstances, ArrayList<ArrayList<String>> instMoves) throws CloneNotSupportedException {
        //{MF1=mRg1, S2=cg8, S1=spf7, Sk3=skh8, MN1=mNh6}
        HashMap<String, PatternVertex> vs = new HashMap<>();
        ArrayList<HashMap<String, String>> squares = new ArrayList<>();
        ArrayList<ArrayList<ScenarioAction>> scenarioInstAux = new ArrayList<>();
        ArrayList<ArrayList<String>> movesInstAux = new ArrayList<>();
        ArrayList<ScenarioAction> inst;
        ArrayList<String> minst = new ArrayList<>();
        ArrayList<ScenarioAction> cf = searchPattern.getScenarioActions();
        instMoves.clear();

        for (int ix = 0; ix < rawInstances.size(); ix++) {
            String i = rawInstances.get(ix);
            minst.clear();
            minst.addAll(searchPattern.getMoves());
            inst = new ArrayList<>();
            String[] spl = i.split("  ");
            boolean firstj = true;
            for (String j : spl) {
                try {
                    String var = j.substring(0, j.indexOf("=")).trim();
                    String val = j.substring(j.indexOf("=") + 2).trim(); // removes the verifying character of the piece owner
                    if ("0123456789".indexOf(var.charAt(1)) < 0) { // if the 2nd character is not numeric, the variable refers to a instance of a piece
                        var = var.substring(1); //removes the prolog variable character
                    }
                    // instantiate actors and actions
                    for (int ft = 0; ft < cf.size(); ft++) {
                        ScenarioAction fInst;

                        if (firstj) {
                            fInst = (ScenarioAction)cf.get(ft).clone();
                            inst.add(fInst);
                        } else {
                            fInst = inst.get(ft);
                        }
                        if (cf.get(ft).notAction) {
                            continue; // ignores when it is a not action (the action can not be in the scenario)
                        }
                        if (fInst.leftActorSource.equals(var)) {
                            fInst.leftActor = val;
                        }
                        for (int fd = 0; fd < fInst.rightActor.size(); fd++) {
                            if (fInst.rightActorSource.get(fd).equals(var)) {
                                fInst.rightActor.set(fd, val);
                            }
                        }
                    }
                    firstj = false;
                } catch (CloneNotSupportedException e) {
                    throw e;
                }
            }
            scenarioInstAux.add(inst);
            movesInstAux.add(minst);
        }

        // checks if all instantiated values already belongs to some other processed instance
        // and cancels the inclusion to the final instances
        // in other words, ignores if already exists an equivalent instance
        // but this task is only executed in authoring mode for performance reasons
        int instProc = 0;
        for (int ft = 0; ft < scenarioInstAux.size(); ft++) {
            inst = scenarioInstAux.get(ft);
            minst = movesInstAux.get(ft);
            squares.add(new HashMap<>());
            for (ScenarioAction f : inst) {
                String val = f.leftActor;
                if (!squares.get(squares.size() - 1).containsKey(val.substring(val.length() - 2))) {
                    squares.get(squares.size() - 1).put(val.substring(val.length() - 2), val.substring(val.length() - 2));
                }
                for (int fd = 0; fd < f.rightActor.size(); fd++) {
                    val = f.rightActor.get(fd);
                    if (!squares.get(squares.size() - 1).containsKey(val.substring(val.length() - 2))) {
                        squares.get(squares.size() - 1).put(val.substring(val.length() - 2), val.substring(val.length() - 2));
                    }
                }
            }

            if (mode == AUTHORING_MODE) {
                boolean equivalent = false;
                for (int ia = 0; ia < squares.size() - 1; ia++) {
                    equivalent = true;
                    for (Map.Entry<String, String> c : squares.get(squares.size() - 1).entrySet()) {
                        equivalent = equivalent && squares.get(ia).containsKey(c.getKey());
                    }
                    if (equivalent) {
                        break;
                    }
                }
                if (!equivalent) {
                    scenarioInstances.add(inst);
                    instMoves.add(minst);
                    addVertex(vs, inst, instProc);
                    instProc++;
                }
            } else {
                scenarioInstances.add(inst);
                instMoves.add(minst);
                addVertex(vs, inst, instProc);
                instProc++;
            }
        }
        return vs;
    }

    private void addVertex(HashMap<String, PatternVertex> vs, ArrayList<ScenarioAction> inst, int instProc) {
        PatternVertex v;
        String val, var, key;
        int x = -1;
        for (ScenarioAction f : inst) {
            x++;
            val = f.leftActor;
            var = f.leftActorSource;
            key = "I_" + instProc + "_" + var;
            if (!vs.containsKey(key)) {
                v = new PatternVertex();
                v.setValue(val);
                if (Character.isUpperCase(val.charAt(0))) {
                    v.setOwner(ScenarioAction.PieceOwner.ME);
                } else {
                    v.setOwner(ScenarioAction.PieceOwner.OPPONENT);
                }
                // ignores any error when the variable wasn't instantiated by prolog
                // because it was found on other alternative solutions on query disjunctions
                try { 
                    v.setSquare(Square.getByAlgebric(val));
                    v.setTipoPeca(translatePieceType(v.getSAN()));
                    v.setPiece(translatePiece(v.getSAN()));
                    vs.put(key, v);
                } catch (Exception e) {
                }
            }
            int ifd = 0;
            for (String fd : f.rightActor) {
                val = fd;
                var = f.rightActorSource.get(ifd++);
                key = "I_" + instProc + "_" + var;
                if (!vs.containsKey(key)) {
                    v = new PatternVertex();
                    v.setValue(val);
                    if (val.length() > 2) {
                        if (Character.isUpperCase(val.charAt(0))) {
                            v.setOwner(ScenarioAction.PieceOwner.ME);
                        } else {
                            v.setOwner(ScenarioAction.PieceOwner.OPPONENT);
                        }
                    } else {
                        v.setOwner(ScenarioAction.PieceOwner.ANYONE);
                    }
                    // ignores any error when the variable wasn't instantiated by prolog
                    // because it was found on other alternative solutions on query disjunctions
                    try { 
                        v.setSquare(Square.getByAlgebric(val));
                        v.setTipoPeca(translatePieceType(v.getSAN()));
                        v.setPiece(translatePiece(v.getSAN()));
                        vs.put(key, v);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private void record(String texto, AttributeSet estiloTexto) {
        if (logFile != null) {
            logFile.registerMessageWithoutCR(texto);
        }
        if (jTextPane != null) {
            UtilsGUI.addFormatedText(jTextPane, texto, estiloTexto);
        }
    }

    private void recordBoardValue() {
        record(String.valueOf(boardHeuristicValue), boardHeuristicValue > 0 ? styleBoldBlue
                : boardHeuristicValue < 0 ? styleBoldRed : styleBoldOrange);
    }



    public void createFactsBase(BitBoard bitboard) {

        setBf(new Facts(isWhite, prolog, gamePhase, mode));
        try {
            getBf().createMyTheory(bitboard);
            if (mode == AUTHORING_MODE) {
                getBf().createOpponentTheory();
            }
        } catch (Exception ex) {
            Logger.getLogger(PatternEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void recalculatesDefenses(BitBoard bitboard) {
        bitboard.resetDefenses();
        setBf(new Facts(isWhite, prolog, gamePhase, mode));
        try {
            getBf().createMyTheory(bitboard);
            if (mode == AUTHORING_MODE) {
                getBf().createOpponentTheory();
            }
        } catch (Exception ex) {
            Logger.getLogger(PatternEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String instantiatedVertex(String instVertex) {
        return "I_" + actualInstanceIndex + "_" + instVertex;
    }

    private PieceType translatePieceType(String arg) {
        PieceType t = null;
        switch (arg.toUpperCase()) {
            case "K":
            case "KING":
            case "REI":
                t = PieceType.KING;
                break;
            case "Q":
            case "QUEEN":
            case "DAMA":
                t = PieceType.QUEEN;
                break;
            case "R":
            case "ROOK":
            case "TORRE":
                t = PieceType.ROOK;
                break;
            case "B":
            case "BISHOP":
            case "BISPO":
                t = PieceType.BISHOP;
                break;
            case "N":
            case "KNIGHT":
            case "CAVALO":
                t = PieceType.KNIGHT;
                break;
            case "P":
            case "PAWN":
            case "PEAO":
                t = PieceType.PAWN;
                break;
        }
        return t;
    }
    
    private Piece translatePiece(String san) {
        Piece t = null;
        switch (san.toUpperCase()) {
            case "K":
            case "KING":
            case "REI":
                t = isWhite ? Piece.WHITE_KING : Piece.BLACK_KING;
                break;
            case "Q":
            case "QUEEN":
            case "DAMA":
                t = isWhite ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN;
                break;
            case "R":
            case "ROOK":
            case "TORRE":
                t = isWhite ? Piece.WHITE_ROOK : Piece.BLACK_ROOK;
                break;
            case "B":
            case "BISHOP":
            case "BISPO":
                t = isWhite ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP;
                break;
            case "N":
            case "KNIGHT":
            case "CAVALO":
                t = isWhite ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
                break;
            case "P":
            case "PAWN":
            case "PEAO":
                t = isWhite ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
                break;
        }
        return t;
    }

    public void dispose() {
        prolog.clearTheory();
        prolog = null;
        setBf(null);
    }

    //</editor-fold>
    
    //<editor-fold desc="PATLAN PREDEFINED FUNCTIONS">
    
    //<editor-fold desc="Conditional boolean functions">
    public boolean WHITEPIECES() {
        return isWhite;
    }

    public boolean BLACKPIECES() {
        return !isWhite;
    }
   
    public boolean ONEDGE(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        boolean res = (v.getSquare().getColumn() == 1 || v.getSquare().getColumn() == 8
                || v.getSquare().getLine() == 1 || v.getSquare().getLine() == 8);
        getFunctionCalls().add("ONEDGE(" + args + ") = " + res);
        return res;
    }

    public boolean ONCORNER(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        boolean res = ((v.getSquare().getColumn() == 1 && v.getSquare().getLine() == 1)
                || (v.getSquare().getColumn() == 1 && v.getSquare().getLine() == 8)
                || (v.getSquare().getColumn() == 8 && v.getSquare().getLine() == 1)
                || (v.getSquare().getColumn() == 8 && v.getSquare().getLine() == 8));
        getFunctionCalls().add("ONCORNER(" + args + ") = " + res);
        return res;
    }
    
    public boolean TYPEOF(String args) throws IllegalArgumentException {
        if (args.split(",").length != 2) {
            throw new IllegalArgumentException("Function TYPEOF needs 2 parameters: " + args);
        }
        String vn = args.split(",")[0];
        PieceType type = translatePieceType(args.split(",")[1]);
        PatternVertex v = vertices.get(instantiatedVertex(vn));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + vn);
        }
        boolean res = v.getPieceType() == type;
        getFunctionCalls().add("TYPEOF(" + args + ") = " + res);
        return res;
    }
    
    // method used to identify end game patterns for pawn promotions
    public boolean INTHESQUARE(String args) { 
        boolean res = true;
        int dp; // distance to promotion
        Square promo;
        String[] arg;
        arg = args.split(",");
        PatternVertex v1 = vertices.get(instantiatedVertex(arg[0]));
        PatternVertex v2 = vertices.get(instantiatedVertex(arg[1]));

        int king2move = 0; // determine if the king will do the next move
        if (v1.getPieceType() == PieceType.PAWN) { //the square for promotion
            if ((isWhite && v2.getSAN().equals("K"))
                    || (!isWhite && v2.getSAN().equals("k"))) {
                king2move = 1;
            }
            if (v1.getSAN().equals("P")) { // if it is white pawn
                promo = Square.getByIndexes(v1.getSquare().getColumn() - 1, 7);
            } else {
                promo = Square.getByIndexes(v1.getSquare().getColumn() - 1, 0);
            }
            dp = PatternEvaluator.this.MAXDISTANCE(promo, v1.getSquare());
            res = !(Math.min(5, dp) < (PatternEvaluator.this.MAXDISTANCE(v2.getSquare(), promo) - king2move));
        } else if (v2.getPieceType() == PieceType.PAWN) { //determines the square for promotion
            if ((isWhite && v1.getSAN().equals("K"))
                    || (!isWhite && v1.getSAN().equals("k"))) {
                king2move = 1;
            }
            if (v2.getSAN().equals("P")) { // if it is white pawn
                promo = Square.getByIndexes(v2.getSquare().getColumn() - 1, 7);
            } else {
                promo = Square.getByIndexes(v2.getSquare().getColumn() - 1, 0);
            }
            dp = PatternEvaluator.this.MAXDISTANCE(promo, v2.getSquare());
            res = !(Math.min(5, dp) < (PatternEvaluator.this.MAXDISTANCE(v1.getSquare(), promo) - king2move));
        }
        getFunctionCalls().add("INTHESQUARE(" + args + ") = " + res);
        return res;
    }

    public boolean SQUAREOWNER(String args) {
        String vn = args.split(",")[0];
        String dono = args.split(",")[1];
        PatternVertex v = vertices.get(instantiatedVertex(vn));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        boolean res = false;
        if ((("EU".equals(dono) || "ME".equals(dono)) && v.getOwner() == ScenarioAction.PieceOwner.ME)) {
            res = true;
        } else if ((("OPONENTE".equals(dono) || "OPPONENT".equals(dono)) && v.getOwner() == ScenarioAction.PieceOwner.OPPONENT)) {
            res = true;
        } else if (v.getOwner() == ScenarioAction.PieceOwner.ANYONE) {
            res = true;
        }
        getFunctionCalls().add("SQUAREOWNER(" + args + ") = " + res);
        return res;
    }
    
    public boolean EXISTINFILE(String pieceType, int file) throws IllegalArgumentException {
        for (int ix = 0; ix < 8; ix++) {
            String pAux = getBitboard().getPiece(file, ix);
            if ((!pAux.equals(" ") && pAux.toUpperCase().equals(pieceType.toUpperCase()))
                    || "F".equals(pieceType.toUpperCase())) {
                getFunctionCalls().add("EXISTINFILE(" + pieceType + "," + file + ") = true");
                return true;
            }
        }
        getFunctionCalls().add("EXISTINFILE(" + pieceType + "," + file + ") = false");
        return false;
    }

    public boolean EXISTINRANK(String pieceType, int rank) throws IllegalArgumentException {
        for (int ix = 0; ix < 8; ix++) {
            String pAux = getBitboard().getPiece(ix, rank);
            if (!pAux.equals(" ")
                    && (pAux.toUpperCase().equals(pieceType.toUpperCase())
                    || "F".equals(pieceType.toUpperCase()))) {
                getFunctionCalls().add("EXISTINRANK(" + pieceType + "," + rank + ") = true");
                return true;
            }
        }
        getFunctionCalls().add("EXISTINRANK(" + pieceType + "," + rank + ") = true");
        return false;
    }

    public boolean MATERIAL(HashMap<String, PATLANParser.TS> st, BitBoard bitboard) {
        boolean res = true;
        Iterator i = st.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry node = ((Map.Entry) i.next());
            PATLANParser.TS ts = (PATLANParser.TS) node.getValue();
            if (ts.NotAction) { //ignores when the is a negation action and goes to the next symbol
                continue; 
            }                                       
            // if the action components are required in the scenario
            // they must appear in other action or in the actors declaration
            if (ts.Category == PATLANParser.FIGURE) {
                if (ts.Parameters.equals("OR")) { // uma peça ou outra
                    switch (ts.Key.charAt(0)) {
                        case 'Q':
                            res = res || Long.bitCount(bitboard.bits.MQ) > 0;
                            break;
                        case 'R':
                            res = res || Long.bitCount(bitboard.bits.MR) > 0;
                            break;
                        case 'N':
                            res = res || Long.bitCount(bitboard.bits.MN) > 0;
                            break;
                        case 'B':
                            res = res || Long.bitCount(bitboard.bits.MB) > 0;
                            break;
                        case 'P':
                            res = res || Long.bitCount(bitboard.bits.MP) > 0;
                            break;
                        case 'q':
                            res = res || Long.bitCount(bitboard.bits.SQ) > 0;
                            break;
                        case 'r':
                            res = res || Long.bitCount(bitboard.bits.SR) > 0;
                            break;
                        case 'n':
                            res = res || Long.bitCount(bitboard.bits.SN) > 0;
                            break;
                        case 'b':
                            res = res || Long.bitCount(bitboard.bits.SB) > 0;
                            break;
                        case 'p':
                            res = res || Long.bitCount(bitboard.bits.SP) > 0;
                            break;
                    }
                } else {
                    switch (ts.Key.charAt(0)) {
                        case 'Q':
                            res = res && Long.bitCount(bitboard.bits.MQ) > 0;
                            break;
                        case 'R':
                            res = res && Long.bitCount(bitboard.bits.MR) > 0;
                            break;
                        case 'N':
                            res = res && Long.bitCount(bitboard.bits.MN) > 0;
                            break;
                        case 'B':
                            res = res && Long.bitCount(bitboard.bits.MB) > 0;
                            break;
                        case 'P':
                            res = res && Long.bitCount(bitboard.bits.MP) > 0;
                            break;
                        case 'q':
                            res = res && Long.bitCount(bitboard.bits.SQ) > 0;
                            break;
                        case 'r':
                            res = res && Long.bitCount(bitboard.bits.SR) > 0;
                            break;
                        case 'n':
                            res = res && Long.bitCount(bitboard.bits.SN) > 0;
                            break;
                        case 'b':
                            res = res && Long.bitCount(bitboard.bits.SB) > 0;
                            break;
                        case 'p':
                            res = res && Long.bitCount(bitboard.bits.SP) > 0;
                            break;
                    }
                }
            }
        }
        getFunctionCalls().add("MATERIAL() = " + res);
        return res;
    }

    public boolean SETOFPIECES(HashSet<Character> setOfPieces, BitBoard bitboard) {
        boolean result;
        HashSet<Character> board = new HashSet<>();
        if (setOfPieces.isEmpty()) {
            return true;
        }
        // creates the set of pieces on the board
        board.add('K');
        board.add('k');
        if (bitboard.bits.MQ != 0) {
            board.add('Q');
        }
        if (bitboard.bits.MR != 0) {
            board.add('R');
        }
        if (bitboard.bits.MN != 0) {
            board.add('N');
        }
        if (bitboard.bits.MB != 0) {
            board.add('B');
        }
        if (bitboard.bits.MP != 0) {
            board.add('P');
        }
        if (bitboard.bits.SQ != 0) {
            board.add('q');
        }
        if (bitboard.bits.SR != 0) {
            board.add('r');
        }
        if (bitboard.bits.SN != 0) {
            board.add('n');
        }
        if (bitboard.bits.SB != 0) {
            board.add('b');
        }
        if (bitboard.bits.SP != 0) {
            board.add('p');
        }
        result = isIdenticalHashSet(setOfPieces, board);
        return result;
    }

    private boolean isIdenticalHashSet(HashSet<Character> h1, HashSet<Character> h2) {
        if (h1.size() != h2.size()) {
            return false;
        }
        for (Character A : h1) {
            if (h2.contains(A)) {
                h2.remove(A);
            } else {
                return false;
            }
        }
        return true; // will only return true if sets are equal
    }

    // returns true if there is a specific pattern in the position
    public boolean EXISTPATTERN(String args) throws Exception, IllegalArgumentException {
        try {
            PATLANParser parser;

            String patternFile = args.split(",")[0];
            String owner = args.split(",")[1];
            Pattern old = this.searchPattern;
            Pattern pb;

            try {
                pb = PatternDAO.loadPattern(patternFile);
            } catch(IOException e) {
                throw new IllegalArgumentException("Pattern file not found: " + patternFile);
            }
            // changes the player case the search is for a HLP from the opponent point of view
            if (owner.startsWith("OP")) { 
                isWhite = !isWhite;
                getBf().changeTheory();
            }
            String patternSource = pb.getSource();
            InputStream stream = new ByteArrayInputStream(patternSource.getBytes(StandardCharsets.UTF_8));
            parser = new PATLANParser(stream);
            parser.pattern = pb;
            parser.Patterns();
            pb.setST(parser.ST);
            pb.prepareStatement();

            PatternEvaluator avalia = new PatternEvaluator(isWhite,
                    null, jTextPane, PatternEvaluator.AUTHORING_MODE);
            avalia.setGamePhase(gamePhase);
            avalia.setBf(this.getBf());
            avalia.evaluatesPattern(pb);
            if (owner.startsWith("OP")) { //restores the state of the search to the actual player
                isWhite = !isWhite;
                getBf().changeTheory();
                this.searchPattern = old;
            } 
            boolean res = pb.Instances.size() > 0;
            getFunctionCalls().add("EXISTPATTERN(" + args + ") = " + res);
            return res;
        } catch (Exception exception) {
            throw exception;
        }
    }

    public boolean KINGOPPOSITION(String args) {
        boolean result = false;
        String[] arg;
        arg = args.split(",");
        PatternVertex v1 = vertices.get(instantiatedVertex(arg[0]));
        PatternVertex v2 = vertices.get(instantiatedVertex(arg[1]));
        //verifies if the kings are in the same file, rank or diagonal
        if (v1.getSquare().getLine() == v2.getSquare().getLine()) {
            result = Math.abs(v1.getSquare().getColumn() - v2.getSquare().getColumn() - 1) % 2 == 1;
        } else if (v1.getSquare().getColumn() == v2.getSquare().getColumn()) {
            result = Math.abs(v1.getSquare().getLine() - v2.getSquare().getLine() - 1) % 2 == 1;
        } else if (Math.abs(v1.getSquare().getLine() - v2.getSquare().getLine())
                == Math.abs(v1.getSquare().getColumn() - v2.getSquare().getColumn())) {
            result = Math.abs(v1.getSquare().getLine() - v2.getSquare().getLine() - 1) % 2 == 1;
        }
        getFunctionCalls().add("KINGOPPOSITION(" + args + ") = " + result);
        return result;
    }

    public boolean ISPAWNSBLOCKED(String args) {
        boolean res = true;
        String pawn;
        boolean me = args.toUpperCase().equals("EU")
                || args.toUpperCase().equals("ME");
        if (me) {
            pawn = isWhite ? "P" : "p";
        } else {
            pawn = isWhite ? "p" : "P";
        }

        for (int ix = 0; ix < 8; ix++) {
            for (int ic = 0; ic < 8; ic++) {
                if (getBf().getActiveBitboard().getPiece(ic, ix).equals(pawn)) {
                    res = res && ((sentry(ic, ix, pawn.equals("P")) > 0)
                            || (ram(ic, ix, pawn.equals("P")) > 0));
                }
            }
        }
        getFunctionCalls().add("ISPAWNSBLOCKED(" + args + ") = " + res);
        return res;
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Numeric functions">
    public int VALUEOF(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v != null) {
            if (v.getPieceType() == null) {
                return 0; //vertex is an empty value
            }
            args = v.getSAN();
        } else {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        int value = 0;
        switch (args.substring(0, 1).toUpperCase()) {
            case "B":
                value = gamePhase.getBishopValue();
                break;
            case "N":
                value = gamePhase.getKnightValue();
                break;
            case "Q":
                value = gamePhase.getQueenValue();
                break;
            case "P":
                value = gamePhase.getPawnValue();
                break;
            case "R":
                value = gamePhase.getRookValue();
                break;
            case "K":
                value = Integer.MAX_VALUE;
                break;
        }
        getFunctionCalls().add("VALUEOF(" + args + ") = " + value);
        return value;
    }

    public double RELATIVEVALUEOF(String args) {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        int col = v.getSquare().getColumn() - 1, lin = 7 - (v.getSquare().getLine() - 1);
        double value = VALUEOF(args) * ((col + lin * 8) + 1);
        getFunctionCalls().add("RELATIVEVALUEOF(" + args + ") = " + value);
        return value;
    }
    
    public int FILEOF(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        int res = v.getSquare().getColumn();
        getFunctionCalls().add("FILEOF(" + args + ") = " + res);
        return res;
    }

    public int RANKOF(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        int res = v.getSquare().getLine();
        getFunctionCalls().add("RANKOF(" + args + ") = " + res);
        return res;
    }

    public int MYDEFENSES(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        int res = getBitboard().getMyDefenses(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("MYDEFENSES(" + args + ") = " + res);
        return res;
    }

    public int YOURDEFENSES(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        int res = getBitboard().getYourDefenses(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("YOURDEFENSES(" + args + ") = " + res);
        return res;
    }

    public double MYDEFENSESVALUE(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        double res = getBitboard().getMyDefensesValue(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("MYDEFENSESVALUE(" + args + ") = " + res);
        return res;
    }

    public double YOURDEFENSESVALUE(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));

        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        double res = getBitboard().getYourDefensesValue(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("YOURDEFENSESVALUE(" + args + ") = " + res);
        return res;
    }

    public int DEFENSESOF(String args) throws IllegalArgumentException {
        String vn = args.split(",")[0];
        String owner = args.split(",")[1];
        PatternVertex v = vertices.get(instantiatedVertex(vn));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        int res = "EU".equals(owner) || "ME".equals(owner)
                ? getBitboard().getMyDefenses(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex())
                : getBitboard().getYourDefenses(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("DEFENSESOF(" + args + ") = " + res);
        return res;
    }

    // returns the number of pieces defending a piece of the type argument
    public int PIECESDEFENDING(String args) throws IllegalArgumentException {
        if (args.split(",").length != 2) {
            throw new IllegalArgumentException("Bad argument exception, two arguments are needed: " + args);
        }
        String vn = args.split(",")[0];
        String type = args.split(",")[1];
        PatternVertex v = vertices.get(instantiatedVertex(vn));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + vn);
        }
        int res = getBitboard().getDefendingPiecesByType(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex(), type);
        getFunctionCalls().add("DEFENDINGPIECES(" + args + ") = " + res);
        return res;
    }

    public int DIRECTDEFENSESOF(String args) throws IllegalArgumentException {
        String vn = args.split(",")[0];
        String owner = args.split(",")[1];
        PatternVertex v = vertices.get(instantiatedVertex(vn));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        int res = "EU".equals(owner) || "ME".equals(owner)
                ? getBitboard().getMyDirectDefenses(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex())
                : getBitboard().getYourDirectDefenses(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("DIRECTDEFENSESOF(" + args + ") = " + res);
        return res;
    }

    public double DEFENSESVALUEOF(String args) throws IllegalArgumentException {
        String vn = args.split(",")[0];
        String owner = args.split(",")[1];
        PatternVertex v = vertices.get(instantiatedVertex(vn));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        double res = "EU".equals(owner) || "ME".equals(owner)
                ? getBitboard().getMyDefensesValue(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex())
                : getBitboard().getYourDefensesValue(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("DEFENSESVALUEOF(" + args + ") = " + res);
        return res;
    }

    public double DIRECTDEFENSESVALUEOF(String args) throws IllegalArgumentException {
        String vn = args.split(",")[0];
        String owner = args.split(",")[1];
        PatternVertex v = vertices.get(instantiatedVertex(vn));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        double res = "EU".equals(owner) || "ME".equals(owner)
                ? getBitboard().getMyDirectDefensesValue(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex())
                : getBitboard().getYourDirectDefensesValue(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("DIRECTDEFENSESVALUEOF(" + args + ") = " + res);
        return res;
    }

    public double RELDEFENSESVALUEOF(String args) throws IllegalArgumentException {
        String vn = args.split(",")[0];
        String owner = args.split(",")[1];
        PatternVertex v = vertices.get(instantiatedVertex(vn));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        double res = "EU".equals(owner) || "ME".equals(owner)
                ? getBitboard().getMyKingDefensesRelativeValue(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex())
                : getBitboard().getYourKingDefensesRelativeValue(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("RELDEFENSESVALUEOF(" + args + ") = " + res);
        return res;
    }

    public int MYDIRDEFENSES(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        int res = getBitboard().getMyDirectDefenses(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("MYDIRDEFENSES(" + args + ") = " + res);
        return res;
    }

    public int YOURDIRDEFENSES(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        int res = getBitboard().getYourDirectDefenses(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("YOURDIRDEFENSES(" + args + ") = " + res);
        return res;
    }

    public double MYDIRDEFENSESVALUE(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        double res = getBitboard().getMyDirectDefensesValue(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("MYDIRDEFENSESVALUE(" + args + ") = " + res);
        return res;
    }

    public double YOURDIRDEFENSESVALUE(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        double res = getBitboard().getYourDirectDefensesValue(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("YOURDIRDEFENSESVALUE(" + args + ") = " + res);
        return res;
    }

    public double MYRELDEFENSESVALUE(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        double res = getBitboard().getMyKingDefensesRelativeValue(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("MYRELDEFENSESVALUE(" + args + ") = " + res);
        return res;
    }

    public double YOURRELDEFENSESVALUE(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        if (v == null) {
            throw new IllegalArgumentException("Action variable cannot be found: " + args);
        }
        double res = getBitboard().getYourKingDefensesRelativeValue(v.getSquare().getColumnIndex(), v.getSquare().getLineIndex());
        getFunctionCalls().add("YOURELDEFENSESVALUE(" + args + ") = " + res);
        return res;
    }

    public int RANKDISTANCE(String args) {
        int res;
        String[] arg;
        arg = args.split(",");
        PatternVertex v1 = vertices.get(instantiatedVertex(arg[0]));
        PatternVertex v2 = vertices.get(instantiatedVertex(arg[1]));
        res = Math.abs(v1.getSquare().getLine() - v2.getSquare().getLine());
        getFunctionCalls().add("RANKDISTANCE(" + args + ") = " + res);
        return res;
    }

    public int FILEDISTANCE(String args) {
        int res;
        String[] arg;
        arg = args.split(",");
        PatternVertex v1 = vertices.get(instantiatedVertex(arg[0]));
        PatternVertex v2 = vertices.get(instantiatedVertex(arg[1]));
        res = Math.abs(v1.getSquare().getColumn() - v2.getSquare().getColumn());
        getFunctionCalls().add("FILEDISTANCE(" + args + ") = " + res);
        return res;
    }

    public int MANHATTANDISTANCE(String args) { //distância manhattan
        int res;
        String[] arg;
        arg = args.split(",");
        PatternVertex v1 = vertices.get(instantiatedVertex(arg[0]));
        PatternVertex v2 = vertices.get(instantiatedVertex(arg[1]));
        res = Math.abs(v1.getSquare().getLine() - v2.getSquare().getLine())
                + Math.abs(v1.getSquare().getColumn() - v2.getSquare().getColumn());
        getFunctionCalls().add("MANHATTANDISTANCE(" + args + ") = " + res);
        return res;
    }

    public int MAXDISTANCE(Square c1, Square c2) {
        int res;
        res = Math.max(Math.abs(c1.getLine() - c2.getLine()),
                Math.abs(c1.getColumn() - c2.getColumn()));
        getFunctionCalls().add("MAXDISTANCE(" + c1.toString() + "," + c2.toString() + ") = " + res);
        return res;
    }

    public int MAXDISTANCE(String args) { // conhecida como distância Chebyshev
        int res;
        String[] arg;
        arg = args.split(",");
        PatternVertex v1 = vertices.get(instantiatedVertex(arg[0]));
        PatternVertex v2 = vertices.get(instantiatedVertex(arg[1]));
        res = Math.max(Math.abs(v1.getSquare().getLine() - v2.getSquare().getLine()),
                Math.abs(v1.getSquare().getColumn() - v2.getSquare().getColumn()));
        getFunctionCalls().add("MAXDISTANCE(" + args + ") = " + res);
        return res;
    }

    public int MINDISTANCE(String args) {
        int res;
        String[] arg;
        arg = args.split(",");
        PatternVertex v1 = vertices.get(instantiatedVertex(arg[0]));
        PatternVertex v2 = vertices.get(instantiatedVertex(arg[1]));
        res = Math.min(Math.abs(v1.getSquare().getLine() - v2.getSquare().getLine()),
                Math.abs(v1.getSquare().getColumn() - v2.getSquare().getColumn()));
        getFunctionCalls().add("MINDISTANCE(" + args + ") = " + res);
        return res;
    }

    public int SQUARECOLOROF(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));

        if (v == null) { // if the arg doesn't belong to an element of the scenario
            int res = 0;
            boolean isMyPiece = "SFKQRNBP".contains(args); // UpperCase chars indicates pieces of the player who is searching
            switch (args.toUpperCase()) {
                case "K":
                    res = isMyPiece
                            ? ((BitBoard.WHITESQUARES & getBitboard().bits.MK) == getBitboard().bits.MK ? 0 : 1)
                            : ((BitBoard.WHITESQUARES & getBitboard().bits.SK) == getBitboard().bits.SK ? 0 : 1);
                    break;
                case "Q":
                    res = isMyPiece
                            ? ((BitBoard.WHITESQUARES & getBitboard().bits.MQ) == getBitboard().bits.MQ ? 0 : 1)
                            : ((BitBoard.WHITESQUARES & getBitboard().bits.SQ) == getBitboard().bits.SQ ? 0 : 1);
                    break;
                case "R":
                    res = isMyPiece
                            ? ((BitBoard.WHITESQUARES & getBitboard().bits.MR) == getBitboard().bits.MR ? 0 : 1)
                            : ((BitBoard.WHITESQUARES & getBitboard().bits.SR) == getBitboard().bits.SR ? 0 : 1);
                    break;
                case "N":
                    res = isMyPiece
                            ? ((BitBoard.WHITESQUARES & getBitboard().bits.MN) == getBitboard().bits.MN ? 0 : 1)
                            : ((BitBoard.WHITESQUARES & getBitboard().bits.SN) == getBitboard().bits.SN ? 0 : 1);
                    break;
                case "B":
                    res = isMyPiece
                            ? ((BitBoard.WHITESQUARES & getBitboard().bits.MB) == getBitboard().bits.MB ? 0 : 1)
                            : ((BitBoard.WHITESQUARES & getBitboard().bits.SB) == getBitboard().bits.SB ? 0 : 1);
                    break;
                case "P":
                    res = isMyPiece
                            ? ((BitBoard.WHITESQUARES & getBitboard().bits.MP) == getBitboard().bits.MP ? 0 : 1)
                            : ((BitBoard.WHITESQUARES & getBitboard().bits.SP) == getBitboard().bits.SP ? 0 : 1);
                    break;
                case "F":
                case "S":
                    res = isMyPiece
                            ? ((BitBoard.WHITESQUARES & getBitboard().bits.MY_PIECES) == getBitboard().bits.MY_PIECES ? 0 : 1)
                            : ((BitBoard.WHITESQUARES & getBitboard().bits.YOUR_PIECES) == getBitboard().bits.YOUR_PIECES ? 0 : 1);
                    break;
                default: {
                    throw new IllegalArgumentException("Variável de formato/tipo de peça inválido: " + args);
                }
            }
            getFunctionCalls().add("SQUARECOLOROF(" + args + ") = " + res);
            return res;
        } else {
            int res = v.getSquare().isWhite() ? 0 : 1;
            getFunctionCalls().add("SQUARECOLOROF(" + args + ") = " + res);
            return res;
        }
    }
    
    public int MOVESOF(String args) throws IllegalArgumentException {
        PatternVertex v;
        int res;
        boolean isMyPiece;
        long moves = 0L;
        v = vertices.get(instantiatedVertex(args)); // checks if the arg is present on the scenario
        if (v != null) {
            moves = getBitboard().directAttacksLong(v.getSAN(), v.getSquare().getLine() - 1,
                    v.getSquare().getColumn() - 1);
            res = getBitboard().countSetBits(moves);
            getFunctionCalls().add("MOVESOF(" + args + ") = " + res);
            return res;
        } else {
            isMyPiece = "KQRNBP".contains(args); // letras maiúsculas sinalizam peça do jogador que busca o padrão
            if ((isMyPiece && isWhite) || (!isMyPiece && !isWhite)) {
                getBitboard().initWhite();
                switch (args.toUpperCase()) {
                    case "K":
                        moves = getBitboard().possibleKSecureLong(getBitboard().unsafeforW, getBitboard().bits.WK);
                        break;
                    case "Q":
                        moves = getBitboard().possibleQLong(getBitboard().bits.WQ);
                        break;
                    case "R":
                        moves = getBitboard().possibleRLong(getBitboard().bits.WR);
                        break;
                    case "N":
                        moves = getBitboard().possibleNLong(getBitboard().bits.WN);
                        break;
                    case "B":
                        moves = getBitboard().possibleBLong(getBitboard().bits.WB);
                        break;
                    case "P":
                        moves = getBitboard().possibleWPLong(getBitboard().bits.WP, getBitboard().bits.BP, getBitboard().bits.EP);
                        break;
                }
            } else if ((isMyPiece && !isWhite) || (!isMyPiece && isWhite)) {
                getBitboard().initBlack();
                switch (args.toUpperCase()) {
                    case "K":
                        moves = getBitboard().possibleKSecureLong(getBitboard().unsafeforB, getBitboard().bits.BK);
                        break;
                    case "Q":
                        moves = getBitboard().possibleQLong(getBitboard().bits.BQ);
                        break;
                    case "R":
                        moves = getBitboard().possibleRLong(getBitboard().bits.BR);
                        break;
                    case "N":
                        moves = getBitboard().possibleNLong(getBitboard().bits.BN);
                        break;
                    case "B":
                        moves = getBitboard().possibleBLong(getBitboard().bits.BB);
                        break;
                    case "P":
                        moves = getBitboard().possibleBPLong(getBitboard().bits.BP, getBitboard().bits.WP, getBitboard().bits.EP);
                        break;
                }
            }
            res = getBitboard().countSetBits(moves);
            getFunctionCalls().add("MOVESOF(" + args + ") = " + res);
            return res;
        }
    }

    public int SAFEMOVESOF(String args) throws IllegalArgumentException {
        PatternVertex v;
        int res;
        boolean minhaPeca;
        long moves = 0L;
        v = vertices.get(instantiatedVertex(args)); 
        if (v != null) {
            moves = getBitboard().directAttacksLong(v.getSAN(), v.getSquare().getLine() - 1, v.getSquare().getColumn() - 1);
            res = getBitboard().countSetBits(moves);
            getFunctionCalls().add("SAFEMOVESOF(" + args + ") = " + res);
            return res;
        } else {
            minhaPeca = "KQRNBP".contains(args); 
            if ((minhaPeca && isWhite) || (!minhaPeca && !isWhite)) {
                getBitboard().initWhite();
                switch (args) {
                    case "K":
                        moves = getBitboard().possibleKSecureLong(getBitboard().unsafeforW, getBitboard().bits.WK);
                        break;
                    case "Q":
                        moves = getBitboard().possibleQSecureLong(getBitboard().unsafeforW, getBitboard().bits.WQ);
                        break;
                    case "R":
                        moves = getBitboard().possibleRSecureLong(getBitboard().unsafeforW, getBitboard().bits.WR);
                        break;
                    case "N":
                        moves = getBitboard().possibleNSecureLong(getBitboard().unsafeforW, getBitboard().bits.WN);
                        break;
                    case "B":
                        moves = getBitboard().possibleBSecureLong(getBitboard().unsafeforW, getBitboard().bits.WB);
                        break;
                    case "P":
                        moves = getBitboard().possibleWPSecureLong(getBitboard().unsafeforW, getBitboard().bits.WP, getBitboard().bits.BP, getBitboard().bits.EP);
                        break;
                }
            } else if ((minhaPeca && !isWhite) || (!minhaPeca && isWhite)) {
                getBitboard().initBlack();
                switch (args) {
                    case "K":
                        moves = getBitboard().possibleKSecureLong(getBitboard().unsafeforB, getBitboard().bits.BK);
                        break;
                    case "Q":
                        moves = getBitboard().possibleQSecureLong(getBitboard().unsafeforB, getBitboard().bits.BQ);
                        break;
                    case "R":
                        moves = getBitboard().possibleRSecureLong(getBitboard().unsafeforB, getBitboard().bits.BR);
                        break;
                    case "N":
                        moves = getBitboard().possibleNSecureLong(getBitboard().unsafeforB, getBitboard().bits.BN);
                        break;
                    case "B":
                        moves = getBitboard().possibleBSecureLong(getBitboard().unsafeforB, getBitboard().bits.BB);
                        break;
                    case "P":
                        moves = getBitboard().possibleBPSecureLong(getBitboard().unsafeforB, getBitboard().bits.BP, getBitboard().bits.WP, getBitboard().bits.EP);
                        break;
                }
            }

            res = getBitboard().countSetBits(moves);
            getFunctionCalls().add("SAFEMOVESOF(" + args + ") = " + res);
            return res;
        }
    }

    private int sentry(int pCol, int pLin, boolean isWhite) {
        Piece pAux;
        int result = 0;
        if (isWhite) { // se quem está avaliando o padrão é as brancas
            for (int ix = pLin + 1; ix < 7; ix++) {
                if (pCol > 0) {
                    pAux = getBitboard().getEnumPiece(pCol - 1, ix);
                    result += (pAux != null && !pAux.isWhite() && pAux.getPieceType() == PieceType.PAWN) ? 1 : 0;
                }
                if (pCol < 7) {
                    pAux = getBitboard().getEnumPiece(pCol + 1, ix);
                    result += (pAux != null && !pAux.isWhite() && pAux.getPieceType() == PieceType.PAWN) ? 1 : 0;
                }
            }
        } else {
            for (int ix = pLin - 1; ix > 0; ix--) {
                if (pCol > 0) {
                    pAux = getBitboard().getEnumPiece(pCol - 1, ix);
                    result += (pAux != null && pAux.isWhite() && pAux.getPieceType() == PieceType.PAWN) ? 1 : 0;
                }
                if (pCol < 7) {
                    pAux = getBitboard().getEnumPiece(pCol + 1, ix);
                    result += (pAux != null && pAux.isWhite() && pAux.getPieceType() == PieceType.PAWN) ? 1 : 0;
                }
            }
        }
        return result;
    }

    public int SENTRY(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        int pCol = v.getSquare().getColumn() - 1;
        int pRow = v.getSquare().getLine() - 1;
        int res = sentry(pCol, pRow, v.getSAN().equals("P"));
        getFunctionCalls().add("SENTRY(" + args + ") = " + res);
        return res;
    }

    private int ram(int pCol, int pLin, boolean isWhite) {
        Piece pAux;
        int result = 0;
        if (isWhite) { // se quem está avaliando o padrão é as brancas
            for (int ix = pLin + 1; ix < 7; ix++) {
                pAux = getBitboard().getEnumPiece(pCol, ix);
                result += (pAux != null && pAux.getPieceType() == PieceType.PAWN) ? 1 : 0;
            }
        } else {
            for (int ix = pLin - 1; ix >= 0; ix--) {
                pAux = getBitboard().getEnumPiece(pCol, ix);
                result += (pAux != null && pAux.getPieceType() == PieceType.PAWN) ? 1 : 0;
            }
        }
        return result;
    }

    public int RAM(String args) throws IllegalArgumentException {
        PatternVertex v = vertices.get(instantiatedVertex(args));
        int pCol = v.getSquare().getColumn() - 1;
        int pRow = v.getSquare().getLine() - 1;
        int res = ram(pCol, pRow, v.getSAN().equals("P"));
        getFunctionCalls().add("RAM(" + args + ") = " + res);
        return res;
    }

    //calculates if there is a safety way for the king to reach a specific square
    public int KINGPATHTO(String args) {
        int caminho = 0;
        String[] arg;
        arg = args.split(",");
        PatternVertex v1 = vertices.get(instantiatedVertex(arg[0]));
        PatternVertex v2 = vertices.get(instantiatedVertex(arg[1]));
        //maps to the bitboard coordinates system
        int oRow = 7 - (v1.getSquare().getLine() - 1);
        int oCol = v1.getSquare().getColumn() - 1;
        int dRow = 7 - (v2.getSquare().getLine() - 1);
        int dCol = v2.getSquare().getColumn() - 1;

        getBf().getActiveBitboard().InitVisitedSquares(oRow * 8 + oCol);
        if (v1.getOwner() == ScenarioAction.PieceOwner.ME) {
            caminho = getBf().getActiveBitboard().kingPathTo(isWhite, ~getBf().getActiveBitboard().unsafeForMe(), oRow, oCol, dRow, dCol);
        } else if (v1.getOwner() == ScenarioAction.PieceOwner.OPPONENT) {
            caminho = getBf().getActiveBitboard().kingPathTo(!isWhite, ~getBf().getActiveBitboard().unsafeForOpponent(), oRow, oCol, dRow, dCol);
        }
        getFunctionCalls().add("KINGPATHTO(" + args + ") = " + caminho);
        return caminho;
    }
    
    //count how much opponent pieces can move between a long range piece and his target piece. 
    public int WEDGESBETWEEN(String args) {
        String[] arg;
        arg = args.split(",");
        PatternVertex v1 = vertices.get(instantiatedVertex(arg[0]));
        PatternVertex v2 = vertices.get(instantiatedVertex(arg[1]));
        //mapea coordenadas para o modelo do bitboard
        int oRow = 7 - (v1.getSquare().getLine() - 1);
        int oCol = v1.getSquare().getColumn() - 1;
        int dRow = 7 - (v2.getSquare().getLine() - 1);
        int dCol = v2.getSquare().getColumn() - 1;
        int wedges = getBf().getActiveBitboard().wedgesBetween(oRow, oCol, dRow, dCol, !isWhite);
        getFunctionCalls().add("WEDGESBETWEEN(" + args + ") = " + wedges);
        return wedges;
    }
    //</editor-fold>
    
    //</editor-fold>

    //<editor-fold desc="Getters and setters">
    /**
     * @return the bf
     */
    public Facts getBf() {
        return bf;
    }

    /**
     * @param bf the bf to set
     */
    public void setBf(Facts bf) {
        this.bf = bf;
    }

    /**
     * @return the bitboard
     */
    public BitBoard getBitboard() {
        return getBf().getActiveBitboard();
    }

    /**
     * @return the functionCalls
     */
    public ArrayList<String> getFunctionCalls() {
        return functionCalls;
    }
    
    /**
    * @return the GamePhase
    */
    public GamePhase getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(GamePhase etapa) throws Exception {
        this.gamePhase = etapa;
    }
    //</editor-fold>
    
}
