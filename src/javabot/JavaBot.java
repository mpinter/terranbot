package javabot;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;

import javabot.model.*;
import javabot.types.*;
import javabot.types.OrderType.OrderTypeTypes;
import javabot.types.TechType.TechTypes;
import javabot.types.UnitType.UnitTypes;
import javabot.types.UpgradeType.UpgradeTypes;
import javabot.util.BWColor;

public class JavaBot implements BWAPIEventListener {
	
	//CREATE ALL THE LISTS!
	//SCV lists
	ArrayList<ArrayList<ExtendedUnit> > miners;
	ArrayList<ArrayList<ExtendedUnit> > gassers;
	ArrayList<ExtendedUnit> builders;
	ArrayList<ExtendedUnit> repairers;
	ArrayList<ExtendedUnit> loiterers;
	ArrayList<ExtendedUnit> berserkers;
	ExtendedUnit scouter;
	int berserkCount;
	ArrayList<Unit> whatToBerserk;
	
	//Building lists
	ArrayList<ExtendedUnit> buildings;
	ArrayList<ExtendedUnit> unfinished;
	ArrayList<ExtendedUnit> rax;
	ArrayList<ExtendedUnit> factories;
	ArrayList<ExtendedUnit> starports;
	ArrayList<ExtendedUnit> CC;
	ArrayList<ExtendedUnit> satComs;
	ArrayList<ArrayList<ExtendedUnit> > turrets;
	ArrayList<ExtendedUnit> refineries;
	ArrayList<ExtendedUnit> research;
	ArrayList<ExtendedUnit> addons;
	ArrayList<ExtendedUnit> armories;
 	
	//Unit lists
	ArrayList<ExtendedUnit> guerilla_vult;
	ArrayList<ExtendedUnit> rines;
	ArrayList<ExtendedUnit> vult;
	ArrayList<ExtendedUnit> siege;
	ArrayList<ExtendedUnit> goliaths;
	ArrayList<ExtendedUnit> vessels;
	ArrayList<ExtendedUnit> wraiths;
	ArrayList<ExtendedUnit> valkyries;
	ArrayList<ExtendedUnit> bc;
	
	//non-panic build-order (when empty, the bot will not tech, focus on economy, build army only to match the opponents)
	Queue<Unit> buildOrder;
	
	//panic mode variables - when panicking, ignoring build order
	boolean need_turrets, need_goliaths, pump_production, need_vessels, need_valkyries;
	
	//switches for enemy tech
	boolean cloak;
	
	//halting unit production
	boolean enoughSCV;
	
	//various booleans
	boolean gas_stolen,all_in,base_destroyed,gas_destroyed,second_fact,building_refinery;
	
	//micro points
	Point leadTank,leadUnit,lastTank,leadPoint,furthestEnemy;
	boolean attack;
	Point underAttack;
	Point furthestTank;
	ExtendedUnit extendedTank;
	
	//free cash
	int disposable_min,disposable_gas;
	
	//various hashsets
	HashSet<Integer> queuedBuildings;
	HashSet<Integer> allEnemyUnits;
	HashMap<Integer,Point> allEnemyBuildings;
	HashSet<Integer> offense;
	HashSet<Integer> defense;
	HashSet<Integer> tech;
	HashSet<Integer> usedBaseLocations;
	HashSet<Integer> handledRefineries; //because of the weird way that refineries are morphed from extractors
	HashMap<Integer,Point> myIds; //misusing point for air and ground
	HashMap<Integer,Point> enemyIds;
	HashMap<Integer,Integer> techMap;
	
	//Regions
	ArrayList<ExtendedRegion> regions;
	
	//Players
	Player me;
	Player enemy;
	Integer myGround;
	Integer myAir;
	Integer enemyGround;
	Integer enemyAir;
	int homePositionX;
	int homePositionY;
	int enemyRace;
	int lastScout;
	int leadPointID;
	int underAttackFrame;
	int atPoint;
	int attckIter;
	int defPointID;
	Deque<Point> enemyBases;
	ArrayList<ArrayList<Point>> baseTrails;
	private JNIBWAPI bwapi;
	Functions functions;
	
	//DEBUGDEF
	Unit builderDraw=null;
	Point buildTileDraw=null;
	
	public static void main(String[] args) {
		new JavaBot();
	}
	public JavaBot() {
		bwapi = new JNIBWAPI(this);
		bwapi.start();
		functions=new Functions(bwapi);
	} 
	public void connected() {
		bwapi.loadTypeData();
	}
	
	// Method called at the beginning of the game.
	public void gameStarted() {		
		//////System.out.println("Game Started");
		bwapi.enableUserInput();
		bwapi.setGameSpeed(0);
		bwapi.loadMapData(true);
		regions=new ArrayList<ExtendedRegion>();
		for (int i=0;i<bwapi.getMap().getRegions().size();i++) {
			regions.add(new ExtendedRegion(bwapi.getMap().getRegions().get(i),bwapi.getMap()));
			regions.get(regions.size()-1).init();
		}
		me=bwapi.getSelf();
		enemy=bwapi.getEnemies().get(0);
		bwapi.printText("This map is called "+bwapi.getMap().getName());
		bwapi.printText("Enemy race ID: "+String.valueOf(enemy));	// Z=0,T=1,P=2
		//TODO:initialize lists, send mining, fill build order, test map analyzer
		//INITIALIZE ALL THE LISTS!
		//SCV lists
		miners=new ArrayList<ArrayList<ExtendedUnit> >();
		miners.add(new ArrayList<ExtendedUnit>());
		gassers=new ArrayList<ArrayList<ExtendedUnit> >();
		builders=new ArrayList<ExtendedUnit>();
		repairers=new ArrayList<ExtendedUnit>();
		loiterers=new ArrayList<ExtendedUnit>();
		berserkers=new ArrayList<ExtendedUnit>();
		whatToBerserk=new ArrayList<Unit>();
		scouter=null;
		berserkCount=0;
		
		//Building lists
		buildings=new ArrayList<ExtendedUnit>();
		unfinished=new ArrayList<ExtendedUnit>();
		rax=new ArrayList<ExtendedUnit>();
		factories=new ArrayList<ExtendedUnit>();
		starports=new ArrayList<ExtendedUnit>();
		CC=new ArrayList<ExtendedUnit>();
		satComs=new ArrayList<ExtendedUnit>();
		turrets=new ArrayList<ArrayList<ExtendedUnit> >();
		turrets.add(new ArrayList<ExtendedUnit>());
		refineries=new ArrayList<ExtendedUnit>();
		research=new ArrayList<ExtendedUnit>();
		addons=new ArrayList<ExtendedUnit>();
		armories=new ArrayList<ExtendedUnit>();
		
		//Unit lists
		guerilla_vult=new ArrayList<ExtendedUnit>();
		rines=new ArrayList<ExtendedUnit>();
		vult=new ArrayList<ExtendedUnit>();
		siege=new ArrayList<ExtendedUnit>();
		goliaths=new ArrayList<ExtendedUnit>();
		vessels=new ArrayList<ExtendedUnit>();
		wraiths=new ArrayList<ExtendedUnit>();
		valkyries=new ArrayList<ExtendedUnit>();
		bc=new ArrayList<ExtendedUnit>();
		
		//non-panic build-order (when empty, the bot will not tech, focus on economy, build army only to match the opponents)
		buildOrder=new LinkedList<Unit>();
		
		//panic mode variables - when panicking, ignoring build order
		need_turrets=false;
		need_goliaths=false;
		need_valkyries=false;
		need_vessels=false;
		pump_production=false; 
		
		//halting unit production (not)
		enoughSCV=false;
		
		//various booleans
		gas_stolen=false;
		all_in=false; //for defending with SCVs against enemy all-in
		base_destroyed=false; 
		gas_destroyed=false; //to know when to manage confused scv-s that don't know where to mine
		second_fact=false; //for the only time that we build 2 buildings of the same kind at once
		building_refinery=false; //since refineries aren't created, but are morphed into (and no event checks for that), we need some extra control
		
		//various hashsets
		queuedBuildings=new HashSet<Integer>();
		allEnemyUnits=new HashSet<Integer>();
		allEnemyBuildings=new HashMap<Integer,Point>();
		offense=new HashSet<Integer>();
		defense=new HashSet<Integer>();
		tech=new HashSet<Integer>();
		usedBaseLocations=new HashSet<Integer>();
		handledRefineries=new HashSet<Integer>();
		myIds=new HashMap<Integer,Point>();
		enemyIds=new HashMap<Integer,Point>();
		
		//trying to keep myArmy 1.5 times greater than opponents (revealed) army most of the time
		myGround=0;
		myAir=0;
		enemyGround=0;
		enemyAir=0;
		
		//various
		lastScout=0;
		baseTrails=new ArrayList<ArrayList<Point> >();
		atPoint=0;
		attckIter=0;
		defPointID=0;
		
		//get opponent
		enemyRace=bwapi.getEnemies().get(0).getRaceID();
		enemyBases=new ArrayDeque<Point>();
		
		//micro
		leadTank=null;
		lastTank=null;
		leadUnit=null;
		leadPoint=null;
		leadPointID=-1;
		furthestEnemy=null;
		furthestTank=null;
		extendedTank=null;
		
		techMap=new HashMap<Integer,Integer>();
		
		attack=false;
		
		//initialization
		ArrayList<Unit> minerals=bwapi.getNeutralUnits();
		int mineral_iterator=0;
		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.getTypeID() == UnitTypes.Terran_Command_Center.ordinal()) {
				CC.add(new ExtendedUnit(unit,this));
				////////System.out.println(CC.size());
				if (!CC.get(0).initCC(0)) {
					//wtf, this should not happen
					bwapi.printText("WAT");
				}
				//refineries.add(null); //add with every new CC
			}
		}
		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.getTypeID() == UnitTypes.Terran_SCV.ordinal()) {
				myIds.put(unit.getID(),new Point(0,0));
				ExtendedUnit miner=new ExtendedUnit(unit,this);
				bwapi.printText("MINER");
				//iterate through minerals, in the unlikely event that there are less minerals in sight than SCVs, just send the remaining to their nearest
				while ((mineral_iterator<minerals.size())&&((minerals.get(mineral_iterator).getTypeID()!=UnitTypes.Resource_Mineral_Field.ordinal())||(minerals.get(mineral_iterator).getResourceGroup()!=CC.get(0).closest_mineral.getResourceGroup())))
					mineral_iterator++;
				if (mineral_iterator<minerals.size()) miner.gathering=minerals.get(mineral_iterator); else miner.gathering=nearestMineral(unit); //when all else fails
				miners.get(0).add(miner);
			}
		}
		underAttack=null;
		underAttackFrame=0;
	}
	
	public void act() {
		//TODO check if enemy is killing our base / scvs
		/*
		underAttack=null;
		for (int j=0;j<miners.size();j++) {
			for (int k=0;k<miners.get(j).size();k++) {
				/*if (miners.get(j).get(k).previous_hitpoints<miners.get(j).get(k).unit.getHitPoints()) {
					bwapi.attack(miners.get(j).get(k).unit.getID(),miners.get(j).get(k).unit.getX(),miners.get(j).get(k).unit.getY());
				}
			}
		}
		for (ExtendedUnit building : buildings) {
			if (building.previous_hitpoints>building.unit.getHitPoints()) {
				underAttack=new Point(building.unit.getX(),building.unit.getY());
				if (building.isUnderAttack) {
					boolean check=false;
					for (Unit eu : bwapi.getEnemyUnits()) {
						if (!(bwapi.getUnitType(eu.getTypeID()).isBuilding()&& ( (eu.getTypeID()!=UnitTypes.Terran_Missile_Turret.ordinal()) || (eu.getTypeID()!=UnitTypes.Terran_Bunker.ordinal())||(eu.getTypeID()!=UnitTypes.Zerg_Spore_Colony.ordinal())||(eu.getTypeID()!=UnitTypes.Zerg_Sunken_Colony.ordinal())||(eu.getTypeID()!=UnitTypes.Protoss_Photon_Cannon.ordinal()) ) )) {
							if (inRange(building.unit,eu)) check=true;
						}
					}
					if (!check) {
						//////System.out.println("NEED SCAN");
						for (int j=0;j<satComs.size();j++) {
							if (satComs.get(j).unit.getEnergy()>49) {
								bwapi.useTech(satComs.get(j).unit.getID(), TechTypes.Scanner_Sweep.ordinal(), underAttack.x, underAttack.y);
								check=true;
								break;
							}
						}
						if (check) break;
					}
				}
				//////System.out.println("Under attack!");
				break;
			}
		}
		if (bwapi.getFrameCount()-underAttackFrame>1000) underAttack=null;*/
		removeDead();
		assessEnemy();
		if ((enemyGround*1.3>myGround)) attack=false; else attack=true;
		//if (enemyAir>myAir) need_goliaths=true;
		int i=0; //our iterator for all of the whiles
		if (building_refinery) {
			////////System.out.println("BUILDING REFINERY");
			for (Unit unit : bwapi.getMyUnits()) {
				if ((unit.getTypeID()==UnitTypes.Terran_Refinery.ordinal())&&(!handledRefineries.contains(unit.getID()))) {
					unfinished.add(new ExtendedUnit(unit,this));
					//////System.out.println("INSERTED REFINERY");
					building_refinery=false;
					queuedBuildings.remove(UnitTypes.Terran_Refinery.ordinal());
					handledRefineries.add(unit.getID());
				}
			}
		}
		//handle unfinished (and just finished) buildings and units		
		i=0; //in case code is added between this and i initialization
		while (i<unfinished.size()) {
			////////System.out.println("UNFINISHED");
			if (unfinished.get(i).unit.isCompleted()) {
				//not an appropriate name for a function when used like this, but it does the job.. it also makes all units hardcore from the beginning
				unfinished.get(i).dontBeAPussy();
				Integer typeID=unfinished.get(i).unit.getTypeID();
				if (bwapi.getUnitType(typeID).isAddon()) {
					if (typeID==UnitTypes.Terran_Comsat_Station.ordinal()) {
						satComs.add(unfinished.get(i));
					} else {
						research.add(unfinished.get(i));
						techMap.put(unfinished.get(i).unit.getID(), unfinished.get(i).unit.getTypeID());
					}
				} else if (bwapi.getUnitType(typeID).isBuilding()) {
					buildings.add(unfinished.get(i));
					if (typeID==UnitTypes.Terran_Command_Center.ordinal()) {
						CC.add(unfinished.get(i));
						miners.add(new ArrayList<ExtendedUnit>());
						CC.get(CC.size()-1).initCC(CC.size()-1);
						tech.remove(UnitTypes.Terran_Refinery.ordinal()); //need another refinery
						this.usedBaseLocations.add(nearestBaseLocationID(new Point(unfinished.get(i).unit.getX(),unfinished.get(i).unit.getY())));
						//findDefChokePoint();
					} else if (typeID==UnitTypes.Terran_Barracks.ordinal()) {
						rax.add(unfinished.get(i));
						techMap.put(unfinished.get(i).unit.getID(), unfinished.get(i).unit.getTypeID());
					} else if (typeID==UnitTypes.Terran_Factory.ordinal()) {
						factories.add(unfinished.get(i));
						techMap.put(unfinished.get(i).unit.getID(), unfinished.get(i).unit.getTypeID());
					} else if (typeID==UnitTypes.Terran_Starport.ordinal()) {
						starports.add(unfinished.get(i));
						techMap.put(unfinished.get(i).unit.getID(), unfinished.get(i).unit.getTypeID());
					} else if (typeID==UnitTypes.Terran_Missile_Turret.ordinal()) {
						//turrets are added on the beginning of construction ... [a week later] o'rly ? I hope I get to the point where I build turrets
					} else if (typeID==UnitTypes.Terran_Refinery.ordinal()) {
						refineries.add(unfinished.get(i));
						gassers.add(new ArrayList<ExtendedUnit>());
						//////System.out.println("FINISHED REFINERY");
					} else if (typeID==UnitTypes.Terran_Armory.ordinal()) {
						////System.out.println("ARMORY");
						armories.add(unfinished.get(i)); 
						techMap.put(unfinished.get(i).unit.getID(), unfinished.get(i).unit.getTypeID());
					} else {
						research.add(unfinished.get(i));
						techMap.put(unfinished.get(i).unit.getID(), unfinished.get(i).unit.getTypeID());
					}
					tech.add(typeID);
					////////System.out.println("building finished");
					//refineries added by constructing SCVs since they don't produce callbacks
					//the basis for ground strength is a lone zergling, for air a single scourge (muta is 2) ... still they're mostly "Bulgarian constants"
				} else {
					if (typeID==UnitTypes.Terran_Dropship.ordinal()) {
						//transport.add(unfinished.get(i)); we don't build dropships
					} else if (typeID==UnitTypes.Terran_Ghost.ordinal()) {
						//ghosts.add(unfinished.get(i)); we don't build ghosts
						//myGround+=bwapi.getUnitType(typeID).getSupplyRequired();
						//myAir+=bwapi.getUnitType(typeID).getSupplyRequired();
						offense.add(unfinished.get(i).unit.getID());
					} else if (typeID==UnitTypes.Terran_Goliath.ordinal()) {
						goliaths.add(unfinished.get(i));
						//myGround+=bwapi.getUnitType(typeID).getSupplyRequired(); //will be built mostly as air defense
						//myAir+=bwapi.getUnitType(typeID).getSupplyRequired();
						offense.add(unfinished.get(i).unit.getID());
					} else if (typeID==UnitTypes.Terran_Marine.ordinal()) {
						rines.add(unfinished.get(i));
						//myGround+=bwapi.getUnitType(typeID).getSupplyRequired();
						//myAir+=bwapi.getUnitType(typeID).getSupplyRequired();
						defense.add(unfinished.get(i).unit.getID()); //going mech - marines for early defense (and to slow down attackers later)
					} else if (typeID==UnitTypes.Terran_Science_Vessel.ordinal()) {
						vessels.add(unfinished.get(i));
					} else if (typeID==UnitTypes.Terran_SCV.ordinal()) {
						loiterers.add(unfinished.get(i));
					} else if (typeID==UnitTypes.Terran_Siege_Tank_Tank_Mode.ordinal()) {
						siege.add(unfinished.get(i));
						//myGround+=bwapi.getUnitType(typeID).getSupplyRequired();
						offense.add(unfinished.get(i).unit.getID());
					} else if (typeID==UnitTypes.Terran_Valkyrie.ordinal()) {
						valkyries.add(unfinished.get(i));
						//myAir+=bwapi.getUnitType(typeID).getSupplyRequired(); //built only against mutas, otherwise their effectiveness isn't that great
						//offense.add(unfinished.get(i).unit.getID());
					} else if (typeID==UnitTypes.Terran_Vulture.ordinal()) {
						vult.add(unfinished.get(i));
						//myGround+=bwapi.getUnitType(typeID).getSupplyRequired(); //do 6 for dragoon , 4 for hydra
						//offense.add(unfinished.get(i).unit.getID());
					} else if (typeID==UnitTypes.Terran_Battlecruiser.ordinal()) {
						bc.add(unfinished.get(i));
						//myGround+=bwapi.getUnitType(typeID).getSupplyRequired();
						//myAir+=bwapi.getUnitType(typeID).getSupplyRequired();
						offense.add(unfinished.get(i).unit.getID());
					}
				}
				unfinished.remove(i);
				continue;
			}
			if (bwapi.getUnitType(unfinished.get(i).unit.getTypeID()).isBuilding()) {
				if (unfinished.get(i).unit.isBeingConstructed()) {
					unfinished.get(i).clearConstructionSite(); //clears all but one scvs
				}
				if (!unfinished.get(i).unit.isBeingConstructed()) {
					//check if assigned scv didn't die on it's way
					int j=0;
					while (j<unfinished.get(i).assignedWorkers.size()) {
						if (!unfinished.get(i).assignedWorkers.get(j).unit.isExists()) {
							unfinished.get(i).assignedWorkers.remove(j);
						} else {
							j++;
						}
					}
					if (unfinished.get(i).assignedWorkers.isEmpty()) {
						ExtendedUnit scv=nearestSCV(unfinished.get(i).unit,true,true);
						if (scv==null) {
							//this ain't goood
							//I won't even bother looking through all of the workers since we're already screwed
							bwapi.printText("no availible SCVs to finish building ... not sure if loosing horribly or something ain't right");
							i++;
							continue;
						}
						scv.buildRepair=unfinished.get(i).unit;
						unfinished.get(i).assignedWorkers.add(scv);
					}
				}
			} 
			i++;
		}
		//every 1000 frames or so reinit CCs (check if mined out)
		if (bwapi.getFrameCount()%1000<30) {
			for (int j=0;j<CC.size();j++) {
				CC.get(j).initCC(j);
			}
		}
		//miners
		for (ArrayList<ExtendedUnit> bases : miners) {
			i=0;
			while (i<bases.size()) {
				if ((!bases.get(i).mine())){//||(bases.get(i).unit.isIdle())) {
					loiterers.add(bases.get(i));
					bases.remove(i);
				} else {
					i++;
				}
			}
		}
		//gassers
		for (ArrayList<ExtendedUnit> gaysers : gassers) {
			i=0;
			while (i<gaysers.size()) {
				if ((!gaysers.get(i).mine())){//||(gaysers.get(i).unit.isIdle())) {
					loiterers.add(gaysers.get(i));
					gaysers.remove(i);
				} else {
					i++;
				}
			}
		}
		//builders
		i=0;
		while (i<builders.size()) {
			//////System.out.println("BUILDD! " + builders.size());
			if (!builders.get(i).build()) {
				loiterers.add(builders.get(i));
				builders.remove(i);
			} else {
				i++;
			}
		}
		/*//////System.out.println("MINERS: " + miners.get(0).size());
		//////System.out.println("GASSERS1: " + gassers.size());
		if (gassers.size()>0) //////System.out.println("GASSERS2: " + gassers.get(0).size());
		//////System.out.println("BUILDERS: " + builders.size());
		//////System.out.println("LOITERERS: " + loiterers.size());*/
		//send all stuck scv-s mining
		for (Unit stuck : bwapi.getMyUnits()) {
			if ((stuck.getTypeID()==UnitTypes.Terran_SCV.ordinal())&&(stuck.isIdle())) {
				Unit mineral=null;
				for (int j=0;j<CC.size();j++) {
					if (CC.get(j).closest_mineral!=null) {
						mineral=CC.get(j).closest_mineral;
						break;
					}
				}
				if (mineral!=null) {
					bwapi.gather(stuck.getID(), mineral.getID());
				}
			}
		}
		//loiterers
		i=0;
		while (i<loiterers.size()) {
			/*if (loiterers.get(i).unit.isConstructing()) {
				//////System.out.println("SHOULD BE CONSTRUCTING");
				i++;
				continue;
			}*/
			//assign builders
			if (loiterers.get(i).gonnaBuild!=-1) {
				builders.add(loiterers.get(i));
				loiterers.remove(i);
				continue;
			}
			for(int j=0;j<gassers.size();j++) {
				if (gassers.get(j).size()<3) {
					enoughSCV=false;
					if ((!all_in)&&(refineries.size()>j)&&(refineries.get(j)!=null)) {
						loiterers.get(i).gathering=refineries.get(j).unit;
						gassers.get(j).add(loiterers.get(i));
					}
				}
			}
			if (loiterers.get(i).gathering!=null) {
				loiterers.remove(i);
				continue;
			}
			for (int j=0;j<CC.size();j++) { //first check, add to underpopulated CC
				if (!CC.get(j).unit.isExists()) continue;
				if (CC.get(j).required_miners/2<miners.get(j).size()) continue;
				enoughSCV=false;
				loiterers.get(i).gathering=CC.get(j).closest_mineral;
				miners.get(j).add(loiterers.get(i));
			}
			if (loiterers.get(i).gathering!=null) {
				loiterers.remove(i);
				continue;
			}
			////////System.out.println("NOT FIRST MINERS");
			for (int j=0;j<CC.size();j++) { //second check, add anywhere with place to mine
				if (!CC.get(j).unit.isExists()) continue;
				if (CC.get(j).required_miners<=miners.get(j).size()) continue;
				enoughSCV=false;
				loiterers.get(i).gathering=CC.get(j).closest_mineral;
				miners.get(j).add(loiterers.get(i));
			}
			if (loiterers.get(i).gathering!=null) {
				loiterers.remove(i);
				continue;
			}
			////////System.out.println("NOT SECOND MINERS");
			miners.get(miners.size()-1).add(loiterers.get(i));
			loiterers.remove(i);
			enoughSCV=true;
			//////System.out.println("LOITER TILL THE END");
		}
		
		//scout
		for (int j=0;j<satComs.size();j++) {
			int frameCountHelp=bwapi.getFrameCount();
			if ((extendedTank!=null)&&(furthestTank!=null)) {
				if ((extendedTank.isUnderAttack)||((bwapi.getUnitType(extendedTank.unit.getTypeID()).getMaxHitPoints()>extendedTank.unit.getHitPoints())&&(frameCountHelp%400<29))) {
					if (satComs.get(j).unit.getEnergy()>49) {
						bwapi.useTech(satComs.get(j).unit.getID(),TechTypes.Scanner_Sweep.ordinal(),furthestTank.x,furthestTank.y);
						break;
					}
				}
			}
			if ((satComs.get(j).unit.getEnergy()>49)&&(frameCountHelp-lastScout>(3000/satComs.size()))) {
				//System.out.println("SHOULD SCAN");
				Random r=new Random();
				lastScout=frameCountHelp;
				if (enemyBases.size()>0) {
					//System.out.println("USING SCAN");
					//we scout first or last base (most probable places to find units)
					if (r.nextBoolean()) {
						bwapi.useTech(satComs.get(j).unit.getID(),TechTypes.Scanner_Sweep.ordinal(),enemyBases.peekFirst().x,enemyBases.peekFirst().y);
					} else {
						bwapi.useTech(satComs.get(j).unit.getID(),TechTypes.Scanner_Sweep.ordinal(),enemyBases.peekLast().x,enemyBases.peekLast().y);
					}
				} else {
					//System.out.println("USING SCAN");
					int randID=r.nextInt()%bwapi.getMap().getBaseLocations().size();
					bwapi.useTech(satComs.get(j).unit.getID(),TechTypes.Scanner_Sweep.ordinal(),bwapi.getMap().getBaseLocations().get(randID).getX(),bwapi.getMap().getBaseLocations().get(randID).getY());
				}
			}
		}
		if (bwapi.getFrameCount()-lastScout>6000) {
			bwapi.printText("GETFRAME SHOULD SCOUT");
		}
		if (scouter==null) {
			bwapi.printText("SCOUTER IS ALREADY DEAD");
		}
		if ((scouter==null)&&((bwapi.getFrameCount()-lastScout>6000)||((lastScout==0)&&(me.getSupplyUsed()>15)))) { //just before the first depo
			bwapi.printText("SCOUT");
			if (scouter==null) {
				if (lastScout==0) {
					scouter=miners.get(0).get(0);
					miners.get(0).remove(0);
					scouter.gathering=null;
					ArrayList<Point> startLocations=new ArrayList<Point>();
					for (BaseLocation bl : bwapi.getMap().getBaseLocations()) {
						if (bl.isStartLocation()) {
							startLocations.add(new Point(bl.getX(),bl.getY()));
						}
					} 
					scouter.initScout(startLocations, false);
					lastScout=bwapi.getFrameCount();
					//System.out.println("Initial scout");
				} else {
				/*	if ((!even)&&(!baseTrails.isEmpty())) {
						//suicidal SCV into enemy base
						scouter=nearestSCV(CC.get(CC.size()-1).unit,true,false);
						//remove from miners
						for (int k=0;i<miners.get(miners.size()-1).size();k++) {
							if (scouter.unit.getID()==miners.get(miners.size()-1).get(k).unit.getID()) {
								miners.get(miners.size()-1).remove(k);
							}
						}
						scouter.gathering=null;
						ArrayList<Point> startLocations=new ArrayList<Point>();
						for (BaseLocation bl : bwapi.getMap().getBaseLocations()) {
							if (bl.isStartLocation()) {
								startLocations.add(new Point(bl.getX(),bl.getY()));
							}
						}
						scouter.initScout(startLocations, false);
						lastScout=bwapi.getFrameCount();
						even=!even;
					} else {
						//later in game, we take the first vulture, run around the bases and mine
						if (vult.size()>0) {
							scouter=vult.get(0);
							vult.remove(0);
							ArrayList<Point> baseLocations=new ArrayList<Point>();
							for (BaseLocation bl : bwapi.getMap().getBaseLocations()) {
								if (usedBaseLocations.contains(bl.getRegionID())) continue;
								baseLocations.add(new Point(bl.getX(),bl.getY()));
							}
							scouter.initScout(baseLocations, false);
							lastScout=bwapi.getFrameCount();
							even=!even;
						}
					}*/
				}
			}
		}
		if (scouter!=null){
			if (scouter.unit.isExists()) scouter.scout(); else
				scouter=null;
		}
		//TODO all micro here
		furthestTank=null;
		extendedTank=null;
		if (enemyBases.size()>0)
			for (int j=0;j<siege.size();j++) {
				if ((furthestTank==null)||(distance(furthestTank,enemyBases.peekFirst())>distance(siege.get(j).unit,enemyBases.peekFirst()))) {
					furthestTank=new Point(siege.get(j).unit.getX(),siege.get(j).unit.getY());
					extendedTank=siege.get(j);
				}
						/*if (baseTrails.size()==0) {

			} else {

			}*/
			}
		for (Unit unit : bwapi.getEnemyUnits()) {
			//check for gas stealing
			if ((unit.getTypeID()==UnitTypes.Terran_Refinery.ordinal())||(unit.getTypeID()==UnitTypes.Zerg_Extractor.ordinal())||(unit.getTypeID()==UnitTypes.Protoss_Assimilator.ordinal())) {
				for (int j=0;j<CC.size();j++) {
					if (CC.get(j).extendedRegion.pointInside(unit.getX(), unit.getY())) {
						gas_stolen=true;
					}
				}
			}
		}
		
		//berserking
		i=0;
		while (i<berserkers.size()) {
			if (!berserkers.get(i).unit.isAttacking()) {
				System.out.println("REMOVING BERSERK");
				berserkers.remove(i);
			} else {
				i++;
			}
		}
		if (berserkers.size()<berserkCount) {
			for (int j=0;j<miners.get(0).size()-2;j++) {
				if (!berserkContains(miners.get(0).get(j).unit.getID())) {
					berserkers.add(miners.get(0).get(j));
				}
			}
		}
		i=0;
		for (int j=0;j<whatToBerserk.size();j++) { //was in a hurry ... sorry! :)
			if ((bwapi.getUnitType(whatToBerserk.get(j).getTypeID()).isFlyer())||(bwapi.getUnitType(whatToBerserk.get(j).getTypeID()).isFlyingBuilding())) continue;
			if (distance(whatToBerserk.get(j),new Point(homePositionX,homePositionY))>550) continue; //not our problem
			if (bwapi.getUnitType(whatToBerserk.get(j).getTypeID()).isWorker()) {
				if (i>=berserkers.size()) break;
				bwapi.attack(berserkers.get(i).unit.getID(),whatToBerserk.get(j).getID());
				i++;
				continue;
			}
			if (bwapi.getUnitType(whatToBerserk.get(j).getTypeID()).isBuilding()) {
				if (i>=berserkers.size()) break;
				bwapi.attack(berserkers.get(i).unit.getID(),whatToBerserk.get(j).getID());
				i++;
				if (i>=berserkers.size()) break;
				bwapi.attack(berserkers.get(i).unit.getID(),whatToBerserk.get(j).getID());
				i++;
				if (i>=berserkers.size()) break;
				bwapi.attack(berserkers.get(i).unit.getID(),whatToBerserk.get(j).getID());
				i++;
				if (i>=berserkers.size()) break;
				bwapi.attack(berserkers.get(i).unit.getID(),whatToBerserk.get(j).getID());
				i++;
				continue;
			}
			if (i>=berserkers.size()) break;
			bwapi.attack(berserkers.get(i).unit.getID(),whatToBerserk.get(j).getID());
			i++;
			if (i>=berserkers.size()) break;
			bwapi.attack(berserkers.get(i).unit.getID(),whatToBerserk.get(j).getID());
			i++;
		}
		while(i<berserkers.size()) {
			berserkers.get(i).gathering=null;
			berserkers.remove(i);
		}
		
		i=0;
		//basic micro
		boolean checked=false;
		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.getTypeID()==UnitTypes.Terran_SCV.ordinal()) {
				continue;
			}
			if (attack) {
				if ((enemyBases.isEmpty())&&(bwapi.getFrameCount()>20000)) {
					if (!allEnemyBuildings.isEmpty()) {
						for(Entry<Integer, Point> entry : allEnemyBuildings.entrySet()) {
						    enemyBases.add(entry.getValue());
						    Integer ebr=initBFS();
						    //findDefChokePoint();
						    allEnemyBuildings.remove(entry.getKey());
						    break;
						}
					} else {
						if (bwapi.getFrameCount()%2000<30) {
							this.attckIter=(this.attckIter+1)%bwapi.getMap().getBaseLocations().size();
						}
						/*(for (int j=0;j<bwapi.getMap().getBaseLocations().size();j++) {
						enemyBases.addFirst(new Point ());
					}*/
						//////System.out.println("ATTACK -> " + attck_iter);
						if (unit.getGroundWeaponCooldown()==0) {
							bwapi.attack(unit.getID(),bwapi.getMap().getBaseLocations().get(attckIter).getX(),bwapi.getMap().getBaseLocations().get(attckIter).getY());
							//////System.out.println("ATTACK !!");
						} else {
							bwapi.move(unit.getID(),homePositionX,homePositionY);
						}
					}
				} else {
					if (enemyBases.isEmpty()) continue;
					if ((!checked)&&(distance(unit,enemyBases.peekFirst())<200)) {
						for (Unit eu : bwapi.getEnemyUnits()) {
							if (distance(unit,eu)<500) checked=true;
						}
						if (!checked) {
							enemyBases.removeFirst();
							if (baseTrails.size()>0) baseTrails.remove(0);
							continue;
						}
					}
					if (furthestTank!=null)
						if ((distance(unit,furthestTank)>300)&&(distance(unit,enemyBases.peekFirst())<distance(furthestTank,enemyBases.peekFirst()))) {
							if (unit.getGroundWeaponCooldown()==0) {
								if (CC.size()<2) bwapi.attack(unit.getID(),homePositionX,homePositionY); else 
									bwapi.attack(unit.getID(),CC.get(1).unit.getX(),CC.get(1).unit.getY());
							} else {
								if (CC.size()<2) bwapi.move(unit.getID(),homePositionX,homePositionY); else 
									bwapi.move(unit.getID(),CC.get(1).unit.getX(),CC.get(1).unit.getY());
							}
						} else {
							bwapi.attack(unit.getID(),enemyBases.peekFirst().x,enemyBases.peekFirst().y);
						}
					else bwapi.attack(unit.getID(),enemyBases.peekFirst().x,enemyBases.peekFirst().y);
				}
			} else {
				if (!attack) {
					if (!whatToBerserk.isEmpty()) {
						bwapi.attack(unit.getID(), whatToBerserk.get(0).getX(), whatToBerserk.get(0).getY());
						continue;
					}
				}
				if (scouter!=null) if (unit.getID()==scouter.unit.getID()) continue;
				if (!unit.isAttackFrame()) {
					if (unit.getGroundWeaponCooldown()==0) {
						if (CC.size()<2) bwapi.attack(unit.getID(),homePositionX,homePositionY); else 
							bwapi.attack(unit.getID(),CC.get(1).unit.getX(),CC.get(1).unit.getY());
					} else {
						if (CC.size()<2) bwapi.move(unit.getID(),homePositionX,homePositionY); else 
							bwapi.move(unit.getID(),CC.get(1).unit.getX(),CC.get(1).unit.getY());
					}
				}
			}
			if (unit.getTypeID()==UnitTypes.Terran_Vulture.ordinal()) {
				if (unit.getSpiderMineCount()<1) continue;
				//Random r=new Random();
				//if ((r.nextInt()%42==0)&&(distance(unit,new Point(homePositionX,homePositionY))>400)) bwapi.useTech(unit.getID(), TechTypes.Spider_Mines.ordinal(), (unit.getX()+r.nextInt()%20), (unit.getY()+r.nextInt()%20));
			}
		}
		
		
		
		//macro !!FIX HERE!!!!
		if ((enemyAir>myAir)||(enemyGround>myGround)) pump_production=true; else pump_production=false;
		//pump_production=false;
		disposable();  //allocate cash
		if (pump_production) {
			supply(false);
			bwapi.printText("----");
			if (factories.size()==0) raxProduction(666);
			factProduction();
			starProduction(4);
			if (me.getSupplyUsed()>22) {
				if ((!tech.contains(UnitTypes.Terran_Refinery.ordinal()))&&!weAreBuilding(UnitTypes.Terran_Refinery.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Refinery.ordinal())) {
					System.out.println("TRY REF");
					build(UnitTypes.Terran_Refinery.ordinal(),CC.get(CC.size()-1).unit,null);
				}
			}
			if (!tech.contains(UnitTypes.Terran_Barracks.ordinal()) && !weAreBuilding(UnitTypes.Terran_Barracks.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Barracks.ordinal())) { 
				build(UnitTypes.Terran_Barracks.ordinal(),null,null);
			}
			if ( (second_fact==false) || (((factories.size()<CC.size()*3)&&!weAreBuilding(UnitTypes.Terran_Factory.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Factory.ordinal())))) {
				build(UnitTypes.Terran_Factory.ordinal(),null,null);
			}
			trainWorkers();
			raxProduction(me.getSupplyUsed()/13);
			if (me.getMinerals()>300) {
				build(UnitTypes.Terran_Factory.ordinal(),null,null);
				if (me.getSupplyTotal()>200) supply(true);
			}
			/*if (!weAreBuilding(UnitTypes.Terran_Command_Center.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Command_Center.ordinal()))
				expand();*/
		} else {
			supply(false);
			trainWorkers();
			if (((me.getSupplyTotal()>120)&&(CC.size()<2))&&(!weAreBuilding(UnitTypes.Terran_Command_Center.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Command_Center.ordinal()))) {
				//to prevent stucking to one base after an unfortunate set of events
				expand();
			}
			if ((enemyBases.size()>CC.size())||(CC.get(CC.size()-1).required_miners<miners.get(miners.size()-1).size()*1.5)&&(!weAreBuilding(UnitTypes.Terran_Command_Center.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Command_Center.ordinal())))
				expand();
			bwapi.printText("----");
			if (me.getSupplyUsed()>22) {
				if ((!tech.contains(UnitTypes.Terran_Refinery.ordinal()))&&!weAreBuilding(UnitTypes.Terran_Refinery.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Refinery.ordinal())) {
					System.out.println("TRY REF");
					build(UnitTypes.Terran_Refinery.ordinal(),CC.get(CC.size()-1).unit,null);
				}
			}
			if (!tech.contains(UnitTypes.Terran_Barracks.ordinal()) && !weAreBuilding(UnitTypes.Terran_Barracks.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Barracks.ordinal())) { 
				build(UnitTypes.Terran_Barracks.ordinal(),null,null);
			}
			tech();
			if (tech.contains(UnitTypes.Terran_Factory.ordinal())&&((factories.size()/3)+1>starports.size()) && !weAreBuilding(UnitTypes.Terran_Starport.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Starport.ordinal())) { 
				build(UnitTypes.Terran_Starport.ordinal(),null,null);
				return;
			}
			if ( (second_fact==false) || ((factories.size()<CC.size()*3) && (tech.contains(UnitTypes.Terran_Starport.ordinal())&&!weAreBuilding(UnitTypes.Terran_Factory.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Factory.ordinal())))) {
				build(UnitTypes.Terran_Factory.ordinal(),null,null);
			}
			raxProduction(4);
			factProduction();
			starProduction(4);
			if (me.getMinerals()>700) {
				if (me.getSupplyTotal()>200) supply(true);
				build(UnitTypes.Terran_Factory.ordinal(),null,null);
			}
		}
	}
	
	
	// Method called on every frame (approximately 30x every second).
	public void gameUpdate() {
		
		// Remember our homeTilePosition at the first frame
		if (bwapi.getFrameCount() == 0) {
			int cc = getNearestUnit(UnitTypes.Terran_Command_Center.ordinal(), 0, 0);
			if (cc == -1) cc = getNearestUnit(UnitTypes.Zerg_Hatchery.ordinal(), 0, 0);
			if (cc == -1) cc = getNearestUnit(UnitTypes.Protoss_Nexus.ordinal(), 0, 0);
			homePositionX = bwapi.getUnit(cc).getX();
			homePositionY = bwapi.getUnit(cc).getY();

		}
		
		// Draw debug information on screen
		drawDebugInfo();

		// Call the act() method every 30 frames
		if (bwapi.getFrameCount() % 30 == 0) {
			act();
		}
		if (bwapi.getFrameCount() % 15 == 0) { //only here because I tried it for smaller interval
			//better micro
			removeDead();
			assessEnemy();
			for (int j=0;j<siege.size();j++) {
				if ((!attack)&&(siege.get(j).unit.getTypeID()==UnitTypes.Terran_Siege_Tank_Tank_Mode.ordinal())) {
					if (((CC.size()<2)&&(distance(siege.get(j).unit,new Point(homePositionX,homePositionY))<200))||((CC.size()>1)&&(distance(siege.get(j).unit,CC.get(1).unit)<200))) {
						if (me.hasResearched(TechTypes.Tank_Siege_Mode.ordinal())) {
							System.out.println("......SIEGING UP");
							bwapi.useTech(siege.get(j).unit.getID(),TechTypes.Tank_Siege_Mode.ordinal());
							continue;
						}
					}
				}
				if (siege.get(j).unit.getTypeID()==UnitTypes.Terran_Siege_Tank_Tank_Mode.ordinal()) {
					if ((me.hasResearched(TechTypes.Tank_Siege_Mode.ordinal()))&&(siege.get(j).siegeEnemy!=null)&&(siege.get(j).enemy==null)) {
						System.out.println("......SIEGING UP");
						bwapi.useTech(siege.get(j).unit.getID(),TechTypes.Tank_Siege_Mode.ordinal());
					} else {
						if ((siege.get(j).threat!=null)&&((siege.get(j).enemy==null)||(siege.get(j).unit.getGroundWeaponCooldown()>0))) {
							if (CC.size()<2) bwapi.move(siege.get(j).unit.getID(),homePositionX,homePositionY); else 
								bwapi.move(siege.get(j).unit.getID(),CC.get(1).unit.getX(),CC.get(1).unit.getY());
						}
					}
				} else {
					if ((siege.get(j).siegeEnemy==null)&&(attack)) {
						System.out.println("......UNSIEGING DOWN");
						bwapi.useTech(siege.get(j).unit.getID(),TechTypes.Tank_Siege_Mode.ordinal());
					}
				}
			}
			//if (extendedTank==null)||(extendedTank.unit==null)||(!extendedTank.unit.isExists())||
			for (int j=0;j<rines.size();j++) {
				if ((rines.get(j).threat!=null)&&(bwapi.getUnitType(rines.get(j).threat.getTypeID()).isBuilding())) {
					if (CC.size()<2) bwapi.move(rines.get(j).unit.getID(),homePositionX,homePositionY); else 
						bwapi.move(rines.get(j).unit.getID(),CC.get(1).unit.getX(),CC.get(1).unit.getY());
				}
				if ((rines.get(j).threat!=null)&&((rines.get(j).enemy==null)||(rines.get(j).unit.getGroundWeaponCooldown()>0))) {
					if (CC.size()<2) bwapi.move(rines.get(j).unit.getID(),homePositionX,homePositionY); else 
						bwapi.move(rines.get(j).unit.getID(),CC.get(1).unit.getX(),CC.get(1).unit.getY());
				}
			}
			//a bit more intelligent micro based on unit type
			for (int j=0;j<vult.size();j++) {
				if ((vult.get(j).threat!=null)&&(bwapi.getUnitType(vult.get(j).threat.getTypeID()).isBuilding())) {
					if (CC.size()<2) bwapi.move(vult.get(j).unit.getID(),homePositionX,homePositionY); else 
						bwapi.move(vult.get(j).unit.getID(),CC.get(1).unit.getX(),CC.get(1).unit.getY());
				}
				if ((vult.get(j).threat!=null)&&((vult.get(j).enemy==null)||(vult.get(j).unit.getGroundWeaponCooldown()>0))) {
					if (CC.size()<2) bwapi.move(vult.get(j).unit.getID(),homePositionX,homePositionY); else 
						bwapi.move(vult.get(j).unit.getID(),CC.get(1).unit.getX(),CC.get(1).unit.getY());
				}
				Random r=new Random();
				if ((furthestTank!=null)&&(distance(vult.get(j).unit,furthestTank)>50)&&(vult.get(j).unit.getSpiderMineCount()>0)&&(vult.get(j).threat!=null)/*&&((vult.get(j).enemy==null)||(vult.get(j).unit.getGroundWeaponCooldown()>0))*/) 
					bwapi.useTech(vult.get(j).unit.getID(), TechTypes.Spider_Mines.ordinal(), (vult.get(j).unit.getX()+r.nextInt()%20), (vult.get(j).unit.getY()+r.nextInt()%20));
			}
			for (int j=0;j<vessels.size();j++) {
				if (furthestTank==null) {
					if (CC.size()<2) bwapi.move(vessels.get(j).unit.getID(),homePositionX,homePositionY); else 
						bwapi.move(vessels.get(j).unit.getID(),CC.get(1).unit.getX(),CC.get(1).unit.getY());
				} else {
					bwapi.move(vessels.get(j).unit.getID(),furthestTank.x,furthestTank.y);
					if (extendedTank.siegeEnemy!=null) {
						if (!extendedTank.unit.isDefenseMatrixed()) {
							bwapi.useTech(vessels.get(j).unit.getID(), TechTypes.Defensive_Matrix.ordinal(), extendedTank.unit.getID());
						}
						if (me.hasResearched(TechTypes.EMP_Shockwave.ordinal())) {
							bwapi.useTech(vessels.get(j).unit.getID(), TechTypes.EMP_Shockwave.ordinal(), extendedTank.siegeEnemy.getX(),extendedTank.siegeEnemy.getY());
						}
						if (me.hasResearched(TechTypes.Irradiate.ordinal())) {
							bwapi.useTech(vessels.get(j).unit.getID(), TechTypes.Irradiate.ordinal(), extendedTank.siegeEnemy.getID());
						}
					}
				}
			}
			for (int j=0;j<wraiths.size();j++) {
				if ((wraiths.get(j).unit.getHitPoints()<bwapi.getUnitType(wraiths.get(j).unit.getTypeID()).getMaxHitPoints())&&(me.hasResearched(TechTypes.Cloaking_Field.ordinal()))&&(!wraiths.get(j).unit.isCloaked())&&(wraiths.get(j).unit.getEnergy()>40)) {
					System.out.println("CLOAKING");
					bwapi.useTech(wraiths.get(j).unit.getID(), TechTypes.Cloaking_Field.ordinal());
				}
				if ((furthestTank!=null)&&(wraiths.get(j).unit.getHitPoints()<bwapi.getUnitType(wraiths.get(j).unit.getTypeID()).getMaxHitPoints()*2/3)) { //on low health do support
					if (distance(wraiths.get(j).unit,furthestTank)>200) bwapi.move(wraiths.get(j).unit.getID(),furthestTank.x,furthestTank.y);
						else bwapi.attack(wraiths.get(j).unit.getID(),furthestTank.x,furthestTank.y);
					continue;
				}
				if (enemyBases.size()>0) {
					if (distance(wraiths.get(j).unit,enemyBases.getFirst())>32) bwapi.move(wraiths.get(j).unit.getID(),enemyBases.getFirst().x,enemyBases.getFirst().y);
						else if (wraiths.get(j).enemy!=null) bwapi.attack(wraiths.get(j).unit.getID(),wraiths.get(j).enemy.getID()); 
								else bwapi.attack(wraiths.get(j).unit.getID(),enemyBases.getFirst().x,enemyBases.getFirst().y);
				}
			}
		}
	}

	// Some additional event-related methods.
	public void gameEnded() {}
	public void matchEnded(boolean winner) {}
	public void nukeDetect() {} //lol no idea how to handle this, just ignore I guess
	public void playerLeft(int id) {}
	public void unitEvade(int unitID) {}
	public void keyPressed(int keyCode) {}
	
	public void nukeDetect(int x, int y) {} //TODO
	
	public void unitCreate(int unitID) {
		////////System.out.println("CREATED!!!!!!!");
		if (bwapi.getUnit(unitID).getPlayerID()==enemy.getID()) return; //handled in discover
		if (!bwapi.getUnit(unitID).isCompleted()) {
			unfinished.add(new ExtendedUnit(bwapi.getUnit(unitID),this));
			queuedBuildings.remove(bwapi.getUnit(unitID).getTypeID());
		} else {
			//probably spider-mine, can't think of anything else, do nothing for now
		}
	}
	
	public void unitDestroy(int unitID) {
		if (myIds.containsKey(unitID)) {
			if ((scouter!=null)&&(unitID==scouter.unit.getID())) {
				bwapi.printText("SCOUT DIED");
				if (enemyBases.isEmpty()) { //don't know where to go, a place where they killed my scout seems like a good idea
					enemyBases.add(new Point(scouter.unit.getX(),scouter.unit.getY()));
					//////System.out.println("FOUND (MAYBE) BASE!");
					Integer ebr=initBFS();
					if (ebr!=null) usedBaseLocations.add(ebr);
					//findDefChokePoint();
				}
				scouter=null;
			}
			if (techMap.containsKey(unitID)) {
				int value=techMap.get(unitID);
				techMap.remove(unitID);
				if (!techMap.containsValue(value)) tech.remove(techMap.get(unitID));
			}
			for (int i=0;i<CC.size();i++) {
				if (CC.get(i).unit.getID()==unitID) {
					usedBaseLocations.remove(CC.get(i).regionID);
				}
			}
			if (myIds.get(unitID)!=null) {
				offense.remove(unitID);
				defense.remove(unitID);
			}
		} else {
			allEnemyBuildings.remove(unitID);
			if (enemyIds.get(unitID)!=null) {
				enemyAir-=enemyIds.get(unitID).y;
				enemyGround-=enemyIds.get(unitID).x;
				enemyIds.remove(unitID);
			}
		}
	}
	
	public void unitDiscover(int unitID) {
		if ((bwapi.getUnit(unitID).getPlayerID()!=me.getID())&&(bwapi.getUnit(unitID).getPlayerID()!=enemy.getID())) return; //ignore neutral
		if (bwapi.getUnit(unitID).getPlayerID()==me.getID()) {
			if (bwapi.getUnitType(bwapi.getUnit(unitID).getTypeID()).isCanAttackAir()) {
				myIds.put(unitID,new Point(bwapi.getUnitType(bwapi.getUnit(unitID).getTypeID()).getSupplyRequired(),bwapi.getUnitType(bwapi.getUnit(unitID).getTypeID()).getSupplyRequired())); //for destroy event, we silently ignore the error for valkyries 
			} else {
				if (bwapi.getUnit(unitID).getTypeID()==UnitTypes.Terran_SCV.ordinal()) myIds.put(unitID,new Point(0,0));
				myIds.put(unitID,new Point(bwapi.getUnitType(bwapi.getUnit(unitID).getTypeID()).getSupplyRequired(),0));
			}
			return; //rest handled in create
		}
		if (bwapi.getUnitType(bwapi.getUnit(unitID).getTypeID()).isBuilding()) {
			if (!allEnemyBuildings.containsKey(unitID)) {
				//TODO checks stuff
				allEnemyBuildings.put(unitID,new Point(bwapi.getUnit(unitID).getX(),bwapi.getUnit(unitID).getY()));
				if ((bwapi.getUnit(unitID).getTypeID()==UnitTypes.Terran_Command_Center.ordinal())||(bwapi.getUnit(unitID).getTypeID()==UnitTypes.Protoss_Nexus.ordinal())||(bwapi.getUnit(unitID).getTypeID()==UnitTypes.Zerg_Hatchery.ordinal())||(bwapi.getUnit(unitID).getTypeID()==UnitTypes.Zerg_Lair.ordinal())||(bwapi.getUnit(unitID).getTypeID()==UnitTypes.Zerg_Hive.ordinal())) {
					enemyBases.add(new Point(bwapi.getUnit(unitID).getX(),bwapi.getUnit(unitID).getY()));
					//////System.out.println("FOUND BASE!");
					Integer ebr=initBFS();
					if (ebr!=null) usedBaseLocations.add(ebr);
					//findDefChokePoint();
				}
			}
		} else {
			if (!allEnemyUnits.contains(unitID)) {
				//TODO check some more stuff
				if ((bwapi.getUnit(unitID).getTypeID()==UnitTypes.Zerg_Larva.ordinal())||(bwapi.getUnit(unitID).getTypeID()==UnitTypes.Zerg_Cocoon.ordinal())||bwapi.getUnit(unitID).getTypeID()==UnitTypes.Zerg_Lurker_Egg.ordinal()) return;
				allEnemyUnits.add(unitID);
				if (getUnitType(bwapi.getUnit(unitID).getTypeID()).isWorker()) return;
				if (bwapi.getUnitType(bwapi.getUnit(unitID).getTypeID()).isFlyer()) {
					enemyIds.put(unitID,new Point(0,bwapi.getUnitType(bwapi.getUnit(unitID).getTypeID()).getSupplyRequired())); //for destroy event, we silently ignore the error for valkyries 
				} else {
					enemyIds.put(unitID,new Point(bwapi.getUnitType(bwapi.getUnit(unitID).getTypeID()).getSupplyRequired(),0));
				}
				enemyAir+=enemyIds.get(unitID).y;
				enemyGround+=enemyIds.get(unitID).x;
			}
		}
	}
	
	public void unitHide(int unitID) {
		for (int i=0;i<satComs.size();i++) {
			if ((satComs.get(i).unit.isExists())&&(satComs.get(i).unit.getEnergy()>=50)) {
				bwapi.useTech(satComs.get(i).unit.getID(), TechTypes.Scanner_Sweep.ordinal());
				return;
			}
		}
	}
	
	public void unitMorph(int unitID) {}
	
	public void unitShow(int unitID) {
		if (bwapi.getUnit(unitID).getPlayerID()!=enemy.getID()) return;
		if (bwapi.getUnitType(bwapi.getUnit(unitID).getTypeID()).isBuilding()) {
			if (!allEnemyBuildings.containsKey(unitID)) { //change ->this will now contain unitIDs
				//TODO checks stuff
				allEnemyBuildings.put(unitID,new Point(bwapi.getUnit(unitID).getX(),bwapi.getUnit(unitID).getY()));
				if ((bwapi.getUnit(unitID).getTypeID()==UnitTypes.Terran_Command_Center.ordinal())||(bwapi.getUnit(unitID).getTypeID()==UnitTypes.Protoss_Nexus.ordinal())||(bwapi.getUnit(unitID).getTypeID()==UnitTypes.Zerg_Hatchery.ordinal())||(bwapi.getUnit(unitID).getTypeID()==UnitTypes.Zerg_Lair.ordinal())||(bwapi.getUnit(unitID).getTypeID()==UnitTypes.Zerg_Hive.ordinal())) {
					enemyBases.add(new Point(bwapi.getUnit(unitID).getX(),bwapi.getUnit(unitID).getY()));
					//////System.out.println("FOUND BASE!");
					Integer ebr=initBFS();
					if (ebr!=null) usedBaseLocations.add(ebr);
					//findDefChokePoint();
				}
			}
			//if (this.enemyBases.isEmpty()) enemyBases.add(new Point(bwapi.getUnit(unitID).getX(),bwapi.getUnit(unitID).getY()));
		} else {
			if (!allEnemyUnits.contains(unitID)) {
				//TODO check some more stuff
				allEnemyUnits.add(unitID);
				if (getUnitType(bwapi.getUnit(unitID).getTypeID()).isWorker()) return;
				if (bwapi.getUnitType(bwapi.getUnit(unitID).getTypeID()).isFlyer()) {
					enemyIds.put(unitID,new Point(0,bwapi.getUnitType(bwapi.getUnit(unitID).getTypeID()).getSupplyRequired())); //for destroy event, we silently ignore the error for valkyries
					if (bwapi.getUnit(unitID).getTypeID()==UnitTypes.Zerg_Mutalisk.ordinal()) need_goliaths=true;
					if (bwapi.getUnit(unitID).getTypeID()!=UnitTypes.Zerg_Overlord.ordinal()) need_goliaths=true;
				} else {
					enemyIds.put(unitID,new Point(bwapi.getUnitType(bwapi.getUnit(unitID).getTypeID()).getSupplyRequired(),0));
				}
				enemyAir+=enemyIds.get(unitID).y;
				enemyGround+=enemyIds.get(unitID).x;
			}
		}
		
	}
	
	//macroing functions
	/*
	 * Variant I: PumpUnits
	 * -trainArmy to no limits
	 * -trainWorkers if every building is pumping already
	 * -only supply depots/turrets/production is built when floating minerals
	 * 
	 *  Variant II: Macro
	 *  -trainWorkers
	 *  -trainArmy to match 1.5x expected enemy army
	 *  -tech (nonagressively)
	 *  -scanner sweep / scout every few frames (also drop if possible / if I'll have time to implement)
	 *  -when floating resources decide between expanding or additional production facilities
	 *  -when still floating build more units
	 *  
	 *  
	 *  Every point allocates money and returns success/failure as bool (false if nothing was built 
	 *  
	 *  Ideal saturation for a base is 1.5 workers / patch, max at 2 - 2.5 , when at 1.5 look for expansion (if macroing)
	 */
	
	private boolean trainWorkers() {
		if (tech.contains(UnitTypes.Terran_Academy.ordinal())) {
			for (int i=0;i<CC.size();i++) {
				if (CC.get(i).unit.isTraining()) continue;
				if (!CC.get(i).unableToAddon) {
					if (CC.get(i).firstAddonFrame==-1) CC.get(i).firstAddonFrame=bwapi.getFrameCount();
					if ((bwapi.getFrameCount()-CC.get(i).firstAddonFrame)>666) CC.get(i).unableToAddon=true;
					bwapi.buildAddon(CC.get(i).unit.getID(), UnitTypes.Terran_Comsat_Station.ordinal()); //it's cheap , let's hoep we get the money in the time frame
					return true;
				}
			}
		}
		if (enoughSCV==true) return true;
		if (disposable_min<50) return false;
		/*int current=0;
		for (int i=0;i<miners.size();i++) {
			current+=miners.get(i).size();
		}
		for (int i=0;i<gassers.size();i++) {
			current+=gassers.get(i).size();
		}
		current+=repairers.size();
		current+=berserkers.size();
		if (current>85) return true;*/
		for (int i=0;i<CC.size();i++) {
			if ((!CC.get(i).unit.isTraining())&&(disposable_min>=50)) {
				bwapi.train(CC.get(i).unit.getID(), UnitTypes.Terran_SCV.ordinal());
				disposable_min-=50;
			}
		}
		return true;
	}
	
	private void tech() {
		//mines -> siege -> range -> start upgrades -> irradiate/emp -> yamato
		//academy -> armory -> engineering bay(turrets) -> research facility
		//buildings
		for (int i=0;i<research.size();i++) {
			if (research.get(i).unit.getTypeID()==UnitTypes.Terran_Control_Tower.ordinal()) {
				/*if (!me.isResearching(TechTypes.Cloaking_Field.ordinal())) { //not in use now
					if (!me.hasResearched(TechTypes.Cloaking_Field.ordinal())) {
						disposable_min-=150;
						disposable_gas-=150;
						bwapi.research(research.get(i).unit.getID(), TechTypes.Cloaking_Field.ordinal());
						continue;
					}
				}*/
			}
			if (research.get(i).unit.getTypeID()==UnitTypes.Terran_Machine_Shop.ordinal()) {
				if (!me.isResearching(TechTypes.Spider_Mines.ordinal())) {
					if (!me.hasResearched(TechTypes.Spider_Mines.ordinal())) {
						disposable_min-=100;
						disposable_gas-=100;
						bwapi.research(research.get(i).unit.getID(), TechTypes.Spider_Mines.ordinal());
						continue;
					}
				}
				if (!me.isResearching(TechTypes.Tank_Siege_Mode.ordinal())) {
					//////System.out.println("NOT RESEARCHING SIEGE");
					if (!me.hasResearched(TechTypes.Tank_Siege_Mode.ordinal())) {
						//////System.out.println("NOT RESEARCHED SIEGE MODE");
						disposable_min-=150;
						disposable_gas-=150;
						bwapi.research(research.get(i).unit.getID(), TechTypes.Tank_Siege_Mode.ordinal());
						continue;
					}
				}
				if (me.hasResearched(TechTypes.Tank_Siege_Mode.ordinal())&&(need_goliaths))
					if (!me.isUpgrading(UpgradeTypes.Charon_Boosters.ordinal()))
						if (me.upgradeLevel(UpgradeTypes.Charon_Boosters.ordinal())<1) {	
							disposable_min-=100;
							disposable_gas-=100;
							bwapi.upgrade(research.get(i).unit.getID(), UpgradeTypes.Charon_Boosters.ordinal());
							continue;
						}
				if (!me.isUpgrading(UpgradeTypes.Ion_Thrusters.ordinal())) {
					if (me.upgradeLevel(UpgradeTypes.Ion_Thrusters.ordinal())<1) {
						////System.out.println("NO ION THRUSTERS");
						disposable_min-=100;
						disposable_gas-=100;
						bwapi.upgrade(research.get(i).unit.getID(), UpgradeTypes.Ion_Thrusters.ordinal());
						continue;
					}
				}
			}
			if (research.get(i).unit.getTypeID()==UnitTypes.Terran_Science_Facility.ordinal()) {
				if (!(research.get(i).unit.getAddOnID()>-1)) {
					bwapi.buildAddon(research.get(i).unit.getID(), UnitTypes.Terran_Physics_Lab.ordinal());
					continue;
				}
				if (me.hasResearched(TechTypes.Tank_Siege_Mode.ordinal())) {
					if ((enemy.getRaceID()==2)&&(!me.hasResearched(TechTypes.EMP_Shockwave.ordinal()))) {
						bwapi.research(research.get(i).unit.getID(), TechTypes.EMP_Shockwave.ordinal());
					}
					if ((enemy.getRaceID()!=2)&&(!me.hasResearched(TechTypes.Irradiate.ordinal()))) {
						bwapi.research(research.get(i).unit.getID(), TechTypes.Irradiate.ordinal());
					}
				}
			}
		}
		for (int i=0;i<armories.size();i++) {
			if (!me.isUpgrading(UpgradeTypes.Terran_Vehicle_Weapons.ordinal())) {
				//////System.out.println("UPGRAAAAAAAAAAAAAAAAAAAAAAAAAADE");
				disposable_min-=150;
				disposable_gas-=150;
				bwapi.upgrade(armories.get(i).unit.getID(), UpgradeTypes.Terran_Vehicle_Weapons.ordinal());
				continue;
			}
			if (!me.isUpgrading(UpgradeTypes.Terran_Vehicle_Plating.ordinal())) {
				disposable_min-=150;
				disposable_gas-=150;
				bwapi.upgrade(armories.get(i).unit.getID(), UpgradeTypes.Terran_Vehicle_Plating.ordinal());
				continue;
			}
		}
		if (tech.contains(UnitTypes.Terran_Factory.ordinal())&&!tech.contains(UnitTypes.Terran_Academy.ordinal()) && !weAreBuilding(UnitTypes.Terran_Academy.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Academy.ordinal())) { 
			build(UnitTypes.Terran_Academy.ordinal(),null,null);
			return;
		}
		if (tech.contains(UnitTypes.Terran_Academy.ordinal())&&(armories.size()<2)/*!tech.contains(UnitTypes.Terran_Armory.ordinal())*/ && !weAreBuilding(UnitTypes.Terran_Armory.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Armory.ordinal())) { 
			build(UnitTypes.Terran_Armory.ordinal(),null,null);
			return;
		}
		if (tech.contains(UnitTypes.Terran_Academy.ordinal())&&!tech.contains(UnitTypes.Terran_Engineering_Bay.ordinal()) && !weAreBuilding(UnitTypes.Terran_Engineering_Bay.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Engineering_Bay.ordinal())) { 
			build(UnitTypes.Terran_Engineering_Bay.ordinal(),null,null);
			return;
		}
		if (tech.contains(UnitTypes.Terran_Starport.ordinal())&&tech.contains(UnitTypes.Terran_Armory.ordinal())&&!tech.contains(UnitTypes.Terran_Science_Facility.ordinal()) && !weAreBuilding(UnitTypes.Terran_Science_Facility.ordinal())&&!queuedBuildings.contains(UnitTypes.Terran_Science_Facility.ordinal())) { 
			build(UnitTypes.Terran_Science_Facility.ordinal(),null,null);
			return;
		}
	}
	
	
	private boolean expand() {
		if (disposable_min<400) {
			disposable_min-=400;
			return false;
		}
		//////System.out.println("SHOULD EXPAND");
		double distance=9999999.9;
		BaseLocation result=null;
		for (BaseLocation base : bwapi.getMap().getBaseLocations()) {
			if ((!usedBaseLocations.contains(base.getRegionID()))&&(distance(new Point(CC.get(0).unit.getX(),CC.get(0).unit.getY()),new Point(base.getX(),base.getY()))<distance)) {
				distance=distance(new Point(CC.get(0).unit.getX(),CC.get(0).unit.getY()),new Point(base.getX(),base.getY()));
				result=base;
			}
		}
		if (result==null) return false;
		//////System.out.println("GONNA EXPAND");
		build(UnitTypes.Terran_Command_Center.ordinal(),null,new Point(result.getTx(),result.getTy()));
		if (rines.size()!=0) bwapi.attack(rines.get(0).unit.getID(),result.getX()+100,result.getY());
		return true;
	}
	
	private void raxProduction(int limit) {
		for (ExtendedUnit oneRax : rax) {
			if (!oneRax.unit.isTraining()) {
				if (defense.size()<limit) bwapi.train(oneRax.unit.getID(), UnitTypes.Terran_Marine.ordinal());
			}
		}
	}
	
	private void factProduction() {
		for (ExtendedUnit oneFact : factories) {
			if ((oneFact.unit.isTraining())||(oneFact.unit.isUpgrading())||oneFact.unit.isConstructing()) continue;
			if ((!all_in)&&(!oneFact.unableToAddon)) {
				if (oneFact.firstAddonFrame==-1) oneFact.firstAddonFrame=bwapi.getFrameCount();
				if ((bwapi.getFrameCount()-oneFact.firstAddonFrame)>100) oneFact.unableToAddon=true; //since all refineries all built in a way that addons can be made, this is sort of a random from "if we have the gass, build it, otherwise don't"
				bwapi.buildAddon(oneFact.unit.getID(), UnitTypes.Terran_Machine_Shop.ordinal()); //it's cheap , let's hope we get the money in the time frame
				continue;
			}
			//first check antiair
			if (((disposable_gas<150)&&(disposable_min>500))||((disposable_gas<50)&&(disposable_min>100)))	bwapi.train(oneFact.unit.getID(),UnitTypes.Terran_Vulture.ordinal());
			if ((oneFact.unit.getAddOnID()>-1)&&(siege.size()<2)) {
				bwapi.train(oneFact.unit.getID(),UnitTypes.Terran_Siege_Tank_Tank_Mode.ordinal());
				continue;
			}
			if (tech.contains(UnitTypes.Terran_Armory.ordinal())) {
				if ((goliaths.size()<2)||(enemyAir>myAir)||((need_goliaths)&&(goliaths.size()<10))) { //this is vs BC & Carriers
					bwapi.train(oneFact.unit.getID(),UnitTypes.Terran_Goliath.ordinal());
					continue;
				}
			}
			if ((vult.size()>5)&&(oneFact.unit.getAddOnID()>-1)) {
				bwapi.train(oneFact.unit.getID(),UnitTypes.Terran_Siege_Tank_Tank_Mode.ordinal());
				continue;
			}
			bwapi.train(oneFact.unit.getID(),UnitTypes.Terran_Vulture.ordinal());
		}
	}
	
	private void starProduction(int limit) {
		for (ExtendedUnit oneStar : starports) {
			if ((oneStar.unit.isTraining())||(oneStar.unit.isUpgrading())||oneStar.unit.isConstructing()) continue;
			if ((!all_in)&&(!oneStar.unableToAddon)) {
				if (oneStar.firstAddonFrame==-1) oneStar.firstAddonFrame=bwapi.getFrameCount();
				if ((bwapi.getFrameCount()-oneStar.firstAddonFrame)>444) oneStar.unableToAddon=true;
				bwapi.buildAddon(oneStar.unit.getID(), UnitTypes.Terran_Control_Tower.ordinal()); //it's cheap , let's hope we get the money in the time frame
				continue;
			}
			if ((tech.contains(UnitTypes.Terran_Science_Facility.ordinal()))&&(vessels.size()<3)) {
				bwapi.train(oneStar.unit.getID(),UnitTypes.Terran_Science_Vessel.ordinal());
				continue;
			}
			if (wraiths.size()<limit) {
					bwapi.train(oneStar.unit.getID(),UnitTypes.Terran_Wraith.ordinal());
					continue;
			}
			if ((need_valkyries)&&(valkyries.size()<4)&&(oneStar.unit.getAddOnID()>-1)) {
				//bwapi.train(oneStar.unit.getID(),UnitTypes.Terran_Valkyrie.ordinal());
				//continue;
			}
			if (tech.contains(UnitTypes.Terran_Physics_Lab.ordinal())&&(oneStar.unit.getAddOnID()>-1)) {
				bwapi.train(oneStar.unit.getID(),UnitTypes.Terran_Battlecruiser.ordinal());
				continue;
			}
		}
	}


	//always called first, expect when pumping units - in that case, we build supply only when we can't fit another unit into current limit
	//on second pass, build even if we're already building one but floating minerals
	//returning false indicates that we need the depo but can't build it (so it should halt other production / reserve money)
	private boolean supply(boolean second_pass) {
		int supply_value=4+bwapi.getSelf().getSupplyUsed()/10;
		if (bwapi.getSelf().getSupplyTotal()>50) supply_value=20;
		if (bwapi.getSelf().getSupplyUsed()==400) {supply_value=0; second_pass=false;}	//don't build supply when at max
		if (bwapi.getSelf().getSupplyUsed()>bwapi.getSelf().getSupplyTotal()) supply_value=400; //build when supply was destroyed
		//if ((bwapi.getSelf().getSupplyTotal()>80)&&(bwapi.getSelf().getSupplyTotal()<110)) supply_value+=4; //this tends to be the point where mass production starts 
		if ((((bwapi.getSelf().getSupplyTotal() - bwapi.getSelf().getSupplyUsed())) < supply_value)||(second_pass)) {
			if (disposable_min<100) {
				disposable_min-=100;
				return false;
			}
			//choose random inner coordinate of a random base - we build around the perimeter to make depots "scout" for drops and such
			Random r=new Random();
			ExtendedUnit randomCC=CC.get(abs(r.nextInt())%CC.size());
			ExtendedRegion random_region=randomCC.extendedRegion;
			/*for (int i=0;i<regions.size();i++) {
				if (regions.get(i).pointInside(randomCC.getX(), randomCC.getY())) {
					random_region=regions.get(i);
				}
			}*/
			//ExtendedRegion random_region=getExtendedRegion(CC.get(abs(r.nextInt())%CC.size()).regionID);
			Point random_point=random_region.innerCoordinates.get(abs(r.nextInt())%random_region.innerCoordinates.size());
			ExtendedUnit builder=nearestSCV(random_point,true,false);
			Point buildTile = getBuildTile(builder.unit.getID(), UnitTypes.Terran_Supply_Depot.ordinal(), random_point.x, random_point.y);
			if ((buildTile.x != -1) && (!queuedBuildings.contains(UnitTypes.Terran_Supply_Depot.ordinal())) && ((second_pass)||(!weAreBuilding(UnitTypes.Terran_Supply_Depot.ordinal())))) {
				builder.gonnaBuild=UnitTypes.Terran_Supply_Depot.ordinal();
				builder.whereToBuild=new Point(buildTile.x,buildTile.y);
				builder.commandFrame=bwapi.getFrameCount();
				queuedBuildings.add(UnitTypes.Terran_Supply_Depot.ordinal());
				disposable_min-=100;
				////////System.out.println("Supply depo");
				//debug
				builderDraw=builder.unit;
				buildTileDraw=buildTile;
			}
		}
		return true;
	}
	
	private boolean build(int unitTypeID, Unit cc,Point tile) {
		if ((disposable_min<bwapi.getUnitType(unitTypeID).getMineralPrice())||(disposable_gas<bwapi.getUnitType(unitTypeID).getGasPrice())) {
			disposable_min-=bwapi.getUnitType(unitTypeID).getMineralPrice();
			disposable_gas-=bwapi.getUnitType(unitTypeID).getMineralPrice();
			return false;
		}
		//build at a random base
		////////System.out.println("Trying to build!");
		Random r=new Random();
		Unit randomCC=null;
		if (cc!=null) randomCC=cc; else	randomCC=CC.get(abs(r.nextInt())%CC.size()).unit;
		ExtendedUnit builder=nearestSCV(new Point(randomCC.getX(),randomCC.getY()),true,false);
		//builderDraw=builder.unit;
		Point buildTile = null;
		if (tile!=null) {
			buildTile=tile;
			//this.usedBaseLocations.add(nearestBaseLocationID(new Point(tile.x/32,tile.y/32)));
		}	else buildTile = getBuildTile(builder.unit.getID(), unitTypeID, randomCC.getX(),randomCC.getY());
		if (unitTypeID==UnitTypes.Terran_Refinery.ordinal()) buildTile=getClosestRefineryBuildTile(builder.unit.getID(), unitTypeID, randomCC.getX(),randomCC.getY());
		if ((buildTile.x != -1) && (!queuedBuildings.contains(unitTypeID))) {// && ((!weAreBuilding(unitTypeID))||((unitTypeID==UnitTypes.Terran_Factory.ordinal())&&(!second_fact)))) {
			if ((weAreBuilding(UnitTypes.Terran_Factory.ordinal()))&&(unitTypeID==UnitTypes.Terran_Factory.ordinal())) second_fact=true;
			if (unitTypeID==UnitTypes.Terran_Refinery.ordinal()) building_refinery=true;
			builder.gonnaBuild=unitTypeID;
			builder.whereToBuild=new Point(buildTile.x,buildTile.y);
			builder.commandFrame=bwapi.getFrameCount();
			queuedBuildings.add(unitTypeID);
			disposable_min-=bwapi.getUnitType(unitTypeID).getMineralPrice();
			disposable_gas-=bwapi.getUnitType(unitTypeID).getMineralPrice();
			//////System.out.println("Build time!");
			//debug
			builderDraw=builder.unit;
			buildTileDraw=buildTile;
		}
		return true;
	}
	
	//counts how much cash is not locked up by buildings that are about to be constructed, call before all other training/construction stuff
	private void disposable() {
		disposable_min=me.getMinerals();
		disposable_gas=me.getGas();
		for (Integer id : queuedBuildings) {
			disposable_min-=bwapi.getUnitType(id).getMineralPrice();
			disposable_gas-=bwapi.getUnitType(id).getGasPrice();
		}
	}
	
	//also clears threat and enemy
	private void removeHelp(ArrayList<ExtendedUnit> list) {
		for (int i=0;i<list.size();i++) {
			list.get(i).threat=null;
			list.get(i).enemy=null;
			list.get(i).siegeEnemy=null;
			if (!list.get(i).unit.isExists()) {
				if (list.get(i).constructionPlans!=-1) queuedBuildings.remove(list.get(i).constructionPlans);
				list.remove(i);
			}
			else {
				if (bwapi.getFrameCount()-list.get(i).underAttackFrame>200) list.get(i).isUnderAttack=false; 
				if (list.get(i).previous_hitpoints>list.get(i).unit.getHitPoints()) {
					list.get(i).isUnderAttack=true;
					list.get(i).underAttackFrame=bwapi.getFrameCount();
				}
				list.get(i).previous_hitpoints=list.get(i).unit.getHitPoints();
				int typeID=list.get(i).unit.getTypeID();
				if ((!bwapi.getUnitType(typeID).isBuilding())&&(!bwapi.getUnitType(typeID).isWorker())) {
					if (bwapi.getUnitType(typeID).isCanAttackGround()) myGround+=bwapi.getUnitType(typeID).getSupplyRequired();
					if (bwapi.getUnitType(typeID).isCanAttackAir()) myAir+=bwapi.getUnitType(typeID).getSupplyRequired();
				}
			}
		} 
	}
	
	//also recounts army size and does a coupe more usefull things
	private void removeDead() {
		myGround=0;
		myAir=0;
		for (int i=0;i<CC.size();i++) {
			if (!CC.get(i).unit.isExists()) {
				this.usedBaseLocations.remove(nearestBaseLocationID(new Point(CC.get(i).unit.getInitialX(),CC.get(i).unit.getInitialY())));
				CC.remove(i);
				for (int j=0;j<miners.get(i).size();j++) {
					miners.get(i).get(j).gathering=null;
					bwapi.stop(miners.get(i).get(j).unit.getID());
					//miners.get(i).remove(j);
				}
				//miners.remove(i);
			} else {
				if (bwapi.getFrameCount()-CC.get(i).underAttackFrame>200) CC.get(i).isUnderAttack=false; 
				if (CC.get(i).previous_hitpoints>CC.get(i).unit.getHitPoints()) {
					CC.get(i).isUnderAttack=true;
					CC.get(i).underAttackFrame=bwapi.getFrameCount();
				}
				CC.get(i).previous_hitpoints=CC.get(i).unit.getHitPoints();
			}
		}
		for (int i=0;i<refineries.size();i++) {
			if (!refineries.get(i).unit.isExists()) {
				refineries.remove(i);
				for (int j=0;j<gassers.get(i).size();j++) {
					gassers.get(i).get(j).gathering=null;
					bwapi.stop(gassers.get(i).get(j).unit.getID());
					//gassers.get(i).remove(j);
				}
				//gassers.remove(i);
			} else {
				if (bwapi.getFrameCount()-refineries.get(i).underAttackFrame>200) refineries.get(i).isUnderAttack=false; 
				if (refineries.get(i).previous_hitpoints>refineries.get(i).unit.getHitPoints()) {
					refineries.get(i).isUnderAttack=true;
					refineries.get(i).underAttackFrame=bwapi.getFrameCount();
				}
				refineries.get(i).previous_hitpoints=refineries.get(i).unit.getHitPoints();
			}
		}
		for (int j=0;j<miners.size();j++) {
			removeHelp(miners.get(j));
		}
		for (int j=0;j<gassers.size();j++) {
			removeHelp(gassers.get(j));
		}
		for (int j=0;j<turrets.size();j++) {
			removeHelp(turrets.get(j));
		}
		removeHelp(builders);
		removeHelp(repairers);
		removeHelp(loiterers);
		removeHelp(berserkers);
		removeHelp(unfinished);
		removeHelp(rax);
		removeHelp(factories);
		removeHelp(starports);
		removeHelp(CC);
		removeHelp(satComs);
		removeHelp(refineries);
		removeHelp(research);
		removeHelp(addons);
		removeHelp(rines);
		removeHelp(vult);
		removeHelp(siege);
		removeHelp(goliaths);
		removeHelp(vessels);
		removeHelp(wraiths);
		removeHelp(valkyries);
		removeHelp(bc);
		removeHelp(armories);
	}
	
	public void assessHelp(Unit enemy,ArrayList<ExtendedUnit> list) {
		for (int i=0;i<list.size();i++) {
			if (bwapi.getUnitType(enemy.getTypeID()).isBuilding()) {
				if (bwapi.getUnitType(list.get(i).unit.getTypeID()).isFlyer()) {
					if (inAirExtendedRange(list.get(i).unit,enemy)) {
						list.get(i).threat=enemy;
					}	
				} else {
					if (inExtendedRange(list.get(i).unit,enemy)) {
						list.get(i).threat=enemy;
					}
				}
				if (inRange(enemy,list.get(i).unit)) { //TODO FIX HERE FOR VALKYRIES
					if (list.get(i).enemy!=null) list.get(i).enemy=enemy;
				}
				if (inSiegeRange(enemy,list.get(i).unit)) {
					if (list.get(i).siegeEnemy!=null) list.get(i).siegeEnemy=enemy;
				}
			} else {
				if (bwapi.getUnitType(list.get(i).unit.getTypeID()).isFlyer()) {
					if (inAirExtendedRange(list.get(i).unit,enemy)) {
						if (list.get(i).threat!=null) list.get(i).threat=enemy;
					}	
				} else {
					if (inExtendedRange(list.get(i).unit,enemy)) {
						if (list.get(i).threat!=null) list.get(i).threat=enemy;
					}
				}
				if (inRange(enemy,list.get(i).unit)) {
					list.get(i).enemy=enemy;
				}
				if (inSiegeRange(enemy,list.get(i).unit)) {
					list.get(i).siegeEnemy=enemy;
				}
			}
		}
	}
	
	public void assessEnemy() {
		berserkCount=0;
		whatToBerserk.clear();
		for (Unit eu : bwapi.getEnemyUnits()) {
			//TODO check if near base and send berserkers
			for (int j=0;j<CC.size();j++) {
				if (distance(eu,CC.get(j).unit)<600) {
					whatToBerserk.add(eu);
					if (j!=0) continue; //only home base defended with scvs
					if ((bwapi.getUnitType(eu.getTypeID()).isFlyer())||(bwapi.getUnitType(eu.getTypeID()).isFlyingBuilding())) continue;
					if (bwapi.getUnitType(eu.getTypeID()).isBuilding()) {
						berserkCount+=4;
					} else {
						if (bwapi.getUnitType(eu.getTypeID()).isWorker()) {
							berserkCount++;
						} else {
							berserkCount+=2;
						}
					}
				}
			}
			assessHelp(eu,rines);
			assessHelp(eu,vult);
			assessHelp(eu,siege);
			assessHelp(eu,goliaths);
			assessHelp(eu,vessels);
			assessHelp(eu,wraiths);
			assessHelp(eu,valkyries);
			assessHelp(eu,bc);
		}
	}
	
	public void findDefChokePoint() {
		//check all CCs against first baseTrail
		if (baseTrails.size()==0) return;
		for (int i=0;i<CC.size();i++) {
			ArrayList<Point> trail=null;
			Region enemyBase=null;
			Region myBase=CC.get(0).extendedRegion.region;
			double distance=999999.9;
			ExtendedRegion extendedRegion=null;
			for (int j=0;j<regions.size();j++) {
				if (distance(enemyBases.peekFirst(), new Point(regions.get(j).region.getCenterX(),regions.get(j).region.getCenterY()))<distance) {
					extendedRegion=regions.get(j);
					distance=distance(enemyBases.peekFirst(), new Point(regions.get(j).region.getCenterX(),regions.get(j).region.getCenterY()));
				}
			}
			enemyBase=extendedRegion.region;
			if ((enemyBase!=null)&&(myBase!=null)) {
				trail=BFS.bfs(enemyBase, myBase);
				int newId=defPointID;
				while (newId<baseTrails.get(0).size()-2) {
					boolean ctrl=false;
					for (int j=0;j<trail.size();j++) {
						if (trail.get(j)==baseTrails.get(0).get(newId)) {
							ctrl=true;
							defPointID=newId;
							System.out.println("ID CHANGED");
							break;
						}
					}
					if (ctrl) break; else newId++;
				}
			} else {
				continue;
			}
		}
	}
	
	public Integer initBFS() {
		Region enemyBase=null;
		Region myBase=CC.get(0).extendedRegion.region;
		double distance=999999.9;
		ExtendedRegion extendedRegion=null;
		for (int i=0;i<regions.size();i++) {
			if (distance(enemyBases.peekLast(), new Point(regions.get(i).region.getCenterX(),regions.get(i).region.getCenterY()))<distance) {
				extendedRegion=regions.get(i);
				distance=distance(enemyBases.peekLast(), new Point(regions.get(i).region.getCenterX(),regions.get(i).region.getCenterY()));
			}
		}
		enemyBase=extendedRegion.region;
		if ((enemyBase!=null)&&(myBase!=null)) {
			//////System.out.println("SHOULD CREATE TRAIL");
			baseTrails.add(BFS.bfs(enemyBase, myBase));
		} else {
			//////System.out.println("DEBUG THIS!!K{!}{!!{P!{");
			return null;
		}
		return enemyBase.getID();
	}

    // Returns the id of a unit of a given type, that is closest to a pixel position (x,y), or -1 if we
    // don't have a unit of this type
    public int getNearestUnit(int unitTypeID, int x, int y) {
    	int nearestID = -1;
	    double nearestDist = 9999999;
	    for (Unit unit : bwapi.getMyUnits()) {
	    	if ((unit.getTypeID() != unitTypeID) || (!unit.isCompleted())) continue;
	    	double dist = Math.sqrt(Math.pow(unit.getX() - x, 2) + Math.pow(unit.getY() - y, 2));
	    	if (nearestID == -1 || dist < nearestDist) {
	    		nearestID = unit.getID();
	    		nearestDist = dist;
	    	}
	    }
	    return nearestID;
    }	
	
    public Point getClosestRefineryBuildTile(int builderID, int buildingTypeID, int x, int y) {
    	Point temp=new Point(666666,666666);
		int tileX = x/32; int tileY = y/32;
    	for (Unit n : bwapi.getNeutralUnits()) {
			if ((n.getTypeID() == UnitTypes.Resource_Vespene_Geyser.ordinal()) && ( Math.abs(n.getTileX()-tileX)+( Math.abs(n.getTileY()-tileY)) < (Math.abs(temp.x-tileX)+Math.abs(temp.y-tileY) ))) //we really don't need more precision
					 temp=new Point(n.getTileX(),n.getTileY());
		}
    	if (temp.x==666666) return new Point(-1,-1); else return temp;
    }
    
	// Returns the Point object representing the suitable build tile position
	// for a given building type near specified pixel position (or Point(-1,-1) if not found)
	// (builderID should be our worker)
	public Point getBuildTile(int builderID, int buildingTypeID, int x, int y) {
		Point ret = new Point(-1, -1);
		int maxDist = 3;
		int stopDist = 50;
		int tileX = x/32; int tileY = y/32;
		
		// Refinery, Assimilator, Extractor
		if (bwapi.getUnitType(buildingTypeID).isRefinery()) {
			stopDist=40;
			for (Unit n : bwapi.getNeutralUnits()) {
				if ((n.getTypeID() == UnitTypes.Resource_Vespene_Geyser.ordinal()) && 
						( Math.abs(n.getTileX()-tileX) < stopDist ) &&
						( Math.abs(n.getTileY()-tileY) < stopDist )
						) return new Point(n.getTileX(),n.getTileY());
			}
		}
		
		while ((maxDist < stopDist) && (ret.x == -1)) {
			for (int i=tileX-maxDist; i<=tileX+maxDist; i++) {
				for (int j=tileY-maxDist; j<=tileY+maxDist; j++) {
					if ((buildingTypeID==UnitTypes.Terran_Factory.ordinal())||(buildingTypeID==UnitTypes.Terran_Starport.ordinal())||(buildingTypeID==UnitTypes.Terran_Science_Facility.ordinal())) {
						////////System.out.println("BUILDING FACT/PORT/SCIENCE");
						if (!bwapi.canBuildHere(builderID, i+4, j+2, UnitTypes.Terran_Machine_Shop.ordinal(), false)) continue;
					}
					if (bwapi.canBuildHere(builderID, i, j, buildingTypeID, false)) {
						// units that are blocking the tile
						boolean unitsInWay = false;
						for (Unit u : bwapi.getAllUnits()) {
							if (u.getID() == builderID) continue;
							if ((Math.abs(u.getTileX()-i) < 4) && (Math.abs(u.getTileY()-j) < 4)) unitsInWay = true;
						}
						if (!unitsInWay) {
							ret.x = i; ret.y = j;
							return ret;
						}
					}
				}
			}
			maxDist += 2;
		}
		
		//if (ret.x == -1) bwapi.printText("Unable to find suitable build position for "+bwapi.getUnitType(buildingTypeID).getName());
		return ret;
	}
	
	// Returns true if we are currently constructing the building of a given type.
	public boolean weAreBuilding(int buildingTypeID) {
		for (Unit unit : bwapi.getMyUnits()) {
			if ((unit.getTypeID() == buildingTypeID) && (!unit.isCompleted())) return true;
			if (bwapi.getUnitType(unit.getTypeID()).isWorker() && unit.getConstructingTypeID() == buildingTypeID) return true;
		}
		return false;
	}
	
	// Draws debug information on the screen. 
	// Reimplement this function however you want. 
	public void drawDebugInfo() {

		// Draw our home position.
		bwapi.drawText(new Point(5,0), "Our home position: "+String.valueOf(homePositionX)+","+String.valueOf(homePositionY), true);
		bwapi.drawText(new Point(5,15), "Frame count: "+String.valueOf(bwapi.getFrameCount()), true);
		bwapi.drawText(new Point(5,30), "MyGround: "+String.valueOf(myGround), true);
		bwapi.drawText(new Point(5,45), "MyAir: "+String.valueOf(myAir), true);
		bwapi.drawText(new Point(5,60), "EnemyGround: "+String.valueOf(enemyGround), true);
		bwapi.drawText(new Point(5,75), "EnemyAir: "+String.valueOf(enemyAir), true);
		if (this.pump_production) bwapi.drawText(new Point(5,150), "PUMP PRODUCTION", true); else bwapi.drawText(new Point(5,150), "MACRO", true); 
		
		// Draw circles over workers (blue if they're gathering minerals, green if gas, yellow if they're constructing).
		for (Unit u : bwapi.getMyUnits())  {
			if (u.isGatheringMinerals()) bwapi.drawCircle(u.getX(), u.getY(), 12, BWColor.BLUE, false, false);
			else if (u.isGatheringGas()) bwapi.drawCircle(u.getX(), u.getY(), 12, BWColor.GREEN, false, false);
		}
		
		/*for (int i=0;i<bwapi.getMap().getWalkHeight();i+=10) {
			for (int j=0;j<bwapi.getMap().getWalkWidth();j+=10) {
				if (bwapi.getMap().isWalkable(j, i)&&getExtendedRegion(CC.get(0).regionID).pointInside(j, i)) {
					bwapi.drawCircle(j*8,i*8, 3, BWColor.TEAL, false, false);
				}
			}
		}*/
		
		for (int k=0;k<regions.size();k++) {
			for (int j=0;j<regions.get(k).coordinates.size();j++) {
				bwapi.drawCircle(regions.get(k).coordinates.get(j).x,regions.get(k).coordinates.get(j).y, 3, BWColor.BLUE, false, false);
			}
			for (int j=0;j<regions.get(k).innerCoordinates.size();j++) {
				switch(k) {
				case 1:
					bwapi.drawCircle(regions.get(k).innerCoordinates.get(j).x,regions.get(k).innerCoordinates.get(j).y, 3, BWColor.CYAN, false, false);
					break;
				case 2:
					bwapi.drawCircle(regions.get(k).innerCoordinates.get(j).x,regions.get(k).innerCoordinates.get(j).y, 3, BWColor.BROWN, false, false);
					break;
				case 3:
					bwapi.drawCircle(regions.get(k).innerCoordinates.get(j).x,regions.get(k).innerCoordinates.get(j).y, 3, BWColor.ORANGE, false, false);
					break;
				case 4:
					bwapi.drawCircle(regions.get(k).innerCoordinates.get(j).x,regions.get(k).innerCoordinates.get(j).y, 3, BWColor.PURPLE, false, false);
					break;
				case 5:
					bwapi.drawCircle(regions.get(k).innerCoordinates.get(j).x,regions.get(k).innerCoordinates.get(j).y, 3, BWColor.RED, false, false);
					break;
				case 6:
					bwapi.drawCircle(regions.get(k).innerCoordinates.get(j).x,regions.get(k).innerCoordinates.get(j).y, 3, BWColor.WHITE, false, false);
					break;
				case 7:
					bwapi.drawCircle(regions.get(k).innerCoordinates.get(j).x,regions.get(k).innerCoordinates.get(j).y, 3, BWColor.YELLOW, false, false);
					break;
				default:
					bwapi.drawCircle(regions.get(k).innerCoordinates.get(j).x,regions.get(k).innerCoordinates.get(j).y, 3, BWColor.GREEN, false, false);
					break;
				}
			}
		}
		
		for (int i=0;i<baseTrails.size();i++) {
			bwapi.drawCircle(baseTrails.get(i).get(this.defPointID).x, baseTrails.get(i).get(this.defPointID).y, 16, BWColor.BLUE, true, false);
			for (int j=1;j<baseTrails.get(i).size();j++) {
				bwapi.drawLine(baseTrails.get(i).get(j-1).x, baseTrails.get(i).get(j-1).y, baseTrails.get(i).get(j).x, baseTrails.get(i).get(j).y, BWColor.YELLOW, false);
				bwapi.drawCircle(baseTrails.get(i).get(j).x, baseTrails.get(i).get(j).y, 14, BWColor.ORANGE, true, false);
			}
		}
		
		for (Unit unit : bwapi.getMyUnits()) {
			bwapi.drawText(unit.getX(), unit.getY(), bwapi.getOrderType(unit.getOrderID()).getName(), false);
		}
		
		for (int k=0;k<bwapi.getMap().getRegions().size();k++) {
			int[] array=bwapi.getMap().getRegions().get(k).getCoordinates();
			for (int j=0;j<array.length;j+=2) {
				bwapi.drawCircle(array[j], array[j+1], 3, BWColor.BLUE, false, false);
			}
		}
		for (int k=0;k<bwapi.getMap().getBaseLocations().size();k++) {
			bwapi.drawCircle(bwapi.getMap().getBaseLocations().get(k).getX(), bwapi.getMap().getBaseLocations().get(k).getY(), 12, BWColor.GREEN, false, false);
			bwapi.drawCircle(bwapi.getMap().getBaseLocations().get(k).getTx(), bwapi.getMap().getBaseLocations().get(k).getTy(), 12, BWColor.YELLOW, false, false); //YAY! ^^
			//bwapi.drawCircle(bwapi.getUnit(bwapi.getMap().getBaseLocations().get(k).getGas()).getX(),bwapi.getUnit(bwapi.getMap().getBaseLocations().get(k).getGas()).getY(), 12, BWColor.BLUE, false, false);
		}
		if (builderDraw!=null) {
			bwapi.drawCircle(builderDraw.getX(), builderDraw.getY(), 14, BWColor.PURPLE, true, false);
		}
		if (buildTileDraw!=null) {
			bwapi.drawCircle(buildTileDraw.x*32, buildTileDraw.y*32, 14, BWColor.PURPLE, true, false);
		}
		
		for (int k=0;k<miners.size();k++) {
			for (int j=0;j<miners.get(k).size();j++) {
				if (miners.get(k).get(j).buildRepair!=null) {
					bwapi.drawCircle(miners.get(k).get(j).unit.getX(), miners.get(k).get(j).unit.getY(), 14, BWColor.RED, true, false);
					////////System.out.println("................buildRepair");
				}
				if (miners.get(k).get(j).gonnaBuild!=-1) {
					bwapi.drawCircle(miners.get(k).get(j).unit.getX(), miners.get(k).get(j).unit.getY(), 14, BWColor.ORANGE, true, false);
					////////System.out.println("................gonnaBuild");
				}
			}
		}
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
	
	public boolean berserkContains(int unitID) {
		for (int k=0;k<berserkers.size();k++) {
			if (berserkers.get(k).unit.getID()==unitID) return true; 
		}
		return false;
	}
	
	public ExtendedUnit nearestSCV(Unit unit, boolean check_miners, boolean check_repairers) {
		ExtendedUnit closest=null;
		double closestDist = 99999999;
		//miners checked only for buildings, repairing of units done only by repairers, option to not allow repairers (probably useless in this code)
		if (check_repairers) {
			for (int i=0;i<repairers.size();i++) {
				if (unit.getID()==repairers.get(i).unit.getID()) continue;
				double distance = Math.sqrt(Math.pow(repairers.get(i).unit.getX() - unit.getX(), 2) + Math.pow(repairers.get(i).unit.getY() - unit.getY(), 2));
				if (((closest == null) || (distance < closestDist))&&(repairers.get(i).buildRepair==null)&&(repairers.get(i).gonnaBuild==-1)&&(!repairers.get(i).unit.isConstructing())) {
					closestDist = distance;
					closest = repairers.get(i);
				}
			}
		}
		if (check_miners) {
			for (int i=0;i<miners.size();i++) {
				for (int j=0;j<miners.get(i).size();j++) {
					if (unit.getID()==miners.get(i).get(j).unit.getID()) continue;
					double distance = Math.sqrt(Math.pow(miners.get(i).get(j).unit.getX() - unit.getX(), 2) + Math.pow(miners.get(i).get(j).unit.getY() - unit.getY(), 2));
					if (((closest == null) || (distance < closestDist))&&(miners.get(i).get(j).buildRepair==null)&&(miners.get(i).get(j).gonnaBuild==-1)&&(!miners.get(i).get(j).unit.isConstructing())) {
						//check berserkers
						if (berserkContains(miners.get(i).get(j).unit.getID())) continue;
						closestDist = distance;
						closest = miners.get(i).get(j);
					}
				}
			} 
		}
		return closest;
	}
	
	public ExtendedUnit nearestSCV(Point p, boolean check_miners, boolean check_repairers) {
		ExtendedUnit closest=null;
		double closestDist = 99999999;
		//miners checked only for buildings, repairing of units done only by repairers, option to not allow repairers (probably useless in this code)
		if (check_repairers) {
			for (int i=0;i<repairers.size();i++) {
				double distance = Math.sqrt(Math.pow(repairers.get(i).unit.getX() - p.getX(), 2) + Math.pow(repairers.get(i).unit.getY() - p.getY(), 2));
				if (((closest == null) || (distance < closestDist))&&(repairers.get(i).buildRepair==null)) {
					closestDist = distance;
					closest = repairers.get(i);
				}
			}
		}
		if (check_miners) {
			for (int i=0;i<miners.size();i++) {
				for (int j=0;j<miners.get(i).size();j++) {
					double distance = Math.sqrt(Math.pow(miners.get(i).get(j).unit.getX() - p.getX(), 2) + Math.pow(miners.get(i).get(j).unit.getY() - p.getY(), 2));
					if (((closest == null) || (distance < closestDist))&&(miners.get(i).get(j).buildRepair==null)) {
						closestDist = distance;
						closest = miners.get(i).get(j);
					}
				}
			} 
		}
		return closest;
	}
	
	public int nearestTrailPoint(Unit unit,ArrayList<Point> baseTrail) {
		double distance=9999999.9;
		int result=-1;
		for (int i=0;i<baseTrail.size();i++) {
			Point p=baseTrail.get(i);
			if (distance(unit,p)<distance) {
				distance=distance(unit,p);
				result=i;
			}
		}
		return result;
	}
	
	public int nearestBaseLocationID(Point p) {
		double distance=9999999.9;
		int result=-1;
		for (BaseLocation base : bwapi.getMap().getBaseLocations()) {
			if (distance(p,new Point(base.getX(),base.getY()))<distance) {
				distance=distance(p,new Point(base.getX(),base.getY()));
				result=base.getRegionID();
			}
		}
		return result;
	}
	
	public boolean inMyResourceGroups(Unit gas) {
		for (int i=0;i<CC.size();i++) {
			if (CC.get(i).closest_mineral.getResourceGroup()==gas.getResourceGroup()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean inExtendedRange(Unit what,Unit who) {
		if (distance(what,who)<=(bwapi.getWeaponType(bwapi.getUnitType(who.getTypeID()).getGroundWeaponID()).getMaxRange()+32+70)) return true;
		return false;
	}
	
	public boolean inAirExtendedRange(Unit what,Unit who) {
		if (distance(what,who)<=(bwapi.getWeaponType(bwapi.getUnitType(who.getTypeID()).getAirWeaponID()).getMaxRange()+32+70)) return true;
		return false;
	}
	
	public boolean inRange(Unit what,Unit who) {
		if (distance(what,who)<=bwapi.getWeaponType(bwapi.getUnitType(who.getTypeID()).getGroundWeaponID()).getMaxRange()+32) return true;
		return false;
	}
	
	public boolean inSiegeRange(Unit what,Unit who) {
		if ((distance(what,who)<=bwapi.getWeaponType(bwapi.getUnitType(UnitTypes.Terran_Siege_Tank_Siege_Mode.ordinal()).getGroundWeaponID()).getMaxRange()+32)&&(distance(what,who)>bwapi.getWeaponType(bwapi.getUnitType(UnitTypes.Terran_Siege_Tank_Siege_Mode.ordinal()).getGroundWeaponID()).getMinRange())) {
			return true;
		}
		return false;
	}
	
	public double distance(Unit x, Unit y) {
		return Math.sqrt(Math.pow(x.getX() - y.getX(), 2) + Math.pow(x.getY() - y.getY(), 2));
	}
	public double distance(Unit x, Point y) {
		return Math.sqrt(Math.pow(x.getX() - y.x, 2) + Math.pow(x.getY() - y.y, 2));
	}
	
	public double distance(Point x, Point y) {
		return Math.sqrt(Math.pow(x.x - y.x, 2) + Math.pow(x.y - y.y, 2));
	}
	
	public boolean nearEnemyBase(Point point) {
		for (Point p : enemyBases) {
			if (distance(p,point)<400.0) {
				return true;
			}
		}
		return false;
	}
	
	//access functions for bwapi
	public ArrayList<Unit> getNeutralUnits() {
		return bwapi.getNeutralUnits();
	}
	
	public void gather(int who, int what) {
		bwapi.gather(who, what);
	}
	
	public void attack(int who, int what) {
		bwapi.attack(who, what);
	}
	public void attack(int who, int x,int y) {
		bwapi.attack(who, x,y);
	}
	
	public void rightClick(int who, int target) {
		bwapi.rightClick(who, target);
	}
	
	public UnitType getUnitType(int id) {
		return bwapi.getUnitType(id);
	}
	
	public int getMaxHitPoints(int unitID) {
		return bwapi.getUnitType(unitID).getMaxHitPoints();
	}
	
	public int getFrameCount() {
		return bwapi.getFrameCount();
	}
	
	public void build(int unitID, int tx, int ty, int typeID) {
		bwapi.build(unitID, tx, ty, typeID);
	}
	
	public void move(int unitID,int x,int y) {
		bwapi.move(unitID, x, y);
	}
	
	public void setRallyPoint(int who,int where) {
		bwapi.setRallyPoint(who, where);
	}
	
	public void print(String s) {
		bwapi.printText(s);
	}
	
	//custom BWTA functions (writing here instead of changing map/region for easier project evaluation ... although you probably already hate me for 1000+ lines of code in a single file.. Sorry! :)
	public int whichRegion(Point p) {
		for (int i=0;i<regions.size();i++) {
			if (regions.get(i).pointInside(p.x, p.y)) return i;
		}
		return -1;
	}
	
	public ExtendedRegion getExtendedRegion(int regionID) {
		for (int i=0;i<regions.size();i++) {
			if (regions.get(i).region.getID()==regionID) return regions.get(i); 
		}
		return null;
	}
	
	//utility
	public int abs(int x) {
		if (x<0) return x*-1; else return x;
	}
}
