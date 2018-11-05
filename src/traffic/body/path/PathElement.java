package traffic.body.path;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.misc.geometry.*;

public class PathElement
{
    private EntityID id;
    private Line2D line;

    public PathElement(EntityID id, Line2D line)
    {
        this.id = id;
        this.line = line;
    }

    public EntityID getID()
    {
        return this.id;
    }

    public Line2D getLine()
    {
        return this.line;
    }

    public void updateOrigin(Point2D origin)
    {
        this.line = new Line2D(origin, this.line.getEndPoint());
    }
}
