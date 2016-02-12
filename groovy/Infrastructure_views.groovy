/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
/*
 * DSL script for Infrastructure Jenkins views
 */

/* Definition of views */

listView('Infrastructure') {
	description 'Infrastructure jobs'
	filterBuildQueue false
	filterExecutors true
	recurse true

	jobs {
		regex('^Infrastructure|Infrastructure/.*')
	}

	columns {
		status()
		weather()
		name()
		lastSuccess()
		lastFailure()
		lastDuration()
		buildButton()
		lastBuildNode()
		lastBuildConsole()
	}
	configure { view ->
		view / columns / 'hudson.plugins.nodenamecolumn.NodeNameColumn'
	}
}
