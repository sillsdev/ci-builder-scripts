/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * DSL script for icu-dotnet Jenkins views
 */

/* Definition of views */

class icuDotNetViews {
	static void IcuDotNetViewAll(viewContext) {
		viewContext.with {
			categorizedJobsView('All') {
				description 'All <b>ICU</b> related jobs'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('(icu4c|icu-dotnet)')
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

nestedView('Icu') {
	configure { view ->
		view / defaultView('All')
	}
	views {
		icuDotNetViews.IcuDotNetViewAll(delegate)
	}
}
