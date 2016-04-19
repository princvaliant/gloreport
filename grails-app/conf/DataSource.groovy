dataSource {
	pooled = true
	driverClassName = "com.mysql.jdbc.Driver"
	username = "root"
	password = "mysql"
}
hibernate {
	cache.use_second_level_cache = false
	cache.use_query_cache = false
	cache.provider_class = 'net.sf.ehcache.hibernate.EhCacheProvider'
	flush.mode="auto"
}

// environment specific settings
environments {
	development {
		dataSource {
			dbCreate = "update" // one of 'create', 'create-drop','update'
			
			url = "jdbc:mysql://calserver04:3306/glo?autoReconnect=true"
			
			pooled = true
			// Other database parameters..
			properties {
				maxActive = 50
				maxIdle = 25
				minIdle = 5
				initialSize = 5
				minEvictableIdleTimeMillis = 1800000
				timeBetweenEvictionRunsMillis = 1800000
				numTestsPerEvictionRun = 3
				maxWait = 10000
				testOnBorrow = true
				testWhileIdle = true
				testOnReturn = true
				validationQuery="select 1"
			}
		}

		grails {
			mongo {
                host = "calserver07"
                port = 27017
                user = "root"
                pwd = "mongodb"
                databaseName = "glo"
				
				options {
					autoConnectRetry = true
					connectTimeout = 120000
					connectionsPerHost = 200
					socketTimeout = 120000
					threadsAllowedToBlockForConnectionMultiplier = 5
					maxAutoConnectRetryTime=5
					maxWaitTime=120000
				}
			}
		}
	}
	test {
		dataSource {
			dbCreate = "update"
			url = props.get("dataSourceUrl")
			username = props.get("dataSourceUsername")
			password = props.get("dataSourcePassword")
		}
	}
	production {
		dataSource {
			dbCreate = "update"
			// Connection params are defined in /usr/local/jd/conf/pricemaster-config.properties
			url = "jdbc:mysql://calserver04:3306/glo?autoReconnect=true"
			username = "root"
			password = "mysql"


			pooled = true
			// Other database parameters..
			properties {
				maxActive = 50
				maxIdle = 25
				minIdle = 5
				initialSize = 5
				minEvictableIdleTimeMillis = 1800000
				timeBetweenEvictionRunsMillis = 1800000
				numTestsPerEvictionRun = 3
				maxWait = 10000
				testOnBorrow = true
				testWhileIdle = true
				testOnReturn = true
				validationQuery="select 1"
			}
		}

		grails {
			mongo {
                host = "calserver07"
                port = 27017
                user = "root"
                pwd = "mongodb"
                databaseName = "glo"
				
				options {
					autoConnectRetry = true
					connectTimeout = 30000
					connectionsPerHost = 200
					socketTimeout = 30000
					threadsAllowedToBlockForConnectionMultiplier = 5
					maxAutoConnectRetryTime=5
					maxWaitTime=120000
				}
			}
		}
	}
}

