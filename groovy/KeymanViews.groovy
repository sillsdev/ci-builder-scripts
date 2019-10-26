/*
 * Copyright (c) 2018 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
/*
 * DSL script for Keyman Jenkins views
 */

//#include utilities/CommonViews.groovy

/* Definition of views */

class keymanViews {
	static void KeymanViewAll(viewContext) {
		viewContext.with {
			categorizedJobsView('All') {
				description 'All <b>Keyman</b> jobs'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('(^Keyman.*')
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
nestedView('Keyman') {
	configure { view ->
		view / defaultView('All')
	}
	views {
		keymanViews.KeymanViewAll(delegate)
		CommonViews.addPackageBuildsListView(delegate, 'Keyman', '^Keyman_Packaging-.*-alpha', 'Alpha Package builds')
		CommonViews.addPackageBuildsListView(delegate, 'Keyman', '^Keyman_Packaging-.*-beta', 'Beta Package builds')
		CommonViews.addPackageBuildsListView(delegate, 'Keyman', '^Keyman_Packaging-.*-stable', 'Stable Package builds')
		CommonViews.addPackageBuildsListView(delegate, 'Keyman', '^Keyman_Packaging-Linux-onboard-keyman-', 'OnboardKeyboard Package builds')
	}
}
