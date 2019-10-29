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

	static void addPRPackageBuildsListView(viewContext) {
		viewContext.with {
			categorizedJobsView('PR package builds') {
				description "Pre-merge package builds of <b>Keyman</b>"
				filterBuildQueue false
				filterExecutors false

				jobs {
					regex('^Keyman_Packaging-PR-Linux.*')
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

				categorizationCriteria {
					regexGroupingRule('^Keyman_Packaging-PR-Linux.*-alpha', 'PR Alpha Package builds')
					regexGroupingRule('^Keyman_Packaging-PR-Linux.*-beta', 'PR Beta Package builds')
					regexGroupingRule('^Keyman_Packaging-PR-Linux.*-stable', 'PR Stable Package builds')
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
					regex('pipeline-keyman-packaging')
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
		CommonViews.addPackageBuildsListView(delegate, 'Keyman', '^Keyman_Packaging-Linux.*-alpha', 'Alpha Package builds')
		CommonViews.addPackageBuildsListView(delegate, 'Keyman', '^Keyman_Packaging-Linux.*-beta', 'Beta Package builds')
		CommonViews.addPackageBuildsListView(delegate, 'Keyman', '^Keyman_Packaging-Linux.*-stable', 'Stable Package builds')
		CommonViews.addPackageBuildsListView(delegate, 'Keyman', '^Keyman_Packaging-Linux-onboard-keyman-.*', 'OnboardKeyboard Package builds')
		keymanViews.addPRPackageBuildsListView(delegate)
		keymanViews.addKeymanPipelineJobs(delegate)
	}
}
