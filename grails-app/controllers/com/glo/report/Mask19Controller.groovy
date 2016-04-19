package com.glo.report

import com.mongodb.BasicDBObject
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream

class Mask19Controller {
	
	def mongo
	def utilService

	def index = {
		
		def db = mongo.getDB("glo")
		
		def result = 0
		def match = new BasicDBObject()
		match.put('WaferID', params.code)
        match.put('TestType', params.tkey)

        def sids = db.measures.aggregate(
                ['$match': match],
                ['$group': [_id: [sid: '$sid'], total: [$sum: 1]]],
                ['$sort': [_id: -1]],
                ['$limit':6]
        ).results()

        sids = sids.collect{it._id.sid}

        match.put('sid', new BasicDBObject('$in', sids))

    	def project = new BasicDBObject()
		project.put('TestType', 1)
        project.put('WaferID', 1)
        project.put('devlist', 1)
        project.put('experimentId', 1)
        project.put('sid', 1)
        project.put('Current', 1)
        project.put('DeviceID', 1)
        project.put('eqe', 1)
        project.put('dominantWavelength', 1)
        project.put('FWHM', 1)
        project.put('Volt', 1)
        project.put('PeakWavelength', 1)
        project.put('CurrentString', 1)

        def volts = [:]

        def list = db.measures.find(match, project).limit(100000).collect{

            def devType = ""
            if (it.devlist.indexOf('Mask19_StdTest1') >= 0)
                devType =  "300"
            if (it.devlist.indexOf('Mask19_StdTest2') >= 0)
                devType =  "200"
            if (it.devlist.indexOf('Mask19_StdTest3') >= 0)
                devType =  "100"
            if (it.devlist.indexOf('Mask19_StdTest4') >= 0)
                devType =  "30"
            if (it.devlist.indexOf('Mask19_StdTest5') >= 0)
                devType =  "20"
            if (it.devlist.indexOf('Mask19_StdTest6') >= 0)
                devType =  "10"


            Float ret = 0.0
            if (it.CurrentString) {

                def devid = it.WaferID + "_" + it.DeviceID
                if (!volts.containsKey(devid)) {
                    def iv = db.testData.find(['value.code':devid, 'value.testId':it.sid.toLong()], ['value.data.Datavoltage.data':1]).collect
                    {  dv ->
                          dv?.value?.data?.Datavoltage?.data
                    }
                    volts.put(devid, [devid:devid, devType:devType, iv: iv])
                }

                def curr = it.CurrentString.tokenize('@')[1]
                if (curr) {
                    Long cc = Long.parseLong(curr.replaceAll("\\D+", ""))
                    if (curr.indexOf('mA') > 0) cc = cc * 1E6
                    if (curr.indexOf('uA') > 0) cc = cc * 1E3

                    // Apply filters
                    def cv = it.Current * 1E6
                    if (Math.abs(cv - cc) / cc < 0.3) {

                        if (it.devlist.indexOf('Mask19_StdTest1') >= 0)
                            ret = cc / (Math.pow(300 - 10, 2) * 1E1)
                        else if (it.devlist.indexOf('Mask19_StdTest2') >= 0)
                            ret = cc / (Math.pow(200 - 10, 2) * 1E1)
                        else if (it.devlist.indexOf('Mask19_StdTest3') >= 0)
                            ret = cc / (Math.pow(100 - 10, 2) * 1E1)
                        else if (it.devlist.indexOf('Mask19_StdTest4') >= 0)
                            ret = cc / (Math.pow(30, 2) * 1E1)
                        else if (it.devlist.indexOf('Mask19_StdTest5') >= 0)
                            ret = cc / (Math.pow(20, 2) * 1E1)
                        else if (it.devlist.indexOf('Mask19_StdTest6') >= 0)
                            ret = cc / (Math.pow(10, 2) * 1E1)

                        [
                                'TestType'          : it.TestType,
                                'WaferID'           : it.WaferID,
                                'devlist'           : it.devlist,
                                'experimentId'      : it.experimentId,
                                'sid'               : it.sid,
                                'CurrentValue'      : it.Current * 1E6,
                                'DeviceID'          : it.DeviceID,
                                'deviceType'        : devType,
                                'currentDensity (A/cm2)': ret.round(3),
                                'eqe'               : it.eqe,
                                'dominantWavelength': it.dominantWavelength,
                                'FWHM'              : it.FWHM,
                                'Volt'              : it.Volt,
                                'PeakWavelength'    : it.PeakWavelength,
                                'CurrentString'     : it.CurrentString
                        ]
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }


        }

        DataView dataView = DataView.get(2148)
        def stream = new ByteArrayInputStream(dataView.excelTemplate)
        XSSFWorkbook workbook = new XSSFWorkbook(stream)

        workbook = utilService.exportExcel(list, "", workbook)

        def idx = workbook.getSheetIndex("voltagesweep")
        def sheet
        if (idx < 0) {
            sheet = workbook.createSheet("voltagesweep")
        } else {
            sheet = workbook.getSheetAt(idx)
        }

        XSSFRow rowHeader = sheet.createRow(0)

        XSSFCell cellHead = rowHeader.createCell(0)
        cellHead.setCellValue(new XSSFRichTextString("device"))
        XSSFCell cellHead1 = rowHeader.createCell(1)
        cellHead1.setCellValue(new XSSFRichTextString("deviceType"))
        XSSFCell cellHead2 = rowHeader.createCell(2)
        cellHead2.setCellValue(new XSSFRichTextString("volt"))
        XSSFCell cellHead3 = rowHeader.createCell(3)
        cellHead3.setCellValue(new XSSFRichTextString("current"))

        def r = 1
        volts.each {
            it.value.iv[0].each { vs ->
                XSSFRow rowData = sheet.createRow(r)
                XSSFCell cellData = rowData.createCell(0)
                cellData.setCellValue(it.value.devid)
                cellData = rowData.createCell(1)
                cellData.setCellValue(it.value.devType)
                cellData = rowData.createCell(2)
                cellData.setCellValue(vs[0].round(1))
                cellData = rowData.createCell(3)
                cellData.setCellValue(Math.log10(Math.abs(vs[1])))
                rowData = null
                cellData = null
                r++
            }
        }
        volts = null

        def fHeader = "attachment; filename=" + params.code + "_mask19.xlsx"
        response.setHeader("Content-disposition", fHeader)
        response.contentType = "application/excel"
        ServletOutputStream f = response.getOutputStream()
        workbook.write(f)
        f.close()
	}
}
