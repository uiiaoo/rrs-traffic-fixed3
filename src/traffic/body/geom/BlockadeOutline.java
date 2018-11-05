package traffic.body.geom;

import traffic.util.*;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.*;
import rescuecore2.misc.geometry.*;

import java.util.*;
import java.util.stream.*;

public class BlockadeOutline
{
    private Line2D original;
    private Line2D expanded;

    public static List<BlockadeOutline> make(Blockade blockade, double ar)
    {
        List<Point2D> originalApexes =
            GeometryTools2D.vertexArrayToPoints(blockade.getApexes());
        List<Point2D> apexes =
            GeomUtil.toClockwise(GeomUtil.toUnique(originalApexes));

        List<BlockadeOutline> outlines = new LinkedList<>();

        for (Line2D line : GeometryTools2D.pointsToLines(apexes, true))
            outlines.add(line2outline(line, ar));

        return outlines;
    }

    private static BlockadeOutline line2outline(
        Line2D original,
        double ar)
    {
        Line2D expanded = GeomUtil.shift2left(original, ar);
        return new BlockadeOutline(original, expanded);
    }

    private BlockadeOutline(Line2D original, Line2D expanded)
    {
        this.original = original;
        this.expanded = expanded;
    }

    public Line2D getOriginal()
    {
        return this.original;
    }

    public Line2D getExpanded()
    {
        return this.expanded;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof BlockadeOutline)) return false;
        BlockadeOutline another = (BlockadeOutline)obj;
        return GeomUtil.isEqual(this.getOriginal(), another.getOriginal());
    }
}
