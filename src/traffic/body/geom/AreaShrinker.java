package traffic.body.geom;

import java.awt.geom.Area;

public interface AreaShrinker
{
    public static Area run(
        rescuecore2.standard.entities.Area area,
        AreaOutlinesMap outlinesMap);
}
