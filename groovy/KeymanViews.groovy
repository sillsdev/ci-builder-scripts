/*
 * Copyright (c) 2018 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
/*
 * DSL script for Keyman Jenkins views
 */

//#include utilities/CommonViews.groovy

/* Definition of views */

nestedView('Keyman') {
/*	configure { view ->
		view / defaultView('All')
	}*/
	views {
		CommonViews.addPackageBuildsListView(delegate, 'Keyman', '^Keyman_Packaging-.*')
	}
}
