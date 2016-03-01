package utils;

public class Vector {

	static public Vector azelToVector (double azRad, double elRad) {
		double cosEl = Math.cos(elRad);
		double sinEl = Math.sin(elRad);
		double cosAz = Math.cos(azRad);
		double sinAz = Math.sin(azRad);
		return new Vector (cosEl * sinAz, cosEl * cosAz, sinEl);
	}	

	static public double dist2P (Vector a, Vector b) {
		return a.subtract(b).length();
	}

	static public double distance (Vector a, Vector pa, Vector b, Vector pb) {
		Vector bdiff = pb.subtract(pa);
		Vector c = a.cross(b);
		//System.out.println ("c len " + c.length());
		return c.dot(bdiff) / c.length();
	}
	
	static public Vector getPosition (Vector base, Vector v, double lambda) {
		return v.scale(lambda).add(base);				
	}

	static public Vector[] intersect(Vector a, Vector pa, Vector b, Vector pb) {
		double [] parms = Vector.intersectParms(a, pa, b, pb);
		Vector res [] = new Vector[] {Vector.getPosition(pa, a, parms[0]),
			Vector.getPosition(pb, b, parms[1])};		
		return res;		
	}
	
	static public double[] intersectParms(Vector a, Vector pa, Vector b, Vector pb) {
		double[][] cov = a.coVarMatrix(a, b);
		double[][] inv = Vector.inverse(cov);
		Vector bdiff = pb.subtract(pa);
		return Vector.mult(inv, Vector.mult(a, b.negavtive(), bdiff));
	}

	static public double[][] inverse(double[][] mat) {
		double res[][] = new double[][] { { 0, 0 }, { 0, 0 } };
		double det = mat[0][0] * mat[1][1] - mat[0][1] * mat[1][0];
		if (det == 0) {
			return res;
		}
		res[0][0] = mat[1][1] / det;
		res[0][1] = -mat[1][0] / det;
		res[1][0] = -mat[0][1] / det;
		res[1][1] = mat[0][0] / det;
		return res;
	}
	
	// **********************************************************

	static public void main(String args[]) {
		Vector a = new Vector(1, 0, 0);
		Vector b = new Vector(0, 1, 1);
		Vector pa = new Vector(0, 0, 40);
		Vector pb = new Vector(0, 0, 0);

		double[] res = Vector.intersectParms(a, pa, b, pb);
		System.out.println("res " + res[0] + " " + res[1]);
		
		Vector v1 = Vector.getPosition(pa, a, res[0]);
		Vector v2 = Vector.getPosition(pb, b, res[1]);
		
		System.out.println("dist " + Vector.distance(a, pa, b, pb));
		System.out.println("dist2P " + Vector.dist2P(v1, v2));		
	}	

	static public double[] mult(double[][] mat, double[] v) {
		double[] res = new double[2];
		res[0] = mat[0][0] * v[0] + mat[1][0] * v[1];
		res[1] = mat[0][1] * v[0] + mat[1][1] * v[1];
		return res;
	}

	static public double[] mult(Vector x1, Vector x2, Vector b) {
		double[] res = new double[2];
		res[0] = x1.dot(b);
		res[1] = x2.dot(b);
		return res;
	}
	
	static public double[][] mult2x2(double[][] mat1, double[][] mat2) {
		double res[][] = new double[2][2];
		res[0][0] = mat1[0][0] * mat2[0][0] + mat1[1][0] * mat2[0][1];
		res[0][1] = mat1[0][1] * mat2[0][0] + mat1[1][1] * mat2[0][1];
		res[1][0] = mat1[0][0] * mat2[1][0] + mat1[1][0] * mat2[1][1];
		res[1][1] = mat1[0][1] * mat2[1][0] + mat1[1][1] * mat2[1][1];
		return res;
	}

	/*****************************************************************/
	
	public double x, y, z;

	public Vector(double x1, double y1, double z1) {
		x = x1;
		y = y1;
		z = z1;
	}

	public Vector add(double x1, double y1, double z1) {
		return new Vector(x + x1, y + y1, z + z1);
	}
	
	public Vector add(Vector b) {
		return new Vector(x + b.x, y + b.y, z + b.z);
	}

	public double[][] coVarMatrix(Vector a, Vector b) {
		double res[][] = new double[2][2];
		res[0][0] = a.dot(a);
		res[0][1] = res[1][0] = -a.dot(b);
		res[1][1] = b.dot(b);
		return res;
	}

	public Vector cross(Vector b) {
		return new Vector(y * b.z - z * b.y, -x * b.z + z * b.x, x * b.y - y
				* b.x);
	}

	public double dot(double x1, double y1, double z1) {
		return x * x1 + y * y1 + z * z1;
	}

	public double dot(Vector b) {
		return x * b.x + y * b.y + z * b.z;
	}

	public double length() {
		return Math.sqrt(x*x+y*y+z*z);
	}

	public Vector negavtive() {
		return new Vector(-x, -y, -z);
	}
	
	public Vector normalized() {
		return scale(1.0/length());
	}

	public Vector scale (double lambda) {
		return new Vector (x*lambda, y*lambda, z*lambda);
	}
	
	public Vector subtract(double x1, double y1, double z1) {
		return new Vector(x - x1, y - y1, z - z1);
	}

	public Vector subtract(Vector b) {
		return new Vector(x - b.x, y - b.y, z - b.z);
	}
	
	public double toAz () {
		return Math.atan2(x, y);
	}
	
	public double toEl () {
		return Math.asin(z);
	}
	
	public String toString () {
		return String.format("%.2f, %.2f, %.2f", x, y, z);				
	}
	
	public String toString (double[][] mat) {
		return String.format("%.2f %.2f\n%.2f %.2f\n", mat[0][0],
				mat[1][0], mat[1][0], mat[1][1]);
	}	
}
