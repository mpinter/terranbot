package javabot;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import javabot.model.ChokePoint;
import javabot.model.Region;

public class BFS {
	
	public static ArrayList<Point> bfs(Region enemyBase, Region myBase) {
		HashMap<Integer,State> idSet=new HashMap<Integer,State>();
		State result=null;
		Queue<State> queue=new LinkedList<State>();
		queue.add(new State(enemyBase,null));
		while (!queue.isEmpty()) {
			State state=queue.poll();
			if (idSet.containsKey(state.which.getID())) continue;
			if (state.which.getID()==myBase.getID()) {
				result=state;
				break;
			}
			for (ChokePoint cp : state.which.getChokePoints()) {
				if (cp.getFirstRegionID()==state.which.getID()) {
					queue.add(new State(cp.getSecondRegion(),state));
				} else {
					queue.add(new State(cp.getFirstRegion(),state));
				}
			}
		}
		if (result==null) {
			System.out.println("COULDN'T FIND A WAY .. WHAAAT?!");
			return null;
		} else {
			ArrayList<Point> list=new ArrayList<Point>();
			while (result!=null) {
				if (result.former==null) {
					list.add(new Point(result.which.getCenterX(),result.which.getCenterY()));
					break;
				}
				for (ChokePoint cp : result.which.getChokePoints()) {
					if ((cp.getFirstRegionID()==result.former.which.getID())||(cp.getSecondRegionID()==result.former.which.getID())) {
						list.add(new Point(cp.getCenterX(),cp.getCenterY()));
						break;
					}
				}
				result=result.former;
			}
			return list;
		}
	}
	
	
	
}
