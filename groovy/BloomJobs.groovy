/*
 * DSL script for Bloom Jenkins jobs
 */
import utilities.common

// Variables
def packagename = 'Bloom';
def subdir_name = 'bloom-desktop';
def distros_tobuild = 'precise trusty';
def revision = "\$(echo \${MERCURIAL_REVISION_SHORT} | cut -b 1-6)";
def package_version = '--package-version "\${FULL_BUILD_NUMBER}" ';

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

job {
    common.defaultPackagingJob(delegate, packagename, subdir_name, package_version, revision,
        distros_tobuild);

    name 'Bloom_Packaging-Linux-all-3.0-release';

    description '''
<p>Nightly builds of the Bloom 3.0 branch.</p>
<p>The job is created by the DSL plugin from <i>BloomJobs.groovy</i> script.</p>
''';

    common.hgScm(delegate, 'https://bitbucket.org/hatton/bloom-desktop', 'bloom-3.0Linux', subdir_name);
}

def subdir_name_unstable = 'bloom-desktop-unstable';

job {
    common.defaultPackagingJob(delegate, packagename, subdir_name_unstable, package_version, revision,
        distros_tobuild);

    name 'Bloom_Packaging-Linux-all-master-release';

    description '''
<p>Nightly builds of the Bloom default branch.</p>
<p>The job is created by the DSL plugin from <i>BloomJobs.groovy</i> script.</p>
''';

    common.hgScm(delegate, 'https://bitbucket.org/hatton/bloom-desktop', 'default', subdir_name_unstable);
}

