package com.glo.report

import com.mongodb.BasicDBObject
import grails.converters.JSON
import java.text.DateFormat
import java.text.SimpleDateFormat


class NiDotPerGrowthController {
	
	def mongo
	def utilService

	def index = {
		
		def db = mongo.getDB("glo")
		
		int reqCurrent = params.current ? params.current.toInteger() : 10
		def retField = params.var ?: ""
		def reactor = params.reactor ?: ""
		def runType = params.runType ?: ""
		def lastDays = params.days ?  params.days.toInteger() : 30
		
		def chart1 = ['x':'ec','xAxisType':'Category','xTitle':' Serial number',
					 'y': [
							 ['y':'1_volt' + reqCurrent,'yTitle':'Voltage [V]','yAxisType':'Numeric'],
							 ['y':'2_volt' + reqCurrent,'yTitle':'Voltage [V]','yAxisType':'Numeric'],
							 ['y':'3_volt' + reqCurrent,'yTitle':'Voltage [V]','yAxisType':'Numeric'],
							 ['y':'4_volt' + reqCurrent,'yTitle':'Voltage [V]','yAxisType':'Numeric'],
							 ['y':'5_volt' + reqCurrent,'yTitle':'Voltage [V]','yAxisType':'Numeric'],
							 ['y':'6_volt' + reqCurrent,'yTitle':'Voltage [V]','yAxisType':'Numeric'],
							 ['y':'7_volt' + reqCurrent,'yTitle':'Voltage [V]','yAxisType':'Numeric'],
							 ['y':'8_volt' + reqCurrent,'yTitle':'Voltage [V]','yAxisType':'Numeric'],
							 ['y':'9_volt' + reqCurrent,'yTitle':'Voltage [V]','yAxisType':'Numeric']
						 ],
					 'tip': ['tip'],
					 'data':[]
					]
		def chart2 = ['x':'ec','xAxisType':'Category','xTitle':' Serial number',
			'y': [			
					['y':'1_eqe' + reqCurrent,'yTitle':'EQE','yAxisType':'Numeric'],
					['y':'2_eqe' + reqCurrent,'yTitle':'EQE','yAxisType':'Numeric'],
					['y':'3_eqe' + reqCurrent,'yTitle':'EQE','yAxisType':'Numeric'],
					['y':'4_eqe' + reqCurrent,'yTitle':'EQE','yAxisType':'Numeric'],
					['y':'5_eqe' + reqCurrent,'yTitle':'EQE','yAxisType':'Numeric'],
					['y':'6_eqe' + reqCurrent,'yTitle':'EQE','yAxisType':'Numeric'],
					['y':'7_eqe' + reqCurrent,'yTitle':'EQE','yAxisType':'Numeric'],
					['y':'8_eqe' + reqCurrent,'yTitle':'EQE','yAxisType':'Numeric'],
					['y':'9_eqe' + reqCurrent,'yTitle':'EQE','yAxisType':'Numeric']
				],
			'tip': ['tip'],
			'data':[]
		   ]
		def chart3 = ['x':'ec','xAxisType':'Category','xTitle':' Serial number',
			'y': [
					['y':'1_peak' + reqCurrent,'yTitle':'Peak [nm]','yAxisType':'Numeric'],
					['y':'2_peak' + reqCurrent,'yTitle':'Peak [nm]','yAxisType':'Numeric'],
					['y':'3_peak' + reqCurrent,'yTitle':'Peak [nm]','yAxisType':'Numeric'],
					['y':'4_peak' + reqCurrent,'yTitle':'Peak [nm]','yAxisType':'Numeric'],
					['y':'5_peak' + reqCurrent,'yTitle':'Peak [nm]','yAxisType':'Numeric'],
					['y':'6_peak' + reqCurrent,'yTitle':'Peak [nm]','yAxisType':'Numeric'],
					['y':'7_peak' + reqCurrent,'yTitle':'Peak [nm]','yAxisType':'Numeric'],
					['y':'8_peak' + reqCurrent,'yTitle':'Peak [nm]','yAxisType':'Numeric'],
					['y':'9_peak' + reqCurrent,'yTitle':'Peak  [nm]','yAxisType':'Numeric']
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
		queryUnit.put("value.ni_dot_test.actualStart", new BasicDBObject(['$gte':df, '$lte': dt]))
	
		def fields = new BasicDBObject()
		fields.put("code",1)
		fields.put("value.epi_growth.runNumber",1)
		fields.put("value.epi_growth_sweden.runNumber",1)
		fields.put("value.test_data_visualization.mask",1)
		fields.put("value.ni_dot_test.actualStart",1)
		
		def runNumbers = new TreeMap()
			
		db.dataReport.find(queryUnit, fields).collect { unit ->
			
			def query = new BasicDBObject()
			query.put("value.parentCode", unit?.code)
			query.put("value.tkey", "ni_dot_test")
			
			def runNumber = unit?.value?.epi_growth?.runNumber?.toUpperCase() ?: unit?.value?.epi_growth_sweden?.runNumber?.toUpperCase()
			def runTypeDb = db.epiRun.find (['runNumber':runNumber], ['runtype':1]).collect { it.runtype }[0]
			
			if (runType == "" || runTypeDb == runType) { 
				
				def run = runNumber + " - " + unit?.code
				
				def temp = db.testData.find(query, new BasicDBObject()).collect {it}
				
				def res1 = [:]
				def res2 = [:]
				def res3 = [:]
				
				res1.put('ec',  unit?.code)
				res1.put('date',  dformat.format(unit?.value?.ni_dot_test?.actualStart))
				res2.put('ec',  unit?.code)
				res2.put('date',  dformat.format(unit?.value?.ni_dot_test?.actualStart))
				res3.put('ec',  unit?.code)
				res3.put('date',  dformat.format(unit?.value?.ni_dot_test?.actualStart))
				def tip1 = ""
				def tip2 = ""
				def tip3 = ""
				def j = 1
			
				temp.each { testRecord ->
					
						def data = testRecord['value']['data']
						def value = testRecord['value']
		
						def v = !value['v' +  reqCurrent] || ((float)value['v' +  reqCurrent])?.isNaN() ? 0 :  value['v' +  reqCurrent].round(3)
						res1.put(j + '_volt' + reqCurrent, v)
						tip1 = tip1 + " " + j + "_" + value.code + ": " + v + "<br/>"
						
						def e = !value['EQE leak WL corr @ '+  reqCurrent + ' mA'] || ((float)value['EQE leak WL corr @ '+  reqCurrent + ' mA'])?.isNaN() ? 0 :  value['EQE leak WL corr @ '+  reqCurrent + ' mA'].round(3)
						res2.put(j + '_eqe' + reqCurrent, e)
						tip2 = tip2 + " " + j + "_" + value.code + ": " + e + "<br/>"
						
						def pk =  data['Data @ ' + reqCurrent + 'mA'] ? data['Data @ ' + reqCurrent + 'mA']["Peak (nm)"].round(3) : 0
						res3.put(j + '_peak' + reqCurrent, !pk || ((float)pk)?.isNaN()  ? 0 :  pk)
						tip3 = tip3 + " " + j + "_" + value.code + ": " + pk + "<br/>"
						
						j++
				}
				
				res1.put('tip', tip1)
				chart1['data'].add(res1)
				res2.put('tip', tip2)
				chart2['data'].add(res2)
				res3.put('tip', tip3)
				chart3['data'].add(res3)
			}
		}
		
		chart1['data'] = chart1['data'].sort {it.date}
		chart2['data'] = chart2['data'].sort {it.date}
		chart3['data'] = chart3['data'].sort {it.date}


		def arr = [] 
		if (retField.indexOf("volt") >= 0)
			arr.add(chart1)
		if (retField.indexOf("eqe") >= 0)
			arr.add(chart2)
		if (retField.indexOf("peak") >= 0)
			arr.add(chart3)

		render arr  as JSON
	}
}
