/*
 * DSL script to create Infrastructure jobs
 */

import utilities.common

// Variables
def distros = "ubuntu:xenial debian:stretch"

/*
 * Definition of jobs
 */

folder('Infrastructure')

freeStyleJob('Infrastructure/image-factory-bootstrap') {

	description '''
<p>Create the docker image for the image builder and testjenkins slaves. Scripts come <i>from cd-infrastructure</i>.</p>
<p>The job is created by the DSL plugin from <i>Infrastructure_jobs.groovy</i> script.<p>
'''

	logRotator {
		daysToKeep(365)
		numToKeep(30)
	}

	label 'master';

	scm {
		git {
			remote {
				url("git://git.lsdev.sil.org/cd-infrastructure.git");
			}
			branch("master");
		}
	}

	wrappers {
		timestamps()
	}

	steps {
		configure common.AnsibleBuildStep("jenkins/slaves/bootstrap.yml", '''
[localhost]
localhost''', "localhost", "-c local")

		systemGroovyScriptFile('jenkins/slaves/groovy/src/addDockerCloud.groovy') {
			binding("workdir", "\${WORKSPACE}")
		}
	}
}

freeStyleJob('Infrastructure/image-factory-nextgen') {

	description '''
<p>Create the docker images for the next, unreleased Ubuntu and Debian versions.</p>
<p>The job is created by the DSL plugin from <i>Infrastructure_jobs.groovy</i> script.<p>
'''

	logRotator {
		daysToKeep(365)
		numToKeep(30)
	}

	label 'imagebuilder';

	parameters {
		stringParam("DISTROS", distros,
				"List of distros to build. The values are in the form <distro>:<version>, e.g. ubuntu:xenial")
	}
	scm {
		git {
			remote {
				url("https://github.com/docker/docker.git");
			}
			branch("master");
		}
	}

	wrappers {
		timestamps()
	}

	steps {
		shell('''
#!/bin/bash
for pair in $DISTROS; do
    parts=(${pair//:/ })
    distro=${parts[0]}
    version=${parts[1]}
    if [ "$distro" = "ubuntu" ]; then
        params="--variant=minbase --components=main,universe"
        [ -e /usr/share/debootstrap/scripts/$version ] || \\
            sudo ln -s /usr/share/debootstrap/scripts/gutsy /usr/share/debootstrap/scripts/$version
    elif [ "$distro" = "debian" ]; then
        params="--variant=minbase"
        [ -e /usr/share/debootstrap/scripts/$version ] || \\
            sudo ln -s /usr/share/debootstrap/scripts/sid /usr/share/debootstrap/scripts/$version
    else
        echo "Unknown distro $distro"
        exit 1
    fi
    sudo contrib/mkimage.sh -t $distro:$version debootstrap $params $version
''')
	}
}

freeStyleJob('Infrastructure/gerrit-ci-builder-scripts') {
	description '''
<p>Triggers a build on main Jenkins when a change gets pushed to ci-builder-scripts for review on Gerrit. This build
job will start a testing Jenkins instance and trigger a build and test run there. The results get captured in this job
and reported back to Gerrit.</p>
<p>The job is created by the DSL plugin from <i>Infrastructure_jobs.groovy</i> script.<p>
'''

	logRotator {
		daysToKeep(365)
		numToKeep(30)
	}

	label 'master';

	parameters {
		stringParam("GERRIT_BRANCH", "master",
				"The branch to build")
		stringParam("GERRIT_REFSPEC", "+refs/heads/*:refs/remotes/origin/*",
				"Refspec for the pull request")
		booleanParam("KEEP_TESTJENKINS", false, "Set to true to let the testjenkins container running after finishing" +
				"the build.\nWhen set to false the container will be killed and removed.")
	}
	scm {
		git {
			remote {
				name("gerrit")
				url('git://gerrit.lsdev.sil.org/ci-builder-scripts.git')
				refspec("\$GERRIT_REFSPEC")
			}
			branch("\$GERRIT_BRANCH")
			strategy {
				gerritTrigger()
			}
		}
	}

	triggers {
		gerrit {
			events {
				patchsetCreated()
				draftPublished()
				refUpdated()
			}
			project('ci-builder-scripts', ['plain:master', 'plain:docker'])
		}
	}
	wrappers {
		timestamps()
	}

	steps {
		// It's necessary to use configure here instead of directly shell because otherwise the order gets messed up
		configure { project ->
			project / builders / 'hudson.tasks.Shell' {
				command '''
docker kill testjenkins 2> /dev/null || true
docker rm testjenkins 2> /dev/null || true'''
			}
		}

		configure common.DockerBuildStep_CreateContainer('slave-testjenkins', 'testjenkins')

		configure common.DockerBuildStep_StartContainer('testjenkins', '''
8081:8080
50001:50000''')

		shell('''
#!/bin/bash
# Start build job
echo "Waiting for jenkins testserver to become ready..."
IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' testjenkins)
while ! curl -s "http://$IP:8080/computer/(master)/api/json" > /dev/null; do
    sleep 5
done
while [ "$(curl -s -I -w '%{http_code}' "http://$IP:8080/computer/(master)/api/json" 2>/dev/null | grep HTTP | cut -d' ' -f 2)" = "503" ]; do
    sleep 5
done

echo "Starting job..."
[ -e jenkins-cli.jar ] || wget http://$IP:8080/jnlpJars/jenkins-cli.jar

java -jar jenkins-cli.jar -s http://$IP:8080/ build ci-builder-scripts -s -v \\
    -p "GERRIT_BRANCH=$GERRIT_BRANCH" -p "GERRIT_REFSPEC=$GERRIT_REFSPEC"
''')

		shell('''
if [ "$KEEP_TESTJENKINS" = "false"  ]; then
  docker kill testjenkins  2> /dev/null || true
  docker rm testjenkins  2> /dev/null || true
fi''')
	}

	publishers {
		allowBrokenBuildClaiming()
	}
}