package com.glo.report

class ProductMaskItem implements Comparable {

	static auditable = true

	String code
	Float plX
	Float plY
	Float sizeX
	Float sizeY 
	Boolean isActive
	
	
	static belongsTo = [
		productMask: ProductMask,
		//derivedProduct: Product
	]

	static constraints = {
		code blank: false, maxSize:100
		plX nullable:true
		plY nullable:true
		sizeX nullable:true
		sizeY nullable:true
		isActive nullable:true
	}

	String toString() {
		code + ", " + derivedProduct?.name
	}

	public int compareTo(def other) {
		return this.toString() <=> other?.toString()
	}
}
