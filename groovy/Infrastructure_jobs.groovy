/*
 * DSL script to create Infrastructure jobs
 */

/*
 * Definition of jobs
 */

freeStyleJob('Infrastructure-Linux-master-debug-checkoutonly') {

	description '''
<p>This job keeps the <i>docker</i> directory up-to-date on the packaging machine.</p>
<p>The job is created by the DSL plugin from <i>Infrastructure_jobs.groovy</i> script.<p>
'''

	label 'master';

	customWorkspace("\$HOME/docker")

	scm {
		git {
			remote {
				url("https://github.com/docker/docker.git");
				refspec("+refs/heads/*:refs/remotes/origin/*");
			}
			branch("master");
		}
	}

	triggers {
		// Run every Sunday
		// Times are UTC
		cron("H H * * 0");
	}
}

