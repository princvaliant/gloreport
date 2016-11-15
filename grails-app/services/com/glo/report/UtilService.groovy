package com.glo.report

import grails.converters.JSON
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFCreationHelper
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import com.mongodb.BasicDBObject
import javax.servlet.ServletOutputStream

import java.text.DateFormat
import java.text.SimpleDateFormat
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics


class UtilService {

	def mongo
	static transactional = true

	def  getValidDevices(def data, def level1) {
		
		Set all = new HashSet()
		Set failed = new HashSet()

		def waferFilters = WaferFilter.executeQuery( """
			select wf from WaferFilter as wf where wf.level1 = '$level1' and wf.isActive = 1 and wf.isAdmin = 1
			""")
		

		waferFilters.each { waferFilter ->

			if (data[waferFilter.level1]) {
				if (waferFilter.level1 == 'Current @ 2V') waferFilter.level2 = 'NA'
				
				def unfiltered = (BasicDBObject)data[waferFilter.level1][waferFilter.level2]

				if (unfiltered) {
					unfiltered.each { k, v ->
						if (!all.contains(k))
							all.add(k)
						
						if ((waferFilter.valFrom > v || waferFilter.valTo < v) ) {
							if (!failed.contains(k))
								failed.add(k)
						}
					}
				}
			}
		}
		
		all.minus(failed)
	}
	
	
	def getWaferFilters(def dieSpec) {
	
		def waferFilters = WaferFilter.findAllByDieSpec(dieSpec)
		
		// Apply optical filter if level2 represents optical
		def temp = []
		waferFilters.eachWithIndex { waferFilter, i ->
			if (!(waferFilter.level2  in [
				"OSA Int Time (ms)",
				"OSA Counts",
				"Pulse Current (mA)",
				"Pulse Voltage (V)"
			])) {
				temp.add(waferFilter.level1)
			}
		}
		temp = temp.unique()
		temp.each {
			def adminFilters = WaferFilter.executeQuery( """
						select wf from WaferFilter as wf where wf.level1 = ? and wf.isActive = 1 and wf.isAdmin = 1
						""", it)
			waferFilters = adminFilters + waferFilters
		}
		waferFilters
	}

	
	def  getValid(def waferFilters, def data) {
		
		def results = []
		def tested = []
		Set filtered = new HashSet()
		
		def record1 = [:]
		record1.filter = "Tested"
		record1.cnt = 0
		results.add (record1)
		
		waferFilters.eachWithIndex { waferFilter, i ->
			if (waferFilter.level1 == 'Current @ 2V')
				waferFilter.level2 = 'NA'
			else
				if (data[waferFilter.level1] && data[waferFilter.level1]["OSA Int Time (ms)"]) {
					def record = [:]
					record.level1 = waferFilter.level1
					record.level2 = "OSA Int Time (ms)"
					record.cnt = data[waferFilter.level1]["OSA Int Time (ms)"].size()
					tested.add(record)
				}
			
			if (data[waferFilter.level1] && data[waferFilter.level1][waferFilter.level2]) {
				def unfiltered = (BasicDBObject)data[waferFilter.level1][waferFilter.level2]
	
				if (i == 0) {
					unfiltered.each { k, v ->
						if (waferFilter.valFrom <= v && v <= waferFilter.valTo)
							filtered.add(k)
					}
				}
				else {
					Set failed = new HashSet()
					filtered.each { k ->
						if (unfiltered.containsKey(k)) {
							if (waferFilter.valFrom > unfiltered[k] || unfiltered[k] > waferFilter.valTo)
								failed.add(k)
						}
						else {
							failed.add(k)
						}
					}
					filtered = filtered.minus(failed)
				}
			}
			else
				filtered = new HashSet()
			
			if (!waferFilter.isAdmin) {
				def record = [:]
				record.filter = waferFilter.toString()
				record.cnt = filtered.size()
				results.add(record)
			}
		}
		
		//select smallest Tested cnt
		if (tested.size() != 0) {
			tested.sort {'cnt'}
			results[0].cnt = tested[0].cnt
		}
		
		return (results)
	}

	
	def getTestData(def code, def tkey, def testId, def path) {
		def temp = null
		def db = mongo.getDB("glo")
		
		def query = new BasicDBObject()
		query.put("value.code", code)
		query.put("value.parentCode", null)
		query.put("value.tkey", tkey)
		query.put("value.testId", testId)
		if (path) query.put(path, new BasicDBObject('$exists',true))
		temp = db.testData.find(query, new BasicDBObject())
		
		temp
	}
	
		
	def getLatestTestData(def code, def testIndexes, def fullIndexes, def path) {
		def temp = null
		def db = mongo.getDB("glo")
		
		def cnt = fullIndexes.size()
		while ( cnt-- > 0 ) {
			
			def query = new BasicDBObject()
			query.put("value.code", code)
			query.put("value.parentCode", null)
			query.put("value.tkey", "full_test_visualization")
			query.put("value.testId", fullIndexes[cnt])
			if (path) query.put(path, new BasicDBObject('$exists',true))
			temp = db.testData.find(query, new BasicDBObject())
			if (temp)
				break
		}
		
		if (!temp) {
			cnt = testIndexes.size()
			while ( cnt-- > 0 ) {
			
				def query2 = new BasicDBObject()
				query2.put("value.code", code)
				query2.put("value.parentCode", null)
				query2.put("value.tkey", "test_data_visualization")
				query2.put("value.testId", testIndexes[cnt])
				if (path) query2.put(path, new BasicDBObject('$exists',true))
				temp = db.testData.find(query2, new BasicDBObject())
				if (temp)
					break
			}
		}
		temp
	}

	
	def getBestEQETestData(def code, def testIndexes, def fullIndexes, def device) {
		def db = mongo.getDB("glo")
		
		def result = [:]
		result.bestEQE = -99
		result.bestEQEcurrent = null
		result.bestEQEpulsecurrent = null
		
		def cnt = fullIndexes.size()
		while ( cnt-- > 0 ) {
			
			def query = new BasicDBObject()
			query.put("value.code", code)
			query.put("value.parentCode", null)
			query.put("value.tkey", "full_test_visualization")
			query.put("value.testId", fullIndexes[cnt])
			//if (path) query.put(path, new BasicDBObject('$exists',true))
			
			db.testData.find(query, new BasicDBObject()).collect { testRecord ->
				def data = testRecord['value']['data']
				data.each { current, currentData ->
					if (currentData.EQE) {
						def newEQE = currentData.EQE[device] ?: -99
						if (newEQE > result.bestEQE) {
							def pulsecurrent = currentData["Pulse Current (mA)"][device] ?: -99
							if (pulsecurrent > -90) {
								Set validDevices = getValidDevices (data, current)
								if (device in validDevices) {
									result.bestEQE = newEQE
									result.bestEQEcurrent = current
									result.bestEQEpulsecurrent = pulsecurrent
								}
							}
						}
					}
				}
			}
		}
		
		cnt = testIndexes.size()
		while ( cnt-- > 0 ) {
		
			def query2 = new BasicDBObject()
			query2.put("value.code", code)
			query2.put("value.parentCode", null)
			query2.put("value.tkey", "test_data_visualization")
			query2.put("value.testId", testIndexes[cnt])
			//if (path) query2.put(path, new BasicDBObject('$exists',true))
			
			db.testData.find(query2, new BasicDBObject()).collect { testRecord ->
				def data = testRecord['value']['data']
				data.each { current, currentData ->
					if (currentData.EQE) {
						def newEQE = currentData.EQE[device] ?: -99
						if (newEQE > result.bestEQE) {
							def pulsecurrent = currentData["Pulse Current (mA)"][device] ?: -99
							if (pulsecurrent > -90) {
								Set validDevices = getValidDevices (data, current)
								if (device in validDevices) {
									result.bestEQE = newEQE
									result.bestEQEcurrent = current
									result.bestEQEpulsecurrent = pulsecurrent
								}
							}
						}
					}
				}
			}
		}
		
		result
	}

    def exportExcel (List objList, String formatting, String sheetName, def cols) {
        return exportExcel(objList, formatting, null, sheetName, cols)
    }

	def exportExcel (List objList, String formatting, workbook, sheetName, cols) {

        if (workbook == null) {
            workbook = new XSSFWorkbook()
        } else {
            def idx = workbook.getSheetIndex("mesdata")
            if (idx >= 0) {
                workbook.removeSheetAt(idx)
            }
        }
        def sheet
        if (sheetName) {
            sheet = workbook.createSheet(sheetName)
        } else {
            sheet = workbook.createSheet("mesdata")
        }

		if (objList) {
			XSSFCellStyle style = workbook.createCellStyle()
			XSSFCreationHelper createHelper = workbook.getCreationHelper();
			style.setDataFormat(createHelper.createDataFormat().getFormat("MM/dd/yyyy"))

			//NORMAL
			if (!formatting.contains('x')) {
				XSSFRow rowHeader = sheet.createRow(0)
						
				def h = 0
                cols.each { name ->
                    XSSFCell cellHead = rowHeader.createCell((int) h)
                    cellHead.setCellValue(new XSSFRichTextString(name))
                    h++
                }

				def r = 1
				objList.each { obj ->
                    if (obj != null) {
                        XSSFRow rowData = sheet.createRow(r)
                        def c = 0
                        cols.each { name ->
                            XSSFCell cellData = rowData.createCell((int) c)
                            def value = obj[name]
                            if (value != null && name != "_id")
                                cellData.setCellValue(value)
                            if (name.toLowerCase().contains("date")) {
                                cellData.setCellStyle(style)
                            }
                            c++
                        }
                        r++
                    }
				}
			}
			
			// TRANSPOSE
			else {
				//put map keys in the first column in the spreadsheet
				int r = 0
				objList[0].keySet().each {
					XSSFRow rowData = sheet.createRow(r)
					XSSFCell cellData = rowData.createCell(0)
					cellData.setCellValue(new XSSFRichTextString(it))
					r++
				}
				
				//put map values in other columns in the spreadsheet
				int c = 1
				objList.each { obj ->
					r = 0
					obj.each { k, v ->
						XSSFRow rowData = sheet.getRow(r)
						XSSFCell cellData = rowData.createCell(c)
						if (v != null ) cellData.setCellValue(v)
						r++
					}
					c++
				}
			}

			//auto-size all columns
			XSSFRow rowData = sheet.getRow(0)
			int lastColumn = rowData.getLastCellNum()
			for (int colNum = 0; colNum < lastColumn; colNum++)  sheet.autoSizeColumn(colNum)
		}
		
		return(workbook)
	}


    def exportExcelLight (List objList, workbook, sheet) {

        if (objList) {
            XSSFCellStyle style = workbook.createCellStyle()
            XSSFCreationHelper createHelper = workbook.getCreationHelper();
            style.setDataFormat(createHelper.createDataFormat().getFormat("MM/dd/yyyy"))

            XSSFRow rowHeader = sheet.createRow(0)
            def h = 0
            for (obj in objList) {
                if (obj != null) {
                    obj.each { name, value ->
                        XSSFCell cellHead = rowHeader.createCell((int) h)
                        cellHead.setCellValue(new XSSFRichTextString(name))
                        h++
                    }
                    break;
                }
            }

            def r = 1
            objList.each { obj ->
                if (obj != null) {
                    XSSFRow rowData = sheet.createRow(r)
                    def c = 0
                    obj.each { name, value ->
                        XSSFCell cellData = rowData.createCell((int) c)
                        if (value != null && name != "_id")
                            cellData.setCellValue(value)
                        if (formatting.contains('d') && name.toLowerCase().contains("date")) {
                            cellData.setCellStyle(style)
                        }
                        c++
                    }
                    r++
                }
            }

            //auto-size all columns
            XSSFRow rowData = sheet.getRow(0)
            int lastColumn = rowData.getLastCellNum()
            for (int colNum = 0; colNum < lastColumn; colNum++)
                sheet.autoSizeColumn(colNum)
        }
    }

}