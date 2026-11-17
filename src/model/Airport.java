package model;

import java.util.Objects;

public class Airport {
	private String name;
	private String code;
	private double x,y;
	
	public Airport(String name, String code, double x, double y) {
		this.name = name;
		this.code = code;
		this.x = x;
		this.y = y;
	}
	
	public String getName() {return name;}
	public String getCode() {return code;}
	public double getX() {return x;}
	public double getY() {return y;}
	
	public void setName(String name) {this.name = name;}
	public void setCode(String code) {this.code = code;}
	public void setX(double x) {this.x =x;}
	public void setY(double y) {this.y = y;}

	@Override
	public int hashCode() {
		return Objects.hash(code, name, x, y);
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj) return true;
	    if (obj == null || getClass() != obj.getClass()) return false;
	    Airport other = (Airport) obj;
	    return Objects.equals(code, other.code) && Objects.equals(name, other.name)
	           && x == other.x && y == other.y;
	}

	@Override
	public String toString() {
		 return code + " - " + name + " (" + x + ", " + y + ")";
	}
	
}
