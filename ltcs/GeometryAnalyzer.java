package ltcs;

import java.util.ArrayList;
import java.util.TreeMap;

import utils.Angle;
import utils.StarUtils;
import utils.Vector;
import models.ChangedEvent;
import models.ChangedEvents;
import models.CollisionResult;
import models.PointingInfo;
import models.SiteInfo;

public class GeometryAnalyzer extends Thread {

	private TreeMap<String, PntInfoReader> pReaders;

	/**
	 * Geometry Analyzer runs as a thread that checks for collisions amongst telescopes.
	 * @param pReaders
	 */
	public GeometryAnalyzer(TreeMap<String, PntInfoReader> pReaders) {
		this.pReaders = pReaders;
		setDaemon(true);
	}

	/**
	 * Checks for collision between tel1 and tel2.
	 * This is a static calculation for one time instance.
	 * 
	 * Results, if there is a collision, are stored in collisions list in the pntInfo 
	 * if the telescope has a laser.
	 * 
	 * @param tel1
	 * @param tel2
	 */
	private void calcCollistion(PntInfoReader tel1, PntInfoReader tel2) {
		SiteInfo site1 = tel1.getSiteInfo();
		SiteInfo site2 = tel2.getSiteInfo();
		PointingInfo pInfo1 = tel1.getPntInfo(false);
		PointingInfo pInfo2 = tel2.getPntInfo(false);

		updatePntInfo(pInfo1, site1);
		updatePntInfo(pInfo2, site2);
		Vector tel1Base = site1.getVector();
		Vector tel2Base = site2.getVector();
		Vector tel1PntDir = getPntDirection(pInfo1);
		Vector tel2PntDir = getPntDirection(pInfo2);

		double parms[] = Vector.intersectParms(tel1PntDir, tel1Base,
				tel2PntDir, tel2Base);

		if (parms[0] > 0 && parms[1] > 0) {
			// possible collision
			Vector pnt1 = Vector.getPosition(tel1Base, tel1PntDir, parms[0]);
			Vector pnt2 = Vector.getPosition(tel2Base, tel2PntDir, parms[1]);
			double dist2P = Vector.dist2P(pnt1, pnt2);

			// check if dist2P is larger than the same of the two radii.
			if (checkDistanceOK(pInfo1, pInfo2, parms[0], parms[1], dist2P))
				return;

			if (site1.hasLaser())
				pInfo1.newCollisions.add(new CollisionResult(pInfo2, parms[0]));
			if (site2.hasLaser())
				pInfo2.newCollisions.add(new CollisionResult(pInfo1, parms[1]));

			System.out.println(String.format("dist2P= %.2f", dist2P));
			System.out.println("tel1 " + tel1PntDir.toString() + " base "
					+ tel1Base.toString() + " az " + pInfo1.azimuth.getDegree()
					+ " " + pInfo1.elevation.getDegree());
			System.out.println("tel2 " + tel2PntDir.toString() + " base "
					+ tel2Base.toString() + " az " + pInfo2.azimuth.getDegree()
					+ " " + pInfo2.elevation.getDegree());
			System.out.println("parms " + parms[0] + " " + parms[1]);
		}
		// double coneWdith = Math.toRadians(pInfo1.fov);
	}

	/**
	 * Check collision for all pair of sites.
	 * Results are saved in the pntReaders, first in the newCollisions.
	 * Then results are copied the collision list.
	 * 
	 * calcCurrCollisions is called when pointing of any site changed or at least every 10s.
	 * This the current collision state of all the sites.
	 * This is not the prediction of future collisions. 
	 * 
	 */
	public void calcCurrCollisions() {
		int i, j;
		PntInfoReader tel1, tel2;
		boolean hasLaser1, hasLaser2;
		ArrayList<PntInfoReader> sList = new ArrayList<PntInfoReader>(
				pReaders.values());
		int len = sList.size();

		// For each site clear the collision list. 
		for (i = 0; i < len; ++i) {
			tel1 = sList.get(i);
			PointingInfo pInfo = tel1.getPntInfo(false);
			pInfo.newCollisions = new java.util.Vector<CollisionResult>();
		}
		
		// Check collision for all pairs of sites
		for (i = 0; i < len; ++i) {
			tel1 = sList.get(i);
			hasLaser1 = tel1.getSiteInfo().hasLaser();

			for (j = i + 1; j < len; ++j) {
				tel2 = sList.get(j);
				hasLaser2 = tel2.getSiteInfo().hasLaser();
				if (hasLaser1 || hasLaser2) {
					calcCollistion(tel1, tel2);
				}
			}
		}
		
		// Make new collisions available
		for (i = 0; i < len; ++i) {
			tel1 = sList.get(i);
			PointingInfo pInfo = tel1.getPntInfo(false);
			pInfo.collisions = pInfo.newCollisions;
		}
	}

	/**
	 * Checks distance between the beams. Compares with the given FOV of the telescopes. 
	 * 
	 * There are difference types of beams: cylindrical and conical.
	 * 
	 * @param pInfo1
	 * @param pInfo2
	 * @param d1
	 * @param d2
	 * @param dist2p
	 * @return
	 */
	private boolean checkDistanceOK(PointingInfo pInfo1, PointingInfo pInfo2,
			double d1, double d2, double dist2p) {
		double fov1 = pInfo1.overrideFields ? pInfo1.ovrfov : pInfo1.fov;
		double fov2 = pInfo2.overrideFields ? pInfo2.ovrfov : pInfo2.fov;
		
		double dist1 = Math.sin(Math.toRadians(fov1)) * d1;
		double dist2 = Math.sin(Math.toRadians(fov2)) * d2;
		return dist2p > (dist1+dist2);
	}

	public double[] findCollision(String pointingTel, String targetTel, double distance) {		
		PntInfoReader pr1 = pReaders.get(pointingTel);
		SiteInfo pointingTelInfo = pr1.getSiteInfo();
		
		PntInfoReader pr2 = pReaders.get(targetTel);
		PointingInfo targetTelPntInfo = pr2.getPntInfo(false);
		SiteInfo targetTelSiteInfo = pr2.getSiteInfo();
		updatePntInfo(targetTelPntInfo, targetTelSiteInfo);
		
		Vector direction = getPntDirection(targetTelPntInfo);
		Vector collisionPoint = direction.scale(distance).add(targetTelSiteInfo.getVector());
		Vector aimVector = collisionPoint.subtract(pointingTelInfo.getVector()).normalized();
		
//		System.out.println("Target " + targetTelPntInfo.azimuth + " " + targetTelPntInfo.elevation);
//		System.out.println("Pointing base " + pointingTelInfo.getVector());
//		
//		System.out.println("direction " + direction);		
//		System.out.println("Col Pnt " + collisionPoint);
//		System.out.println("AimVector " + aimVector);
//		System.out.println();
		
		return new double[] {Math.toDegrees(aimVector.toAz()), Math.toDegrees(aimVector.toEl())};
	}

	
	private Vector getPntDirection(PointingInfo pInfo) {
		return Vector.azelToVector(pInfo.azimuth.getRadian(),
				pInfo.elevation.getRadian());
	}

	/**
	 * Loops forever, waits for a change event, then check collision.
	 * The event is used only for synchronization. It has no content.
	 *  
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		ChangedEvents cev = LTCSServer.changedEvents;
		while (true) {
			ChangedEvent ev = cev.pop();
			calcCurrCollisions();
		}
	}

	/**
	 * Converts RA/DEC to AZ/EL and saves them back in PointingInfo.
	 *  
	 * @param pInfo
	 * @param site
	 */
	private void updatePntInfo(PointingInfo pInfo, SiteInfo site) {
		double raHour = pInfo.ra.getDegree(); // hours
		double decDeg = pInfo.dec.getDegree();

		double raDeg = raHour * 15;
		// System.out.println ("updatePntInfo ");
		// System.out.println
		// (String.format("raDeg %.2f,  decDeg %.2f, lat %.2f long %.2f",
		// raDeg, decDeg, site.latitude, site.longitude));
		double azel[] = StarUtils.raDec2AzEl(raDeg, decDeg, site.latitude,
				site.longitude);
		pInfo.azimuth = Angle.asDegree(azel[0]);
		pInfo.elevation = Angle.asDegree(azel[1]);
	}
}
