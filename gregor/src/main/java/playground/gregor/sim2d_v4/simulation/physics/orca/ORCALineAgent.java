/* *********************************************************************** *
 * project: org.matsim.*
 * ORCAabTau.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.gregor.sim2d_v4.simulation.physics.orca;

import org.matsim.core.utils.collections.Tuple;

import playground.gregor.sim2d_v4.debugger.VisDebugger;
import playground.gregor.sim2d_v4.simulation.physics.ORCAAgent;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;

@Deprecated
public class ORCALineAgent implements ORCALine {


	private float directionX;
	private float directionY;
	private float pointX;
	private float pointY;
	private int sign = 1;

	
	public ORCALineAgent(ORCAAgent orcaAgent, Tuple<Float,Sim2DAgent> neighbor, float tau) {
		construct(orcaAgent,neighbor,tau);
	}

	private void construct(ORCAAgent a, Tuple<Float,Sim2DAgent> neighbor, float tau) {

		final float sqrDist = neighbor.getFirst();
		Sim2DAgent b = neighbor.getSecond();

		final float[] aPos = a.getPos();
		final float[] bPos = b.getPos();
		final float[] aV = a.getVelocity();
		final float[] bV = b.getVelocity();
//		Coordinate pA = orcaAgent.getPosition();
//		Coordinate pB = neighbor.getPosition();

		//+(MatsimRandom.getRandom().nextDouble()-0.5)/10
		
		// 1. construct VO_{A|B}^\tau
		float xpBpA = bPos[0]-aPos[0];
		float ypBpA = bPos[1]-aPos[1];
		float rAB = a.getRadius()+b.getRadius();
		
		
		

		if (sqrDist < rAB) {//collision! construct ORCA Line so, that the collision is resolved in the next time step  
			float norm = (float) Math.sqrt(sqrDist);
			float moveHalfe = 40*(rAB-norm)/2;
			
			
			this.directionX = - ypBpA/norm;
			this.directionY = xpBpA/norm;
			this.pointX = aV[0] - xpBpA*moveHalfe;
			this.pointY = aV[1] - ypBpA*moveHalfe;
			
			return;
		}
		
		final float [] tangents = new float[4]; //left x, left y, right x, right y
		computeVOabTangents(xpBpA,ypBpA,rAB,tangents);
		
		//DEBUG
//		GuiDebugger.addVector(new float []{0, 0, tangents[0], tangents[1],128,128,128});
//		GuiDebugger.addVector(new float []{0, 0, tangents[2], tangents[3],128,128,128});
//		GuiDebugger.addVector(0, 0, xpBpA,ypBpA);
		

		float tauX = (xpBpA)/tau;
		float tauY = (ypBpA)/tau;
		float tauR = rAB/tau;
		
//		GuiDebugger.addCircle(new float []{tauX,tauY,tauR,128,128,128});
		


		float tauXleft = tangents[0]/tau;
		float tauYleft = tangents[1]/tau;
		float tauXright = tangents[2]/tau;
		float tauYright = tangents[3]/tau;		


//		float vxA = (float) orcaAgent.getVx();
//		float vyA = (float) orcaAgent.getVy();
//
//		float vxB = (float) neighbor.getVx();
//		float vyB = (float) neighbor.getVy();

		float vxAvxB = aV[0] - bV[0];
		float vyAvyB = aV[1] - bV[1];

		//2. figure out what the responsible component is
		//tau circle is responsible if v_A - v_B is right of line tauLeft ---> tauRight
		if (isLeftOfLine(vxAvxB, vyAvyB, tauXleft, tauYleft, tauXright, tauYright) <= 0) {
			computeVectorUForCircle(tauX, tauY, tauR,vxAvxB,vyAvyB);
			
		}else {
			//construct centerline of VO cone
			float VOcenterX = tangents[0]+tangents[2];
			float VOcenterY = tangents[1]+tangents[3];
			
			if (isLeftOfLine(vxAvxB, vyAvyB, 0, 0, VOcenterX, VOcenterY) > 0){//left side
				computeVectorUForTangent(tangents[0],tangents[1],vxAvxB,vyAvyB);
				if (isLeftOfLine(vxAvxB, vyAvyB, 0, 0, tangents[0], tangents[1]) > 0) {
					this.sign  = -1;
				}
				
			} else {//right side
				computeVectorUForTangent(tangents[2],tangents[3],vxAvxB,vyAvyB);
				if (isLeftOfLine(vxAvxB, vyAvyB, 0, 0, tangents[2], tangents[3]) < 0) {
					this.sign  = -1;
				}
			}
		}


		calcORCA(aV[0],aV[1]);
		
	}

	
	private void calcORCA(float vxA, float vyA) {
		this.setPointX(vxA + this.getDirectionX()/2);
		this.setPointY(vyA + this.getDirectionY()/2);
		float n = norm(this);
		multiply(this, this.sign/n);
		float tmp = this.getDirectionX();
		this.setDirectionX(this.getDirectionY());
		this.setDirectionY(-tmp);
		
	}


	private void computeVectorUForTangent(float vx, float vy,float vxAvxB, float vyAvyB) {
		
		
		float c1 = dot (vxAvxB,vyAvyB,vx,vy);
		float c2= dot (vx,vy,vx,vy); //Check!! c2 = sqrt(c2)??
		float b = c1 /c2;
		float x = b * vx;
		float y = b * vy;
		this.setDirectionX(x - vxAvxB);
		this.setDirectionY(y - vyAvyB);
		
	}



	private void computeVectorUForCircle(float tauX, float tauY, float tauR, float vxAvxB, float vyAvyB) {
		float x = tauX - vxAvxB;
		float y = tauY - vyAvyB;
		//length of vector u
		float norm = (float) Math.sqrt(x*x+y*y);
		
		this.setDirectionX((x / norm) * (norm - tauR));
		this.setDirectionY((y / norm) * (norm - tauR));
		
		if (norm > tauR) {
			this.sign = -1;
		}
	}

	private void computeVOabTangents(float xpApB, float ypApB, float rAB, float[] tangents) {

		float dx = -xpApB/2;
		float dy = -ypApB/2;

		float d = (float) Math.sqrt(dx*dx+dy*dy); // hypot avoids underflow/overflow  ... but it is to slow

		float rSqr = rAB*rAB;
		float a =  rSqr / (2.0f *d);

		float x2 = xpApB + (dx * a /d);
		float y2 = ypApB + (dy * a /d);

		float aSqr = a*a;
		float h = (float) Math.sqrt(rSqr - aSqr);

		float rx = -dy * (h/d);
		float ry = dx * (h/d);

		float xi = x2 + rx;
		float yi = y2 + ry;
		float xiPrime = x2 - rx;
		float yiPrime = y2 - ry;

		tangents[0] = xiPrime;
		tangents[1] = yiPrime;
		tangents[2] = xi;
		tangents[3] = yi;
	}

//	/* (non-Javadoc)
//	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#gisDump()
//	 */
//	@Override
//	public void gisDump() {
//
//
////		LineString left = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(this.getPointX(),this.getPointY()),new Coordinate((double)this.getPointX()+this.getDirectionX(),(double)this.getPointY()+this.getDirectionY()),new Coordinate((double)this.getPointX()+this.getDirectionX()+this.getDirectionY()/10,(double)this.getPointY()+this.getDirectionY()-this.getDirectionX()/10)});
////		GisDebugger.addGeometry(left, "ORCA_dir");
//		
//		Coordinate c0 = new Coordinate(this.pointX - 10*this.directionX,this.pointY - 10*this.directionY);
//		Coordinate c1 = new Coordinate(this.pointX + 10*this.directionX,this.pointY + 10*this.directionY);
//		Coordinate c2 = new Coordinate(this.pointX + 10*this.directionX+20*this.directionY,this.pointY + 10*this.directionY-20*this.directionX);
//		Coordinate c3 = new Coordinate(this.pointX - 10*this.directionX+20*this.directionY,this.pointY - 10*this.directionY-20*this.directionX);
//		Coordinate [] coords = {c0,c1,c2,c3,c0};
//		LinearRing lr = GisDebugger.geofac.createLinearRing(coords);
//		Polygon p = GisDebugger.geofac.createPolygon(lr, null);
//		GisDebugger.addGeometry(p, "agent");
//	}

//	@Override
//	public boolean solutionSatisfyConstraint(double x, double y) {
//		return Algorithms.isLeftOfLine(x, y, this.p0x, this.p0y, this.p1x, this.p1y) > 0;
//	}
	
	private static float norm(ORCALine line) {
		return (float) Math.sqrt(line.getDirectionX()*line.getDirectionX()+line.getDirectionY()*line.getDirectionY());
	}
	
	private static void multiply(ORCALineAgent line, float scalar) {
		line.setDirectionX(line.getDirectionX() * scalar);
		line.setDirectionY(line.getDirectionY() * scalar);
	}
	
	
	//TODO put public static methods to a more central class like Algorithms.java
	static float dot(float x0, float y0, float x1, float y1) {
		return x0 * x1 + y0  * y1;
	}

	/**
	 * tests whether coordinate x0,y0 is located left of the infinite vector that runs from x1,y1  to x2,y2
	 * @param x0 the x-coordinate to test
	 * @param y0 the y-coordinate to test
	 * @param x1 first x-coordinate of the vector
	 * @param y1 first y-coordinate of the vector
	 * @param x2 second x-coordinate of the vector
	 * @param y2 second y-coordinate of the vector
	 * @return >0 if coordinate is left of the vector
	 * 		  ==0 if coordinate is on the vector
	 * 		   <0 if coordinate is right of the vector
	 */
	static float isLeftOfLine(float x0, float y0, float x1, float y1, float x2, float y2) {
		return (x2 - x1)*(y0 - y1) - (x0 - x1) * (y2 - y1);
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#solutionSatisfyConstraint(float[])
	 */
	@Override
	public boolean solutionSatisfyConstraint(float[] v) {
		return isLeftOfLine(v[0], v[1], this.getPointX(), this.getPointY(), this.getPointX()+this.getDirectionX(),this.getPointY()+this.getDirectionY()) >= 0;
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#getPointX()
	 */
	@Override
	public float getPointX() {
		return this.pointX;
	}

	@Override
	public void setPointX(float pointX) {
		this.pointX = pointX;
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#getPointY()
	 */
	@Override
	public float getPointY() {
		return this.pointY;
	}

	@Override
	public void setPointY(float pointY) {
		this.pointY = pointY;
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#getDirectionX()
	 */
	@Override
	public float getDirectionX() {
		return this.directionX;
	}

	public void setDirectionX(float directionX) {
		this.directionX = directionX;
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#getDirectionY()
	 */
	@Override
	public float getDirectionY() {
		return this.directionY;
	}

	public void setDirectionY(float directionY) {
		this.directionY = directionY;
	}

	@Override
	public void debug(VisDebugger debugger, int r, int g, int b) {
		float x0 = this.pointX - 10*this.directionX;
		float y0 = this.pointY - 10*this.directionY;
		float x1 = this.pointX + 10*this.directionX;
		float y1 = this.pointY + 10*this.directionY;
		float x2 = this.pointX + 10*this.directionX + 2*this.directionY;
		float y2 = this.pointY + 10*this.directionY -2*this.directionX;
		float x3 = this.pointX - 10*this.directionX + 2*this.directionY;
		float y3 = this.pointY - 10*this.directionY -2*this.directionX;
		debugger.addPolygon(new float[]{x0,x1,x2,x3}, new float[]{y0,y1,y2,y3}, r, g, b, 32, 0);
		x2 -= this.directionY;
		y2 += this.directionX;
		x3 -= this.directionY;
		y3 += this.directionX;
		debugger.addPolygon(new float[]{x0,x1,x2,x3}, new float[]{y0,y1,y2,y3}, r, g, b, 32, 0);
		x2 -= this.directionY/2;
		y2 += this.directionX/2;
		x3 -= this.directionY/2;
		y3 += this.directionX/2;
		debugger.addPolygon(new float[]{x0,x1,x2,x3}, new float[]{y0,y1,y2,y3}, r, g, b, 32, 0);
		x2 -= this.directionY/4;
		y2 += this.directionX/4;
		x3 -= this.directionY/4;
		y3 += this.directionX/4;
		debugger.addPolygon(new float[]{x0,x1,x2,x3}, new float[]{y0,y1,y2,y3}, r, g, b, 32, 0);
		x2 -= this.directionY/8;
		y2 += this.directionX/8;
		x3 -= this.directionY/8;
		y3 += this.directionX/8;
		debugger.addPolygon(new float[]{x0,x1,x2,x3}, new float[]{y0,y1,y2,y3}, r, g, b, 64, 0);
		x2 -= this.directionY/16;
		y2 += this.directionX/16;
		x3 -= this.directionY/16;
		y3 += this.directionX/16;
		debugger.addPolygon(new float[]{x0,x1,x2,x3}, new float[]{y0,y1,y2,y3}, r, g, b, 128, 0);;
	}

}
