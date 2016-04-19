package com.glo.report

import com.mongodb.BasicDBObject
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream

//Bruno

class FrankD65ReportController {
	
	def mongo
	def utilService
	
	def index = {
		
		def results = []
		def gcKeepers = []
		def phKeepers = []
		
		def db = mongo.getDB("glo")
		String gcColor = null, phColor = null
		float gcMin = -1, gcMax = -1
		float phMin = -1, phMax = -1
		
		String waf = params.waf ? params.waf.toUpperCase() : null
		def dev = params.dev ? params.dev.toUpperCase().tokenize(',') : null
		def rName = params.rName ?: "D65_data"
		
		String clr = params.gcColor ? params.gcColor.toLowerCase() : null
		if (clr) gcColor = clr[0..0].toUpperCase() + clr[1..-1]
		def mm = params.gc ? params.gc.tokenize(',') : null
		if (mm && mm.size() == 2 && mm[0].isFloat() && mm[1].isFloat()) {
			gcMin = mm[0].toFloat()
			gcMax = mm[1].toFloat()
		} else gcColor = null
		
		clr = params.phColor ? params.phColor.toLowerCase() : null
		if (clr) phColor = clr[0..0].toUpperCase() + clr[1..-1]
		mm = params.ph ? params.ph.tokenize(',') : null
		if (mm && mm.size() == 2 && mm[0].isFloat() && mm[1].isFloat()) {
			phMin = mm[0].toFloat()
			phMax = mm[1].toFloat()
		} else phColor = null
	
		//render ( gcColor + "  " + gcMin + "  " + gcMax + "  " + phColor + "  " + phMin + "  " + phMax )

		def match = new BasicDBObject()
		if (waf != null) match.put('WaferID', waf)
		if (dev != null) match.put('DeviceID', new BasicDBObject('$in':dev))
		match.put('TestType', 'd65_after_encap')			// for required Result Types, TestType should be d65_after_encap anyway
		match.put('ResultType', new BasicDBObject('$in':["White","Red","Green","Blue"]))
		
		def project = new BasicDBObject()
		project.put('wavelengthPowerScan', 0)
		
		db.measures.find (match, project).collect{ unit ->
			def result = [:]
			result.headerId = unit.headerId
			result.WaferID = unit.WaferID
			result.DeviceID = unit.DeviceID
			result.build_number = unit.build_number
			//result.TestType = unit.TestType
			result.ResultType = unit.ResultType
			result.'EQE in D65' = unit.eqe
			result.'EQE on wafer @ 20mA' = ""
			result.'RedCurr [mA]' = unit.RedCurr ? unit.RedCurr * 1000 : -1
			result.'RedSet [mA]' = unit.RedSet ? unit.RedSet * 1000 : -1
			result.'RedVolt [V]' = unit.RedVolt
			result.'GreenCurr [mA]' = unit.GreenCurr ? unit.GreenCurr * 1000 : -1
			result.'GreenSet [mA]' = unit.GreenSet ? unit.GreenSet * 1000 : -1
			result.'GreenVolt [V]' = unit.GreenVolt
			result.'BlueCurr [mA]' = unit.BlueCurr ? unit.BlueCurr * 1000 : -1
			result.'BlueSet [mA]' = unit.BlueSet ? unit.BlueSet * 1000 : -1
			result.'BlueVolt [V]' = unit.BlueVolt
			result.radiometric = unit.radiometric ? unit.radiometric * 1000 : -1
			result.'photometric [lm]' = unit.photometric
			result.'PeakWavelength [nm]' = unit.PeakWavelength
			result.'dominantWavelength [nm]' = unit.dominantWavelength
			result.'FWHM [nm]' = unit.FWHM
			result.CCT_K = unit.CCT_K
			result.Duv = unit.Duv
			result.experimentId = unit.experimentId
			result.Comment = unit.Comment
			result.MaxAdcValue = unit.MaxAdcValue
			result.TimeRun = unit.TimeRun
			result.TimeRunMs = unit.TimeRunMs
			result.RedPhosBlueLeakRatio = unit.RedPhosBlueLeakRatio
			result.RedPhosBluePeaknm = unit.RedPhosBluePeaknm
			
			//result.IntegrationTime = unit.IntegrationTime
			//result.FilterPosition = unit.FilterPosition
			//result.x = unit.x
			//result.y = unit.y
			//result.z = unit.z
			//result.u = unit.u
			//result.v1960 = unit.v1960
			//result.PeakWavelengthIntensity = unit.PeakWavelengthIntensity
			//result.Centroid = unit.Centroid
			//result.CRI = unit.CRI
			
			results.add(result)

			if (gcColor && gcColor == result.ResultType)
				if (gcMin <= result.'GreenCurr [mA]' && result.'GreenCurr [mA]' <= gcMax)
					gcKeepers.add(result.WaferID + result.DeviceID)
			if (phColor && phColor == result.ResultType)
				if (phMin <= result.'photometric [lm]' && result.'photometric [lm]' <= phMax)
					phKeepers.add(result.WaferID + result.DeviceID)
		}
		
		if (gcColor) results = results.findAll { (it.WaferID + it.DeviceID) in gcKeepers }
		if (phColor) results = results.findAll { (it.WaferID + it.DeviceID) in phKeepers }

		//pick-up EQE on wafer from testData
		results = results.sort { it.'WaferID' }
		def code = ""
		def data = null
		results.eachWithIndex { obj, idx ->
			if (code != obj.WaferID) {
				code = obj.WaferID
				data = null
				
				def queryDR = new BasicDBObject()
				queryDR.put("code",obj.WaferID)
				queryDR.put("parentCode",null)
				queryDR.put("value.productCode","100")
				
				def fieldsDR = new BasicDBObject()
				fieldsDR.put("value.test_data_visualization.testDataIndex",1)
				fieldsDR.put("value.full_test_visualization.testFullIndex",1)
				
				def unitDR = db.dataReport.find(queryDR, fieldsDR).collect { it }[0]
				if (unitDR) {
					def testIndexes = unitDR?.value?.test_data_visualization?.testDataIndex ?: []
					def fullIndexes = unitDR?.value?.full_test_visualization?.testFullIndex ?: []
					def temp = utilService.getLatestTestData(obj.WaferID, testIndexes, fullIndexes, "value.data.Data @ 20mA.EQE")
					if (temp)
						temp.collect { testRecord ->
							data = testRecord['value']['data']['Data @ 20mA']['EQE']
						}
				}
			}
			if (data)
				obj.'EQE on wafer @ 20mA' = data[obj.DeviceID]
		}
		
		results = results.sort { it.'TimeRun' }.sort { it.'headerId' }.reverse()
		
		XSSFWorkbook workbook = utilService.exportExcel(results, "")

		def fHeader = "attachment; filename=" + rName + ".xlsx"
		response.setHeader("Content-disposition", fHeader)
		response.contentType = "application/excel"
		ServletOutputStream f = response.getOutputStream()
		workbook.write(f)
		f.close()
	}
}