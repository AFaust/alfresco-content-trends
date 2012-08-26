PROJECT LAYOUT
--------------

src -------------------------------------> (source folder)
        |
        |__ main ___ __ messages --------> I18n bundles
        |           |
        |           |__ config-----------> Module configuration
        |           |
        |           |__ java ------------> Java stuff (JAR fodder)
        |           |
        |           |__ site-webscripts -> Surf components / Share tier web scripts
        |           |
        |           |__ templates -------> Surf templates
        |           |
        |           |__ site-data -------> Surf model objects
        |           |
        |           |__ webapp ----------> Web resources - will be processed by merger / minifer
        |
        target - Project build dir