package com.glo.report

import com.mongodb.BasicDBObject
import grails.converters.JSON
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

import java.text.DateFormat
import java.text.SimpleDateFormat

class PeakRangePerGrowthController {
	
	def mongo
	def utilService

	def index = {
		
		def db = mongo.getDB("glo")
		
		int reqCurrent = params.current ? params.current.toInteger() : 5
		def retField = params.var ?: "WPE"
		def reactor = params.reactor ?: ""
		def lastDays = params.days ?  params.days.toInteger() : 30

		def chart = ['x':'ec','xAxisType':'Category','xTitle':'Run number + Serial number',
					 'y': [
							 ['y':'top 5 ' + retField + ' 490-510','yTitle':'top 5 ' + retField + ' 490-510','yAxisType':'Numeric'],
							 ['y':'top 5 ' + retField + ' 510-520','yTitle':'top 5 ' + retField + ' 510-520','yAxisType':'Numeric'],
							 ['y':'top 5 ' + retField + ' 520-535','yTitle':'top 5 ' + retField + ' 520-535','yAxisType':'Numeric'],
						 ],
					 'tip': ['tip'],
					 'data':[]
					]
		
		int i = 0
		DateFormat  dformat = new SimpleDateFormat("yyyy-MM-dd")
		
		def queryUnit = new BasicDBObject()
			
		def df = new Date() - lastDays
		def dt = new Date() + 1
		queryUnit.put("parentCode",null)
		queryUnit.put("value.productCode","100")
		queryUnit.put("value.test_data_visualization.actualStart", new BasicDBObject(['$gte':df, '$lte': dt]))
		
		def fields = new BasicDBObject()
		fields.put("code",1)
		fields.put("value.epi_growth.runNumber",1)
		fields.put("value.epi_growth_sweden.runNumber",1)
		fields.put("value.test_data_visualization.mask",1)
		fields.put("value.test_data_visualization.testDataIndex",1)
		fields.put("value.full_test_visualization.testFullIndex",1)
		
		def runNumbers = new TreeMap()
			
		db.dataReport.find(queryUnit, fields).collect { unit ->
			
			def testIndexes = unit?.value?.test_data_visualization?.testDataIndex ?: []
			def fullIndexes = unit?.value?.full_test_visualization?.testFullIndex ?: []
			def temp = utilService.getLatestTestData(unit?.code, testIndexes, fullIndexes, null)
			
			def run = (unit?.value?.epi_growth?.runNumber?.toUpperCase() ?: unit?.value?.epi_growth_sweden?.runNumber?.toUpperCase()) + " - " + unit?.code
			
			if (temp && run && (reactor == "" || run.indexOf(reactor.toUpperCase()) >= 0)) {
			
				temp.collect { testRecord ->
				
					def data = testRecord['value']['data']
					
					// Will contain 3 elements each containing list of devices: 0 490-509, 1 509-520, 2 520-535
					def peakDistribution = [[:],[:],[:]]
					
					// Loop through
					data?.each { current, currentData ->
						
						if ((unit?.value?.test_data_visualization?.mask in ['MASK5', 'MASK6', 'MASK7'] && current == "Data @ " + reqCurrent * 2 + "mA") ||
							(unit?.value?.test_data_visualization?.mask == 'MASK4' && current == "Data @ " + reqCurrent  + "mA")) {
							
							Set validDevices = utilService.getValidDevices (data, current)
	
							
							def peaks = currentData["Peak (nm)"]
							peaks.each { deviceCode, peak ->
								
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
						}
					}
					i++
				
					def topWpes = [[],[],[]]
					def top5Wpes = [[],[],[]]
								
					topWpes.eachWithIndex { topWpe, idx ->
						
						// Find top 5 WPE devices for each range of Peak wavelength
						peakDistribution[idx].each { deviceCode, current ->
							topWpe.add([data[current]["WPE"][deviceCode] ?: 0, deviceCode])
						}
						topWpe = topWpe.sort { it[0] }.reverse()
												
						(0..4).collect {
							def last = topWpe[it]
							if (last) {
								top5Wpes[idx].add(last[1])
							}
						}
					}
					
					def res = [:]
					//def top5Var = [[],[],[]]
					
					res.put("ec", run )
					
					top5Wpes.eachWithIndex { top5Wpe, idx ->
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
					
					chart['data'].add(res)
				}
			}
		}
		
		chart['data'] = chart['data'].sort {it.ec}

		def arr = [chart]

		render arr  as JSON
	}
}
