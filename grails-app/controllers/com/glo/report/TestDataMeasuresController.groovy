package com.glo.report

import com.mongodb.BasicDBObject
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat

// testData vs. measures

class TestDataMeasuresController {
	
	def mongo
	def utilService

	/***
	def calcDurations (def code, def tkey) {
		
			def db = mongo.getDB("glo")
	
			def bdo = new BasicDBObject()
			if (code) {
				bdo.put("value.code", code)
			}
			if (tkey) {
				bdo.put("value.tkey", tkey)
			} else {
				bdo.put("value.tkey", new BasicDBObject('$in', [
					"test_data_visualization",
					"top_test_visualization",
					"char_test_visualization",
					"full_test_visualization"
				]))
			}
			bdo.put("value.parentCode", null)
			bdo.put("value.sync",  new BasicDBObject('$ne',"1"))
	
	
			def testIds = db.testData.find(bdo, new BasicDBObject("value.code",1)).collect{it}
	
			testIds.each { testId ->
	
				def val = db.testData.find(new BasicDBObject("_id", testId._id), new BasicDBObject()).collect{it.value}[0]
			}
			db.testData.update(new BasicDBObject("_id", testId._id), new BasicDBObject('$set', new BasicDBObject("value.sync", "1")))
	}
	***/
	
	
	def index = {
		
		def resync = params.resync ?: "n"
		
		def db = mongo.getDB("glo")
		def results = []
		
		DateFormat  dformat = new SimpleDateFormat("MM/dd/yyyy")

		def fields = new BasicDBObject()
		fields.put("_id",0)
		fields.put("value.data",0)
		
		def fields1 = new BasicDBObject()
		fields1.put("wavelengthPowerScan", 0)
		fields1.put("VISwp", 0)
		
		def totalCnt = 0
		def resetCnt = 0
		
		def query = new BasicDBObject()
		query.put("value.parentCode", null)
		//query.put("value.code", 'YA11B005003')
		def tkeys = ['test_data_visualization', 'top_test_visualization', 'char_test_visualization', 'full_test_visualization']
		query.put('value.tkey', new BasicDBObject(['$in':tkeys]))
		
		def arr = db.testData.find(query, fields).collect { it.value }
		
		arr.each {
			totalCnt++
			
			def query1 = new BasicDBObject()
			query1.put("WaferID", it.code)
			
			def testId1 = it.testId ?: "unknown"
			def testId = testId1.toString().isDouble() ? testId1.toLong() : testId1
			
			query1.put("sid", testId.toString())
			query1.put("TestType",it.tkey)
			if (db.measures.find(query1, fields1))
				it.measures = 1
			else {
				it.measures = 0
				
				if (it.sync.toString() == "1") {
					resetCnt++
					def query2 = new BasicDBObject()
					query2.put("value.code", it.code)
					query2.put("value.parentCode", null)
					query2.put("value.tkey", it.tkey)
					query2.put("value.testId", testId)
					def update = new BasicDBObject()
					update.put('$set', new BasicDBObject("value.sync", ""))
					if (resync == 'y')
						db.testData.update(query2, update, false, true)
				}
				def record = [:]
				record.code = it.code
				record.testId = testId.toString()
				record.sync = it.sync.toString()
				record.experimentId = it.experimentId
				record.date = it.date ?: ""
				record.tkey = it.tkey
				record.measures = it.measures
				results.add(record)
			}
		}

		def record = [:]
		record.code = "Total"
		record.testId = totalCnt
		record.sync = ""
		record.experimentId = ""
		record.date = ""
		if (resync == 'y')
			record.tkey = "Sync reset performed"
		else
			record.tkey = "Sync reset candidates"
		record.measures = resetCnt
		results.add(record)
	
		XSSFWorkbook workbook = utilService.exportExcel(results, "")

		def fheader = "attachment; filename=KStests.xlsx"
		response.setHeader("Content-disposition", fheader)
		response.contentType = "application/excel"
		ServletOutputStream f = response.getOutputStream()
		workbook.write(f)
		f.close()
	}
}