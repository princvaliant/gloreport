package com.glo.report

import com.mongodb.BasicDBObject
import grails.converters.JSON

//import com.glo.custom.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream
import java.text.SimpleDateFormat

class VisualizationYieldController {
	
	def mongo
	def utilService
	
	def index = {
		
		def results = []
		
		def db = mongo.getDB("glo")
		
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy")
		//def df = params.start? formatter.parse(params.start) : null
		//def dt = params.end? formatter.parse(params.end) + 1 : new Date() + 1		// add 1 day in order to include the 'end' day

		def pspec = params.spec ?: null
		def pexp = params.exp ?: null
		def lastDays = params.days ?  params.days.toInteger() : 2				// from test_data_visualization date
		def df = new Date() - lastDays
		def dt = new Date() + 1
		def tdv = params.tdv ?  params.tdv.toInteger() : 0						// latest from test_data_visualization
		def ttv = params.ttv ?  params.ttv.toInteger() : 0						// latest from top_test_visualization
		def ctv = params.ctv ?  params.ctv.toInteger() : 0						// latest from char_test_visualization
		def ftv = params.ftv ?  params.ftv.toInteger() : 0						// latest from full_test_visualization
        def nbp = params.nbp ?  params.nbp.toInteger() : 0                      // latest from nbp_test_data_visualization
        def nbpf = params.nbpf ?  params.nbpf.toInteger() : 0                   // latest from nbp_full_test_visualization
		def detail = params.detail ?: ""
		def rName = params.rName ?: "visualization_yield"
		
		if (pspec == null || pexp == null) {
			//export list of specs, nothing else
			def ret = DieSpec.list(sort:"dateCreated", order:"desc")
			results = ret.collect { [Id: it.id, Name:it.toString(), User:it.username] }	// , dateCreated:it.dateCreated
		}
		
		else {
			def exps = pexp.toUpperCase().tokenize(',').collect { it?.trim() }
			
			def waferFiltersSet = []
			def dieSpecs = []
			def dieSpecsNo = []
			
			def pspecs = pspec.tokenize(',').collect { it?.trim() }
			pspecs = pspecs.unique()
			pspecs.each {
				if (it.isInteger()) {
					dieSpecsNo.add(it.toInteger())
					def dieSpec = DieSpec.get(it.toInteger())
					dieSpecs.add(dieSpec)
					
					def waferFilters = utilService.getWaferFilters(dieSpec)
					waferFiltersSet.add(waferFilters)
				}
			}
			
			def tkeys = []
			if (tdv != 0) tkeys.add("test_data_visualization")
			if (ttv != 0) tkeys.add("top_test_visualization")
			if (ctv != 0) tkeys.add("char_test_visualization")
			if (ftv != 0) tkeys.add("full_test_visualization")
            if (nbp != 0) tkeys.add("nbp_test_data_visualization")
            if (nbpf != 0) tkeys.add("nbp_full_test_visualization")
			
			if (dieSpecsNo.size() !=0 && tkeys.size() != 0) {

				//get units
				def queryDR = new BasicDBObject()
				queryDR.put("parentCode",null)
				queryDR.put("value.productCode","100")
			//	queryDR.put("code","UN0005788")
				queryDR.put("value.test_data_visualization.actualStart", new BasicDBObject(['$gte':df, '$lte': dt]))
				
				if (exps) {
					//queryDR.put('value.experimentId', new BasicDBObject(['$in':exps]))
					queryDR.put('$or', [])
					exps.each {
						queryDR['$or'].add(new BasicDBObject('value.experimentId', new BasicDBObject('$regex', "^" + it)))
					}
				}
	
				def fieldsDR = new BasicDBObject()
				fieldsDR.put("code",1)
				fieldsDR.put("value.test_data_visualization.mask",1)
				fieldsDR.put("value.test_data_visualization.testDataIndex",1)
				fieldsDR.put("value.top_test_visualization.testTopIndex",1)
				fieldsDR.put("value.char_test_visualization.testCharIndex",1)
				fieldsDR.put("value.full_test_visualization.testFullIndex",1)
				fieldsDR.put("value.test_data_visualization.experimentId",1)
                fieldsDR.put("value.nbp_test_data_visualization.testDataIndex",1)
                fieldsDR.put("value.nbp_full_test_visualization.testDataIndex",1)
				fieldsDR.put("value.epi_growth.runNumber",1)
				fieldsDR.put("value.epi_growth_sweden.runNumber",1)
				fieldsDR.put("value.epi_growth.actualStart",1)
				fieldsDR.put("value.epi_growth_sweden.actualStart",1)
				fieldsDR.put("value.active",1)
				
				def totalDevicesCnt = [:]
				
				db.dataReport.find(queryDR, fieldsDR).collect { unit ->
					
					def query = new BasicDBObject()
					query.put("value.code", unit.code)
					query.put("value.parentCode", null)
					query.put('value.tkey', new BasicDBObject(['$in':tkeys]))
					
					db.testData.find(query, new BasicDBObject()).collect { testData ->
					
						def mask = unit?.value?.test_data_visualization?.mask
						if (!totalDevicesCnt[mask]) {
							def prodMask = ProductMask.findByName(mask)
							def maskDevices = new HashSet()
							prodMask.productMaskItems.each {
								if (it.isActive)
									maskDevices.add(it.code)
							}
							totalDevicesCnt[mask] = maskDevices.size()
						}
						
						def data = testData?.value?.data
						
						def result = [:]
						result.Exp = testData.value.experimentId
						result.Code = testData.value.code
						
						//result.Active = unit?.value?.active ? "active" : "archived"
						result.Mask = mask
						result.Total = totalDevicesCnt[mask]

						result.epiRun = unit?.value?.epi_growth?.runNumber ?: unit?.value?.epi_growth_sweden?.runNumber
						result.epiDate = formatter.format(unit?.value?.epi_growth?.actualStart ?: unit?.value?.epi_growth_sweden?.actualStart)
						result.TestDate	 = formatter.format(testData?.value.date ?: unit?.value?.test_data_visualization?.actualStart)
						
						def testedDevicesTotal = 0
						
						for (def spec=0; spec<dieSpecs.size(); spec++) {
							def no = dieSpecsNo[spec].toString()

							
							def arr = utilService.getValid(waferFiltersSet[spec], data)
							
							def testedDevicesCnt = arr[0].cnt ?: 0
							if (result.Tkey == "test_data_visualization" && mask in ["MASK6","MASK7"] && testedDevicesCnt >= 30) testedDevicesCnt -= 30
							testedDevicesTotal += testedDevicesCnt
							result['Spec' + no + ': ' + arr[0].filter] = testedDevicesCnt
							
							def filteredDevicesCnt = 0
							arr.eachWithIndex { el, idx ->
								if (detail == "on" && idx != 0) result['Spec' + no + ': ' + el.filter] = el.cnt
								filteredDevicesCnt = el.cnt
							}
							result['Spec' + no + ': Pass'] = filteredDevicesCnt

                            def testedYield = 'n/a'
                            if (testedDevicesCnt != 0)
                                testedYield = (filteredDevicesCnt.toFloat() / testedDevicesCnt * 100).round(1)

							result['Spec' + no + ': TestedYield [%]'] = testedYield
                            result['Spec' + no] = dieSpecs[spec].toString()
						}

                        result.Tkey = testData.value.tkey
                        result.TestId = testData.value.testId.toString()


                        if (testedDevicesTotal != 0) {
							results.add(result)
						}
					}
				}
				
				results = results.sort { it.TestId }.reverse()
				results.sort { it.Tkey }.sort { it.Code }
				results = results.sort { it.Exp }.reverse()
			}
		}

 //       render (results as JSON)
//
		XSSFWorkbook workbook = utilService.exportExcel(results, "")

		def fHeader = "attachment; filename=" + rName + ".xlsx"
		response.setHeader("Content-disposition", fHeader)
		response.contentType = "application/excel"
		ServletOutputStream f = response.getOutputStream()
		workbook.write(f)
		f.close()
	}
}