package com.glo.report

import com.mongodb.BasicDBObject
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream

class PeakDistributionController {

	def mongo
	def utilService

	def index = {

		def db = mongo.getDB("glo")

		def minEqe = params.minEqe ? params.minEqe.toFloat() : 1.0
		def minPeak = params.minPeak ? params.minPeak.toFloat() : 450.0
		def lastDays = params.days ?  params.days.toInteger() : 60

		def waferFilters = WaferFilter.findAllByIsAdmin(1)

		def queryUnit = new BasicDBObject()

		def df = new Date() - lastDays
		def dt = new Date() + 1
		queryUnit.put("parentCode",null)
		queryUnit.put("value.productCode","100")
		queryUnit.put("value.test_data_visualization.actualStart", new BasicDBObject(['$gte':df, '$lte': dt]))

		def fields = new BasicDBObject()
		fields.put("code",1)
		fields.put("value.test_data_visualization.actualStart",1)
		fields.put("value.test_data_visualization.testDataIndex",1)
		fields.put("value.test_data_visualization.mask",1)
		fields.put("value.test_data_visualization.experimentId",1)
		fields.put("value.full_test_visualization.testFullIndex",1)
		
		def results =[]

		db.dataReport.find(queryUnit, fields).collect { unit ->
			
			
			def testIndexes = unit?.value?.test_data_visualization?.testDataIndex ?: []
			def fullIndexes = unit?.value?.full_test_visualization?.testFullIndex ?: []
			def temp = utilService.getLatestTestData(unit?.code, testIndexes, fullIndexes, null)

			if (temp) {
				def testData = temp.collect{it}[0]
	
				if (testData && testData["value"] && testData["value"]["data"]) {
					def data = (BasicDBObject)testData["value"]["data"]
	
					data.each { current, obj ->
	
						Set filtered = new HashSet()
	
						def filters = waferFilters.findAll { it.level1 == current }
						filters.eachWithIndex { filter, i ->
							
							if (obj) {
								
								def unfiltered = (BasicDBObject)obj[filter.level2]
								if (unfiltered) {
									
									unfiltered.each { k, v ->
										
										if (filter.valFrom <= v && filter.valTo >= v && i == 0) {
											filtered.add(k)
										} else if ((filter.valFrom > v || filter.valTo < v) && i > 0) {
											if (filtered.contains(k))
												filtered.remove(k)
										}
									}
								}
							}
						}
	
						def curr = current.tokenize('@')[1]
						if (curr && curr.size() >= 2) {
							
							Integer currVal = Integer.parseInt(curr.replaceAll("\\D+",""))
							Float currVal2 = currVal
							if (curr.indexOf('mA') > 0) currVal2 = currVal * 1E-3
							if (curr.indexOf('uA') > 0) currVal2 = currVal * 1E-6
			
			
							filtered?.each {
								def result = [:]
								result.code = unit.code + "_" + it
								result.experimentId = unit?.value?.test_data_visualization?.experimentId
								result.mask =  unit?.value?.test_data_visualization?.mask
								result.current = currVal2
								result.eqe = obj["EQE"] ?  obj["EQE"][it] : null
								result.peak = obj["Peak (nm)"] ? obj["Peak (nm)"][it] : null
								
								if (result.eqe >= minEqe && result.peak >= minPeak ) {
									results.add(result)
								}
							}
						}
						
						filtered = null
					}
				}
				testData = null
			}
		}
		
		XSSFWorkbook workbook = utilService.exportExcel(results, "")
		
		response.setHeader("Content-disposition", "attachment; filename=data.xlsx")
		response.contentType = "application/excel"
		ServletOutputStream f = response.getOutputStream()
		workbook.write(f)
		f.close()

		results = null
	}
	
}