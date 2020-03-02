/*
 * Copyright (c) 2018-2020 SIL International
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
					regex('^Keyman.*')
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

	static void addKeymanPipelineJobs(viewContext) {
		viewContext.with {
			categorizedJobsView('Pipeline') {
				description 'All pipeline related keyman packaging jobs'
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('pipeline-keyman-.*')
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
		view / defaultView('Pipeline')
	}
	views {
		keymanViews.addKeymanPipelineJobs(delegate)
		CommonViews.addPackageBuildsListView(delegate, 'Keyman', '^Keyman_Packaging-Linux-onboard-keyman-.*', 'OnboardKeyboard Package builds')
	}
}
