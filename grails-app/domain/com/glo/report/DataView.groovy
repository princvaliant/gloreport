package com.glo.report

class DataView {

   String name
   String description
   Boolean isPublic
   String tag
   
   Date dateCreated 
   Date lastUpdated 
   String owner
   byte[] excelTemplate
   String excelName
   Boolean publishToDashboard
   String urlDashboardData
   String urlExportData
   String chartType
   Boolean filterByPath
   Float yMin
   Float yMax
   Float zMin
   Float zMax
 
   static constraints = {
		name blank:false,maxSize:100
		tag nullable: true
		description nullable:true
		isPublic nullable:true
		publishToDashboard nullable:true
		excelTemplate nullable: true, maxSize: 4000000
		urlDashboardData nullable: true
		urlExportData nullable: true
		chartType nullable: true
		excelName nullable: true
		filterByPath nullable:true
		yMin nullable:true
		yMax nullable:true
		zMin nullable:true
		zMax nullable:true
   }
   
   String toString() {
	  name 
   }
}
