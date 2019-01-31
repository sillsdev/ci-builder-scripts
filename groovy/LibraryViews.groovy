/*
 * Copyright (c) 2016-2018 SIL International
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
					regex('(icu4c|icu-dotnet|SIL.BuildTasks|libpalaso)')
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
