/*
 * Copyright (c) 2016-2019 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * DSL script for Library related Jenkins views
 */

/* Definition of views */

class libraryViews {
	static void LibraryViewAll(viewContext) {
		viewContext.with {
			categorizedJobsView('All') {
				description 'All jobs related to libraries'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('(icu)')
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

nestedView('Libraries') {
	configure { view ->
		view / defaultView('All')
	}
	views {
		libraryViews.LibraryViewAll(delegate)
	}
}
