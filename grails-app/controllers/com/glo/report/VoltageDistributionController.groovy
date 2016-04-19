package com.glo.report

import com.mongodb.BasicDBObject
import grails.converters.JSON
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat

class VoltageDistributionController {
	
	def mongo
	def utilService

	def index = {
		
		def db = mongo.getDB("glo")
		
		int reqCurrent = params.current ? params.current.toInteger() : 10	// current for MASK5 and MASK6; it's half for MASK4
		def retField = params.var ?: "EQE"
		def top5 = params.top5 ?: "WPE"
		def lastDays = params.days ?  params.days.toInteger() : 30			// from test_data_visualization date
		def tdv = params.tdv ?  params.tdv.toInteger() : 0						// latest from test_data_visualization
		def ftv = params.ftv ?  params.ftv.toInteger() : 0						// latest from full_test_visualization
		def ranges = params.ranges ?  params.ranges.toInteger() : 0			// 0 for original 3 ranges; 1 for additional 5 ranges
		def out = params.out ?  params.out.toLowerCase() : "chart"			// "chart" for dashboard; "xls" for export
		def rName = params.rName ?: "voltage_distribution"
		
		def chart = []
		if (ranges == 0) {
			chart = ['x':'ec','xAxisType':'Category','xTitle':'Experiment + Serial#',
						 'y': [
								 ['y':'top 5 ' + retField + ' 490-510','yTitle':'top 5 ' + retField + ' 490-510','yAxisType':'Numeric'],
								 ['y':'top 5 ' + retField + ' 510-520','yTitle':'top 5 ' + retField + ' 510-520','yAxisType':'Numeric'],
								 ['y':'top 5 ' + retField + ' 520-535','yTitle':'top 5 ' + retField + ' 520-535','yAxisType':'Numeric'],
							 ],
						 'tip': ['tip'],
						 'data':[]
						]
		}
		else {
			chart = ['x':'ec','xAxisType':'Category','xTitle':'Experiment + Serial#',
				'y': [
						['y':'top 5 ' + retField + ' 440-460','yTitle':'top 5 ' + retField + ' 440-460','yAxisType':'Numeric'],
						['y':'top 5 ' + retField + ' 460-480','yTitle':'top 5 ' + retField + ' 460-480','yAxisType':'Numeric'],
						['y':'top 5 ' + retField + ' 480-500','yTitle':'top 5 ' + retField + ' 480-500','yAxisType':'Numeric'],
						['y':'top 5 ' + retField + ' 500-520','yTitle':'top 5 ' + retField + ' 500-520','yAxisType':'Numeric'],
						['y':'top 5 ' + retField + ' 520-540','yTitle':'top 5 ' + retField + ' 520-540','yAxisType':'Numeric'],
					],
				'tip': ['tip'],
				'data':[]
			   ]
		}
		
		DateFormat  dformat = new SimpleDateFormat("yyyy-MM-dd")
		
		def queryUnit = new BasicDBObject()
		
		def df = new Date() - lastDays
		def dt = new Date() + 1
		queryUnit.put("parentCode",null)
		queryUnit.put("value.productCode","100")
		queryUnit.put("value.test_data_visualization.actualStart", new BasicDBObject(['$gte':df, '$lte': dt]))
		
		def fields = new BasicDBObject()
		fields.put("code",1)
		//fields.put("value.test_data_visualization.actualStart",1)
		fields.put("value.test_data_visualization.mask",1)
		fields.put("value.test_data_visualization.testDataIndex",1)
		fields.put("value.full_test_visualization.testFullIndex",1)
		
		db.dataReport.find(queryUnit, fields).collect { unit ->
			
			def testIndexes = unit?.value?.test_data_visualization?.testDataIndex ?: []
			def fullIndexes = unit?.value?.full_test_visualization?.testFullIndex ?: []
			if (tdv == 0) testIndexes = []
			if (ftv == 0) fullIndexes = []

			def temp = utilService.getLatestTestData(unit?.code, testIndexes, fullIndexes, null)
			
			if (temp) {
				temp.collect { testRecord ->
				
					def data = testRecord['value']['data']
					def mask = unit?.value?.test_data_visualization?.mask
					def mask_current = "unknown"
					if (mask in ['MASK5', 'MASK6', 'MASK7']) mask_current = "Data @ " + reqCurrent + "mA"
					if (mask == 'MASK4') mask_current = "Data @ " + reqCurrent / 2 + "mA"
					
					// Will contain 3 or 5 elements, each containing list of devices in given peakWL ranges
					def peakDistribution = [[:],[:],[:],[:],[:]]
					
					// Loop through
					data?.each { current, currentData ->
						
						if (current == mask_current) {
							
							Set validDevices = utilService.getValidDevices (data, current)
							
							def peaks = currentData["Peak (nm)"]
							peaks.each { deviceCode, peak ->
								
								if (ranges == 0) {
									if (peak >= 490 && peak < 510 && validDevices.contains(deviceCode)) {
										peakDistribution[0].put(deviceCode, current)
									}
									if (peak >= 510 && peak < 520 && validDevices.contains(deviceCode)) {
										peakDistribution[1].put(deviceCode, current)
									}
									if (peak >= 520 && peak < 535 && validDevices.contains(deviceCode)) {
										peakDistribution[2].put(deviceCode, current)
									}
								}
								else {
									if (peak >= 440 && peak < 460 && validDevices.contains(deviceCode)) {
										peakDistribution[0].put(deviceCode, current)
									}
									if (peak >= 460 && peak < 480 && validDevices.contains(deviceCode)) {
										peakDistribution[1].put(deviceCode, current)
									}
									if (peak >= 480 && peak < 500 && validDevices.contains(deviceCode)) {
										peakDistribution[2].put(deviceCode, current)
									}
									if (peak >= 500 && peak < 520 && validDevices.contains(deviceCode)) {
										peakDistribution[3].put(deviceCode, current)
									}
									if (peak >= 520 && peak < 540 && validDevices.contains(deviceCode)) {
										peakDistribution[4].put(deviceCode, current)
									}
								}
							}
						}
					}
				
					def topWpes = [[],[],[],[],[]]
					def top5Wpes = [[],[],[],[],[]]
					
					topWpes.eachWithIndex { topWpe, idx ->
						
						if ((ranges != 0) || (idx <= 2)) {
							// Find top 5 WPE devices for each range of Peak wavelength
							// We choose which parameter decides top 5 (WPE or EQE or something else)
							peakDistribution[idx].each { deviceCode, current ->
								topWpe.add([data[current][top5][deviceCode] ?: 0, deviceCode])
							}
							topWpe = topWpe.sort { it[0] }.reverse()
							
							(0..4).collect {
								def last = topWpe[it]
								if (last) {
									top5Wpes[idx].add(last[1])
								}
							}
						}
					}
					
					def res = [:]
					//def top5Var = [[],[],[],[],[]]
					
					if (out == "xls") {
						res.put("exp", testRecord["value"]["experimentId"])
						res.put("code", testRecord["value"]["code"])
						res.put("mask", mask)
						res.put("current", mask_current)
						res.put("process step", testRecord["value"]["tkey"])
						res.put("test Id", testRecord["value"]["testId"])
						if (testRecord?.value?.date)
							res.put("date",  dformat.format(testRecord["value"]["date"]))
						else
							res.put("date",  "")
					}
					else {
						res.put("ec", testRecord["value"]["experimentId"] + "-" + testRecord["value"]["code"] )
						if (testRecord?.value?.date)
							res.put("date",  dformat.format(testRecord["value"]["date"]))
						else
							res.put("date",  "")
						//res.put("date",  dformat.format(unit?.value?.test_data_visualization?.actualStart) )
					}
					
					top5Wpes.eachWithIndex { top5Wpe, idx ->
						
						if ((ranges != 0) || (idx <= 2)) {
							def top5Var = []
							top5Wpe.each {
								if (it) {
									def current = peakDistribution[idx][it]
									top5Var.add(data[current][retField][it])
								}
							}
								
							DescriptiveStatistics stats = new DescriptiveStatistics()
							top5Var.each {
								if (it) {
									stats.addValue(it)
								}
							}
							def d = stats.getMean()
							res.put(chart.y[idx].y, d.isNaN() ? 0: d )
						}
					}
					
					// ADD TOOLTIPS
					chart['data'].add(res)
				}
			}
		}
		
		chart['data'] = chart['data'].sort {it.date}

		if (out == "xls") {
			XSSFWorkbook workbook = utilService.exportExcel(chart['data'], "")
	
			def fHeader = "attachment; filename=" + rName + ".xlsx"
			response.setHeader("Content-disposition", fHeader)
			response.contentType = "application/excel"
			ServletOutputStream f = response.getOutputStream()
			workbook.write(f)
			f.close()
		}
		else {
			def arr = [chart]
	
			render arr  as JSON
		}
	}
}
