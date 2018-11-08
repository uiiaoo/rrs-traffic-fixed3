package traffic.body.path;

import traffic.util.*;

import rescuecore2.worldmodel.*;
import rescuecore2.standard.entities.*;
import rescuecore2.misc.geometry.*;

import java.util.*;

public class Agent
{
    private Human wrapped;
    private boolean executable;

    private List<PathElement> path;

    private Point2D location;
    private List<Point2D> history;

    private boolean colocated;
    private Vector2D velocity;
    private double velocityLimit;

    private EntityID position;

    public Agent(Human human)
    {
        this.wrapped = human;
        this.location = new Point2D(this.unwrap().getX(), this.unwrap().getY());

        this.colocated = false;
        this.velocity = new Vector2D(0.0, 0.0);
        this.velocityLimit = this.makeVelocityLimit();
    }

    public Human unwrap()
    {
        return this.wrapped;
    }

    public void update(StandardWorldModel model)
    {
        //  update executable
        EntityID position = this.unwrap().getPosition();
        this.executable = model.getEntity(position) instanceof Area;

        if (this.unwrap() instanceof Civilian &&
            model.getEntity(position) instanceof Refuge)
        {
            this.executable = false;
        }

        this.path = null;
        this.history = new LinkedList<>();

        this.unwrap().undefinePositionHistory();
        this.unwrap().setTravelDistance(0);
    }

    public Point2D getXY()
    {
        return this.location;
    }

    public void setPath(List<PathElement> path)
    {
        this.path = new LinkedList<>();
        if (path.isEmpty()) return;

        EntityID id = path.get(0).getID();
        Line2D line = path.get(0).getLine();
        for (int i=1; i<path.size(); ++i)
        {
            PathElement next = path.get(i);
            if (next.getID().equals(id) &&
                GeometryTools2D.parallel(line, next.getLine()))
            {
                line = new Line2D(line.getOrigin(), next.getLine().getEndPoint());
            }
            else
            {
                this.path.add(new PathElement(id, line));
                id = next.getID();
                line = next.getLine();
            }
        }

        this.path.add(new PathElement(id, line));
    }

    public void restrictAction()
    {
        this.executable = false;
    }

    public void reflect(ChangeSet changes, StandardWorldModel model)
    {
        this.reflectToWrapped(model);

        Human unwrapped = this.unwrap();
        changes.addChange(unwrapped, unwrapped.getXProperty());
        changes.addChange(unwrapped, unwrapped.getYProperty());
        changes.addChange(unwrapped, unwrapped.getPositionProperty());
        changes.addChange(unwrapped, unwrapped.getPositionHistoryProperty());
        changes.addChange(unwrapped, unwrapped.getTravelDistanceProperty());
    }

    public void load(Agent target)
    {
        this.restrictAction();
        target.loaded(this);
    }

    public void loaded(Agent agent)
    {
        this.restrictAction();
        this.location = null;

        this.unwrap().setPosition(agent.unwrap().getID());
        this.unwrap().undefineY();
        this.unwrap().undefineX();
    }

    public void unload(Agent target)
    {
        this.restrictAction();
        target.unloaded(this);
    }

    public void unloaded(Agent agent)
    {
        this.restrictAction();
        this.location =
            new Point2D(agent.unwrap().getX(), agent.unwrap().getY());

        this.unwrap().setPosition(agent.unwrap().getPosition());
        this.unwrap().setX(agent.unwrap().getX());
        this.unwrap().setY(agent.unwrap().getY());
    }

    public boolean canMove()
    {
        if (this.unwrap() instanceof Civilian &&
            this.unwrap().isDamageDefined() &&
            this.unwrap().getDamage() > 0)
        {
            return false;
        }

        return this.canExecuteAction();
    }

    public boolean canLoad(Agent target)
    {
        if (!this.canExecuteAction())
            return false;

        if (!(this.unwrap() instanceof AmbulanceTeam))
            return false;

        if (!(target.unwrap() instanceof Civilian))
            return false;

        if (target.unwrap().isBuriednessDefined() &&
            target.unwrap().getBuriedness() > 0)
        {
            return false;
        }

        EntityID position = this.unwrap().getPosition();
        return position.equals(target.unwrap().getPosition());
    }

    public boolean canUnload()
    {
        if (!this.canExecuteAction())
            return false;

        if (!(this.unwrap() instanceof AmbulanceTeam))
            return false;

        return true;
    }

    public boolean isLoaded(Agent agent)
    {
        return this.unwrap().getPosition().equals(agent.unwrap().getID());
    }

    private boolean canExecuteAction()
    {
        if (this.unwrap().isHPDefined() &&
            this.unwrap().getHP() <= 0)
        {
            return false;
        }

        if (this.unwrap().isBuriednessDefined() &&
            this.unwrap().getBuriedness() > 0)
        {
            return false;
        }

        return this.executable;
    }

    private void reflectToWrapped(StandardWorldModel model)
    {
        if (this.history.isEmpty())
            return;

        //  dedupe position history
        List<Point2D> deduped = new LinkedList<>();
        deduped.add(this.history.get(0));
        for (int i=1; i<this.history.size(); ++i)
        {
            Point2D point = this.history.get(i);
            Point2D last = deduped.get(deduped.size()-1);
            if (!point.equals(last)) deduped.add(point);
        }

        //  set position history
        int[] positionHistory = new int[deduped.size()*2];
        for (int i=0; i<deduped.size(); ++i)
        {
            Point2D point = deduped.get(i);
            positionHistory[i*2+0] = (int)point.getX();
            positionHistory[i*2+1] = (int)point.getY();
        }

        this.unwrap().setPositionHistory(positionHistory);

        //  set x and y
        Point2D last = deduped.get(deduped.size()-1);
        this.unwrap().setX((int)last.getX());
        this.unwrap().setY((int)last.getY());

        //  set travel distance
        double travelDistance = 0.0;
        List<Line2D> lines = GeometryTools2D.pointsToLines(deduped, false);

        for (Line2D line : lines)
        {
            Vector2D vec = line.getDirection();
            travelDistance += vec.getLength();
        }

        this.unwrap().setTravelDistance((int)travelDistance);

        EntityID position = this.unwrap().getPosition();
        if (this.canMove())
        {
            Collection<StandardEntity> entities =
                model.getObjectsInRange((int)this.location.getX(), (int)this.location.getY(), 100);

            for (StandardEntity entity : entities)
            {
                if (!(entity instanceof Area)) continue;
                Area area = (Area)entity;

                if (area.getShape().contains(this.location.getX(), this.location.getY()))
                {
                    position = area.getID();
                    break;
                }
            }
        }
        this.unwrap().setPosition(position);
    }

    private double makeVelocityLimit()
    {
        StandardEntityURN urn = this.unwrap().getStandardURN();
        return Environment.AGENT_VELOCITY_MEAN;
    }

    public List<PathElement> getPath()
    {
        return this.path;
    }

    public void updateNextPathElement()
    {
        if (this.path == null || this.path.isEmpty()) return;


        while (this.path.size() >= 2)
        {
            Line2D l1 = this.path.get(0).getLine();
            Line2D l2 = this.path.get(1).getLine();

            double d1 = GeometryTools2D.getDistance(
                GeometryTools2D.getClosestPointOnSegment(l1, this.location),
                this.location);
            double d2 = GeometryTools2D.getDistance(
                GeometryTools2D.getClosestPointOnSegment(l2, this.location),
                this.location);

            if (d1 < d2) break;

            this.path.remove(0);
        }

        this.path.get(0).updateOrigin(this.location);
    }

    public void setXY(double x, double y)
    {
        this.location = new Point2D(x, y);
        this.history.add(this.location);
    }

    public PathElement getNextPathElement()
    {
        if (this.path == null || this.path.isEmpty()) return null;
        return this.path.get(0);
    }

    public Point2D getFinalDestination()
    {
        if (this.path == null || this.path.isEmpty()) return null;
        return this.path.get(this.path.size()-1).getLine().getEndPoint();
    }

    public double getVelocityLimit()
    {
        return this.velocityLimit;
    }

    public boolean isColocated()
    {
        return this.colocated;
    }

    public void setColocated(boolean colocated)
    {
        this.colocated = colocated;
    }

    public Vector2D getVelocity()
    {
        return this.velocity;
    }

    public void setVelocity(Vector2D velocity)
    {
        this.velocity = velocity;
    }
}
