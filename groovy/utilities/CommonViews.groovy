/*
 * Copyright (c) 2017 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
/*
 * DSL script to with some views helper method
 */

class CommonViews {
	static void addPackageBuildsListView(viewContext, projectName, regexString) {
		viewContext.with {
			listView('Package builds') {
				description "Package builds of <b>${projectName}</b>"
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex(regexString)
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
