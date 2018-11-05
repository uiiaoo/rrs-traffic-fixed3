package traffic.body.geom;

import traffic.util.*;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.Edge;
import rescuecore2.misc.geometry.*;

import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.*;
import java.util.stream.*;

public class DecomposeShape
{
    public static List<Cell> run(
        Area shape,
        rescuecore2.standard.entities.Area area)
    {
        PathIterator pi = shape.getPathIterator(null);

        List<Line2D> outlines = GeomUtil.toPointList(pi)
            .stream()
            .map(as -> GeomUtil.removeError(as))
            .filter(as -> !as.isEmpty())
            .map(as -> GeometryTools2D.pointsToLines(as, true))
            .map(ls -> GeomUtil.toUniqueLines(ls))
            .flatMap(ls -> ls.stream())
            .collect(Collectors.toList());

        if (outlines.isEmpty()) return new LinkedList<>();

        List<Line2D> separators = new LinkedList<>();
        for (Line2D outline : outlines)
        {
            Point2D apex = outline.getOrigin();
            Line2D upseed = new Line2D(apex, new Vector2D(0.0, +1.0));
            Line2D dnseed = new Line2D(apex, new Vector2D(0.0, -1.0));

            Line2D up = seekShortestSeparator(upseed, outlines, shape);
            if (up != null) separators.add(up);

            Line2D dn = seekShortestSeparator(dnseed, outlines, shape);
            if (dn != null) separators.add(GeomUtil.reverse(dn));

            if (up == null && dn == null)
                separators.add(new Line2D(apex, apex));
        }

        List<Line2D> arranged = GeomUtil.distinct(separators)
            .stream()
            .sorted((l1, l2) -> {
                double x1 = l1.getOrigin().getX();
                double x2 = l2.getOrigin().getX();
                return Double.compare(x1, x2);
            })
            .collect(Collectors.toList());

        boolean[][] neighbourMatrix =
            makeNeighbourMatrix(arranged, outlines, shape);
        return Cell.make(arranged, neighbourMatrix, area.getEdges());
    }

    private static Line2D seekShortestSeparator(
        Line2D seed,
        List<Line2D> outlines,
        Area shape)
    {
        Point2D apex = seed.getOrigin();
        Line2D shortest = null;

        for (Line2D outline : outlines)
        {
            if (apex.equals(outline.getOrigin())) continue;
            if (apex.equals(outline.getEndPoint())) continue;

            double intersection = outline.getIntersection(seed);
            if (Double.isNaN(intersection)) continue;
            if (!(0.0 <= intersection && intersection <= 1.0)) continue;

            Point2D end = outline.getPoint(intersection);
            if (apex.equals(end)) continue;

            Line2D separator = new Line2D(apex, end);
            if (!isSameDirection(seed, separator)) continue;

            shortest = GeomUtil.min(shortest, separator);
        }

        if (shortest != null)
        {
            Point2D median = GeomUtil.makeMedian(shortest);
            if (!shape.contains(median.getX(), median.getY())) shortest = null;
        }

        return shortest;
    }

    private static boolean[][] makeNeighbourMatrix(
        List<Line2D> separators,
        List<Line2D> outlines,
        Area shape)
    {
        int n = separators.size();
        boolean[][] retval = new boolean[n][n];
        for (boolean[] row : retval) Arrays.fill(row, false);

        for (int i=0; i<n; ++i) J:for (int j=i+1; j<n; ++j)
        {
            Line2D from = separators.get(i);
            Line2D dest = separators.get(j);

            if (from.getDirection().getLength() == 0.0 &&
                dest.getDirection().getLength() == 0.0) continue;

            Point2D fp = GeomUtil.makeMedian(from);
            Point2D dp = GeomUtil.makeMedian(dest);
            Line2D connector = new Line2D(fp, dp);

            Point2D cp = GeomUtil.makeMedian(connector);
            if (!shape.contains(cp.getX(), cp.getY())) continue;

            for (int k=0; k<n; ++k)
            {
                if (k == i) continue;
                if (k == j) continue;

                Point2D intersection =
                    GeometryTools2D.getSegmentIntersectionPoint(
                        connector,
                        separators.get(k));
                if (intersection != null) continue J;
            }

            for (Line2D outline : outlines)
            {
                Point2D intersection = GeometryTools2D.getSegmentIntersectionPoint(
                    connector, outline);

                if (intersection == null) continue;
                if (intersection.equals(connector.getOrigin())) continue;
                if (intersection.equals(connector.getEndPoint())) continue;
                continue J;
            }

            retval[i][j] = true;
            retval[j][i] = true;
        }

        return retval;
    }

    private static boolean isSameDirection(Line2D line1, Line2D line2)
    {
        Vector2D vec1 = line1.getDirection();
        Vector2D vec2 = line2.getDirection();
        return vec1.getY()*vec2.getY() >= 0.0;
    }
}
