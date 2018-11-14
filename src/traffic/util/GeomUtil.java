package traffic.util;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.misc.geometry.*;

import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.stream.*;

public class GeomUtil
{
    public static List<List<Point2D>> toPointList(PathIterator pi)
    {
        List<List<Point2D>> ret = new LinkedList<>();
        List<Point2D> sub = new LinkedList<>();
        while (!pi.isDone())
        {
            double[] coords = new double[6];
            int type = pi.currentSegment(coords);
            switch (type)
            {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    sub.add(new Point2D(coords[0], coords[1]));
                    break;
                case PathIterator.SEG_CLOSE:
                    ret.add(sub);
                    sub = new LinkedList<>();
                    break;
                case PathIterator.SEG_CUBICTO:
                    sub.add(new Point2D(coords[4], coords[5]));
                    break;
                default:
                    System.err.println("unexpected path type");
            }

            pi.next();
        }

        return ret;
    }

    public static List<Area> toSingulars(Area original)
    {
        List<Area> retval = new LinkedList<>();
        if (original.isEmpty())
        {
            return retval;
        }

        if (original.isSingular())
        {
            retval.add(original);
            return retval;
        }

        PathIterator pi = original.getPathIterator(null);
        Path2D path = new Path2D.Double();
        while (!pi.isDone())
        {
            double[] coords = new double[6];
            int type = pi.currentSegment(coords);
            switch (type)
            {
                case PathIterator.SEG_MOVETO:
                    path.moveTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    path.lineTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    path.curveTo(
                        coords[0], coords[1],
                        coords[2], coords[3],
                        coords[4], coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    path.closePath();
                    Area area = new Area(path);
                    if (!area.isEmpty()) retval.add(area);
                    path = new Path2D.Double();
                    break;
                default:
                    System.err.println("[!] unexpected path type");
            }

            pi.next();
        }

        return retval;
    }

    public static List<Line2D> extractStraightLines(PathIterator pi)
    {
        List<Line2D> retval = new LinkedList<>();

        Point2D before = null;
        while (!pi.isDone())
        {
            double[] coords = new double[6];
            int type = pi.currentSegment(coords);
            switch (type)
            {
                case PathIterator.SEG_MOVETO:
                    before = new Point2D(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    Point2D point = new Point2D(coords[0], coords[1]);
                    retval.add(new Line2D(before, point));
                    before = point;
                    break;
                case PathIterator.SEG_CUBICTO:
                    before = new Point2D(coords[4], coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    retval.add(new Line2D(before, retval.get(0).getOrigin()));
                    break;
                default:
                    System.err.println("unexpected path type");
            }

            pi.next();
        }

        return retval;
    }

    private static final double ERROR_THRESH = 1.0E-5;
    //private static final double ERROR_THRESH = 100.0;
    //
    public static boolean isOverlapping(Line2D l1, Line2D l2)
    {
        if (Math.abs(MathUtil.cross(l1, l2)) >= ERROR_THRESH) return false;
        return GeometryTools2D.contains(l1, l2.getOrigin())   ||
               GeometryTools2D.contains(l1, l2.getEndPoint()) ||
               GeometryTools2D.contains(l2, l1.getOrigin())   ||
               GeometryTools2D.contains(l2, l1.getEndPoint());
    }

    public static Line2D computeOverlapping(Line2D l1, Line2D l2)
    {
        if (Math.abs(MathUtil.cross(l1, l2)) >= ERROR_THRESH) return null;

        if (GeometryTools2D.contains(l1, l2.getOrigin()) &&
            GeometryTools2D.contains(l1, l2.getEndPoint()))
            return l2;

        if (GeometryTools2D.contains(l2, l1.getOrigin()) &&
            GeometryTools2D.contains(l2, l1.getEndPoint()))
            return l1;

        if (GeometryTools2D.contains(l1, l2.getOrigin()) &&
            GeometryTools2D.contains(l2, l1.getOrigin()))
            return new Line2D(l2.getOrigin(), l1.getOrigin());

        if (GeometryTools2D.contains(l1, l2.getEndPoint()) &&
            GeometryTools2D.contains(l2, l1.getEndPoint()))
            return new Line2D(l2.getEndPoint(), l1.getEndPoint());

        if (GeometryTools2D.contains(l1, l2.getOrigin()) &&
            GeometryTools2D.contains(l2, l1.getEndPoint()))
            return new Line2D(l2.getOrigin(), l1.getEndPoint());

        if (GeometryTools2D.contains(l1, l2.getEndPoint()) &&
            GeometryTools2D.contains(l2, l1.getOrigin()))
            return new Line2D(l2.getEndPoint(), l1.getOrigin());

        return null;
    }

    public static List<Point2D> removeError(List<Point2D> points)
    {
        List<Point2D> ret = new LinkedList<>();
        List<Line2D> lines = GeometryTools2D.pointsToLines(points, true)
            .stream()
            .filter(l -> l.getDirection().getLength() >= ERROR_THRESH)
            .collect(Collectors.toList());

        Point2D p = null;
        Line2D line1 = null;
        Line2D line2 = null;
        for (int i=0; i<lines.size(); ++i)
        {
            Line2D l1 = lines.get((i  )%lines.size());
            Line2D l2 = lines.get((i+1)%lines.size());

            Point2D point = l1.getEndPoint();
            if (!point.equals(l2.getOrigin()) && Math.abs(MathUtil.cross(l1, l2)) >= ERROR_THRESH)
                point = GeometryTools2D.getIntersectionPoint(l1, l2);

            if (point == null) continue;
            ret.add(point);
        }

        //boolean isAll = true;
        //for (Line2D line : lines)
        //{
        //    if (line.getDirection().getLength() >= 1.0E-9) isAll = false;
        //}
        //if (isAll) System.out.println("is all : " + lines.size());

        //int beforeIndex = -1;
        //for (int i=lines.size()-1; i>=0 && beforeIndex == -1; --i)
        //{
        //    Line2D line = lines.get(i);
        //    if (line.getDirection().getLength() >= ERROR_THRESH) beforeIndex = i;
        //}

        //for (int i=0; i<lines.size(); ++i)
        //{
        //    Line2D line = lines.get(i);
        //    if (line.getDirection().getLength() < ERROR_THRESH) continue;

        //    if (beforeIndex == (i-1+lines.size())%lines.size())
        //    {
        //        ret.add(line.getOrigin());
        //        beforeIndex = i;
        //        continue;
        //    }

        //    Line2D before = lines.get(beforeIndex);
        //    Point2D intersection =
        //        GeometryTools2D.getIntersectionPoint(line, before);
        //    if (intersection != null) ret.add(intersection);
        //    beforeIndex = i;
        //}

        return ret;
    }

    public static Line2D reverse(Line2D line)
    {
        return new Line2D(line.getEndPoint(), line.getOrigin());
    }

    public static Line2D shift2left(Line2D line, double width)
    {
        return shift2left(line.getOrigin(), line.getEndPoint(), width);
    }

    public static Line2D shift2left(Point2D p1, Point2D p2, double width)
    {
        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();

        double length = Math.hypot(x2-x1, y2-y1);
        double dx = (y1-y2) * width / length;
        double dy = (x2-x1) * width / length;

        return new Line2D(
            new Point2D(x1+dx, y1+dy),
            new Point2D(x2+dx, y2+dy));
    }

    public static Line2D shift2right(Line2D line, double width)
    {
        return shift2right(line.getOrigin(), line.getEndPoint(), width);
    }

    public static Line2D shift2right(Point2D p1, Point2D p2, double width)
    {
        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();

        double length = Math.hypot(x2-x1, y2-y1);
        double dx = (y2-y1) * width / length;
        double dy = (x1-x2) * width / length;

        return new Line2D(
            new Point2D(x1+dx, y1+dy),
            new Point2D(x2+dx, y2+dy));
    }

    //  without endpoints
    public static boolean haveSegmentIntersection(Line2D l1, Line2D l2)
    {
        double x1 = l1.getOrigin().getX();
        double x2 = l1.getEndPoint().getX();
        double x3 = l2.getOrigin().getX();
        double x4 = l2.getEndPoint().getX();

        double y1 = l1.getOrigin().getY();
        double y2 = l1.getEndPoint().getY();
        double y3 = l2.getOrigin().getY();
        double y4 = l2.getEndPoint().getY();

        double t1 = (x3-x4) * (y1-y3) + (y3-y4) * (x3-x1);
        double t2 = (x3-x4) * (y2-y3) + (y3-y4) * (x3-x2);
        double t3 = (x1-x2) * (y3-y1) + (y1-y2) * (x1-x3);
        double t4 = (x1-x2) * (y4-y1) + (y1-y2) * (x1-x4);

        return t1*t2 < 0.0 && t3*t4 < 0.0;
    }

    public static List<Point2D> toUnique(List<Point2D> ps)
    {
        List<Point2D> ret = new LinkedList<>();
        if (ps == null || ps.isEmpty()) return ret;
        int n = ps.size();

        ret.add(ps.get(0));
        Point2D last = ret.get(0);
        for (int i=1; i<n; ++i)
        {
            Point2D p = ps.get(i);
            if (!p.equals(last)) ret.add(p);
            last = p;
        }

        if (ret.size() >= 3 && ret.get(0).equals(last))
        {
            ret.remove(ret.size()-1);
        }
        return ret;
    }

    public static List<Line2D> distinct(List<Line2D> lines)
    {
        List<Line2D> ret = new LinkedList<>();
        L: for (Line2D l : lines)
        {
            for (Line2D u : ret)
            {
                if (GeomUtil.isEqual(l, u)) continue L;
            }

            ret.add(l);
        }

        return ret;
    }

    public static List<Line2D> toUniqueLines(List<Line2D> lines)
    {
        List<Line2D> ret = new LinkedList<>();
        Line2D current = null;
        for (Line2D line : lines)
        {
            if (current == null)
            {
                current = line;
                continue;
            }

            //if (!GeometryTools2D.parallel(current, line) &&
            //    !GeometryTools2D.nearlyZero(line.getDirection().getLength()))
            if (!GeometryTools2D.parallel(current, line))
            {
                ret.add(current);
                current = line;
                continue;
            }

            current = new Line2D(current.getOrigin(), line.getEndPoint());
        }

        if (current == null) return ret;
        if (ret.isEmpty()) { ret.add(current); return ret; }

        if (GeometryTools2D.parallel(ret.get(0), current))
        {
            Line2D l = new Line2D(current.getOrigin(), ret.get(0).getEndPoint());
            ret.remove(0);
            ret.add(0, l);
        }
        else ret.add(current);
        return ret;
    }

    public static List<Point2D> toClockwise(List<Point2D> ps)
    {
        List<Line2D> ls = GeometryTools2D.pointsToLines(ps, true);

        double cross = ls.stream()
            .mapToDouble(l -> MathUtil.cross(l.getOrigin(), l.getEndPoint()))
            .sum();

        if (cross >= 0.0) Collections.reverse(ps);

        return ps;
    }

    public static boolean isEqual(Line2D l1, Line2D l2)
    {
        if (l1 == null) return false;
        if (l2 == null) return false;

        Point2D o1 = l1.getOrigin();
        Point2D e1 = l1.getEndPoint();
        Point2D o2 = l2.getOrigin();
        Point2D e2 = l2.getEndPoint();

        return (o1.equals(o2) && e1.equals(e2)) ||
               (o1.equals(e2) && e1.equals(o2));
    }

    public static Point2D makeMedian(Line2D l)
    {
        return l.getPoint(0.5);
    }

    public static Point2D computeCentroid(List<Point2D> vs) {
        return new Point2D(
            vs.stream().mapToDouble(Point2D::getX).average().orElse(0.0),
            vs.stream().mapToDouble(Point2D::getY).average().orElse(0.0));
    }

    public static Area makeArea(Line2D l1, Line2D l2)
    {
        Point2D p1 = l1.getOrigin();
        Point2D p2 = l1.getEndPoint();
        Point2D p3 = l2.getEndPoint();
        Point2D p4 = l2.getOrigin();

        return makeArea(new Point2D[]{p1, p2, p3, p4});
    }

    public static Area makeArea(Point2D[] ps)
    {
        Path2D path = new Path2D.Double();
        path.moveTo(ps[0].getX(), ps[0].getY());

        for (int i=1; i<ps.length; ++i)
        {
            path.lineTo(ps[i].getX(), ps[i].getY());
        }

        path.closePath();
        return new Area(path);
    }

    public static Area makeExpandedArea(Point2D p, double w)
    {
        double d = w * 2.0;
        double x = p.getX() - w;
        double y = p.getY() - w;

        Ellipse2D ellipse = new Ellipse2D.Double(x, y, d, d);
        return new Area(ellipse);
    }

    public static Line2D min(Line2D l1, Line2D l2)
    {
        if (l1 == null) return l2;
        if (l2 == null) return l1;

        double d1 = l1.getDirection().getLength();
        double d2 = l2.getDirection().getLength();
        return d1 < d2 ? l1 : l2;
    }

    public static java.awt.geom.Line2D convert2awt(Line2D l)
    {
        double x1 = l.getOrigin().getX();
        double x2 = l.getEndPoint().getX();
        double y1 = l.getOrigin().getY();
        double y2 = l.getEndPoint().getY();

        return new java.awt.geom.Line2D.Double(x1, y1, x2, y2);
    }
}
