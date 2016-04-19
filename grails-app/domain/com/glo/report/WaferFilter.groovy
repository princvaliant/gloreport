package com.glo.report

/*
class WaferFilter  {

	String userName
	boolean isActive
	boolean isAdmin
	String level1
	String level2
	float valFrom
	float valTo

	static constraints = {
		level1 blank:false,maxSize:1000
	}

	String toString() {
		level1 + " " + level2
	}
}
*/

class WaferFilter implements Comparable  {

	String userName
	boolean isActive
	boolean isAdmin
	String level1
	String level2
	float valFrom
	float valTo
	
	static belongsTo = [
		dieSpec: DieSpec
	]

	static constraints = {
		level1 blank:false,maxSize:1000
		dieSpec nullable:true
	}

	String toString() {
		level1 + " " + level2
	}

	public int compareTo(def other) {
		return this.toString() <=> other?.toString()
	}
}