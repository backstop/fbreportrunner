fbreportrunner - Fundbutter Report Runner
==============

This is a simple project that will run some or all of the reports saved in a backstop instance.
The project was built using IntelliJ / Maven

To build, click on package.  This will product a self contained jar file.

fbreportrunner uses command line arguments.
To run, open up a command window and execute

java -jar fbreportrunner.jar -help


Typical usage is:

java -jar fbreportrunner.jar -uri https://somehost -user myUserId -password myPassword

There are other options available.  Please see the help text to find out more.
( java -jar fbreportrunner.jar -help )

