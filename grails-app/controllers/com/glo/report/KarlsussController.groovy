package com.glo.report

import com.mongodb.BasicDBObject
import grails.converters.JSON
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream

class KarlsussController {

    def mongo
    def utilService

    def index() {

        def workbook = null
        def db = mongo.getDB("glo")
        def code =  params.code
        if (!code) {
            return ["provide 'code' url parameter"] as JSON
        }
        def queryTest = new BasicDBObject("value.parentCode", code)
        queryTest.put("value.tkey", "ni_dot_test")
        db.testData.find(queryTest, new BasicDBObject()).collect { record ->
            record.value.data.each { k, v ->
                if (k in ["setting", "Datavoltage"]) {}
                else {
                    def cl = v.getClass().toString()
                    if (cl == "class com.mongodb.BasicDBObject" && v.Spectrum && v.Spectrum.data) {
                        def results = v.Spectrum.data.collect {
                            def row = [:]
                            row["wave"] = it[0]
                            row["intens"] = it[1]
                            return row
                        }
                        workbook = utilService.exportExcel(results, "", workbook, record.value.code.tokenize("_")[1] + "_" + k)
                    }
                }
            }
        }

        def fHeader = "attachment; filename=NiDotSpectrums_" + code + ".xlsx"
        response.setHeader("Content-disposition", fHeader)
        response.contentType = "application/excel"
        ServletOutputStream f = response.getOutputStream()
        workbook.write(f)
        f.close()

    }
}
