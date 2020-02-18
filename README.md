# Jenkins Job Definitions

This repo contains definitions of our Jenkins jobs (consisting of groovy
scripts) as well as shell scripts used in these jobs. Additional shell
scripts can be found in the [FwSupportTools](https://github.com/sillsdev/FwSupportTools) repo.

## File Structure

    .
    ├── bash                    # Shell script files
    ├── groovy                  # DSL script files (job definitions)
    ├── src
    │   ├── main
    │   │   ├── groovy          # support classes
    │   │   └── resources
    │   │       └── idea.gdsl   # IDE support for IDEA
    │   └── test
    │       └── groovy          # specs
    ├── Tests                   # NUnit tests
    └── build.gradle            # build file

## Information for developers

The Jenkins jobs will be created by the `Job-Wrapper-Seed-debug` job which runs some tests and
then executes the groovy scripts.

The tests can be run on a local machine by running:

	./gradlew clean test

This can helpful because it reveals any syntax errors in the scripts.

The tests can also be run by the included VS Code task.

(see [job-dsl-gradle-example](https://github.com/sheehan/job-dsl-gradle-example)
for additional information about the setup of gradle)

### Troubleshoot

Sometimes gradlew claims success but doesn't run all the tests. Delete scripts
that you aren't testing e.g. `groovy/Bloom* groovy/LfMerge* groovy/Mono*` and
make sure that the script you have changed does pass.
Don't forget to `git checkout --` them before you commit.
