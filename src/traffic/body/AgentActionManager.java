package traffic.body;

import traffic.body.geom.*;
import traffic.body.path.*;
import traffic.body.time.*;
import traffic.util.*;

import rescuecore2.worldmodel.*;
import rescuecore2.standard.entities.*;
import rescuecore2.misc.geometry.*;

import java.awt.Shape;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;

import java.util.*;
import java.util.stream.*;

public class AgentActionManager
{
    private CellManager cellManager;
    private Map<EntityID, Agent> agents;

    public AgentActionManager(
        CellManager cellManager,
        StandardWorldModel model)
    {
        this.cellManager = cellManager;
        this.agents = makeWrappedObjects(model);
    }

    public void update(ChangeSet changes, StandardWorldModel model)
    {
        this.cellManager.update(changes, model);

        synchronized (this.agents)
        {
        for (Agent agent : this.agents.values())
            agent.update(model);
        }
    }

    public void moveAgent(EntityID agentID, List<EntityID> path, Point2D dest, StandardWorldModel model)
    {
        AgentLog.debug(agentID, "try to execute MOVE command");

        synchronized (this.agents)
        {
        if (!this.agents.containsKey(agentID))
        {
            AgentLog.error(agentID, "is not a human");
            return;
        }

        Agent agent = this.agents.get(agentID);

        if (!agent.canMove())
        {
            AgentLog.error(agentID, "is not meet requirements");
            return;
        }

        //  correct path
        EntityID position = agent.unwrap().getPosition();
        if (path.isEmpty() || !position.equals(path.get(0)))
        {
            path.add(0, position);
        }

        //  execute command
        Map<EntityID, List<Cell>> areaCells = new HashMap<>();
        for (EntityID id : path)
        {
            if (!(model.getEntity(id) instanceof Area)) return;
            areaCells.put(id, this.cellManager.computeCells(id, model));
        }

        if (dest == null)
        {
            EntityID id = path.get(path.size()-1);
            Area area = (Area)model.getEntity(id);
            dest = new Point2D((double)area.getX(), (double)area.getY());
        }

        Map<EntityID, List<Cell>> neededCells = Cell.updateNeighbourJunctions(areaCells, path);
        List<PathElement> elems = PathPlanning.run(agent, path, neededCells, dest, model);

        AgentLog.debug(agentID, "execute MOVE command");
        agent.setPath(elems);
        }
    }

    public void loadHumanToAgent(EntityID agentID, EntityID humanID)
    {
        AgentLog.debug(agentID, "try to execute LOAD command -> @" + humanID);

        if (!this.agents.containsKey(agentID))
        {
            AgentLog.error(agentID, "is not a human");
            return;
        }

        if (!this.agents.containsKey(humanID))
        {
            AgentLog.error(agentID, "@" + humanID + " is not a human");
            return;
        }

        Agent agent = this.agents.get(agentID);
        Agent human = this.agents.get(humanID);

        if (!agent.canLoad(human))
        {
            AgentLog.error(agentID, "is not meet requirements");
            return;
        }

        //  check other humans
        for (Agent other : this.agents.values())
        {
            if (other.isLoaded(agent))
            {
                EntityID otherID = other.unwrap().getID();
                AgentLog.error(agentID, "has already LOADed @" + otherID);
                return;
            }
        }

        //  execute command
        AgentLog.debug(agentID, "execute LOAD command -> @" + humanID);
        agent.load(human);
    }

    public void unloadHumanFromAgent(EntityID agentID)
    {
        AgentLog.debug(agentID, "try to execute UNLOAD command");

        if (!this.agents.containsKey(agentID))
        {
            AgentLog.error(agentID, "is not a human");
            return;
        }

        Agent agent = this.agents.get(agentID);

        if (!agent.canUnload())
        {
            AgentLog.error(agentID, "is not meet requirements");
            return;
        }

        //  seek target
        for (Agent human : this.agents.values())
        {
            if (!human.isLoaded(agent))
            {
                continue;
            }

            //  execute command
            EntityID humanID = human.unwrap().getID();
            AgentLog.debug(agentID, "execute UNLOAD command -> @" + humanID);
            agent.unload(human);
        }
    }

    public void preventAgentAction(EntityID agentID)
    {
        if (!this.agents.containsKey(agentID))
        {
            return;
        }

        Agent agent = this.agents.get(agentID);
        agent.restrictAction();
    }

    public ChangeSet makeChangeSet(StandardWorldModel model)
    {
        ChangeSet ret = new ChangeSet();
        this.decideOnMovement(model);

        for (Agent agent : this.agents.values())
        {
            agent.reflect(ret, model);
        }

        return ret;
    }

    private static Map<EntityID, Agent> makeWrappedObjects(
        StandardWorldModel model)
    {
        Map<EntityID, Agent> agents = new HashMap<>();

        for (StandardEntity e : model)
        {
            if (!(e instanceof Human)) continue;

            Human human = (Human)e;
            Agent agent = new Agent(human);

            EntityID id = e.getID();
            agents.put(id, agent);
        }

        return agents;
    }

    private void decideOnMovement(StandardWorldModel model)
    {
        Time.run(this.agents.values(), model);
    }

    public void paint(Graphics2D g, AffineTransform transform)
    {
        synchronized (this.agents)
        {
            for (Agent agent : this.agents.values())
            {
                if (agent.getPath() == null) continue;
                for (PathElement elem : agent.getPath())
                {
                    Shape shape = GeomUtil.convert2awt(elem.getLine());
                    Shape transformed = transform.createTransformedShape(shape);
                    g.setColor(Color.blue);
                    g.draw(transformed);
                }
            }
        }
    }
}
