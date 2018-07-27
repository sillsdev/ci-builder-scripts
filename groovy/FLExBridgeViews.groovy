/*
 * Copyright (c) 2017 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
/*
 * DSL script for FlexBridge Jenkins views
 */
//#include utilities/CommonViews.groovy

/* Definition of views */

class flexBridgeViews {
	static void flexBridgeViewAll(viewContext) {
		viewContext.with {
			categorizedJobsView('All') {
				description 'All <b>FLExBridge</b> jobs'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('FlexBridge')
				}

				columns {
					status()
					weather()
					categorizedJob()
					lastSuccess()
					lastFailure()
					lastDuration()
					lastBuildTriggerColumn {
						causeDisplayType("icon")
					}
					buildButton()
				}
			}
		}
	}
}

nestedView('FlexBridge') {
	configure { view ->
		view / defaultView('All')
	}
	views {
		flexBridgeViews.flexBridgeViewAll(delegate)
		CommonViews.addPackageBuildsListView(delegate, 'FLExBridge', '^FlexBridge.*_(Nightly)?Packaging-.*')
	}
}
