PROJECT LAYOUT
--------------

src --------------------------------------------------------> (source folder)
		|
		|__ main ___ __ resources --------------------------> Classpath resources (land in JAR)
		|			|
		|			|__ config------------------------------> Module configuration.
		|			|
		|			|__ java -------------------------------> Java classes, duh!
		|			|
		|			|__ webapp -----------------------------> Web resources - will be processed by merger / minifer
        |
		|			
		|__ test _____ resources
		|
		target - Project build dir