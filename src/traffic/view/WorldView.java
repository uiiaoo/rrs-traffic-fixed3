package traffic.view;

import rescuecore2.standard.entities.*;

import traffic.body.*;

import javax.swing.JComponent;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;

public class WorldView extends JComponent
    implements MouseWheelListener, MouseMotionListener
{
    private static final String NAME = "Fixed Traffic Simulator 3";
    private static final double MOUSE_WHEEL_POWER = 0.15;

    private PassableShapeManager shapeManager;
    private CellManager cellManager;
    private AgentActionManager agentActionManager;
    private StandardWorldModel model;

    private AffineTransform moveTransform;
    private Point mousePoint;
    private double subscale = 1.0;

    public WorldView(
        PassableShapeManager shapeManager,
        CellManager cellManager,
        AgentActionManager agentActionManager,
        StandardWorldModel model)
    {
        this.shapeManager = shapeManager;
        this.cellManager = cellManager;
        this.agentActionManager = agentActionManager;
        this.model = model;

        this.moveTransform = new AffineTransform();
        this.addMouseWheelListener(this);
        this.addMouseMotionListener(this);
    }

    public String getName()
    {
        return NAME;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        this.shapeManager.paint((Graphics2D)g, this.makeTransform());
        //this.cellManager.paint((Graphics2D)g, this.makeTransform());
        this.agentActionManager.paint((Graphics2D)g, this.makeTransform());
    }

    @Override
    synchronized public void mouseDragged(MouseEvent evt)
    {
        Point before = this.mousePoint;
        this.mousePoint = evt.getPoint();

        if (before == null) return;

        double dx = this.mousePoint.getX() - before.getX();
        double dy = this.mousePoint.getY() - before.getY();
        this.move(dx, dy);

        this.repaint();
    }

    @Override
    synchronized public void mouseMoved(MouseEvent evt)
    {
        this.mousePoint = null;

        this.repaint();
    }

    @Override
    synchronized public void mouseWheelMoved(MouseWheelEvent evt)
    {
        double d = evt.getWheelRotation() * MOUSE_WHEEL_POWER;
        this.scale(d);

        this.repaint();
    }

    private void move(double dx, double dy)
    {
        this.moveTransform.translate(dx, dy);
    }

    private void scale(double d)
    {
        this.subscale += d;
    }

    synchronized private AffineTransform makeTransform()
    {
        Rectangle2D worldsize = this.model.getBounds();
        Rectangle size = this.getBounds();

        double sx = size.getMaxX()/worldsize.getMaxX();
        double sy = size.getMaxY()/worldsize.getMaxY();
        double scale = Math.min(sx, sy) * this.subscale;

        AffineTransform transform = new AffineTransform();
        transform.translate(size.getCenterX(), size.getCenterY());
        transform.scale(scale, -scale);
        transform.translate(-worldsize.getCenterX(), -worldsize.getCenterY());

        transform.preConcatenate(this.moveTransform);
        return transform;
    }
}
