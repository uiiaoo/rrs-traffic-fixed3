package traffic.body.path;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.misc.Pair;

import java.util.*;

public class Node
{
    private Pair<EntityID, Integer> cell;
    private Node parent;

    private List<EntityID> idHistory;

    private double cost;
    private double heuristic;

    public Node(EntityID id, int num, Node parent, double cost, double heuristic, List<EntityID> idHistory)
    {
        this.cell = new Pair<>(id, num);
        this.parent  = parent;
        this.cost = cost;
        this.heuristic = heuristic;
        this.idHistory = idHistory;
    }

    public Pair<EntityID, Integer> getCell()
    {
        return this.cell;
    }

    public Node getParent()
    {
        return this.parent;
    }

    public double getCost()
    {
        return this.cost;
    }

    public double estimate()
    {
        return this.cost + this.heuristic;
    }

    public List<EntityID> getIDHistory()
    {
        return this.idHistory;
    }
}
