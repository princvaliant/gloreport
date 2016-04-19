package com.glo.report

import com.mongodb.BasicDBObject
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream

//Bruno
import grails.plugin.jms.*

//import javax.jms.Message
import org.activiti.engine.impl.bpmn.behavior.*
import com.glo.mongo.*
import com.glo.ndo.*
import com.glo.security.*

class RehabWafersController {
	
	def mongo
	def utilService
	
	def index = {
		
		def results = []
				
		def db = mongo.getDB("glo")
		
		String waf = params.waf ? params.waf : null

		def match = new BasicDBObject()
		if (waf != null) match.put('code', waf)
		match.put('productCode', new BasicDBObject(['$in':["100"]]))
		
		def project = new BasicDBObject()
		project.put('code', 1)		//seek to include the "-R" condition in this line
		project.put('pkey', 1)
		project.put('tkey', 1)
		project.put('tname', 1)
		
		db.unit.find (match, project).collect{ unit ->
			def x = unit.code.indexOf("-R")
			if (x >= 0) {
				def result = [:]
				
				result.code = unit.code
				result.pkey = unit.pkey
				result.tkey = unit.tkey
				result.tname = unit.tname
				
				result."ngan_inspection" = "No"
				result."sin_deposition" = "No"
				result."inventory_nil" = "No"
				result."descum_etch_inspection" = "No"
				result."final_clean" = "No"
				
				def match1 = new BasicDBObject()
				match1.put('code', unit.code.substring(0,x))
				
				db.history.find (match1).collect{ unit1 ->
					unit1.audit.each { 
						if (it.tkey == "ngan_inspection") result."ngan_inspection" = "Yes"
						if (it.tkey == "sin_deposition") result."sin_deposition" = "Yes"
						if (it.tkey == "inventory_nil") result."inventory_nil" = "Yes"
						if (it.tkey == "descum_etch_inspection") result."descum_etch_inspection" = "Yes"
						if (it.tkey == "final_clean") result."final_clean" = "Yes"
					}
				}
				results.add(result)
			}
		}

		XSSFWorkbook workbook = utilService.exportExcel(results, "")

		response.setHeader("Content-disposition", "attachment; filename=data.xlsx")
		response.contentType = "application/excel"
		ServletOutputStream f = response.getOutputStream()
		workbook.write(f)
		f.close()
		
		//render (['result':results.size()])  as JSON
	}
}