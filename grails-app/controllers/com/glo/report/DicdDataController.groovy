package com.glo.report

import com.glo.mongo.*
import com.glo.ndo.*
import com.glo.security.*
import com.mongodb.BasicDBObject
import com.mongodb.BasicDBList
import com.mongodb.DBCollection
import grails.plugin.jms.*
import org.activiti.engine.impl.bpmn.behavior.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream

//Bruno
//import javax.jms.Message
class DicdDataController {

    def mongo
    def utilService

    def index = {

        def db = mongo.getDB("glo")

        def query = new BasicDBObject()
        query.put('parentCode', null)
        query.put('unit.productCode', "100")
        query.put('value.dicd_meas', new BasicDBObject('$exists': true))

        def fields = new BasicDBObject()
        fields.put('code', 1)
        fields.put('value.dicd_meas.dicdViaSizeRawData', 1)
        fields.put('value.ficd_meas.ficdViaSizeRawData', 1)
        fields.put('value.nil.stampNumber', 1)
        fields.put('value.nil.stampID', 1)

        def results = []
        db.dataReport.find(query, fields).collect {
            def obj = [:]

            if (it.value?.dicd_meas?.dicdViaSizeRawData?.getClass() == BasicDBObject) {
                obj.put("code", it.code)
                obj.put("stampNumber", it.value?.nil?.stampNumber)
                obj.put("stampID", it.value?.nil?.stampID)
                obj.put("dicd_od1", it.value?.dicd_meas?.dicdViaSizeRawData?.od1)
                obj.put("dicd_od2", it.value?.dicd_meas?.dicdViaSizeRawData?.od2)
                obj.put("dicd_od3", it.value?.dicd_meas?.dicdViaSizeRawData?.od3)
                obj.put("dicd_od4", it.value?.dicd_meas?.dicdViaSizeRawData?.od4)
                obj.put("dicd_od5", it.value?.dicd_meas?.dicdViaSizeRawData?.od5)
                obj.put("dicd_id1", it.value?.dicd_meas?.dicdViaSizeRawData?.id1)
                obj.put("dicd_id2", it.value?.dicd_meas?.dicdViaSizeRawData?.id2)
                obj.put("dicd_id3", it.value?.dicd_meas?.dicdViaSizeRawData?.id3)
                obj.put("dicd_id4", it.value?.dicd_meas?.dicdViaSizeRawData?.id4)
                obj.put("dicd_id5", it.value?.dicd_meas?.dicdViaSizeRawData?.id5)
                if (it.value?.ficd_meas?.ficdViaSizeRawData?.getClass() == BasicDBObject) {
                    obj.put("ficd_od1", it.value?.ficd_meas?.ficdViaSizeRawData?.od1)
                    obj.put("ficd_od2", it.value?.ficd_meas?.ficdViaSizeRawData?.od2)
                    obj.put("ficd_od3", it.value?.ficd_meas?.ficdViaSizeRawData?.od3)
                    obj.put("ficd_id1", it.value?.ficd_meas?.ficdViaSizeRawData?.id1)
                    obj.put("ficd_id2", it.value?.ficd_meas?.ficdViaSizeRawData?.id2)
                    obj.put("ficd_id3", it.value?.ficd_meas?.ficdViaSizeRawData?.id3)
                }
            }

            if (obj) results.add(obj)
        }
        results.sort({a, b ->
            b.ficd_od3 <=> a.ficd_od3
        })

        XSSFWorkbook workbook = utilService.exportExcel(results, "")
        response.setHeader("Content-disposition", "attachment; filename=dicdexport.xlsx")
        response.contentType = "application/excel"
        ServletOutputStream f = response.getOutputStream()
        workbook.write(f)
        f.close()
    }

}