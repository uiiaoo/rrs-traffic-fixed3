package traffic.body.geom;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.Edge;
import rescuecore2.misc.geometry.*;
import rescuecore2.misc.Pair;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.stream.*;

import traffic.util.*;

public class Cell
{
    private Area shape;
    private Point2D centroid;

    private List<Line2D> outlines;
    private Map<Integer, Point2D> junctions;
    private Map<Pair<EntityID, Integer>, Point2D> neighbourJunctions;
    private List<EntityID> neighbours;

    public Cell(
        Area shape,
        List<Line2D> outlines,
        Map<Integer, Point2D> junctions,
        List<EntityID> neighbours)
    {
        this.shape = shape;
        this.outlines = outlines;
        this.junctions = junctions;
        this.neighbours = neighbours;
        this.neighbourJunctions = new HashMap<>();

        PathIterator pi = shape.getPathIterator(null);
        List<Point2D> apexes = GeomUtil.toUnique(GeomUtil.toPointList(pi).get(0));
        this.centroid = GeomUtil.computeCentroid(apexes);
    }

    public Area getShape()
    {
        return this.shape;
    }

    public Point2D getCentroid()
    {
        return this.centroid;
    }

    public List<Line2D> getOutlines()
    {
        return this.outlines;
    }

    public Map<Integer, Point2D> getJunctions()
    {
        return this.junctions;
    }

    public Map<Pair<EntityID, Integer>, Point2D> getNeighbourJunctions()
    {
        return this.neighbourJunctions;
    }

    public List<EntityID> getNeighbours()
    {
        return this.neighbours;
    }

    public static List<Cell> make(
        List<Line2D> separators,
        boolean[][] neighbourMatrix,
        List<Edge> edges)
    {
        //  make using
        int n = separators.size();
        boolean[][] using = new boolean[n][n];
        for (boolean[] row : using) Arrays.fill(row, false);

        for (int i=0; i<n; ++i)
        {
            Integer ri = new Integer(i);
            if (IntStream.range(0, i).filter(j -> using[j][ri]).count() >= 2)
                continue;

            using[i][i] = true;

            double ix = separators.get(i).getOrigin().getX();
            int e = i;
            for (int j=n-1; j>i; --j)
            {
                double jx = separators.get(j).getOrigin().getX();
                if (!neighbourMatrix[ix != jx ? i : e][j]) continue;
                if (e == i) e = j;

                using[i][j] = true;
            }

            double ex = separators.get(e).getOrigin().getX();
            if (ix == ex) Arrays.fill(using[i], false);
        }

        //  make cells
        List<Cell> retval = new LinkedList<>();
        for (int i=0; i<n; ++i)
        {
            if (!using[i][i]) continue;

            //  decide begin and end line
            Line2D begin = separators.get(i);
            double bx = begin.getOrigin().getX();
            Line2D end = null;

            for (int j=i+1; j<n; ++j)
            {
                if (!using[i][j]) continue;
                Line2D next = separators.get(j);

                boolean isBegin = bx == next.getOrigin().getX();
                if (isBegin) begin = join(begin, next);
                else end = join(end, next);
            }
            Area shape = GeomUtil.makeArea(begin, end);

            //  make outlines with neighbour cell's number
            List<Line2D> outlines = new LinkedList<>();
            Map<Integer, Point2D> junctions = new HashMap<>();
            for (int j=i; j<n; ++j)
            {
                if (!using[i][j]) continue;

                int next = -1;
                for (int k=0; k<n; ++k) if (k != i && using[k][j]) next = k;

                int skips = 0;
                for (int k=0; k<next; ++k) if (!using[k][k]) skips++; 

                outlines.add(separators.get(j));
                if (next-skips >= 0)
                    junctions.put(
                        next-skips,
                        GeomUtil.makeMedian(separators.get(j)));
            }

            Line2D top = new Line2D(begin.getOrigin(), end.getOrigin());
            Line2D btm = new Line2D(begin.getEndPoint(), end.getEndPoint());
            outlines.add(top);
            outlines.add(btm);

            //  make outlines with neighbour area ID
            List<EntityID> neighbours = new LinkedList<>();
            for (Line2D outline : outlines)
            {
                List<EntityID> neighbourIDs = edges
                    .stream()
                    .filter(Edge::isPassable)
                    .filter(e -> GeomUtil.isOverlapping(e.getLine(), outline))
                    .map(Edge::getNeighbour)
                    .collect(Collectors.toList());

                neighbours.addAll(neighbourIDs);
            }

            Cell cell = new Cell(shape, outlines, junctions, neighbours);
            retval.add(cell);
        }

        return retval;
    }

    private static Line2D join(Line2D line1, Line2D line2)
    {
        if (line1 == null) return line2;
        if (line2 == null) return line1;

        Point2D op1 = line1.getOrigin();
        Point2D ep1 = line1.getEndPoint();
        Point2D op2 = line2.getOrigin();
        Point2D ep2 = line2.getEndPoint();

        Point2D op = op1.getY() <= op2.getY() ? op1 : op2;
        Point2D ep = ep1.getY() >= ep2.getY() ? ep1 : ep2;

        return new Line2D(op, ep);
    }

    public static void merge(List<Cell> cells, Area diff, List<Edge> edges)
    {
        List<Area> shapes = GeomUtil.toSingulars(diff);
        List<Cell> newCells = new LinkedList<>();

        for (Area shape : shapes)
        {
            List<Line2D> outlines1 = GeomUtil.extractStraightLines(
                shape.getPathIterator(null));

            Map<Integer, Point2D> junctions = new HashMap<>();
            List<EntityID> neighbours = new LinkedList<>();

            for (Line2D outline1 : outlines1)
            {
                for (int i=0; i<cells.size(); ++i)
                {
                    List<Line2D> outlines2 = cells.get(i).getOutlines();
                    for (Line2D outline2 : outlines2)
                    {
                        Line2D overlapping =
                            GeomUtil.computeOverlapping(outline1, outline2);
                        if (overlapping == null) continue;

                        Point2D junction = GeomUtil.makeMedian(overlapping);
                        junctions.put(i, junction);
                        cells.get(i).getJunctions().put(cells.size()+newCells.size(), junction);
                    }
                }

                for (Edge edge : edges)
                {
                    if (!edge.isPassable()) continue;

                    if (GeomUtil.isOverlapping(edge.getLine(), outline1))
                        neighbours.add(edge.getNeighbour());
                }
            }

            Cell cell = new Cell(shape, outlines1, junctions, neighbours);
            newCells.add(cell);
        }

        cells.addAll(newCells);
    }

    public static Map<EntityID, List<Cell>> updateNeighbourJunctions(
        Map<EntityID, List<Cell>> areaCells,
        List<EntityID> path)
    {
        for (EntityID id : path)
        {
            List<Cell> cells = areaCells.get(id);
            cells
                .stream()
                .forEach(c -> c.getNeighbourJunctions().clear());
        }

        for (int i=0; i<path.size()-1; ++i)
        {
            EntityID id1 = path.get(i  );
            EntityID id2 = path.get(i+1);

            List<Cell> cells1 = areaCells.get(id1);
            List<Cell> cells2 = areaCells.get(id2);

            for (int j=0; j<cells1.size(); ++j)
            {
                Cell cell1 = cells1.get(j);
                if (!cell1.getNeighbours().contains(id2)) continue;
                List<Line2D> outlines1 = cell1.getOutlines();

                for (int k=0; k<cells2.size(); ++k)
                {
                    Cell cell2 = cells2.get(k);
                    if (!cell2.getNeighbours().contains(id1)) continue;
                    List<Line2D> outlines2 = cell2.getOutlines();

                    for (int l=0; l<outlines1.size(); ++l)
                    {
                        Line2D outline1 = outlines1.get(l);
                        for (int m=0; m<outlines2.size(); ++m)
                        {
                            Line2D outline2 = outlines2.get(m);

                            Line2D overlapping =
                                GeomUtil.computeOverlapping(outline1, outline2);
                            if (overlapping == null) continue;

                            Point2D median = GeomUtil.makeMedian(overlapping);
                            cell1.getNeighbourJunctions().put(new Pair<>(id2, k), median);
                            cell2.getNeighbourJunctions().put(new Pair<>(id1, j), median);
                        }
                    }
                }
            }
        }

        Map<EntityID, List<Cell>> retval = new HashMap<>();
        for (EntityID id : path)
        {
            retval.put(id, areaCells.get(id));
        }

        return retval;
    }
}
