package traffic.body.time;

import traffic.body.path.*;
import traffic.util.*;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.*;
import rescuecore2.misc.geometry.*;

import java.util.*;
import java.util.stream.*;

public class Time
{
    private static final int MICROSTEPS = 600;
    private static final double MICROSTEP_TIME_MS = 100.0;

    public static void run(Collection<Agent> agents, StandardWorldModel model)
    {
        Map<EntityID, List<Wall>> walls = new HashMap<>();
        for (Agent agent : agents)
        {
            EntityID id = agent.unwrap().getID();
            walls.put(id, extractWalls(agent, model));
        }

        for (int i=0; i<MICROSTEPS; ++i)
        {
            microstep(agents, MICROSTEP_TIME_MS, walls, model);
        }
    }

    public static void microstep(
        Collection<Agent> agents,
        double dt,
        Map<EntityID, List<Wall>> walls,
        StandardWorldModel model)
    {
        for (Agent agent : agents)
        {
            updateWalls(agent, walls.get(agent.unwrap().getID()), dt);
            agent.updateNextPathElement();
            Vector2D force = computeForces(agent, agents, walls.get(agent.unwrap().getID()), dt);
            updatePosition(agent, dt, force, walls.get(agent.unwrap().getID()));
        }
    }

    private static void updateWalls(Agent agent, List<Wall> walls, double dt)
    {
        Point2D xy = agent.getXY();
        //double cutoff =
        //    Math.max(dt*agent.getVelocityLimit(), Environment.WALL_DISTANCE_CUTOFF);

        for (Wall wall : walls)
        {
            wall.updateClosest(xy);
        }

        walls.sort((w1, w2) -> Double.compare(w1.getDistance(), w2.getDistance()));
    }

    private static Vector2D computeForces(Agent agent, Collection<Agent> agents, List<Wall> walls, double dt)
    {
        agent.setColocated(false);
        Vector2D agentsForce = computeAgentsForce(agent, agents);
        Vector2D force = new Vector2D(0.0, 0.0);
        force = force.add(agentsForce);

        if (!agent.isColocated())
        {
            Vector2D destinationForce = computeDestinationForce(agent);
            force = force.add(destinationForce);
            Vector2D wallsForce = computeWallsForce(agent, dt, walls, destinationForce, agentsForce);
            force = force.add(wallsForce);
        }

        return force;
    }

    private static Vector2D computeAgentsForce(Agent agent, Collection<Agent> agents)
    {
        double cutoff = Environment.AGENT_DISTANCE_CUTOFF;
        double a = Environment.AGENT_FORCE_COEF_A;
        double b = Environment.AGENT_FORCE_COEF_B;
        double k = Environment.AGENT_FORCE_COEF_K;
        double limit = Environment.AGENT_FORCE_LIMIT;

        double xsum = 0.0;
        double ysum = 0.0;

        for (Agent other : agents)
        {
            if (agent.unwrap().getID().equals(other.unwrap().getID())) continue;
            if (!agent.canMove()) continue;

            double dx = other.getXY().getX() - agent.getXY().getX();
            if (Math.abs(dx) > cutoff) continue;
            double dy = other.getXY().getY() - agent.getXY().getY();
            if (Math.abs(dy) > cutoff) continue;

            double totalr = Environment.AGENT_RADIUS*2.0;
            double dist = Math.hypot(dx, dy);

            if (dist == 0.0)
            {
                xsum = Environment.getColocatedAgentNudge();
                ysum = Environment.getColocatedAgentNudge();
                agent.setColocated(true);
                break;
            }

            double dxn = dx / dist;
            double dyn = dy / dist;

            double negativeSeparation = totalr - dist;
            double tmp = -a * Math.exp(negativeSeparation * b);

            xsum += tmp * dxn;
            ysum += tmp * dyn;

            if (negativeSeparation > 0.0)
            {
                xsum += -k * negativeSeparation * dxn;
                ysum += -k * negativeSeparation * dyn;
            }
        }

        double forcesum = Math.hypot(xsum, ysum);
        if (forcesum > limit)
        {
            //double rate = limit / forcesum;
            //forcesum = limit;
            //xsum *= rate;
            //ysum *= rate;
            forcesum /= limit;
            xsum /= forcesum;
            ysum /= forcesum;
        }

        return new Vector2D(xsum, ysum);
    }

    private static final double DDD = 0.001;
    private static final double SSS_1 = 0.0001;
    private static final double SSS_2 = 0.0002;

    private static Vector2D computeDestinationForce(Agent agent)
    {
        Vector2D velocity = agent.getVelocity();
        PathElement nextelem = agent.getNextPathElement();
        if (nextelem == null)
        {
            return new Vector2D(SSS_1*-velocity.getX(), SSS_1*-velocity.getY());
        }

        Point2D dest = nextelem.getLine().getEndPoint();
        double dx = dest.getX() - agent.getXY().getX();
        double dy = dest.getY() - agent.getXY().getY();

        double dist = Math.hypot(dx, dy);
        if (dist != 0.0)
        {
            dx /= dist;
            dy /= dist;
        }

        if (dest.equals(agent.getFinalDestination()))
        {
            dx = Math.min(agent.getVelocityLimit(), DDD*dist) * dx;
            dy = Math.min(agent.getVelocityLimit(), DDD*dist) * dy;
        }
        else
        {
            dx = agent.getVelocityLimit() * dx;
            dy = agent.getVelocityLimit() * dy;
        }

        return new Vector2D(
            SSS_2*(dx-velocity.getX()),
            SSS_2*(dy-velocity.getY()));
    }

    private static Vector2D computeWallsForce(
        Agent agent,
        double dt,
        List<Wall> walls,
        Vector2D destinationForce,
        Vector2D agentsForce)
    {
        double xsum = 0.0;
        double ysum = 0.0;

        Point2D xy = agent.getXY();

        double r = Environment.AGENT_RADIUS;
        double cutoff = Environment.WALL_DISTANCE_CUTOFF;
        double b = Environment.WALL_FORCE_COEF_B;

        for (Wall wall : walls)
        {
            if (wall.getDistance() > cutoff) break;
            if (!wall.hasLineOfSight(walls)) continue;

            Vector2D velocity = agent.getVelocity();
            double currentFX = destinationForce.getX() + agentsForce.getX();
            double currentFY = destinationForce.getY() + agentsForce.getY();
            double expectedVX = velocity.getX() + dt * currentFX;
            double expectedVY = velocity.getY() + dt * currentFY;
            Vector2D expectedVelocity = new Vector2D(expectedVX, expectedVY);
            Vector2D wallForceVector = wall.getVector().scale(-1.0/wall.getDistance());
            double radii = wall.getDistance() / r;

            double magnitude = -expectedVelocity.dot(wallForceVector);
            if (magnitude < 0.0 || radii >= 1.0)
            {
                magnitude = 0.0;
            }
            else
            if (radii < 1.0)
            {
                double d = Math.exp(-(radii-1.0)*b);
                if (d < 1.0) d = 0.0;
                magnitude *= d;
                if (wall.isClosestPointEnd()) magnitude /= 2.0;
            }

            Vector2D stopForce = wallForceVector.scale(magnitude / dt);
            xsum += stopForce.getX();
            ysum += stopForce.getY();
        }

        return new Vector2D(xsum, ysum);
    }

    private static void updatePosition(Agent agent, double dt, Vector2D force, List<Wall> walls)
    {
        double newVX = agent.getVelocity().getX() + dt*force.getX();
        double newVY = agent.getVelocity().getY() + dt*force.getY();
        //test
        //if (newVX != 0.0 || newVY != 0.0)
        //    System.out.println("[" + agent.unwrap().getID() + "]" + newVY + ", " + newVY);
        //
        double v = Math.hypot(newVX, newVY);
        if (v > agent.getVelocityLimit())
        {
            //double rate = agent.getVelocityLimit()/ v;
            //v = agent.getVelocityLimit();
            //newVX *= rate;
            //newVY *= rate;
            v /= agent.getVelocityLimit();
            newVX /= v;
            newVY /= v;
        }

        double x = agent.getXY().getX() + dt*newVX;
        double y = agent.getXY().getY() + dt*newVY;

        Line2D moveline = new Line2D(agent.getXY(), new Point2D(x, y));
        for (Wall wall : walls)
        {
            if (wall.getDistance() > moveline.getDirection().getLength()) break;
            Point2D intersection =
                GeometryTools2D.getSegmentIntersectionPoint(wall.unwrap(), moveline);
            if (intersection != null)
            {
                agent.setVelocity(new Vector2D(0.0, 0.0));
                return;
            }
        }
 
        agent.setVelocity(new Vector2D(newVX, newVY));
        agent.setXY(x, y);
    }

    private static List<Wall> extractWalls(Agent agent, StandardWorldModel model)
    {
        List<Wall> retval = new LinkedList<>();
        List<PathElement> elems = agent.getPath();
        if (elems == null) return retval;

        Set<EntityID> completed = new HashSet<>();

        for (PathElement elem : elems)
        {
            Area area = (Area)model.getEntity(elem.getID());

            if (!completed.contains(elem.getID()))
            {
                completed.add(elem.getID());

                List<Wall> walls = area.getEdges()
                    .stream()
                    .filter(e -> !e.isPassable())
                    .map(Edge::getLine)
                    .map(l -> new Wall(l))
                    .collect(Collectors.toList());
                retval.addAll(walls);

                if (area.isBlockadesDefined())
                {
                    area.getBlockades()
                        .stream()
                        .map(i -> (Blockade)model.getEntity(i))
                        .flatMap(b -> GeometryTools2D.pointsToLines(
                            GeometryTools2D.vertexArrayToPoints(
                                b.getApexes()), true).stream())
                        .map(l -> new Wall(l))
                        .forEach(l -> retval.add(l));
                }
            }

            for (EntityID neighbourID : area.getNeighbours())
            {
                Area neighbour = (Area)model.getEntity(neighbourID);
                if (!completed.contains(neighbourID))
                {
                    completed.add(neighbourID);

                    List<Wall> walls = neighbour.getEdges()
                        .stream()
                        .filter(e -> !e.isPassable())
                        .map(Edge::getLine)
                        .map(l -> new Wall(l))
                        .collect(Collectors.toList());
                    retval.addAll(walls);

                    if (neighbour.isBlockadesDefined())
                    {
                        neighbour.getBlockades()
                            .stream()
                            .map(i -> (Blockade)model.getEntity(i))
                            .flatMap(b -> GeometryTools2D.pointsToLines(
                                GeometryTools2D.vertexArrayToPoints(
                                    b.getApexes()), true).stream())
                            .map(l -> new Wall(l))
                            .forEach(l -> retval.add(l));
                    }
                }
            }
        }

        return retval;
    }
}
