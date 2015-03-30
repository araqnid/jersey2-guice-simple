Simple Jersey
=============

Recently I've been making Java webapps that embed a web server into a command-line application rather than making a .war file to add into a container. This works nicely, thanks to Heroku for giving me the initial recipe.

I've also been using Guice for dependency injection, and wanted to use Jersey as a JAX-RS platform. But Guice uses its own DI tool, HK2, inherited from its Glassfish background. Although there is a HK2-Guice bridge, I found actually getting it all working rather frustrating.

So this is my end result, a simple application that starts a web server and uses Jersey to serve resources. It can run either Jetty or the JDK-supplied HttpServer.
