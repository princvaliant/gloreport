package com.glo.report

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream

//Bruno
import grails.plugin.jms.*

//import javax.jms.Message
import org.activiti.engine.impl.bpmn.behavior.*
import com.glo.mongo.*
import com.glo.ndo.*
import com.glo.security.*

class WaferCheckController {
	
	def mongo
	def utilService
	
	def index = {
		
		def results = []
		def collections = ['unit', 'unitarchive', 'dataReport', 'history']		//  , 'testData', 'measures'
		def codepath = ['code', 'code', 'code', 'code']		// , 'value.code', 'WaferID'
		
		def input = params.input ?: 'file'
		int exact = params.exact ? params.exact.toInteger() : 1				// 0 (substring, case insensitive, VERY SLOW), 1 (exact match, case sensitive)
		int report = params.report ? params.report.toInteger() : 0			// 0 - errors only; 1 - all
		int check = params.check ? params.check.toInteger() : 0				// 0 - check only; 1 - check and fix 'active' flag
		
		def db = mongo.getDB("glo")
		
		def arr = []
		if (input == 'file') {
			arr = readWaferFile()
		}
		else {
			def query = new BasicDBObject()
			query.put('value.productCode', '100')
			
			def fields = new BasicDBObject()
			fields.put('code', 1)
			arr = db.dataReport.find(query, fields).collect{it.code}
		}

		arr.eachWithIndex { obj1, idx1 ->
			def result = [:]
			result.waferID = obj1
			collections.eachWithIndex {  obj2, idx2 ->
				DBCollection collection = db.getCollection(obj2)
			
				def query = new BasicDBObject()
				if (exact == 1)
					query.put(codepath[idx2], obj1)
				else
					query.put(codepath[idx2], new BasicDBObject(['$regex': obj1,  '$options': 'i']))
				def project = new BasicDBObject()
				project.put(codepath[idx2], 1)
				project.put('tkey', 1)
				project.put('value.active', 1)
				project.put('parentCode', 1)
				
				def units = collection.find (query, project).collect { it }
				result[obj2] = units.size()
				
				if (obj2 in ["unit","unitarchive"]) {
					result[obj2 + '.tkey'] = ""
					if (units[0] && units[0].tkey != null)  result[obj2 + '.tkey'] = units[0].tkey
				}
				
				if (obj2 == "dataReport") {
					result['dataReport.value.active'] = ""
					if (units[0] && units[0].value.active != null)
						result['dataReport.value.active'] = units[0].value.active
					
					result['dataReport.parentCode'] = ""
					if (units[0] && units[0].parentCode != null)
						result['dataReport.parentCode'] = units[0].parentCode
				}
			}
			
			// check some integrity points
			result['assessment'] = 'not performed'
			def assmt = ""
			def kk = result['unit'] + result['unitarchive']
			def active = result['dataReport.value.active']
			switch (kk) {
				case 0:
					if (active != "") {
						assmt += "unit + archive = 0 but still active ; "
						if (check == 1) {
							db.dataReport.update(['code':result.waferID], ['$set':['value.active':'']])
							assmt += "FIXED; "
						}
					}
					break
				case 1:
					if (result['unit'] == 1 && active != "true") {
						assmt += "unit = 1 but active not true ; "
						if (check == 1) {
							db.dataReport.update(['code':result.waferID], ['$set':['value.active':'true']])
							assmt += "FIXED; "
						}
					}
					if (result['unitarchive'] == 1 && active != "") {
						assmt += "archive = 1 but still active ; "
						if (check == 1) {
							db.dataReport.update(['code':result.waferID], ['$set':['value.active':'']])
							assmt += "FIXED; "
						}
					}
					break
				default:
					assmt += "unit + archive = $kk ; "
			}
			
			kk = result['dataReport']
			if (kk != 1) assmt += "dataReport = $kk ; "
			
			kk = result['history']
			if (kk != 1) assmt += "history = $kk ; "
			
			if (assmt == "") assmt = "ok"
			result['assessment'] = assmt
			
			if (result['assessment'] != 'ok' || report == 1)
				results.add(result)
		}
		
		XSSFWorkbook workbook = utilService.exportExcel(results, "")

		response.setHeader("Content-disposition", "attachment; filename=waferCheck.xlsx")
		response.contentType = "application/excel"
		ServletOutputStream f = response.getOutputStream()
		workbook.write(f)
		f.close()
		
		//render (['result':results.size()])  as JSON
	}

	private def readWaferFile () {
		
		def file = "C:\\Users\\bruno.s\\Documents\\MES\\Data corrections - manual or js\\wafers.txt"
		FileReader fr = null
		BufferedReader br = null
		def arr = []
		
		try {
			fr = new FileReader(file)
			br = new BufferedReader(fr)
			def line
			
			while ((line = br.readLine()) !=null) {
				arr.add(line)
			}
		} catch (Exception exc) {
			throw new RuntimeException(exc.getMessage())
		}
		finally {
			if (br != null)
				br.close()
			if (fr != null)
				fr.close()
			br = null
			fr = null
		}

		arr
	}
			
}