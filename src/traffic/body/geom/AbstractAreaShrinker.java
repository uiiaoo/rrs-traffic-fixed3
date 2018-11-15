package traffic.body.geom;

import java.awt.geom.Area;

public abstract class AbstractAreaShrinker implements AreaShrinker
{
    @Override
    public static Area run(
        rescuecore2.standard.entities.Area area,
        AreaOutlinesMap outlinesMap)
    {
        final Area base = new Area(area.getShape());

        outlinesMap.getShrinked(area.getID())
            .stream()
            .forEach();
    }
}
