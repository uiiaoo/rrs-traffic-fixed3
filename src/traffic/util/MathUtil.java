package traffic.util;

import rescuecore2.misc.geometry.*;

public class MathUtil
{
    public static double cross(Point2D p1, Point2D p2)
    {
        return cross(
            p1.getX(), p1.getY(),
            p2.getX(), p2.getY());
    }

    public static double cross(Line2D l1, Line2D l2)
    {
        Vector2D v1 = l1.getDirection();
        Vector2D v2 = l2.getDirection();
        return cross(v1.getX(), v1.getY(), v2.getX(), v2.getY());
    }

    public static double cross(
        double x1, double y1,
        double x2, double y2)
    {
        return x1*y2 - y1*x2;
    }

    public static double angle(Line2D l1, Line2D l2)
    {
        Point2D p1 = l1.getOrigin();
        Point2D p2 = l1.getEndPoint();
        Point2D p3 = l2.getOrigin();
        Point2D p4 = l2.getEndPoint();

        if (p2.equals(p3)) return angle(p1, p2, p4);
        if (p4.equals(p1)) return angle(p3, p4, p1);

        return Double.NaN;
    }

    public static double angle(Point2D p1, Point2D p2, Point2D p3)
    {
        double t1 = Math.atan2(p2.getY()-p1.getY(), p2.getX()-p1.getX());
        double t2 = Math.atan2(p3.getY()-p2.getY(), p3.getX()-p2.getX());
        double theta = t2 - t1;

        if (theta >   Math.PI) theta -= Math.PI*2.0;
        if (theta <= -Math.PI) theta += Math.PI*2.0;

        return theta;
    }
}
