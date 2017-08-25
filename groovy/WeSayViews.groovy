/*
 * Copyright (c) 2017 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
/*
 * DSL script for WeSay Jenkins views
 */

/* Definition of views */

class weSayViews {
	static void WeSayViewPackageBuilds(viewContext) {
		viewContext.with {
			listView('Package builds') {
				description 'Package builds of <b>WeSay</b>'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('^WeSay.*_Packaging-.*')
				}

				columns {
					status()
					weather()
					name()
					lastSuccess()
					lastFailure()
					lastDuration()
					lastBuildTriggerColumn {
						causeDisplayType("icon")
					}
					buildButton()
					lastBuildNode()
					lastBuildConsole()
					slaveOrLabel()
				}
			}
		}
	}
}
nestedView('WeSay') {
/*	configure { view ->
		view / defaultView('All')
	}*/
	views {
		weSayViews.WeSayViewPackageBuilds(delegate)
	}
}
