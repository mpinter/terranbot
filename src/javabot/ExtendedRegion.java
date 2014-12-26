package javabot;

import java.awt.Point;
import java.util.ArrayList;

import javabot.model.BaseLocation;
import javabot.model.Map;
import javabot.model.Region;

public class ExtendedRegion {
	
	Region region;
	Map map;
	BaseLocation baseLocation;
	public ArrayList<Point> innerCoordinates;
	public ArrayList<Point> coordinates;
	public int polySides;
	public int[] constant,multiple;
	
	public ExtendedRegion(Region _region,Map _map) {
		region=_region;
		map=_map;
		innerCoordinates=new ArrayList<Point>();
		coordinates=new ArrayList<Point>();
		polySides=0;
		for (int i=0;i<region.getCoordinates().length;i+=2) {
			polySides++;
			coordinates.add(new Point(region.getCoordinates()[i],region.getCoordinates()[i+1]));
		}
		constant=new int[coordinates.size()];
		multiple=new int[coordinates.size()];
		//from http://alienryderflex.com/polygon/ ...would not need this if working under C++ where pointInsideRegion() exists
		int i, j=polySides-1;
		for(i=0; i<polySides; i++) {
			if(coordinates.get(j).y==coordinates.get(i).y) {
				constant[i]=coordinates.get(i).x;
				multiple[i]=0; }
			else {
				constant[i]=coordinates.get(i).x-(coordinates.get(i).y*coordinates.get(j).x)/(coordinates.get(j).y-coordinates.get(i).y)+(coordinates.get(i).y*coordinates.get(i).x)/(coordinates.get(j).y-coordinates.get(i).y);
				multiple[i]=(coordinates.get(j).x-coordinates.get(i).x)/(coordinates.get(j).y-coordinates.get(i).y); }
			j=i; 
		}
	}
	
	//init calculates inner coordinates
	public void init() {
		for (int i=0;i<coordinates.size();i++) {
			//System.out.println(i);
			int x,y;
			double norm=abs(region.getCenterX()-coordinates.get(i).x)/80.0; 
			//if ((norm<0.1)&&(norm>-0.1)) continue;*/
			x=coordinates.get(i).x+(int)((region.getCenterX()-coordinates.get(i).x)/norm);
			norm=abs(region.getCenterY()-coordinates.get(i).y)/80.0; 
			//if ((norm<0.1)&&(norm>-0.1)) continue;*/
			y=coordinates.get(i).y+(int)((region.getCenterY()-coordinates.get(i).y)/norm);
			//such I shame that I found out regions are not always convex ... still works reasonably well with a small patch
			if (!map.isWalkable(x/8, y/8)) continue;
			if (pointInside(x,y)) {
				boolean should_add=true;
				if (distance(new Point(region.getCenterX(),region.getCenterY()),new Point(x,y))>500.0) continue;
				for (int j=0;j<innerCoordinates.size();j++)
					if (distance(innerCoordinates.get(j),new Point(x,y))<100.0) {
						should_add=false;
						break;
					}
				for (int j=0;j<region.getChokePoints().size();j++)
					if (distance(new Point(region.getChokePoints().get(j).getCenterX(),region.getChokePoints().get(j).getCenterY()),new Point(x,y))<150.0) {
						should_add=false;
						break;
					}
				if (should_add) innerCoordinates.add(new Point(x,y));
			}
		}
		for (int i=0;i<map.getBaseLocations().size();i++) {
			if (pointInside(map.getBaseLocations().get(i).getX(),map.getBaseLocations().get(i).getY())) {
				baseLocation=map.getBaseLocations().get(i);
			}
		}
	}
	
	//from http://alienryderflex.com/polygon/
	public boolean pointInside(int x, int y) {
		x-=10; //fixing weird bug on maps like python
		y-=10;
		int i,j=polySides-1;
		boolean  oddNodes=false;
		for (i=0; i<polySides; i++) {
			if ((coordinates.get(i).y< y && coordinates.get(j).y>=y) || (coordinates.get(j).y< y && coordinates.get(i).y>=y)) {
				oddNodes^=(y*multiple[i]+constant[i]<x);
				}
			j=i; }
		return oddNodes;
	}
	
	private int abs(int x) {
		if (x<0) return -1*x;
		return x;
	}
	
	private double distance(Point x, Point y) {
		return Math.sqrt(Math.pow(x.getX() - y.getX(), 2) + Math.pow(x.getY() - y.getY(), 2));
	}

}
