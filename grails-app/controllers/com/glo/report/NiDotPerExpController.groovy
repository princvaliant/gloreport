package com.glo.report

import com.mongodb.BasicDBObject
import grails.converters.JSON
import java.text.DateFormat
import java.text.SimpleDateFormat



class NiDotPerExpController {
	
	def mongo
	def utilService

	def index = {
		
		def db = mongo.getDB("glo")
		
		int reqCurrent = params.current ? params.current.toInteger() : 5
		def retField = params.var ?: "voltage"
		def reactor = params.reactor ?: ""
		def lastDays = params.days ?  params.days.toInteger() : 10
		
		def chart1 = ['x':'ec','xAxisType':'Category','xTitle':' Serial number',
					 'y': [
							 ['y':'voltage10_1','yTitle':'Voltage at 10mA [V]','yAxisType':'Numeric'],
							 ['y':'voltage10_2','yTitle':'Voltage at 10mA [V]','yAxisType':'Numeric'],
							 ['y':'voltage10_3','yTitle':'Voltage at 10mA [V]','yAxisType':'Numeric'],
							 ['y':'voltage10_4','yTitle':'Voltage at 10mA [V]','yAxisType':'Numeric']
							 
						 ],
					 'tip': ['tip'],
					 'data':[]
					]
		def chart2 = ['x':'ec','xAxisType':'Category','xTitle':' Serial number',
			'y': [			
					['y':'eqe10','yTitle':'EQE at 10mA','yAxisType':'Numeric']
				],
			'tip': ['tip'],
			'data':[]
		   ]
		def chart3 = ['x':'ec','xAxisType':'Category','xTitle':' Serial number',
			'y': [
					['y':'peak10','yTitle':'Peak at 10mA [nm]','yAxisType':'Numeric']
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
		queryUnit.put("value.probe_test.actualStart", new BasicDBObject(['$gte':df, '$lte': dt]))
		
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
			query.put("value.tkey", "probe_test")
				
			def run = (unit?.value?.epi_growth?.runNumber?.toUpperCase() ?: unit?.value?.epi_growth_sweden?.runNumber?.toUpperCase()) + " - " + unit?.code
			
						
			def temp = db.testData.find(query, new BasicDBObject()).collect { testRecord ->
				
					def data = testRecord['value']['data']
					def value = testRecord['value']
					
					def res1 = [:]
					def res2 = [:]
					def res3 = [:]
					
					res1.put('ec', testRecord.value.parentCode)
					res1.put('date',  dformat.format(unit?.value?.probe_test?.actualStart))		
					res1.put('tip', testRecord.value.code)
					res1.put('voltage10', !value.v10 || ((float)value.v10)?.isNaN() ? 0 :  value.v10)
					
					res2.put('ec', testRecord.value.parentCode)
					res2.put('date',  dformat.format(unit?.value?.probe_test?.actualStart))
					res2.put('tip', testRecord.value.code)
					res2.put('eqe10', !value["EQE leak WL corr @ 10 mA"] || ((float)value["EQE leak WL corr @ 10 mA"])?.isNaN() ? 0 :  value["EQE leak WL corr @ 10 mA"])
					
					res3.put('ec', testRecord.value.code)
					res3.put('date',  dformat.format(unit?.value?.probe_test?.actualStart))
					res3.put('tip', testRecord.value.code)
					def pk =  data['Data @ 10mA'] ? data['Data @ 10mA']["Peak (nm)"] : 0
					res3.put('peak10', !pk || ((float)pk)?.isNaN()  ? 0 :  pk)


					chart1['data'].add(res1)
					chart2['data'].add(res2)
					chart3['data'].add(res3)
			}
		}
		
		chart1['data'] = chart1['data'].sort {it.date}
		chart2['data'] = chart2['data'].sort {it.date}
		chart3['data'] = chart3['data'].sort {it.date}

		def arr = [chart1,chart2,chart3]

		render arr  as JSON
	}
}
