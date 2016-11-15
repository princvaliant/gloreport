package com.glo.report

import com.mongodb.BasicDBObject
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream

class NiDotController {

    def mongo
    def utilService

    def index() {

        def results = []
        def db = mongo.getDB("glo")

        def dback = params.daysback ?: "90"
        def queryTest = new BasicDBObject("value.tkey", "ni_dot_test")
        queryTest.put("value.date", new BasicDBObject('$gt', new Date().minus(dback.toInteger())))
        db.testData.find(queryTest).collect { data ->

            def obj = [:]
            data.value.each { k, v ->
                if (k != "data" && v) {
                    if (k == "code") {
                        v = v.tokenize("_")[1]
                    }
                    obj.put(k, v)
                }
            }
            [100, 200, 400, 600, 800, 1, 4, 5, 10].each { curr ->
                def cnt = 0
                def currStr = ''
                if (curr < 50) {
                    currStr = curr + "mA"
                } else {
                    currStr = curr + "uA"
                }
                def dc = data.value.data["Data @ " + currStr]
                if (dc) {
                    obj.put("Peak (nm) @ " + currStr, dc["Peak (nm)"])
                }
            }
            results.add(obj)
        }

        def cols = [
                'parentCode',
                'code',
                'tkey',
                'date',
                'rpp',
                'vbp',
                'v02',
                'wpe02',
                'eqe02',
                'v04',
                'wpe04',
                'eqe04',
                'v06',
                'wpe06',
                'eqe06',
                'v08',
                'wpe08',
                'eqe08',
                'v1',
                'wpe1',
                'eqe1',
                'v4',
                'wpe4',
                'eqe4',
                'v5',
                'wpe5',
                'eqe5',
                'Peak WPE (%)',
                'Peak EQE (%)',
                'J @ Peak WPE (A/cm2)',
                'J @ Peak EQE (A/cm2)',
                'EQE leak WL corr @ 1 mA',
                'EQE leak WL corr @ 4 mA',
                'EQE leak WL corr @ 5 mA',
                'Peak (nm) @ 200uA',
                'Peak (nm) @ 400uA',
                'Peak (nm) @ 600uA',
                'Peak (nm) @ 800uA',
                'Peak (nm) @ 1mA',
                'Peak (nm) @ 4mA',
                'Peak (nm) @ 5mA']

        XSSFWorkbook workbook = utilService.exportExcel(results, "", null, cols)

        def fHeader = "attachment; filename=NiDotData.xlsx"
        response.setHeader("Content-disposition", fHeader)
        response.contentType = "application/excel"
        ServletOutputStream f = response.getOutputStream()
        workbook.write(f)
        f.close()
    }

//    def index() {
//
//        def results = []
//        def db = mongo.getDB("glo")
//
//        def codes =  params.code.tokenize(",")
//
//        codes.each { code ->
//
//            def queryTest = new BasicDBObject("value.code", new BasicDBObject('$regex', "^" + code))
//            queryTest.put("value.tkey", "ni_dot_test")
//            db.testData.find(queryTest, new BasicDBObject()).collect { data ->
//
//                [1, 4, 5, 10, 20].each { curr ->
//
//                    def row = [:]
//                    def c = data.value.code.tokenize("_")
//                    row.code = c[0]
//                    row.nidot = c[1]
//                    row.current = curr
//
//                    data.value.data["Data @ " + curr + "mA"].Spectrum.data.collect {
//                        int i = Math.round(it[0])
//                        if (!row.containsKey(i)) {
//                            row.put(i.toString(), it[1])
//                        }
//                    }
//                    results.add(row)
//                }
//            }
//        }
//
//        XSSFWorkbook workbook = utilService.exportExcel(results, "", null)
//
//        def fHeader = "attachment; filename=NiDotSpectrums.xlsx"
//        response.setHeader("Content-disposition", fHeader)
//        response.contentType = "application/excel"
//        ServletOutputStream f = response.getOutputStream()
//        workbook.write(f)
//        f.close()
//
//    }
}
