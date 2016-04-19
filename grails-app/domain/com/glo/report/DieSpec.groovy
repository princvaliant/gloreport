package com.glo.report

class DieSpec   {
	
	String 		name
	Integer 	revision
	String 		username
	Date 		dateCreated

	static hasMany = [
		waferFilters: WaferFilter
	]
	
	static constraints = {
		name blank:false
		revision blank: false
		username blank: false
	}

	String toString() {
		name + " (Rev. " + revision + ")"
	}
}