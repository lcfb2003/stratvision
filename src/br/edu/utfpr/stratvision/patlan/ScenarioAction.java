package br.edu.utfpr.stratvision.patlan;

import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author Luis C F Bueno - defines a tuprolog line of action of the pattern scenario code 
 */
public class ScenarioAction implements Cloneable {

    public enum PieceStatus {
        PROTECTED,
        UNPROTECTED,
        ANYONE
    }

    public enum PieceOwner {
        ME,
        OPPONENT,
        ANYONE
    }

    //<editor-fold desc="Fields">
    public String description;  // F1 => (F2,f3)
    public boolean found; // if the pattern was found on the current board
    public boolean isStaticAction; // is a static action?
    public boolean isIndirect; // is an indirect action
    public boolean orBegin; // true if it is teh beginning of an or operation
    public boolean orEnd; // true if it is the end of an or operation
    public boolean notOR; // true if the action is not inside of an or operation
    // Example:  
    // { F1 INDIRECTDEFENSE (S1,S2)
    //   F1 POSSIBLEDEFENSE (S1,S2) }
    public boolean isPositional; // true if the action is only positional
    public boolean notAction; // true if the action has not to be present  --> Prolog not operator
    public String leftActor, leftActorSource; // the first figure or actor of the action (protagonist)
    public PieceOwner leftOwner; // protagonist owner
    public PieceStatus leftStatus = PieceStatus.ANYONE; // status of the protagonist 
    public ArrayList<String> rightActor, rightActorSource; // a list of the actors to the right of the action operator
    public ArrayList<PieceStatus> rightStatus; // status of each actor (piece) to the right of the action operator
    public ArrayList<PieceOwner> rightOwner; // owners of each actor (piece) to the right
    public PatternActionType actionType;
    private final ArrayList<String> defenses; // list used to set the protected actors
    private String leftPiece, rightPiece = "";
    
    //</editor-fold>

    public ScenarioAction() {
        rightActorSource = new ArrayList<>();
        rightActor = new ArrayList<>();
        rightStatus = new ArrayList<>();
        rightOwner = new ArrayList<>();
        defenses  = new ArrayList<>();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        
        ScenarioAction clone = (ScenarioAction)super.clone();
        clone.rightOwner = new ArrayList<>();
        clone.rightActor = new ArrayList<>();
        clone.rightActorSource = new ArrayList<>();
        clone.rightStatus = new ArrayList<>();
        clone.rightOwner.addAll(this.rightOwner);
        clone.rightActor.addAll(this.rightActor);
        clone.rightActorSource.addAll(this.rightActorSource);
        clone.rightStatus.addAll(this.rightStatus);
        return clone;
    }

    private String getLeftOwner() {
        if (null == leftOwner) {
            return "V" + leftActor;
        } else switch (leftOwner) {
            case OPPONENT:
                return "Y" + leftActor;
            case ME:
                return "M" + leftActor;
            default:
                return "V" + leftActor;
        }
    }

    private String getRightOwner(String rightActor, int id) {
        if (rightOwner.get(id) == PieceOwner.OPPONENT) {
            return "Y" + rightActor;
        } else if (rightOwner.get(id) == PieceOwner.ME) {
            return "M" + rightActor;
        } else if (rightActor.charAt(0) == 'L' || rightActor.charAt(0) == 'l') { // if it is a reference to a rank (line)
            return "_";
        } else if (rightActor.charAt(0) == 'D' || rightActor.charAt(0) == 'd') { // if its a reference to a diagonal
            return "_";
        } else if (rightActor.charAt(0) == 'C' || rightActor.charAt(0) == 'c') { // if its a reference to a file (column)
            return "_";
        } else {
            switch (rightActor.charAt(0)) {
                case 'Y':
                    return "M" + rightActor;
                case 'y':
                    return "S" + rightActor;
                default:
                    return "V" + rightActor;
            }
        }
    }

    public String toPrologPreparedStatement(String fact, Map<String, String> ts) {
        leftPiece = getLeftOwner();
        rightPiece = "";
        if (!ts.containsKey(leftPiece)) {
            ts.put(leftPiece, leftPiece);
        }
        leftActorSource = leftActor;
        rightActor.clear();
        rightActor.addAll(rightActorSource);
        if (!fact.contains(leftActor)) {
            if ("kqrbnp".contains(String.valueOf(leftActor.charAt(0)))) { // if it is not a wildcard, the type of the piece is included in the search
                fact += ",yours(" + leftPiece + ")," + leftActor.charAt(0) + "(" + leftPiece + ")";
            } else if ("KQRBNP".contains(String.valueOf(leftActor.charAt(0)))) {
                fact += ",mine(" + leftPiece + ")," + String.valueOf(leftActor.charAt(0)).toLowerCase() + "(" + leftPiece + ")";
            } else if (leftActor.charAt(0) == 'F') { // if it is a wildcard, any piece will be included 
                fact += ",mine(" + leftPiece + ")";
            } else if (leftActor.charAt(0) == 'f') {
                fact += ",yours(" + leftPiece + ")";
            }
            //dbm - defended by me / udbm - undefended by me
            //dby - defended by you / udby - undefended by you
            if (leftStatus == ScenarioAction.PieceStatus.PROTECTED) {
                defenses.add(leftOwner == PieceOwner.ME ? ",dbm(" + leftPiece + "," + leftPiece + ")" : ",dby(" + leftPiece + ")");
            } else if (leftStatus == ScenarioAction.PieceStatus.UNPROTECTED) {
                defenses.add(leftOwner == PieceOwner.ME ? ",udbm(" + leftPiece + "," + leftPiece + ")" : ",udby(" + leftPiece + ")");
            }
        }
        if (actionType != PatternActionType.DECLARATION) {
            int id = 0; // right figure index
            for (String figD : rightActor) {
                rightPiece = getRightOwner(figD, id);
                if (!ts.containsKey(rightPiece)) {
                    ts.put(rightPiece, rightPiece);
                }
                if (!fact.contains(figD)) {
                    if ("kqrbnp".contains(String.valueOf(figD.charAt(0)))) {
                        fact += ",yours(" + rightPiece + ")," + figD.charAt(0) + "(" + rightPiece + ")";
                    } else if ("KQRBNP".contains(String.valueOf(figD.charAt(0)))) {
                        fact += ",mine(" + rightPiece + ")," + String.valueOf(figD.charAt(0)).toLowerCase() + "(" + rightPiece + ")";
                    } else if (figD.charAt(0) == 'F') {
                        fact += ",mine(" + rightPiece + ")";
                    } else if (figD.charAt(0) == 'f') {
                        fact += ",yours(" + rightPiece + ")";
                    }
                    if (rightStatus.get(id) == ScenarioAction.PieceStatus.PROTECTED) {
                        if (rightOwner.get(id) == PieceOwner.ME) { 
                            defenses.add(",dbm(" + rightPiece + "," + leftPiece + ")"); 
                        } else if (figD.charAt(0) == 's' || rightOwner.get(id) == PieceOwner.OPPONENT) {
                            defenses.add(",dby(" + rightPiece + ")");
                        }
                    } else if (rightStatus.get(id) == ScenarioAction.PieceStatus.UNPROTECTED) {
                        if (rightOwner.get(id) == PieceOwner.ME) { 
                            defenses.add(",udbm(" + rightPiece + "," + leftPiece + ")");
                        } else if (figD.charAt(0) == 's' || rightOwner.get(id) == PieceOwner.OPPONENT) {
                            defenses.add(",dby(" + rightPiece + ")");
                        }
                    }
                }
                rightActor.set(id++, rightPiece);
            }
        }
        return fact;
    }

    public String toProlog(String fact) {
        String notOperator = (notAction?"not(":"");// if its a not action
        // handle the right actors of the action
        for (String figD : rightActor) {
            rightPiece = figD;
            if (actionType.positional()) {
                fact += ",";
                switch (actionType) {
                    case BOTTOMSIDE:
                        fact += notOperator + "south(";
                        break;
                    case UPPERSIDE:
                        fact += notOperator + "north(";
                        break;
                    case RIGHTSIDE:
                        fact += notOperator + "east(";
                        break;
                    case LEFTSIDE:
                        fact += notOperator + "west(";
                        break;
                    case NE:
                        fact += notOperator + "ne(";
                        break;
                    case NW:
                        fact += notOperator + "no(";
                        break;
                    case SE:
                        fact += notOperator + "se(";
                        break;
                    case SW:
                        fact += notOperator + "so(";
                        break;
                }
                fact += leftPiece + "," + rightPiece + ")" + (notAction?")":"");
                
            } else { // semantic actions of the scenario
                if (rightActor.size() == 1) { // direct actions
                    fact += ",";
                    switch (actionType) {
                        case MOVES:
                            fact += notOperator + "move(" + leftPiece + "," + rightPiece + ")";
                            break;
                        case ATTACKS:
                            fact += notOperator + "attacks(" + leftPiece + "," + rightPiece + ")";
                            break;
                        case DEFENDS:
                            fact += notOperator + "defends(" + leftPiece + "," + rightPiece + ")";
                            break;
                        case CHECKMATE:
                            fact += notOperator + "mate(" + leftPiece + "," + rightPiece + ")";
                            break;
                    }
                    fact += (notAction?")":"");
                }
            }
        }
        if (rightActor.size() > 1) { // indirect actions
            fact += ",";
            switch (actionType) {
                case ATTACKS:
                case IATTACK:
                    fact += notOperator + "iattacks(" + leftPiece;
                    break;
                case DEFENDS:
                case IDEFENSE:
                    fact += notOperator + "idefense(" + leftPiece;
                    break;
                case PATTACK:
                    fact += notOperator + "pattacks(" + leftPiece;
                    break;
                case PDEFENSE:
                    fact += notOperator + "pdefense(" + leftPiece;
                    break;
            }
            fact = rightActor.stream().map((figD) -> "," + figD).reduce(fact, String::concat);
            fact += ")";
            fact += (notAction?")":"");
        }
        return fact;
    }
    
    public String addProtections(String fact) {
        for(String p:defenses) {
            if(!fact.contains(p)) fact += p;
        }
        return fact;
    }

    public String toNaturalLanguage(String fact, String locale) {
        String aux = actionType.toEnglish(notAction);
        fact += leftActor;
        if (leftStatus == PieceStatus.PROTECTED) {
            fact += "(defended)";
        } else if (leftStatus == PieceStatus.UNPROTECTED) {
            fact += "(undefended)";
        }
        if (actionType != PatternActionType.DECLARATION) {
            if (actionType.getDirect()) {
                aux += rightActor.get(0);
            } else {
                for (int ic = 0; ic < rightActor.size(); ic++) {
                    String sit = "";
                    if (rightStatus.get(ic) == PieceStatus.PROTECTED) {
                        sit = "(defended)";
                    } else if (rightStatus.get(ic) == PieceStatus.UNPROTECTED) {
                        sit = "(undefended)";
                    }
                    if (ic == rightActor.size() - 1) {
                        aux = aux.replaceAll("[X]", rightActor.get(ic) + sit);
                    } else {
                        aux = aux.replaceAll("[Y]", rightActor.get(ic) + sit);
                    }
                }
            }
        }
        fact += aux;
        return fact;
    }

    @Override
    public String toString() {
        if(notAction) return ""; // issue: include not actions 
        StringBuilder s = new StringBuilder();
        s.append(leftActor).append(" ");
        s.append(actionType.operator()).append(" ");
        if (actionType != PatternActionType.DECLARATION) {
            if (actionType.getDirect()) {
                s.append(rightActor.get(0));
            } else {
                s.append("(");
                boolean prim = true;
                for (String fd : rightActor) {
                    if (prim) {
                        prim = false;
                        s.append(fd);
                    } else {
                        s.append(",").append(fd);
                    }
                }
                s.append(")\n");
            }
        }
        return s.toString();
    }
}
