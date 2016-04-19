package com.glo.report

class ProductMask implements Comparable {

	static auditable = true

	String name
	
	//static belongsTo = [
	//	product: Product
	//]

	static hasMany = [
		productMaskItems: ProductMaskItem
	]
	
	static constraints = {
		name blank: false, maxSize:100
	}

	String toString() {
		name 
	}

	public int compareTo(def other) {
		return this.toString() <=> other?.toString()
	}
}
