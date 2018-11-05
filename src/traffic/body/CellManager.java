package traffic.body;

import traffic.body.geom.*;
import traffic.util.*;

import rescuecore2.worldmodel.*;
import rescuecore2.standard.entities.*;
import rescuecore2.misc.geometry.*;

import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.stream.*;

public class CellManager 
{
    private PassableShapeManager passableShapeManager;
    private Map<EntityID, List<Cell>> areaCells;

    public CellManager(PassableShapeManager passableShapeManager)
    {
        this.passableShapeManager = passableShapeManager;
        this.areaCells = new HashMap<>();
    }

    public List<Cell> computeCells(EntityID id, StandardWorldModel model)
    {
        if (!this.areaCells.containsKey(id))
            this.areaCells.put(id, makeAreaCell(id, this.passableShapeManager, model));

        return this.areaCells.get(id);
    }

    public void update(ChangeSet changes, StandardWorldModel model)
    {
        this.passableShapeManager.update(changes);

        changes.getChangedEntities()
            .stream()
            .map(i -> model.getEntity(i))
            .filter(e -> e instanceof Blockade)
            .map(e -> (Blockade)e)
            .map(Blockade::getPosition)
            .forEach(i -> this.areaCells.remove(i));

        changes.getChangedEntities()
            .stream()
            .map(i -> model.getEntity(i))
            .filter(e -> e instanceof Blockade)
            .map(e -> (Blockade)e)
            .map(Blockade::getPosition)
            .map(i -> model.getEntity(i))
            .map(e -> (Area)e)
            .flatMap(a -> a.getNeighbours().stream())
            .forEach(i -> this.areaCells.remove(i));
    }

    private static List<Cell> makeAreaCell(
        EntityID id,
        PassableShapeManager passableShapeManager,
        StandardWorldModel model)
    {
        Area area = (Area)model.getEntity(id);

        java.awt.geom.Area simpleShape =
            passableShapeManager.computeSimple(id, model);

        List<Cell> cells = DecomposeShape.run(simpleShape, area);

        java.awt.geom.Area completeShape =
            passableShapeManager.computeComplete(id, model);
        java.awt.geom.Area diff = (java.awt.geom.Area)simpleShape.clone();
        diff.subtract(completeShape);

        Cell.merge(cells, diff, area.getEdges());
        return cells;
    }

    //  @TEST
    private List<Shape> separatorShapes = new LinkedList<>();
    private List<Shape> networkShapes = new LinkedList<>();
    public void initTest(StandardWorldModel model)
    {
        List<Area> areas = model.getAllEntities()
            .stream()
            .filter(e -> e instanceof Area)
            .map(e -> (Area)e)
            .collect(Collectors.toList());

        for (Area area : areas)
        {
            List<Cell> cells = this.computeCells(area.getID(), model);
            for (Cell cell : cells)
            {
                List<Line2D> outlines = cell.getOutlines();
                for (Line2D outline : outlines)
                {
                    synchronized (this.separatorShapes)
                    {
                        this.separatorShapes.add(GeomUtil.convert2awt(outline));
                    }
                }

                for (int n : cell.getJunctions().keySet())
                {
                    synchronized (this.networkShapes)
                    {
                        this.networkShapes.add(GeomUtil.convert2awt(new Line2D(
                            cell.getCentroid(),
                            cell.getJunctions().get(n))));
                    }
                }
            }
        }
    }

    public void paint(Graphics2D g, AffineTransform transform)
    {
        synchronized (this.separatorShapes)
        {
            for (Shape shape : this.separatorShapes)
            {
                Shape transformed = transform.createTransformedShape(shape);
                g.setColor(Color.red);
                g.draw(transformed);
            }
        }
        synchronized (this.networkShapes)
        {
            for (Shape shape : this.networkShapes)
            {
                Shape transformed = transform.createTransformedShape(shape);
                g.setColor(Color.blue);
                g.draw(transformed);
            }
        }
    }
}
