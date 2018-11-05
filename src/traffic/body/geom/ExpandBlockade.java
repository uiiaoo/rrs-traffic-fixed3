package traffic.body.geom;

import traffic.util.*;

import rescuecore2.standard.entities.Blockade;
import rescuecore2.misc.geometry.*;

import java.awt.geom.Area;
import java.util.*;

public class ExpandBlockade
{
    public static Area run(Blockade blockade)
    {
        double ar = Environment.AGENT_RADIUS;
        List<BlockadeOutline> outlines = BlockadeOutline.make(blockade, ar);

        Area base = new Area(blockade.getShape());
        base.add(makeLineExpander(outlines));
        base.add(makeCompleteApexExpander(outlines));
        return base;
    }

    public static Area runSimply(Blockade blockade)
    {
        double ar = Environment.AGENT_RADIUS;
        List<BlockadeOutline> outlines = BlockadeOutline.make(blockade, ar);

        Area base = new Area(blockade.getShape());
        base.add(makeLineExpander(outlines));
        base.add(makeSimpleApexExpander(outlines));
        return base;
    }

    private static Area makeLineExpander(List<BlockadeOutline> outlines)
    {
        Area base = new Area();
        for (BlockadeOutline outline : outlines)
        {
            Line2D original = outline.getOriginal();
            Line2D expanded = outline.getExpanded();
            Area expander = GeomUtil.makeArea(original, expanded);
            base.add(expander);
        }
        return base;
    }

    private static Area makeCompleteApexExpander(List<BlockadeOutline> outlines)
    {
        Area base = new Area();
        for (BlockadeOutline outline : outlines)
        {
            double ar = Environment.AGENT_RADIUS;
            Point2D apex = outline.getOriginal().getOrigin();
            Area expander = GeomUtil.makeExpandedArea(apex, ar);
            base.add(expander);
        }
        return base;
    }

    private static Area makeSimpleApexExpander(List<BlockadeOutline> outlines)
    {
        Area base = new Area();
        int n = outlines.size();

        for (int i=0; i<n; ++i)
        {
            BlockadeOutline outline1 = outlines.get((i  )%n);
            BlockadeOutline outline2 = outlines.get((i+1)%n);

            Line2D original1 = outline1.getOriginal();
            Line2D original2 = outline2.getOriginal();

            double theta = MathUtil.angle(original1, original2);
            if (theta >= 0.0) continue;

            Point2D corner = original1.getEndPoint();
            base.add(makeComplementaryArea(corner, outline1, outline2));
        }

        return base;
    }

    private static Area makeComplementaryArea(
        Point2D corner,
        BlockadeOutline outline1,
        BlockadeOutline outline2)
    {
        Line2D expanded1 = outline1.getExpanded();
        Line2D expanded2 = outline2.getExpanded();

        Point2D complementer =
            GeometryTools2D.getIntersectionPoint(expanded1, expanded2);

        Point2D p1 = expanded1.getEndPoint();
        Point2D p2 = expanded2.getOrigin();
        return GeomUtil.makeArea(new Point2D[]{corner, p1, complementer, p2});
    }
}
