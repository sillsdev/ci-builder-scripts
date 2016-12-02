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
					regex('(^IcuDotNet.*|^GitHub-IcuDotNet.*|icu4c)')
				}

				columns {
					status()
					weather()
					categorizedJob()
					lastSuccess()
					lastFailure()
					lastDuration()
					buildButton()
				}

				categorizationCriteria {
					regexGroupingRule('^IcuDotNet.*-any-master-release$', 'master branch jobs')
					regexGroupingRule('^GitHub.*-master-.*', 'Pre-merge builds of GitHub pull requests (master branch)')
					regexGroupingRule('icu4c', 'Builds of icu4c')
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
