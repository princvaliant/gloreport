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
        def device =  params.device
        def tkey =  params.tkey
        if (!code) {
            return ["provide 'code' url parameter"] as JSON
        }
        if (!device) {
            return ["provide 'device' url parameter"] as JSON
        }
        if (!tkey) {
           tkey = "test_data_visualization"
        }
        def queryTest = new BasicDBObject("value.code", code + "_" + device)
        queryTest.put("value.tkey", tkey)
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

        def fHeader = "attachment; filename=Spectrums_" + code + ".xlsx"
        response.setHeader("Content-disposition", fHeader)
        response.contentType = "application/excel"
        ServletOutputStream f = response.getOutputStream()
        workbook.write(f)
        f.close()

    }

    def niDot() {

        def workbook = null
        def db = mongo.getDB("glo")
        def code =  params.code
        def tkey =  params.tkey
        if (!code) {
            return ["provide 'code' url parameter"] as JSON
        }
        if (!tkey) {
            tkey = "ni_dot_test"
        }
        def queryTest = new BasicDBObject("value.parentCode", code)
        queryTest.put("value.tkey", tkey)
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

        def fHeader = "attachment; filename=Spectrums_" + code + ".xlsx"
        response.setHeader("Content-disposition", fHeader)
        response.contentType = "application/excel"
        ServletOutputStream f = response.getOutputStream()
        workbook.write(f)
        f.close()

    }

}
