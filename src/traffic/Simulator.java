package traffic;

import rescuecore2.GUIComponent;
import rescuecore2.worldmodel.*;
import rescuecore2.standard.components.*;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.messages.*;
import rescuecore2.messages.Command;
import rescuecore2.messages.control.*;
import rescuecore2.misc.geometry.*;

import traffic.body.*;
import traffic.view.WorldView;
import traffic.util.Environment;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import java.util.*;

public class Simulator extends StandardSimulator implements GUIComponent
{
    private AgentActionManager agentActionManager;
    private PassableShapeManager shapeManager;
    private CellManager cellManager;
    private JComponent view;

    @Override
    protected void postConnect()
    {
        Environment.init(this.config);
        this.shapeManager = new PassableShapeManager(this.model);
        this.cellManager = new CellManager(this.shapeManager);
        this.agentActionManager = new AgentActionManager(this.cellManager, this.model);

        this.view = new JScrollPane(
            new WorldView(
                this.shapeManager,
                this.cellManager,
                this.agentActionManager,
                this.model));
    }

    private int count = 0;

    @Override
    protected void processCommands(
        KSCommands ksCommands,
        ChangeSet changes)
    {
        this.shapeManager.update(changes);
        this.cellManager.update(changes, this.model);
        this.agentActionManager.update(changes, this.model);
        if (++count == 5)
        {
            this.shapeManager.initTest(this.model);
            //this.cellManager.initTest(this.model);
        }

        for (Command command : ksCommands.getCommands())
        {
            if (command instanceof AKMove)
            {
                this.handleMove((AKMove)command);
            }
            else
            if (command instanceof AKLoad)
            {
                this.handleLoad((AKLoad)command);
            }
            else
            if (command instanceof AKUnload)
            {
                this.handleUnload((AKUnload)command);
            }
            else
            {
                this.handleOtherCommand(command);
            }
        }

        ChangeSet result = this.agentActionManager.makeChangeSet(this.model);
        changes.merge(result);
    }

    @Override
    public JComponent getGUIComponent()
    {
        return this.view;
    }

    @Override
    public String getGUIComponentName()
    {
        return this.view.getName();
    }

    private void handleMove(AKMove akMove)
    {
        EntityID agent = akMove.getAgentID();
        List<EntityID> path = new ArrayList<>(akMove.getPath());

        int x = akMove.getDestinationX();
        int y = akMove.getDestinationY();

        Point2D dest = new Point2D(x, y);
        if (x < 0 || y < 0) dest = null;

        this.agentActionManager.moveAgent(agent, path, dest, this.model);
    }

    private void handleLoad(AKLoad akLoad)
    {
        EntityID agent = akLoad.getAgentID();
        EntityID human = akLoad.getTarget();

        this.agentActionManager.loadHumanToAgent(agent, human);
    }

    private void handleUnload(AKUnload akUnload)
    {
        EntityID agent = akUnload.getAgentID();

        this.agentActionManager.unloadHumanFromAgent(agent);
    }

    private void handleOtherCommand(Command command)
    {
        if (command instanceof AKSay)
            return;

        if (command instanceof AKSpeak)
            return;

        if (command instanceof AKSubscribe)
            return;

        if (command instanceof AKTell)
            return;

        EntityID agent = command.getAgentID();
        this.agentActionManager.preventAgentAction(agent);
    }
}
