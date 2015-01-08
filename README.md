# fbreportrunner - Fundbutter Report Runner

This is a simple project that will run some or all of the reports saved in a backstop instance.
The project was built using IntelliJ / Maven

## How the Program works

The Java application connects with a Backstop instance using SOAP protocol.

The first call grabs a list of report by calling `listSharedReports` from the Soap Endpoint `BackstopReportDirectoryService_1_0`

The array of report definitions `InvocableReport`.  This structure includes the type of report, the JSON string of the definition and the JSON string of the restritions, and the endpoint to call for the report.

To run a report for instance, call the endpoint of the report, sending it the definition, restriction and `asOfDate`.
The as of Date is the date in which to run the report.  See documention on asOfDate from your Backstop implementation.

The report will return an array of ReportRow.  This is a structure with 50 fields returned.  field1 ... field50

This was done due to a pre-existing limitation with the dotNet WSDL compiler and how it handled array of arrays.

If your report contains more than 50 fields, you can call runReport...Large.  This will return an a structure with field1 ... field500.



## Building the project

To build, click on package.  This will product a self contained jar file.

fbreportrunner uses command line arguments.
To run, open up a command window and execute

java -jar fbreportrunner.jar -help

## Running the Project
Typical usage is:

java -jar fbreportrunner.jar -uri https://somehost -user myUserId -password myPassword

There are other options available.  Please see the help text to find out more.
( java -jar fbreportrunner.jar -help )

