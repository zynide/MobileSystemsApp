package edu.illinois.mitra.demo.projectapp;




import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;

import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.motion.RRTNode;
import edu.illinois.mitra.starl.objects.*;
import edu.illinois.mitra.starlSim.draw.Drawer;

public class ProjectDrawer extends Drawer {

    private Stroke stroke = new BasicStroke(8);
    private Color selectColor = new Color(0,0,255,100);
    private int numSetsWaypoints = 4;

    @Override
    public void draw(LogicThread lt, Graphics2D g) {
        ProjectApp app = (ProjectApp) lt;

        //g.setColor(Color.RED);
        for(int i = 0; i < numSetsWaypoints; i++ ) {
            switch(i) {
                case 0:
                    g.setColor(Color.RED);
                    break;
                case 1:
                    g.setColor(Color.BLUE);
                    break;
                case 2:
                    g.setColor(Color.GREEN);
                    break;
                case 3:
                    g.setColor(Color.YELLOW);
                    break;
                default:
                    g.setColor(Color.RED);
                    break;
            }
            for (ItemPosition dest : app.destinations.get(i).values()) {
                g.fillRect(dest.getX() - 13, dest.getY() - 13, 26, 26);
            }
        }

		/*
		//traverse the tree to get the full picture of the tree
		//maybe add child is the easiest way to draw the whole picture
		g.setColor(Color.cyan);
		KDTree<RRTNode> kd = app.kd;
		double [] temp = {0,0};
		RRTNode curNode0 = null;
		try {
			curNode0 = kd.nearest(temp);
		} catch (KeySizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while(curNode0 != null){
			g.drawRect(curNode0.position.x, curNode0.position.y, 30, 30);
			if(curNode0.parent != null)
			g.drawLine(curNode0.position.x, curNode0.position.y, curNode0.parent.position.x, curNode0.parent.position.y);
			curNode0 = curNode0.parent;
		}
		*/
        //draw kdTree final path stack for debugging
        g.setColor(Color.orange);
        RRTNode curNode = app.kdTree;
        while(curNode != null){
            g.drawRect(curNode.position.x, curNode.position.y, 30, 30);
            if(curNode.parent != null)
                g.drawLine(curNode.position.x, curNode.position.y, curNode.parent.position.x, curNode.parent.position.y);
            curNode = curNode.parent;
        }

        g.setColor(Color.GRAY);
        ObstacleList list = app.obEnvironment;
        for(int i = 0; i < list.ObList.size(); i++)
        {
            Obstacles currobs = list.ObList.get(i);
            if(currobs.hidden)
                g.setColor(Color.LIGHT_GRAY);
            else
                g.setColor(Color.GRAY);

            Point nextpoint = currobs.obstacle.firstElement();
            Point curpoint = currobs.obstacle.firstElement();
            int[] xs = new int[currobs.obstacle.size()];
            int[] ys = new int[currobs.obstacle.size()]; ;

            for(int j = 0; j < currobs.obstacle.size() -1 ; j++){
                curpoint = currobs.obstacle.get(j);
                nextpoint = currobs.obstacle.get(j+1);
                g.drawLine(curpoint.x, curpoint.y, nextpoint.x, nextpoint.y);
                xs[j] = curpoint.x;
                ys[j] = curpoint.y;
            }
            xs[currobs.obstacle.size()-1] = nextpoint.x;
            ys[currobs.obstacle.size()-1] = nextpoint.y;

            g.drawLine(nextpoint.x, nextpoint.y, currobs.obstacle.firstElement().x, currobs.obstacle.firstElement().y);
            g.fillPolygon(xs,ys,currobs.obstacle.size());
        }

        g.setColor(selectColor);
        g.setStroke(stroke);
        if(app.currentDestination != null)
            g.drawOval(app.currentDestination.getX() - 20, app.currentDestination.getY() - 20, 40, 40);
    }

}
