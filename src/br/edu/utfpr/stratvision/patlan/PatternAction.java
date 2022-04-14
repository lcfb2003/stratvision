package br.edu.utfpr.stratvision.patlan;

/**
 *
 * @author Luis Carlos F. Bueno - 16/11/2021
 * This class represents the action played between two vertex of the graph of the pattern
 * used to graph searchs like the king path
 */


public class PatternAction {

    private PatternVertex source;
    private PatternVertex target;
    private Double weight;
    private PatternActionType edgeType;

    public PatternAction(PatternVertex v1, PatternVertex v2, Double weight, PatternActionType edgeType) {
        this.source = v1;
        this.target = v2;
        this.weight = weight;
        this.edgeType = edgeType;
    }

    /**
     * @return the source vertex
     */
    public PatternVertex getSource() {
        return source;
    }

    /**
     * @param v1 the source vertex to set
     */
    public void setSource(PatternVertex v1) {
        this.source = v1;
    }

    /**
     * @return the target vertex
     */
    public PatternVertex getTarget() {
        return target;
    }

    /**
     * @param v2 the target vertex to set
     */
    public void setTarget(PatternVertex v2) {
        this.target = v2;
    }

    /**
     * @return the weight
     */
    public Double getWeight() {
        return weight;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(Double weight) {
        this.weight = weight;
    }

    /**
     * @return the edge type
     */
    public PatternActionType getEdgeType() {
        return edgeType;
    }

    /**
     * @param edgeType the edge type to set
     */
    public void setEdgeType(PatternActionType edgeType) {
        this.edgeType = edgeType;
    }

}
