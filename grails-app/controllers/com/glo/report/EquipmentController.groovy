package com.glo.report

import com.healthmarketscience.jackcess.Cursor
import com.healthmarketscience.jackcess.Database
import com.healthmarketscience.jackcess.Table
import grails.converters.JSON

class EquipmentController {
	
	def mongo
	def utilService

    def index = { 
		
		def db = mongo.getDB("glo")
		
		def recipe = params.recipe ?: "PC"

		def chart = ['x':'ec','xAxisType':'Category','xTitle':'Run#',
					 'y': [
							 ['y':'CurrentMin','yTitle':'CurrentMin','yAxisType':'Numeric']
						 ],
	                 'tip': ['tip'],
					 'data':[]
					]
		
		int i = 0

		def mdb = Database.open(new File("\\\\calserver03\\Growth\\DB\\S2RecLog\\RecLogDB_V3.mdb")) 
		
		Table tableRuns = mdb.getTable("tRuns");
		Table tableSteps = mdb.getTable("tSteps");
		Table tableStats = mdb.getTable("tStatistics");
		
//		
//		for(Map<String, Object> rowRun : tableRuns) {
//			
//			def rcp = rowRun.get("fRecipeName")
//			def run = rowRun.get("fProductId")
//			def key = rowRun.get("Key")
//			
//			for(Map<String, Object> rowStep : tableSteps) {
//		
//				if(rowStep != null && rowStep.get("fRun") == key) {
//					
//					def stepName = rowStep.get("fStepText") 
//					def keyStep = rowStep.get("Key")
//					
//					for(Map<String, Object> rowStat : tableStats) {
//						
//						if(rowStat != null && rowStat.get("fStep") == keyStep) {
//							
//							def name = rowStat.get("fDeviceName")
//							def value = rowStat.get("fCurrentMean")
//						}
//					}
//					
//					System.out.println(stepName)
//				}	
//			}
//		}
//		
		
		for(Map<String, Object> rowStat : tableStats) {
			
			def name = rowStat.get("fDeviceName")
			def value = rowStat.get("fCurrentMean")
			
			Map<String, Object> rowStep = Cursor.findRow(tableSteps, Collections.singletonMap("Key", rowStat.get("fStep")));
			
			def stepName = rowStep.get("fStepText")
			def keyStep = rowStep.get("Key")
			
			Map<String, Object> rowRun = Cursor.findRow(tableRuns, Collections.singletonMap("Key", rowStep.get("fRun")));
			
			def rcp = rowRun.get("fRecipeName")
			def run = rowRun.get("fProductId")
			def key = rowRun.get("Key")
			
			System.out.println(stepName)
		}
		
		
		chart['data'] = chart['data'].sort {it.date}

		def arr = [chart]

		render arr  as JSON
	
	}
}
