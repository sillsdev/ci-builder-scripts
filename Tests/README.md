This directory contains unit and integration tests for the build jobs.

One main purpose of these tests is to have automated tests when upgrading Jenkins and/or the
various plugins. Sometimes the plugins modify the way they store the information in the job
config files. This is problematic if the Job DSL plugin hasn't been updated yet for the new
version of the plugin, or in the cases where the Job DSL plugin doesn't provide support for
the plugin and we added custom configuration.

To run the tests you'll have to install phantomjs (version >= 1.9.2). This can be done through
npm:

    npm install phantomjs

You also need a Jenkins instance listening on `http://jenkins-vm.local:8080`, e.g. a virtual
machine with Jenkins installed, that has the groovy jobs created.

The current version of `Selenium.WebDriver` (2.45.0) has a bug when running PhantomJS on Linux
(Name of the executable is expected as `Phantomjs.exe`, whereas on Linux it is simply `phantomjs`).
Therefore we also install `Selenium.WebDriver.PhantomJS`. However, because both packages
install the same files it depends on the order the nuget packages get installed.