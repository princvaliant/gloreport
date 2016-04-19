package com.glo.report

import com.mongodb.BasicDBObject
import grails.converters.JSON
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream

//Bruno

class CdsemMeasureDataController {
	
	def mongo
	def utilService
	
	def index = {
		
		def ps = params.ps ? params.ps.toUpperCase() : "FICD"
		def stamps = params.stamps ? params.stamps.toUpperCase().tokenize(',') : []
		def codes = params.codes ? params.codes.tokenize(',') : []
		def lastDays = params.days ?  params.days.toInteger() : 30
		def df = new Date() - lastDays
		def dt = new Date() + 1
		def out = params.out ?  params.out.toLowerCase() : "chart"			// "chart" for dashboard; "raw" for Raw data; "stat" for Stat data
		
		def allRawData = []
		def allStatData = []
		
		def db = mongo.getDB("glo")
		def query = new BasicDBObject()
		
		def path
		if (ps == "DICD") {
			path = 'value.dicd_meas.'
			query.put(path + 'dicd_measured', 'YES')
		}
		else {
			path = 'value.ficd_meas.'
			query.put(path + 'ficd_measured', 'YES')
		}
		query.put("parentCode", null)
		if (codes.size() != 0) query.put('code',new BasicDBObject(['$in':codes]))
		if (stamps.size() != 0) query.put(path + 'stampID',new BasicDBObject(['$in':stamps]))
		query.put(path + "actualStart", new BasicDBObject(['$gte':df, '$lte': dt]))
		
		db.dataReport.find (query).collect{ unit ->
			def unitRawData = []
			def temp = []
			def data
			def finalID,finalOD
			
			if (ps == 'DICD') {
				data = unit?.value?.dicd_meas
				temp = data?.dicdViaSizeRawData
				finalID = data?.did_via_size ?: 0
				finalOD = data?.od_via_size ?: 0
			}
			else {
				data = unit?.value?.ficd_meas
				temp = data?.ficdViaSizeRawData
				finalID = data?.fid_via_size ?: 0
				finalOD = data?.fod_via_size ?: 0
			}
			
			if (temp) {
				temp.each {
					def record = [:]
					record.Wafer = unit.code
					record.Location = '(' + it.locX + '-' + it.locY.padLeft(2,'0') + ')'
					record.Stamp = data?.stampID
					record.Date = data?.actualStart
					record.OD = finalOD.round(1)
					record.ID = finalID.round(1)
					record.Type = it.type
					record.ViaNo = it.viaNo
					record.Measurement = it.measurement
					unitRawData.add(record)
				}
			}
			def tempMap = unitRawData.groupBy { it.Location }

			//if (unitRawData.size() == 30) {
				allRawData.addAll(unitRawData)
				
				tempMap.each() { key, values ->
					def ODmeas = values.findAll { w -> w.Type == 'OD'}.collect { it.Measurement }
					DescriptiveStatistics ODstats = new DescriptiveStatistics((double[]) ODmeas)
					
					def IDmeas = values.findAll { w -> w.Type == 'ID'}.collect { it.Measurement }
					DescriptiveStatistics IDstats = new DescriptiveStatistics((double[]) IDmeas)
					
					def record = [:]
					record.Wafer = values[0].Wafer
					record.Location = key
					record.Num_of_locations = unitRawData.size() / IDmeas.size()
					record.Num_of_vias = IDmeas.size()
					record.Stamp = values[0].Stamp
					record.Date = values[0].Date
					record['OD'] = values[0].OD
					record['OD mean+sigma'] = (ODstats.getMean() + ODstats.getStandardDeviation()).round(1)
					record['OD median'] = ODstats.getPercentile(50).round(1)
					record['OD mean-sigma'] = (ODstats.getMean() - ODstats.getStandardDeviation()).round(1)
					record['ID'] = values[0].ID
					record['ID mean+sigma'] = (IDstats.getMean() + IDstats.getStandardDeviation()).round(1)
					record['ID median'] = IDstats.getPercentile(50).round(1)
					record['ID mean-sigma'] = (IDstats.getMean() - IDstats.getStandardDeviation()).round(1)
					allStatData.add(record)
				}
			//}
		}
		allStatData.sort { it.Location }.sort { it.Date }
		
		def title = ps + ' raw data'
		if (params.stamps) title += '-' + params.stamps
		title += '-last ' + lastDays + ' days'

		if (out == "chart") {
			def chart = []
			chart = ['x':'Location','xAxisType':'Category','xTitle':title,
						 'y': [
								 ['y':'OD','yTitle':'OD','yAxisType':'Numeric'],
								 ['y':'OD mean+sigma','yTitle':'OD mean+sigma','yAxisType':'Numeric'],
								 ['y':'OD median','yTitle':'OD median','yAxisType':'Numeric'],
								 ['y':'OD mean-sigma','yTitle':'OD mean-sigma','yAxisType':'Numeric'],
								 ['y':'ID','yTitle':'ID','yAxisType':'Numeric'],
								 ['y':'ID mean+sigma','yTitle':'ID mean+sigma','yAxisType':'Numeric'],
								 ['y':'ID median','yTitle':'ID median','yAxisType':'Numeric'],
								 ['y':'ID mean-sigma','yTitle':'ID mean-sigma','yAxisType':'Numeric'],
							 ],
						 'tip': ['tip'],
						 'data':[]
						]
			chart['data'].addAll(allStatData)
			
			def arr = [chart]
			render arr  as JSON
		}
		else {
			XSSFWorkbook workbook
			
			if (out == "raw")
				workbook= utilService.exportExcel(allRawData, "")
			else
				workbook = utilService.exportExcel(allStatData, "")
		
			title = title.replaceAll(',','')
			title = title.replaceAll(' ','_')
			def fheader = "attachment; filename=" + title + ".xlsx"
			response.setHeader("Content-disposition", fheader)
			response.contentType = "application/excel"
			ServletOutputStream f = response.getOutputStream()
			workbook.write(f)
			f.close()
		}
	}
}