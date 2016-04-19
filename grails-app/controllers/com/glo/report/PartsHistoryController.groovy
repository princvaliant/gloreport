package com.glo.report

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat



class PartsHistoryController {
	
	def mongo
	def utilService
	
	def index = {
		
		def results = []
				
		DateFormat  dformat = new SimpleDateFormat("yyyy-MM-dd")
		DBCollection collection

		def db = mongo.getDB("glo")
		
		def processSteps = params.ps ? params.ps.tokenize(',') : ["archive","parts_mrb"]
		def rName = params.rName ?: "parts_history"
		
		processSteps.eachWithIndex { processStep, idx ->
			def match = new BasicDBObject()
			match.put('pctg', "RctHdw")
			if (processStep == "archive")
				collection = db.getCollection("unitarchive")
			else {
				collection = db.getCollection("unit")
				match.put('tkey', processStep)
			}
		
			collection.find (match).collect{ unit ->
				def result = [:]
				
				result."current pStep" = processStep
				result.product = unit.product
				result.productCode = unit.productCode
				result.code = unit.code
				result.originalWeight = unit.originalWeight
				result.weight = unit.weight
				
				//result.dateCreated = unit.dateCreated		This date seems same to inventory_incoming 
				//result.actualStart = unit.actualStart		This date seems off
				//result.start = unit.start					This date seems off
				
				def incomingDate = ""
				def cleanCnt = 0
				def repairCnt = 0
				def mrbCnt = 0
				def mrbDate = ""
				def lossDate = ""
				def equipment = ""
				def notes = ""
				def eqDate = new Date(0)
				
				def match1 = new BasicDBObject()
				match1.put('code', unit.code)
				
				db.history.find (match1).collect{ unit1 ->
					unit1.audit.each { 
						switch (it.tkey) {
							case "inventory_incoming":
								incomingDate = it.start? dformat.format(it.start).toString() : ""
								break
							case "clean":
								cleanCnt++
								break
							case "repair":
								repairCnt++
								break
							case "parts_mrb":
								mrbCnt++
								mrbDate = it.start? dformat.format(it.start).toString() : ""
								break
						}
						if (it.loss != null)
							lossDate = it.loss[0].date? dformat.format(it.loss[0].date).toString() : ""
						if (it.equipment != null && it.start != null) {
							if (eqDate < it.start) {
								eqDate = it.start
								equipment = it.equipment
							}
						}
					}
					unit1.note.sort { it.dateCreated }
					unit1.note.each { 
						if (notes != "")
							notes += "  *****  \r\n"
						notes += "[" + it.stepName + "] " + it.userName + ", " + dformat.format(it.dateCreated).toString()+ ": " + it.comment
					}
				}
				
				result.inventory_incoming = incomingDate
				result.equipment = equipment
				result.cleanCnt = cleanCnt
				result.repairCnt = repairCnt
				result.mrbCnt = mrbCnt
				
				result.parts_mrb = mrbDate
				result.lossDate = lossDate
				result.yieldLossId = unit.yieldLossId
				result.yieldLoss = unit.yieldLoss
				result.notes = notes
	
				results.add(result)
			}
		}
		
		results.sort { it.productCode }
		results.sort { it."current pStep" }
		
		XSSFWorkbook workbook = utilService.exportExcel(results, "")

		def fHeader = "attachment; filename=" + rName + ".xlsx"
		response.setHeader("Content-disposition", fHeader)
		response.contentType = "application/excel"
		ServletOutputStream f = response.getOutputStream()
		workbook.write(f)
		f.close()
		
		//render (['result':results.size()])  as JSON
	}
}