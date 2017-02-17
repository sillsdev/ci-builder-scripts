/*
 * Copyright (c) 2017 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * DSL script for WeSay Jenkins views
 */

/* Definition of views */

class wesayViews {
	static void WeSayViewAll(viewContext) {
		viewContext.with {
			categorizedJobsView('All') {
				description 'All <b>Wesay</b> jobs'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('.*WeSay.*')
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

				categorizationCriteria {
					regexGroupingRule('^WeSay', 'Regular builds')
					regexGroupingRule('^Gerrit', 'Pre-merge builds')
				}
			}
		}
	}
}
nestedView('WeSay') {
	configure { view ->
		view / defaultView('All')
	}
	views {
		wesayViews.WeSayViewAll(delegate)
	}
}
