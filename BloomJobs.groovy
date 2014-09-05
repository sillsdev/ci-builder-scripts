/*
 * DSL script for Bloom Jenkins jobs
 */
import utilities.Helper;

// Variables
def packagename = 'Bloom';
def supported_distros = 'precise trusty utopic wheezy saucy';
def VCS = 'hg';

/*
 * Definition of source scripts
 */

// The bash shell script to make a source package. Called with the two string parameters
// defined above.
def makeSourceScript = '''
    # set supported distros and architectures
    DISTRIBUTIONS_TO_PACKAGE="$1"
    DISTS_TO_PROCESS="@@{supported_distros}"
    ARCHES_TO_PACKAGE="$2"
    ARCHES_TO_PROCESS="amd64 i386"
    PACKAGING_ROOT="$HOME/packages"
    VCS="@@{VCS}"

    # set Debian/changelog environment
    DEBFULLNAME='@@{packagename} Packages Signing Key'

    package_name_suffix=
    repo_base_dir=$PACKAGING_ROOT/@@{packagename}

    stderr "Preparing"

    # Clean out old source packages
    source_package_name=$(dpkg-parsechangelog |grep ^Source:|cut -d' ' -f2)
    rm -f "$repo_base_dir"/${source_package_name}_*.{dsc,build,changes,tar.*}

    # Clean out previously built binary packages, or any cancelled builds left on disk,
    # that may be in the way.
    mkdir -p "$pbuilder_path"
    cd "$pbuilder_path"
    for dist in $DISTRIBUTIONS_TO_PACKAGE; do
        for arch in $ARCHES_TO_PACKAGE; do
            rm -f $dist/$arch/result/*
            sudo umount $dist/$arch/build/*/proc 2>/dev/null || true
            sudo umount $dist/$arch/build/*/dev/pts 2>/dev/null || true
            sudo umount $dist/$arch/build/*/dev 2>/dev/null || true
            sudo rm -rf $dist/$arch/build/*
        done
    done

    # TODO: Might want to add suffix appending from build-packages

    # Don't try to process 32-bit package products if they are all architectureless.
    grep "^Architecture: " "$debian_path"/control|grep -q -v "Architecture: all$" || {
        stderr "All binary packages are listed as 'all' architecture. Resetting --arches to 'amd64'."
        ARCHES_TO_PACKAGE="amd64"
    }

    # Add entry to debian/changelog

    timestamp=$(date +"%Y%m%d.%H%M%S")
    latest_version_in_debian_changelog=$(dpkg-parsechangelog |grep ^Version:|cut -d' ' -f2)
    base_version=${code_version:-$latest_version_in_debian_changelog}
    nightlydelimeter="."
    nightlyversion="$base_version${nightlydelimeter}nightly$timestamp$package_version_extension"
    if [ "$VCS" = "hg" ]; then
        hash_of_current_commit=$(hg id -i 2>/dev/null |cut -c -12)
    else
        hash_of_current_commit=$(git rev-parse --short HEAD)
    fi

    distribution_of_last_change=$(dpkg-parsechangelog |grep ^Distribution:|cut -d' ' -f2)
    changelog_message="Built from commit $hash_of_current_commit"
    if [ -n "$most_recent_tag" ]; then
        changelog_message="See git log $most_recent_tag..$hash_of_current_commit"
    fi
    if [ -z "$PRESERVE_CHANGELOG" ]; then
        stderr "Using package version: $nightlyversion"
        dch --distribution $distribution_of_last_change --force-distribution --upstream --newversion "$nightlyversion" --force-bad-version "$changelog_message"
    else
        stderr "Using package version: $latest_version_in_debian_changelog"
    fi

    stderr "Building source package:"
    cd "$debian_path"/..
    debuild -uc -us -S -nc

    stderr "Source package files exist with the following sha256sums:"
    cd "$repo_base_dir"
    sha256sum ${source_package_name}_*.{dsc,build,changes,tar.gz}
'''
//'''.replaceAll(~/\$/, /\\\$/).replaceAll(~/@@/, /\$/);

/*
 * Definition of jobs
 */

job {
	name 'Bloom-packaging2'

    parameters {
        stringParam("DistributionsToPackage", "precise trusty utopic",
            "The distributions to build packages for");
        stringParam("ArchesToPackage", "amd64 i386",
            "The architectures to build packages for");
    }

    scm {
        hg('https://bitbucket.org/yautokes/bloom-desktop', 'default') { node ->
            node / clean('true');
        }
    }

    wrappers {
        timestamps();
    }

    steps {
        def values = [ 'packagename' : packagename,
            'supported_distros' : supported_distros,
            'VCS' : VCS ];

        shell(Helper.PrepareScript(makeSourceScript, values));
        /*
        def templateEngine = new SimpleTemplateEngine();
        def template = templateEngine.createTemplate(makeSourceScript);

        shell(template.make(values).toString());
*/
    }
}
