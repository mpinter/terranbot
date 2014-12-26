package javabot;

import java.awt.Point;
import java.util.ArrayList;

import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;

public class ExtendedUnit {

	JavaBot bot;
	Unit unit;
	
	//id in array, which may or may not be used .. and may or may not be kept accurate for every unit
	public int id;
	
	//SCV stuff
	public Unit gathering;
	public Unit buildRepair;
	public int gonnaBuild; //typeID
	public int constructionPlans; //to prevent freezing when scv's destroyd while isConstructing is true but the building isn't built
	public Point whereToBuild;
	public int commandFrame;
	public ArrayList<Point> scoutsTrail;
	int trailIterator;
	boolean runAround;
	
	//list of SCVs on their way to build/repair
	public ArrayList<ExtendedUnit> assignedWorkers;
	
	//CC stuff
	public Unit closest_mineral,closest_refinery;
	public int required_miners;
	public int regionID; //also for refinery
	public int firstAddonFrame;
	public ExtendedRegion extendedRegion;
	boolean floating,unableToAddon;
	
	//addons
	boolean triedBuildingAddon;
	
	//enemy in range
	public Unit enemy;
	public Unit siegeEnemy;
	
	//unit in range of an enemy 
	public Unit threat;
	
	//boolean for now
	public boolean isUnderAttack;
	int underAttackFrame;
	
	//micro stuff
	public boolean was_safe,is_safe; //to determine when unit ran into firing range
	public boolean virgin; //if not yet attacked by anything , allowing scouts to decimate workerlines of bots that are too lazy to attack with workers
	public boolean panic; //tells unit to run away from everything
	public int previous_hitpoints;
	
	public ExtendedUnit(Unit _u, JavaBot _bot) {
		bot=_bot;
		unit=_u;
		previous_hitpoints=0;
		buildRepair=null;
		gonnaBuild=-1;
		whereToBuild=null; //TODO clear this and buildRepair when finished
		gathering=null;
		was_safe=true;
		is_safe=true;
		enemy=null;
		threat=null;
		virgin=true;
		panic=false;
		required_miners=0;
		id=-1;
		assignedWorkers=new ArrayList<ExtendedUnit>();
		floating=false;
		unableToAddon=false;
		firstAddonFrame=-1;
		commandFrame=0;
		extendedRegion=null;
		triedBuildingAddon=false;
		isUnderAttack=false;
		underAttackFrame=-1;
		constructionPlans=-1;
	}
	
	public boolean initCC(int _id) {
		id=_id;
		closest_mineral=bot.nearestMineral(unit);
		if (bot.distance(closest_mineral, unit)>320){
			//TODO float away ..or maybe just forget it
			closest_mineral=null;
			required_miners=0;
			for (int i=0;i<bot.miners.get(id).size();i++) {
				bot.miners.get(id).get(i).gathering=null;
			}
			return false;
		}
		int groupId=closest_mineral.getResourceGroup();
		required_miners=0;
		for (Unit neu : bot.getNeutralUnits()) {
			if (neu.getResourceGroup()==groupId) required_miners++;
		}
		required_miners=(required_miners/4)+2*required_miners;
		bot.setRallyPoint(unit.getID(), closest_mineral.getID());
		bot.enoughSCV=false; //we've just expanded, we probably need more
		initCCRegion();
		bot.usedBaseLocations.add(extendedRegion.region.getID());
		return true;
	}
	
	public void initCCRegion() {
		//get ExtendedRegion
		double distance=999999.9;
		for (int i=0;i<bot.regions.size();i++) {
			if (bot.distance(unit, new Point(bot.regions.get(i).region.getCenterX(),bot.regions.get(i).region.getCenterY()))<distance) {
				extendedRegion=bot.regions.get(i);
				distance=bot.distance(unit, new Point(bot.regions.get(i).region.getCenterX(),bot.regions.get(i).region.getCenterY()));
			}
		}
	}
	
	//apparently , unit becomes idle for a tiny moment when given new order... so that's pretty useless to chekc for
	public boolean mine() {
		constructionPlans=-1;
		if ((buildRepair!=null)&&(buildRepair.isCompleted()&&(buildRepair.getHitPoints()==bot.getMaxHitPoints(buildRepair.getTypeID())))) {
			buildRepair=null;
		}
		if (unit.isStartingAttack()) return true;
		if (unit.isAttackFrame()) return true;
		if (unit.isAttacking()) return true;
		if (unit.isConstructing()) return true;
		if (unit.isRepairing()) return true;
		if (isUnderAttack) {
			bot.attack(unit.getID(), unit.getX(),unit.getY());
			//System.out.println("SCV ATTACKING!");
		}
		if (gonnaBuild!=-1) {
			//transfer to builders
			////System.out.println("TRANSFERING TO BUILDERS");
			gathering=null;
			return false;
		}
		if (buildRepair!=null) {
			bot.rightClick(unit.getID(), buildRepair.getID());
			return true;
		}
		if ((gathering==null)||(gathering.getResources()<1)) {
			gathering=null;
			return false;
		}
		/*if (unit.isFollowing()) {
			//System.out.println("FOLLOW FORBIDDEN!");
			bot.gather(unit.getID(), gathering.getID());
			commandFrame=bot.getFrameCount();
		}*/
		if ((unit.isGatheringGas())&&(gathering.getTypeID()==UnitTypes.Resource_Mineral_Field.ordinal())&&((commandFrame==0)||(bot.getFrameCount()-commandFrame>50))) {
			commandFrame=bot.getFrameCount();
			bot.gather(unit.getID(), gathering.getID());
		}
		if ((!unit.isGatheringGas())&&(!unit.isGatheringMinerals())&&((commandFrame==0)||(bot.getFrameCount()-commandFrame>50))) {
			////System.out.println("BACK TO WORK!!");
			commandFrame=bot.getFrameCount();
			bot.gather(unit.getID(), gathering.getID());
		}

		return true;
	}
	
	public boolean build() {
		if (constructionPlans==-1) constructionPlans=gonnaBuild;
		//bot.print("builder " + gonnaBuild);
		if (unit.isConstructing()) { //finally switched to correct state
			if (bot.weAreBuilding(gonnaBuild)) gonnaBuild=-1;
			////System.out.println("CONSTRUCTING");
			return true;
		}
		////System.out.println("SHOULD BE CONSTRUCTING");
		if ((gonnaBuild==-1)&&(!unit.isConstructing())) {
			constructionPlans=-1;
			return false;
		}
		if (!unit.isConstructing()) { //we probably don't have vision of the place
			bot.move(unit.getID(), whereToBuild.x*32, whereToBuild.y*32);
		}
		if (bot.getFrameCount()-commandFrame>1000) {
			//System.out.println("...........BUILD FAILED");
			bot.queuedBuildings.remove(gonnaBuild);
			gonnaBuild=-1;
			constructionPlans=-1;
			return false;
		}
		bot.build(unit.getID(), whereToBuild.x, whereToBuild.y, gonnaBuild);
		return true;
	}
	
	public void initScout(ArrayList<Point> _list,boolean _runAround) {
		scoutsTrail=_list;
		trailIterator=0;
		runAround=_runAround;
	}
	
	public void scout() {
		constructionPlans=-1;
		//first look for enemy base
		if ((!runAround)&&(bot.nearEnemyBase(new Point(unit.getX(),unit.getY())))) {
			initCCRegion();
			initScout(extendedRegion.innerCoordinates,true);
			return;
		}
		if (bot.distance(unit, scoutsTrail.get(trailIterator))<200.0) {
			//add support for spider mines ... lol I really thought there'll be time for this :D
			trailIterator=(trailIterator+1)%scoutsTrail.size();
		} else {
			bot.move(unit.getID(), scoutsTrail.get(trailIterator).x, scoutsTrail.get(trailIterator).y);
		}
	}
	
	public void attack() {
		if (enemy==null) return;
		if (!bot.inRange(enemy,unit)) {
			enemy=null;
			return;
		}
		if (unit.getGroundWeaponCooldown()==0) {
			bot.attack(unit.getID(), enemy.getID());
		}
	}
	
	//runs away if in range
	public void avoid() {
		if (threat==null) return;
		if (!bot.inRange(unit,threat)) {
			threat=null;
			return;
		}
		//TODO compute
	}
	
	//runs away if close to range
	public void evade() {
		if (threat==null) return;
		if (!bot.inExtendedRange(unit,threat)) {
			threat=null;
			if ((gathering!=null)&&(gathering.getTypeID()==UnitTypes.Resource_Mineral_Field.ordinal())) {
				Unit possible_new=bot.nearestMineral(unit);
				if (possible_new.getResourceGroup()==gathering.getResourceGroup()) gathering=possible_new;
			}
			return;
		}
		//TODO compute
	}
	
	//interrupts the repairing of a unit (even when under 100%), to return it to the fight and allow other units to be repaired
	//also viable for cleaning assigned workers whenever needed (i.e. when constructing buildings)
	public void dontBeAPussy() {
		for (int i=0;i<assignedWorkers.size();i++) {
			assignedWorkers.get(i).buildRepair=null;
		}
		assignedWorkers.clear();
	}
	
	//to get rid of multiple scvs constructing one building
	public void clearConstructionSite() {
		int i=0;
		while (i<assignedWorkers.size()) {
			if (!assignedWorkers.get(i).unit.isConstructing()) {
				assignedWorkers.get(i).buildRepair=null;
				assignedWorkers.remove(i);
			} else {
				i++;
			}
		}
		assignedWorkers.clear();
	}
	
}
