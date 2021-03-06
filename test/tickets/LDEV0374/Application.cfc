component {

	this.name	=	Hash( GetCurrentTemplatePath() ) & "2s";
	this.sessionManagement 	= false;

	
	this.datasource = {
		  class: 'org.h2.Driver'
		, bundleName: 'org.h2'
		, bundleVersion: '1.3.172'
		, connectionString: 'jdbc:h2:#getDirectoryFromPath(getCurrentTemplatePath())#/datasource/db;MODE=MySQL'
		, connectionLimit:100 // default:-1
	};
	

	// ORM settings
	this.ormEnabled = true;
	this.ormSettings = {
		// dialect = "MySQLwithInnoDB",
		// dialect = "MicrosoftSQLServer",
		dbcreate="dropcreate"
	};

	function onApplicationStart(){
		try{
			query {
			echo("DROP TABLE users");
			}
		}
		catch(local.e) {}

		query{
			echo("CREATE TABLE users( 
				id INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,
				DateJoined DATETIME )");
		}

	}

	function onRequestStart(){
		setting showdebugOutput=false;
	}
}