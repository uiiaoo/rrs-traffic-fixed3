package traffic.body.time;

import traffic.util.*;

import rescuecore2.misc.geometry.*;
import java.util.*;
import java.util.stream.*;

public class Wall
{
    private Line2D wrapped;
    private Point2D closest;
    private Line2D line;

    public Wall(Line2D wrapped)
    {
        this.wrapped = wrapped;
    }

    public Line2D unwrap()
    {
        return this.wrapped;
    }

    public Point2D getClosest()
    {
        return this.closest;
    }

    public double getDistance()
    {
        return this.line.getDirection().getLength();
    }

    public Line2D getLine()
    {
        return this.line;
    }

    public void updateClosest(Point2D from)
    {
        this.closest =
            GeometryTools2D.getClosestPointOnSegment(this.unwrap(), from);
        this.line = new Line2D(from, this.closest);
    }

    public Vector2D getVector()
    {
        return this.line.getDirection();
    }

    public boolean isClosestPointEnd()
    {
        if (this.unwrap().getOrigin().equals(this.closest)) return true;
        if (this.unwrap().getEndPoint().equals(this.closest)) return true;
        return false;
    }

    public boolean hasLineOfSight(List<Wall> walls)
    {
        for (Wall wall : walls)
        {
            if (GeomUtil.isEqual(this.unwrap(), wall.getLine())) break;

            if (this.closest.equals(wall.unwrap().getOrigin()) ||
                this.closest.equals(wall.unwrap().getEndPoint())) continue;

            double dotp = this.getVector().dot(wall.getVector());
            if (dotp < wall.getDistance()*wall.getDistance()) continue;

            if (GeometryTools2D.getSegmentIntersectionPoint(this.line, wall.unwrap()) != null)
                return false;
        }

        return true;
    }
}
