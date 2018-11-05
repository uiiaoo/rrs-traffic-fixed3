package traffic.body.geom;

import traffic.util.*;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.*;
import rescuecore2.misc.geometry.*;

import java.util.*;
import java.util.stream.*;

public class AreaOutline
{
    private Line2D original;
    private EntityID neighbour;
    private Line2D shrinked;

    public static List<AreaOutline> make(Area area, double ar)
    {
        List<Point2D> originalApexes =
            GeometryTools2D.vertexArrayToPoints(area.getApexList());
        List<Point2D> apexes =
            GeomUtil.toClockwise(GeomUtil.toUnique(originalApexes));

        List<Edge> edges = area.getEdges();
        List<AreaOutline> outlines = new LinkedList<>();

        for (Line2D line : GeometryTools2D.pointsToLines(apexes, true))
            outlines.add(line2outline(line, edges, ar));

        return outlines;
    }

    private static AreaOutline line2outline(
        Line2D original,
        List<Edge> edges,
        double ar)
    {
        EntityID neighbour = edges.stream()
            .filter(Edge::isPassable)
            .filter(e -> GeomUtil.isEqual(original, e.getLine()))
            .map(Edge::getNeighbour)
            .findFirst().orElse(null);

        Line2D shrinked = GeomUtil.shift2right(original, ar);
        return new AreaOutline(original, neighbour, shrinked);
    }

    private AreaOutline(Line2D original, EntityID neighbour, Line2D shrinked)
    {
        this.original = original;
        this.neighbour = neighbour;
        this.shrinked = shrinked;
    }

    public Line2D getOriginal()
    {
        return this.original;
    }

    public Line2D getShrinked()
    {
        return this.shrinked;
    }

    public boolean isPassable()
    {
        return this.neighbour != null;
    }

    public EntityID getNeighbour()
    {
        return this.neighbour;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof AreaOutline)) return false;
        AreaOutline another = (AreaOutline)obj;
        return GeomUtil.isEqual(this.getOriginal(), another.getOriginal());
    }
}
