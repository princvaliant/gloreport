package com.glo.report

import com.mongodb.BasicDBObject
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream

//Bruno

class BarcodeStatsController {
	
	def mongo
	def utilService
	
	def index = {
		
		def results = []
		def record = [:]
		
		def db = mongo.getDB("glo")
		
		def query = new BasicDBObject()
		
		def project = new BasicDBObject()
		project.put('_id', 0)
		
		def installNames = db.barcodeLabels.distinct("installName", query)
		installNames.each { installName ->
			def query1 = new BasicDBObject()
			query1.put('installName', installName)
			def labelNames = db.barcodeLabels.distinct("labelName", query1)
			labelNames.each { labelName ->
				def query2 = new BasicDBObject()
				query2.put('installName', installName)
				query2.put('labelName', labelName)
				def cnt = db.barcodeLabels.find(query2).count()
				
				if (installName == "EpiSmall" && labelName == "Wafer") cnt += 200	// 200 labels printed back in printLabel collection
				
				record = [:]
				record['Location'] = installName
				record['Label type'] = labelName
				record['Printed qty'] = cnt
				results.add(record)
			}
		}
		
		record = [:]
		record['Location'] = ""
		record['Label type'] = ""
		record['Printed qty'] = ""
		results.add(record)
		
		record = [:]
		record['Location'] = "Process"
		record['Label type'] = "Total moves"
		record['Printed qty'] = "Scanner moves"
		results.add(record)
		

		db.systemStats.find (query, project).collect{ unit ->
			record = [:]
			record['Location'] = unit.pkey
			record['Label type'] = unit.total ?: ""
			record['Printed qty'] = unit.barcode ?: ""
			results.add(record)
		}

		XSSFWorkbook workbook = utilService.exportExcel(results, "")

		response.setHeader("Content-disposition", "attachment; filename=barcodeStats.xlsx")
		response.contentType = "application/excel"
		ServletOutputStream f = response.getOutputStream()
		workbook.write(f)
		f.close()
	}
}