package com.glo.report

import com.mongodb.BasicDBObject
import grails.converters.JSON
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream
import java.awt.geom.Arc2D

class Mask19ExcelController {

    def mongo
    def utilService

    def getPerDeviceType = {

        def data = getData(params)

        def allData = data[0]
        def volts = data[1]

        def result = new TreeMap()

        def groupVar = "deviceType"
        def seriesVar = "DeviceID"
        def xVar = "currentDensity"
        def yVars = ["eqe", "dominantWavelength", "PeakWavelength", "Volt", "FWHM"]

        def dataGrouped = allData.groupBy({ it[groupVar] ?: '' })
        dataGrouped.each { grpvar, grpvarData ->
            if (!result.containsKey(grpvar)) {
                result.put(grpvar, [:])
                yVars.each { yvar ->
                    result[grpvar].put(yvar, ["columns": [xVar], "data": []])
                }
            }

            def list = []
            grpvarData.sort { a, b ->
                a[seriesVar] <=> b[seriesVar] ?: a[xVar] <=> b[xVar]
            }.each {
                yVars.each { yvar ->
                    list.add([yvar, it[seriesVar], it[xVar], it[yvar]])
                }
            }

            list.each {
                if (!result[grpvar][it[0]]["columns"].contains(it[1])) {
                    result[grpvar][it[0]]["columns"].add(it[1])
                }
            }

            def glist = list.groupBy({ it[0] }, { it[2] })
            glist.each {
                it.value.each { k, v ->
                    result[grpvar][it.key]["data"].add([k])
                    int s = result[grpvar][it.key]["data"].size()
                    v.each { v2 ->
                        result[grpvar][it.key]["data"][s - 1].add(v2[3])
                    }
                }
            }
        }

        XSSFWorkbook workbook
        DataView dataView = DataView.get(Long.parseLong(params.dataviewid))
        if (dataView?.excelTemplate != null) {
            def stream = new ByteArrayInputStream(dataView.excelTemplate)
            workbook = new XSSFWorkbook(stream)
        } else {
            workbook = new XSSFWorkbook()
        }

        result.each { deviceType, objects ->
            if (deviceType) {
                def idx = workbook.getSheetIndex(deviceType)
                if (idx >= 0) {
                    workbook.removeSheetAt(idx)
                }
                def sheet = workbook.createSheet(deviceType)
                int pos = 0

                objects.each { variable, values ->
                    def r = 0
                    values.columns.each {
                        XSSFRow row = sheet.getRow(r)
                        if (!row)
                            row = sheet.createRow(r)
                        XSSFCell cell = row.createCell(pos)
                        cell.setCellValue(new XSSFRichTextString(it))
                        r++
                    }
                    sheet.getRow(0).createCell(pos).setCellValue(new XSSFRichTextString(variable))

                    def p = 0
                    for (int i = 0; i < r ; i++) {
                        XSSFRow rowh = sheet.getRow(i)
                        def c = 0
                        values.data.each {
                            XSSFCell celld = rowh.createCell(pos + 1 + c++)
                            celld.setCellValue(it[p])
                        }
                        p++
                    }
                    pos += 50
                }
            }
        }

        def fHeader = "attachment; filename=" + params.code + "_mask19.xlsx"
        response.setHeader("Content-disposition", fHeader)
        response.contentType = "application/excel"
        ServletOutputStream f = response.getOutputStream()
        workbook.write(f)
        f.close()
    }

    def getSummaryEqe = {

        def data = getData(params)

        def allData = data[0]
        def volts = data[1]

        def result = [:]
        result.put("columns", ["deviceType", "EQE avg", "EQE median", "EQE max", "CurrentDensity avg"])
        result.put("data", [])

        def groupVar = "deviceType"
        def seriesVar = "DeviceID"
        def xVar = "currentDensity"
        def yVar = "eqe"

        def dataGrouped = allData.groupBy({ it[groupVar] ?: '' }, { it[seriesVar] ?: '' })
        dataGrouped.each { grpvar, grpvarData ->

            def statsY = new DescriptiveStatistics()
            def statsX = new DescriptiveStatistics()
            grpvarData.each { servar, servarData ->
                def stats = new DescriptiveStatistics()
                def map = [:]
                servarData.each {
                    stats.addValue(it[yVar])
                    map.put(it[yVar], it[xVar])
                }
                def max = stats.getMax()
                statsY.addValue(max)
                statsX.addValue(map[max])
            }

            result["data"].add([grpvar, statsY.getMean(), statsY.getPercentile(50), statsY.getMax(), statsX.getMean()])
        }


        XSSFWorkbook workbook
        DataView dataView = DataView.get(Long.parseLong(params.dataviewid))
        if (dataView?.excelTemplate != null) {
            def stream = new ByteArrayInputStream(dataView.excelTemplate)
            workbook = new XSSFWorkbook(stream)
        } else {
            workbook = new XSSFWorkbook()
        }

        def idx = workbook.getSheetIndex("eqe")
        if (idx >= 0) {
            workbook.removeSheetAt(idx)
        }
        def sheet = workbook.createSheet("eqe")

        int pos = 0
        def r = 0
        result.columns.each {
            XSSFRow row = sheet.getRow(r)
            if (!row)
                row = sheet.createRow(r)
            XSSFCell cell = row.createCell(pos)
            cell.setCellValue(new XSSFRichTextString(it))
            r++
        }
        sheet.getRow(0).createCell(pos).setCellValue(new XSSFRichTextString("eqe"))

        def p = 0
        for (int i = 0; i < r ; i++) {
            XSSFRow rowh = sheet.getRow(i)
            def c = 0
            result.data.each {
                XSSFCell celld = rowh.createCell(pos + 1 + c++)
                if (it[p]?.toString()?.trim() != "") {
                    def v = Float.parseFloat(it[p]?.toString()).floatValue()
                    println(v)
                    celld.setCellValue(v)
                }
            }
            p++
        }



        def fHeader = "attachment; filename=" + params.code + "_mask19_summary.xlsx"
        response.setHeader("Content-disposition", fHeader)
        response.contentType = "application/excel"
        ServletOutputStream f = response.getOutputStream()
        workbook.write(f)
        f.close()
    }

    private def getData(parms) {

        def db = mongo.getDB("glo")

        def match = new BasicDBObject()
        match.put('WaferID', parms.code)
        match.put('TestType', parms.tkey)

        def sids
        if (!parms.tids) {
            sids = db.measures.aggregate(
                    ['$match': match],
                    ['$group': [_id: [sid: '$sid'], total: [$sum: 1]]],
                    ['$sort': [_id: -1]],
                    ['$limit':6]
            ).results()
            sids = sids.collect{it._id.sid}
        } else {
            sids = parms.tids.tokenize(",")
        }

        match.put('sid', new BasicDBObject('$in', sids))
        match.put('radiometric', ['$gt':(double)2E-7,'$lt':1])
        match.put('PeakWavelength', ['$gt':360,'$lt':999])
        match.put('dominantWavelength', ['$gt':360,'$lt':999])

        def project = new BasicDBObject()
        project.put('WaferID', 1)
        project.put('DeviceID', 1)
        project.put('devlist', 1)
        project.put('experimentId', 1)
        project.put('sid', 1)
        project.put('Current', 1)
        project.put('eqe', 1)
        project.put('dominantWavelength', 1)
        project.put('FWHM', 1)
        project.put('Volt', 1)
        project.put('PeakWavelength', 1)
        project.put('CurrentString', 1)

        def volts = [:]

        def list = db.measures.find(match, project).limit(100000).collect {

            def devType = ""
            if (it.devlist.indexOf('Mask19_StdTest1') >= 0)
                devType = "300"
            if (it.devlist.indexOf('Mask19_StdTest2') >= 0)
                devType = "200"
            if (it.devlist.indexOf('Mask19_StdTest3') >= 0)
                devType = "100"
            if (it.devlist.indexOf('Mask19_StdTest4') >= 0)
                devType = "30"
            if (it.devlist.indexOf('Mask19_StdTest5') >= 0)
                devType = "20"
            if (it.devlist.indexOf('Mask19_StdTest6') >= 0)
                devType = "10"


            Float ret = 0.0
            if (it.CurrentString) {

                def devid = it.WaferID + "_" + it.DeviceID
                if (!volts.containsKey(devid)) {
                    def iv = db.testData.find(['value.code': devid, 'value.testId': it.sid.toLong()], ['value.data.Datavoltage.data': 1]).collect
                            { dv ->
                                dv?.value?.data?.Datavoltage?.data
                            }
                    volts.put(devid, [devid: devid, devType: devType, iv: iv])
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
                                'WaferID'           : it.WaferID,
                                'DeviceID'          : it.DeviceID,
                                'experimentId'      : it.experimentId,
                                'sid'               : it.sid,
                                'CurrentValue'      : it.Current * 1E6,
                                'deviceType'        : devType,
                                'currentDensity'    : ret.round(3),
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
        return ([list.findAll { it != null }, volts])
    }
}
