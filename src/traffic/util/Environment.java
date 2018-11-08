package traffic.util;

import rescuecore2.standard.entities.*;
import rescuecore2.config.Config;

public class Environment
{
    public static double AGENT_RADIUS = 500.0;

    public static final double AGENT_VELOCITY_MEAN = 0.7;
    public static final double AGENT_VELOCITY_SD = 0.1;
    public static final double CIVILIAN_VELOCITY_MEAN = 0.2;
    public static final double CIVILIAN_VELOCITY_SD = 0.002;

    public static final double WALL_DISTANCE_CUTOFF = 2000.0;

    public static final double AGENT_DISTANCE_CUTOFF = 10000.0;
    public static final double AGENT_FORCE_COEF_A = 0.0001;
    public static final double AGENT_FORCE_COEF_B = 0.001;
    public static final double AGENT_FORCE_COEF_K = 0.00001;
    public static final double AGENT_FORCE_LIMIT = 0.0001;

    public static final double WALL_FORCE_COEF_B = 0.7;

    public static final double NUDGE_MAGNITUDE = 0.001;

    public static void init(Config config)
    {
    }

    public static double getColocatedAgentNudge()
    {
        return 0.0;
    }
}
