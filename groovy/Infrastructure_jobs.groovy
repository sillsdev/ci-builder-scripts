/*
 * DSL script to create Infrastructure jobs
 */

def thisFile = getClass().protectionDomain.codeSource.location.path

/*
 * Definition of jobs
 */

freeStyleJob('Infrastructure-Linux-master-debug-checkoutonly') {

	description '''
<p>This job keeps the <i>docker</i> directory up-to-date on the packaging machine.</p>
<p>The job is created by the DSL plugin from <i>$thisFile</i> script.<p>
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
}

