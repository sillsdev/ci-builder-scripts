/*
 * Copyright (c) 2017 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
/*
 * DSL script for FlexBridge Jenkins views
 */
//#include utilities/CommonViews.groovy

/* Definition of views */

nestedView('FlexBridge') {
/*	configure { view ->
		view / defaultView('All')
	}*/
	views {
		CommonViews.addPackageBuildsListView(delegate, 'FLExBridge', '^FlexBridge.*_(Nightly)?Packaging-.*')
	}
}
