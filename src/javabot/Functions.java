package javabot;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Queue;

import javabot.model.*;
import javabot.types.*;
import javabot.types.OrderType.OrderTypeTypes;
import javabot.types.UnitType.UnitTypes;
import javabot.util.BWColor;

public class Functions {
	
	JNIBWAPI bwapi;
	
	public Functions(JNIBWAPI _bwapi) {
		bwapi=_bwapi;
	}

	public Unit nearestMineral(Unit unit) {
		Unit closest=null;
		double closestDist = 99999999;
		for (Unit neu : bwapi.getNeutralUnits()) {
			if (neu.getTypeID() == UnitTypes.Resource_Mineral_Field.ordinal()) {
				double distance = Math.sqrt(Math.pow(neu.getX() - unit.getX(), 2) + Math.pow(neu.getY() - unit.getY(), 2));
				if ((closest == null) || (distance < closestDist)) {
					closestDist = distance;
					closest = neu;
				}
			}
		}
		return closest;
	}
	
	public boolean inExtendedRange(Unit what,Unit who) {
		if (distance(what,who)<=(bwapi.getWeaponType(bwapi.getUnitType(who.getTypeID()).getGroundWeaponID()).getMaxRange()*32+20)) return true;
		return false;
	}
	
	public boolean inRange(Unit what,Unit who) {
		if (distance(what,who)<=bwapi.getWeaponType(bwapi.getUnitType(who.getTypeID()).getGroundWeaponID()).getMaxRange()*32) return true;
		return false;
	}
	
	public double distance(Unit x, Unit y) {
		return Math.sqrt(Math.pow(x.getX() - y.getX(), 2) + Math.pow(x.getY() - y.getY(), 2));
	}
	
}
