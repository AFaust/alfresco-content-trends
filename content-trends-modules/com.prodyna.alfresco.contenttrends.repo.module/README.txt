PROJECT LAYOUT
--------------

src -------------------------------------> (source folder)
		|
		|__ main ___ __ messages --------> I18n bundles
		|			|
		|			|__ config-----------> Module configuration
		|			|
		|			|__ java ------------> Java stuff (JAR fodder)
		|			|
        |           |__ webscripts ------> Repository tier web scripts
        |           |
		|			|__ webapp ----------> Web resources - will be processed by merger / minifer
		|
		target - Project build dir