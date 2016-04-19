package com.glo.report

import com.mongodb.BasicDBObject
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat

//Bruno

class TopWaferDevicesController {
	
	def mongo
	def utilService

	def index = {
		
		def results = []
	
		def db = mongo.getDB("glo")
		
		def sortVar = params.var ?: "EQE"										// sorting criteria
		def sortTop = "Top5_Avg" + sortVar
		
		def topCnt = params.top ?  params.top.toInteger() : 1					// how many top devices for each wafer
		int reqCurrent = params.current ? params.current.toInteger() : 20		// current for MASK5 and MASK6; it will be half for MASK4, MASK8
		def reactor = params.reactor ?: ""
		def lastDays = params.days ?  params.days.toInteger() : 30				// from test_data_visualization date
		
		def tdv = params.tdv ?  params.tdv.toInteger() : 0						// latest from test_data_visualization
		def ttv = params.ttv ?  params.ttv.toInteger() : 0						// latest from top_test_visualization
		def ctv = params.ctv ?  params.ctv.toInteger() : 0						// latest from char_test_visualization
		def ftv = params.ftv ?  params.ftv.toInteger() : 0						// latest from full_test_visualization
		
		def peakMin = params.peakMin ?  params.peakMin.toInteger() : 500		// peak WL range
		def peakMax = params.peakMax ?  params.peakMax.toInteger() : 520
		def domMin = params.domMin ?  params.domMin.toInteger() : 1				// dom WL range
		def domMax = params.domMax ?  params.domMax.toInteger() : 999
		
		def fwhmMax = 1000	// 37
		
		def query = new BasicDBObject()
		def df = new Date() - lastDays
		def dt = new Date() + 1
		query.put("parentCode",null)
		query.put("value.productCode","100")
		query.put('$or', [
                new BasicDBObject("value.test_data_visualization.actualStart", new BasicDBObject(['$gte':df, '$lte': dt])),
                new BasicDBObject("value.nbp_test_data_visualization.actualStart", new BasicDBObject(['$gte':df, '$lte': dt]))
        ])
		
		def fields = new BasicDBObject()
		fields.put("code",1)
		fields.put("value.test_data_visualization.mask",1)
		fields.put("value.test_data_visualization.testDataIndex",1)
        fields.put("value.nbp_test_data_visualization.mask",1)
		fields.put("value.nbp_test_data_visualization.testDataIndex",1)
        fields.put("value.top_test_visualization.testTopIndex",1)
		fields.put("value.char_test_visualization.testCharIndex",1)
		fields.put("value.full_test_visualization.testFullIndex",1)
		
		fields.put("value.test_data_visualization.experimentId",1)
		fields.put("value.experimentId",1)
		fields.put("value.epi_growth.runNumber",1)
		fields.put("value.epi_growth_sweden.runNumber",1)
		fields.put("value.epi_growth.actualStart",1)
		fields.put("value.epi_growth_sweden.actualStart",1)
		
		db.dataReport.find(query, fields).collect { unit ->
			
			def waferResults = []
			def waferIndexes = getAllIndexes(unit, tdv, ttv, ctv, ftv)
			
			def mask =  unit?.value?.nbp_test_data_visualization?.mask ?: unit?.value?.test_data_visualization?.mask
			def mask_current = "unknown"
			if (mask in ['MASK5', 'MASK6', 'MASK7', 'MASK8']) mask_current = "Data @ " + reqCurrent + "mA"
			if (mask in ['MASK4','MASK8']) mask_current = "Data @ " + reqCurrent/2  + "mA"

			def epiRun = unit?.value?.epi_growth?.runNumber ?: unit?.value?.epi_growth_sweden?.runNumber
			def epiDate = unit?.value?.epi_growth?.actualStart ?: unit?.value?.epi_growth_sweden?.actualStart
			//def expId = unit?.value?.test_data_visualization?.experimentId
			def expId = unit?.value?.experimentId
			
			if (epiRun && epiDate && !(expId[0..3].toUpperCase() in ['EXP0','EXP1','EXP4','EXP5'])
				&& (reactor == "" || epiRun.toUpperCase().indexOf(reactor.toUpperCase()) >= 0)) {
				
				//def temp = utilService.getLatestTestData(unit?.code, testIndexes, fullIndexes, "value.data." + mask_current)
				
				waferIndexes.each {
					def temp = utilService.getTestData(unit?.code, it[0], it[1], "value.data." + mask_current)
					
					if (temp) {
						def testRecord = temp.collect {it}[0]
						def testResults = getTestRecordData(unit?.code, mask, testRecord, mask_current, sortVar, sortTop, peakMin, peakMax, domMin, domMax, epiRun ,epiDate ,expId, topCnt)
						if (testResults.size() != 0) {
							if (waferResults.size() == 0)
								waferResults.addAll(testResults)
							else if (testResults[0][sortTop] > waferResults[0][sortTop]) {
								waferResults.clear()
								waferResults.addAll(testResults)
							}
						}
					}
				}
			}
			results.addAll(waferResults)
		}

		// sort and identify champions
		results = results.sort { it[sortVar] }.sort { it[sortTop] }.reverse().sort { it.'Week' }
		for (int i=0; i<results.size(); i++) {
			if (i == 0 || results[i].'Week' != results[i-1].'Week') {
				results[i]["Weekly champion by " + sortTop] = results[i][sortTop]
			}
		}
		
		XSSFWorkbook workbook = utilService.exportExcel(results, "")

		def fheader = "attachment; filename=" + reqCurrent.toString() + "Acm2_top" + topCnt + "_from_" + sortTop + "_peak" + peakMin + "-" + peakMax + "nm_" + "_dom" + domMin + "-" + domMax + "nm_" + lastDays + "days.xlsx"
		response.setHeader("Content-disposition", fheader)
		response.contentType = "application/excel"
		ServletOutputStream f = response.getOutputStream()
		workbook.write(f)
		f.close()
	}
	

	private def getTestRecordData (def code, def mask, def testRecord, def mask_current, def sortVar, def sortTop, def peakMin, def peakMax, def domMin, def domMax, def epiRun ,def epiDate ,def expId, def topCnt) {
		
		def testResults = []
		DateFormat  dformat = new SimpleDateFormat("MM/dd/yyyy")
		
		def data = testRecord['value']['data']
		Set validDevices = utilService.getValidDevices (data, mask_current)
		
		validDevices.each { device ->
			def eqe = getValue(data, mask_current, "EQE", device)
			def wpe = getValue(data, mask_current, "WPE", device)
			def peak = getValue(data, mask_current, "Peak (nm)", device)
			def dom = getValue(data, mask_current, "Dominant wl (nm)", device)
			def fwhm = getValue(data, mask_current, "FWHM (nm)", device)
			
			if (peak >= peakMin && peak < peakMax && eqe > -90 && dom >= domMin && dom < domMax) {
				def result = [:]
				result.Wafer = code
				result.Die = device
				result.Current = mask_current
				result.Mask = mask
				result."Exp ID" = expId

				result.EQE = eqe
				result."Top5_AvgEQE" = 0
				result.WPE = wpe
				result."Top5_AvgWPE" = 0
				
				result."FWHM (nm)" = fwhm
				
				result."Peak WL" = peak
				result."Top5_AvgPeakWL" = 0
				result."Dom WL" = dom
				result."Top5_AvgDomWL" = 0
				
				result."EPI Run #" = epiRun
				
				def epiDate1 = dformat.format(epiDate)
				result."EPI date" = epiDate1
				result."EPI Date" = epiDate
				def yy = dformat.parse(epiDate1)[Calendar.YEAR]
				def ww = dformat.parse(epiDate1)[Calendar.WEEK_OF_YEAR]
				result."Week" = String.format('%4d-%2d', yy, ww)
				result["Weekly champion by " + sortTop] = ""
				
				result."process step" = testRecord["value"]["tkey"]
				result."test Id" = testRecord["value"]["testId"].toString()
				
				// NEW requirements
				result.NEW = "****"
				result."Pulse Voltage (V)" = getValue(data, mask_current, "Pulse Voltage (V)", device)
				result."Current @ 2V" = getValue(data, "Current @ 2V", "NA", device)
				
				result."EQE @ 10mA" = getValue(data, "Data @ 10mA", "EQE", device)
				result."Peak (nm) @ 10mA" = getValue(data, "Data @ 10mA", "Peak (nm)", device)
				result."FWHM (nm) @ 10mA" = getValue(data, "Data @ 10mA", "FWHM (nm)", device)
				result."Pulse Voltage (V) @ 10mA" = getValue(data, "Data @ 10mA", "Pulse Voltage (V)", device)
					
				result."bestEQE" = ""
				result."bestEQE current" = ""
				result."bestEQE Pulse Current (mA)" = ""
				
				testResults.add(result)
			}
		}
		
		def totalcnt = testResults.size()
		if (totalcnt > 0) {
			def cnt = (totalcnt < 5) ? totalcnt : 5
			
			testResults = testResults.sort { it[sortVar] }.reverse()
			def avgEQE = 0
			def avgWPE = 0
			def avgPeak = 0
			def avgDom = 0
			
			for (int i=0; i<cnt; i++) {
				avgEQE += testResults[i].EQE
				avgWPE += testResults[i].WPE
				avgPeak += testResults[i]."Peak WL"
				avgDom += testResults[i]."Dom WL"
			}
			avgEQE /= cnt
			avgWPE /= cnt
			avgPeak /= cnt
			avgDom /= cnt
			
			if (cnt > topCnt && topCnt > 0) cnt = topCnt
			for (int i=0; i<cnt; i++) {
				testResults[i]."Top5_AvgEQE" = avgEQE
				testResults[i]."Top5_AvgWPE" = avgWPE
				testResults[i]."Top5_AvgPeakWL" = avgPeak
				testResults[i]."Top5_AvgDomWL" = avgDom
				
				/***
				def temp1 = utilService.getBestEQETestData(testResults[i].Wafer, waferIndexes, testResults[i].Die)
				if (temp1.bestEQEcurrent) {
					testResults[i]."bestEQE" =  temp1.bestEQE
					testResults[i]."bestEQE current" = temp1.bestEQEcurrent
					testResults[i]."bestEQE Pulse Current (mA)" = temp1.bestEQEpulsecurrent
				}
				***/
			}
			return (testResults[0..cnt-1])
		}
		
		return (testResults)
	}

	
	def getAllIndexes(def unit, def tdv, def ttv, def ctv, def ftv) {
		def allIndexes = []
		
		if (tdv != 0) {
			def testIndexes = (unit?.value?.test_data_visualization?.testDataIndex) ?: []
			testIndexes.each {
				allIndexes.add(['test_data_visualization', it])
			}
		}
        if (tdv != 0) {
            def testIndexes = (unit?.value?.nbp_test_data_visualization?.testDataIndex) ?: []
            testIndexes.each {
                allIndexes.add(['nbp_test_data_visualization', it])
            }
        }
		if (ttv != 0) {
			def topIndexes = unit?.value?.top_test_visualization?.testTopIndex ?: []
			topIndexes.each {
				allIndexes.add(['top_test_visualization', it])
			}
		}
		if (ctv != 0) {
			def charIndexes = unit?.value?.char_test_visualization?.testCharIndex ?: []
			charIndexes.each {
				allIndexes.add(['char_test_visualization', it])
			}
		}
		if (ftv != 0) {
			def fullIndexes = unit?.value?.full_test_visualization?.testFullIndex ?: []
			fullIndexes.each {
				allIndexes.add(['full_test_visualization', it])
			}
		}

		allIndexes
	}

	def getValue(def objFrom, def k1, def k2, def k3) {
		def ret = -99
		
		if (objFrom)
			if (objFrom[k1])
				if (objFrom[k1][k2])
					if (objFrom[k1][k2][k3])
						ret = objFrom[k1][k2][k3]
		ret
	}
}