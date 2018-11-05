package traffic.body;

import traffic.body.geom.*;
import traffic.util.*;

import rescuecore2.worldmodel.*;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.Blockade;

import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.Area;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.stream.*;

public class PassableShapeManager
{
    private Map<EntityID, Area> completeAreaShapes;
    private Map<EntityID, Area> simpleAreaShapes;

    private Map<EntityID, Area> completeBlockadeShapes;
    private Map<EntityID, Area> simpleBlockadeShapes;

    public PassableShapeManager(StandardWorldModel model)
    {
        this.completeAreaShapes = new HashMap<>();
        this.simpleAreaShapes = new HashMap<>();
        this.completeBlockadeShapes = new HashMap<>();
        this.simpleBlockadeShapes = new HashMap<>();

        List<rescuecore2.standard.entities.Area> areas = model.getAllEntities()
            .stream()
            .filter(e -> e instanceof rescuecore2.standard.entities.Area)
            .map(e -> (rescuecore2.standard.entities.Area)e)
            .collect(Collectors.toList());

        Map<EntityID, List<AreaOutline>> areaOutlines = makeAreaOutlines(areas);
        for (rescuecore2.standard.entities.Area area : areas)
        {
            EntityID id = area.getID();
            Area completeShape = ShrinkArea.run(area, areaOutlines);
            Area simpleShape = ShrinkArea.runSimply(area, areaOutlines);

            this.completeAreaShapes.put(id, completeShape);
            this.simpleAreaShapes.put(id, simpleShape);
        }
    }

    public Area computeSimple(EntityID id, StandardWorldModel model)
    {
        Area base = this.simpleAreaShapes.get(id);
        List<EntityID> blockadeIDs = collectOverlappingBlockades(id, model);

        for (EntityID blockadeID : blockadeIDs)
        {
            if (!this.simpleBlockadeShapes.containsKey(blockadeID))
            {
                Blockade blockade = (Blockade)model.getEntity(blockadeID);
                Area shape = ExpandBlockade.runSimply(blockade);
                this.simpleBlockadeShapes.put(blockadeID, shape);
            }

            Area subtractor = this.simpleBlockadeShapes.get(blockadeID);
            base.subtract(subtractor);
        }

        return base;
    }

    public Area computeComplete(EntityID id, StandardWorldModel model)
    {
        Area base = this.completeAreaShapes.get(id);
        List<EntityID> blockadeIDs = collectOverlappingBlockades(id, model);

        for (EntityID blockadeID : blockadeIDs)
        {
            if (!this.completeBlockadeShapes.containsKey(blockadeID))
            {
                Blockade blockade = (Blockade)model.getEntity(blockadeID);
                Area shape = ExpandBlockade.run(blockade);
                this.completeBlockadeShapes.put(blockadeID, shape);
            }

            Area subtractor = this.completeBlockadeShapes.get(blockadeID);
            base.subtract(subtractor);
        }

        return base;
    }

    public void update(ChangeSet changes)
    {
        for (EntityID id : changes.getChangedEntities())
        {
            this.completeBlockadeShapes.remove(id);
            this.simpleBlockadeShapes.remove(id);
        }
    }

    private static Map<EntityID, List<AreaOutline>> makeAreaOutlines(
        List<rescuecore2.standard.entities.Area> areas)
    {
        Map<EntityID, List<AreaOutline>> retval = new HashMap<>();
        for (rescuecore2.standard.entities.Area area : areas)
        {
            EntityID id = area.getID();
            double ar = Environment.AGENT_RADIUS;
            List<AreaOutline> outlines = AreaOutline.make(area, ar);
            retval.put(id, outlines);
        }

        return retval;
    }

    private static List<EntityID> collectOverlappingBlockades(
        EntityID id,
        StandardWorldModel model)
    {
        rescuecore2.standard.entities.Area area =
            (rescuecore2.standard.entities.Area)model.getEntity(id);

        List<EntityID> retval = new LinkedList<>();
        if (area.isBlockadesDefined()) retval.addAll(area.getBlockades());

        for (EntityID neighbourID : area.getNeighbours())
        {
            StandardEntity entity = model.getEntity(neighbourID);
            rescuecore2.standard.entities.Area neighbour =
                (rescuecore2.standard.entities.Area)entity;

            if (neighbour.isBlockadesDefined())
                retval.addAll(neighbour.getBlockades());
        }

        return retval;
    }

    //  @TEST
    private List<Shape> drawingShapes = new LinkedList<>();
    private List<Shape> areaShapes  = new LinkedList<>();
    private List<Shape> blockadeShapes  = new LinkedList<>();
    public void initTest(StandardWorldModel model)
    {
        List<rescuecore2.standard.entities.Area> areas = model.getAllEntities()
            .stream()
            .filter(e -> e instanceof rescuecore2.standard.entities.Area)
            .map(e -> (rescuecore2.standard.entities.Area)e)
            .collect(Collectors.toList());

        synchronized (this.areaShapes)
        {
            areas.stream()
                .map(rescuecore2.standard.entities.Area::getShape)
                .forEach(s -> this.areaShapes.add(s));
        }

        synchronized (this.blockadeShapes)
        {
            areas.stream()
                .filter(rescuecore2.standard.entities.Area::isBlockadesDefined)
                .flatMap(a -> a.getBlockades().stream())
                .map(i -> (Blockade)model.getEntity(i))
                .map(Blockade::getShape)
                .forEach(s -> this.blockadeShapes.add(s));
        }

        synchronized (this.drawingShapes)
        {
            for (rescuecore2.standard.entities.Area area : areas)
            {
                Area base = (Area)this.computeSimple(area.getID(), model).clone();
                //base.subtract(this.computeSimple(area.getID(), model));
                this.drawingShapes.add(base);
            }
        }
    }

    public void paint(Graphics2D g, AffineTransform transform)
    {
        synchronized (this.areaShapes)
        {
            for (Shape shape : this.areaShapes)
            {
                Shape transformed = transform.createTransformedShape(shape);
                g.setColor(Color.black);
                g.draw(transformed);
            }
        }

        synchronized (this.blockadeShapes)
        {
            for (Shape shape : this.blockadeShapes)
            {
                Shape transformed = transform.createTransformedShape(shape);
                g.setColor(Color.black);
                g.fill(transformed);
            }
        }

        synchronized (this.drawingShapes)
        {
            for (Shape shape : this.drawingShapes)
            {
                Shape transformed = transform.createTransformedShape(shape);
                g.setColor(Color.gray);
                g.fill(transformed);
            }
        }
    }
}
