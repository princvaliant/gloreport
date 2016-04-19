package com.glo.report

import com.mongodb.BasicDBObject
import grails.converters.JSON
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

import javax.servlet.ServletOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat

// testData vs. measures

class MeasuresController {
	
	def mongo
	def utilService
    def currents = ['9','32','66','116']

	def index = {

		def db = mongo.getDB("glo")
        def tkey = "test_data_visualization"

		def fields = new BasicDBObject()
        fields.put("WaferID", 1)
        fields.put("C", 1)
		fields.put("eqe", 1)
		fields.put("wpe", 1)
        fields.put("PeakWavelength", 1)
        fields.put("FWHM", 1)
        fields.put("Volt", 1)
		
		def query = new BasicDBObject()
        query.put("TestType", tkey)
        query.put("WaferID", ['$ne': ''])
        query.put("DeviceID", ['$regex': /^060500|^060400|^030500|^030400/])
        query.put("ResultType", "CurrentSweepGreen")
        query.put("mask", "MASK28")
        query.put("C", ['$in': currents])
        def wids = [:]
    	def s = db.measures.find(query, fields).collect {
           if (!wids[it.WaferID]) {
               wids[it.WaferID] = this.getRes()
           }
           wids[it.WaferID][it.C].eqe.addValue(it.eqe)
           wids[it.WaferID][it.C].wpe.addValue(it.wpe)
           wids[it.WaferID][it.C].volt.addValue(it.Volt)
           wids[it.WaferID][it.C].fwhm.addValue(it.FWHM)
           wids[it.WaferID][it.C].peak.addValue(it.PeakWavelength)
        }
        def results = []
        wids.each { k, v ->
            def obj = [:]
            obj.code = k
            v.each { curr, stats ->
                stats.each { p, vals ->
                    obj[p + curr + '_std'] = vals.getStandardDeviation()
                    if (obj[p + curr + '_std'] && obj[p + curr + '_std'].isNaN()) {
                        obj[p + curr + '_std'] = 0
                    }
                    obj[p + curr + '_mean'] = vals.getMean()
                    if (obj[p + curr + '_mean'] && obj[p + curr + '_mean'].isNaN()) {
                        obj[p + curr + '_mean'] = 0
                    }
                }
            }
            results.add(obj)
        }
        if (params.json) {
            render results as JSON
        } else {
            XSSFWorkbook workbook = utilService.exportExcel(results, "")
            def fheader = "attachment; filename=measurestests.xlsx"
            response.setHeader("Content-disposition", fheader)
            response.contentType = "application/excel"
            ServletOutputStream f = response.getOutputStream()
            workbook.write(f)
            f.close()
        }
	}

    private def getRes() {
        def res = [:]
        currents.each {
            res[it] = [:]
            res[it].eqe = new DescriptiveStatistics()
            res[it].wpe = new DescriptiveStatistics()
            res[it].volt = new DescriptiveStatistics()
            res[it].fwhm = new DescriptiveStatistics()
            res[it].peak = new DescriptiveStatistics()
        }
        return res;
    }
}