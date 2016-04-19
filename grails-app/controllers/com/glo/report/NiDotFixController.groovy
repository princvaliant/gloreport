package com.glo.report

import com.mongodb.BasicDBObject
import grails.converters.JSON

class NiDotFixController {
	
	def mongo
	def utilService

	def index = {
		
		def db = mongo.getDB("glo")
		
		def result = 0
		def match = new BasicDBObject()
        match.put('value.tkey', 'ni_dot_test')
		match.put('value.parentCode', ['$in': ['UN0013709','L238041641','UN0013723','UN0013711','UN0013714','UN0013713','UN0013710','UN0013720','L239004786']])

		def project = new BasicDBObject()
		project.put('value.code', 1)

		def arr = db.testData.find (match, project).collect{it}

        def res = []
        arr.each {

            def q = new BasicDBObject('value.code', it.value.code)

            def r = it.value.code.replace('I', '1')

            db.testData.update(q, ['$set':['value.code':r]], false, true)

        }
		
		render res  as JSON
	}
}
