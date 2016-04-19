package com.glo.report

import com.mongodb.BasicDBObject
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream

//Bruno

class RgbPkgSortDataController {
	
	def mongo
	def utilService
	
	def index = {
		
		def results = []

		def db = mongo.getDB("glo")
		
		float redSet = params.red ? params.red.toFloat() : 0.011
		float greenSet = params.green ? params.green.toFloat() : 0.052
		float blueSet = params.blue ? params.blue.toFloat() : 0.005
		float diffThreshold = params.diff ? params.diff.toFloat() : 0.003
		int topCnt = params.top ? params.top.toInteger() : 1
		def builds = params.bld ? params.bld.toUpperCase().tokenize(',') : null
		def rName = params.rName ?: "RGB_data"
		
		def match = new BasicDBObject()
		match.put('TestType', 'd65_after_encap')
		if (builds != null) match.put('build_number', new BasicDBObject(['$in':builds]))
		match.put('ResultType', new BasicDBObject(['$in':['CurrentSweepRed','CurrentSweepGreen','CurrentSweepBlue']]))
		
		def project = new BasicDBObject()
		project.put('wavelengthPowerScan', 0)
		
		db.measures.find (match, project).collect{ unit ->
			def result = [:]
			result.Build = unit.build_number
			result.WaferID = unit.WaferID
			result.DeviceID = unit.DeviceID
			result.headerId = unit.headerId
			result.Channel = unit.ResultType

			//result.'source' = unit.CurrentSet? "ok" : "null"
			result.'SetCurrent [A]' = unit.CurrentSet? unit.CurrentSet : -1 	//CurrentSet should always have value; if missing, unit.Current is close but not same
			
			//  CurrentSet and Volt variables always have appropriate values as per ResultType
			switch (unit.ResultType) {
				case 'CurrentSweepRed':
					result.Diff = result.'SetCurrent [A]' - redSet
					break
				case 'CurrentSweepGreen':
					result.Diff = result.'SetCurrent [A]' - greenSet
					break
				case 'CurrentSweepBlue':
					result.Diff = result.'SetCurrent [A]' - blueSet
					break
				default:
					result.Diff = 9999		//should never happen, as only sweep data is processed
			}
			result.'Vf [V]' = unit.Volt
			
			result.'LOP [lm]' = unit.photometric
			result.'WL Peak [nm]' = unit.PeakWavelength
			result.'WL Dom [nm]' = unit.dominantWavelength
			result.'WL Centroid [nm]' = unit.Centroid
			result.'FWHM [nm]' = unit.FWHM
			
			result.Diff = result.Diff.abs()
			if (result.Diff <= diffThreshold) results.add(result)
		}

		results = results.sort  { it.'Diff' }.reverse()
		results = results.sort  { it.'Channel' }.reverse()
		results.sort  { it.'DeviceID' }
		results.sort  { it.'WaferID' }
		
		//trim unwanted records (keep topCnt # of smallest Diff's only)
		int recordCnt = results.size()

		int recordNo = 0
		int cnt = 1
		int sameBuild = 1
	
		if (recordCnt != 0) {
			//results[recordNo].Rank = 'Top'
			while (cnt < recordCnt) {
				recordNo++
				cnt++
				if ((results[recordNo].DeviceID == results[recordNo-1].DeviceID) && (results[recordNo].Channel == results[recordNo-1].Channel)) {
					sameBuild++
					//results[recordNo].Rank = '#' + sameBuild
					if (sameBuild > topCnt) {
						results.remove(recordNo)
						recordNo--
					}
				} else {
					sameBuild = 1
					//results[recordNo].Rank = 'Top'
				}
			}
		}

		results.sort  { it.'Package' }

		XSSFWorkbook workbook = utilService.exportExcel(results, "")

		def fHeader = "attachment; filename=" + rName + ".xlsx"
		response.setHeader("Content-disposition", fHeader)
		response.contentType = "application/excel"
		ServletOutputStream f = response.getOutputStream()
		workbook.write(f)
		f.close()
	}
}