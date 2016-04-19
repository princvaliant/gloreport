package com.glo.report

import com.mongodb.BasicDBObject
import grails.converters.JSON
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream

class TestController {
	
	def mongo
	def utilService

	def index = {
		
		def db = mongo.getDB("glo")
		
		def result = 0
		def match = new BasicDBObject()
		match.put('value.parentCode', 'SLAB00003')
        match.put('value.testId', ['$in': [150411020336,150411005329,150411000008,150410210354,150410183956,150410172954]])
	
		def project = new BasicDBObject()
		project.put('value.code', 1)
        project.put('value.testId', 1)
        project.put('value.data.Datavoltage', 1)


		def arr = db.testData.find (match, project).collect{it}

        def out = []
        arr.each {
            if (it.value.data) {
                it.value.data.Datavoltage.data.each { v ->
                    def o = [:]
                    o.code = it.value.code
                    o.testId = it.value.testId.toString()
                    o.volt = v[0]
                    o.current = v[1]
                    out.add(o)
                }
            }
        }

        XSSFWorkbook workbook = utilService.exportExcel(out, "")

        def fHeader = "attachment; filename=voltagesweeps.xlsx"
        response.setHeader("Content-disposition", fHeader)
        response.contentType = "application/excel"
        ServletOutputStream f = response.getOutputStream()
        workbook.write(f)
        f.close()
	}
}
