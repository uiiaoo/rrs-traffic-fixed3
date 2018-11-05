package traffic.body.path;

import traffic.body.geom.*;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.*;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.*;

import java.util.*;
import java.util.stream.*;

public class PathPlanning
{
    public static List<PathElement> run(
        Agent agent,
        List<EntityID> pathIDs,
        Map<EntityID, List<Cell>> areaCells,
        Point2D dest,
        StandardWorldModel model)
    {
        EntityID position = agent.unwrap().getPosition();
        List<Cell> cells = areaCells.get(position);
        int s = seekStartingNode(agent.getXY(), cells);
        if (s < 0) return new LinkedList<>();

        EntityID target = pathIDs.get(pathIDs.size()-1);
        int g = computeClosest(dest, areaCells.get(target));
        for (int i=pathIDs.size()-2; i>=0 && g<0; --i)
        {
            Area area = (Area)model.getEntity(pathIDs.get(i+1));
            dest = new Point2D((double)area.getX(), (double)area.getY());
            g = computeClosest(dest, areaCells.get(pathIDs.get(i)));
        }
        if (g < 0) return new LinkedList<>();

        List<Pair<EntityID, Integer>> open = new LinkedList<>();
        List<Pair<EntityID, Integer>> close = new LinkedList<>();
        Map<Pair<EntityID, Integer>, Node> nodeMap = new HashMap<>();

        open.add(new Pair<>(position, s));
        nodeMap.put(
            new Pair<>(position, s),
            new Node(position, s, null, 0.0, computeH(cells.get(s).getCentroid(), dest),
                     computeIDHistory(null, new Pair<>(position, s))));

        while (!open.isEmpty())
        {
            Node n = null;
            for (Pair<EntityID, Integer> pair : open)
            {
                Node node = nodeMap.get(pair);
                if (n == null) n = node;
                else if (node.estimate() < n.estimate()) n = node;
            }

            if (n.estimate() == Double.POSITIVE_INFINITY) break;

            if (n.getCell().equals(new Pair<>(target, g)))
            {
                return toPathElement(agent.getXY(), dest, n, areaCells);
            }
            open.remove(n.getCell());
            close.add(n.getCell());

            List<Pair<EntityID, Integer>> neighbours = extractNeighbours(n, areaCells);
            for (Pair<EntityID, Integer> neighbour : neighbours)
            {
                EntityID id = neighbour.first();
                int num = neighbour.second();
                List<EntityID> idHistory = computeIDHistory(n, neighbour);
                double cost = computeCost(n, neighbour, areaCells, pathIDs, idHistory);
                Node m = new Node(id, num, n, cost,
                        computeH(toCell(neighbour, areaCells).getCentroid(), dest), idHistory);

                if (!open.contains(neighbour) && !close.contains(neighbour))
                {
                    open.add(neighbour);
                    nodeMap.put(neighbour, m);
                }
                else
                if (open.contains(neighbour) && m.estimate() < nodeMap.get(neighbour).estimate())
                {
                    nodeMap.put(neighbour, m);
                }
                else
                if (close.contains(neighbour) && m.estimate() < nodeMap.get(neighbour).estimate())
                {
                    nodeMap.put(neighbour, m);
                    close.remove(neighbour);
                    open.add(neighbour);
                }
            }
        }

        int closestIdx = -1;
        for (Node node : nodeMap.values())
        {
            if (node.estimate() == Double.POSITIVE_INFINITY) continue;
            int i = pathIDs.lastIndexOf(node.getCell().first());
            if (i > closestIdx) closestIdx = i;
        }

        EntityID closestID = pathIDs.get(closestIdx);
        if (!closestID.equals(pathIDs.get(pathIDs.size()-1)))
        {
            Area area = (Area)model.getEntity(pathIDs.get(closestIdx+1));
            dest = new Point2D((double)area.getX(), (double)area.getY());
        }

        Point2D fdest = dest;
        Node closest = nodeMap.values()
            .stream()
            .filter(n -> n.getCell().first().equals(closestID))
            .min((n1, n2) -> {
                Cell cell1 = toCell(n1.getCell(), areaCells);
                Cell cell2 = toCell(n2.getCell(), areaCells);
                Double d1 = GeometryTools2D.getDistance(cell1.getCentroid(), fdest);
                Double d2 = GeometryTools2D.getDistance(cell2.getCentroid(), fdest);
                return Double.compare(d1, d2);
            })
            .orElse(null);

        if (closest == null) return new LinkedList<>();

        return toPathElement(agent.getXY(), dest, closest, areaCells);
    }

    private static int seekStartingNode(Point2D point, List<Cell> cells)
    {
        int candidate = computeClosest(point, cells);
        if (candidate == -1) return -1;
        Cell cell = cells.get(candidate);

        return cell.getShape().contains(point.getX(), point.getY())
            ?   candidate : -1;
    }

    private static int computeClosest(Point2D point, List<Cell> cells)
    {
        return IntStream.range(0, cells.size())
            .boxed()
            .min((i1, i2) -> {
                Point2D c1 = cells.get(i1).getCentroid();
                Point2D c2 = cells.get(i2).getCentroid();
                double d1 = GeometryTools2D.getDistance(point, c1);
                double d2 = GeometryTools2D.getDistance(point, c2);
                return Double.compare(d1, d2);
            })
            .orElse(-1);
    }

    private static double computeH(Point2D point1, Point2D point2)
    {
        return GeometryTools2D.getDistance(point1, point2);
    }

    private static List<Pair<EntityID, Integer>> extractNeighbours(
        Node node,
        Map<EntityID, List<Cell>> areaCells)
    {
        EntityID id = node.getCell().first();
        int num = node.getCell().second();

        Cell cell = areaCells.get(id).get(num);
        List<Pair<EntityID, Integer>> retval = new LinkedList<>();

        cell.getJunctions().keySet()
            .stream()
            .map(i -> new Pair<>(id, i))
            .forEach(n -> retval.add(n));

        cell.getNeighbourJunctions().keySet()
            .stream()
            .forEach(n -> retval.add(n));

        return retval;
    }

    private static Cell toCell(
        Pair<EntityID, Integer> pair,
        Map<EntityID, List<Cell>> areaCells)
    {
        return areaCells.get(pair.first()).get(pair.second());
    }

    private static double computeCost(
        Node node,
        Pair<EntityID, Integer> next,
        Map<EntityID, List<Cell>> areaCells,
        List<EntityID> path,
        List<EntityID> idHistory)
    {
        Cell cell1 = toCell(node.getCell(), areaCells);
        Cell cell2 = toCell(next, areaCells);

        for (int i=0; i<Math.min(path.size(), idHistory.size()); ++i)
        {
            EntityID id1 = path.get(i);
            EntityID id2 = idHistory.get(i);
            if (!id1.equals(id2)) return Double.POSITIVE_INFINITY;
        }

        double dist = GeometryTools2D.getDistance(
            cell1.getCentroid(),
            cell2.getCentroid());

        return node.getCost() + dist;
    }

    private static List<EntityID> computeIDHistory(
        Node node,
        Pair<EntityID, Integer> next)
    {
        if (node == null)
        {
            List<EntityID> retval = new LinkedList<>();
            retval.add(next.first());
            return retval;
        }

        List<EntityID> cloned = new LinkedList<>(node.getIDHistory());
        if (cloned.isEmpty() || !cloned.get(cloned.size()-1).equals(next.first()))
            cloned.add(next.first());

        return cloned;
    }

    private static List<PathElement> toPathElement(
        Point2D from,
        Point2D dest,
        Node last,
        Map<EntityID, List<Cell>> areaCells)
    {
        List<PathElement> retval = new LinkedList<>();

        List<Pair<EntityID, Integer>> pathPair = new LinkedList<>();
        while (last != null)
        {
            pathPair.add(last.getCell());
            last = last.getParent();
        }
        Collections.reverse(pathPair);

        for (int i=0; i<pathPair.size(); ++i)
        {
            if (i == 0 && pathPair.size() == 1)
            {
                Pair<EntityID, Integer> pair = pathPair.get(i  );
                PathElement elem = new PathElement(pair.first(), new Line2D(from, dest));
                retval.add(elem);
                break;
            }

            if (i == 0 && pathPair.size() >= 2)
            {
                Pair<EntityID, Integer> pair = pathPair.get(i  );
                Pair<EntityID, Integer> next = pathPair.get(i+1);
                Cell cell = toCell(pair, areaCells);

                Point2D junction = pair.first().equals(next.first())
                    ?   cell.getJunctions().get(next.second())
                    :   cell.getNeighbourJunctions().get(next);

                PathElement elem = new PathElement(
                    pair.first(),
                    new Line2D(from, junction));

                retval.add(elem);
                continue;
            }
            else
            if (i == pathPair.size()-1)
            {
                Pair<EntityID, Integer> prev = pathPair.get(i-1);
                Pair<EntityID, Integer> pair = pathPair.get(i  );
                Cell cell = toCell(pair, areaCells);

                Point2D junction = pair.first().equals(prev.first())
                    ?   cell.getJunctions().get(prev.second())
                    :   cell.getNeighbourJunctions().get(prev);

                PathElement elem = new PathElement(
                    pair.first(),
                    new Line2D(junction, dest));

                retval.add(elem);
                continue;
            }

            Pair<EntityID, Integer> prev = pathPair.get(i-1);
            Pair<EntityID, Integer> pair = pathPair.get(i  );
            Pair<EntityID, Integer> next = pathPair.get(i+1);
            Cell cell = toCell(pair, areaCells);

            Point2D junction1 = pair.first().equals(prev.first())
                ?   cell.getJunctions().get(prev.second())
                :   cell.getNeighbourJunctions().get(prev);

            Point2D junction2 = pair.first().equals(next.first())
                ?   cell.getJunctions().get(next.second())
                :   cell.getNeighbourJunctions().get(next);

            PathElement elem = new PathElement(
                pair.first(),
                new Line2D(junction1, junction2));

            retval.add(elem);
        }

        return retval;
    }
}
