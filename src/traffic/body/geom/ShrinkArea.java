package traffic.body.geom;

import traffic.util.*;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.Edge;
import rescuecore2.misc.geometry.*;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.*;
import java.util.stream.*;

public class ShrinkArea
{
    public static Area run(
        rescuecore2.standard.entities.Area area,
        Map<EntityID, List<AreaOutline>> areaOutlines)
    {
        Area base = new Area(area.getShape());

        List<AreaOutline> outlines = areaOutlines.get(area.getID());
        base.subtract(makeLineShrinker(outlines));
        base.subtract(makeCompleteApexShrinker(outlines));

        outlines.stream()
            .filter(AreaOutline::isPassable)
            .map(AreaOutline::getNeighbour)
            .map(i -> areaOutlines.get(i))
            .forEach(o -> {
                base.subtract(makeLineShrinker(o));
                base.subtract(makeCompleteApexShrinker(o));
            });

        return base;
    }

    public static Area runSimply(
        rescuecore2.standard.entities.Area area,
        Map<EntityID, List<AreaOutline>> areaOutlines)
    {
        Area base = new Area(area.getShape());

        List<AreaOutline> outlines = areaOutlines.get(area.getID());
        base.subtract(makeLineShrinker(outlines));
        base.subtract(makeSimpleApexShrinker(outlines, areaOutlines));

        outlines.stream()
            .filter(AreaOutline::isPassable)
            .map(AreaOutline::getNeighbour)
            .map(i -> areaOutlines.get(i))
            .forEach(o -> {
                base.subtract(makeLineShrinker(o));
                base.subtract(makeSimpleApexShrinker(o, areaOutlines));
            });

        return base;
    }

    private static Area makeLineShrinker(List<AreaOutline> outlines)
    {
        Area base = new Area();
        for (AreaOutline outline : outlines)
        {
            if (outline.isPassable()) continue;
            Line2D original = outline.getOriginal();
            Line2D shrinked = outline.getShrinked();
            Area shrinker = GeomUtil.makeArea(original, shrinked);
            base.add(shrinker);
        }
        return base;
    }

    private static Area makeCompleteApexShrinker(List<AreaOutline> outlines)
    {
        Area base = new Area();

        Set<Point2D> unpassableApexes = extractUnpassableApexes(outlines);
        for (Point2D apex : unpassableApexes)
        {
            double ar = Environment.AGENT_RADIUS;
            Area shrinker = GeomUtil.makeExpandedArea(apex, ar);
            base.add(shrinker);
        }

        return base;
    }

    private static Area makeSimpleApexShrinker(
        List<AreaOutline> outlines,
        Map<EntityID, List<AreaOutline>> areaOutlines)
    {
        Area base = new Area();
        int n = outlines.size();
        for (int i=0; i<n; ++i)
        {
            AreaOutline outline1 = outlines.get((i  )%n);
            AreaOutline outline2 = outlines.get((i+1)%n);
            Point2D corner = outline1.getOriginal().getEndPoint();

            if (outline1.isPassable())
                outline1 = seekUnpassable(outline1, corner, areaOutlines);
            if (outline1 == null) continue;

            if (outline2.isPassable())
                outline2 = seekUnpassable(outline2, corner, areaOutlines);
            if (outline2 == null) continue;

            Line2D original1 = outline1.getOriginal();
            Line2D original2 = outline2.getOriginal();
            Line2D shrinked1 = outline1.getShrinked();
            Line2D shrinked2 = outline2.getShrinked();

            base.add(GeomUtil.makeArea(original1, shrinked1));
            base.add(GeomUtil.makeArea(original2, shrinked2));

            double theta = MathUtil.angle(original1, original2);
            if (theta <= 0.0) continue;
            base.add(makeComplementaryArea(corner, outline1, outline2));
        }

        return base;
    }

    private static Set<Point2D> extractUnpassableApexes(
        List<AreaOutline> outlines)
    {
        Set<Point2D> retval = new HashSet<>();
        for (AreaOutline outline : outlines)
        {
            if (outline.isPassable()) continue;
            Line2D original = outline.getOriginal();
            retval.add(original.getOrigin());
            retval.add(original.getEndPoint());
        }
        return retval;
    }

    private static Area makeComplementaryArea(
        Point2D corner,
        AreaOutline outline1,
        AreaOutline outline2)
    {
        Line2D shrinked1 = outline1.getShrinked();
        Line2D shrinked2 = outline2.getShrinked();

        Point2D complementer =
            GeometryTools2D.getIntersectionPoint(shrinked1, shrinked2);
        if (complementer == null) complementer = shrinked2.getOrigin();

        Point2D p1 = shrinked1.getEndPoint();
        Point2D p2 = shrinked2.getOrigin();
        return GeomUtil.makeArea(new Point2D[]{corner, p1, complementer, p2});
    }

    private static AreaOutline seekUnpassable(
        AreaOutline outline0,
        Point2D corner,
        Map<EntityID, List<AreaOutline>> areaOutlines)
    {
        Set<EntityID> done = new HashSet<>();
        LinkedList<EntityID> candidates = new LinkedList<>();
        candidates.add(outline0.getNeighbour());

        while (!candidates.isEmpty())
        {
            EntityID id = candidates.poll();
            if (done.contains(id)) continue;

            List<AreaOutline> outlines = areaOutlines.get(id);
            for (AreaOutline outline : outlines)
            {
                Point2D op = outline.getOriginal().getOrigin();
                Point2D ep = outline.getOriginal().getEndPoint();

                if (!op.equals(corner) && !ep.equals(corner)) continue;
                if (!outline.isPassable()) return outline;

                EntityID neighbour = outline.getNeighbour();
                if (!outline.equals(outline0)) candidates.add(neighbour);
            }
            done.add(id);
        }

        return null;
    }
}
