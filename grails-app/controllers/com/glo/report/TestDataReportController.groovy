package com.glo.report

import com.mongodb.BasicDBObject
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat

//Bruno

class TestDataReportController {
	
	def mongo
	def utilService

	def index = {
		
		def lastDays = params.days ?  params.days.toInteger() : 2
		
		def results = []
	
		def db = mongo.getDB("glo")
		
		DateFormat  dformat = new SimpleDateFormat("MM/dd/yyyy")

		def query = new BasicDBObject()
		query.put("parentCode",null)
		query.put("value.productCode","100")
		
		def df = new Date() - lastDays
		def dt = new Date() + 1
		def q1 = new BasicDBObject("value.test_data_visualization.actualStart", new BasicDBObject(['$gte':df, '$lte': dt]))
		def q2 = new BasicDBObject("value.top_test_visualization.actualStart", new BasicDBObject(['$gte':df, '$lte': dt]))
		def q3 = new BasicDBObject("value.char_test_visualization.actualStart", new BasicDBObject(['$gte':df, '$lte': dt]))
		def q4 = new BasicDBObject("value.full_test_visualization.actualStart", new BasicDBObject(['$gte':df, '$lte': dt]))
		query.put('$or', [q1, q2, q3, q4])

		def fields = new BasicDBObject()
		fields.put("code",1)
		
		def arr = db.dataReport.find(query, fields).collect { it.code }
		
		arr.each { code ->
			query = new BasicDBObject()
			query.put("code", code)
			query.put("productCode","100")
			
			db.history.find(query).collect { unit ->
				unit.audit.eachWithIndex { obj, idx ->
					def testIds = null
					if (obj.tkey == "test_data_visualization") testIds = obj?.dc?.testDataIndex ?: []
					else if (obj.tkey == "top_test_visualization") testIds = obj?.dc?.testTopIndex ?: []
					else if (obj.tkey == "char_test_visualization") testIds = obj?.dc?.testCharIndex ?: []
					else if (obj.tkey == "full_test_visualization") testIds = obj?.dc?.testFullIndex ?: []
					
					if (testIds != null) {
						def record = [:]
						record.code = unit.code
						record.experimentId = obj.experimentId
						record.source = "history"
						record.tkey = obj.tkey
						//record.testId = testIds.size() > 0 ? testIds[testIds.size() - 1] : ""
						record.testId = testIds.toString()
						record.date = obj.actualStart
						record.data = ""
						results.add(record)
					}
				}
			}
			
			query = new BasicDBObject()
			query.put("value.code", code)
			query.put("value.parentCode",null)
			
			db.testData.find(query).collect { unit ->
				def record = [:]
				record.code = unit.value.code
				record.experimentId = unit.value.experimentId
				record.source = "testData"
				record.tkey = unit.value.tkey
				record.testId = unit.value.testId
				record.date = unit.value.date
				record.data = ""
				unit.value.data.each { k, v ->
					record.data += k + ", "
				}
				results.add(record)
			}
		}
		
		results = results.sort {it.source}.reverse()
		results.sort {it.date}.sort {it.code}
		
		XSSFWorkbook workbook = utilService.exportExcel(results, "")

		def fheader = "attachment; filename=KStests_last_" + lastDays + "days.xlsx"
		response.setHeader("Content-disposition", fheader)
		response.contentType = "application/excel"
		ServletOutputStream f = response.getOutputStream()
		workbook.write(f)
		f.close()
	}
}