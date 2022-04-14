package br.edu.utfpr.stratvision.patlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author Luis Carlos F. Bueno - 16/11/2021
 * This class represents a HLP - High Level Pattern of chess game
 */
public class Pattern implements Cloneable {

    //<editor-fold desc="Fields and variables">
    private String name = "";
    private String author = "";
    private String source = "";
    private String description = "";
    private String scenario = "";
    private Double weight = 0.00;
    private String notes = "";
    private PatternType patternType = PatternType.STATIC;
    
    private HashMap<String, PATLANParser.TS> st; // minimal material required to set the pattern
    private HashSet<Character> exclusiveSet = new HashSet<>(); // the piece types to be exclusively on the board
   
    private ArrayList<ScenarioAction> scenarioActions;
    private String preConditionToJava = "";
    private String posConditionToJava = "";
    private String toProlog = ""; // used on authoring mode and game bases search mode
    
    // true if the pattern does a recursive search from opponent point of view. In this case the prolog theory for the opponent is created.
    private boolean isRecursiveSearch = false;  
    
    // used when the pattern is being executed on the game
    // tells which movement must be done
    private int currentTacticalMoveIndex = -1; 
    
    // set of tactical moves of the pattern
    private ArrayList<String> moves; // linhas do código fonte
    private ArrayList<Ply> tacticalMoves; 
    
    // list of all instances found by tuProlog and instances which matches all the conditions of the pattern
    public ArrayList<ArrayList<ScenarioAction>> Instances, OKInstances; 
    public HashMap<String, PatternVertex> Vertices;
    
    public enum PatternType {

        STATIC("Static", 56),
        DYNAMIC("Dynamic", 57);
        private final String description;
        private final int id;

        private PatternType(String description, int id) {
            this.description = description;
            this.id = id;
        }

        public int id() {
            return id;
        }

        @Override
        public String toString() {
            return description;
        }
    }
    //</editor-fold>
    
    //<editor-fold desc="Constructors">
    public Pattern() {
        reset();
    }
    
    public Pattern(String name, String author, PatternType type) {
        this.name = name;
        this.author = author;
        this.st = new HashMap<>();
        this.patternType = type;
        reset();
    }
    //</editor-fold>

    //<editor-fold desc="Class methods">
    public final void reset() {
        currentTacticalMoveIndex = -1;
        moves = new ArrayList<>();
        tacticalMoves = new ArrayList<>();
        scenarioActions = new ArrayList<>();
        Instances = new ArrayList<>(); // the instances that matches the scenario only
        OKInstances = new ArrayList<>(); // the final instances after all pattern conditions evaluation 
    }

    public void prepareStatement(int mode) {
        this.prepareStatement();
        if (mode == PatternEvaluator.SEARCH_MODE || mode == PatternEvaluator.GAME_MODE) {
            toProlog = toProlog.substring(0, toProlog.length() - 1) + ",!.";
        }
    }

    // issue: not actions are not included yet
    public void prepareStatement() {
        boolean primAlt = false;
        Map<String, String> ts = new HashMap<>();
        String fact = "";
        String notEquals = "";

        for (ScenarioAction action : scenarioActions) { 
            fact = action.toPrologPreparedStatement(fact, ts);
        }
        // deletes all prolog solutions where there are different variables with the same instantiated value
        for (String s1 : ts.keySet()) {
            for (String s2 : ts.keySet()) {
                if (!s2.equals(s1)) {
                    if ((s1.substring(1, 2).equals("F") && s2.substring(1, 2).equals("F")) ||
                        (s1.substring(1, 2).equals("f") && s2.substring(1, 2).equals("f")) || 
                        (s1.substring(1, 2).equals("S") && s2.substring(1, 2).equals("S")) ||
                        (s1.substring(1, 2).equals("s") && s2.substring(1, 2).equals("s"))
                       ) {
                        if (!notEquals.contains(s2 + "=" + s1)) {
                            notEquals += ",not(" + s1 + "=" + s2 + ")";
                        }
                    }
                }
            }
        }
        for (ScenarioAction action : scenarioActions) {
            if (action.orBegin && !primAlt) { // if its the firs action of a sequence
                primAlt = true;
                fact += ",(";
            }
            if (action.orBegin) {
                fact += action.toProlog(new String()).substring(1);
            } else {
                fact += action.toProlog(new String());
            }
            if (action.orBegin && !action.orEnd) {
                fact += ";"; // if the action is an or operator;
            }
            if (action.orEnd) { // or end code reached;
                primAlt = false;
                fact += ")";
            }
        }
        if (primAlt) {
            fact += ")";
        }
        for (ScenarioAction formato : scenarioActions) {
            fact = formato.addProtections(fact);
        }
        //fato += notEquals;
        fact = fact.substring(1) + "."; 

        toProlog = fact;
    }

    public String toNaturalLanguage(int instancia) {
        String fato = "";
        int ic = 0;
        for (ScenarioAction formato : OKInstances.get(instancia)) {
            fato = formato.toNaturalLanguage(fato, "PT-BR");
            if (ic < (OKInstances.get(instancia).size() - 1)) {
                fato += " E \n";
            }
            ic++;
        }
        return fato;
    }
    @Override
    public Pattern clone() throws CloneNotSupportedException {
        try {
            return (Pattern) super.clone();
        } catch (CloneNotSupportedException e) {
            throw e;
        }
    }
    //</editor-fold>

    //<editor-fold desc="Getters and setters">
    /**
     * @return the toProlog
     */
    public String getToProlog() {
        return toProlog;
    }

    /**
     * @param toProlog the toProlog to set
     */
    public void setToProlog(String toProlog) {
        this.toProlog = toProlog;
    }

    /**
     * @return the recursiveSearch
     */
    public boolean isRecursiveSearch() {
        return isRecursiveSearch;
    }

    /**
     * @param recursiveSearch the recursiveSearch to set
     */
    public void setRecursiveSearch(boolean recursiveSearch) {
        this.isRecursiveSearch = recursiveSearch;
    }

    /**
     * @return the Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param Description the Description to set
     */
    public void setDescription(String Description) {
        this.description = Description;
    }

    /**
     * @return the Formato
     */
    public String getScenario() {
        return scenario;
    }

    /**
     * @param Formato the Formato to set
     */
    public void setScenario(String Formato) {
        this.scenario = Formato;
    }

    /**
     * @return the Weight
     */
    public Double getWeight() {
        return weight;
    }

    /**
     * @param Weight the Weight to set
     */
    public void setWeight(Double Weight) {
        this.weight = Weight;
    }

    /**
     * @return the Condicao
     */
    public String getPreConditionToJava() {
        return preConditionToJava;
    }

    /**
     * @return the Anotacao
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @param notes the Anotacao to set
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    /**
     * @return the patternType
     */
    public PatternType getPatternType() {
        return patternType;
    }

    /**
     * @param patternType the patternType to set
     */
    public void setPatternType(PatternType patternType) {
        this.patternType = patternType;
    }
    /**
     * @return the mode
     */
    
    public PatternType getMode() {
        return getPatternType();
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(PatternType mode) {
        this.setPatternType(mode);
    }

    public String getSource() {
        return source;
    }

    /**
     * @param source the Source to set
     */
    public final void setSource(String source) {
        this.source = source;
        int abAspas = source.indexOf("\"");
        int fcAspas = source.indexOf("\"",abAspas+1);
        setName(source.substring(abAspas+1,fcAspas));
        setMode((scenario.indexOf("S") > 0 || scenario.indexOf("s") > 0) ? PatternType.DYNAMIC : PatternType.STATIC);
    }

    /**
     * @return the scenario members
     */
    public ArrayList<ScenarioAction> getScenarioActions() {
        return scenarioActions;
    }

    public void setPreConditionToJava(String condicao) {
        this.preConditionToJava = condicao;
    }

    public String getPreConditionToJavaOutput() {
        return "analise.condicaoOK = (" + preConditionToJava + ");";
    }

    public void setScenarioActions(ArrayList<ScenarioAction> controle) {
        this.scenarioActions = controle;
    }

    /**
     * @return the ExclusiveSet
     */
    public HashSet<Character> getExclusiveSet() {
        return exclusiveSet;
    }

    /**
     * @param ExclusiveSet the ExclusiveSet to set
     */
    public void setExclusiveSet(HashSet<Character> ExclusiveSet) {
        this.exclusiveSet = ExclusiveSet;
    }

    /**
     * @return the required material to accomplish the pattern
     */
    public HashMap<String, PATLANParser.TS> getST() {
        return st;
    }

    /**
     * @param RequiredMaterial the required material to set
     */
    public void setST(HashMap<String, PATLANParser.TS> RequiredMaterial) {
        this.st = RequiredMaterial;
    }

    /**
     * @return the postConditionToJava
     */
    public String getPostConditionToJava() {
        if (!posConditionToJava.isEmpty()) {
            return "analise.condicaoOK = (" + posConditionToJava + ");";
        } else {
            return posConditionToJava;
        }
    }

    /**
     * @param posConditionToJava the posCondicaoToJava to set
     */
    public void setPosConditionToJava(String posConditionToJava) {
        this.posConditionToJava = posConditionToJava;
    }
    
    /**
    * @return the currentTacticalMove
    */
    public int getCurrentTacticalMoveIndex() {
        return currentTacticalMoveIndex;
    }
    public void incCurrentTacticalMoveIndex() {
        currentTacticalMoveIndex++;
    }
    
    public Ply getNextTacticalMove() {
        if(++currentTacticalMoveIndex < tacticalMoves.size()) {
            return tacticalMoves.get(currentTacticalMoveIndex);
        } else {
            --currentTacticalMoveIndex;
        }
        return null;
    }
    
    public Ply getCurrentTacticalMove() {
        if(currentTacticalMoveIndex < tacticalMoves.size()) {
            return tacticalMoves.get(currentTacticalMoveIndex);
        }
        return null;
    }
    
    /**
    * @return the TacticalMoves
    */
    public ArrayList<Ply> getTacticalMoves() {
        return tacticalMoves;
    }
    
    /**
     * @return the preConditions
     */
    public ArrayList<String> getPreConditions() {
        
        ArrayList<String> pre = new ArrayList<>();
        
        String[] prec = preConditionToJava.split(" AND ");
        pre.addAll(Arrays.asList(prec));
        return pre;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @param author the author to set
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * @return the moves
     */
    public ArrayList<String> getMoves() {
        return moves;
    }

    /**
     * @param moves the moves to set
     */
    public void setMoves(ArrayList<String> moves) {
        this.moves = moves;
    }
    //</editor-fold>
}
