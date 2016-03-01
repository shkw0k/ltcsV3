package models;

public class CollisionResult {
	
	public PointingInfo telescope;
	public double collisionDist;
	
	public CollisionResult (PointingInfo tel, double colDist) {
		telescope = tel;
		collisionDist = colDist;
	}	
}
