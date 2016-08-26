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

        def codes =  params.code.tokenize(",")

        codes.each { code ->

            def queryTest = new BasicDBObject("value.code", new BasicDBObject('$regex', "^" + code))
            queryTest.put("value.tkey", "ni_dot_test")
            db.testData.find(queryTest, new BasicDBObject()).collect { data ->

                [1, 4, 5, 10, 20].each { curr ->

                    def row = [:]
                    def c = data.value.code.tokenize("_")
                    row.code = c[0]
                    row.nidot = c[1]
                    row.current = curr

                    data.value.data["Data @ " + curr + "mA"].Spectrum.data.collect {
                        int i = Math.round(it[0])
                        if (!row.containsKey(i)) {
                            row.put(i.toString(), it[1])
                        }
                    }
                    results.add(row)
                }
            }
        }

        XSSFWorkbook workbook = utilService.exportExcel(results, "", null)

        def fHeader = "attachment; filename=NiDotSpectrums.xlsx"
        response.setHeader("Content-disposition", fHeader)
        response.contentType = "application/excel"
        ServletOutputStream f = response.getOutputStream()
        workbook.write(f)
        f.close()

    }
}
