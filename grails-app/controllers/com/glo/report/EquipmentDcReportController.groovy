package com.glo.report

import com.mongodb.BasicDBObject
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import javax.servlet.ServletOutputStream
import grails.converters.JSON

class EquipmentDcReportController {
	
	def mongo
	def utilService
	
	def index = {
		
		def db = mongo.getDB("glo")
		
		def query = new BasicDBObject()
        query.put("parentCode", null)
		query.put("value.tags", "nwLED|epi|epi_growth")
        query.put("value.productCode", "100")

        def fields = new BasicDBObject()
        fields.put("code", 1)
		fields.put("value.runNumber", 1)
        fields.put("value.epi_growth.satellite", 1)
        fields.put("value.epi_growth.pocket", 1)
		def resultsWafer = db.dataReport.find(query, fields)

        def tag = params.reactor?.toLowerCase() ?: "d1"
        def query2 = new BasicDBObject()
        query2.put("parentCode", null)
        query2.put("value.tags", "EquipmentDC|" + tag + "ds")

        def f2 = new BasicDBObject()
        f2.put('value.runNumber', 1)
        f2.put('value.nanowire_growth', 1)
        f2.put('value.nw2_growth', 1)
        f2.put('value.nw1_growth', 1)
        f2.put('value.underlayer_growth', 1)
        f2.put('value.ul1_growth', 1)
        f2.put('value.grow_first_well', 1)
        f2.put('value.qw1_growth', 1)
        f2.put('value.rounding', 1)
        f2.put('value.tip_anneal', 1)
        f2.put('value.palgan_growth', 1)
        f2.put('value.palgan1_growth', 1)
        f2.put('value.end_p++growth', 1)
        f2.put('value.end_p+_growth', 1)
        f2.put('value.p+_growth', 1)
        f2.put('value.prep1_growth', 1)
        f2.put('value.spacer1_growth', 1)
        f2.put('value.palgan2_growth', 1)

        f2.put('value.scndunderlayer_growth', 1)
        f2.put('value.ramp_cp2mg', 1)
        f2.put('value.cap1_growth', 1)
        f2.put('value.cap2_growth', 1)
        f2.put('value.pgan1_growth', 1)
        f2.put('value.p+gan_growth', 1)

        def equipment = db.dataReport.find(query2, f2).limit(1000).sort(['value.runNumber':-1]).collect{ it}
        def resultsEquipment = equipment.groupBy{it.value?.runNumber}

   //   def query3 = new BasicDBObject()
   //     query3.put("parentCode", null)
   //     query3.put("value.tags", "EquipmentDC|recipeSummary")

   //     def recipes = db.dataReport.find(query3, new BasicDBObject()).collect{ it}
   //     def resultRecipes = recipes.groupBy{it.value?.runNumber}

        def results = []
        resultsWafer.each { obj ->

			def ret = [:]
            ret.put("runNumber", obj.value?.runNumber?.toUpperCase())
            ret.put("code", obj.code)
            ret.put("satellite", obj.value?.epi_growth?.satellite)
            ret.put("pocket", obj.value?.epi_growth?.pocket)

            def lst = resultsEquipment[ret.runNumber]
     //       def rcp = resultRecipes[ret.runNumber]

            if (ret.runNumber && ret.satellite && lst) {

                def s = lst[0]
            //    def t = rcp ? rcp[0] : null

                def puck = ret.satellite.toInteger() + 8

                ret.put("Puck temp NW1", '')
                ret.put("Puck temp NW1 step", '')

                ret.put("Puck temp NW2", '')
                ret.put("Puck temp NW2 step", '')

                ret.put("Puck temp UL1", '')
                ret.put("Puck temp UL1 step", '')

                ret.put("Puck temp UL2", '')
                ret.put("Puck temp UL2 step", '')

                ret.put("Puck temp prep", '')
                ret.put("Puck temp prep step", '')

                ret.put("Puck temp spacer", '')
                ret.put("Puck temp spacer step", '')

                ret.put("Puck temp QW1", '')
                ret.put("Puck temp QW1 step", '')

                ret.put("Puck temp Cap1", '')
                ret.put("Puck temp Cap1 step", '')

                ret.put("Puck temp Cap2", '')
                ret.put("Puck temp Cap2 step", '')

                ret.put("Puck temp TipRound", '')
                ret.put("Puck temp TipRound step", '')

                ret.put("Puck temp pAlGaN1", '')
                ret.put("Puck temp pAlGaN1 step", '')

                ret.put("Puck temp palgan2", '')
                ret.put("Puck temp palgan2 step", '')

                ret.put("Puck temp pGaN", '')
                ret.put("Puck temp pGaN step", '')

                ret.put("Puck temp p++", '')
                ret.put("Puck temp p++ step", '')


                if (s && s.value?.nanowire_growth) {
                    ret["Puck temp NW2"] = s.value?.nanowire_growth["EpiTemp" + puck]
                    ret["Puck temp NW2 step"] = 'nanowire_growth'
                    ret["Puck temp NW1"] = s.value?.nanowire_growth["EpiTemp" + puck]
                    ret["Puck temp NW1 step"] = 'nanowire_growth'
                 }
                if (s && s.value?.nw2_growth) {
                    ret["Puck temp NW2"] = s.value?.nw2_growth["EpiTemp" + puck]
                    ret["Puck temp NW2 step"] = 'nw2_growth'
                }
                if (s && s.value?.nw1_growth) {
                    ret["Puck temp NW1"] = s.value?.nw1_growth["EpiTemp" + puck]
                    ret["Puck temp NW1 step"] = 'nw1_growth'
                }

                if (s && s.value?.underlayer_growth) {
                    ret["Puck temp UL1"] = s.value?.underlayer_growth["EpiTemp" + puck]
                    ret["Puck temp UL1 step"] = 'underlayer_growth'
                }
                if (s && s.value?.ul1_growth) {
                    ret["Puck temp UL1"] = s.value?.ul1_growth["EpiTemp" + puck]
                    ret["Puck temp UL1 step"] = 'ul1_growth'
                }

                if (s && s.value?.scndunderlayer_growth) {
                    ret["Puck temp UL2"] = s.value?.scndunderlayer_growth["EpiTemp" + puck]
                    ret["Puck temp UL2 step"] = 'ul2_growth'
                }

                if (s && s.value?.ramp_cp2mg) {
                    ret["Puck temp pGaN"] = s.value?.ramp_cp2mg["EpiTemp" + puck]
                    ret["Puck temp pGaN step"] = 'pgan_growth'
                }
                if (s && s.value?.pgan1_growth) {
                    ret["Puck temp pGaN"] = s.value?.pgan1_growth["EpiTemp" + puck]
                    ret["Puck temp pGaN step"] = 'pgan_growth'
                }

                if (s && s.value?.grow_first_well) {
                    ret["Puck temp QW1"] = s.value?.grow_first_well["EpiTemp" + puck]
                    ret["Puck temp QW1 step"] = 'grow_first_well'
                }
                if (s && s.value?.qw1_growth) {
                    ret["Puck temp QW1"] = s.value?.qw1_growth["EpiTemp" + puck]
                    ret["Puck temp QW1 step"] = 'qw1_growth'
                }

                if (s && s.value?.rounding) {
                    ret["Puck temp TipRound"] = s.value?.rounding["EpiTemp" + puck]
                    ret["Puck temp TipRound step"] = 'rounding'
                }
                if (s && s.value?.tip_anneal) {
                    ret["Puck temp TipRound"] = s.value?.tip_anneal["EpiTemp" + puck]
                    ret["Puck temp TipRound step"] = 'tip_anneal'
                }

                if (s && s.value?.palgan_growth) {
                    ret["Puck temp pAlGaN1"] = s.value?.palgan_growth["EpiTemp" + puck]
                    ret["Puck temp pAlGaN1 step"] = 'palgan_growth'
                }
                if (s && s.value?.palgan1_growth) {
                    ret["Puck temp pAlGaN1"] = s.value?.palgan1_growth["EpiTemp" + puck]
                    ret["Puck temp pAlGaN1 step"] = 'palgan1_growth'
                }

                if (s && s.value["end_p++growth"]) {
                    ret["Puck temp p++"] = s.value["end_p++growth"]["EpiTemp" + puck]
                    ret["Puck temp p++ step"] = 'end_p++growth'
                }
                if (s && s.value["end_p+_growth"]) {
                    ret["Puck temp p++"] = s.value["end_p+_growth"]["EpiTemp" + puck]
                    ret["Puck temp p++ step"] = 'end_p+_growth'
                }
                if (s && s.value["p+_growth"]) {
                    ret["Puck temp p++"] = s.value["p+_growth"]["EpiTemp" + puck]
                    ret["Puck temp p++ step"] = 'p+_growth'
                }

                if (s && s.value["prep1_growth"]) {
                    ret["Puck temp prep"] = s.value["prep1_growth"]["EpiTemp" + puck]
                    ret["Puck temp prep step"] = 'prep1_growth'
                }

                if (s && s.value["spacer1_growth"]) {
                    ret["Puck temp spacer"] = s.value["spacer1_growth"]["EpiTemp" + puck]
                    ret["Puck temp spacer step"] = 'spacer1_growth'
                }

                if (s && s.value["spacer_growth"]) {
                    ret["Puck temp spacer"] = s.value["spacer_growth"]["EpiTemp" + puck]
                    ret["Puck temp spacer step"] = 'spacer_growth'
                }

                if (s && s.value["palgan2_growth"]) {
                    ret["Puck temp palgan2"] = s.value["palgan2_growth"]["EpiTemp" + puck]
                    ret["Puck temp palgan2 step"] = 'palgan2_growth'
                }

                if (s && s.value["cap1_growth"]) {
                    ret["Puck temp Cap1"] = s.value?.cap1_growth["EpiTemp" + puck]
                    ret["Puck temp Cap1 step"] = 'cap1_growth'
                }

                if (s && s.value["cap2_growth"]) {
                    ret["Puck temp Cap2"] = s.value?.cap2_growth["EpiTemp" + puck]
                    ret["Puck temp Cap2 step"] = 'cap2_growth'
                }

                if (s && s.value["p+gan_growth"]) {
                    ret["Puck temp p++"] = s.value["p+gan_growth"]["EpiTemp" + puck]
                    ret["Puck temp p++ step"] = 'p+gan_growth'
                }
                results.add(ret)
            }

		}

        if (params.json) {
            render results as JSON
        } else {
            XSSFWorkbook workbook = utilService.exportExcel(results, "")
            response.setHeader("Content-disposition", "attachment; filename=waferGrowth.xlsx")
            response.contentType = "application/excel"
            ServletOutputStream f = response.getOutputStream()
            workbook.write(f)
            f.close()
        }
	}
}